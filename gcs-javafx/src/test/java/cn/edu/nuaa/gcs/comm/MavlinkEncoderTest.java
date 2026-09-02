package cn.edu.nuaa.gcs.comm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MAVLink 上行编码器单元测试。
 *
 * 验证策略：
 * 1. 帧结构断言：STX/LEN/SYSID/COMPID/MSGID 字段正确。
 * 2. 往返一致性：MavlinkEncoder 编码的帧用 MavlinkParser 解析，
 *    验证字段值与编码输入一致（编解码对称）。
 * 3. 命令语义：armDisarm 的 command=400 + p1=1/0，setMode 的 command=176 + p2=mode。
 */
class MavlinkEncoderTest {

    private static final int GCS_SYS = 255;
    private static final int GCS_COMP = 0;
    private static final int DRONE_SYS = 1;
    private static final int DRONE_COMP = 1;

    // ============================================================
    // 1. HEARTBEAT 编码测试
    // ============================================================

    @Test
    @DisplayName("heartbeat 帧结构与字段语义")
    void testHeartbeatStructure() {
        byte[] frame = MavlinkEncoder.heartbeat(GCS_SYS, GCS_COMP);
        assertEquals(0xFE, frame[0] & 0xFF, "STX 应为 0xFE");
        assertEquals(9, frame[1] & 0xFF, "HEARTBEAT payload 长度 9");
        assertEquals(GCS_SYS, frame[3] & 0xFF, "sysId 应为 GCS_SYS");
        assertEquals(GCS_COMP, frame[4] & 0xFF, "compId 应为 GCS_COMP");
        assertEquals(0, frame[5] & 0xFF, "msgId 应为 0（HEARTBEAT）");
        // payload 布局：[0..3] custom_mode(0) [4] type(6=GCS) [5] autopilot(8)
        //               [6] base_mode(0) [7] system_status(4) [8] mavlink_version(3)
        assertEquals(6, frame[10] & 0xFF, "type 应为 MAV_TYPE_GCS=6");
        assertEquals(8, frame[11] & 0xFF, "autopilot 应为 MAV_AUTOPILOT_INVALID=8");
        assertEquals(0, frame[12] & 0xFF, "base_mode 应为 0");
        assertEquals(4, frame[13] & 0xFF, "system_status 应为 MAV_STATE_ACTIVE=4");
        assertEquals(3, frame[14] & 0xFF, "mavlink_version 应为 3");
    }

