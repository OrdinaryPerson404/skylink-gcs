package com.cherglow.gcs.protocol.mavlink;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * MAVLink 精简编解码（对齐 CF-Drone 固件 mavlink.ino / c_library_v2）：
 * 支持 v1（0xFE）与 v2（0xFD，含尾零截断的零填充语义）帧解析；下行帧用 v1 构建。
 * CRC-16/MCRF4XX + 每消息 CRC_EXTRA；extras 取自 pymavlink 2.4.49 对 common.xml 的权威计算，
 * 解析失败时对已知 msgid 做 0..255 自适应学习（防固件库方言版本差异，如旧 ATTITUDE_QUATERNION 无
 * repr_offset / 旧 COMMAND_ACK 3 字节）。
 * 坐标口径：固件把姿态转成 FRD 上行；本类只管字节，FRD→FLU 转换在 MavLinkLink。
 * GCS：sysid 255 / compid 190；飞控：sysid 1 / compid 1（MAV_COMP_ID_AUTOPILOT1）。
 */
public final class MavLink {

    public static final int SYSID_GCS = 255;
    public static final int COMPID_GCS = 190;

    // 本工程用到的消息（extras 表键）
    public static final int MSG_HEARTBEAT = 0;
    public static final int MSG_PARAM_REQUEST_READ = 20;
    public static final int MSG_PARAM_REQUEST_LIST = 21;
    public static final int MSG_PARAM_VALUE = 22;
    public static final int MSG_PARAM_SET = 23;
    public static final int MSG_ATTITUDE_QUATERNION = 31;
    public static final int MSG_RC_CHANNELS_RAW = 35;
    public static final int MSG_MANUAL_CONTROL = 69;
    public static final int MSG_COMMAND_LONG = 76;
    public static final int MSG_COMMAND_ACK = 77;
    public static final int MSG_ACTUATOR_CONTROL_TARGET = 140;
    public static final int MSG_SCALED_IMU = 26;
    public static final int MSG_LOG_DATA = 119;
    public static final int MSG_SERIAL_CONTROL = 126;
    public static final int MSG_AUTOPILOT_VERSION = 148;
    public static final int MSG_EXTENDED_SYS_STATE = 245;
    public static final int MSG_SCALED_PRESSURE = 29;
    public static final int MSG_HYGROMETER_SENSOR = 12920;

    /** 现代 c_library_v2/common.xml 的 CRC_EXTRA（pymavlink 2.4.49 权威计算） */
    private static final Map<Integer, Integer> CRC_EXTRA = Map.ofEntries(
            Map.entry(MSG_HEARTBEAT, 50),
            Map.entry(MSG_PARAM_REQUEST_READ, 214),
            Map.entry(MSG_PARAM_REQUEST_LIST, 159),
            Map.entry(MSG_PARAM_VALUE, 220),
            Map.entry(MSG_PARAM_SET, 168),
            Map.entry(MSG_ATTITUDE_QUATERNION, 246),
            Map.entry(MSG_RC_CHANNELS_RAW, 244),
            Map.entry(MSG_MANUAL_CONTROL, 243),
            Map.entry(MSG_COMMAND_LONG, 152),
            Map.entry(MSG_COMMAND_ACK, 143),
            Map.entry(MSG_ACTUATOR_CONTROL_TARGET, 181),
            Map.entry(MSG_SCALED_IMU, 170),
            Map.entry(MSG_LOG_DATA, 134),
            Map.entry(MSG_SERIAL_CONTROL, 189),
            Map.entry(MSG_AUTOPILOT_VERSION, 178),
            Map.entry(MSG_EXTENDED_SYS_STATE, 130),
            Map.entry(MSG_SCALED_PRESSURE, 115),
            Map.entry(MSG_HYGROMETER_SENSOR, 20));

    /** 运行时学习到的 extras（自适应固件库方言差异；仅对表内 msgid 学习，防 UDP 噪声污染） */
    private static final Map<Integer, Integer> LEARNED = new ConcurrentHashMap<>();

    private MavLink() {
    }

    public static int knownExtra(int msgid) {
        Integer e = CRC_EXTRA.get(msgid);
        return e != null ? e : 0;
    }

    static int extraFor(int msgid) {
        Integer l = LEARNED.get(msgid);
        if (l != null) {
            return l;
        }
        return knownExtra(msgid);
    }

