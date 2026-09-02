package cn.edu.nuaa.gcs.comm;

import java.util.Timer;
import java.util.TimerTask;
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

    private Link link;
    private volatile boolean running = false;
    private Thread listenThread;
    private Timer heartbeatTimer;
    private final ConcurrentLinkedQueue<MavlinkParser.Telemetry> queue = new ConcurrentLinkedQueue<>();
    private Consumer<MavlinkParser.Telemetry> callback;
    private int reconnectAttempts = 0;
    private volatile long lastReceivedMs = 0;  // 最近一次收到有效遥测的时间戳

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
                    MavlinkParser.Telemetry t = MavlinkParser.parse(data, 0, data.length);
                    if (t.valid) {
                        lastReceivedMs = System.currentTimeMillis();
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
