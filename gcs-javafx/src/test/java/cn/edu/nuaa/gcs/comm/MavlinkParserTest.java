package cn.edu.nuaa.gcs.comm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MAVLink v1 解析器单元测试
 * 覆盖：帧结构校验、4 类报文解码、CRC 校验、坏帧处理、多帧流解析
 */
class MavlinkParserTest {

    // ============================================================
    // 辅助方法：构造合法 MAVLink v1 帧（自动计算 CRC16-X25）
    // ============================================================

    private byte[] buildFrame(int sysId, int compId, int msgId, byte[] payload) {
        int payloadLen = payload.length;
        byte[] frame = new byte[payloadLen + 8];
        frame[0] = (byte) 0xFE;            // STX
        frame[1] = (byte) payloadLen;      // LEN
        frame[2] = 0;                      // SEQ
        frame[3] = (byte) sysId;           // SYSID
        frame[4] = (byte) compId;          // COMPID
        frame[5] = (byte) msgId;           // MSGID
        System.arraycopy(payload, 0, frame, 6, payloadLen);

        // 计算 CRC16-X25（从 LEN 到 PAYLOAD 末尾 + CRC_EXTRA）
        int crc = 0xFFFF;
        for (int i = 1; i <= payloadLen + 5; i++) {
            crc = crc16Step(crc, frame[i] & 0xFF);
        }
        int extra = getCrcExtra(msgId);
        if (extra >= 0) {
            crc = crc16Step(crc, extra);
        }
        frame[6 + payloadLen] = (byte) (crc & 0xFF);        // CK_A (low)
        frame[7 + payloadLen] = (byte) ((crc >> 8) & 0xFF); // CK_B (high)
        return frame;
    }

    private int crc16Step(int crc, int data) {
        crc ^= data;
        for (int i = 0; i < 8; i++) {
            if ((crc & 1) != 0)
                crc = (crc >>> 1) ^ 0x8408;
            else
                crc = crc >>> 1;
        }
        return crc;
    }

    private int getCrcExtra(int msgId) {
        return switch (msgId) {
            case 0   -> 50;   // HEARTBEAT
            case 1   -> 124;  // SYS_STATUS
            case 22  -> 220;  // PARAM_VALUE
            case 24  -> 87;   // GPS_RAW_INT
            case 26  -> 170;  // SCALED_IMU
            case 31  -> 246;  // ATTITUDE_QUATERNION
            case 33  -> 104;  // GLOBAL_POSITION_INT
            case 35  -> 244;  // RC_CHANNELS_RAW
            case 44  -> 221;  // MISSION_COUNT
            case 74  -> 20;   // VFR_HUD
            case 77  -> 143;  // COMMAND_ACK
            case 126 -> 220;  // SERIAL_CONTROL
            case 140 -> 181;  // ACTUATOR_CONTROL_TARGET
            case 147 -> 117;  // BATTERY_STATUS
            case 105 -> 97;   // HIGHRES_IMU
            case 245 -> 130;  // EXTENDED_SYS_STATE
            case 253 -> 83;   // STATUSTEXT
            default -> -1;
        };
    }

    /**
     * 构造合法标准 MAVLink v2 帧（0xFD 起始），用于测试 v2 协议解析。
     * 布局（c_library_v2 权威定义，与 CF-Drone 实测帧一致）：
     * STX(1) LEN(1) INCOMPAT(1) COMPAT(1) SEQ(1) SYSID(1) COMPID(1) MSGID(3) PAYLOAD(N) CRC(2)
     */
    private byte[] buildV2Frame(int sysId, int compId, int msgId, byte[] payload) {
        int payloadLen = payload.length;
        byte[] frame = new byte[12 + payloadLen];
        frame[0] = (byte) 0xFD;
        frame[1] = (byte) (payloadLen & 0xFF);  // LEN 为 1 字节
        frame[2] = 0;  // incompat flags
        frame[3] = 0;  // compat flags
        frame[4] = 0;  // seq
        frame[5] = (byte) sysId;
        frame[6] = (byte) compId;
        frame[7] = (byte) (msgId & 0xFF);
        frame[8] = (byte) ((msgId >> 8) & 0xFF);
        frame[9] = (byte) ((msgId >> 16) & 0xFF);
        System.arraycopy(payload, 0, frame, 10, payloadLen);
        // CRC 覆盖 LEN..MSGID（9 字节头）+ payload + CRC_EXTRA
        int crc = 0xFFFF;
        for (int i = 1; i <= 9 + payloadLen; i++) {
            crc = crc16Step(crc, frame[i] & 0xFF);
        }
        int extra = getCrcExtra(msgId);
        if (extra >= 0) crc = crc16Step(crc, extra);
        frame[10 + payloadLen] = (byte) (crc & 0xFF);
        frame[11 + payloadLen] = (byte) ((crc >> 8) & 0xFF);
        return frame;
    }

    // ============================================================
    // 1. 帧结构基础测试
    // ============================================================

    @Test
    @DisplayName("空数据或数据过短返回无效")
    void testTooShortData() {
        MavlinkParser.Telemetry t = MavlinkParser.parse(new byte[0], 0, 0);
        assertFalse(t.valid);

        byte[] shortData = new byte[]{(byte)0xFE, 0x01, 0x00, 0x01, 0x01, 0x00};
        t = MavlinkParser.parse(shortData, 0, shortData.length);
        assertFalse(t.valid);
    }

    @Test
    @DisplayName("错误起始字节被跳过")
    void testWrongStartByte() {
        byte[] data = new byte[]{0x00, 0x01, 0x02, (byte)0xFE, 0x00, 0x00, 0x01, 0x01, 0x00};
        // 前面都是垃圾字节，后面是一个空 payload 的 HEARTBEAT 帧头（无 CRC，长度不够）
        MavlinkParser.Telemetry t = MavlinkParser.parse(data, 0, data.length);
        // 不应崩溃，但因为帧不完整所以 valid=false
        assertFalse(t.valid);
    }

    // ============================================================
    // 2. HEARTBEAT (MSGID=0) 解析测试
    // ============================================================