    /** CRC-16/MCRF4XX（增量），初始 0xFFFF */
    public static int crc(int crc, byte[] buf, int off, int len) {
        for (int i = off; i < off + len; i++) {
            int tmp = (buf[i] & 0xFF) ^ (crc & 0xFF);
            tmp = (tmp ^ (tmp << 4)) & 0xFF;
            crc = ((crc >> 8) ^ (tmp << 8) ^ (tmp << 3) ^ (tmp >> 4)) & 0xFFFF;
        }
        return crc;
    }

    // ---- 帧构建（下行统一 v1） ----

    private static int seq;

    /** 构建完整 v1 帧（GCS 源）；extra 取 knownExtra（下行必须与固件方言一致） */
    public static byte[] buildV1(int msgid, byte[] payload) {
        return buildV1(msgid, payload, knownExtra(msgid), nextSeq(), SYSID_GCS, COMPID_GCS);
    }

    /** 指定 extra 的 v1 帧（GCS 源、自动序号） */
    public static byte[] buildV1(int msgid, byte[] payload, int extra) {
        return buildV1(msgid, payload, extra, nextSeq(), SYSID_GCS, COMPID_GCS);
    }

    public static byte[] buildV1(int msgid, byte[] payload, int extra, int seq, int sysid, int compid) {
        byte[] out = new byte[6 + payload.length + 2]; // magic+len+seq+sys+comp+msgid 6B + payload + crc2B
        out[0] = (byte) 0xFE;
        out[1] = (byte) payload.length;
        out[2] = (byte) seq;
        out[3] = (byte) sysid;
        out[4] = (byte) compid;
        out[5] = (byte) msgid;
        System.arraycopy(payload, 0, out, 6, payload.length);
        int c = crcWithExtra(0xFFFF, out, 1, out.length - 3, extra);
        out[out.length - 2] = (byte) (c & 0xFF);
        out[out.length - 1] = (byte) ((c >> 8) & 0xFF);
        return out;
    }

    /** CRC-16/MCRF4XX 累加 + CRC_EXTRA 作为字节累加（与 pymavlink mavcrc.accumulate 语义一致，非移位异或） */
    public static int crcWithExtra(int crc, byte[] buf, int off, int len, int extra) {
        crc = crc(crc, buf, off, len);
        return crc(crc, new byte[]{(byte) extra}, 0, 1);
    }

    private static synchronized int nextSeq() {
        return seq++ & 0xFF;
    }

    // ---- 下行命令载荷（MavLinkLink 与黄金帧单测共用，保证编码可验证） ----

    // 线上字段序 = MAVLink v1.0+ 尺寸降序稳定排序（大字段在前），偏移与 pymavlink 权威导出一致

    /** GCS 心跳：custom_mode u32 → type/autopilot/base_mode/system_status/mavlink_version u8 */
    public static byte[] payloadHeartbeatGcs() {
        return new PBuf().u32(0).u8(6).u8(0).u8(0).u8(0).u8(3).toArray();
    }

    /** COMMAND_LONG：param1..7 f32 → command u16 → target_system/target_component/confirmation u8 */
    public static byte[] payloadCommandLong(int command, float param1, float param2) {
        return new PBuf()
                .f32(param1).f32(param2).f32(0).f32(0).f32(0).f32(0).f32(0)
                .u16(command)
                .u8(1).u8(1).u8(0)
                .toArray();
    }

    /** MANUAL_CONTROL（11B 经典布局，extra 243 与现代 30B 一致，新旧固件库通吃）：x/y/z/r s16 → buttons u16 → target u8 */
    public static byte[] payloadManualControl(int x, int y, int z, int r, int buttons, int target) {
        return new PBuf().s16(x).s16(y).s16(z).s16(r).u16(buttons).u8(target).toArray();
    }

    public static byte[] payloadParamRequestList() {
        return new PBuf().u8(1).u8(1).toArray();
    }

    /** PARAM_REQUEST_READ：param_index s16 → targets u8 → param_id[16] */
    public static byte[] payloadParamRequestRead(String id, int index) {
        return new PBuf().s16(index).u8(1).u8(1).ascii(id, 16).toArray();
    }

    /** PARAM_SET：value f32 → targets u8 → param_id[16] → type u8(REAL32=9) */
    public static byte[] payloadParamSet(String id, float value) {
        return new PBuf().f32(value).u8(1).u8(1).ascii(id, 16).u8(9).toArray();
    }

