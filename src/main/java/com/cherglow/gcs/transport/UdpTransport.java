package com.cherglow.gcs.transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.function.BiConsumer;

/**
 * MAVLink over UDP（S13）：
 * 本机绑定 14550（固件 wifi.ino 只回 udpRemotePort=14550），主动发 192.168.4.1:14550 触发出流；
 * 首个有效来源地址锁定（udpRemoteIP 语义对齐）。Windows 下 ICMP Port Unreachable 会令 receive()
 * 抛"Connection reset"（10054），按容错忽略。
 */
public final class UdpTransport {

    public interface Listener {
        void accept(byte[] data, int len, InetAddress from);
    }

    private DatagramSocket socket;
    private volatile boolean running;
    private volatile InetAddress remoteAddr;
    private volatile int remotePort;
    private volatile Listener listener;

    /** 绑定本地端口并启动接收线程 */
    public synchronized void open(int localPort, String remoteIp, int remotePort) throws IOException {
        if (socket != null && !socket.isClosed()) {
            throw new SocketException("UDP 通道已打开");
        }
        socket = new DatagramSocket(null);
        socket.setReuseAddress(false);
        socket.bind(new java.net.InetSocketAddress(localPort));
        socket.setSoTimeout(500);
        this.remoteAddr = InetAddress.getByName(remoteIp);
        this.remotePort = remotePort;
        running = true;
        Thread t = new Thread(this::rxLoop, "mavlink-udp-rx");
        t.setDaemon(true);
        t.start();
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    private void rxLoop() {
        byte[] buf = new byte[2048];
        while (running) {
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(p);
                InetAddress from = p.getAddress();
                // 锁定首个数据来源（与固件 udpRemoteIP 学习对齐）
                if (!from.equals(remoteAddr)) {
                    remoteAddr = from;
                }
                Listener l = listener;
                if (l != null && p.getLength() > 0) {
                    byte[] copy = new byte[p.getLength()];
                    System.arraycopy(p.getData(), p.getOffset(), copy, 0, copy.length);
                    l.accept(copy, copy.length, from);
                }
            } catch (SocketTimeoutException e) {
                // 常规轮询超时
            } catch (IOException e) {
                if (!running) {
                    break;
                }
                // Windows UDP 10054（对端不可达触发 ICMP）容错：静默重试
                String msg = String.valueOf(e.getMessage());
                if (!msg.contains("reset") && !msg.contains("10054")) {
                    // 其他 IO 异常也继续重试，避免 WiFi 波动直接杀死通道
                }
            }
        }
    }

    /** 发送原始字节到当前锁定地址 */
    public synchronized void send(byte[] data) {
        DatagramSocket s = socket;
        InetAddress addr = remoteAddr;
        if (s == null || s.isClosed() || addr == null) {
            return;
        }
        try {
            s.send(new DatagramPacket(data, data.length, addr, remotePort));
        } catch (IOException ignored) {
            // 发送失败交给看门狗判定离线
        }
    }

    public boolean isOpen() {
        return socket != null && !socket.isClosed();
    }

    public synchronized void close() {
        running = false;
        DatagramSocket s = socket;
        socket = null;
        if (s != null && !s.isClosed()) {
            s.close();
        }
    }
}