    @Test
    @DisplayName("HEARTBEAT 报文解析 - system_status 字段")
    void testHeartbeat() {
        // HEARTBEAT payload (9 bytes) 布局：
        //   [0..3] custom_mode (uint32)
        //   [4]    type (uint8)
        //   [5]    autopilot (uint8)
        //   [6]    base_mode (uint8, bit7=已解锁)
        //   [7]    system_status (uint8, MAV_STATE)
        //   [8]    mavlink_version (uint8)
        byte[] payload = new byte[9];
        payload[0] = 2;    // type = QUADROTOR
        payload[1] = 12;   // autopilot = PX4
        payload[6] = 0;    // base_mode = 0（未解锁）
        payload[7] = 4;    // system_status = ACTIVE(4)

        byte[] frame = buildFrame(1, 1, 0, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid, "HEARTBEAT 帧应解析为有效");
        assertEquals(0, t.msgId);
        assertEquals(4, t.systemStatus, "system_status 应为 ACTIVE(4)");
        assertFalse(t.armed, "base_mode=0 不应识别为解锁");
    }

    // ============================================================
    // 3. SYS_STATUS (MSGID=1) 解析测试
    // ============================================================

    @Test
    @DisplayName("SYS_STATUS 报文解析 - 电压/电流/电量（标准偏移）")
    void testSysStatus() {
        // SYS_STATUS payload (31 bytes), 标准偏移:
        //   offset 8: voltage_battery (uint16_t, mV)
        //   offset 10: current_battery (int16_t, 10mA → /100A)
        //   offset 12: battery_remaining (uint8_t, %)
        byte[] payload = new byte[31];
        // voltage_battery = 11250 mV = 11.25 V → little-endian 0x2BF2
        payload[8] = (byte) 0xF2;
        payload[9] = (byte) 0x2B;
        // current_battery = 1500 (=15.00 A, 10mA 单位) → 0x05DC
        payload[10] = (byte) 0xDC;
        payload[11] = 0x05;
        // battery_remaining at offset 12 = 75%
        payload[12] = 75;

        byte[] frame = buildFrame(1, 1, 1, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid, "SYS_STATUS 帧应解析为有效");
        assertEquals(1, t.msgId);
        assertEquals(11.25, t.voltage, 0.01, "电压应为 11.25V");
        assertEquals(15.0, t.batteryCurrent, 0.01, "电流应为 15.0A");
        assertEquals(75, t.batteryPct, 0.01, "电量应为 75%");
        assertTrue(t.hasBattery, "hasBattery 应为 true");
    }

    // ============================================================
    // 4. GLOBAL_POSITION_INT (MSGID=33) 解析测试
    // ============================================================

    @Test
    @DisplayName("GLOBAL_POSITION_INT 报文解析 - 经纬度高度速度航向")
    void testGlobalPositionInt() {
        // 标准 GLOBAL_POSITION_INT payload (28 bytes):
        //   0: time_boot_ms (uint32)
        //   4: lat (int32, degE7)
        //   8: lon (int32, degE7)
        //  12: alt (int32, mm)
        //  16: relative_alt (int32, mm)
        //  20: vx (int16, cm/s)
        //  22: vy (int16, cm/s)
        //  24: vz (int16, cm/s)
        //  26: hdg (uint16, cdeg)
        byte[] payload = new byte[28];

        // time_boot_ms = 1000
        writeInt32(payload, 0, 1000);
        // lat = 32.0612 -> 320612000 (1e7)
        writeInt32(payload, 4, 320612000);
        // lon = 118.793 -> 1187930000
        writeInt32(payload, 8, 1187930000);
        // alt = 85.5m -> 85500 mm
        writeInt32(payload, 12, 85500);
        // relative_alt = 80.0m -> 80000 mm
        writeInt32(payload, 16, 80000);
        // vx = 5.5 m/s -> 550 cm/s
        writeInt16(payload, 20, 550);
        // vy = 3.2 m/s -> 320 cm/s
        writeInt16(payload, 22, 320);
        // vz = 0
        writeInt16(payload, 24, 0);
        // heading = 180.0° -> 18000 cdeg
        writeUint16(payload, 26, 18000);

        byte[] frame = buildFrame(1, 1, 33, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid, "GLOBAL_POSITION_INT 帧应解析为有效");
        assertEquals(33, t.msgId);
        assertEquals(32.0612, t.lat, 0.0001, "纬度应为 32.0612°");
        assertEquals(118.793, t.lon, 0.001, "经度应为 118.793°");
        assertEquals(85.5, t.alt, 0.01, "高度应为 85.5m");
        assertEquals(80.0, t.relAlt, 0.01, "相对高度应为 80.0m");

        // 速度 = sqrt(vx^2 + vy^2) = sqrt(5.5^2 + 3.2^2) = sqrt(30.25 + 10.24) = sqrt(40.49) ≈ 6.36
        assertEquals(6.36, t.speed, 0.1, "地速约 6.36 m/s");
        assertEquals(180.0, t.heading, 0.1, "航向应为 180°");
    }

    // ============================================================
    // 5. VFR_HUD (MSGID=74) 解析测试
    // ============================================================

    @Test
    @DisplayName("VFR_HUD 报文解析 - 地速/高度/航向/油门")
    void testVfrHud() {
        // 标准 VFR_HUD payload (20 bytes):
        //   0: airspeed (float32, m/s)
        //   4: groundspeed (float32, m/s)
        //   8: alt (float32, m)
        //  12: climb (float32, m/s)
        //  16: heading (int16, deg)
        //  18: throttle (uint16, %)
        byte[] payload = new byte[20];
        writeFloat(payload, 0, 7.2f);    // airspeed
        writeFloat(payload, 4, 8.5f);    // groundspeed
        writeFloat(payload, 8, 120.0f);  // alt
        writeFloat(payload, 12, 0.5f);   // climb
        writeInt16(payload, 16, 270);    // heading
        writeUint16(payload, 18, 65);    // throttle

        byte[] frame = buildFrame(1, 1, 74, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid, "VFR_HUD 帧应解析为有效");
        assertEquals(74, t.msgId);
        assertTrue(t.hasVfrHud, "VFR_HUD 标志应置位");
        assertEquals(8.5, t.vfrSpeed, 0.01, "地速应为 8.5 m/s");
        assertEquals(270, t.vfrHeading, 0.01, "航向应为 270°");
        assertEquals(120.0, t.vfrAlt, 0.01, "高度应为 120m");
        assertEquals(0.5, t.vfrClimb, 0.01, "爬升率应为 0.5 m/s");
        assertEquals(65, t.vfrThrottle, "油门应为 65%");
    }

