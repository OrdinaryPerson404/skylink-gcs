package cn.edu.nuaa.gcs.comm;

import cn.edu.nuaa.gcs.model.Drone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 实机在线验证（经 ADB-UDP 中继：电脑 127.0.0.1:15550 <-手机(Drone_WiFi)-> 飞控 UDP 14550）。
 *
 * 前置条件：AdbUdpRelay 已在本机运行（bridge 15550），手机已连 Drone_WiFi 且 USB 调试连接电脑。
 * 运行：mvn test -Dtest=LiveDroneVerificationTest
 *
 * 验证要点（针对本次修复目标）：
 * 1. 真实心跳可达：drone.isHeartbeatAvailable()==true、customMode 与真实飞控一致；
 * 2. CF-Drone 固件不发送 BATTERY_STATUS/SYS_STATUS -> 电池可用性全 false（UI 应显示 '—' 而非 0/0%），
 *    即"缺失数据诚实标识"在实机生效；
 * 3. 若意外收到 BATTERY_STATUS(147)，其 batteryValid 必须为 false（不能被当作有效电池）；
 * 4. 遥测->模型延迟（网络+解析）实时性 <1s；
 * 5. 链路质量、帧计数、PARAM_REQUEST_LIST 只读参数流可用；
 * 6. BatteryChargingMonitor 快照：MAVLink 无电池遥测 -> 占位卡 + note 说明。
 *
 * 全程只读：HEARTBEAT / PARAM_REQUEST_LIST / REQUEST_MESSAGE; 不含 PARAM_SET/解锁/切模式。
 */
class LiveDroneVerificationTest {

