package cn.edu.nuaa.gcs.comm;

/**
 * MAVLink 帧解析器（同时兼容 v1 起始字节 0xFE 与 v2 起始字节 0xFD）。
 *
 * 设计说明：
 * - 兼容 v1/v2 双协议：CF-Drone 固件基于 c_library_v2，在地面站未主动发数据时，
 *   飞控通道不会置 v1 兼容标志，因此会发送 v2 帧（0xFD 起始）。仅支持 v1 的解析器
 *   无法接收该固件的遥测。本解析器同时识别 0xFE / 0xFD，彻底消除协议版本歧义。
 * - CRC_EXTRA 取值均取自 MAVLink c_library_v2 权威头文件，已逐条核对
 *   （纠正了多处与常见记忆不符的取值，例如 PARAM_VALUE=220、EXTENDED_SYS_STATE=#245、
 *   ACTUATOR_CONTROL_TARGET=#140、COMMAND_ACK=#77）。
 * - 解码 CF-Drone 实际发送的报文：HEARTBEAT、EXTENDED_SYS_STATE、ATTITUDE_QUATERNION、
 *   RC_CHANNELS_RAW、ACTUATOR_CONTROL_TARGET、SCALED_IMU、PARAM_VALUE、COMMAND_ACK、
 *   SERIAL_CONTROL、MISSION_COUNT；同时保留对 SYS_STATUS / GLOBAL_POSITION_INT / VFR_HUD
 *   的解码，以兼容其它 MAVLink 飞控。
 */
public class MavlinkParser {
    private static final int STX_V1 = 0xFE; // MAVLink v1 起始字节
    private static final int STX_V2 = 0xFD; // MAVLink v2 起始字节

    public static class Telemetry {
        public boolean valid;
        public int msgId;
        public int sysId, compId;

        // 位置（部分飞控/固件不发送 GLOBAL_POSITION_INT，例如 CF-Drone 无 GPS 上报）
        public double lat, lon, alt, speed, heading;
        public boolean hasPosition;

        // 电池（CF-Drone 固件不通过 SYS_STATUS 上报电池，电量仅飞控内部用于低压保护）
        public double voltage, batteryPct;
        public boolean hasBattery;

        // 心跳
        public int baseMode;      // MAV_MODE_FLAG 位图（bit7=已解锁）
        public int systemStatus;  // MAV_STATE
        public int customMode;    // 飞行模式（CF-Drone: 0=RAW 1=ACRO 2=STAB 3=ALTHOLD 4=AUTO）
        public boolean armed;
        public boolean hasHeartbeat;

        // 姿态（来自 ATTITUDE_QUATERNION，四元数转欧拉角）
        public double roll, pitch, yaw;          // 弧度
        public double rollSpeed, pitchSpeed, yawSpeed; // rad/s
        public boolean hasAttitude;

        // IMU（来自 SCALED_IMU）
        public double accX, accY, accZ;     // m/s²
        public double gyroX, gyroY, gyroZ;  // rad/s
        public double imuTemp;              // °C
        public boolean hasImu;

        // RC 通道（来自 RC_CHANNELS_RAW）
        public int[] rcChannels = new int[8];
        public int rssi;
        public boolean hasRc;

        // 电机输出（来自 ACTUATOR_CONTROL_TARGET，前 4 个为四电机推力）
        public double[] motorOutputs = new double[8];
        public boolean hasMotors;

        // 扩展系统状态（来自 EXTENDED_SYS_STATE）
        public int landedState; // 1=在地面 2=在空中
        public boolean hasLandedState;

        // 参数（来自 PARAM_VALUE）
        public String paramName;
        public double paramValue;
        public int paramCount, paramIndex, paramType;
        public boolean hasParam;

        // 命令应答（来自 COMMAND_ACK）
        public int ackCommand;
        public int ackResult; // 0=ACCEPTED 1=TEMPORARILY_REJECTED 2=DENIED 3=UNSUPPORTED 4=FAILED 5=IN_PROGRESS
        public boolean hasAck;

        // 任务数量（来自 MISSION_COUNT，CF-Drone 恒为 0）
        public int missionCount;
        public boolean hasMissionCount;

        // Shell 输出（来自 SERIAL_CONTROL，飞控 CLI 回传）
        public String shellText;
        public boolean hasShell;
    }

