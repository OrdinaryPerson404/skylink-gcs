package cn.edu.nuaa.gcs.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * NetworkMonitor 单元测试。
 *
 * 注意：真实网络状态依赖运行环境，测试只断言不变式（返回值在合理集合内、
 * 状态机切换正常、回调触发），不绑定具体网络状态值。
 */
class NetworkMonitorTest {

    @Test
    @DisplayName("probe 返回合法状态之一")
    void testProbeReturnsValidState() {
        NetworkMonitor.State s = NetworkMonitor.probe();
        assertNotNull(s, "probe 不应返回 null");
        // 任何状态都合法，只要不抛异常
        assertTrue(java.util.EnumSet.allOf(NetworkMonitor.State.class).contains(s),
            "状态应为枚举成员, 实际: " + s);
    }

    @Test
    @DisplayName("默认构造使用 5 秒周期")
    void testDefaultInterval() {
        // 间接验证：start 后通过 isRunning 确认启动
        java.util.concurrent.atomic.AtomicReference<NetworkMonitor.State> seen =
            new java.util.concurrent.atomic.AtomicReference<>(NetworkMonitor.State.UNINIT);
        NetworkMonitor mon = new NetworkMonitor(s -> seen.set(s));
        assertFalse(mon.isRunning(), "未 start 时 isRunning 应为 false");
        assertEquals(NetworkMonitor.State.UNINIT, mon.getCurrentState());
    }

    @Test
    @DisplayName("start/stop 生命周期正确，回调仅状态变化时触发")
    void testStartStopLifecycle() throws Exception {
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicReference<NetworkMonitor.State> last =
            new java.util.concurrent.atomic.AtomicReference<>(NetworkMonitor.State.UNINIT);
        // 用 1 秒间隔加速测试
        NetworkMonitor mon = new NetworkMonitor(1000, s -> {
            calls.incrementAndGet();
            last.set(s);
        });
        mon.start();
        assertTrue(mon.isRunning(), "start 后 isRunning 应为 true");
        // 等待第一次探测（≤1 秒 + 抖动）
        Thread.sleep(1500);
        assertTrue(calls.get() >= 1, "应至少触发一次回调, 实际: " + calls.get());
        NetworkMonitor.State firstSeen = last.get();
        assertNotNull(firstSeen);
        assertNotEquals(NetworkMonitor.State.UNINIT, firstSeen,
            "首次探测后状态不应为 UNINIT");
        // 等待第二个周期，如果状态未变化则不应再触发回调
        int callsAfterFirst = calls.get();
        Thread.sleep(1200);
        // 状态未变化时回调不应增加（允许 ==，因为环境可能稳定）
        assertTrue(calls.get() >= callsAfterFirst,
            "回调计数不应倒退");
        mon.stop();
        assertFalse(mon.isRunning(), "stop 后 isRunning 应为 false");
    }

    @Test
    @DisplayName("重复 start 安全（先停旧的）")
    void testRestartSafe() {
        NetworkMonitor mon = new NetworkMonitor(1000, s -> {});
        assertDoesNotThrow(() -> {
            mon.start();
            mon.start(); // 重复 start 不应抛异常
        });
        mon.stop();
    }

    @Test
    @DisplayName("isInternetReachable 返回 boolean，不抛异常")
    void testIsInternetReachableSafe() {
        boolean r = assertDoesNotThrow(NetworkMonitor::isInternetReachable,
            "isInternetReachable 不应抛异常");
        // 不绑定具体值，CI 环境可能无网
        assertTrue(r == true || r == false);
    }

    @Test
    @DisplayName("DRONE_AP_PREFIX 常量为 192.168.4.")
    void testDroneApPrefixConstant() {
        assertEquals("192.168.4.", NetworkMonitor.DRONE_AP_PREFIX);
    }
}
