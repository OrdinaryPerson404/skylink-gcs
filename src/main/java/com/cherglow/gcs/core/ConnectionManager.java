package com.cherglow.gcs.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 连接状态机（纯 Java，可单测）。
 *
 * 语义（沿用复刻项目已修坑）：
 * 1) connect() 进入 CONNECTING；开通道失败 → ERROR；
 * 2) 失败后保持 ERROR，后台重试只置 retrying 标记，期间状态不得回跳 CONNECTING（error 不被覆盖）；
 * 3) 首条数据到达才转 CONNECTED（retrying 清除）；
 * 4) CONNECTED 下 3s 无任何数据 → ERROR（心跳超时），自动重试；
 * 5) 退避 1s→2s→4s→8s（封顶），CONNECTED 后重置。
 */
public final class ConnectionManager {

    public enum State { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

    public interface LinkChannel {
        void setLineHandler(Consumer<String> onLine);

        void open() throws Exception;

        void close();

        boolean isOpen();
    }

    public interface TimeSource {
        long nowMs();
    }

    private static final long HEARTBEAT_TIMEOUT_MS = 3000;
    private static final long RETRY_INITIAL_MS = 1000;
    private static final long RETRY_MAX_MS = 8000;

    private final LinkChannel channel;
    private final ScheduledExecutorService executor;
    private final TimeSource time;

    private final List<Consumer<State>> stateListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> detailListeners = new CopyOnWriteArrayList<>();

    private volatile State state = State.DISCONNECTED;
    private volatile boolean retrying;
    private volatile String detail = "";
    private volatile long lastDataMs;
    private volatile long retryDelayMs = RETRY_INITIAL_MS;
    private volatile boolean userClosed;
    private volatile boolean retryScheduled;

    public ConnectionManager(LinkChannel channel, ScheduledExecutorService executor, TimeSource time) {
        this.channel = channel;
        this.executor = executor;
        this.time = time;
        channel.setLineHandler(this::onLine);
    }

    // ---- 对外 API ----

    public void addStateListener(Consumer<State> l) {
        stateListeners.add(l);
    }

    public void addDetailListener(Consumer<String> l) {
        detailListeners.add(l);
    }

    public State getState() {
        return state;
    }

    public boolean isRetrying() {
        return retrying;
    }

    public String getDetail() {
        return detail;
    }

    /** 用户发起连接；仅 DISCONNECTED 状态有效 */
    public synchronized void connect() {
        if (state != State.DISCONNECTED) {
            return;
        }
        userClosed = false;
        retrying = false;
        retryDelayMs = RETRY_INITIAL_MS;
        setState(State.CONNECTING);
        asyncOpen();
    }

    public synchronized void disconnect() {
        userClosed = true;
        retrying = false;
        retryScheduled = false;
        try {
            channel.close();
        } catch (Exception ignored) {
            // 关闭失败按已断开处理
        }
        setState(State.DISCONNECTED);
        setDetail("");
    }

    // ---- 内部流转 ----

    private void asyncOpen() {
        executor.submit(() -> {
            if (userClosed) {
                return;
            }
            try {
                if (channel.isOpen()) {
                    channel.close();
                }
                channel.open();
                // 打开成功：等首条数据；ERROR 期间的重试成功不回跳 CONNECTING
            } catch (Exception e) {
                if (!userClosed) {
                    fail(safeMessage(e));
                }
            }
        });
    }

    /** 每条串口数据都算活性信号；首条数据从 CONNECTING/ERROR 转 CONNECTED */
    public void onLine(String line) {
        lastDataMs = time.nowMs();
        State s = state;
        if (s == State.CONNECTING || s == State.ERROR) {
            retrying = false;
            retryDelayMs = RETRY_INITIAL_MS;
            setState(State.CONNECTED);
            setDetail("");
        }
    }

    /** 看守器：CONNECTED 下 3s 无数据判掉线（由外部定时调用） */
    public void checkWatchdog() {
        if (state == State.CONNECTED && time.nowMs() - lastDataMs > HEARTBEAT_TIMEOUT_MS) {
            fail("3 秒无响应（可能已断开）");
        }
    }

    private void fail(String reason) {
        setDetail(reason);
        setState(State.ERROR);
        retrying = true;
        scheduleRetry();
    }

    private void scheduleRetry() {
        if (retryScheduled || userClosed) {
            return;
        }
        retryScheduled = true;
        long delay = retryDelayMs;
        executor.schedule(() -> {
            retryScheduled = false;
            if (!userClosed && state == State.ERROR) {
                asyncOpen(); // 重试期间保持 ERROR 态（retrying 标记语义）
            }
        }, delay, TimeUnit.MILLISECONDS);
        retryDelayMs = Math.min(retryDelayMs * 2, RETRY_MAX_MS);
    }

    /** 启动看门狗定时器（App 装配时调用一次） */
    public void startWatchdog() {
        executor.scheduleWithFixedDelay(this::checkWatchdog, 500, 500, TimeUnit.MILLISECONDS);
    }

    private void setState(State s) {
        if (state != s) {
            state = s;
            for (Consumer<State> l : stateListeners) {
                l.accept(s);
            }
        }
    }

    private void setDetail(String d) {
        detail = d == null ? "" : d;
        for (Consumer<String> l : detailListeners) {
            l.accept(detail);
        }
    }

    private static String safeMessage(Throwable t) {
        String m = t.getMessage();
        return m == null || m.isEmpty() ? t.toString() : m;
    }
}
