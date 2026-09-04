package cn.edu.nuaa.gcs.comm;

import cn.edu.nuaa.gcs.comm.BatteryChargingMonitor.BatteryInfo;
import cn.edu.nuaa.gcs.comm.BatteryChargingMonitor.Snapshot;
import cn.edu.nuaa.gcs.comm.MavlinkParser.Telemetry;
import cn.edu.nuaa.gcs.model.Drone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BatteryChargingMonitor 单元测试。
 *
 * <p>直接构造 Drone / CommunicationService，不启动 JavaFX，也不调用 monitor.start()
 * （避免启动定时采样任务）。通过反射注入 {@code lastReceivedMs} 伪造"链路活跃"，
 * 通过手置 Telemetry 字段驱动 Drone 进入"有效电池" / "无电池"状态。
 * 不依赖真实串口 / WMI / 网络。
 */
class BatteryChargingMonitorTest {

    /** 伪造一个近 5 秒内有遥测的活跃链路，使 isLinkActive() 返回 true。 */
    private static CommunicationService activeComm() throws Exception {
        CommunicationService comm = new CommunicationService();
        java.lang.reflect.Field f = CommunicationService.class.getDeclaredField("lastReceivedMs");
        f.setAccessible(true);
        f.setLong(comm, System.currentTimeMillis());
        return comm;
    }

    /** 构造带有效 BATTERY_STATUS 的遥测并喂给 Drone，使其进入"有效电池"状态。 */
    private static Drone validBatteryDrone() {
        Telemetry t = new Telemetry();
        t.valid = true;
        t.hasBatteryStatus = true;
        t.batteryValid = true;
        t.batteryRemaining2 = 80;
        t.batteryCurrent = 1.5;
        t.batteryTemp = 30.0;
        t.cellVoltages = new int[]{3600, 3600, 0, 0, 0, 0, 0, 0, 0, 0};
        Drone d = new Drone();
        d.updateFromTelemetry(t);
        return d;
    }

    @Test
    @DisplayName("非 Windows 平台优雅降级：WMI 不可用且无数据卡")
    void testNonWindowsFallbackNoCrash() {
        BatteryChargingMonitor mon = new BatteryChargingMonitor(new Drone(), null, false);
        Snapshot s = mon.snapshot();   // 不应崩溃
        assertFalse(s.wmiAvailable, "非 Windows 平台 WMI 应不可用");
        assertFalse(s.batteries.stream().anyMatch(b -> "wmi-system".equals(b.id)),
                "不应包含 wmi-system 数据卡");
        assertEquals(0, s.validation.checkedBatteries, "无任何电池数据源时应为 0");
    }

    @Test
    @DisplayName("MAVLink 链路活跃 + 有效电池遥测：生成有效数据卡")
    void testMavlinkValidBatteryCard() throws Exception {
        BatteryChargingMonitor mon = new BatteryChargingMonitor(validBatteryDrone(), activeComm(), false);
        Snapshot s = mon.snapshot();

        assertTrue(s.mavlinkConnected);
        BatteryInfo mav = s.batteries.stream()
                .filter(b -> "mavlink-drone".equals(b.id))
                .findFirst().orElse(null);
        assertNotNull(mav, "应包含 mavlink-drone 数据卡");
        assertEquals(80, mav.capacity, "容量应来自 BATTERY_STATUS remaining=80");
        assertTrue(mav.valid, "有效电池遥测生成有效数据卡");
    }

    @Test
    @DisplayName("MAVLink 链路连接但无电池遥测：生成说明卡而非无意义数据")
    void testMavlinkNoBatteryNoteCard() throws Exception {
        Drone fresh = new Drone();          // 未喂任何电池遥测
        BatteryChargingMonitor mon = new BatteryChargingMonitor(fresh, activeComm(), false);
        Snapshot s = mon.snapshot();

        assertTrue(s.mavlinkConnected);
        BatteryInfo mav = s.batteries.stream()
                .filter(b -> "mavlink-drone".equals(b.id))
                .findFirst().orElse(null);
        assertNotNull(mav, "链路连接时应出现占位说明卡");
        assertFalse(mav.valid, "无真实电池遥测时数据卡应标记为无效");
        assertNotNull(mav.note);
        assertTrue(mav.note.contains("未上报") || mav.note.contains("固件"),
                "说明卡应提示固件未上报电池遥测");
        assertFalse(s.mavlinkBatterySupported, "固件未上报 BATTERY_STATUS 时应为 false");
    }

    @Test
    @DisplayName("MAVLink 电压阈值按电芯数推导：2 电芯 7.2V 不应误报过压")
    void testMavlinkCardUsesCellDerivedMaxVoltage() throws Exception {
        BatteryChargingMonitor mon = new BatteryChargingMonitor(validBatteryDrone(), activeComm(), false);
        Snapshot s = mon.snapshot();

        BatteryInfo mav = s.batteries.stream()
                .filter(b -> "mavlink-drone".equals(b.id))
                .findFirst().orElse(null);
        assertNotNull(mav);
        // 电芯 3600mV×2=7.2V，maxVoltage=2×4.2=8.4，7.2V 不应触发 over_voltage
        assertEquals(7.2, mav.voltage, 1e-9);
        assertTrue(s.anomalies.stream().noneMatch(a -> "over_voltage".equals(a.code)),
                "电芯推导阈值下 7.2V 不应误报过压");
    }

    @Test
    @DisplayName("同一监控快照多次生成内容一致，验证不可变语义稳定")
    void testSnapshotConsistentOverCalls() throws Exception {
        BatteryChargingMonitor mon = new BatteryChargingMonitor(validBatteryDrone(), activeComm(), false);
        Snapshot s1 = mon.snapshot();
        Snapshot s2 = mon.snapshot();

        assertEquals(s1.batteries.size(), s2.batteries.size(), "两次快照电池卡数量应一致");
        for (int i = 0; i < s1.batteries.size(); i++) {
            BatteryInfo a = s1.batteries.get(i);
            BatteryInfo b = s2.batteries.get(i);
            assertEquals(a.id, b.id);
            assertEquals(a.valid, b.valid);
            assertEquals(a.capacity, b.capacity, 1e-9);
            assertEquals(a.voltage, b.voltage, 1e-9);
            assertEquals(a.charging, b.charging);
        }
    }
}