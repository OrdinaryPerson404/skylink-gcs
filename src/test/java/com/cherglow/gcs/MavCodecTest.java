package com.cherglow.gcs.protocol.mavlink;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MAVLink 编解码黄金帧测试：goldens.json 由 pymavlink 2.4.49 生成（v20 common 方言，
 * 与固件 c_library_v2 字段布局一致）。编码侧逐字节对照下行帧；解析侧覆盖 v2 截断、
 * SERIAL_CONTROL（固件布局）与旧方言变体的 extras 自适应学习。
 */
class MavCodecTest {

    private static Map<String, String> goldens;

    private static synchronized Map<String, String> goldens() throws Exception {
        if (goldens == null) {
            try (InputStream in = MavCodecTest.class.getResourceAsStream(
                    "/com/cherglow/gcs/protocol/mavlink/goldens.json")) {
                assertNotNull(in, "goldens.json 缺失");
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                goldens = new java.util.HashMap<>();
                Matcher m = Pattern.compile("\"([a-z0-9_]+)\": \"([0-9a-f]+)\"").matcher(json);
                while (m.find()) {
                    goldens.put(m.group(1), m.group(2));
                }
            }
        }
        return goldens;
    }

    private static int extraOf(int msgid) throws Exception {
        Matcher m = Pattern.compile("\"" + msgid + "\": \\{[^}]*\"extra\": (\\d+)").matcher(
                new String(MavCodecTest.class.getResourceAsStream(
                        "/com/cherglow/gcs/protocol/mavlink/goldens.json").readAllBytes(),
                        StandardCharsets.UTF_8));
        assertTrue(m.find(), "crc_extra 缺少 msgid " + msgid);
        return Integer.parseInt(m.group(1));
    }

    private static List<MavLink.Frame> parseAll(byte[] data) {
        List<MavLink.Frame> out = new ArrayList<>();
        new MavLink.Parser().feed(data, 0, data.length, out::add);
        return out;
    }

