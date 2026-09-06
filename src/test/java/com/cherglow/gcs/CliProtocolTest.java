package com.cherglow.gcs;

import com.cherglow.gcs.protocol.cli.CliProtocol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 黄金样本 = 2026-09-06 COM5 真机抓包（workspace/com5_dump.txt），禁止手改数值语义。
 */
class CliProtocolTest {

    private static final String STATUS_SAMPLE = """
            armed(解锁): 0
            controlSource(当前控制源): 无
            rcLinkUp(物理RC链路): 0  crsfLinkQuality(CRSF上行链路质量): -1
            webLinkUp(WebRC链路): 0
            mavLinkUp(MAVLink链路): 0
            loopRate(主循环频率Hz): 997  maxDt(近1秒内最大dt,s): 0.0021  maxDtEver(开机以来最大dt,s): 0.2774
            loopStage(各阶段近1秒最大耗时us,诊断报告第11节): readIMU=170 step=15 readRC=26 readWebRC=1289 consoleQ=1 readBaro=5 readRange=5 readMag=1 estimate=112 readFlow=3 flightSt=8 battery=798 armChk=9 control=177 sendMotor=58 input=41 mavlink=283 log=4 syncParam=43 led=12\s
            armingDisabled(禁止解锁原因): 电量低(3.31V) 禁止解锁
            phase(飞行阶段): DISARMED  isAirborne(离地): 0  landed(已降落): 1
            altState(定高子状态): GROUND_IDLE  thrustTarget(目标推力): 0.000  hoverThrust(悬停推力): 0.000
            batteryVoltage(电池瞬时电压V): 3.311  hoverVoltRef(悬停基准电压V): 0.000  voltComp(当前补偿系数): 1.000
            altitude(融合高度): nan  verticalVel(垂直速度): 0  baroAlt(气压高度): nan  rangeM(激光距离m): nan
            altWobbleRms(高度波动RMS估计m): 0.000
            accUpRms(垂直加速度RMS m/s^2): 0.000
            altMeasRaw(相对地面原始高度): nan  tofValidNow(ToF本帧有效): 0
            altSensorBlend(定高增益混合系数 1=ToF/0=气压计): 0.00
            altVelPID.integral(内环积分): 0.0000  altHoverTauEff(悬停学习时间常数s): 5.00
            altLandBleed(下降卡住推力衰减量): 0.000  accUp(垂直加速度m/s^2): 0.00  accUpSlow(判据用慢速值): 0.00
            landTimer(落地计时s): 0.00  aExpect(预期下沉加速度m/s^2): 0.00  wantClimb(=1否决)=0 thrAtIdle=0 nearGround=0 slowVert=0 supported=0
            tofStatus(激光状态 0=正常 -1=未就绪 -3=未初始化): -3  tofRawMM(原始毫米): 0
            sensors(传感器在线): baro(气压计)=0  range(激光测距)=0  hasAltitude(有高度源)=0
            mag(磁力计): 未检测到/不健康，偏航修正未生效
            """;

    @Test
    void parseStatus_golden() {
        CliProtocol.StatusData s = CliProtocol.parseStatus(STATUS_SAMPLE);
        assertNotNull(s);
        assertFalse(s.armed());
        assertEquals("无", s.controlSource());
        assertFalse(s.rcLinkUp());
        assertFalse(s.webLinkUp());
        assertFalse(s.mavLinkUp());
        assertEquals(997, s.loopRate());
        assertEquals("电量低(3.31V) 禁止解锁", s.armingDisabled());
        assertEquals("DISARMED", s.phase());
        assertFalse(s.isAirborne());
        assertTrue(s.landed());
        assertEquals(3.311, s.batteryVoltage(), 1e-9);
        assertNull(s.altitude()); // nan → 无高度源
        assertEquals(-3, s.tofStatus());
        assertFalse(s.baro());
        assertFalse(s.range());
        assertFalse(s.hasAltitude());
        assertFalse(s.magOk());
    }

    @Test
    void parseStatus_armingEnabledWhenEmpty() {
        CliProtocol.StatusData s = CliProtocol.parseStatus(
                "armed(解锁): 0\narmingDisabled(禁止解锁原因): \nphase(飞行阶段): DISARMED\n");
        assertNotNull(s);
        assertNull(s.armingDisabled());
    }

    @Test
    void parseEuler_golden() {
        CliProtocol.Euler e = CliProtocol.parseEuler("roll: 0.552018 pitch: 1.201174 yaw: -0.544528");
        assertNotNull(e);
        assertEquals(0.552018, e.rollDeg(), 1e-9);
        assertEquals(1.201174, e.pitchDeg(), 1e-9);
        assertEquals(-0.544528, e.yawDeg(), 1e-9);
    }

    @Test
    void parseQuat_golden() {
        CliProtocol.Quat q = CliProtocol.parseQuat("qw: 0.999934 qx: 0.002667 qy: 0.001839 qz: -0.011045");
        assertNotNull(q);
        assertEquals(0.999934, q.qw(), 1e-9);
        assertEquals(-0.011045, q.qz(), 1e-9);
    }

