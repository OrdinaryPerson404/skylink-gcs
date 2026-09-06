package com.cherglow.gcs;

import com.cherglow.gcs.core.ConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 状态机转换矩阵测试。回归点：失败后保持 ERROR + retrying 标记，
 * 重试期间不得回跳 CONNECTING（error 不被覆盖），首条数据才转 CONNECTED。
 */
class ConnectionManagerTest {

    /** 测试用内存通道：可切换 open 成功/失败，记录状态变化 */
    static final class FakeChannel implements ConnectionManager.LinkChannel {
        volatile boolean openOk = true;
        volatile boolean opened;
        volatile int openCalls;
        volatile Consumer<String> lineHandler;

        @Override
        public void setLineHandler(Consumer<String> onLine) {
            this.lineHandler = onLine;
        }

        @Override
        public void open() {
            openCalls++;
            if (!openOk) {
                throw new RuntimeException("打开 COM5 失败（错误码 3）；请确认端口未被其他程序占用");
            }
            opened = true;
        }

        @Override
        public void close() {
            opened = false;
        }

        @Override
        public boolean isOpen() {
            return opened;
        }
    }

    static final class MutableTime implements ConnectionManager.TimeSource {
        volatile long now = 1_000_000;

        void advance(long ms) {
            now += ms;
        }

        @Override
        public long nowMs() {
            return now;
        }
    }

    private final ScheduledExecutorService exec =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "test-cm");
                t.setDaemon(true);
                return t;
            });

    @AfterEach
    void tearDown() {
        exec.shutdownNow();
    }

    private static void awaitTrue(String what, java.util.function.BooleanSupplier cond) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (cond.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        fail("等待超时: " + what);
    }

    @Test
    void connect_thenFirstLine_transitionsToConnected() throws Exception {
        FakeChannel ch = new FakeChannel();
        MutableTime time = new MutableTime();
        ConnectionManager cm = new ConnectionManager(ch, exec, time);
        cm.connect();
        assertEquals(ConnectionManager.State.CONNECTING, cm.getState());
        awaitTrue("channel opened", () -> ch.opened);
        cm.onLine("any data");
        assertEquals(ConnectionManager.State.CONNECTED, cm.getState());
        assertFalse(cm.isRetrying());
        cm.disconnect();
        assertEquals(ConnectionManager.State.DISCONNECTED, cm.getState());
    }

    @Test
    void openFailure_keepsErrorAndRetries_withoutConnectingFlicker() throws Exception {
        FakeChannel ch = new FakeChannel();
        ch.openOk = false;
        MutableTime time = new MutableTime();
        ConnectionManager cm = new ConnectionManager(ch, exec, time);
        List<ConnectionManager.State> seen = new ArrayList<>();
        cm.addStateListener(seen::add);

        cm.connect();
        awaitTrue("first failure recorded", () -> cm.getState() == ConnectionManager.State.ERROR);
        assertEquals("打开 COM5 失败（错误码 3）；请确认端口未被其他程序占用", cm.getDetail());
        assertTrue(cm.isRetrying());

        // 重试窗口（1s 后）；期间状态必须保持 ERROR
        assertEquals(ConnectionManager.State.ERROR, cm.getState());

        // 退避时间走满，重试成功
        ch.openOk = true;
        time.advance(1100);
        awaitTrue("retry open attempted", () -> ch.openCalls >= 2);
        assertEquals(ConnectionManager.State.ERROR, cm.getState()); // 重试成功也不回跳 CONNECTING
        cm.onLine("first telemetry line");
        assertEquals(ConnectionManager.State.CONNECTED, cm.getState());
        assertFalse(cm.isRetrying());

        // 回归断言：ERROR→CONNECTED 之间不得出现 CONNECTING
        int errIdx = seen.lastIndexOf(ConnectionManager.State.ERROR);
        int connIdx = seen.lastIndexOf(ConnectionManager.State.CONNECTED);
        assertTrue(errIdx >= 0 && connIdx > errIdx);
        for (int i = errIdx; i < connIdx; i++) {
            assertNotEquals(ConnectionManager.State.CONNECTING, seen.get(i),
                    "重试期间状态被 CONNECTING 覆盖（复刻项目已修坑回归）");
        }
    }

    @Test
    void watchdog_silence3s_transitionsToError_thenRecoversOnData() throws Exception {
        FakeChannel ch = new FakeChannel();
        MutableTime time = new MutableTime();
        ConnectionManager cm = new ConnectionManager(ch, exec, time);
        cm.connect();
        awaitTrue("channel opened", () -> ch.opened);
        cm.onLine("data@t0");
        assertEquals(ConnectionManager.State.CONNECTED, cm.getState());

        time.advance(2000);
        cm.checkWatchdog();
        assertEquals(ConnectionManager.State.CONNECTED, cm.getState());

        time.advance(1100); // 累计 3.1s 无数据
        cm.checkWatchdog();
        assertEquals(ConnectionManager.State.ERROR, cm.getState());
        assertTrue(cm.getDetail().contains("无响应"));
        assertTrue(cm.isRetrying());

        // 数据恢复 → CONNECTED
        cm.onLine("data@t1");
        assertEquals(ConnectionManager.State.CONNECTED, cm.getState());
    }

    @Test
    void connect_ignoredWhenNotDisconnected() throws Exception {
        FakeChannel ch = new FakeChannel();
        ConnectionManager cm = new ConnectionManager(ch, exec, new MutableTime());
        cm.connect();
        awaitTrue("open attempted", () -> ch.openCalls >= 1);
        cm.onLine("x");
        assertEquals(ConnectionManager.State.CONNECTED, cm.getState());
        cm.connect(); // 忽略
        assertEquals(ConnectionManager.State.CONNECTED, cm.getState());
        assertEquals(1, ch.openCalls);
        cm.disconnect();
    }

    @Test
    void watchdogLive_checkDoesNotFailWhileDataFlows() {
        FakeChannel ch = new FakeChannel();
        MutableTime time = new MutableTime();
        ConnectionManager cm = new ConnectionManager(ch, exec, time);
        cm.connect();
        cm.onLine("x"); // t=now
        time.advance(1000);
        cm.onLine("y"); // t=now+1s，刷新活性
        time.advance(1000);
        cm.checkWatchdog(); // 距最后数据仅 1s
        assertEquals(ConnectionManager.State.CONNECTED, cm.getState());
    }
}
