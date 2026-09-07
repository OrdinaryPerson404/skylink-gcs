package com.cherglow.gcs.core;

import com.cherglow.gcs.model.VehicleSnapshot;
import com.cherglow.gcs.protocol.cli.CliProtocol;
import com.cherglow.gcs.serial.SerialTransport;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * CLI 链路：轮询调度 + 响应解析 + 快照合并发布。
 * 命令严格串行（写→收→解析→下一命令），响应结束以『解析器满足或超时』判定（CLI 无帧界）。
 * 同时实现 LinkChannel 供 ConnectionManager 驱动。
 */
public final class CliLink implements ConnectionManager.LinkChannel, CommandLink {

    /** 轮询计划：ps/psq×2（欧拉+四元数）、mot×2、rc、imu、status ≈ 姿态 ~3.3Hz / status ~1.7Hz */
    private static final String[] POLL_PLAN = {
            CliProtocol.CMD_PS, CliProtocol.CMD_MOT, CliProtocol.CMD_PSQ, CliProtocol.CMD_RC,
            CliProtocol.CMD_PS, CliProtocol.CMD_MOT, CliProtocol.CMD_PSQ, CliProtocol.CMD_IMU,
            CliProtocol.CMD_STATUS
    };

    private static final long DEFAULT_BUDGET_MS = 300;
    private static final Map<String, Long> BUDGET_MS = Map.of(
            CliProtocol.CMD_STATUS, 800L,
            CliProtocol.CMD_PARAMS, 2600L,
            CliProtocol.CMD_SYS, 1500L,
            CliProtocol.CMD_WIFI, 900L,
            CliProtocol.CMD_RC, 450L,
            CliProtocol.CMD_IMU, 450L);

    public interface SnapshotConsumer {
        void accept(VehicleSnapshot snapshot);
    }

    private final SerialTransport transport;
    private final ScheduledExecutorService executor;
    private final SnapshotConsumer out;
    private final VehicleSnapshot.Builder cur = new VehicleSnapshot.Builder();
    private final LinkedBlockingQueue<String> lines = new LinkedBlockingQueue<>();
    private final Deque<PendingCmd> onDemand = new ArrayDeque<>();

    /** 待发命令（带输出回调） */
    private record PendingCmd(String cmd, java.util.function.Consumer<String> out) {
    }
    private volatile java.util.function.Consumer<String> errorConsumer;

    private int planIdx;
    private long lastPublishMs;
    private volatile boolean polling;
    private String portName;
    private int baud;

    public CliLink(String portName, int baud, ScheduledExecutorService executor, SnapshotConsumer out) {
        this.transport = new SerialTransport();
        this.portName = portName;
        this.baud = baud;
        this.executor = executor;
        this.out = out;
        transport.setLineListener(line -> {
            lines.offer(line);
            java.util.function.Consumer<String> h = lineHandler;
            if (h != null) {
                h.accept(line);
            }
        });
        transport.setErrorListener(err -> {
            java.util.function.Consumer<String> c = errorConsumer;
            if (c != null) {
                c.accept(err);
            }
        });
    }

    /** 串口读写错误回调（S4 将接入 ConnectionManager.fail） */
    public void setErrorListener(java.util.function.Consumer<String> listener) {
        this.errorConsumer = listener;
    }

    private volatile java.util.function.Consumer<String> lineHandler;

    // ---- LinkChannel ----

    @Override
    public void setLineHandler(java.util.function.Consumer<String> onLine) {
        // 构造时已挂好 fanout（lines 队列 + 此 handler），此处仅登记
        this.lineHandler = onLine;
    }