    @Test
    void parseMotors_golden() {
        CliProtocol.Motors m = CliProtocol.parseMotors("front-right 0 front-left 0 rear-right 0 rear-left 0");
        assertNotNull(m);
        assertEquals(0, m.frontRight(), 1e-9);
        assertEquals(0, m.rearLeft(), 1e-9);
    }

    @Test
    void parseRc_golden() {
        String sample = """
                channels(CH1-16): CH1=0 CH2=0 CH3=0 CH4=0 CH5=0 CH6=0 CH7=0 CH8=0 CH9=0 CH10=0 CH11=0 CH12=0 CH13=0 CH14=0 CH15=0 CH16=0\s
                通道映射(1-based):
                  Roll      未设置
                  Pitch     未设置
                  Yaw       未设置
                  Throttle  未设置
                  Mode      未设置
                roll: 0 pitch: 0 yaw: 0 throttle: 0 mode: nan
                phys(物理RC归一化,仲裁前): roll=nan pitch=nan yaw=nan throttle=nan mode=nan
                time: 0.0
                mode: STAB
                controlSource(当前控制源): 无
                """;
        CliProtocol.RcData r = CliProtocol.parseRc(sample);
        assertNotNull(r);
        assertEquals(16, r.channels().length);
        assertEquals(0, r.channels()[0]);
        assertEquals(0.0, r.roll(), 1e-9); // 真机归零时控制量为 0（phys 行才是 nan）
        assertEquals("STAB", r.modeName());
        assertEquals("无", r.controlSource());
        assertEquals(0.0, r.time(), 1e-9);
    }

    @Test
    void parseImu_golden() {
        String sample = """
                status: OK
                model: MPU-6500
                who am I: 0x70
                rate: 996
                gyro: 0.002744 0.000072 0.001684
                acc: -0.030895 0.061377 9.752141
                raw gyro: -0.086288 0.022371 -0.013849
                raw acc: 0.019154 -0.095770 10.515494
                gyro bias: -0.085151 0.020692 -0.012337
                accel bias: 0.079227 -0.089417 0.426173
                accel scale: 1.000019 0.999003 1.022202
                landed: 1
                """;
        CliProtocol.ImuData i = CliProtocol.parseImu(sample);
        assertNotNull(i);
        assertEquals("MPU-6500", i.model());
        assertEquals(9.752141, i.acc()[2], 1e-9);
        assertTrue(i.landed());
        assertEquals("OK", i.status());
    }

    @Test
    void parseParams_golden() {
        StringBuilder sb = new StringBuilder();
        sb.append("CTL_R_RATE_P = 0.06\nCTL_R_RATE_I = 0.1\nIMU_ROT_YAW = -1.5708\n");
        sb.append("RC_ROLL = nan\nMOT_THR_MIN = 0.1\nMOT_THR_MAX = 0.9\nWIFI_LOC_PORT = 14550\n");
        sb.append("ARM_CHK_LEVEL = 0\nBARO_LPF_HZ = 2\nFLOW_ROT_YAW = 0\nMAHONY_KP = 0.5\n");
        var params = CliProtocol.parseParams(sb.toString());
        assertNotNull(params);
        assertEquals(0.06, params.get("CTL_R_RATE_P"), 1e-9);
        assertEquals(-1.5708, params.get("IMU_ROT_YAW"), 1e-9);
        assertTrue(Double.isNaN(params.get("RC_ROLL")));
        assertEquals(14550.0, params.get("WIFI_LOC_PORT"), 1e-9);
        assertEquals(11, params.size());
    }

    @Test
    void parseWifi_golden() {
        String sample = """
                Mode: Access Point (AP)
                MAC: 68:09:47:58:9D:8D
                SSID: Drone_WiFi
                Password: ***
                Clients: 0
                IP: 192.168.4.1
                Remote IP: 255.255.255.255
                MAVLink connected: 0
                """;
        CliProtocol.WifiData w = CliProtocol.parseWifi(sample);
        assertNotNull(w);
        assertEquals("Access Point (AP)", w.mode());
        assertEquals("Drone_WiFi", w.ssid());
        assertEquals("192.168.4.1", w.ip());
        assertFalse(w.mavlinkConnected());
    }

    @Test
    void parseSys_golden() {
        String sample = "Chip: ESP32-D0WD-V3\nTemperature: 63.9 °C\nFree heap: 144996\n"
                + "loopRate(主循环频率Hz): 973  maxDt(近1秒内最大dt,s): 0.0257\n";
        CliProtocol.SysData s = CliProtocol.parseSys(sample);
        assertNotNull(s);
        assertEquals("ESP32-D0WD-V3", s.chip());
        assertEquals(63.9, s.temperatureC(), 1e-9);
        assertEquals(144996L, s.freeHeap());
        assertEquals(973, s.loopRate());
    }
}