    /** SERIAL_CONTROL 旧库 79B 布局的 CRC_EXTRA（无 targets） */
    public static final int SERIAL_CONTROL_EXTRA_LEGACY = 220;

    /** 运行时学习到的 CRC_EXTRA（无则 null），供通道层选择下行布局 */
    public static Integer learnedExtra(int msgid) {
        return LEARNED.get(msgid);
    }

    /** SERIAL_CONTROL(SHELL，固件 81B 布局带 targets)：baudrate u32 → timeout u16 → device/flags/count u8 → data[70] → targets */
    public static byte[] payloadSerialControlShell(String cmd) {
        byte[] data = cmd.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        int count = Math.min(data.length, 70);
        PBuf pb = new PBuf().u32(0).u16(0).u8(10).u8(0).u8(count);
        pb.bytes(data, count);
        for (int i = count; i < 70; i++) {
            pb.u8(0);
        }
        return pb.u8(1).u8(1).toArray();
    }

    // ---- 载荷读写 ----

    /** 小端载荷构建器（255 字节上限） */
    public static final class PBuf {
        private final byte[] b = new byte[255];
        private int n;

        // ---- 面向测试的静态写入方法 ----

        /** 小端写 float */
        public static void packFloat(byte[] dst, int off, float v) {
            int bits = Float.floatToIntBits(v);
            for (int i = 0; i < 4; i++) {
                dst[off + i] = (byte) (bits >> (8 * i));
            }
        }

        /** 小端写 signed short */
        public static void packShort(byte[] dst, int off, short v) {
            dst[off] = (byte) v;
            dst[off + 1] = (byte) (v >> 8);
        }

        /** 小端写 unsigned short (0~65535) */
        public static void packUShort(byte[] dst, int off, short v) {
            dst[off] = (byte) v;
            dst[off + 1] = (byte) (v >> 8);
        }

        public PBuf u8(int v) {
            b[n++] = (byte) v;
            return this;
        }

        public PBuf u16(int v) {
            b[n++] = (byte) v;
            b[n++] = (byte) (v >> 8);
            return this;
        }

        public PBuf u32(long v) {
            for (int i = 0; i < 4; i++) {
                b[n++] = (byte) (v >> (8 * i));
            }
            return this;
        }

        public PBuf s16(int v) {
            return u16(v);
        }

        public PBuf f32(float v) {
            return u32(Float.floatToIntBits(v));
        }

        public PBuf ascii(String s, int fixed) {
            byte[] d = (s == null ? "" : s).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            for (int i = 0; i < fixed; i++) {
                b[n++] = i < d.length ? d[i] : 0;
            }
            return this;
        }

        public PBuf bytes(byte[] d, int len) {
            for (int i = 0; i < len; i++) {
                b[n++] = d[i];
            }
            return this;
        }

        public int size() {
            return n;
        }

        public byte[] toArray() {
            byte[] r = new byte[n];
            System.arraycopy(b, 0, r, 0, n);
            return r;
        }
    }

    // 载荷读取（buf 语义为 v2 零填充后的 255 字节）
    public static int rU8(byte[] p, int off) {
        return p[off] & 0xFF;
    }

    public static int rU16(byte[] p, int off) {
        return (p[off] & 0xFF) | ((p[off + 1] & 0xFF) << 8);
    }

    public static long rU32(byte[] p, int off) {
        long v = 0;
        for (int i = 3; i >= 0; i--) {
            v = (v << 8) | (p[off + i] & 0xFFL);
        }
        return v;
    }

    public static int rS16(byte[] p, int off) {
        return (short) rU16(p, off);
    }

    public static float rF32(byte[] p, int off) {
        return Float.intBitsToFloat((int) rU32(p, off));
    }

    public static String rAscii(byte[] p, int off, int len) {
        int end = off;
        for (int i = off; i < off + len; i++) {
            if (p[i] == 0) {
                break;
            }
            end = i + 1;
        }
        return new String(p, off, end - off, java.nio.charset.StandardCharsets.US_ASCII);
    }

    // ---- 流式帧解析器 ----

    /** 解析产出：msgid + 零填充到 255 字节的载荷 + 源地址信息 */
    public static final class Frame {
        public final int msgid;
        public final int sysid;
        public final int compid;
        public final byte[] payload; // 255 字节（v2 截断部分为零填充）

