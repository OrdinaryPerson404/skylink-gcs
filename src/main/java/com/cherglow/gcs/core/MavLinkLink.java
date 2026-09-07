package com.cherglow.gcs.core;

import com.cherglow.gcs.model.VehicleSnapshot;
import com.cherglow.gcs.protocol.cli.CliProtocol;
import com.cherglow.gcs.protocol.mavlink.MavLink;
import com.cherglow.gcs.transport.UdpTransport;
import com.cherglow.gcs.util.FxSafe;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * MAVLink/UDP 链路（S13）：实现 LinkChannel 接入 ConnectionManager 状态机。
 * 口径（对齐固件 mavlink.ino / wifi.ino）：
 *  - 本机绑 UDP 14550；发 192.168.4.1:14550 触发固件出流（任意下行包激活 mavlinkConnected）；
 *  - 遥测：HEARTBEAT 2Hz + ATTITUDE_QUATERNION/SCALED_IMU/RC_CHANNELS_RAW/ACTUATOR_CONTROL_TARGET 5~10Hz；
 *    ATTITUDE_QUATERNION/SCALED_IMU 为 FRD 航空系（固件已转），此处转回 FLU 与 CLI ps 同口径；
 *  - 电池/armingDisabled/phase 等不在 MAVLink 遥测里 → 用 SERIAL_CONTROL(SHELL) 桥接 CLI `status`
 *    （1Hz），复用 CliProtocol.parseStatus 解析；
 *  - 控制：COMMAND_LONG（ARM_DISARM 400 / DO_SET_MODE 176，固件回 COMMAND_ACK）；
 *    解锁前发 MANUAL_CONTROL 油门归零（固件拒绝油门>5% 时解锁且不发 ACK）；
 *  - 参数：PARAM_REQUEST_LIST/READ/SET（REAL32）；MISSION_REQUEST_LIST 由固件回 count=0（QGC 兼容）。
 */
public final class MavLinkLink implements ConnectionManager.LinkChannel, CommandLink {

    /** 模式整型 → 名称（固件仅 RAW/ACRO/STAB，用户 2026-09-07 确认；DO_SET_MODE param2 同此映射） */
    private static final String[] MODE_NAMES = {"RAW", "ACRO", "STAB"};
    private static final Map<String, Integer> MODE_IDS = Map.of(
            "raw", 0, "acro", 1, "stab", 2);
    private static final float ONE_G_MS2 = 9.80665f;

    private final String remoteIp;
    private final int remotePort;
    private final ScheduledExecutorService executor;
    private final Consumer<VehicleSnapshot> out;
    private final VehicleSnapshot.Builder cur = new VehicleSnapshot.Builder();
    private final MavLink.Parser parser = new MavLink.Parser();
    private final UdpTransport udp = new UdpTransport();

    private volatile Consumer<String> lineHandler;
    private long lastPublishMs;

    private ScheduledFuture<?> heartbeatTask;
    private ScheduledFuture<?> statusTask;
    private ScheduledFuture<?> sweeperTask;
    // SERIAL_CONTROL 下行变体自适应：false=现代 81B(含 targets, extra 189)，true=旧 79B(extra 220)
    private boolean serialVariant;
    private long lastShellRxMs;
    private int shellNoRxStreak;

    /** 进行中的按需命令（单在途） */
    private static final class PendingCmd {
        final Integer mavCommand;      // 等待 COMMAND_ACK 的命令号（null=非 ACK 类）
        final Consumer<String> out;    // 完成回调（null=仅执行）
        final StringBuilder acc = new StringBuilder();
        final long deadlineMs;
        boolean paramList;             // PARAM_REQUEST_LIST 汇总
        int paramCount = -1;
        int paramSeen;

        PendingCmd(Integer mavCommand, Consumer<String> out, long deadlineMs) {
            this.mavCommand = mavCommand;
            this.out = out;
            this.deadlineMs = deadlineMs;
        }
    }

    private volatile PendingCmd pending;

