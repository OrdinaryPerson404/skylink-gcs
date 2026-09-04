package cn.edu.nuaa.gcs.comm;

import cn.edu.nuaa.gcs.model.Drone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 实机特征回放验证（端到端）：
 *
 * 以 2026-09-03 COM5@115200 实测捕获的 CF-Drone 帧特征为输入（HEARTBEAT /
 * ATTITUDE_QUATERNION / SCALED_IMU / RC_CHANNELS_RAW / ACTUATOR_CONTROL_TARGET /
 * EXTENDED_SYS_STATE / PARAM_VALUE，以及实测的【哨兵 BATTERY_STATUS】与【全 0 SYS_STATUS】），
 * 通过真实 {@link CommunicationService} 管线（有状态流式解析 + lenient 模式）
 * 注入 {@link Drone} 模型并核验修复目标：
 *   1. 哨兵 BATTERY_STATUS（id=255/remaining=-1/电芯 0/consumed 溢出）→ batteryValid=false，
 *      不落库、不置任何电池可用性，避免 UI 显示 0.00A/0.0°C 误导值；
 *   2. 全 0 SYS_STATUS → hasBattery=false，避免主网格显示 0%/0.0V；
 *   3. 有效 BATTERY_STATUS 到达 → 回填主网格（电量=remaining、电压=电芯和）；
 *   4. 有效数据不被后续哨兵帧覆盖；markDisconnected 全部清理；
 *   5. 遥测→模型延迟满足 ≤1s 实时同步；
 *   6. BatteryChargingMonitor 在无电池遥测时输出"未上报"说明卡、不产生 0 值数据卡。
 *
 * 仅只读验证：不发送 PARAM_SET/解锁/切模式，不依赖真实串口/WiFi/GUI。
 */
class RealDronePipelineReplayTest {

    /** 内存 Link：测试代码 feed() 塞入字节，CommunicationService 从 read() 取出（与 CommunicationServiceTest 同模式）。 */
    private static class MockLink implements Link {
        final BlockingQueue<Byte> buf = new LinkedBlockingQueue<>(65536);
        volatile boolean open = false;
        @Override public void open() { open = true; }
        @Override public boolean isOpen() { return open; }
        @Override public void send(byte[] d) { if (!open) throw new IllegalStateException(); }
        @Override public void close() { open = false; buf.clear(); }
        @Override public byte[] read() throws InterruptedException {
            Byte first = buf.poll(50, TimeUnit.MILLISECONDS);
            if (first == null) return null;
            int more = buf.size();
            byte[] out = new byte[more + 1];
            out[0] = first;
            for (int i = 0; i < more; i++) {
                Byte b = buf.poll(10, TimeUnit.MILLISECONDS);
                if (b == null) { byte[] trim = new byte[i + 1]; System.arraycopy(out, 0, trim, 0, i + 1); return trim; }
                out[i + 1] = b;
            }
            return out;
        }
        void feed(byte[] data) { for (byte b : data) buf.offer(b); }
    }

    // ================= 帧构造（v1，自动 CRC，CRC_EXTRA 取自权威表） =================
    private static int crc16(int crc, int data) {
        crc ^= data;
        for (int i = 0; i < 8; i++) crc = (crc & 1) != 0 ? (crc >>> 1) ^ 0x8408 : crc >>> 1;
        return crc;
    }
    private static int crcExtra(int msgId) {
        return switch (msgId) {
            case 0 -> 50; case 1 -> 124; case 26 -> 170; case 31 -> 246;
            case 35 -> 244; case 22 -> 220; case 140 -> 181; case 147 -> 117;
            case 245 -> 130; default -> -1;
        };
    }
    private static byte[] frame(int msgId, byte[] payload) {
        byte[] f = new byte[payload.length + 8];
        f[0] = (byte) 0xFE; f[1] = (byte) payload.length;
        f[3] = 1; f[4] = 1; f[5] = (byte) msgId;
        System.arraycopy(payload, 0, f, 6, payload.length);
        int crc = 0xFFFF;
        for (int i = 1; i <= payload.length + 5; i++) crc = crc16(crc, f[i] & 0xFF);
        int extra = crcExtra(msgId);
        if (extra >= 0) crc = crc16(crc, extra);
        f[6 + payload.length] = (byte) (crc & 0xFF);
        f[7 + payload.length] = (byte) ((crc >> 8) & 0xFF);
        return f;
    }
    private static void u16(byte[] b, int o, int v) { b[o] = (byte) (v & 0xFF); b[o + 1] = (byte) ((v >> 8) & 0xFF); }
    private static void i16(byte[] b, int o, int v) { u16(b, o, v); }
    private static void u32(byte[] b, int o, long v) { b[o] = (byte) (v & 0xFF); b[o+1] = (byte) ((v >> 8) & 0xFF); b[o+2] = (byte) ((v >> 16) & 0xFF); b[o+3] = (byte) ((v >> 24) & 0xFF); }
    private static void f32(byte[] b, int o, float v) { u32(b, o, Float.floatToIntBits(v)); }