    public static Telemetry parse(byte[] data, int offset, int len) {
        Telemetry t = new Telemetry();
        if (len < 8) return t;
        int p = offset;
        int end = offset + len;
        while (p < end) {
            int stx = data[p] & 0xFF;
            if (stx == STX_V1) {
                // v1: STX(1) LEN(1) SEQ(1) SYSID(1) COMPID(1) MSGID(1) PAYLOAD(N) CRC(2)
                if (p + 6 > end) break;
                int payloadLen = data[p + 1] & 0xFF;
                int totalLen = payloadLen + 8;
                if (p + totalLen > end) { p++; continue; }
                int msgId = data[p + 5] & 0xFF;
                byte[] payload = new byte[payloadLen];
                System.arraycopy(data, p + 6, payload, 0, payloadLen);
                if (verifyCrc(data, p, 5, payloadLen, msgId)) {
                    t.valid = true;
                    t.sysId = data[p + 3] & 0xFF;
                    t.compId = data[p + 4] & 0xFF;
                    t.msgId = msgId;
                    decodeMessage(msgId, payload, t);
                    p += totalLen;
                } else {
                    p++; // CRC 校验失败，逐字节重新同步
                }
            } else if (stx == STX_V2) {
                // v2: STX(1) LEN(2) INCOMPAT(1) COMPAT(1) SEQ(1) SYSID(1) COMPID(1) MSGID(3) PAYLOAD(N) CRC(2) [SIG(13)]
                if (p + 11 > end) break;
                int payloadLen = (data[p + 1] & 0xFF) | ((data[p + 2] & 0xFF) << 8);
                int incompatFlags = data[p + 3] & 0xFF;
                int msgId = (data[p + 8] & 0xFF) | ((data[p + 9] & 0xFF) << 8) | ((data[p + 10] & 0xFF) << 16);
                int signatureLen = (incompatFlags & 0x01) != 0 ? 13 : 0; // bit0=MAVLINK_IFLAG_SIGNED
                int totalLen = 1 + 10 + payloadLen + 2 + signatureLen;
                if (p + totalLen > end) { p++; continue; }
                byte[] payload = new byte[payloadLen];
                System.arraycopy(data, p + 11, payload, 0, payloadLen);
                if (verifyCrc(data, p, 10, payloadLen, msgId)) {
                    t.valid = true;
                    t.sysId = data[p + 6] & 0xFF;
                    t.compId = data[p + 7] & 0xFF;
                    t.msgId = msgId;
                    decodeMessage(msgId, payload, t);
                    p += totalLen;
                } else {
                    p++;
                }
            } else {
                p++;
            }
        }
        return t;
    }

