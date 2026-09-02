package cn.edu.nuaa.gcs.comm;

public class MavlinkParser {
    private static final int FRAME_START = 0xFE;

    public static class Telemetry {
        public boolean valid;
        public int msgId;
        public double lat, lon, alt, speed, heading;
        public double voltage, batteryPct;
        public int systemStatus;
        public double roll, pitch, yaw;
    }

    public static Telemetry parse(byte[] data, int offset, int len) {
        Telemetry t = new Telemetry();
        if (len < 8) return t;
        int p = offset;
        int end = offset + len;
        while (p < end - 7) {
            if ((data[p] & 0xFF) != FRAME_START) { p++; continue; }
            int payloadLen = data[p + 1] & 0xFF;
            int totalLen = payloadLen + 8;
            if (p + totalLen > end) break;
            int seq = data[p + 2] & 0xFF;
            int sysId = data[p + 3] & 0xFF;
            int compId = data[p + 4] & 0xFF;
            int msgId = data[p + 5] & 0xFF;
            byte[] payload = new byte[payloadLen];
            System.arraycopy(data, p + 6, payload, 0, payloadLen);
            if (!verifyCrc(data, p, payloadLen, msgId)) { p++; continue; }
            t.valid = true;
            t.msgId = msgId;
            decodeMessage(msgId, payload, t);
            p += totalLen;
        }
        return t;
    }

    private static void decodeMessage(int msgId, byte[] payload, Telemetry t) {
        switch (msgId) {
            case 0:
                if (payload.length >= 7)
                    t.systemStatus = payload[6] & 0xFF;
                break;
            case 1:
                if (payload.length >= 2)
                    t.voltage = readUint16(payload, 0) / 1000.0;
                if (payload.length >= 11)
                    t.batteryPct = payload[10] & 0xFF;
                break;
            case 33:
                if (payload.length >= 28) {
                    t.lat = readInt32(payload, 0) / 1e7;
                    t.lon = readInt32(payload, 4) / 1e7;
                    t.alt = readInt32(payload, 10) / 1000.0;
                    t.speed = Math.sqrt(
                        Math.pow(readInt16(payload, 14) / 100.0, 2) +
                        Math.pow(readInt16(payload, 16) / 100.0, 2));
                    t.heading = (payload[20] & 0xFF) * 2.0;
                }
                break;
            case 74:
                if (payload.length >= 4) {
                    t.speed = readUint16(payload, 0) / 100.0;
                    t.heading = readUint16(payload, 2) / 100.0;
                }
                break;
        }
    }

    private static boolean verifyCrc(byte[] data, int offset, int payloadLen, int msgId) {
        int crc = 0xFFFF;
        // 从 LEN 字节开始（offset+1），计算到 PAYLOAD 末尾
        for (int i = 1; i <= payloadLen + 5; i++) {
            crc = crc16Step(crc, data[offset + i] & 0xFF);
        }
        // 附加 MSGID 对应的 CRC_EXTRA 字节
        int extra = getCrcExtra(msgId);
        if (extra >= 0) {
            crc = crc16Step(crc, extra);
        }
        // 校验帧尾两字节 CRC（小端）
        int crcLow = data[offset + 6 + payloadLen] & 0xFF;
        int crcHigh = data[offset + 7 + payloadLen] & 0xFF;
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
     * MAVLink v1 各报文的 CRC_EXTRA 值（用于校验不同报文结构）
     * 仅包含系统支持的 4 类报文，其余返回 -1 表示跳过 CRC_EXTRA
     */
    private static int getCrcExtra(int msgId) {
        return switch (msgId) {
            case 0  -> 50;   // HEARTBEAT
            case 1  -> 124;  // SYS_STATUS
            case 33 -> 104;  // GLOBAL_POSITION_INT
            case 74 -> 117;  // VFR_HUD
            default -> -1;
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
        // 符号扩展：32 位有符号数提升为 long 时保留负号
        if ((val & 0x80000000L) != 0)
            val |= 0xFFFFFFFF00000000L;
        return val;
    }

    private static long readUint32(byte[] b, int off) {
        return (b[off] & 0xFFL) | ((b[off + 1] & 0xFFL) << 8) |
               ((b[off + 2] & 0xFFL) << 16) | ((b[off + 3] & 0xFFL) << 24);
    }
}