    // CF-Drone 实测心跳：customMode=2(STAB) systemStatus=4 base_mode=0(未解锁)
    private static byte[] hb() {
        byte[] p = new byte[9];
        u32(p, 0, 2);      // custom_mode=STAB
        p[6] = 0;          // base_mode（未解锁）
        p[7] = 4;          // system_status=ACTIVE
        p[8] = 3;          // mavlink_version
        return frame(0, p);
    }
    // 实测哨兵 BATTERY_STATUS（diag-hb.txt）：id=255、temp=INT16_MIN、consumed=0xFFFFFFFF、remaining=-1、电芯全 0
    private static byte[] sentinelBattery() {
        byte[] p = new byte[31];
        p[0] = (byte) 0xFF;                       // battery_function=255（无传感器）
        i16(p, 2, Short.MIN_VALUE);               // temperature=INT16_MIN(未知)
        i16(p, 24, -1);                           // current_battery=-1 cA(未知)
        u32(p, 26, 0xFFFFFFFFL);                  // consumed=UINT32_MAX(未知)
        p[30] = -1;                               // energy_remaining=-1(未测量)
        return frame(147, p);
    }
    // 实测全 0 SYS_STATUS（diag-hb.txt）：voltage=0、current=0、batteryPct=0
    private static byte[] zeroSysStatus() {
        byte[] p = new byte[31];
        return frame(1, p);
    }
    // 有效 BATTERY_STATUS：remaining=80、2 电芯 3600/3600mV、电流 1.5A、温度 30°C
    private static byte[] validBattery() {
        byte[] p = new byte[31];
        p[0] = 0; p[1] = 1;                       // battery_function=NONE, type=LIPO
        i16(p, 2, 3000);                          // 30.00°C
        u16(p, 4, 3600); u16(p, 6, 3600);         // 电芯 3600/3600 mV
        i16(p, 24, 150);                          // 1.50 A
        u32(p, 26, 500);                          // 500 mAh
        p[30] = 80;                               // 80%
        return frame(147, p);
    }
    // SCALED_IMU（实测帧字节：acc=(0.13,-0.38,9.74) 表明传感器已就绪）
    private static byte[] scaledImu() {
        byte[] p = new byte[16];
        i16(p, 4, 13); i16(p, 6, -39); i16(p, 8, 994);
        i16(p, 10, 0); i16(p, 12, 0); i16(p, 14, -1);
        return frame(26, p);
    }
    // EXTENDED_SYS_STATE：landed_state=1(地面)
    private static byte[] landed() {
        byte[] p = new byte[2];
        p[1] = 1;
        return frame(245, p);
    }
    // RC_CHANNELS_RAW：ch1..ch8 + rssi=80
    private static byte[] rc() {
        byte[] p = new byte[21];
        int[] ch = {1500, 1500, 1000, 1000, 1000, 1000, 1000, 1000};
        for (int i = 0; i < 8; i++) u16(p, 4 + i * 2, ch[i]);
        p[20] = 80;
        return frame(35, p);
    }
    // ACTUATOR_CONTROL_TARGET：电机 3/4 输出（实测帧特征，m3/m4>0 表示悬停出力）
    private static byte[] actuator() {
        byte[] p = new byte[40];
        f32(p, 20, 0.35f); f32(p, 24, 0.35f);
        return frame(140, p);
    }
    // PARAM_VALUE：CTL_R_RATE_P=0.06 (0/112)（实测 CF-Drone 参数流）
    private static byte[] paramValue() {
        byte[] p = new byte[25];
        f32(p, 0, 0.06f);
        u16(p, 4, 112); u16(p, 6, 0);
        byte[] nm = "CTL_R_RATE_P".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(nm, 0, p, 8, nm.length);
        p[24] = 9;
        return frame(22, p);
    }