    public MavLinkLink(String remoteIp, int remotePort, ScheduledExecutorService executor,
                       Consumer<VehicleSnapshot> out) {
        this.remoteIp = remoteIp;
        this.remotePort = remotePort;
        this.executor = executor;
        this.out = out;
    }

    // ---- LinkChannel ----

    @Override
    public void setLineHandler(Consumer<String> onLine) {
        this.lineHandler = onLine;
    }

    @Override
    public void open() throws Exception {
        udp.setListener((data, len, from) -> parser.feed(data, 0, len, this::onFrame));
        udp.open(14550, remoteIp, remotePort);
        heartbeatTask = executor.scheduleWithFixedDelay(this::sendGcsHeartbeat, 150, 1000, TimeUnit.MILLISECONDS);
        statusTask = executor.scheduleWithFixedDelay(() -> sendShell("status"), 900, 1000, TimeUnit.MILLISECONDS);
        sweeperTask = executor.scheduleWithFixedDelay(this::sweepPending, 100, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        for (ScheduledFuture<?> t : new ScheduledFuture<?>[]{heartbeatTask, statusTask, sweeperTask}) {
            if (t != null) {
                t.cancel(false);
            }
        }
        udp.close();
    }

    @Override
    public boolean isOpen() {
        return udp.isOpen();
    }

    // ---- 定时任务 ----

    /** GCS 心跳：保持固件 mavlinkConnected 出流 */
    private void sendGcsHeartbeat() {
        if (!isOpen()) {
            return;
        }
        udp.send(MavLink.buildV1(MavLink.MSG_HEARTBEAT, MavLink.payloadHeartbeatGcs()));
    }

    /** 超时清扫：到期的在途命令按已收内容完成 */
    private void sweepPending() {
        PendingCmd pc = pending;
        if (pc == null || System.currentTimeMillis() < pc.deadlineMs) {
            return;
        }
        pending = null;
        String acc = pc.acc.toString().trim();
        String text = acc.isEmpty() && pc.mavCommand != null
                ? "⚠ 未收到 COMMAND_ACK（固件可能拒绝：解锁需油门 < 5%，或链路未激活）"
                : acc.isEmpty() ? "⚠ 响应超时" : acc;
        if (pc.out != null) {
            FxSafe.run(() -> pc.out.accept(text));
        }
    }

    // ---- 数据面 ----

    private void onDatagram(byte[] data, int len, java.net.InetAddress from) {
        parser.feed(data, 0, len, this::onFrame);
    }

    private void onFrame(MavLink.Frame f) {
        Consumer<String> lh = lineHandler;
        if (lh != null) {
            lh.accept("mavlink msgid=" + f.msgid);
        }
        boolean changed;
        switch (f.msgid) {
            case MavLink.MSG_HEARTBEAT -> {
                long customMode = MavLink.rU32(f.payload, 0);
                int baseMode = MavLink.rU8(f.payload, 6);
                cur.armed = (baseMode & 0x80) != 0;
                int m = (int) Math.min(customMode, MODE_NAMES.length - 1);
                if (customMode <= MODE_NAMES.length - 1) {
                    cur.modeName = MODE_NAMES[m];
                }
                changed = true;
            }
            case MavLink.MSG_EXTENDED_SYS_STATE -> {
                int landedState = MavLink.rU8(f.payload, 1); // vtol_state@0
                cur.landed = landedState == 1 ? Boolean.TRUE : landedState == 2 ? Boolean.FALSE : null;
                changed = true;
            }
            case MavLink.MSG_ATTITUDE_QUATERNION -> {
                // FRD → FLU：四元数矢量部 y/z 取反，欧拉角即与 CLI ps 同口径
                double w = MavLink.rF32(f.payload, 4);
                double x = MavLink.rF32(f.payload, 8);
                double y = -MavLink.rF32(f.payload, 12);
                double z = -MavLink.rF32(f.payload, 16);
                double sqx = x * x, sqy = y * y, sqz = z * z, sqw = w * w;
                double sarg = clamp(-2 * (x * z - w * y));
                double roll = Math.atan2(2 * (y * z + w * x), sqw - sqx - sqy + sqz);
                double pitch = Math.asin(sarg);
                double yaw = Math.atan2(2 * (x * y + w * z), sqw + sqx - sqy - sqz);
                cur.rollDeg = Math.toDegrees(roll);
                cur.pitchDeg = Math.toDegrees(pitch);
                cur.yawDeg = Math.toDegrees(yaw);
                float rs0 = MavLink.rF32(f.payload, 20), rs1 = MavLink.rF32(f.payload, 24),
                        rs2 = MavLink.rF32(f.payload, 28);
                cur.imuGyro = new double[]{rs0, -rs1, -rs2}; // rad/s FLU
                changed = true;
            }
            case MavLink.MSG_SCALED_IMU -> {
                double ax = MavLink.rS16(f.payload, 4) * ONE_G_MS2 / 1000;
                double ay = -MavLink.rS16(f.payload, 6) * ONE_G_MS2 / 1000;
                double az = -MavLink.rS16(f.payload, 8) * ONE_G_MS2 / 1000;
                cur.imuAcc = new double[]{ax, ay, az}; // m/s² FLU
                changed = true;
            }
            case MavLink.MSG_RC_CHANNELS_RAW -> {
                int[] ch = new int[8];
                for (int i = 0; i < 8; i++) {
                    ch[i] = MavLink.rU16(f.payload, 4 + 2 * i); // time u32 之后
                }
                cur.rcChannels = ch;
                changed = true;
            }
            case MavLink.MSG_ACTUATOR_CONTROL_TARGET -> {
                // controls 顺序 = 固件 motors[]：RL, RR, FR, FL
                cur.motorRL = MavLink.rF32(f.payload, 8);   // time_usec u64 之后
                cur.motorRR = MavLink.rF32(f.payload, 12);
                cur.motorFR = MavLink.rF32(f.payload, 16);
                cur.motorFL = MavLink.rF32(f.payload, 20);
                changed = true;
            }
            case MavLink.MSG_SERIAL_CONTROL -> {
                if (MavLink.rU8(f.payload, 6) != 10) { // device@6：只处理 SHELL
                    changed = false;
                    break;
                }
                int count = MavLink.rU8(f.payload, 8);
                String chunk = new String(f.payload, 9, Math.min(count, 70),
                        java.nio.charset.StandardCharsets.US_ASCII);
                lastShellRxMs = System.currentTimeMillis();
                Integer learnedSc = MavLink.learnedExtra(MavLink.MSG_SERIAL_CONTROL);
                if (learnedSc != null) {
                    serialVariant = learnedSc == 220; // 学习到固件为旧布局则锁定
                }
                feedShell(chunk);
                changed = false;
            }
            case MavLink.MSG_COMMAND_ACK -> {
                int command = MavLink.rU16(f.payload, 0);
                int result = MavLink.rU8(f.payload, 2);
                PendingCmd pc = pending;
                if (pc != null && pc.mavCommand != null && pc.mavCommand == command) {
                    pending = null;
                    String res = switch (result) {
                        case 0 -> "已接受";
                        case 1 -> "临时拒绝（IN_PROGRESS）";
                        case 2 -> "被拒绝";
                        case 4 -> "固件不支持";
                        default -> "结果码 " + result;
                    };
                    if (pc.out != null) {
                        String text = "指令 " + command + " → " + res;
                        FxSafe.run(() -> pc.out.accept(text));
                    }
                }
                changed = false;
            }
            case MavLink.MSG_PARAM_VALUE -> {
                float value = MavLink.rF32(f.payload, 0);   // 线上序：value f32 → count/index u16 → id[16]
                int total = MavLink.rU16(f.payload, 4);
                String id = MavLink.rAscii(f.payload, 8, 16);
                if (cur.params == null) {
                    cur.params = new LinkedHashMap<>();
                }
                cur.params.put(id, (double) value);
                PendingCmd pc = pending;
                if (pc != null && pc.mavCommand == null) {
                    pc.acc.append(id).append(" = ").append(fmt(value)).append('\n');
                    if (pc.paramList) {
                        pc.paramCount = total;
                        pc.paramSeen++;
                        if (pc.paramCount > 0 && pc.paramSeen >= pc.paramCount) {
                            complete(pc);
                        }
                    } else {
                        complete(pc);
                    }
                }
                changed = true;
            }
            default -> changed = false;
        }
        long now = System.currentTimeMillis();
        if (changed && now - lastPublishMs >= 40) {
            lastPublishMs = now;
            cur.timestampMs = now;
            out.accept(cur.build());
        }
    }

    /** CLI 文本（SERIAL_CONTROL SHELL 桥）分行处理：控制台回调收集 + status 块解析 */
    private final StringBuilder shellLine = new StringBuilder();
    private final StringBuilder statusAcc = new StringBuilder();

    private void feedShell(String chunk) {
        shellLine.append(chunk);
        int idx;
        while ((idx = shellLine.indexOf("\n")) >= 0) {
            String line = shellLine.substring(0, idx).replace("\r", "");
            shellLine.delete(0, idx + 1);
            if (line.isBlank() || line.startsWith("> ")) {
                continue; // 空行 / 命令回显
            }
            PendingCmd pc = pending;
            if (pc != null && pc.mavCommand == null && !pc.paramList) {
                pc.acc.append(line).append('\n');
            }
            statusAcc.append(line).append('\n');
            if (line.contains("mag(")) { // status 块结束标记（与 CliLink completeFor 一致）
                CliProtocol.StatusData sd = CliProtocol.parseStatus(statusAcc.toString());
                if (sd != null) {
                    mergeStatus(sd);
                }
                statusAcc.setLength(0);
                PendingCmd shell = pending;
                if (shell != null && shell.mavCommand == null && !shell.paramList
                        && shell.acc.toString().contains("mag(")) {
                    complete(shell); // 桥接命令收到完整 status 块 → 立即完成，不等超时
                }
            }
        }
    }

    private void mergeStatus(CliProtocol.StatusData s) {
        cur.armed = s.armed();
        cur.controlSource = s.controlSource();
        cur.rcLinkUp = s.rcLinkUp();
        cur.webLinkUp = s.webLinkUp();
        cur.mavLinkUp = s.mavLinkUp();
        cur.crsfLinkQuality = s.crsfLinkQuality();
        cur.loopRate = s.loopRate();
        cur.armingDisabled = s.armingDisabled();
        cur.phase = s.phase();
        cur.isAirborne = s.isAirborne();
        cur.landed = s.landed();
        cur.batteryVoltage = s.batteryVoltage();
        cur.altitude = s.altitude();
        cur.tofStatus = s.tofStatus();
        cur.sensorsBaro = s.baro();
        cur.sensorsRange = s.range();
        cur.hasAltitude = s.hasAltitude();
        cur.magOk = s.magOk();
        long now = System.currentTimeMillis();
        cur.timestampMs = now;
        out.accept(cur.build());
    }

    // ---- 命令面 ----

    @Override
    public void requestOnce(String cmd) {
        sendCommand(cmd, null);
    }

    @Override
    public void sendCommand(String cmd, Consumer<String> out) {
        String c = cmd == null ? "" : cmd.trim();
        String low = c.toLowerCase();
        if (pending != null) {
            // 单在途：直接超时丢弃旧命令（CLI 链路为排队语义，此处简化）
            complete(pending);
        }
        long now = System.currentTimeMillis();
        switch (low) {
            case "arm", "disarm" -> {
                sendManualControlZero();
                mavLong(400, low.equals("arm") ? 1 : 0, 0, out, 2500);
            }
            case "raw", "acro", "stab", "althold", "poshold", "auto" -> {
                mavLong(176, 0, MODE_IDS.get(low), out, 2000);
            }
            case "p" -> {
                PendingCmd pc = new PendingCmd(null, out, now + 4000);
                pc.paramList = true;
                pending = pc;
                udp.send(MavLink.buildV1(MavLink.MSG_PARAM_REQUEST_LIST, MavLink.payloadParamRequestList()));
            }
            default -> {
                if (low.startsWith("p ")) {
                    String[] parts = c.substring(2).trim().split("\\s+");
                    if (parts.length == 1) {
                        pending = new PendingCmd(null, out, now + 1500);
                        udp.send(MavLink.buildV1(MavLink.MSG_PARAM_REQUEST_READ,
                                MavLink.payloadParamRequestRead(parts[0], -1)));
                    } else if (parts.length >= 2) {
                        float v;
                        try {
                            v = Float.parseFloat(parts[1]);
                        } catch (NumberFormatException e) {
                            if (out != null) {
                                FxSafe.run(() -> out.accept("⚠ 参数值非法：" + parts[1]));
                            }
                            return;
                        }
                        pending = new PendingCmd(null, out, now + 1500);
                        udp.send(MavLink.buildV1(MavLink.MSG_PARAM_SET,
                                MavLink.payloadParamSet(parts[0], v)));
                    }
                } else {
                    int budget = switch (low) {
                        case "status" -> 3000;
                        case "wifi" -> 2500;
                        case "sys" -> 3000;
                        case "log" -> 4000;
                        default -> 1500;
                    };
                    PendingCmd pc = new PendingCmd(null, out, now + budget);
                    pc.acc.append(""); // 保持非 paramList 语义
                    pending = pc;
                    sendShell(c);
                }
            }
        }
    }

    private void mavLong(int command, float param1, float param2, Consumer<String> out, long budgetMs) {
        pending = new PendingCmd(command, out, System.currentTimeMillis() + budgetMs);
        udp.send(MavLink.buildV1(MavLink.MSG_COMMAND_LONG, MavLink.payloadCommandLong(command, param1, param2)));
    }

    /** 油门归零并接管控制源（固件拒绝油门>5% 时解锁） */
    private void sendManualControlZero() {
        udp.send(MavLink.buildV1(MavLink.MSG_MANUAL_CONTROL, MavLink.payloadManualControl(1, 0, 0, 0, 0, 0)));
    }

    private void sendShell(String cmd) {
        if (!isOpen()) {
            return;
        }
        // 变体自适应：连续 2 次发送无任何 SHELL 回包 → 切换布局（现代 81B/189 与 旧 79B/220）
        long now = System.currentTimeMillis();
        if (lastShellRxMs == 0 || now - lastShellRxMs > 4000) {
            if (++shellNoRxStreak >= 2) {
                serialVariant = !serialVariant;
                shellNoRxStreak = 0;
            }
        } else {
            shellNoRxStreak = 0;
        }
        byte[] data = cmd.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        int count = Math.min(data.length, 70);
        byte[] frame;
        if (serialVariant) {
            // 旧 79B：device/flags/timeout u16/count/data[70]（无 targets），extra 220
            MavLink.PBuf pb = new MavLink.PBuf()
                    .u8(10).u8(0).u16(0).u8(count);
            pb.bytes(data, count);
            for (int i = count; i < 70; i++) {
                pb.u8(0);
            }
            frame = MavLink.buildV1(MavLink.MSG_SERIAL_CONTROL, pb.toArray(),
                    MavLink.SERIAL_CONTROL_EXTRA_LEGACY);
        } else {
            frame = MavLink.buildV1(MavLink.MSG_SERIAL_CONTROL,
                    MavLink.payloadSerialControlShell(cmd));
        }
        udp.send(frame);
    }

    private void complete(PendingCmd pc) {
        if (pending == pc) {
            pending = null;
        }
        String text = pc.acc.toString().trim();
        if (pc.out != null) {
            FxSafe.run(() -> pc.out.accept(text));
        }
    }

    private static String fmt(double v) {
        return v == Math.floor(v) && !Double.isInfinite(v)
                ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static double clamp(double v) {
        return Math.max(-1, Math.min(1, v));
    }
}
