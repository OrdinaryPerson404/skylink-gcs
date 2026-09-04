package cn.edu.nuaa.gcs.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * 网络状态监控：周期检查本机网络，识别无人机 WiFi 直连场景并通知状态变化。
 *
 * <p>应用场景：连接无人机 WiFi（SSID: Drone_WiFi）后电脑将失去互联网。
 * 地面站需要：
 * <ol>
 *   <li>检测当前是否处于无人机 AP 网段（{@code 192.168.4.x}），用于在 UI 上提示
 *       "已连接无人机 WiFi，互联网不可用"；</li>
 *   <li>周期回调通知状态变化，避免 UI 每次手动刷新；</li>
 *   <li>可选地探测互联网可达性，用于区分 "Drone WiFi 但本地代理可用" 与 "完全断网"。</li>
 * </ol>
 *
 * <p>设计约束（项目硬约束）：
 * <ul>
 *   <li>只读检测，不修改任何网络配置、不连接无人机硬件或固件；</li>
 *   <li>使用 {@link NetworkInterface} 枚举本机 IPv4，不依赖 OS 命令，跨平台；</li>
 *   <li>互联网探测用 TCP connect（80/443）而非 ICMP，无需管理员权限。</li>
 * </ul>
 *
 * <p>状态机：{@link State#UNINIT} → {@link State#ONLINE} / {@link State#DRONE_WIFI} /
 * {@link State#OFFLINE}，仅状态变化时回调，避免 UI 抖动。
 */
public class NetworkMonitor {

    /** 默认无人机 AP 网段前缀（192.168.4.）。 */
    public static final String DRONE_AP_PREFIX = "192.168.4.";
    /** 默认监控周期（毫秒）。 */
    private static final long DEFAULT_INTERVAL_MS = 5000;
    /** 互联网探测超时（毫秒）。 */
    private static final int NET_PROBE_TIMEOUT_MS = 1500;
    /** 互联网探测目标（依次尝试，任一成功即认为可达）。 */
    private static final String[] NET_PROBE_HOSTS = {"1.1.1.1", "8.8.8.8", "223.5.5.5"};
    /** 互联网探测端口。 */
    private static final int NET_PROBE_PORT = 80;

    /** 网络状态分类。 */
    public enum State {
        /** 未启动监控。 */
        UNINIT,
        /** 在线（有非无人机网段 IPv4 或互联网可达）。 */
        ONLINE,
        /** 处于无人机 AP 网段，互联网可能不可用。 */
        DRONE_WIFI,
        /** 无任何可用 IPv4 接口。 */
        OFFLINE
    }

    private final long intervalMs;
    private final Consumer<State> callback;
    private Timer timer;
    private volatile State current = State.UNINIT;
    private volatile boolean running = false;

    public NetworkMonitor(Consumer<State> callback) {
        this(DEFAULT_INTERVAL_MS, callback);
    }

    public NetworkMonitor(long intervalMs, Consumer<State> callback) {
        this.intervalMs = Math.max(1000, intervalMs);
        this.callback = callback;
    }

    /** 启动周期监控。重复调用安全（先停止旧 Timer）。 */
    public synchronized void start() {
        stop();
        running = true;
        current = State.UNINIT;
        timer = new Timer("GCS-NetMon", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (!running) return;
                State next = probe();
                if (next != current) {
                    current = next;
                    if (callback != null) {
                        try {
                            callback.accept(next);
                        } catch (Exception e) {
                            System.err.println("[NetMon] 回调异常: " + e.getMessage());
                        }
                    }
                }
            }
        }, 0, intervalMs);
    }

    /** 停止监控，释放 Timer。 */
    public synchronized void stop() {
        running = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public State getCurrentState() { return current; }

    public boolean isRunning() { return running; }

    /**
     * 一次性探测当前网络状态（不依赖 Timer，可独立调用）。
     * 规则：无任何 IPv4 → OFFLINE；含 192.168.4.x → DRONE_WIFI；否则 ONLINE。
     */
    public static State probe() {
        try {
            boolean anyIpv4 = false;
            boolean onDrone = false;
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            if (nics == null) return State.OFFLINE;
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback()) continue;
                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address) {
                        anyIpv4 = true;
                        String ip = a.getHostAddress();
                        if (ip != null && ip.startsWith(DRONE_AP_PREFIX)) {
                            onDrone = true;
                        }
                    }
                }
            }
            if (!anyIpv4) return State.OFFLINE;
            return onDrone ? State.DRONE_WIFI : State.ONLINE;
        } catch (Exception e) {
            return State.OFFLINE;
        }
    }

    /**
     * 主动探测互联网可达性（尽力而为，失败不代表断网）。
     * 用 TCP connect 80 端口替代 ICMP ping，无需管理员权限。
     */
    public static boolean isInternetReachable() {
        for (String h : NET_PROBE_HOSTS) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(h, NET_PROBE_PORT), NET_PROBE_TIMEOUT_MS);
                return true;
            } catch (Exception ignored) {
                // 继续尝试下一个
            }
        }
        return false;
    }
}
