package cn.edu.nuaa.gcs.comm;

import java.util.Deque;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * 通信服务：管理链路、监听线程解析 MAVLink 遥测，并周期发送 GCS 心跳以触发飞控完整遥测。
 *
 * 稳定性增强：
 * - 监听线程异常按类型区分处理（中断即退出，IO 类重连，其它记录）；
 * - 队列容量上限保护，防止 UI 线程消费不及时导致内存堆积；
 * - 发送与接收分离，发送失败不污染接收线程。
 *
 * 与飞控的约束：地面站所有发送均为 v1 帧，飞控 mavlink_parse_char 可同时识别 v1/v2，
 * 因此不会因协议版本导致命令被丢弃。
 */
public class CommunicationService {
    private static final int GCS_SYS_ID = 255;          // 地面站系统 ID
    private static final int GCS_COMP_ID = 190;          // MAV_COMP_ID_MISSIONPLANNER / GCS
    private static final int DRONE_SYS_ID = 1;          // CF-Drone 默认 mavlinkSysId=1
    private static final int DRONE_COMP_ID = 1;          // MAV_COMP_ID_AUTOPILOT1
    private static final int HEARTBEAT_INTERVAL_MS = 1000;
    private static final int QUEUE_MAX = 256;
    /** 链路质量滑动窗口长度（秒）。 */
    private static final long QUALITY_WINDOW_S = 10;
    /** 预期遥测频率（Hz），用于计算链路质量百分比。 */
    private static final int EXPECTED_HZ = 1;

    private Link link;
    private volatile boolean running = false;
    private Thread listenThread;
    private Timer heartbeatTimer;
    /** 有状态流式解析器（跨块帧拼接 + 多帧输出）。仅监听线程访问，无需并发保护。 */
    private final MavlinkParser streamParser = new MavlinkParser();
    {
        // CF-Drone 固件部分消息（如 BATTERY_STATUS）的 CRC_EXTRA 可能非标准，
        // 严格模式会丢弃这些帧。启用 lenient 模式确保数据流通，
        // crcValid 字段仍可用于上层判断数据完整性。
        streamParser.setLenient(true);
    }
    private final ConcurrentLinkedQueue<MavlinkParser.Telemetry> queue = new ConcurrentLinkedQueue<>();
    private final Deque<Long> recentRcvMs = new ConcurrentLinkedDeque<>();
    private Consumer<MavlinkParser.Telemetry> callback;
    private int reconnectAttempts = 0;
    private volatile long lastReceivedMs = 0;  // 最近一次收到有效遥测的时间戳
    private volatile long totalReceived = 0;   // 累计接收有效帧数（诊断用）

    public void setLink(Link link) {
        if (running) stop();
        this.link = link;
    }

    public void setCallback(Consumer<MavlinkParser.Telemetry> cb) {
        this.callback = cb;
    }

