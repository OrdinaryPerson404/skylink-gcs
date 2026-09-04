package cn.edu.nuaa.gcs.comm;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * TCP 链路实现：通过 WiFi 连接无人机飞控的 MAVLink 服务。
 *
 * <p>应用场景：CF-Drone 飞控开放 WiFi（SSID: Drone_WiFi，密码 12345678），
 * 地面站电脑连接后通过固定 TCP 端口与飞控 MAVLink 服务通信。
 * 注意：连接该 WiFi 后电脑将失去互联网，地面站应配合 {@link cn.edu.nuaa.gcs.util.NetworkMonitor}
 * 检测并提示该离线状态。
 *
 * <p>设计约束（项目硬约束）：
 * <ul>
 *   <li>仅作为链路层，不修改飞控配置或固件；</li>
 *   <li>接收使用独立守护线程把 InputStream 累积到 BlockingQueue，
 *       与 {@link SerialLink} 模式一致，便于 CommunicationService 统一调度；</li>
 *   <li>{@link #read()} 短超时返回，避免监听线程长时间阻塞；</li>
 *   <li>连接失败/对端断开通过 {@link #isOpen()} 体现，
 *       由 {@link CommunicationService} 的重连机制统一处理。</li>
 * </ul>
 */
public class TcpLink implements Link {

    /** 默认无人机 WiFi 网关地址（ESP32/CF-Drone 默认 AP 模式 192.168.4.1）。 */
    public static final String DEFAULT_HOST = "192.168.4.1";
    /** 默认连接超时（毫秒）。 */
    private static final int CONNECT_TIMEOUT_MS = 3000;
    /** read 短超时（毫秒），防止监听线程长时间阻塞。 */
    private static final int READ_POLL_MS = 50;
    /** 接收缓冲上限（与 SerialLink 一致，65536 字节）。 */
    private static final int RX_BUFFER_CAP = 65536;
    /** 单次 read 系统调用缓冲（字节）。 */
    private static final int READ_CHUNK = 1024;

    private final String host;
    private final int port;

    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private final BlockingQueue<Byte> rxBuffer = new LinkedBlockingQueue<>(RX_BUFFER_CAP);
    private volatile boolean open = false;
    private Thread rxThread;

    public TcpLink(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** 使用默认网关地址构造（192.168.4.1:port）。 */
    public TcpLink(int port) {
        this(DEFAULT_HOST, port);
    }

    @Override
    public void open() throws Exception {
        socket = new Socket();
        socket.setTcpNoDelay(true); // MAVLink 帧小且频繁，禁用 Nagle 降低延迟
        // 接收线程单独处理读，不依赖 SO_TIMEOUT；但设一个上限避免 read 永久阻塞
        socket.setSoTimeout(0);
        System.out.println("[TCP] 连接 " + host + ":" + port + " ...");
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        out = socket.getOutputStream();
        in = socket.getInputStream();
        open = true;
        System.out.println("[TCP] 已连接, 启动接收线程");
        rxThread = new Thread(this::rxLoop, "GCS-TcpRx");
        rxThread.setDaemon(true);
        rxThread.start();
    }

    /** 接收循环：把 InputStream 累积到 BlockingQueue，缓冲满时丢最旧数据。 */
    private void rxLoop() {
        byte[] buf = new byte[READ_CHUNK];
        try {
            while (open && socket != null && !socket.isClosed()) {
                int n = in.read(buf);
                if (n < 0) break; // EOF，对端关闭连接
                if (n > 0) {
                    for (int i = 0; i < n; i++) {
                        if (!rxBuffer.offer(buf[i])) {
                            rxBuffer.poll(); // 缓冲满，丢弃最旧以避免内存堆积
                            rxBuffer.offer(buf[i]);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (open) {
                System.err.println("[TCP] 接收异常: " + e.getMessage());
            }
        } finally {
            open = false;
        }
    }

    @Override
    public byte[] read() throws Exception {
        if (!open) return null;
        if (rxBuffer.isEmpty()) {
            Thread.sleep(READ_POLL_MS);
            return null;
        }
        int available = rxBuffer.size();
        byte[] data = new byte[available];
        for (int i = 0; i < available; i++) {
            Byte b = rxBuffer.poll(READ_POLL_MS, TimeUnit.MILLISECONDS);
            if (b == null) break;
            data[i] = b;
        }
        return data;
    }

    @Override
    public void send(byte[] data) throws Exception {
        if (!open || out == null) throw new IllegalStateException("Link not open");
        out.write(data);
        out.flush();
    }

    @Override
    public void close() {
        open = false;
        if (rxThread != null) rxThread.interrupt();
        if (socket != null) {
            try { socket.close(); } catch (Exception ignored) { /* 关闭异常忽略 */ }
        }
        rxBuffer.clear();
        socket = null;
        in = null;
        out = null;
    }

    @Override
    public boolean isOpen() {
        return open
                && socket != null
                && !socket.isClosed()
                && socket.isConnected();
    }

    public String getHost() { return host; }
    public int getPort() { return port; }

    @Override
    public String toString() { return host + ":" + port; }
}