    @Test
    @DisplayName("heartbeat 往返：Parser 解析 Encoder 编码的帧")
    void testHeartbeatRoundTrip() {
        byte[] frame = MavlinkEncoder.heartbeat(GCS_SYS, GCS_COMP);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);
        assertTrue(t.valid, "Encoder 编码的 HEARTBEAT 应被 Parser 解析为有效");
        assertEquals(0, t.msgId);
        assertEquals(GCS_SYS, t.sysId);
        assertEquals(GCS_COMP, t.compId);
        assertTrue(t.hasHeartbeat);
        // base_mode=0 → armed=false
        assertFalse(t.armed, "GCS 心跳 base_mode=0 不应被识别为解锁");
    }

    // ============================================================
    // 2. PARAM_REQUEST_LIST 编码测试
    // ============================================================

    @Test
    @DisplayName("paramRequestList 帧结构与目标系统")
    void testParamRequestList() {
        byte[] frame = MavlinkEncoder.paramRequestList(GCS_SYS, GCS_COMP, DRONE_SYS, DRONE_COMP);
        assertEquals(0xFE, frame[0] & 0xFF);
        assertEquals(2, frame[1] & 0xFF, "PARAM_REQUEST_LIST payload 长度 2");
        assertEquals(21, frame[5] & 0xFF, "msgId 应为 21");
        // payload[0]=target_system, payload[1]=target_component
        assertEquals(DRONE_SYS, frame[6] & 0xFF, "target_system 应为 DRONE_SYS");
        assertEquals(DRONE_COMP, frame[7] & 0xFF, "target_component 应为 DRONE_COMP");
    }

    @Test
    @DisplayName("paramRequestList 往返：CRC 通过 Parser 校验")
    void testParamRequestListRoundTrip() {
        byte[] frame = MavlinkEncoder.paramRequestList(GCS_SYS, GCS_COMP, DRONE_SYS, DRONE_COMP);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);
        assertTrue(t.valid, "Encoder 编码的 PARAM_REQUEST_LIST CRC 应通过 Parser 校验");
        assertEquals(21, t.msgId);
    }

    // ============================================================
    // 3. PARAM_REQUEST_READ 编码测试
    // ============================================================

    @Test
    @DisplayName("paramRequestRead 按名称请求")
    void testParamRequestReadByName() {
        byte[] frame = MavlinkEncoder.paramRequestRead(GCS_SYS, GCS_COMP, DRONE_SYS, DRONE_COMP, -1, "MOT_THR_LVL");
        assertEquals(0xFE, frame[0] & 0xFF);
        assertEquals(20, frame[1] & 0xFF, "PARAM_REQUEST_READ payload 长度 20");
        assertEquals(20, frame[5] & 0xFF, "msgId 应为 20");
        // payload[2]=target_system, payload[3]=target_component
        assertEquals(DRONE_SYS, frame[8] & 0xFF);
        assertEquals(DRONE_COMP, frame[9] & 0xFF);
        // 参数名从 payload offset 4 开始（frame offset 10）
        byte[] nameBytes = new byte[11];
        System.arraycopy(frame, 10, nameBytes, 0, 11);
        String name = new String(nameBytes, java.nio.charset.StandardCharsets.US_ASCII).trim();
        assertEquals("MOT_THR_LVL", name, "参数名应被写入 char[16]");
    }

    // ============================================================
    // 4. COMMAND_LONG 编码测试
    // ============================================================

    @Test
    @DisplayName("commandLong 帧结构与命令字段")
    void testCommandLongStructure() {
        byte[] frame = MavlinkEncoder.commandLong(GCS_SYS, GCS_COMP, DRONE_SYS, DRONE_COMP,
                400, 1f, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(0xFE, frame[0] & 0xFF);
        assertEquals(33, frame[1] & 0xFF, "COMMAND_LONG payload 长度 33");
        assertEquals(76, frame[5] & 0xFF, "msgId 应为 76");
        // command 字段在 payload offset 28（frame offset 34），uint16
        int cmd = (frame[34] & 0xFF) | ((frame[35] & 0xFF) << 8);
        assertEquals(400, cmd, "command 应为 400");
        // target_system 在 payload offset 30（frame offset 36）
        assertEquals(DRONE_SYS, frame[36] & 0xFF);
    }

    @Test
    @DisplayName("armDisarm(true) → command=400, p1=1.0")
    void testArmCommand() {
        byte[] frame = MavlinkEncoder.armDisarm(GCS_SYS, GCS_COMP, DRONE_SYS, DRONE_COMP, true);
        // p1 在 payload offset 0（frame offset 6），float
        int bits = (frame[6] & 0xFF) | ((frame[7] & 0xFF) << 8) |
                   ((frame[8] & 0xFF) << 16) | ((frame[9] & 0xFF) << 24);
        assertEquals(1.0f, Float.intBitsToFloat(bits), 0.001f, "arm 时 p1=1.0");
        // command 在 frame offset 34
        int cmd = (frame[34] & 0xFF) | ((frame[35] & 0xFF) << 8);
        assertEquals(400, cmd);
    }

    @Test
    @DisplayName("armDisarm(false) → p1=0.0")
    void testDisarmCommand() {
        byte[] frame = MavlinkEncoder.armDisarm(GCS_SYS, GCS_COMP, DRONE_SYS, DRONE_COMP, false);
        int bits = (frame[6] & 0xFF) | ((frame[7] & 0xFF) << 8) |
                   ((frame[8] & 0xFF) << 16) | ((frame[9] & 0xFF) << 24);
        assertEquals(0.0f, Float.intBitsToFloat(bits), 0.001f, "disarm 时 p1=0.0");
    }

    @Test
    @DisplayName("setMode(4=AUTO) → command=176, p2=4.0")
    void testSetModeCommand() {
        byte[] frame = MavlinkEncoder.setMode(GCS_SYS, GCS_COMP, DRONE_SYS, DRONE_COMP, 4);
        // p2 在 payload offset 4（frame offset 10），float
        int bits = (frame[10] & 0xFF) | ((frame[11] & 0xFF) << 8) |
                   ((frame[12] & 0xFF) << 16) | ((frame[13] & 0xFF) << 24);
        assertEquals(4.0f, Float.intBitsToFloat(bits), 0.001f, "mode=4 时 p2=4.0");
        int cmd = (frame[34] & 0xFF) | ((frame[35] & 0xFF) << 8);
        assertEquals(176, cmd);
    }

    @Test
    @DisplayName("commandLong 往返：CRC 通过 Parser 校验")
    void testCommandLongRoundTrip() {
        byte[] frame = MavlinkEncoder.armDisarm(GCS_SYS, GCS_COMP, DRONE_SYS, DRONE_COMP, true);
        MavlinkParser.Telemetry t = MavlinkParser.parse(frame, 0, frame.length);
        assertTrue(t.valid, "COMMAND_LONG 的 CRC 应通过 Parser 校验");
        assertEquals(76, t.msgId);
    }

    // ============================================================
    // 5. SEQ 自增测试
    // ============================================================

    @Test
    @DisplayName("连续编码的帧 SEQ 递增")
    void testSeqIncrement() {
        byte[] f1 = MavlinkEncoder.heartbeat(GCS_SYS, GCS_COMP);
        byte[] f2 = MavlinkEncoder.heartbeat(GCS_SYS, GCS_COMP);
        int seq1 = f1[2] & 0xFF;
        int seq2 = f2[2] & 0xFF;
        assertEquals(1, (seq2 - seq1 + 256) % 256, "SEQ 应递增 1（模 256）");
    }
}
