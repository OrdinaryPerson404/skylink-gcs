package cn.edu.nuaa.gcs.model;

import cn.edu.nuaa.gcs.comm.MavlinkParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Drone 数据模型单元测试。
 *
 * 直接构造 MavlinkParser.Telemetry 对象（new + 手动置 has* 标志与值）驱动
 * Drone.updateFromTelemetry，验证"仅有效数据才落库"的电池翻译修复：
 * - 哨兵 BATTERY_STATUS（batteryValid=false）不再污染可用性标识与字段；
 * - 有效 BATTERY_STATUS 会回填主网格电池（CF-Drone 无 SYS_STATUS 场景）；
 * - SYS_STATUS / GPS_RAW_INT 等来源映射及各可用性标识语义。
 *
 * 注：Drone 的 JavaFX Property 可直接读写，无需启动 JavaFX 运行时。
 */
class DroneTest {

    /** 构造一个携带有效 BATTERY_STATUS 的 Telemetry。 */
    private MavlinkParser.Telemetry validBattery(double current, double temp, int remaining, int cell1mv, int cell2mv) {
        MavlinkParser.Telemetry t = new MavlinkParser.Telemetry();
        t.valid = true;
        t.hasBatteryStatus = true;
        t.batteryValid = true;
        t.batteryCurrent = current;
        t.batteryTemp = temp;
        t.batteryRemaining2 = remaining;
        t.cellVoltages[0] = cell1mv;
        t.cellVoltages[1] = cell2mv;
        return t;
    }

    /** 构造一个携带哨兵 BATTERY_STATUS（CF-Drone 无电池传感器）的 Telemetry。 */
    private MavlinkParser.Telemetry sentinelBattery() {
        MavlinkParser.Telemetry t = new MavlinkParser.Telemetry();
        t.valid = true;
        t.hasBatteryStatus = true;
        t.batteryValid = false;
        t.batteryRemaining2 = -1;
        t.batteryCurrent = 0.0;   // 电芯保持全 0（默认）
        return t;
    }

    @Test
    @DisplayName("心跳映射到模型")
    void testHeartbeatMapsToModel() {
        MavlinkParser.Telemetry t = new MavlinkParser.Telemetry();
        t.valid = true;
        t.hasHeartbeat = true;
        t.customMode = 2;          // STAB
        t.systemStatus = 4;        // ACTIVE
        t.armed = false;

        Drone d = new Drone();
        d.updateFromTelemetry(t);

        assertEquals(2, d.getCustomMode());
        assertEquals(4, d.getSystemStatus());
        assertEquals("STAB", d.getCustomModeName());
        assertTrue(d.isHeartbeatAvailable());
    }

    @Test
    @DisplayName("姿态映射到模型")
    void testAttitudeMapsToModel() {
        MavlinkParser.Telemetry t = new MavlinkParser.Telemetry();
        t.valid = true;
        t.hasAttitude = true;
        t.roll = 0.1;
        t.pitch = -0.2;
        t.yaw = 1.0;
        t.rollSpeed = 0.3;
        t.pitchSpeed = -0.4;
        t.yawSpeed = 0.5;

        Drone d = new Drone();
        d.updateFromTelemetry(t);

        assertEquals(0.1, d.getRoll(), 1e-9);
        assertEquals(-0.2, d.getPitch(), 1e-9);
        assertEquals(1.0, d.getYaw(), 1e-9);
        assertEquals(0.3, d.getRollSpeed(), 1e-9);
        assertEquals(-0.4, d.getPitchSpeed(), 1e-9);
        assertEquals(0.5, d.getYawSpeed(), 1e-9);
        assertTrue(d.isAttitudeAvailable());
    }

    @Test
    @DisplayName("位置映射到模型")
    void testPositionMapsToModel() {
        MavlinkParser.Telemetry t = new MavlinkParser.Telemetry();
        t.valid = true;
        t.hasPosition = true;
        t.lat = 32.0612;
        t.lon = 118.793;
        t.alt = 85.5;
        t.speed = 6.36;
        t.heading = 180.0;

        Drone d = new Drone();
        d.updateFromTelemetry(t);

        assertEquals(32.0612, d.getLat(), 1e-9);
        assertEquals(118.793, d.getLon(), 1e-9);
        assertEquals(85.5, d.getAlt(), 1e-9);
        assertEquals(6.36, d.getSpeed(), 1e-9);
        assertEquals(180.0, d.getHeading(), 1e-9);
        assertTrue(d.isPositionAvailable());
    }

    @Test
    @DisplayName("哨兵 BATTERY_STATUS 不落库")
    void testInvalidBatteryStatusNotStored() {
        Drone d = new Drone();
        d.updateFromTelemetry(sentinelBattery());

        assertFalse(d.isBatteryStatusAvailable(), "哨兵数据不应置 batteryStatusAvailable");
        assertFalse(d.isBatteryAvailable(), "哨兵数据不应置 batteryAvailable");
        assertFalse(d.isBatteryDataValid(), "哨兵数据不应视为电池有效");
        assertEquals(0, d.getBatteryCurrent(), 1e-9);
        assertEquals(0, d.getBatteryTemp(), 1e-9);
    }