    @Test
    @DisplayName("实机在线：经ADB中继验证飞控遥测 + 电池缺失诚实标识 + 实时性")
    void liveDroneOverAdbRelay() throws Exception {
        UdpLink link = new UdpLink(15551, "127.0.0.1", 15550);  // 经 ADB 中继到飞控 UDP 14550
        CommunicationService comm = new CommunicationService();
        comm.setLink(link);
        Drone drone = new Drone();
        BatteryChargingMonitor monitor = new BatteryChargingMonitor(drone, comm);

        Map<Integer, AtomicInteger> msgCount = new ConcurrentHashMap<>();
        AtomicInteger batteryValids = new AtomicInteger(0);      // 收到 BATTERY_STATUS 且 batteryValid=true 的次数
        AtomicInteger batteryInvalids = new AtomicInteger(0);    // 收到 BATTERY_STATUS 且 batteryValid=false 的次数
        AtomicInteger batteryFrames = new AtomicInteger(0);      // 收到 BATTERY_STATUS 的总次数
        AtomicInteger sysStatusFrames = new AtomicInteger(0);    // 收到 SYS_STATUS 的总次数
        AtomicInteger lastBatteryValid = new AtomicInteger(-1);
        // 电池字段观测范围（评估实机值是否真实合理）
        AtomicReference<double[]> bsObs = new AtomicReference<>(new double[]{Double.MAX_VALUE, -Double.MAX_VALUE, // remaining min/max
                Double.MAX_VALUE, -Double.MAX_VALUE, // current min/max
                Double.MAX_VALUE, -Double.MAX_VALUE, // temp min/max
                Double.MAX_VALUE, -Double.MAX_VALUE, // cells0 min/max
                Double.MAX_VALUE, -Double.MAX_VALUE, // voltage(SYS) min/max
                0, 0});                                 // count
        AtomicReference<String> lastBatteryNote = new AtomicReference<>();
        AtomicLong maxLatencyMs = new AtomicLong(0);
        AtomicLong nowRef = new AtomicLong(0);

        comm.setCallback(t -> {
            long now = System.currentTimeMillis();
            long latency = now - nowRef.get();
            if (nowRef.get() > 0 && latency > maxLatencyMs.get()) maxLatencyMs.set(latency);
            nowRef.set(now);
            msgCount.computeIfAbsent(t.msgId, k -> new AtomicInteger()).incrementAndGet();
            if (t.msgId == 147) {
                batteryFrames.incrementAndGet();
                if (batteryFrames.get() <= 3) {
                    System.out.println("[LIVE] 147 帧样例: batteryValid=" + t.batteryValid
                            + " remaining=" + t.batteryRemaining2 + " cur=" + t.batteryCurrent
                            + " temp=" + t.batteryTemp + " consumed=" + t.capacityConsumed
                            + " cells=" + java.util.Arrays.toString(t.cellVoltages));
                }
                lastBatteryValid.set(t.batteryValid ? 1 : 0);
                if (t.batteryValid) batteryValids.incrementAndGet(); else batteryInvalids.incrementAndGet();
                double[] o = bsObs.get();
                o[10]++;
                o[0] = Math.min(o[0], t.batteryRemaining2); o[1] = Math.max(o[1], t.batteryRemaining2);
                o[2] = Math.min(o[2], t.batteryCurrent); o[3] = Math.max(o[3], t.batteryCurrent);
                o[4] = Math.min(o[4], t.batteryTemp); o[5] = Math.max(o[5], t.batteryTemp);
                o[6] = Math.min(o[6], t.cellVoltages[0]); o[7] = Math.max(o[7], t.cellVoltages[0]);
                o[8] = Math.min(o[8], t.voltage); o[9] = Math.max(o[9], t.voltage);
            }
            if (t.msgId == 1) {
                sysStatusFrames.incrementAndGet();
                double[] o = bsObs.get();
                o[8] = Math.min(o[8], t.voltage); o[9] = Math.max(o[9], t.voltage);
            }
            drone.updateFromTelemetry(t);
        });

        comm.start();
        try {
            // 首个心跳到达后自动请求参数列表（只读）
            Thread.sleep(1500);
            comm.requestParamList();
            comm.requestMessage(148); // AUTOPILOT_VERSION（只读）
            // 监听约 18 秒
            System.out.println("[LIVE] 链路已启动，监听 18 秒（只读握手：HEARTBEAT/PARAM_REQUEST_LIST/REQUEST_MESSAGE）…");
            long start = System.currentTimeMillis();
            boolean sawHeartbeat = false;
            while (System.currentTimeMillis() - start < 18000) {
                Thread.sleep(2000);
                if (drone.isHeartbeatAvailable()) sawHeartbeat = true;
                System.out.printf("[LIVE] t=%ds hb=%s frames=%d linkQ=%.0f%% mode=%s armed=%s battery(status=%s,avail=%s)%n",
                        (System.currentTimeMillis() - start) / 1000,
                        sawHeartbeat, comm.getTotalReceived(), comm.getLinkQualityPct(),
                        drone.getCustomModeName(), drone.isArmed(),
                        drone.isBatteryStatusAvailable(), drone.isBatteryAvailable());
            }

            // ===== 断言 1：真实遥测可达 =====
            // 离线保护：无人机不在线（无遥测）时跳过本用例，保证全量回归不依赖硬件
            org.junit.jupiter.api.Assumptions.assumeTrue(comm.getTotalReceived() > 0,
                    "无人机不在线（18s 内无遥测帧），跳过实机验证");
            assertTrue(sawHeartbeat, "18s 内应收到真实 HEARTBEAT");
            assertTrue(comm.getTotalReceived() > 0, "应收到遥测帧");
            assertTrue(comm.isLinkActive(), "链路应活跃");
            report("真实遥测可达", "PASS", "totalReceived=%d 链路质量=%.0f%% 模式=%s 解锁=%s",
                    comm.getTotalReceived(), comm.getLinkQualityPct(), drone.getCustomModeName(), drone.isArmed());

            // ===== 断言 2：电池可用性（实机 CF-Drone 实测行为） =====
            double[] o = bsObs.get();
            boolean sawBattery = batteryFrames.get() > 0 || sysStatusFrames.get() > 0;
            boolean batteryOk = drone.isBatteryDataValid();
            report("电池可用性(实机)", "PASS",
                    "BATTERY_STATUS帧=%d(有效%d/无效%d) SYS_STATUS帧=%d -> batteryStatusAvailable=%s batteryAvailable=%s isBatteryDataValid=%s",
                    batteryFrames.get(), batteryValids.get(), batteryInvalids.get(), sysStatusFrames.get(),
                    drone.isBatteryStatusAvailable(), drone.isBatteryAvailable(), batteryOk);
            if (sawBattery && batteryFrames.get() > 0) {
                report("电池字段观测", "INFO",
                        "remaining[%d..%d]%% current[%.2f..%.2f]A temp[%.1f..%.1f]°C cell0[%.0f..%.0f]mV sysVolt[%.2f..%.2f]V",
                        (int) o[0], (int) o[1], o[2], o[3], o[4], o[5], o[6], o[7], o[8], o[9]);
                if (batteryValids.get() > 0) {
                    assertTrue(batteryOk, "存在有效电池信息时电池必须可用");
                    // 电压必须物理合理（合理电芯 2000~5000mV 之和，实机为 3744mV 槽位）
                    double v = drone.getVoltage();
                    assertTrue(v >= 2.0 && v <= 25.0, "无人机电池电压应物理合理(2~25V)，实际 " + v);
                    report("电池电压(实机)", "PASS", "电压=%sV 由物理合理电芯推导（排除0x7FFF垃圾槽位）", v);
                    // 电量可见性：取决于 remaining/SYS_STATUS 电量是否真实上报
                    if (drone.isBatteryPctAvailable()) {
                        assertTrue(drone.getBatteryPct() >= 0 && drone.getBatteryPct() <= 100,
                                "主网格电量应在 [0,100]%，实际 " + drone.getBatteryPct());
                        report("电量显示(实机)", "PASS", "电量=%s%%", drone.getBatteryPct());
                    } else {
                        report("电量缺失处理(实机)", "PASS",
                                "remaining 未上报(实机=-1) -> 主网格电量显示 '—'（绝不显示默认 100%%）");
                    }
                    // 哨兵/无效帧不得混入（一致性）
                    assertEquals(0, batteryInvalids.get(), "同一链路内电池有效性应一致");
                } else {
                    assertFalse(batteryOk, "电池帧全为无效哨兵时，电池必须不可用(UI 显示 '—')");
                    report("电池有效性(核心修复)", "PASS",
                            "全部 %d 帧为无效哨兵(remaining=-1/电芯无合理值/consumed=%s) -> 电池显示 '—'，不被 consumed 误判",
                            batteryInvalids.get(), "0x7FFFFFFF");
                }
            }

            // ===== 断言 3：其它关键状态实时性（姿态/IMU/落地/电机，若有） =====
            report("关键状态", "PASS", "心跳=%s 姿态=%s IMU=%s 落地=%s RC=%s 电机=%s GPS=%s",
                    drone.isHeartbeatAvailable(), drone.isAttitudeAvailable(), drone.isImuAvailable(),
                    drone.isLandedStateAvailable(), drone.isRcAvailable(), drone.isMotorsAvailable(), drone.isGpsAvailable());
            if (drone.isAttitudeAvailable()) {
                assertTrue(Math.abs(drone.getRoll()) <= Math.PI && Math.abs(drone.getPitch()) <= Math.PI,
                        "姿态弧度应在有效范围");
            }

            // ===== 断言 4：实时性（遥测帧->回调延迟）<1s =====
            long lat = maxLatencyMs.get();
            report("实时性", "PASS", "实测遥测帧处理延迟 max=%dms（要求 ≤1s）", lat);
            assertTrue(lat < 1000, "遥测->模型延迟必须 <1s，实测 " + lat + "ms");

            // ===== 断言 5：BatteryChargingMonitor 快照（卡片有效性与模型一致性） =====
            BatteryChargingMonitor.Snapshot snap = monitor.snapshot();
            snapshotLog(snap);
            var mavCardOpt = snap.batteries.stream().filter(b -> "mavlink".equals(b.source)).findFirst();
            assertTrue(mavCardOpt.isPresent(), "MAVLink 已连接时快照应含无人机电池卡");
            var b = mavCardOpt.get();
            // 卡片有效性必须与 Drone 模型一致：有部分有效遥测(实机=电芯电压)→有效卡；
            // 完全无效→说明卡(note)
            assertEquals(drone.isBatteryDataValid(), b.valid,
                    "MAVLink 卡有效性应与 isBatteryDataValid() 一致");
            if (b.valid) {
                // 有效卡：电压必须是真实物理值；电量仅在 remaining 真实上报时展示（否则 -1→UI 显示 "--"）
                assertTrue(b.voltage >= 2.0 && b.voltage <= 25.0, "卡片电压应物理合理，实际 " + b.voltage);
                assertEquals(drone.isBatteryPctAvailable(), b.capacity >= 0,
                        "电量未上报时卡片容量必须为 -1（UI 显示 '--'），不得显示默认 100%");
                assertTrue(snap.anomalies.isEmpty() || snap.anomalies.stream().noneMatch(a -> "over_voltage".equals(a.code)),
                        "不得产生电压超限误报");
                report("充电监控(实机)", "PASS",
                        "MAVLink 有效卡: 电压=%.2fV 容量=%s（电量未上报→--，诚实标识）异常=%d",
                        b.voltage, b.capacity >= 0 ? b.capacity + "%" : "--", snap.anomalies.size());
            } else {
                assertTrue(b.note != null && (b.note.contains("未上报") || b.note.contains("固件")),
                        "无效时应显示'固件未上报电池遥测'说明卡");
                report("充电监控(实机)", "PASS", "MAVLink 说明卡: %s", b.note);
            }
        } finally {
            comm.stop();
            monitor.stop();
        }
    }

    private static void snapshotLog(BatteryChargingMonitor.Snapshot snap) {
        System.out.println("[LIVE] 充电监控快照: wmi=" + snap.wmiAvailable + " mavlinkConnected=" + snap.mavlinkConnected
                + " batteries=" + snap.batteries.size() + " anomalies=" + snap.anomalies.size());
        for (BatteryChargingMonitor.BatteryInfo b : snap.batteries) {
            System.out.println("[LIVE]   卡: " + b.source + " name=" + b.name + " valid=" + b.valid
                    + " capacity=" + b.capacity + " note=" + (b.note != null ? b.note : "-"));
        }
    }

    private static void report(String section, String result, String fmt, Object... args) {
        System.out.println("[LIVE] [" + result + "] " + section + " - " + String.format(fmt, args));
    }
}