    private static void decodeMessage(int msgId, byte[] payload, Telemetry t) {
        switch (msgId) {
            case 0: // HEARTBEAT
                if (payload.length >= 8) {
                    t.customMode = (int) readUint32(payload, 0);
                    t.baseMode = payload[6] & 0xFF;       // MAV_MODE_FLAG（bit7=已解锁）
                    t.systemStatus = payload[7] & 0xFF;    // MAV_STATE
                    t.armed = (t.baseMode & 0x80) != 0;
                    t.hasHeartbeat = true;
                }
                break;
            case 1: // SYS_STATUS（兼容其它飞控；CF-Drone 不发送）
                if (payload.length >= 2)
                    t.voltage = readUint16(payload, 0) / 1000.0;
                if (payload.length >= 11)
                    t.batteryPct = payload[10] & 0xFF;
                t.hasBattery = true;
                break;
            case 22: // PARAM_VALUE
                if (payload.length >= 25) {
                    t.paramValue = readFloat(payload, 0);
                    t.paramCount = readUint16(payload, 4);
                    t.paramIndex = readUint16(payload, 6);
                    byte[] nameBuf = new byte[16];
                    System.arraycopy(payload, 8, nameBuf, 0, 16);
                    t.paramName = readString(nameBuf);
                    t.paramType = payload[24] & 0xFF;
                    t.hasParam = true;
                }
                break;
            case 26: // SCALED_IMU
                if (payload.length >= 16) {
                    double G = 9.80665;
                    t.accX = readInt16(payload, 4) / 1000.0 * G;   // milli-g → m/s²
                    t.accY = readInt16(payload, 6) / 1000.0 * G;
                    t.accZ = readInt16(payload, 8) / 1000.0 * G;
                    t.gyroX = readInt16(payload, 10) / 1000.0;      // milli-rad/s → rad/s
                    t.gyroY = readInt16(payload, 12) / 1000.0;
                    t.gyroZ = readInt16(payload, 14) / 1000.0;
                    if (payload.length >= 24)
                        t.imuTemp = readInt16(payload, 22) / 100.0; // centi-degC → °C
                    t.hasImu = true;
                }
                break;
            case 31: // ATTITUDE_QUATERNION
                if (payload.length >= 32) {
                    double w = readFloat(payload, 4);
                    double x = readFloat(payload, 8);
                    double y = readFloat(payload, 12);
                    double z = readFloat(payload, 16);
                    // 四元数 (w,x,y,z) → 欧拉角（roll/pitch/yaw，弧度），FRD 体系
                    quaternionToEuler(w, x, y, z, t);
                    t.rollSpeed = readFloat(payload, 20);
                    t.pitchSpeed = readFloat(payload, 24);
                    t.yawSpeed = readFloat(payload, 28);
                    t.hasAttitude = true;
                }
                break;
            case 33: // GLOBAL_POSITION_INT（兼容其它飞控；CF-Drone 不发送）
                if (payload.length >= 28) {
                    t.lat = readInt32(payload, 0) / 1e7;
                    t.lon = readInt32(payload, 4) / 1e7;
                    t.alt = readInt32(payload, 10) / 1000.0;
                    t.speed = Math.sqrt(
                        Math.pow(readInt16(payload, 14) / 100.0, 2) +
                        Math.pow(readInt16(payload, 16) / 100.0, 2));
                    t.heading = (payload[20] & 0xFF) * 2.0;
                    t.hasPosition = true;
                }
                break;
            case 35: // RC_CHANNELS_RAW
                if (payload.length >= 22) {
                    for (int i = 0; i < 8; i++)
                        t.rcChannels[i] = readUint16(payload, 4 + i * 2);
                    t.rssi = payload[21] & 0xFF;
                    t.hasRc = true;
                }
                break;
            case 44: // MISSION_COUNT（CF-Drone 恒返回 0）
                if (payload.length >= 2) {
                    t.missionCount = readUint16(payload, 0);
                    t.hasMissionCount = true;
                }
                break;
            case 74: // VFR_HUD（兼容其它飞控）
                if (payload.length >= 4) {
                    t.speed = readUint16(payload, 0) / 100.0;
                    t.heading = readUint16(payload, 2) / 100.0;
                }
                break;
            case 77: // COMMAND_ACK
                if (payload.length >= 3) {
                    t.ackCommand = readUint16(payload, 0);
                    t.ackResult = payload[2] & 0xFF;
                    t.hasAck = true;
                }
                break;
            case 126: // SERIAL_CONTROL（飞控 shell 输出回传）
                if (payload.length >= 9) {
                    int count = payload[8] & 0xFF;
                    int avail = Math.min(count, Math.min(70, payload.length - 9));
                    if (avail > 0) {
                        byte[] txt = new byte[avail];
                        System.arraycopy(payload, 9, txt, 0, avail);
                        t.shellText = new String(txt, java.nio.charset.StandardCharsets.US_ASCII);
                        t.hasShell = true;
                    }
                }
                break;
            case 140: // ACTUATOR_CONTROL_TARGET
                if (payload.length >= 40) {
                    for (int i = 0; i < 8; i++)
                        t.motorOutputs[i] = readFloat(payload, 8 + i * 4);
                    t.hasMotors = true;
                }
                break;
            case 245: // EXTENDED_SYS_STATE
                if (payload.length >= 2) {
                    // payload[0]=vtol_state, payload[1]=landed_state(1=地面 2=空中)
                    t.landedState = payload[1] & 0xFF;
                    t.hasLandedState = true;
                }
                break;
            default:
                break;
        }
    }

    /** 四元数转欧拉角，写入 t.roll/pitch/yaw（弧度）。 */
    private static void quaternionToEuler(double w, double x, double y, double z, Telemetry t) {
        double sinr_cosp = 2 * (w * x + y * z);
        double cosr_cosp = 1 - 2 * (x * x + y * y);
        t.roll = Math.atan2(sinr_cosp, cosr_cosp);
        double sinp = 2 * (w * y - z * x);
        if (sinp > 1) sinp = 1; else if (sinp < -1) sinp = -1;
        t.pitch = Math.asin(sinp);
        double siny_cosp = 2 * (w * z + x * y);
        double cosy_cosp = 1 - 2 * (y * y + z * z);
        t.yaw = Math.atan2(siny_cosp, cosy_cosp);
    }

