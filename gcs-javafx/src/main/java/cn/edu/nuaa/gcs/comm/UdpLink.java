package cn.edu.nuaa.gcs.comm;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class UdpLink implements Link {
    private DatagramSocket socket;
    private final int localPort;
    private final String remoteHost;
    private final int remotePort;
    private volatile boolean open = false;

    public UdpLink(int localPort, String remoteHost, int remotePort) {
        this.localPort = localPort;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    public void open() throws Exception {
        socket = new DatagramSocket(localPort);
        socket.setSoTimeout(5000);
        open = true;
    }

    @Override
    public byte[] read() throws Exception {
        if (!open) throw new IllegalStateException("Link not open");
        byte[] buf = new byte[2048];
        DatagramPacket pkt = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(pkt);
            byte[] data = new byte[pkt.getLength()];
            System.arraycopy(pkt.getData(), 0, data, 0, pkt.getLength());
            return data;
        } catch (SocketTimeoutException e) {
            return null;
        }
    }

    @Override
    public void send(byte[] data) throws Exception {
        if (!open) throw new IllegalStateException("Link not open");
        InetAddress addr = InetAddress.getByName(remoteHost);
        DatagramPacket pkt = new DatagramPacket(data, data.length, addr, remotePort);
        socket.send(pkt);
    }

    @Override
    public void close() {
        if (socket != null) socket.close();
        open = false;
    }

    @Override
    public boolean isOpen() { return open; }
}