    @Override
    public void open() throws Exception {
        transport.open(portName, baud);
        polling = true;
        planIdx = 0;
        executor.scheduleWithFixedDelay(this::pollStep, 0, 10, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        polling = false;
        transport.close();
    }

    @Override
    public boolean isOpen() {
        return transport.isOpen();
    }

    // ---- 按需命令（调参页 p、sys 等） ----

    public void requestOnce(String command) {
        onDemand.offer(new PendingCmd(command, null));
    }

    /** 发送 CLI 指令（arm/disarm/模式切换等），完成后回调原始输出（FX 线程） */
    public void sendCommand(String cmd, java.util.function.Consumer<String> out) {
        onDemand.offer(new PendingCmd(cmd, out));
    }

    public boolean isPolling() {
        return polling;
    }

    // ---- 轮询主步 ----

    private void pollStep() {
        if (!polling || !transport.isOpen()) {
            return;
        }
        PendingCmd pc = onDemand.poll();
        boolean onDemandCmd = pc != null;
        String cmd = onDemandCmd ? pc.cmd() : POLL_PLAN[planIdx++ % POLL_PLAN.length];
        long budget = BUDGET_MS.getOrDefault(cmd, DEFAULT_BUDGET_MS);
        if (cmd.startsWith("p ")) {
            budget = 800; // 单参数读取
        }
        StringBuilder acc = new StringBuilder();
        long start = System.currentTimeMillis();
        try {
            transport.writeLine(cmd);
        } catch (Exception e) {
            java.util.function.Consumer<String> c = errorConsumer;
            if (c != null) {
                c.accept("serial-write: " + e);
            }
            return;
        }
        while (polling) {
            String line;
            try {
                line = lines.poll(30, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (line != null) {
                acc.append(line).append('\n');
                if (!onDemandCmd && completeFor(cmd, acc)) {
                    break;
                }
            }
            if (System.currentTimeMillis() - start > budget) {
                break;
            }
        }
        apply(cmd, acc.toString());
        if (onDemandCmd && pc.out() != null) {
            String text = acc.toString().trim();
            if (!text.isEmpty()) {
                com.cherglow.gcs.util.FxSafe.run(() -> pc.out().accept(text));
            }
        }
    }

    private static boolean completeFor(String cmd, StringBuilder acc) {
        String s = acc.toString();
        if (cmd.startsWith("p ")) {
            return s.contains(" = "); // 单参数读取：出现赋值行即完成
        }
        return switch (cmd) {
            case CliProtocol.CMD_PS -> s.contains("yaw:");
            case CliProtocol.CMD_PSQ -> s.contains("qz:");
            case CliProtocol.CMD_MOT -> s.contains("rear-left");
            case CliProtocol.CMD_RC -> P_CONTROL_SOURCE_END.matcher(s).find();
            case CliProtocol.CMD_IMU -> s.contains("landed:");
            case CliProtocol.CMD_STATUS -> s.contains("mag(");
            default -> false; // p/sys/wifi 走预算超时
        };
    }

    private static final java.util.regex.Pattern P_CONTROL_SOURCE_END =
            java.util.regex.Pattern.compile("controlSource\\([^)]*\\):");

    // ---- 解析并合并 ----

    private void apply(String cmd, String text) {
        boolean changed = false;
        switch (cmd) {
            case CliProtocol.CMD_PS -> {
                CliProtocol.Euler e = CliProtocol.parseEuler(text);
                if (e != null) {
                    cur.rollDeg = e.rollDeg();
                    cur.pitchDeg = e.pitchDeg();
                    cur.yawDeg = e.yawDeg();
                    changed = true;
                }
            }
            case CliProtocol.CMD_PSQ -> {
                CliProtocol.Quat q = CliProtocol.parseQuat(text);
                if (q != null) {
                    cur.qw = q.qw();
                    cur.qx = q.qx();
                    cur.qy = q.qy();
                    cur.qz = q.qz();
                    changed = true;
                }
            }
            case CliProtocol.CMD_MOT -> {
                CliProtocol.Motors m = CliProtocol.parseMotors(text);
                if (m != null) {
                    cur.motorFR = m.frontRight();
                    cur.motorFL = m.frontLeft();
                    cur.motorRR = m.rearRight();
                    cur.motorRL = m.rearLeft();
                    changed = true;
                }
            }
            case CliProtocol.CMD_RC -> {
                CliProtocol.RcData r = CliProtocol.parseRc(text);
                if (r != null) {
                    cur.rcChannels = r.channels();
                    cur.ctrlRoll = r.roll();
                    cur.ctrlPitch = r.pitch();
                    cur.ctrlYaw = r.yaw();
                    cur.ctrlThrottle = r.throttle();
                    if (r.modeName() != null) {
                        cur.modeName = r.modeName();
                    }
                    if (r.controlSource() != null) {
                        cur.controlSource = r.controlSource();
                    }
                    changed = true;
                }
            }
            case CliProtocol.CMD_IMU -> {
                CliProtocol.ImuData i = CliProtocol.parseImu(text);
                if (i != null) {
                    cur.imuModel = i.model();
                    cur.imuGyro = i.gyro();
                    cur.imuAcc = i.acc();
                    cur.landed = i.landed();
                    changed = true;
                }
            }
            case CliProtocol.CMD_STATUS -> {
                CliProtocol.StatusData s = CliProtocol.parseStatus(text);
                if (s != null) {
                    cur.armed = s.armed();
                    if (s.controlSource() != null) {
                        cur.controlSource = s.controlSource();
                    }
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
                    changed = true;
                }
            }
            case CliProtocol.CMD_PARAMS -> {
                LinkedHashMap<String, Double> params = CliProtocol.parseParams(text);
                if (params != null && !params.isEmpty()) {
                    if (cur.params == null) {
                        cur.params = new LinkedHashMap<>();
                    }
                    cur.params.putAll(params);
                    changed = true;
                }
            }
            case CliProtocol.CMD_SYS -> {
                CliProtocol.SysData sys = CliProtocol.parseSys(text);
                if (sys != null) {
                    cur.chip = sys.chip();
                    cur.temperatureC = sys.temperatureC();
                    cur.freeHeap = sys.freeHeap();
                    if (sys.loopRate() != null) {
                        cur.loopRate = sys.loopRate();
                    }
                    changed = true;
                }
            }
            case CliProtocol.CMD_WIFI -> {
                CliProtocol.WifiData w = CliProtocol.parseWifi(text);
                if (w != null) {
                    cur.wifiMode = w.mode();
                    cur.wifiSsid = w.ssid();
                    cur.wifiIp = w.ip();
                    cur.mavlinkConnected = w.mavlinkConnected();
                    changed = true;
                }
            }
            case CliProtocol.CMD_TIME -> {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("Time:\\s*([\\d.]+)").matcher(text);
                if (m.find()) {
                    cur.upTimeS = Double.parseDouble(m.group(1));
                    changed = true;
                }
            }
            default -> {
            }
        }
        long now = System.currentTimeMillis();
        if (changed && now - lastPublishMs >= 40) { // 最多 ~25Hz 发布
            lastPublishMs = now;
            cur.timestampMs = now;
            out.accept(cur.build());
        }
    }
}