    private static byte[] hex(String h) {
        byte[] b = new byte[h.length() / 2];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
        }
        return b;
    }

    // ---- 解析侧：设备上行（v2，含尾零截断） ----

    @Test
    void parseDevHeartbeat() throws Exception {
        MavLink.Frame f = parseAll(hex(goldens().get("dev_heartbeat"))).get(0);
        assertEquals(MavLink.MSG_HEARTBEAT, f.msgid);
        assertEquals(1, f.sysid);
        assertEquals(1, f.compid);
        assertEquals(2, MavLink.rU8(f.payload, 4));          // QUADROTOR（custom_mode u32 在前）
        assertEquals(144, MavLink.rU8(f.payload, 6));        // armed + stabilize
        assertEquals(2, MavLink.rU32(f.payload, 0));         // custom_mode = STAB
    }

    @Test
    void parseDevAttitudeQuaternion() throws Exception {
        MavLink.Frame f = parseAll(hex(goldens().get("dev_attitude_quaternion"))).get(0);
        assertEquals(MavLink.MSG_ATTITUDE_QUATERNION, f.msgid);
        assertEquals(0.999f, MavLink.rF32(f.payload, 4), 1e-6);
        assertEquals(0.01f, MavLink.rF32(f.payload, 8), 1e-6);
        assertEquals(0.02f, MavLink.rF32(f.payload, 12), 1e-6);
        assertEquals(0.03f, MavLink.rF32(f.payload, 16), 1e-6);
        assertEquals(0.01f, MavLink.rF32(f.payload, 20), 1e-6);
        assertEquals(-0.02f, MavLink.rF32(f.payload, 24), 1e-6);
    }

    @Test
    void parseDevSerialControlShell() throws Exception {
        MavLink.Frame f = parseAll(hex(goldens().get("dev_serial_control_shell"))).get(0);
        assertEquals(MavLink.MSG_SERIAL_CONTROL, f.msgid);
        assertEquals(10, MavLink.rU8(f.payload, 6));          // SHELL（baudrate u32/timeout u16 在前）
        assertEquals(8, MavLink.rU8(f.payload, 8));           // count
        assertEquals("roll: 1.", MavLink.rAscii(f.payload, 9, 70));
    }

    @Test
    void parseDevCommandAck() throws Exception {
        MavLink.Frame f = parseAll(hex(goldens().get("dev_command_ack_arm"))).get(0);
        assertEquals(MavLink.MSG_COMMAND_ACK, f.msgid);
        assertEquals(400, MavLink.rU16(f.payload, 0));
        assertEquals(0, MavLink.rU8(f.payload, 2));           // ACCEPTED
        assertEquals(255, MavLink.rU8(f.payload, 3));         // progress
    }

    @Test
    void parseDevParamValue() throws Exception {
        MavLink.Frame f = parseAll(hex(goldens().get("dev_param_value"))).get(0);
        assertEquals(MavLink.MSG_PARAM_VALUE, f.msgid);
        assertEquals(0.06f, MavLink.rF32(f.payload, 0), 1e-9);   // value@0
        assertEquals(112, MavLink.rU16(f.payload, 4));            // count@4
        assertEquals("CTL_R_RATE_P", MavLink.rAscii(f.payload, 8, 16)); // id@8
    }

    @Test
    void parseDevActuatorControlTarget() throws Exception {
        MavLink.Frame f = parseAll(hex(goldens().get("dev_actuator_control_target"))).get(0);
        assertEquals(MavLink.MSG_ACTUATOR_CONTROL_TARGET, f.msgid);
        assertEquals(0.1f, MavLink.rF32(f.payload, 8), 1e-9);   // RL（time_usec u64 之后）
        assertEquals(0.0f, MavLink.rF32(f.payload, 12), 1e-9);  // RR
        assertEquals(0.1f, MavLink.rF32(f.payload, 20), 1e-9);  // FL
    }

    @Test
    void parseDevScaledImuAndRc() throws Exception {
        MavLink.Frame imu = parseAll(hex(goldens().get("dev_scaled_imu"))).get(0);
        assertEquals(MavLink.MSG_SCALED_IMU, imu.msgid);
        assertEquals(30, MavLink.rS16(imu.payload, 4));
        assertEquals(-980, MavLink.rS16(imu.payload, 8));
        MavLink.Frame rc = parseAll(hex(goldens().get("dev_rc_channels_raw"))).get(0);
        assertEquals(1000, MavLink.rU16(rc.payload, 4));   // time u32 之后
        assertEquals(1500, MavLink.rU16(rc.payload, 6));
    }

    // ---- 编码侧：GCS 下行（v1）逐字节对照 ----

    @Test
    void encodeGcsHeartbeat() throws Exception {
        byte[] got = MavLink.buildV1(MavLink.MSG_HEARTBEAT, MavLink.payloadHeartbeatGcs(),
                extraOf(0), 0, 255, 190);
        assertEquals(goldens().get("gcs_heartbeat"), hexStr(got));
    }

    @Test
    void encodeGcsCommandArm() throws Exception {
        byte[] got = MavLink.buildV1(MavLink.MSG_COMMAND_LONG, MavLink.payloadCommandLong(400, 1, 0),
                extraOf(76), 0, 255, 190);
        assertEquals(goldens().get("gcs_command_arm"), hexStr(got));
    }

    @Test
    void encodeGcsCommandSetMode() throws Exception {
        byte[] got = MavLink.buildV1(MavLink.MSG_COMMAND_LONG, MavLink.payloadCommandLong(176, 0, 2),
                extraOf(76), 0, 255, 190);
        assertEquals(goldens().get("gcs_command_setmode_stab"), hexStr(got));
    }

    @Test
    void encodeGcsManualControl() throws Exception {
        byte[] got = MavLink.buildV1(MavLink.MSG_MANUAL_CONTROL, MavLink.payloadManualControl(0, 0, 0, 0, 0, 1),
                extraOf(69), 0, 255, 190);
        assertEquals(goldens().get("gcs_manual_control_zero"), hexStr(got));
    }

    @Test
    void encodeGcsParamFrames() throws Exception {
        byte[] list = MavLink.buildV1(MavLink.MSG_PARAM_REQUEST_LIST, MavLink.payloadParamRequestList(),
                extraOf(21), 0, 255, 190);
        assertEquals(goldens().get("gcs_param_request_list"), hexStr(list));
        byte[] read = MavLink.buildV1(MavLink.MSG_PARAM_REQUEST_READ,
                MavLink.payloadParamRequestRead("CTL_R_RATE_P", -1), extraOf(20), 0, 255, 190);
        assertEquals(goldens().get("gcs_param_request_read"), hexStr(read));
        byte[] set = MavLink.buildV1(MavLink.MSG_PARAM_SET,
                MavLink.payloadParamSet("CTL_R_RATE_P", 0.06f), extraOf(23), 0, 255, 190);
        assertEquals(goldens().get("gcs_param_set"), hexStr(set));
    }

    @Test
    void encodeGcsSerialControl() throws Exception {
        byte[] got = MavLink.buildV1(MavLink.MSG_SERIAL_CONTROL,
                MavLink.payloadSerialControlShell("status"), extraOf(126), 0, 255, 190);
        assertEquals(goldens().get("gcs_serial_control_status"), hexStr(got));
    }

    // ---- 旧方言变体：CRC 自适应学习 ----

    @Test
    void parseLegacyAttitudeQuaternionWithLearn() throws Exception {
        // 旧库 ATTITUDE_QUATERNION 无 repr_offset（32B payload / extra=97），构造一帧
        MavLink.PBuf pb = new MavLink.PBuf()
                .u32(12345)
                .f32(0.999f).f32(0.01f).f32(0.02f).f32(0.03f)
                .f32(0.01f).f32(-0.02f).f32(0.03f);
        int legacyExtra = 97; // goldens.json crc_extra_legacy_variants.ATTITUDE_QUATERNION_32B
        byte[] frame = MavLink.buildV1(MavLink.MSG_ATTITUDE_QUATERNION, pb.toArray(),
                legacyExtra, 0, 1, 1);
        List<MavLink.Frame> frames = parseAll(frame);
        assertEquals(1, frames.size(), "旧方言帧应经 extras 学习后被接受");
        assertEquals(0.999f, MavLink.rF32(frames.get(0).payload, 4), 1e-6);
        // 学习后同 msgid 后续帧直接通过
        frames = parseAll(frame);
        assertEquals(1, frames.size());
    }

    /** S18：SCALED_PRESSURE(msgid 29) 解析 — 用 buildV1 构造后验证解析 */
    @org.junit.jupiter.api.Test
    void parseDevScaledPressure() throws Exception {
        // press_abs=1013.25 hPa → f32@4；temperature=2350 cdegC → i16@12
        byte[] payload = new byte[14];
        MavLink.PBuf.packFloat(payload, 4, 1013.25f);
        MavLink.PBuf.packShort(payload, 12, (short) 2350);
        int spExtra = extraOf(29); // must be 115 from goldens.json
        byte[] frame = MavLink.buildV1(MavLink.MSG_SCALED_PRESSURE, payload, spExtra, 0, 1, 1);
        
        // Verify the built frame is parseable
        List<MavLink.Frame> frames = parseAll(frame);
        assertEquals(1, frames.size(), "SCALED_PRESSURE v1 frame should be parsed");
        MavLink.Frame f = frames.get(0);
        assertEquals(MavLink.MSG_SCALED_PRESSURE, f.msgid);
        assertEquals(1013.25f, MavLink.rF32(f.payload, 4), 1e-3);
        assertEquals(2350, MavLink.rS16(f.payload, 12));
        assertEquals(spExtra, MavLink.knownExtra(MavLink.MSG_SCALED_PRESSURE));
    }

    /** S18：HYGROMETER_SENSOR(msgid 12920) CRC_EXTRA 表注册验证 — msgid>255 需 v2 协议支持 */
    @org.junit.jupiter.api.Test
    void hygrometerSensorExtraRegistered() throws Exception {
        // msgid 12920 is registered with correct extra from goldens.json
        assertEquals(extraOf(12920), MavLink.knownExtra(MavLink.MSG_HYGROMETER_SENSOR));
        // Full frame parsing test deferred: v2 msgid u32 layout requires careful hex construction
        // TODO: add golden v2 hex frame when a real HYGYROMETER_SENSOR frame is captured
    }

    private static String hexStr(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte v : b) {
            sb.append(String.format("%02x", v));
        }
        return sb.toString();
    }
}
