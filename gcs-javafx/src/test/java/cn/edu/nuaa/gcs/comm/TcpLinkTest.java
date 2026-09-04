package cn.edu.nuaa.gcs.comm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TcpLink 单元测试。
 *
 * 注意：真实 socket 连接依赖无人机 WiFi 环境，不在单元测试中建立；
 * 此处仅验证构造、默认值、状态机与 close 行为，避免测试依赖网络。
 * 端到端连接验证在 P8 阶段手动进行。
 */
class TcpLinkTest {

    @Test
    @DisplayName("默认网关地址构造正确")
    void testDefaultHostConstructor() {
        TcpLink link = new TcpLink(5760);
        assertEquals(TcpLink.DEFAULT_HOST, link.getHost(), "默认 host 应为 192.168.4.1");
        assertEquals(5760, link.getPort(), "端口应保持传入值");
    }

    @Test
    @DisplayName("自定义 host:port 构造正确")
    void testCustomConstructor() {
        TcpLink link = new TcpLink("10.0.0.2", 14550);
        assertEquals("10.0.0.2", link.getHost());
        assertEquals(14550, link.getPort());
    }

    @Test
    @DisplayName("未 open 时 isOpen 返回 false")
    void testNotOpen() {
        TcpLink link = new TcpLink(5760);
        assertFalse(link.isOpen(), "未 open 的链路 isOpen 应为 false");
    }

    @Test
    @DisplayName("close 后状态安全，重复 close 不抛异常")
    void testCloseIdempotent() {
        TcpLink link = new TcpLink(5760);
        assertDoesNotThrow(link::close, "未 open 直接 close 不应抛异常");
        assertDoesNotThrow(link::close, "重复 close 不应抛异常");
        assertFalse(link.isOpen(), "close 后 isOpen 应为 false");
    }

    @Test
    @DisplayName("未 open 时 read 返回 null 而非抛异常")
    void testReadReturnsNullWhenNotOpen() {
        TcpLink link = new TcpLink(5760);
        assertDoesNotThrow(() -> {
            byte[] data = link.read();
            assertNull(data, "未 open 的 read 应返回 null");
        });
    }

    @Test
    @DisplayName("未 open 时 send 抛 IllegalStateException")
    void testSendThrowsWhenNotOpen() {
        TcpLink link = new TcpLink(5760);
        assertThrows(IllegalStateException.class, () -> link.send(new byte[]{(byte) 0xFE, 0x00}));
    }

    @Test
    @DisplayName("TcpLink 实现 Link 接口")
    void testImplementsLink() {
        TcpLink link = new TcpLink(5760);
        assertTrue(link instanceof Link, "TcpLink 必须实现 Link 接口");
    }
}