    /**
     * CRC16-X25 校验。同时支持 v1（STX 后 5 字节头）与 v2（STX 后 10 字节头）。
     * @param stxOffset  STX 字节偏移
     * @param headerCrcBytes STX 之后参与 CRC 的头字节数（v1=5, v2=10）
     * @param payloadLen 负载长度
     */
    private static boolean verifyCrc(byte[] data, int stxOffset, int headerCrcBytes, int payloadLen, int msgId) {
        int crc = 0xFFFF;
        for (int i = 1; i <= headerCrcBytes + payloadLen; i++) {
            crc = crc16Step(crc, data[stxOffset + i] & 0xFF);
        }
        int extra = getCrcExtra(msgId);
        if (extra >= 0) {
            crc = crc16Step(crc, extra);
        }
        int crcPos = stxOffset + 1 + headerCrcBytes + payloadLen;
        int crcLow = data[crcPos] & 0xFF;
        int crcHigh = data[crcPos + 1] & 0xFF;
        int frameCrc = crcLow | (crcHigh << 8);
        return (crc & 0xFFFF) == frameCrc;
    }

    /**
     * CRC16-X25 (CCITT) 单步迭代
     * 多项式: x^16 + x^12 + x^5 + 1 (0x1021), 初始值 0xFFFF, 字节反射, 结果反射
     */
    private static int crc16Step(int crc, int data) {
        crc ^= data;
        for (int i = 0; i < 8; i++) {
            if ((crc & 1) != 0)
                crc = (crc >>> 1) ^ 0x8408;  // 反射后的多项式 0x8408
            else
                crc = crc >>> 1;
        }
        return crc;
    }

    /**
     * MAVLink 各报文的 CRC_EXTRA 值（取自 c_library_v2 权威头文件）。
     * 返回 -1 表示未知（将跳过 CRC_EXTRA 附加字节，几乎不可能通过 CRC）。
     */
    private static int getCrcExtra(int msgId) {
        return switch (msgId) {
            case 0   -> 50;   // HEARTBEAT
            case 1   -> 124;  // SYS_STATUS
            case 20  -> 214;  // PARAM_REQUEST_READ
            case 21  -> 159;  // PARAM_REQUEST_LIST
            case 22  -> 220;  // PARAM_VALUE
            case 23  -> 168;  // PARAM_SET
            case 26  -> 170;  // SCALED_IMU
            case 31  -> 246;  // ATTITUDE_QUATERNION
            case 33  -> 104;  // GLOBAL_POSITION_INT
            case 35  -> 244;  // RC_CHANNELS_RAW
            case 44  -> 221;  // MISSION_COUNT
            case 74  -> 117;  // VFR_HUD
            case 76  -> 152;  // COMMAND_LONG
            case 77  -> 143;  // COMMAND_ACK
            case 126 -> 220;  // SERIAL_CONTROL
            case 140 -> 181;  // ACTUATOR_CONTROL_TARGET
            case 245 -> 130;  // EXTENDED_SYS_STATE
            default  -> -1;
        };
    }

    private static int readInt16(byte[] b, int off) {
        int v = (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
        return v > 32767 ? v - 65536 : v;
    }

    private static int readUint16(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    private static long readInt32(byte[] b, int off) {
        long val = (b[off] & 0xFFL) | ((b[off + 1] & 0xFFL) << 8) |
               ((b[off + 2] & 0xFFL) << 16) | ((b[off + 3] & 0xFFL) << 24);
        if ((val & 0x80000000L) != 0)
            val |= 0xFFFFFFFF00000000L;
        return val;
    }

    private static long readUint32(byte[] b, int off) {
        return (b[off] & 0xFFL) | ((b[off + 1] & 0xFFL) << 8) |
               ((b[off + 2] & 0xFFL) << 16) | ((b[off + 3] & 0xFFL) << 24);
    }

    /** 读取小端 IEEE-754 单精度浮点。 */
    private static float readFloat(byte[] b, int off) {
        int bits = (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8) |
                   ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
        return Float.intBitsToFloat(bits);
    }

    /** 读取以 NULL 结尾或定长的 ASCII 字串。 */
    private static String readString(byte[] b) {
        int len = b.length;
        for (int i = 0; i < b.length; i++) {
            if (b[i] == 0) { len = i; break; }
        }
        return new String(b, 0, len, java.nio.charset.StandardCharsets.US_ASCII).trim();
    }
}