    @Test
    @DisplayName("有效 BATTERY_STATUS 回填主网格电池")
    void testValidBatteryStatusBackfillsMainGrid() {
        Drone d = new Drone();
        d.updateFromTelemetry(validBattery(1.5, 30.0, 80, 3600, 3600));

        assertTrue(d.isBatteryStatusAvailable());
        assertTrue(d.isBatteryAvailable(), "有效 BATTERY_STATUS 应驱动主网格电池");
        assertTrue(d.isBatteryDataValid());
        assertEquals(80, d.getBatteryPct(), 1e-9);
        assertEquals(7.2, d.getVoltage(), 1e-9, "电芯和 3600+3600=7200mV 应作为总电压");
        assertEquals(1.5, d.getBatteryCurrent(), 1e-9);
        assertEquals(30.0, d.getBatteryTemp(), 1e-9);
    }

    @Test
    @DisplayName("哨兵值不覆盖既有有效电池数据")
    void testSentinelDoesNotOverwriteValid() {
        Drone d = new Drone();
        d.updateFromTelemetry(validBattery(1.5, 30.0, 80, 3600, 3600));
        // 再收到哨兵 BATTERY_STATUS
        d.updateFromTelemetry(sentinelBattery());

        assertEquals(1.5, d.getBatteryCurrent(), 1e-9, "有效旧值不应被哨兵覆盖");
        assertEquals(30.0, d.getBatteryTemp(), 1e-9);
        assertTrue(d.isBatteryStatusAvailable(), "有效旧值对应的可用性不应被清除");
        assertTrue(d.isBatteryDataValid());
    }

    @Test
    @DisplayName("SYS_STATUS 电池映射到模型")
    void testSysStatusBatteryMapsToModel() {
        MavlinkParser.Telemetry t = new MavlinkParser.Telemetry();
        t.valid = true;
        t.hasBattery = true;
        t.voltage = 11.25;
        t.batteryPct = 75;
        t.batteryCurrent = 1.2;

        Drone d = new Drone();
        d.updateFromTelemetry(t);

        assertTrue(d.isBatteryAvailable());
        assertEquals(11.25, d.getVoltage(), 1e-9);
        assertEquals(75, d.getBatteryPct(), 1e-9);
        assertEquals(1.2, d.getBatteryCurrent(), 1e-9);
    }

    @Test
    @DisplayName("GPS_RAW_INT 回填位置")
    void testGpsRawBackfillsPosition() {
        MavlinkParser.Telemetry t = new MavlinkParser.Telemetry();
        t.valid = true;
        t.hasGpsRaw = true;
        t.hasPosition = false;
        t.fixType = 3;            // DGPS
        t.lat = 32.0612;
        t.lon = 118.793;
        t.alt = 50.0;
        t.gpsSpeed = 6.0;
        t.gpsCog = 90.0;

        Drone d = new Drone();
        d.updateFromTelemetry(t);

        assertTrue(d.isGpsAvailable());
        assertTrue(d.isPositionAvailable(), "有效 GPS 定位应回填位置");
        assertEquals(32.0612, d.getLat(), 1e-9);
        assertEquals(118.793, d.getLon(), 1e-9);
        assertEquals(3, d.getFixType());
        assertEquals("DGPS", d.getFixTypeName());
    }

    @Test
    @DisplayName("断开后全部可用性标志清零")
    void testMarkDisconnected() {
        Drone d = new Drone();
        MavlinkParser.Telemetry t = new MavlinkParser.Telemetry();
        t.valid = true;
        t.hasHeartbeat = true;
        t.hasAttitude = true;
        t.hasPosition = true;
        t.hasBattery = true;
        d.updateFromTelemetry(t);

        assertTrue(d.isHeartbeatAvailable());
        assertTrue(d.isPositionAvailable());
        assertTrue(d.isBatteryAvailable());
        assertTrue(d.isAttitudeAvailable());

        d.markDisconnected();

        assertFalse(d.isHeartbeatAvailable());
        assertFalse(d.isPositionAvailable());
        assertFalse(d.isBatteryAvailable());
        assertFalse(d.isAttitudeAvailable());
        assertFalse(d.isArmed());
        assertEquals("DISCONNECTED", d.getStatus());
        assertFalse(d.isBatteryDataValid());
    }

    @Test
    @DisplayName("未置位标志保持 false")
    void testMissingFlagsRemainFalse() {
        MavlinkParser.Telemetry t = new MavlinkParser.Telemetry();
        t.valid = true;
        t.hasHeartbeat = true;

        Drone d = new Drone();
        d.updateFromTelemetry(t);

        assertTrue(d.isHeartbeatAvailable());
        assertFalse(d.isAttitudeAvailable());
        assertFalse(d.isImuAvailable());
        assertFalse(d.isPositionAvailable());
        assertFalse(d.isGpsAvailable());
        assertFalse(d.isBatteryAvailable());
        assertFalse(d.isBatteryStatusAvailable());
    }
}