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

    /**
     * 宽松模式开关：true 时跳过 CRC 校验仍解码（用于诊断布局问题）。
     * 生产环境必须为 false（默认），确保数据完整性。
     */
    private volatile boolean lenient = false;

    public void setLenient(boolean v) { this.lenient = v; }
    public boolean isLenient() { return lenient; }

    public static class Telemetry {
        public boolean valid;
        public boolean crcValid;  // 真实 CRC 校验结果（lenient 模式下 valid=true 但 crcValid 可能 false）
        public int msgId;
        public int sysId, compId;

        // 位置（部分飞控/固件不发送 GLOBAL_POSITION_INT，例如 CF-Drone 无 GPS 上报）
        public double lat, lon, alt, relAlt, speed, heading;
        public boolean hasPosition;

        // GPS 原始数据（GPS_RAW_INT(24)）
        public int fixType;        // GPS_FIX_TYPE: 0=无定位 1=2D 2=3D 3=DGPS 4=RTK 5=RTK Float
        public int satellites;     // 可见卫星数
        public double hdop;        // 水平精度因子
        public double vdop;        // 垂直精度因子
        public double gpsSpeed;    // 地面速度 (m/s)
        public double gpsCog;      // 航迹方向 (deg)
        public boolean hasGpsRaw;

        // VFR_HUD(74)：提供 alt/speed/heading，不含 lat/lon
        public double vfrAlt, vfrSpeed, vfrHeading, vfrClimb;
        public int vfrThrottle;
        public boolean hasVfrHud;

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

        // 电池状态（来自 BATTERY_STATUS(147)）
        public int batteryId;
        public double batteryCurrent;       // A（cA → /100）
        public double batteryTemp;          // °C（centi-°C → /100）
        public int[] cellVoltages = new int[10]; // mV
        public double capacityConsumed;    // mAh
        public int batteryRemaining2;       // %（-1=未测量）
        public boolean hasBatteryStatus;

        // 状态文本（来自 STATUSTEXT(253)）
        public int statusSeverity;          // MAV_SEVERITY
        public String statusText;
        public boolean hasStatusText;

        // 高精度 IMU（来自 HIGHRES_IMU(105)）
        public double absPressure;          // hPa
        public double pressureAlt;          // m
        public double imuTemp2;             // °C
        public double airTemp;              // °C
        public double magX, magY, magZ;     // T（milli-T → /1000）
        public boolean hasHighresImu;
    }

    public static Telemetry parse(byte[] data, int offset, int len) {
        return new MavlinkParser().parseInstance(data, offset, len);
    }

    /** 实例方法：根据 lenient 标志决定 CRC 失败时是否仍解码。 */
    public Telemetry parseInstance(byte[] data, int offset, int len) {
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
                boolean crcOk = verifyCrc(data, p, 5, payloadLen, msgId);
                if (crcOk || lenient) {
                    t.crcValid = crcOk;
                    t.valid = true;  // lenient 模式下也置 valid=true，确保 feed() 不丢弃
                    t.sysId = data[p + 3] & 0xFF;
                    t.compId = data[p + 4] & 0xFF;
                    t.msgId = msgId;
                    decodeMessage(msgId, payload, t);
                    p += totalLen;
                } else {
                    p++; // CRC 校验失败，逐字节重新同步
                }
            } else if (stx == STX_V2) {
                // 标准 MAVLink v2: STX(1) LEN(1) INCOMPAT(1) COMPAT(1) SEQ(1) SYSID(1) COMPID(1) MSGID(3) PAYLOAD(N) CRC(2) [SIG(13)]
                if (p + 10 > end) break;
                int payloadLen = data[p + 1] & 0xFF;            // LEN 为 1 字节
                int incompatFlags = data[p + 2] & 0xFF;
                int msgId = (data[p + 7] & 0xFF) | ((data[p + 8] & 0xFF) << 8) | ((data[p + 9] & 0xFF) << 16);
                int signatureLen = (incompatFlags & 0x01) != 0 ? 13 : 0; // bit0=MAVLINK_IFLAG_SIGNED
                int totalLen = 10 + payloadLen + 2 + signatureLen;
                if (p + totalLen > end) { p++; continue; }
                byte[] payload = new byte[payloadLen];
                System.arraycopy(data, p + 10, payload, 0, payloadLen);
                boolean crcOk = verifyCrc(data, p, 9, payloadLen, msgId);
                if (crcOk || lenient) {
                    t.crcValid = crcOk;
                    t.valid = true;  // lenient 模式下也置 valid=true，确保 feed() 不丢弃
                    t.sysId = data[p + 5] & 0xFF;
                    t.compId = data[p + 6] & 0xFF;
                    t.msgId = msgId;
                    decodeMessage(msgId, payload, t);
                    p += totalLen;
                } else {
                    p++; // CRC 校验失败，逐字节重新同步
                }
            } else {
                p++;
            }
        }
        return t;
    }

    // ============================================================
    // 有状态流式解析：解决串口/TCP 分块到达导致帧跨块被截断的问题，
    // 并保证一次 feed 返回缓冲区内【所有】完整帧（parse 只返回最后一帧）。
    // ============================================================
    private final java.io.ByteArrayOutputStream rxBuf = new java.io.ByteArrayOutputStream(8192);

    /** 累积接收数据并解析，返回其中所有完整有效的帧（按到达顺序）。 */
    public java.util.List<Telemetry> feed(byte[] data, int len) {
        rxBuf.write(data, 0, len);
        byte[] buf = rxBuf.toByteArray();
        java.util.List<Telemetry> out = new java.util.ArrayList<>();
        int p = 0;
        int end = buf.length;
        int lastConsumed = 0;
        while (p < end) {
            int stx = buf[p] & 0xFF;
            if (stx != STX_V1 && stx != STX_V2) { p++; lastConsumed = p; continue; }
            // 依据头部长度字段计算完整帧长（数据不足则等待更多字节）
            Integer frameLen = peekFrameLength(buf, p, end);
            if (frameLen == null) break;              // 帧头不完整，等待下一块
            if (p + frameLen > end) break;            // 帧体不完整，保留剩余字节
            Telemetry t = parseInstance(buf, p, frameLen);
            if (t.valid) {
                out.add(t);
                p += frameLen;      // 完整消费该帧
            } else {
                p++;                // CRC 失败：与 parse 一致，逐字节重新同步
            }
            lastConsumed = p;
        }
        // 压缩缓冲区：丢弃已消费部分；防止垃圾数据导致无限增长
        if (lastConsumed > 0) {
            byte[] remaining = new byte[end - lastConsumed];
            System.arraycopy(buf, lastConsumed, remaining, 0, remaining.length);
            rxBuf.reset();
            rxBuf.write(remaining, 0, remaining.length);
        } else if (buf.length > 65536) {
            rxBuf.reset();  // 无同步点且超限，丢弃（链路噪声场景）
        }
        return out;
    }

    /**
     * 依据 STX 后的头部字段计算帧总长。
     * @return 帧总长；头部字节尚不完整时返回 null
     */
    private Integer peekFrameLength(byte[] buf, int p, int end) {
        int stx = buf[p] & 0xFF;
        if (stx == STX_V1) {
            if (p + 2 > end) return null;                    // LEN 未到齐
            int payloadLen = buf[p + 1] & 0xFF;
            return payloadLen + 8;
        } else { // STX_V2
            if (p + 2 > end) return null;                    // LEN 未到齐
            int payloadLen = buf[p + 1] & 0xFF;
            int incompatFlags = buf[p + 2] & 0xFF;           // 需 p+3 字节，读前先判
            if (p + 3 > end) return null;
            int sigLen = (incompatFlags & 0x01) != 0 ? 13 : 0;
            return 10 + payloadLen + 2 + sigLen;
        }
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
                if (payload.length >= 10) {
                    int vRaw = (int) readUint16(payload, 8);
                    t.voltage = (vRaw == 0xFFFF) ? 0 : vRaw / 1000.0;  // mV → V（UINT16_MAX=未知）
                }
                if (payload.length >= 12) {
                    int cRaw = readInt16(payload, 10);
                    t.batteryCurrent = (cRaw == -1) ? 0 : cRaw / 100.0;  // 10mA → A（-1=未知）
                }
                if (payload.length >= 13) {
                    int pctRaw = payload[12] & 0xFF;
                    t.batteryPct = (pctRaw == 0xFF) ? -1 : pctRaw;  // %（-1=未知）
                }
                if (payload.length >= 13)
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
            case 24: // GPS_RAW_INT（标准 30 字节，线上布局按字段尺寸降序排列，与 c_library 生成结构体一致）
                // time_usec u64@0, lat i32@8, lon i32@12, alt i32@16,
                // eph u16@20, epv u16@22, vel u16@24, cog u16@26, fix_type u8@28, satellites_visible u8@29
                if (payload.length >= 30) {
                    long latRaw = readInt32(payload, 8);
                    long lonRaw = readInt32(payload, 12);
                    if (latRaw != 0 || lonRaw != 0) {
                        t.lat = latRaw / 1e7;
                        t.lon = lonRaw / 1e7;
                    }
                    t.alt = readInt32(payload, 16) / 1000.0;  // mm → m
                    int eph = (int) readUint16(payload, 20);
                    t.hdop = (eph == 0xFFFF) ? 0 : eph / 100.0;
                    int epv = (int) readUint16(payload, 22);
                    t.vdop = (epv == 0xFFFF) ? 0 : epv / 100.0;
                    int vel = (int) readUint16(payload, 24);
                    t.gpsSpeed = (vel == 0xFFFF) ? 0 : vel / 100.0;   // cm/s → m/s
                    int cog = (int) readUint16(payload, 26);
                    t.gpsCog = (cog == 0xFFFF) ? 0 : cog / 100.0;     // cdeg → deg
                    t.fixType = payload[28] & 0xFF;
                    t.satellites = payload[29] & 0xFF;                // UINT8_MAX=未知
                    t.hasGpsRaw = true;
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
            case 33: // GLOBAL_POSITION_INT（标准 common.xml 布局，28 字节）
                // time_boot_ms u32@0, lat i32@4, lon i32@8, alt i32@12, relative_alt i32@16,
                // vx i16@20, vy i16@22, vz i16@24, hdg u16@26
                if (payload.length >= 28) {
                    t.lat = readInt32(payload, 4) / 1e7;
                    t.lon = readInt32(payload, 8) / 1e7;
                    t.alt = readInt32(payload, 12) / 1000.0;       // mm → m
                    t.relAlt = readInt32(payload, 16) / 1000.0;    // mm → m（相对高度）
                    t.speed = Math.sqrt(
                        Math.pow(readInt16(payload, 20) / 100.0, 2) +   // cm/s → m/s
                        Math.pow(readInt16(payload, 22) / 100.0, 2));
                    int hdgRaw = (int) readUint16(payload, 26);        // cdeg
                    t.heading = (hdgRaw == 0xFFFF) ? 0 : hdgRaw / 100.0;
                    t.hasPosition = true;
                }
                break;
            case 35: // RC_CHANNELS_RAW（标准 21 字节：time_boot_ms u32 + chan1-8 u16×8 + rssi u8）
                if (payload.length >= 20) {
                    for (int i = 0; i < 8; i++) {
                        if (payload.length >= 6 + i * 2)
                            t.rcChannels[i] = readUint16(payload, 4 + i * 2);
                    }
                    if (payload.length >= 21)
                        t.rssi = payload[20] & 0xFF;
                    t.hasRc = true;
                }
                break;
            case 44: // MISSION_COUNT（标准：target_system u8@0 target_component u8@1 count u16@2；CF-Drone 恒为 0）
                if (payload.length >= 2) {
                    // count=0 时 v2 载荷截断可能只剩 2~3 字节，此时 count 视为 0
                    t.missionCount = (payload.length >= 4) ? readUint16(payload, 2) : 0;
                    t.hasMissionCount = true;
                }
                break;
            case 74: // VFR_HUD（标准 20 字节：airspeed f32@0 groundspeed f32@4 alt f32@8 climb f32@12 heading i16@16 throttle u16@18）
                if (payload.length >= 18) {
                    t.vfrSpeed = readFloat(payload, 4);     // 地面速度 (m/s)
                    t.vfrHeading = readInt16(payload, 16);  // 航向 (deg, 0-360)
                    if (payload.length >= 8) t.vfrAlt = readFloat(payload, 8);       // 高度 MSL (m)
                    if (payload.length >= 16) t.vfrClimb = readFloat(payload, 12);  // 爬升率 (m/s)
                    if (payload.length >= 20) t.vfrThrottle = (int) readUint16(payload, 18);  // 油门 %
                    t.hasVfrHud = true;
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
            case 147: // BATTERY_STATUS（标准 common.xml 布局，共 31 字节）
                // battery_function@0(u8) type@1(u8) temperature@2(i16 centi-°C)
                // voltages[10]@4(u16 mV) current_battery@24(i16 cA) consumed@26(u32 mAh) energy_remaining@30(i8 %)
                if (payload.length >= 4) {
                    t.batteryId = payload[0] & 0xFF;
                    if (payload.length >= 4) {
                        int tempRaw = readInt16(payload, 2);
                        t.batteryTemp = (tempRaw == Short.MIN_VALUE || tempRaw < -100) ? 0 : tempRaw / 100.0;  // centi-°C → °C（INT16_MIN=未知）
                    }
                    for (int i = 0; i < 10; i++) {
                        if (payload.length >= 6 + i * 2) {
                            int mv = (int) readUint16(payload, 4 + i * 2);
                            t.cellVoltages[i] = (mv >= 0xFFFF) ? 0 : mv;  // UINT16_MAX=未使用
                        }
                    }
                    if (payload.length >= 26) {
                        int curRaw = readInt16(payload, 24);
                        t.batteryCurrent = (curRaw == -1) ? 0 : curRaw / 100.0;  // cA → A（-1=未知）
                    }
                    if (payload.length >= 30) {
                        long consRaw = readUint32(payload, 26);
                        t.capacityConsumed = (consRaw == 0xFFFFFFFFL) ? 0 : consRaw;  // mAh（UINT32_MAX=未知）
                    }
                    if (payload.length >= 31)
                        t.batteryRemaining2 = payload[30];             // %（int8，-1=未测量）
                    // CF-Drone 无电池传感器时回传全 0xFF：id=255/temp=INT16_MIN/consumed=UINT32_MAX/remaining=-1
                    // 此时 hasBatteryStatus 仍置 true（消息确实收到），上层可通过 remaining<0 判断数据无效
                    t.hasBatteryStatus = true;
                }
                break;
            case 253: // STATUSTEXT（severity u8 + text char[50]，v2 截断后文本可能不足 50）
                if (payload.length >= 2) {
                    t.statusSeverity = payload[0] & 0xFF;
                    int txtLen = Math.min(50, payload.length - 1);
                    byte[] txt = new byte[txtLen];
                    System.arraycopy(payload, 1, txt, 0, txtLen);
                    t.statusText = readString(txt);
                    t.hasStatusText = true;
                }
                break;
            case 105: // HIGHRES_IMU（标准：time_usec u64@0 + float 字段组；v2 截断容错）
                // xacc@8 yacc@12 zacc@16 xgyro@20 ygyro@24 zgyro@28 xmag@32 ymag@36 zmag@40
                // abs_pressure@44 diff_pressure@48 pressure_alt@52 altitude@56 temperature@60 (float °C)
                if (payload.length >= 12) {
                    if (payload.length >= 44) {  // zmag@40..43 完整
                        t.magX = readFloat(payload, 32);  // mT
                        t.magY = readFloat(payload, 36);
                        t.magZ = readFloat(payload, 40);
                    }
                    if (payload.length >= 48)
                        t.absPressure = readFloat(payload, 44);   // hPa
                    if (payload.length >= 56)
                        t.pressureAlt = readFloat(payload, 52);   // m
                    if (payload.length >= 64)
                        t.imuTemp2 = readFloat(payload, 60);      // °C
                    t.hasHighresImu = true;
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
     * CRC16-X25 校验。同时支持 v1（STX 后 5 字节头：LEN SEQ SYSID COMPID MSGID）
     * 与 v2（STX 后 9 字节头：LEN INCOMPAT COMPAT SEQ SYSID COMPID MSGID×3）。
     * @param stxOffset  STX 字节偏移
     * @param headerCrcBytes STX 之后参与 CRC 的头字节数（v1=5, v2=9）
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
            case 24  -> 87;   // GPS_RAW_INT
            case 26  -> 170;  // SCALED_IMU
            case 31  -> 246;  // ATTITUDE_QUATERNION
            case 33  -> 104;  // GLOBAL_POSITION_INT
            case 35  -> 244;  // RC_CHANNELS_RAW
            case 44  -> 221;  // MISSION_COUNT
            case 74  -> 20;   // VFR_HUD
            case 76  -> 152;  // COMMAND_LONG
            case 77  -> 143;  // COMMAND_ACK
            case 126 -> 220;  // SERIAL_CONTROL
            case 140 -> 181;  // ACTUATOR_CONTROL_TARGET
            case 147 -> 117;  // BATTERY_STATUS
            case 105 -> 97;   // HIGHRES_IMU
            case 245 -> 130;  // EXTENDED_SYS_STATE
            case 253 -> 83;   // STATUSTEXT
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
