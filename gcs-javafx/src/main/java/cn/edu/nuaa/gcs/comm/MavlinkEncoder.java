package cn.edu.nuaa.gcs.comm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MAVLink 上行帧编码器（v1 帧，起始字节 0xFE）。
 *
 * 关键作用：CF-Drone 固件在 receiveMavlink() 中只要收到任意字节即置 mavlinkConnected=true，
 * 此后才会发送完整遥测（ATTITUDE_QUATERNION / SCALED_IMU / RC_CHANNELS_RAW /
 * ACTUATOR_CONTROL_TARGET 等）。若地面站从不发送任何数据，飞控只发心跳，地面站将
 * 收不到姿态/IMU/电机等数据。因此地面站必须周期性发送心跳（或任意帧）以触发完整遥测。
 *
 * 采用 v1 帧（0xFE）编码：CF-Drone 固件基于 c_library_v2，其 mavlink_parse_char 同时识别
 * v1/v2 入站帧，故 v1 上行帧可被正确解析。
 *
 * 安全约束：本类只提供编码能力，是否真正发送由上层按安全策略门控（见 CommunicationService
 * 与控制器中的安全确认）。解锁/切模式/设参数等控制类操作默认禁用，需用户确认。
 */
public class MavlinkEncoder {
    private static final int STX = 0xFE;
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    // 常用命令 ID
    public static final int CMD_COMPONENT_ARM_DISARM = 400; // param1: 1=解锁 0=上锁
    public static final int CMD_DO_SET_MODE = 176;           // param2: 飞行模式（0=RAW 2=STAB 4=AUTO）
    public static final int CMD_REQUEST_MESSAGE = 512;       // param1: 请求的 msgId（如 148=AUTOPILOT_VERSION）
    public static final int CMD_REQUEST_AUTOPILOT_VERSION = 148;

    // MAV_TYPE / MAV_AUTOPILOT（用于 GCS 心跳）
    private static final int MAV_TYPE_GCS = 6;
    private static final int MAV_AUTOPILOT_INVALID = 8;
    private static final int MAV_STATE_ACTIVE = 4;