    public void start() {
        if (link == null) throw new IllegalStateException("Link not set");
        running = true;
        reconnectAttempts = 0;
        // 首次显式打开链路：避免依赖 listenLoop 中的指数退避路径（首次 open 延迟数秒）。
        try {
            if (!link.isOpen()) link.open();
        } catch (Exception e) {
            // 交给 listenLoop 重试；不要让 start() 抛异常中断后续初始化
            System.err.println("[GCS] 首次打开链路失败，将在监听线程中重试: " + e.getMessage());
        }
        listenThread = new Thread(this::listenLoop, "GCS-Listen");
        listenThread.setDaemon(true);
        listenThread.start();
        // 周期发送 GCS 心跳：触发飞控 mavlinkConnected=true，使其发送完整遥测
        heartbeatTimer = new Timer("GCS-Heartbeat", true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!running) return;
                try {
                    send(MavlinkEncoder.heartbeat(GCS_SYS_ID, GCS_COMP_ID));
                } catch (Exception e) {
                    // 心跳发送失败不中断接收线程，仅记录
                    System.err.println("[GCS] 心跳发送失败: " + e.getMessage());
                }
            }
        }, 0, HEARTBEAT_INTERVAL_MS);
    }

    public void stop() {
        running = false;
        if (heartbeatTimer != null) { heartbeatTimer.cancel(); heartbeatTimer = null; }
        if (listenThread != null) listenThread.interrupt();
        if (link != null) link.close();
        recentRcvMs.clear();
    }

    public boolean isConnected() {
        return link != null && link.isOpen() && running;
    }

    /** 最近一次收到有效遥测的时间戳（ms），用于判断链路活跃度。 */
    public long getLastReceivedMs() { return lastReceivedMs; }

    /** 链路是否在近 5 秒内收到过遥测。 */
    public boolean isLinkActive() {
        return lastReceivedMs > 0 && (System.currentTimeMillis() - lastReceivedMs) < 5000;
    }

    /** 当前链路端口名（如 COM5 / 192.168.4.1:14550），用于电池监控显示数据来源。 */
    public String getPortName() {
        if (link == null) return null;
        String s = link.toString();
        if (s != null && !s.isBlank() && !s.startsWith("cn.edu.nuaa")) return s;
        return link.getClass().getSimpleName();
    }

    /** 累计接收有效 MAVLink 帧数（诊断用，不重置直到 stop）。 */
    public long getTotalReceived() { return totalReceived; }

    /**
     * 链路质量百分比（0-100），基于滑动窗口内实际接收帧数 / 期望帧数估算。
     *
     * <p>计算方式：
     * <pre>
     *   窗口期望帧数 = min(QUALITY_WINDOW_S, (now - firstMs) / 1000) * EXPECTED_HZ
     *   quality = min(100, 实际帧数 / 期望帧数 * 100)
     * </pre>
     * 未启动 / 未收到任何数据时返回 -1；刚启动不足 1 秒按窗口 = 1s 避免除零。
     */
    public double getLinkQualityPct() {
        // 同步清理旧条目（滑动窗口）
        long cutoff = System.currentTimeMillis() - QUALITY_WINDOW_S * 1000;
        while (!recentRcvMs.isEmpty() && recentRcvMs.peekFirst() < cutoff) {
            recentRcvMs.pollFirst();
        }
        int n = recentRcvMs.size();
        if (n == 0) {
            if (lastReceivedMs == 0) return -1; // 从未收到过
            return 0;
        }
        long now = System.currentTimeMillis();
        long firstMs = recentRcvMs.peekFirst();
        double windowS = Math.max(1.0, Math.min(QUALITY_WINDOW_S, (now - firstMs) / 1000.0));
        double expected = windowS * EXPECTED_HZ;
        double ratio = n / expected;
        return Math.min(100.0, ratio * 100.0);
    }

    /** 追加一次接收时间到滑动窗口，同时按需淘汰旧数据（非严格清理）。 */
    private void recordReception(long nowMs) {
        recentRcvMs.addLast(nowMs);
        // 轻量淘汰：避免 deque 膨胀超过窗口最大值 + 余量
        long cutoff = nowMs - QUALITY_WINDOW_S * 1000;
        while (!recentRcvMs.isEmpty() && recentRcvMs.peekFirst() < cutoff) {
            recentRcvMs.pollFirst();
        }
    }

    /** 发送上行帧。 */
    public void send(byte[] data) throws Exception {
        if (link == null || !link.isOpen()) throw new IllegalStateException("Link not open");
        link.send(data);
    }

    /** 请求飞控参数列表（只读，安全）。 */
    public void requestParamList() {
        safeSend(MavlinkEncoder.paramRequestList(GCS_SYS_ID, GCS_COMP_ID, DRONE_SYS_ID, DRONE_COMP_ID));
    }

    /** 按索引请求单个参数（只读，安全）。 */
    public void requestParamRead(int paramIndex, String paramName) {
        safeSend(MavlinkEncoder.paramRequestRead(GCS_SYS_ID, GCS_COMP_ID, DRONE_SYS_ID, DRONE_COMP_ID, paramIndex, paramName));
    }

    /** 请求 AUTOPILOT_VERSION（只读，安全）。 */
    public void requestAutopilotVersion() {
        safeSend(MavlinkEncoder.requestAutopilotVersion(GCS_SYS_ID, GCS_COMP_ID, DRONE_SYS_ID, DRONE_COMP_ID));
    }

    /** 请求任意消息（只读，安全）。 */
    public void requestMessage(int msgId) {
        safeSend(MavlinkEncoder.requestMessage(GCS_SYS_ID, GCS_COMP_ID, DRONE_SYS_ID, DRONE_COMP_ID, msgId));
    }

    /** 请求 BATTERY_STATUS(147)（只读，安全）。 */
    public void requestBatteryStatus() {
        safeSend(MavlinkEncoder.requestBatteryStatus(GCS_SYS_ID, GCS_COMP_ID, DRONE_SYS_ID, DRONE_COMP_ID));
    }

    /** 解锁/上锁（控制类，调用方须自行完成安全确认）。 */
    public void armDisarm(boolean arm) {
        safeSend(MavlinkEncoder.armDisarm(GCS_SYS_ID, GCS_COMP_ID, DRONE_SYS_ID, DRONE_COMP_ID, arm));
    }

    /** 切换飞行模式（控制类，调用方须自行完成安全确认）。 */
    public void setMode(int mode) {
        safeSend(MavlinkEncoder.setMode(GCS_SYS_ID, GCS_COMP_ID, DRONE_SYS_ID, DRONE_COMP_ID, mode));
    }

    /** 设置飞控参数（配置变更类，调用方须自行完成安全确认）。 */
    public void setParam(String name, float value, int paramType) {
        safeSend(MavlinkEncoder.paramSet(GCS_SYS_ID, GCS_COMP_ID, DRONE_SYS_ID, DRONE_COMP_ID, name, value, paramType));
    }

    private void safeSend(byte[] frame) {
        try {
            if (link != null && link.isOpen()) link.send(frame);
        } catch (Exception e) {
            System.err.println("[GCS] 上行发送失败: " + e.getMessage());
        }
    }

    private void listenLoop() {
        while (running) {
            try {
                if (!link.isOpen()) {
                    if (reconnectAttempts < 3) {
                        reconnectAttempts++;
                        Thread.sleep((long) Math.pow(2, reconnectAttempts) * 1000);
                        link.open();
                    } else {
                        running = false;
                        break;
                    }
                }
                byte[] data = link.read();
                if (data != null && data.length > 0) {
                    // 有状态流式解析：跨块帧自动拼接，且处理缓冲区内所有完整帧
                    // （旧实现 parse() 每次只取最后 1 帧，且跨块帧 CRC 失败被整帧丢弃）
                    for (MavlinkParser.Telemetry t : streamParser.feed(data, data.length)) {
                        long now = System.currentTimeMillis();
                        lastReceivedMs = now;
                        totalReceived++;
                        recordReception(now);
                        // 队列容量保护：超出上限丢弃最旧数据，避免内存堆积
                        if (queue.size() >= QUEUE_MAX) queue.poll();
                        queue.add(t);
                        if (callback != null) callback.accept(t);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // IO/解析异常：递增重连计数，但不立刻中断，避免单包坏数据导致监听退出
                reconnectAttempts++;
                System.err.println("[GCS] 监听异常: " + e.getMessage());
            }
        }
    }

    public MavlinkParser.Telemetry poll() {
        return queue.poll();
    }
}