    // ============================================================
    // 5b. GPS_RAW_INT (MSGID=24) 解析测试
    // ============================================================

    @Test
    @DisplayName("GPS_RAW_INT 报文解析 - 卫星/HDOP/定位类型/速度")
    void testGpsRawInt() {
        // 标准 GPS_RAW_INT payload (30 字节，线上布局按字段尺寸降序):
        //   0: time_usec (uint64)
        //   8: lat (int32, degE7)
        //  12: lon (int32, degE7)
        //  16: alt (int32, mm)
        //  20: eph (uint16, HDOP*100)
        //  22: epv (uint16, VDOP*100)
        //  24: vel (uint16, cm/s)
        //  26: cog (uint16, cdeg)
        //  28: fix_type (uint8)
        //  29: satellites_visible (uint8)
        byte[] payload = new byte[30];
        // lat = 32.0612 -> 320612000
        writeInt32(payload, 8, 320612000);
        // lon = 118.793 -> 1187930000
        writeInt32(payload, 12, 1187930000);
        // alt = 50.0m -> 50000 mm
        writeInt32(payload, 16, 50000);
        // eph = 1.2 -> 120
        writeUint16(payload, 20, 120);
        // vel = 6.0 m/s -> 600 cm/s
        writeUint16(payload, 24, 600);
        // cog = 90.0° -> 9000 cdeg
        writeUint16(payload, 26, 9000);
        // fix_type = 3 (DGPS)
        payload[28] = 3;
        // satellites = 14
        payload[29] = 14;

        byte[] frame = buildFrame(1, 1, 24, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid, "GPS_RAW_INT 帧应解析为有效");
        assertEquals(24, t.msgId);
        assertTrue(t.hasGpsRaw, "hasGpsRaw 应置位");
        assertEquals(3, t.fixType, "定位类型应为 3 (DGPS)");
        assertEquals(14, t.satellites, "卫星数应为 14");
        assertEquals(1.2, t.hdop, 0.01, "HDOP 应为 1.2");
        assertEquals(6.0, t.gpsSpeed, 0.01, "地面速度应为 6.0 m/s");
        assertEquals(90.0, t.gpsCog, 0.1, "航迹方向应为 90°");
        assertEquals(50.0, t.alt, 0.01, "高度应为 50m");
    }

    // ============================================================
    // 6. CRC 校验测试
    // ============================================================