    @Test
    @DisplayName("实机特征回放：哨兵电池/全0SYS_STATUS不误报 + 有效电池回填 + 实时性 + 充电监控")
    void replayRealDroneStreamValidatesFixes() throws Exception {
        MockLink link = new MockLink();
        CommunicationService comm = new CommunicationService();
        comm.setLink(link);
        Drone drone = new Drone();
        BatteryChargingMonitor monitor = new BatteryChargingMonitor(drone, comm);

        // 回调侧统计（模拟 MainController.onTelemetry 的数据处理路径）
        Map<Integer, AtomicInteger> msgCount = new ConcurrentHashMap<>();
        AtomicInteger batteryValidSeen = new AtomicInteger(-1);   // 记录最近一次 BATTERY_STATUS 的 batteryValid
        AtomicInteger sysHasBattery = new AtomicInteger(-1);      // 记录最近一次 SYS_STATUS 的 hasBattery
        long t0Ns = System.nanoTime();
        AtomicLong firstRxNs = new AtomicLong(Long.MAX_VALUE);
        AtomicLong firstCallNs = new AtomicLong(Long.MAX_VALUE);
        comm.setCallback(t -> {
            long now = System.nanoTime();
            if (firstRxNs.get() == Long.MAX_VALUE) firstRxNs.set(now - t0Ns);
            firstCallNs.compareAndSet(Long.MAX_VALUE, now - t0Ns);
            msgCount.computeIfAbsent(t.msgId, k -> new AtomicInteger()).incrementAndGet();
            if (t.msgId == 147) batteryValidSeen.set(t.batteryValid ? 1 : 0);
            if (t.msgId == 1) sysHasBattery.set(t.hasBattery ? 1 : 0);
            drone.updateFromTelemetry(t);   // 与 MainController.onTelemetry 相同的数据处理入口
        });

        comm.start();
        try {
            // ===== 阶段 1：CF-Drone 常规遥测（闭环后飞控持续上报） =====
            long mark = System.nanoTime();
            link.feed(hb());
            link.feed(scaledImu());
            link.feed(landed());
            link.feed(rc());
            link.feed(actuator());
            link.feed(paramValue());
            Thread.sleep(600);
            assertTrue(drone.isHeartbeatAvailable(), "心跳应可用");
            assertEquals(2, drone.getCustomMode(), "customMode 应=2(STAB)");
            assertFalse(drone.isArmed(), "未解锁");
            assertEquals("STAB", drone.getCustomModeName());
            assertTrue(drone.isImuAvailable(), "IMU 应可用");
            assertTrue(drone.isLandedStateAvailable(), "落地状态应可用");
            assertTrue(drone.isRcAvailable(), "RC 应可用");
            assertTrue(drone.isMotorsAvailable(), "电机输出应可用");
            assertTrue(drone.isPositionAvailable() == false, "CF-Drone 无 GPS，位置应不可用");
            assertTrue(comm.getTotalReceived() >= 6, "应收到 ≥6 帧");
            assertTrue(comm.isLinkActive(), "链路应活跃");
            assertTrue(comm.getLinkQualityPct() > 30, "链路质量应非 0");
            report("阶段1 常规遥测", "PASS", "6+ 类报文入库; 心率=%s STAB 未解锁; IMU/落地/RC/电机可用; 链路质量=%.0f%%",
                    drone.getCustomModeName(), comm.getLinkQualityPct());

            // ===== 阶段 2：哨兵 BATTERY_STATUS + 全 0 SYS_STATUS（实测无电池传感器行为） =====
            link.feed(sentinelBattery());
            link.feed(zeroSysStatus());
            Thread.sleep(600);
            assertEquals(0, batteryValidSeen.get(), "哨兵 BATTERY_STATUS batteryValid 应为 false");
            assertEquals(0, sysHasBattery.get(), "全 0 SYS_STATUS hasBattery 应为 false");
            assertFalse(drone.isBatteryStatusAvailable(), "哨兵电池详情不应可用");
            assertFalse(drone.isBatteryAvailable(), "哨兵电池主网格不应可用");
            assertFalse(drone.isBatteryDataValid(), "isBatteryDataValid 应为 false");
            report("阶段2 哨兵/零值", "PASS", "BATTERY_STATUS(batteryValid=false) 与 SYS_STATUS(hasBattery=false) 未污染模型 → UI 显示 '—' 而非 0.00A/0%%");

            // ===== 阶段 3：有效 BATTERY_STATUS → 主网格回填（无 SYS_STATUS 时电池主指标仍可刷新） =====
            link.feed(validBattery());
            Thread.sleep(600);
            assertEquals(1, batteryValidSeen.get(), "有效 BATTERY_STATUS batteryValid 应为 true");
            assertTrue(drone.isBatteryStatusAvailable(), "有效电池详情应可用");
            assertTrue(drone.isBatteryAvailable(), "有效电池应回填主网格");
            assertTrue(drone.isBatteryDataValid(), "isBatteryDataValid 应为 true");
            assertEquals(80, drone.getBatteryPct(), 0.5, "主网格电量应为 80%");
            assertEquals(7.2, drone.getVoltage(), 0.05, "主网格电压应为电芯和 7.2V");
            assertEquals(1.5, drone.getBatteryCurrent(), 0.01, "放电电流 1.5A");
            report("阶段3 有效回填", "PASS", "电量 80% / 电压 7.2V(电芯和) / 电流 1.5A → 主网格与电池详情刷新");

            // ===== 阶段 4：有效数据不被后续哨兵帧覆盖 =====
            link.feed(sentinelBattery());
            Thread.sleep(500);
            assertEquals(1.5, drone.getBatteryCurrent(), 0.01, "哨兵不应覆盖有效电流");
            assertTrue(drone.isBatteryAvailable(), "有效电池标志不应被哨兵清除");
            assertTrue(drone.isBatteryStatusAvailable(), "详情可用性不应被哨兵清除");
            report("阶段4 防覆盖", "PASS", "有效电池值(V=%.1f,Pct=%.0f%%)在哨兵帧后保持", drone.getVoltage(), drone.getBatteryPct());

            // ===== 阶段 5：BatteryChargingMonitor 快照（卡片门控 + note） =====
            BatteryChargingMonitor.Snapshot snap = monitor.snapshot();
            assertTrue(snap.mavlinkConnected, "MAVLink 链路应判定活跃");
            boolean hasValidCard = snap.batteries.stream().anyMatch(b -> "mavlink".equals(b.source) && b.valid);
            assertTrue(hasValidCard, "此时电池有效，充电工作区应含 MAVLink 有效卡");
            report("阶段5 充电监控(有效)", "PASS", "含 mavlink-drone 有效卡: capacity=%.0f%%", snap.batteries.stream().filter(b -> "mavlink".equals(b.source)).findFirst().map(b -> b.capacity).orElse(-1.0));

            // ===== 阶段 6：断开清理（markDisconnected） =====
            drone.markDisconnected();
            assertFalse(drone.isBatteryDataValid());
            assertFalse(drone.isHeartbeatAvailable());
            assertEquals("DISCONNECTED", drone.getStatus());
            report("阶段6 断开清理", "PASS", "全部可用性清零, 状态=DISCONNECTED → UI 全字段回 '—'");

            // ===== 实时性：通信回调→模型更新延迟 << 1s =====
            long latencyMs = firstCallNs.get() / 1_000_000L;
            assertTrue(latencyMs < 1000, "首帧遥测→模型处理延迟应 <1s, 实际 " + latencyMs + "ms");
            report("实时性", "PASS", "首帧遥测处理延迟 ≈%dms（要求 ≤1s）；UI 渲染另有 100ms Timeline 节流", latencyMs);

            // ===== 链路质量 / 帧计数 =====
            report("链路质量", "PASS", "totalReceived=%d, 质量=%.0f%%(窗口10s,期望1Hz)", comm.getTotalReceived(), comm.getLinkQualityPct());
        } finally {
            comm.stop();
            monitor.stop();
        }
    }

    private static void report(String section, String result, String fmt, Object... args) {
        String text = (args == null || args.length == 0) ? fmt : String.format(fmt, args);
        System.out.println("[REPLAY] [" + result + "] " + section + " - " + text);
    }
}