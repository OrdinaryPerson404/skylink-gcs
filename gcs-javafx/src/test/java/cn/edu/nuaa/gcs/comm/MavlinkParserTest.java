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
            case 0  -> 50;   // HEARTBEAT
            case 1  -> 124;  // SYS_STATUS
            case 33 -> 104;  // GLOBAL_POSITION_INT
            case 74 -> 117;  // VFR_HUD
            default -> -1;
        };
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
        // 简化版 HEARTBEAT payload (9 bytes):
        //   offset 6: system_status
        byte[] payload = new byte[9];
        payload[0] = 2;    // type = QUADROTOR
        payload[1] = 12;   // autopilot = PX4
        payload[6] = 4;    // system_status = ACTIVE (简化版，偏移 6)

        byte[] frame = buildFrame(1, 1, 0, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid, "HEARTBEAT 帧应解析为有效");
        assertEquals(0, t.msgId);
        assertEquals(4, t.systemStatus, "system_status 应为 ACTIVE(4)");
    }

    // ============================================================
    // 3. SYS_STATUS (MSGID=1) 解析测试
    // ============================================================

    @Test
    @DisplayName("SYS_STATUS 报文解析 - 电压与电量")
    void testSysStatus() {
        // SYS_STATUS payload (31 bytes), 关键字段:
        //   offset 0: voltage_battery (uint16_t, mV)
        //   offset 10: battery_remaining (uint8_t, %)
        byte[] payload = new byte[31];
        // voltage_battery = 11250 mV = 11.25 V
        payload[0] = (byte) 0x72;  // low byte of 11250 = 0x2BF2 -> wait, let me compute
        // 11250 = 0x2BF2, little-endian: low=0xF2, high=0x2B
        payload[0] = (byte) 0xF2;
        payload[1] = (byte) 0x2B;
        // battery_remaining at offset 10 = 75%
        payload[10] = 75;

        byte[] frame = buildFrame(1, 1, 1, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid, "SYS_STATUS 帧应解析为有效");
        assertEquals(1, t.msgId);
        assertEquals(11.25, t.voltage, 0.01, "电压应为 11.25V");
        assertEquals(75, t.batteryPct, 0.01, "电量应为 75%");
    }

    // ============================================================
    // 4. GLOBAL_POSITION_INT (MSGID=33) 解析测试
    // ============================================================

    @Test
    @DisplayName("GLOBAL_POSITION_INT 报文解析 - 经纬度高度速度航向")
    void testGlobalPositionInt() {
        // 简化版 GLOBAL_POSITION_INT payload (28 bytes):
        //   0: lat (int32, degE7)
        //   4: lon (int32, degE7)
        //   10: alt (int32, mm)
        //   14: vx (int16, cm/s)
        //   16: vy (int16, cm/s)
        //   20: heading (uint8 * 2 deg)
        byte[] payload = new byte[28];

        // lat = 32.0612 -> 320612000 (1e7)
        writeInt32(payload, 0, 320612000);
        // lon = 118.793 -> 1187930000
        writeInt32(payload, 4, 1187930000);
        // alt = 85.5m -> 85500 mm
        writeInt32(payload, 10, 85500);
        // vx = 5.5 m/s -> 550 cm/s
        writeInt16(payload, 14, 550);
        // vy = 3.2 m/s -> 320 cm/s
        writeInt16(payload, 16, 320);
        // heading = 180 (simplified: uint8 * 2)
        payload[20] = 90;  // 90 * 2 = 180 deg

        byte[] frame = buildFrame(1, 1, 33, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid, "GLOBAL_POSITION_INT 帧应解析为有效");
        assertEquals(33, t.msgId);
        assertEquals(32.0612, t.lat, 0.0001, "纬度应为 32.0612°");
        assertEquals(118.793, t.lon, 0.001, "经度应为 118.793°");
        assertEquals(85.5, t.alt, 0.01, "高度应为 85.5m");

        // 速度 = sqrt(vx^2 + vy^2) = sqrt(5.5^2 + 3.2^2) = sqrt(30.25 + 10.24) = sqrt(40.49) ≈ 6.36
        assertEquals(6.36, t.speed, 0.1, "地速约 6.36 m/s");
    }

    // ============================================================
    // 5. VFR_HUD (MSGID=74) 解析测试
    // ============================================================

    @Test
    @DisplayName("VFR_HUD 报文解析 - 地速与航向")
    void testVfrHud() {
        // VFR_HUD payload:
        //   0: airspeed (float) -> 但代码用 uint16，需核对
        // 实际代码: t.speed = readUint16(payload, 0) / 100.0;
        //          t.heading = readUint16(payload, 2) / 100.0;
        // 代码简化为 uint16 读取，测试按代码行为验证
        byte[] payload = new byte[20];
        // groundspeed = 8.5 m/s -> 850 (1/100 m/s)
        writeUint16(payload, 0, 850);
        // heading = 270.5° -> 27050 (1/100 deg)
        writeUint16(payload, 2, 27050);

        byte[] frame = buildFrame(1, 1, 74, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid, "VFR_HUD 帧应解析为有效");
        assertEquals(74, t.msgId);
        assertEquals(8.5, t.speed, 0.01, "地速应为 8.5 m/s");
        assertEquals(270.5, t.heading, 0.01, "航向应为 270.5°");
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
        ssPayload[0] = (byte) 0xF2;  // 11.25V
        ssPayload[1] = (byte) 0x2B;
        ssPayload[10] = 75;
        byte[] ssFrame = buildFrame(1, 1, 1, ssPayload);

        // 合并
        byte[] stream = new byte[hbFrame.length + ssFrame.length];
        System.arraycopy(hbFrame, 0, stream, 0, hbFrame.length);
        System.arraycopy(ssFrame, 0, stream, hbFrame.length, ssFrame.length);

        MavlinkParser.Telemetry t = MavlinkParser.parse(stream, 0, stream.length);
        // parse 返回最后一帧的解析结果
        assertTrue(t.valid);
        assertEquals(1, t.msgId, "连续流解析应返回最后一帧（SYS_STATUS）");
        assertEquals(75, t.batteryPct, 0.01);
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
        // lat = -34.5 -> -345000000 (简化版：offset 0)
        writeInt32(payload, 0, -345000000);
        // lon = -58.0 -> -580000000 (简化版：offset 4)
        writeInt32(payload, 4, -580000000);
        // alt = 100m (offset 10)
        writeInt32(payload, 10, 100000);

        byte[] frame = buildFrame(1, 1, 33, payload);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);

        assertTrue(t.valid);
        assertEquals(-34.5, t.lat, 0.001, "南纬应为负值");
        assertEquals(-58.0, t.lon, 0.001, "西经应为负值");
    }

    // ============================================================
    // 辅助方法：小端字节写入
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
}