        Frame(int msgid, int sysid, int compid, byte[] payload) {
            this.msgid = msgid;
            this.sysid = sysid;
            this.compid = compid;
            this.payload = payload;
        }
    }

    /** 字节流状态机；v1/v2 自适应；CRC 校验失败时对表内 msgid 做 0..255 学习 */
    public static final class Parser {
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        private int state = 0; // 0=找 magic，>0=已收字节数
        private int proto, payloadLen, msgid, sysid, compid, incompat;
        private final byte[] payload255 = new byte[255];

        public void feed(byte[] data, int off, int len, Consumer<Frame> out) {
            for (int i = off; i < off + len; i++) {
                step(data[i] & 0xFF, out);
            }
        }

        private void step(int b, Consumer<Frame> out) {
            if (state == 0) {
                if (b == 0xFE || b == 0xFD) {
                    buf.reset();
                    buf.write(b);
                    proto = b;
                    state = 1;
                }
                return;
            }
            buf.write(b);
            int n = buf.size();
            byte[] a = buf.toByteArray();
            if (proto == 0xFE) {
                if (state <= 5) {
                    if (state == 1) {
                        payloadLen = b; // len
                    }
                    if (state == 5) {
                        msgid = b;
                    }
                    state++;
                    return;
                }
                if (n < 6 + payloadLen + 2) {
                    return;
                }
                int extra = extraFor(msgid);
                int c = crcWithExtra(0xFFFF, a, 1, n - 3, extra);
                int want = (a[n - 2] & 0xFF) | ((a[n - 1] & 0xFF) << 8);
                if (c != want && CRC_EXTRA.containsKey(msgid)) {
                    int learned = learnV1(a, n, msgid, want);
                    if (learned < 0) {
                        reset();
                        return;
                    }
                } else if (c != want) {
                    reset();
                    return;
                }
                fill(a, 6, payloadLen);
                out.accept(new Frame(msgid, a[3] & 0xFF, a[4] & 0xFF, payload255));
                reset();
            } else { // 0xFD
                if (state == 1) {
                    payloadLen = b;
                } else if (state == 2) {
                    incompat = b;
                }
                state++;
                if (n < 10 + payloadLen + 2) {
                    return;
                }
                if ((incompat & 0x01) != 0) {
                    reset(); // 签名帧不支持
                    return;
                }
                sysid = a[5] & 0xFF;
                compid = a[6] & 0xFF;
                msgid = rU16(a, 7) | ((a[9] & 0xFF) << 16);
                int extra = extraFor(msgid);
                int c = crcWithExtra(0xFFFF, a, 1, n - 3, extra);
                int want = (a[n - 2] & 0xFF) | ((a[n - 1] & 0xFF) << 8);
                if (c != want && CRC_EXTRA.containsKey(msgid)) {
                    int learned = learnV2(a, n, msgid, want);
                    if (learned < 0) {
                        reset();
                        return;
                    }
                } else if (c != want) {
                    reset();
                    return;
                }
                fill(a, 10, payloadLen);
                out.accept(new Frame(msgid, sysid, compid, payload255));
                reset();
            }
        }

        private void fill(byte[] a, int off, int len) {
            System.arraycopy(a, off, payload255, 0, len);
            if (len < 255) {
                java.util.Arrays.fill(payload255, len, 255, (byte) 0);
            }
        }

        private void reset() {
            state = 0;
            buf.reset();
            incompat = 0;
        }

        /** CRC 不匹配时对已知 msgid 穷举 0..255 学习真实 extra；失败返回 -1 */
        private int learnV1(byte[] a, int n, int msgid, int want) {
            return learn(a, n, msgid, want, 1, a[1] & 0xFF);
        }

        private int learnV2(byte[] a, int n, int msgid, int want) {
            return learn(a, n, msgid, want, 10, a[1] & 0xFF);
        }

        private int learn(byte[] a, int n, int msgid, int want, int payloadOff, int payloadLen) {
            for (int e = 0; e < 256; e++) {
                int c = crcWithExtra(0xFFFF, a, 1, n - 3, e);
                if (c == want) {
                    LEARNED.put(msgid, e);
                    return e;
                }
            }
            return -1;
        }
    }
}