    @Test
    @DisplayName("CRC 错误的帧被正确拒绝")
    void testBadCrc() {
        byte[] payload = new byte[9];
        payload[3] = 4; // system_status
        byte[] frame = buildFrame(1, 1, 0, payload);

        // 篡改最后一个 CRC 字节
        int lastIdx = frame.length - 1;
        frame[lastIdx] = (byte) (frame[lastIdx] ^ 0xFF);

        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);
        assertFalse(t.valid, "CRC 错误的帧应被拒绝");
    }

    @Test
    @DisplayName("payload 数据被篡改后 CRC 不通过")
    void testTamperedPayload() {
        byte[] payload = new byte[9];
        payload[3] = 4;
        byte[] frame = buildFrame(1, 1, 0, payload);

        // 篡改 payload 中的一个字节
        frame[9] = (byte) 0xFF;  // 修改 system_status 字段

        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);
        assertFalse(t.valid, "payload 被篡改的帧应被 CRC 拒绝");
    }

    // ============================================================
    // 7. 多帧流解析测试
    // ============================================================

    @Test
    @DisplayName("连续多帧流中解析到最后一帧")
    void testMultipleFrames() {
        // 构造 HEARTBEAT + SYS_STATUS 连续流
        byte[] hbPayload = new byte[9];
        hbPayload[3] = 4;
        byte[] hbFrame = buildFrame(1, 1, 0, hbPayload);

        byte[] ssPayload = new byte[31];
        // 按 MAVLink 标准偏移: voltage_battery@8 (uint16 mV), current_battery@10 (int16 cA), battery_remaining@12 (uint8 %)
        ssPayload[8] = (byte) 0xE1;       // voltage_battery low = 0x2BE1 = 11233 mV
        ssPayload[9] = (byte) 0x2B;       // voltage_battery high
        ssPayload[10] = (byte) 0x60;      // current_battery low  0x0060 = 96 cA = 0.96A
        ssPayload[11] = 0x00;             // current_battery high
        ssPayload[12] = 75;               // battery_remaining = 75%
        byte[] ssFrame = buildFrame(1, 1, 1, ssPayload);

        // 合并
        byte[] stream = new byte[hbFrame.length + ssFrame.length];
        System.arraycopy(hbFrame, 0, stream, 0, hbFrame.length);
        System.arraycopy(ssFrame, 0, stream, hbFrame.length, ssFrame.length);

        MavlinkParser.Telemetry t = MavlinkParser.parse(stream, 0, stream.length);
        // parse 返回最后一帧的解析结果
        assertTrue(t.valid);
        assertEquals(1, t.msgId, "连续流解析应返回最后一帧（SYS_STATUS）");
        assertTrue(t.hasBattery, "应标记 hasBattery=true");
        assertEquals(75, t.batteryPct, 0.01);
        assertEquals(11.233, t.voltage, 0.01);
        assertEquals(0.96, t.batteryCurrent, 0.01);
    }

    @Test
    @DisplayName("帧间存在垃圾字节仍能正确同步")
    void testGarbageBetweenFrames() {
        byte[] hbPayload = new byte[9];
        hbPayload[3] = 4;
        byte[] hbFrame = buildFrame(1, 1, 0, hbPayload);

        byte[] ssPayload = new byte[31];
        ssPayload[10] = 80;
        byte[] ssFrame = buildFrame(1, 1, 1, ssPayload);

        // 在两帧之间插入 5 字节垃圾
        byte[] garbage = new byte[]{0x01, 0x02, (byte)0xFE, 0x03, 0x04};
        byte[] stream = new byte[hbFrame.length + garbage.length + ssFrame.length];
        System.arraycopy(hbFrame, 0, stream, 0, hbFrame.length);
        System.arraycopy(garbage, 0, stream, hbFrame.length, garbage.length);
        System.arraycopy(ssFrame, 0, stream, hbFrame.length + garbage.length, ssFrame.length);

        MavlinkParser.Telemetry t = MavlinkParser.parse(stream, 0, stream.length);
        assertTrue(t.valid, "帧间垃圾不应影响正确帧的解析");
        assertEquals(1, t.msgId);
    }

    // ============================================================
    // 8. 边界条件测试
    // ============================================================

    @Test
    @DisplayName("零长度 payload 帧")
    void testZeroPayload() {
        // 构造一个零负载的帧（非标准报文，仅测试边界）
        byte[] payload = new byte[0];
        byte[] frame = buildFrame(1, 1, 0, payload); // 注意: HEARTBEAT 实际长度是 9
        // 由于 payload 长度为 0 但 msgId=0 的 CRC_EXTRA=50，CRC 计算仍然正确
        // 但解析时 decodeMessage 会访问 payload[6]，导致 ArrayIndexOutOfBoundsException
        // 这个测试验证解析器在越界时是否健壮

        // 当前代码没有长度保护，会越界。这是一个已知问题，测试用例用于标记。
        // 实际工程中应添加 payload 长度校验。
        assertDoesNotThrow(() -> {
            MavlinkParser.parse(frame, 0, frame.length);
        }, "零长度 payload 不应导致解析器崩溃");
    }

    @Test
    @DisplayName("负数经纬度解析（南半球/西半球）")
    void testNegativeLatLon() {
        byte[] payload = new byte[28];
        // 标准 GLOBAL_POSITION_INT 偏移：lat@4, lon@8, alt@12
        // lat = -34.5 -> -345000000 (degE7)
        writeInt32(payload, 4, -345000000);
        // lon = -58.0 -> -580000000 (degE7)
        writeInt32(payload, 8, -580000000);
        // alt = 100m (offset 12)
        writeInt32(payload, 12, 100000);

        byte[] frame = buildFrame(1, 1, 33, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid);
        assertEquals(-34.5, t.lat, 0.001, "南纬应为负值");
        assertEquals(-58.0, t.lon, 0.001, "西经应为负值");
    }

    // ============================================================
    // 9. ATTITUDE_QUATERNION (MSGID=31) 解析测试
    // ============================================================

    @Test
    @DisplayName("ATTITUDE_QUATERNION 解析 - 四元数转欧拉角")
    void testAttitudeQuaternion() {
        // ATTITUDE_QUATERNION payload (32 bytes):
        //   0: time_boot_ms (uint32)
        //   4: q1=w (float)
        //   8: q2=x (float)
        //  12: q3=y (float)
        //  16: q4=z (float)
        //  20: rollspeed (float)
        //  24: pitchspeed (float)
        //  28: yawspeed (float)
        byte[] payload = new byte[32];
        // 单位四元数 w=1, x=y=z=0 → roll=pitch=yaw=0
        writeFloat(payload, 4, 1.0f);
        writeFloat(payload, 8, 0.0f);
        writeFloat(payload, 12, 0.0f);
        writeFloat(payload, 16, 0.0f);
        writeFloat(payload, 20, 0.1f);   // rollspeed
        writeFloat(payload, 24, 0.2f);   // pitchspeed
        writeFloat(payload, 28, 0.3f);    // yawspeed

        byte[] frame = buildFrame(1, 1, 31, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid && t.hasAttitude, "ATTITUDE_QUATERNION 应置 hasAttitude");
        assertEquals(0.0, t.roll, 0.001, "单位四元数 roll=0");
        assertEquals(0.0, t.pitch, 0.001, "单位四元数 pitch=0");
        assertEquals(0.0, t.yaw, 0.001, "单位四元数 yaw=0");
        assertEquals(0.1, t.rollSpeed, 0.001);
        assertEquals(0.3, t.yawSpeed, 0.001);
    }

    @Test
    @DisplayName("ATTITUDE_QUATERNION - 90° 偏航四元数")
    void testAttitudeYaw90() {
        // 绕 Z 轴 90°: w=cos(45°)=0.7071, z=sin(45°)=0.7071
        byte[] payload = new byte[32];
        writeFloat(payload, 4, 0.70710678f);
        writeFloat(payload, 8, 0.0f);
        writeFloat(payload, 12, 0.0f);
        writeFloat(payload, 16, 0.70710678f);

        byte[] frame = buildFrame(1, 1, 31, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.hasAttitude);
        assertEquals(Math.PI / 2, t.yaw, 0.001, "yaw 应为 90°=π/2 弧度");
    }

    // ============================================================
    // 10. SCALED_IMU (MSGID=26) 解析测试
    // ============================================================

    @Test
    @DisplayName("SCALED_IMU 解析 - 加速度/角速度/温度")
    void testScaledImu() {
        // SCALED_IMU payload:
        //   0: time_boot_ms (uint32)
        //   4: xacc (int16, milli-g)
        //   6: yacc
        //   8: zacc
        //  10: xgyro (int16, milli-rad/s)
        //  12: ygyro
        //  14: zgyro
        //  22: temperature (int16, centi-degC)
        byte[] payload = new byte[24];
        // xacc = 1000 milli-g = 1 g = 9.80665 m/s²
        writeInt16(payload, 4, 1000);
        // yacc = -500 milli-g = -4.903 m/s²
        writeInt16(payload, 6, -500);
        // zacc = 2000 milli-g = 19.61 m/s²
        writeInt16(payload, 8, 2000);
        // xgyro = 500 milli-rad/s = 0.5 rad/s
        writeInt16(payload, 10, 500);
        // temperature = 25.50°C = 2550 centi-degC
        writeInt16(payload, 22, 2550);

        byte[] frame = buildFrame(1, 1, 26, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid && t.hasImu);
        assertEquals(9.80665, t.accX, 0.01, "accX 应为 1g");
        assertEquals(-4.903, t.accY, 0.01);
        assertEquals(19.6133, t.accZ, 0.01);
        assertEquals(0.5, t.gyroX, 0.001);
        assertEquals(25.5, t.imuTemp, 0.01, "IMU 温度应为 25.5°C");
    }

    // ============================================================
    // 11. RC_CHANNELS_RAW (MSGID=35) 解析测试
    // ============================================================

    @Test
    @DisplayName("RC_CHANNELS_RAW 解析 - 8 通道与 RSSI（标准 21 字节布局）")
    void testRcChannelsRaw() {
        // RC_CHANNELS_RAW payload (21 bytes), 标准布局:
        //   0: time_boot_ms (uint32)
        //   4-19: chan1..chan8 (uint16 each)
        //  20: rssi (uint8)
        byte[] payload = new byte[21];
        // 8 通道值
        int[] chans = {1500, 1600, 1700, 1800, 1900, 2000, 1000, 1100};
        for (int i = 0; i < 8; i++) writeUint16(payload, 4 + i * 2, chans[i]);
        payload[20] = 80;  // rssi

        byte[] frame = buildFrame(1, 1, 35, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid && t.hasRc);
        for (int i = 0; i < 8; i++) {
            assertEquals(chans[i], t.rcChannels[i], "通道 " + (i + 1) + " 值应匹配");
        }
        assertEquals(80, t.rssi, "RSSI 应为 80");
    }

    // ============================================================
    // 12. ACTUATOR_CONTROL_TARGET (MSGID=140) 解析测试
    // ============================================================

    @Test
    @DisplayName("ACTUATOR_CONTROL_TARGET 解析 - 8 电机输出")
    void testActuatorControlTarget() {
        // payload (40 bytes):
        //   0: time_usec (uint64)
        //   8-39: controls[8] (float each)
        byte[] payload = new byte[40];
        float[] motors = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f};
        for (int i = 0; i < 8; i++) writeFloat(payload, 8 + i * 4, motors[i]);

        byte[] frame = buildFrame(1, 1, 140, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid && t.hasMotors);
        for (int i = 0; i < 8; i++) {
            assertEquals(motors[i], t.motorOutputs[i], 0.001, "电机 " + i + " 输出应匹配");
        }
    }

    // ============================================================
    // 13. EXTENDED_SYS_STATE (MSGID=245) 解析测试
    // ============================================================

    @Test
    @DisplayName("EXTENDED_SYS_STATE 解析 - 落地状态")
    void testExtendedSysState() {
        byte[] payload = new byte[2];
        payload[0] = 0;   // vtol_state
        payload[1] = 2;   // landed_state = IN-AIR

        byte[] frame = buildFrame(1, 1, 245, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid && t.hasLandedState);
        assertEquals(2, t.landedState, "landed_state=2 表示在空中");
    }

    // ============================================================
    // 14. PARAM_VALUE (MSGID=22) 解析测试
    // ============================================================

    @Test
    @DisplayName("PARAM_VALUE 解析 - 参数名/值/索引/总数")
    void testParamValue() {
        // PARAM_VALUE payload (25 bytes):
        //   0: param_value (float)
        //   4: param_count (uint16)
        //   6: param_index (uint16)
        //   8: param_id (char[16])
        //  24: param_type (uint8)
        byte[] payload = new byte[25];
        writeFloat(payload, 0, 1.5f);     // param_value
        writeUint16(payload, 4, 84);       // param_count (CF-Drone 共 84 参数)
        writeUint16(payload, 6, 3);        // param_index
        // param_id = "MOT_THR_LVL"（11 字符 + 5 个 \0）
        String name = "MOT_THR_LVL";
        byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(nameBytes, 0, payload, 8, nameBytes.length);
        payload[24] = 9;  // param_type = MAV_PARAM_TYPE_REAL32

        byte[] frame = buildFrame(1, 1, 22, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid && t.hasParam);
        assertEquals(1.5, t.paramValue, 0.001);
        assertEquals(84, t.paramCount);
        assertEquals(3, t.paramIndex);
        assertEquals("MOT_THR_LVL", t.paramName, "参数名应去除 NULL 后保留");
        assertEquals(9, t.paramType);
    }

    // ============================================================
    // 15. COMMAND_ACK (MSGID=77) 解析测试
    // ============================================================

    @Test
    @DisplayName("COMMAND_ACK 解析 - 命令 ID 与结果")
    void testCommandAck() {
        // COMMAND_ACK payload (3 bytes):
        //   0: command (uint16)
        //   2: result (uint8)
        byte[] payload = new byte[3];
        writeUint16(payload, 0, 400);  // CMD_COMPONENT_ARM_DISARM
        payload[2] = 0;  // ACCEPTED

        byte[] frame = buildFrame(1, 1, 77, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid && t.hasAck);
        assertEquals(400, t.ackCommand);
        assertEquals(0, t.ackResult, "0=ACCEPTED");
    }

    // ============================================================
    // 16. SERIAL_CONTROL (MSGID=126) 解析测试 - Shell 输出
    // ============================================================

    @Test
    @DisplayName("SERIAL_CONTROL 解析 - Shell 文本回传")
    void testSerialControl() {
        // SERIAL_CONTROL payload (≥9 bytes):
        //   0: device (uint8)
        //   1: flags (uint8)
        //   2: timeout (uint16)
        //   4: baudrate (uint32)
        //   8: count (uint8)
        //   9: data[70]
        String text = "OK\r\n";
        byte[] payload = new byte[9 + text.length()];
        payload[8] = (byte) text.length();  // count
        byte[] textBytes = text.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(textBytes, 0, payload, 9, textBytes.length);

        byte[] frame = buildFrame(1, 1, 126, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid && t.hasShell);
        assertEquals("OK\r\n", t.shellText);
    }

    // ============================================================
    // 17. MISSION_COUNT (MSGID=44) 解析测试
    // ============================================================

    @Test
    @DisplayName("MISSION_COUNT 解析 - 任务数（CF-Drone 恒为 0）")
    void testMissionCount() {
        byte[] payload = new byte[2];
        writeUint16(payload, 0, 0);  // CF-Drone 恒返回 0

        byte[] frame = buildFrame(1, 1, 44, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid && t.hasMissionCount);
        assertEquals(0, t.missionCount, "CF-Drone 任务数恒为 0");
    }

    // ============================================================
    // 18. MAVLink v2 帧解析测试（0xFD 起始）
    // ============================================================

    @Test
    @DisplayName("v2 帧解析 - HEARTBEAT 通过 0xFD 起始字节")
    void testV2Heartbeat() {
        byte[] payload = new byte[9];
        payload[0] = 2;
        payload[6] = (byte) 0x80;  // base_mode bit7 = MAV_MODE_FLAG_SAFETY_ARMED
        payload[7] = 4;    // system_status = ACTIVE

        byte[] frame = buildV2Frame(1, 1, 0, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid, "v2 帧应解析为有效");
        assertEquals(0, t.msgId);
        assertTrue(t.armed, "bit7=1 表示已解锁");
        assertEquals(4, t.systemStatus);
    }

    @Test
    @DisplayName("v2 帧解析 - ATTITUDE_QUATERNION")
    void testV2Attitude() {
        byte[] payload = new byte[32];
        writeFloat(payload, 4, 1.0f);
        writeFloat(payload, 8, 0.0f);
        writeFloat(payload, 12, 0.0f);
        writeFloat(payload, 16, 0.0f);

        byte[] frame = buildV2Frame(1, 1, 31, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid);
        assertEquals(31, t.msgId);
        assertTrue(t.hasAttitude);
        assertEquals(0.0, t.roll, 0.001);
    }

    @Test
    @DisplayName("v1 + v2 混合流解析")
    void testMixedV1V2Stream() {
        // v1 HEARTBEAT
        byte[] hb1Payload = new byte[9];
        hb1Payload[7] = 4;
        byte[] v1Frame = buildFrame(1, 1, 0, hb1Payload);

        // v2 ATTITUDE_QUATERNION
        byte[] attPayload = new byte[32];
        writeFloat(attPayload, 4, 1.0f);
        byte[] v2Frame = buildV2Frame(1, 1, 31, attPayload);

        byte[] stream = new byte[v1Frame.length + v2Frame.length];
        System.arraycopy(v1Frame, 0, stream, 0, v1Frame.length);
        System.arraycopy(v2Frame, 0, stream, v1Frame.length, v2Frame.length);

        MavlinkParser.Telemetry t = MavlinkParser.parse(stream, 0, stream.length);
        assertTrue(t.valid, "混合流最后一帧应有效");
        assertEquals(31, t.msgId, "最后一帧为 v2 ATTITUDE_QUATERNION");
        assertTrue(t.hasAttitude);
    }

    // ============================================================
    // 辅助方法：小端字节写入
    // ============================================================

    private void writeInt16_2(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    private void writeInt32_2(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buf[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buf[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    private void writeFloat2(byte[] buf, int offset, float value) {
        int bits = Float.floatToIntBits(value);
        buf[offset] = (byte) (bits & 0xFF);
        buf[offset + 1] = (byte) ((bits >> 8) & 0xFF);
        buf[offset + 2] = (byte) ((bits >> 16) & 0xFF);
        buf[offset + 3] = (byte) ((bits >> 24) & 0xFF);
    }

    // ============================================================
    // 14. BATTERY_STATUS (MSGID=147) 解析测试
    // ============================================================

    @Test
    @DisplayName("BATTERY_STATUS 报文解析 - 电芯/电流/温度/容量（标准 common.xml 布局）")
    void testBatteryStatus() {
        // BATTERY_STATUS payload (31 bytes), 标准布局:
        //   0: battery_function (u8)
        //   1: type (u8)
        //   2: temperature (int16, centi-°C)
        //   4: voltages[10] (uint16, mV)
        //  24: current_battery (int16, cA)
        //  26: consumed_capacity (uint32, mAh)
        //  30: energy_remaining (int8, %)
        byte[] payload = new byte[31];
        payload[0] = 0;   // battery_function
        payload[1] = 1;   // type (LIPO)
        // temperature@2 = 2525 centi-°C → 25.25°C
        writeInt16(payload, 2, 2525);
        // voltages[0]@4 = 4200 mV, voltages[1]@6 = 4195 mV
        writeUint16(payload, 4, 4200);
        writeUint16(payload, 6, 4195);
        // current_battery@24 = 150 cA → 1.50 A
        writeInt16(payload, 24, 150);
        // consumed_capacity@26 = 500 mAh (uint32)
        writeInt32(payload, 26, 500);
        // energy_remaining@30 = 80%
        payload[30] = 80;

        byte[] frame = buildFrame(1, 1, 147, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid, "BATTERY_STATUS 帧应解析为有效");
        assertEquals(147, t.msgId);
        assertTrue(t.hasBatteryStatus);
        assertEquals(25.25, t.batteryTemp, 0.01, "电池温度应为 25.25°C");
        assertEquals(4200, t.cellVoltages[0], "电芯1应为 4200mV");
        assertEquals(4195, t.cellVoltages[1], "电芯2应为 4195mV");
        assertEquals(1.50, t.batteryCurrent, 0.01, "电流应为 1.50A");
        assertEquals(500, t.capacityConsumed, 1, "已耗容量应为 500mAh");
        assertEquals(80, t.batteryRemaining2, "剩余电量应为 80%");
    }

    @Test
    @DisplayName("BATTERY_STATUS v2 帧解析（标准布局 + 截断容错）")
    void testV2BatteryStatus() {
        byte[] payload = new byte[31];
        writeInt16(payload, 2, 3000);  // 30.00°C
        writeUint16(payload, 4, 3900); // 3900mV
        writeInt16(payload, 24, 200);  // 2.00A
        payload[30] = 60;              // 60%

        byte[] frame = buildV2Frame(1, 1, 147, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid);
        assertEquals(147, t.msgId);
        assertTrue(t.hasBatteryStatus);
        assertEquals(30.0, t.batteryTemp, 0.01);
        assertEquals(3900, t.cellVoltages[0]);
        assertEquals(2.0, t.batteryCurrent, 0.01);
        assertEquals(60, t.batteryRemaining2);
    }

    @Test
    @DisplayName("BATTERY_STATUS v2 截断帧（仅温度+电压，缺电流/容量/剩余）")
    void testV2BatteryStatusTruncated() {
        // v2 载荷截断：尾部零字段被裁剪 → 只剩 8 字节（function+type+temp+voltages[0]）
        byte[] payload = new byte[8];
        writeInt16(payload, 2, 2850);  // 28.50°C
        writeUint16(payload, 4, 4100); // 4100mV

        byte[] frame = buildV2Frame(1, 1, 147, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid);
        assertTrue(t.hasBatteryStatus, "截断帧也应标记 hasBatteryStatus");
        assertEquals(28.5, t.batteryTemp, 0.01);
        assertEquals(4100, t.cellVoltages[0]);
        assertEquals(0.0, t.batteryCurrent, 0.001, "缺电流字段时应保持默认 0");
    }

    // ============================================================
    // 15. STATUSTEXT (MSGID=253) 解析测试
    // ============================================================

    @Test
    @DisplayName("STATUSTEXT 报文解析 - 严重级别与文本")
    void testStatustext() {
        byte[] payload = new byte[51];
        payload[0] = 4;  // severity = WARNING (MAV_SEVERITY_WARNING=4)
        String msg = "Low battery warning";
        byte[] msgBytes = msg.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(msgBytes, 0, payload, 1, Math.min(msgBytes.length, 50));

        byte[] frame = buildFrame(1, 1, 253, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid, "STATUSTEXT 帧应解析为有效");
        assertEquals(253, t.msgId);
        assertTrue(t.hasStatusText);
        assertEquals(4, t.statusSeverity, "严重级别应为 4 (WARNING)");
        assertEquals("Low battery warning", t.statusText, "文本内容应匹配");
    }

    // ============================================================
    // 16. HIGHRES_IMU (MSGID=105) 解析测试
    // ============================================================

    @Test
    @DisplayName("HIGHRES_IMU 报文解析 - 气压/气压高度/温度/磁场（标准 float 布局）")
    void testHighresImu() {
        // HIGHRES_IMU payload (67 bytes), 标准布局（float 字段组）:
        //   0: time_usec (uint64)
        //   8: xacc (float) ... 16: zacc, 20: xgyro, 24: ygyro, 28: zgyro
        //  32: xmag (float, mT)  36: ymag  40: zmag
        //  44: abs_pressure (float, hPa)
        //  48: diff_pressure
        //  52: pressure_alt (float, m)
        //  56: altitude
        //  60: temperature (float, °C)
        //  64: fields_updated (uint16)  66: id (uint8)
        byte[] payload = new byte[67];
        // xmag@32 = 100 mT, ymag@36 = 50 mT, zmag@40 = -30 mT
        writeFloat(payload, 32, 100.0f);
        writeFloat(payload, 36, 50.0f);
        writeFloat(payload, 40, -30.0f);
        // abs_pressure@44 = 1013.25 hPa
        writeFloat(payload, 44, 1013.25f);
        // pressure_alt@52 = 100.0 m
        writeFloat(payload, 52, 100.0f);
        // temperature@60 = 25.5°C
        writeFloat(payload, 60, 25.5f);

        byte[] frame = buildFrame(1, 1, 105, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid, "HIGHRES_IMU 帧应解析为有效");
        assertEquals(105, t.msgId);
        assertTrue(t.hasHighresImu);
        assertEquals(100.0, t.magX, 0.0001, "magX 应为 100 mT");
        assertEquals(50.0, t.magY, 0.0001, "magY 应为 50 mT");
        assertEquals(-30.0, t.magZ, 0.0001, "magZ 应为 -30 mT");
        assertEquals(1013.25, t.absPressure, 0.01, "绝对气压应为 1013.25 hPa");
        assertEquals(100.0, t.pressureAlt, 0.01, "气压高度应为 100.0 m");
        assertEquals(25.5, t.imuTemp2, 0.01, "IMU温度应为 25.5°C");
    }

    // ============================================================
    // 19. 真实飞控字节回归测试（2026-09-03 COM5@115200 实测捕获）
    //     使用 lenient 模式（跳过 CRC）验证帧布局修复：
    //     LEN 1 字节、MSGID@[7..9]、payload@[10]
    //     注：字节从终端捕获，CRC 可能因转录误差不匹配，故用 lenient 模式
    // ============================================================

    @Test
    @DisplayName("真实帧回归 - CF-Drone ATTITUDE_QUATERNION(31) 布局验证")
    void testRealDroneAttitudeFrame() {
        byte[] frame = hexToBytes(
            "FD 20 00 00 03 01 01 1F 00 00 74 49 0F 00 8E 0D"
          + "2E 3F F4 CB 6F 3D 0B A9 36 BF 4C 96 22 3E 00 86"
          + "9E 3A F7 25 0D BB F2 30 12 39 DF 88");

        MavlinkParser parser = new MavlinkParser();
        parser.setLenient(true);
        MavlinkParser.Telemetry t = parser.parseInstance(frame, 0, frame.length);
        assertTrue(t.valid, "lenient 模式下帧应被解码");
        assertEquals(31, t.msgId, "MSGID 应为 31 (ATTITUDE_QUATERNION)");
        assertEquals(1, t.sysId, "SYSID 应为 1");
        assertEquals(1, t.compId, "COMPID 应为 1");
        assertTrue(t.hasAttitude, "应标记 hasAttitude");
    }

    @Test
    @DisplayName("真实帧回归 - CF-Drone ACTUATOR_CONTROL_TARGET(140) 布局验证")
    void testRealDroneActuatorFrame() {
        // 52 字节 = 10头 + 40载荷(time_usec u64 + controls[8] float) + 2CRC
        byte[] frame = hexToBytes(
            "FD 28 00 00 05 01 01 8C 00 00 74 49 0F 00 00 00"
          + "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
          + "00 00 00 00 00 00 A6 3E 08 40 F0 81 FB 3F 1A D2"
          + "08 40 B6 40");

        MavlinkParser parser = new MavlinkParser();
        parser.setLenient(true);
        MavlinkParser.Telemetry t = parser.parseInstance(frame, 0, frame.length);
        assertTrue(t.valid, "lenient 模式下帧应被解码");
        assertEquals(140, t.msgId, "MSGID 应为 140");
        assertTrue(t.hasMotors);
    }

    @Test
    @DisplayName("真实帧回归 - CF-Drone SCALED_IMU(26) 布局验证")
    void testRealDroneScaledImuFrame() {
        byte[] frame = hexToBytes(
            "FD 10 00 00 06 01 01 1A 00 00 74 49 0F 00 22 FC"
          + "94 00 1B 00 01 00 FF FF FF FF 9E 06");

        MavlinkParser parser = new MavlinkParser();
        parser.setLenient(true);
        MavlinkParser.Telemetry t = parser.parseInstance(frame, 0, frame.length);
        assertTrue(t.valid, "lenient 模式下帧应被解码");
        assertEquals(26, t.msgId, "MSGID 应为 26 (SCALED_IMU)");
        assertTrue(t.hasImu);
    }

    @Test
    @DisplayName("真实帧回归 - 三帧连续流 feed 全部解析")
    void testRealDroneStreamFeed() {
        byte[] attitude = hexToBytes(
            "FD 20 00 00 03 01 01 1F 00 00 74 49 0F 00 8E 0D"
          + "2E 3F F4 CB 6F 3D 0B A9 36 BF 4C 96 22 3E 00 86"
          + "9E 3A F7 25 0D BB F2 30 12 39 DF 88");
        byte[] actuator = hexToBytes(
            "FD 28 00 00 05 01 01 8C 00 00 74 49 0F 00 00 00"
          + "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
          + "00 00 00 00 00 00 A6 3E 08 40 F0 81 FB 3F 1A D2"
          + "08 40 B6 40");
        byte[] scaledImu = hexToBytes(
            "FD 10 00 00 06 01 01 1A 00 00 74 49 0F 00 22 FC"
          + "94 00 1B 00 01 00 FF FF FF FF 9E 06");

        byte[] stream = new byte[attitude.length + actuator.length + scaledImu.length];
        System.arraycopy(attitude, 0, stream, 0, attitude.length);
        System.arraycopy(actuator, 0, stream, attitude.length, actuator.length);
        System.arraycopy(scaledImu, 0, stream, attitude.length + actuator.length, scaledImu.length);

        MavlinkParser parser = new MavlinkParser();
        parser.setLenient(true);
        java.util.List<MavlinkParser.Telemetry> frames = parser.feed(stream, stream.length);
        assertEquals(3, frames.size(), "feed 应解析出全部 3 帧");
        assertEquals(31, frames.get(0).msgId);
        assertEquals(140, frames.get(1).msgId);
        assertEquals(26, frames.get(2).msgId);
    }

    @Test
    @DisplayName("feed 跨块帧拼接 - 帧被任意位置切断仍能完整解析")
    void testFeedSplitAcrossChunks() {
        byte[] actuator = hexToBytes(
            "FD 28 00 00 05 01 01 8C 00 00 74 49 0F 00 00 00"
          + "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
          + "00 00 00 00 00 00 A6 3E 08 40 F0 81 FB 3F 1A D2"
          + "08 40 B6 40");

        MavlinkParser parser = new MavlinkParser();
        parser.setLenient(true);
        int[] cuts = {8, 25, actuator.length};
        java.util.List<MavlinkParser.Telemetry> all = new java.util.ArrayList<>();
        int prev = 0;
        for (int cut : cuts) {
            byte[] chunk = new byte[cut - prev];
            System.arraycopy(actuator, prev, chunk, 0, chunk.length);
            all.addAll(parser.feed(chunk, chunk.length));
            prev = cut;
        }
        assertEquals(1, all.size(), "跨块帧拼接后应完整解析出 1 帧");
        assertEquals(140, all.get(0).msgId);
        assertTrue(all.get(0).hasMotors);
    }

    @Test
    @DisplayName("feed 严格模式 CRC 失败帧被丢弃")
    void testFeedStrictModeDropsBadCrc() {
        // 用 buildV2Frame 构造合法帧，再篡改 CRC
        byte[] payload = new byte[32];
        byte[] frame = buildV2Frame(1, 1, 31, payload);
        frame[frame.length - 1] ^= 0xFF;  // 破坏 CRC

        MavlinkParser parser = new MavlinkParser();  // 严格模式（默认）
        java.util.List<MavlinkParser.Telemetry> frames = parser.feed(frame, frame.length);
        assertTrue(frames.isEmpty(), "严格模式下 CRC 失败的帧应被丢弃");
    }

    @Test
    @DisplayName("feed lenient 模式 CRC 失败帧仍解码")
    void testFeedLenientModeKeepsBadCrc() {
        byte[] payload = new byte[32];
        byte[] frame = buildV2Frame(1, 1, 31, payload);
        frame[frame.length - 1] ^= 0xFF;  // 破坏 CRC

        MavlinkParser parser = new MavlinkParser();
        parser.setLenient(true);
        java.util.List<MavlinkParser.Telemetry> frames = parser.feed(frame, frame.length);
        assertEquals(1, frames.size(), "lenient 模式下 CRC 失败的帧仍应被解码");
        assertFalse(frames.get(0).crcValid, "crcValid 应为 false");
        assertTrue(frames.get(0).valid, "valid 应为 true");
        assertEquals(31, frames.get(0).msgId);
    }

    /** 十六进制字符串转字节数组（忽略所有非十六进制字符，每 2 字符一组）。 */
    private byte[] hexToBytes(String hex) {
        String clean = hex.replaceAll("[^0-9A-Fa-f]", "");
        byte[] out = new byte[clean.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    // ============================================================
    // 辅助方法：小端字节写入（原始）
    // ============================================================

    private void writeInt16(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    private void writeUint16(byte[] buf, int offset, int value) {
        writeInt16(buf, offset, value);
    }

    private void writeInt32(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buf[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buf[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    /** 写入小端 IEEE-754 单精度浮点。 */
    private void writeFloat(byte[] buf, int offset, float value) {
        int bits = Float.floatToIntBits(value);
        buf[offset] = (byte) (bits & 0xFF);
        buf[offset + 1] = (byte) ((bits >> 8) & 0xFF);
        buf[offset + 2] = (byte) ((bits >> 16) & 0xFF);
        buf[offset + 3] = (byte) ((bits >> 24) & 0xFF);
    }
}