    /** 地面站心跳：触发飞控 mavlinkConnected=true，使其发送完整遥测。 */
    public static byte[] heartbeat(int gcsSysId, int gcsCompId) {
        // HEARTBEAT payload(9): custom_mode(u32) type(u8) autopilot(u8) base_mode(u8) system_status(u8) mavlink_version(u8)
        ByteBuffer pb = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN);
        pb.putInt(0);                     // custom_mode
        pb.put((byte) MAV_TYPE_GCS);      // type
        pb.put((byte) MAV_AUTOPILOT_INVALID); // autopilot
        pb.put((byte) 0);                 // base_mode
        pb.put((byte) MAV_STATE_ACTIVE);  // system_status
        pb.put((byte) 3);                 // mavlink_version
        return buildFrame(gcsSysId, gcsCompId, 0, pb.array());
    }

    /** 通用 COMMAND_LONG。 */
    public static byte[] commandLong(int gcsSysId, int gcsCompId,
                                     int targetSystem, int targetComponent,
                                     int command, float p1, float p2, float p3,
                                     float p4, float p5, float p6, float p7, int confirmation) {
        // payload(33): param1-7(f32) command(u16) target_system(u8) target_component(u8) confirmation(u8)
        ByteBuffer pb = ByteBuffer.allocate(33).order(ByteOrder.LITTLE_ENDIAN);
        pb.putFloat(p1);
        pb.putFloat(p2);
        pb.putFloat(p3);
        pb.putFloat(p4);
        pb.putFloat(p5);
        pb.putFloat(p6);
        pb.putFloat(p7);
        pb.putShort((short) command);
        pb.put((byte) targetSystem);
        pb.put((byte) targetComponent);
        pb.put((byte) confirmation);
        return buildFrame(gcsSysId, gcsCompId, 76, pb.array());
    }

    /** 解锁/上锁（控制类，默认禁用，需安全确认）。 */
    public static byte[] armDisarm(int gcsSysId, int gcsCompId, int targetSystem, int targetComponent, boolean arm) {
        return commandLong(gcsSysId, gcsCompId, targetSystem, targetComponent,
                CMD_COMPONENT_ARM_DISARM, arm ? 1f : 0f, 0, 0, 0, 0, 0, 0, 0);
    }

    /** 切换飞行模式（控制类，默认禁用，需安全确认）。 */
    public static byte[] setMode(int gcsSysId, int gcsCompId, int targetSystem, int targetComponent, int mode) {
        // MAV_CMD_DO_SET_MODE: param1=保留(0) param2=mode param3-7=0
        return commandLong(gcsSysId, gcsCompId, targetSystem, targetComponent,
                CMD_DO_SET_MODE, 0, mode, 0, 0, 0, 0, 0, 0);
    }

    /** 请求 AUTOPILOT_VERSION（只读，安全）。 */
    public static byte[] requestAutopilotVersion(int gcsSysId, int gcsCompId, int targetSystem, int targetComponent) {
        return commandLong(gcsSysId, gcsCompId, targetSystem, targetComponent,
                CMD_REQUEST_MESSAGE, CMD_REQUEST_AUTOPILOT_VERSION, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * 请求任意 MAVLink 消息（只读，安全）。
     * 使用 MAV_CMD_REQUEST_MESSAGE(512)，param1 = 目标 msgId。
     * 若飞控固件支持该消息，会回传一帧；不支持则返回 COMMAND_ACK 结果非 0。
     */
    public static byte[] requestMessage(int gcsSysId, int gcsCompId,
                                        int targetSystem, int targetComponent, int msgId) {
        return commandLong(gcsSysId, gcsCompId, targetSystem, targetComponent,
                CMD_REQUEST_MESSAGE, msgId, 0, 0, 0, 0, 0, 0, 0);
    }

    /** 请求 BATTERY_STATUS(147) - 电池状态（只读，安全）。 */
    public static byte[] requestBatteryStatus(int gcsSysId, int gcsCompId,
                                              int targetSystem, int targetComponent) {
        return requestMessage(gcsSysId, gcsCompId, targetSystem, targetComponent, 147);
    }

    /** 请求参数列表（只读，安全）。 */
    public static byte[] paramRequestList(int gcsSysId, int gcsCompId, int targetSystem, int targetComponent) {
        // payload(2): target_system(u8) target_component(u8)
        byte[] payload = new byte[]{(byte) targetSystem, (byte) targetComponent};
        return buildFrame(gcsSysId, gcsCompId, 21, payload);
    }

    /** 按索引请求单个参数（只读，安全）。paramName 为 null 时按索引请求。 */
    public static byte[] paramRequestRead(int gcsSysId, int gcsCompId,
                                          int targetSystem, int targetComponent, int paramIndex, String paramName) {
        // payload(20): param_index(i16) target_system(u8) target_component(u8) param_id(char[16])
        ByteBuffer pb = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
        pb.putShort((short) paramIndex);
        pb.put((byte) targetSystem);
        pb.put((byte) targetComponent);
        byte[] name = new byte[16];
        if (paramName != null) {
            byte[] nb = paramName.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            System.arraycopy(nb, 0, name, 0, Math.min(nb.length, 16));
        }
        pb.put(name);
        return buildFrame(gcsSysId, gcsCompId, 20, pb.array());
    }

    /** 设置参数（配置变更类，默认禁用，需安全确认；会修改飞控配置）。 */
    public static byte[] paramSet(int gcsSysId, int gcsCompId,
                                  int targetSystem, int targetComponent, String paramName, float value, int paramType) {
        // payload(23): param_value(f32) target_system(u8) target_component(u8) param_id(char[16]) param_type(u8)
        ByteBuffer pb = ByteBuffer.allocate(23).order(ByteOrder.LITTLE_ENDIAN);
        pb.putFloat(value);
        pb.put((byte) targetSystem);
        pb.put((byte) targetComponent);
        byte[] name = new byte[16];
        if (paramName != null) {
            byte[] nb = paramName.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            System.arraycopy(nb, 0, name, 0, Math.min(nb.length, 16));
        }
        pb.put(name);
        pb.put((byte) paramType);
        return buildFrame(gcsSysId, gcsCompId, 23, pb.array());
    }

    /** 构造 v1 帧：STX LEN SEQ SYSID COMPID MSGID PAYLOAD CRC(2)。 */
    private static byte[] buildFrame(int sysId, int compId, int msgId, byte[] payload) {
        int payloadLen = payload.length;
        byte[] frame = new byte[payloadLen + 8];
        frame[0] = (byte) STX;
        frame[1] = (byte) payloadLen;
        frame[2] = (byte) (SEQ.getAndIncrement() & 0xFF);
        frame[3] = (byte) sysId;
        frame[4] = (byte) compId;
        frame[5] = (byte) msgId;
        System.arraycopy(payload, 0, frame, 6, payloadLen);
        int crc = crc16(0xFFFF, frame, 1, payloadLen + 5);
        int extra = getCrcExtra(msgId);
        if (extra >= 0) crc = crc16Step(crc, extra);
        frame[6 + payloadLen] = (byte) (crc & 0xFF);
        frame[7 + payloadLen] = (byte) ((crc >> 8) & 0xFF);
        return frame;
    }

    private static int crc16(int crc, byte[] data, int start, int len) {
        for (int i = 0; i < len; i++) {
            crc = crc16Step(crc, data[start + i] & 0xFF);
        }
        return crc;
    }

    private static int crc16Step(int crc, int data) {
        crc ^= data;
        for (int i = 0; i < 8; i++) {
            if ((crc & 1) != 0) crc = (crc >>> 1) ^ 0x8408;
            else crc = crc >>> 1;
        }
        return crc;
    }

    /** 上行报文 CRC_EXTRA（与 MavlinkParser 保持一致）。 */
    private static int getCrcExtra(int msgId) {
        return switch (msgId) {
            case 0  -> 50;   // HEARTBEAT
            case 20 -> 214;  // PARAM_REQUEST_READ
            case 21 -> 159;  // PARAM_REQUEST_LIST
            case 23 -> 168;  // PARAM_SET
            case 76 -> 152;  // COMMAND_LONG
            default -> -1;
        };
    }
}
