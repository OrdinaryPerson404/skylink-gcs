package cn.edu.nuaa.gcs.comm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CommunicationService 单元测试（链路质量 / 帧计数 / 滑动窗口边界）。
 *
 * 使用内存 MockLink 替代真实 Socket 注入遥测数据，避免 TCP 连接竞争、
 * 端口冲突与平台差异，测试更稳定可复现。
 */
class CommunicationServiceTest {

    /** 内存 Link：测试代码通过 feed() 塞入数据，CommunicationService 从 read() 取出。 */
    private static class MockLink implements Link {
        final BlockingQueue<Byte> buf = new LinkedBlockingQueue<>(65536);
        volatile boolean open = false;
        @Override public void open() { open = true; }
        @Override public boolean isOpen() { return open; }
        @Override public void send(byte[] d) { if (!open) throw new IllegalStateException(); }
        @Override public void close() { open = false; buf.clear(); }
        @Override public byte[] read() throws InterruptedException {
            Byte first = buf.poll(50, TimeUnit.MILLISECONDS);
            if (first == null) return null;
            int more = buf.size();
            byte[] out = new byte[more + 1];
            out[0] = first;
            for (int i = 0; i < more; i++) {
                Byte b = buf.poll(10, TimeUnit.MILLISECONDS);
                if (b == null) {
                    // 剩余不足：截断数组
                    byte[] trim = new byte[i + 1];
                    System.arraycopy(out, 0, trim, 0, i + 1);
                    return trim;
                }
                out[i + 1] = b;
            }
            return out;
        }
        void feed(byte[] data) { for (byte b : data) buf.offer(b); }
    }

    private static int crc16(int crc, int data) {
        crc ^= data;
        for (int i = 0; i < 8; i++) {
            if ((crc & 1) != 0) crc = (crc >>> 1) ^ 0x8408;
            else crc = crc >>> 1;
        }
        return crc;
    }

    /** 构造一条合法 MAVLink v1 HEARTBEAT 帧（9 字节 payload + 8 字节头 CRC）。 */
    private static byte[] heartbeatFrame() {
        byte[] payload = new byte[9];
        payload[0] = (byte) 0xFF; // base_mode: 全标记
        payload[7] = 3;           // system_status = MAV_STATE_ACTIVE
        payload[8] = 2;           // custom_mode = STAB（CF-Drone）
        byte[] frame = new byte[payload.length + 8];
        frame[0] = (byte) 0xFE;
        frame[1] = (byte) payload.length;
        frame[2] = 0; frame[3] = 1; frame[4] = 1; frame[5] = 0; // HEARTBEAT msgId=0
        System.arraycopy(payload, 0, frame, 6, payload.length);
        int crc = 0xFFFF;
        for (int i = 1; i <= payload.length + 5; i++) crc = crc16(crc, frame[i] & 0xFF);
        crc = crc16(crc, 50); // HEARTBEAT CRC_EXTRA=50
        frame[6 + payload.length] = (byte) (crc & 0xFF);
        frame[7 + payload.length] = (byte) ((crc >> 8) & 0xFF);
        return frame;
    }

    @Test
    @DisplayName("未启动时 getLinkQualityPct 返回 -1")
    void testQualityNotStarted() {
        CommunicationService cs = new CommunicationService();
        assertEquals(-1, cs.getLinkQualityPct(), 1e-9, "未启动且无数据时质量应为 -1");
    }

    @Test
    @DisplayName("getTotalReceived 初始为 0，未 start 的 stop 不抛异常")
    void testTotalReceivedInitialAndStop() {
        CommunicationService cs = new CommunicationService();
        assertEquals(0, cs.getTotalReceived(), "初始累计应 0");
        assertDoesNotThrow(cs::stop);
    }

    @Test
    @DisplayName("isLinkActive 无数据时返回 false")
    void testLinkActiveWithoutData() {
        CommunicationService cs = new CommunicationService();
        assertFalse(cs.isLinkActive());
    }

    @Test
    @DisplayName("getLastReceivedMs 初始为 0")
    void testLastReceivedInitial() {
        CommunicationService cs = new CommunicationService();
        assertEquals(0, cs.getLastReceivedMs());
    }

    @Test
    @DisplayName("未设置 Link 时 start 抛 IllegalStateException")
    void testStartWithoutLink() {
        CommunicationService cs = new CommunicationService();
        assertThrows(IllegalStateException.class, cs::start);
    }

    @Test
    @DisplayName("未设置 Link 时 send 抛 IllegalStateException")
    void testSendWithoutLink() {
        CommunicationService cs = new CommunicationService();
        byte[] dummy = new byte[]{(byte) 0xFE, 0x01, 0x00, 1, 1, 0, 0, 0, 0};
        assertThrows(IllegalStateException.class, () -> cs.send(dummy));
    }

    @Test
    @DisplayName("链路质量：注入 HEARTBEAT 后 totalReceived、linkActive、质量范围均正确")
    void testLinkQualityViaMockLink() throws Exception {
        MockLink link = new MockLink();
        CommunicationService cs = new CommunicationService();
        cs.setLink(link);
        AtomicInteger count = new AtomicInteger(0);
        cs.setCallback(t -> count.incrementAndGet());
        cs.start();

        try {
            byte[] f = heartbeatFrame();
            // 连续注入 5 帧，每帧间隔 200ms（约 5Hz），模拟良好链路
            for (int i = 0; i < 5; i++) {
                link.feed(f);
                Thread.sleep(200);
            }
            // 等待监听线程完全消费
            Thread.sleep(300);

            assertTrue(count.get() >= 3,
                "至少应收到多帧 HEARTBEAT, 实际: " + count.get());
            assertTrue(cs.getTotalReceived() >= 3,
                "累计接收应 >= 3, 实际: " + cs.getTotalReceived());
            assertTrue(cs.isLinkActive(), "链路应为 active");
            assertTrue(cs.getLastReceivedMs() > 0, "lastReceivedMs 应已更新");

            double q = cs.getLinkQualityPct();
            assertTrue(q >= 0 && q <= 100, "链路质量应在 [0,100], 实际: " + q);
            // 5Hz 远超期望 1Hz，质量应截断为 100
            assertTrue(q > 50, "5Hz 注入质量应接近满值, 实际: " + q);
        } finally {
            cs.stop();
        }
    }

    @Test
    @DisplayName("stop 后累计与滑动窗口重置，再次启动不污染旧数据")
    void testStopResetsState() throws Exception {
        MockLink link = new MockLink();
        CommunicationService cs = new CommunicationService();
        cs.setLink(link);
        cs.start();
        try {
            link.feed(heartbeatFrame());
            Thread.sleep(250);
            assertTrue(cs.getTotalReceived() >= 1);
        } finally {
            cs.stop();
        }
        // 重启：totalReceived 不会自动清零（累计统计），但 lastReceivedMs/isLinkActive 应重新评估
        cs.setLink(new MockLink());
        cs.start();
        try {
            // 新链路未注入数据：质量应返回 -1（从未收到），或因 lastReceivedMs 未清零但 data=0 返回 0
            double q = cs.getLinkQualityPct();
            assertTrue(q == -1 || q == 0, "新窗口无数据应返回 0 或 -1, 实际: " + q);
        } finally {
            cs.stop();
        }
    }
}
