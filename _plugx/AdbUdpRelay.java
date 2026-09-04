import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ADB-UDP 中继：让电脑经「手机(已连 Drone_WiFi, 192.168.4.x)」访问无人机 UDP 14550 MAVLink。
 *
 * 拓扑：地面站 UDP:bridgePort -> [本程序] -> adb exec-in -> 手机 nc -u 192.168.4.1 14550
 *      飞控(广播/回发):14550 -> 手机 nc -l -u -p 14550 -> adb exec-out -> [本程序] -> 地面站
 *
 * 运行（Java 17+ 单文件）：java AdbUdpRelay.java <adb路径> <bridgePort> <droneHost> <dronePort> <phoneListenPort>
 * 例：java AdbUdpRelay.java adb.exe 15550 192.168.4.1 14550 14550
 */
public class AdbUdpRelay {
    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println("usage: java AdbUdpRelay.java <adbPath> <bridgePort> <droneHost> <dronePort> <phoneListenPort>");
            return;
        }
        String adb = args[0];
        final int bridgePort = Integer.parseInt(args[1]);
        String droneHost = args[2];
        int dronePort = Integer.parseInt(args[3]);
        int phoneListen = Integer.parseInt(args[4]);
        String adbDir = new File(adb).getAbsoluteFile().getParent();
        String adbExe = new File(adb).getAbsolutePath();

        // 方向1（下）：手机 nc -u 192.168.4.1 14550 （读 stdin → UDP 发飞控）
        // 用 while 循环常驻：toybox nc UDP 连接会在超时/异常后退出，自愈重启保持链路
        Process writer = new ProcessBuilder(adbExe, "exec-in",
                "while true; do toybox nc -u " + droneHost + " " + dronePort + " 2>/dev/null; sleep 0.5; done")
                .directory(new File(adbDir)).redirectErrorStream(true).start();
        // 方向2（上）：手机 nc -l -u -p 14550 （UDP 收飞控 → stdout → adb exec-out）
        Process reader = new ProcessBuilder(adbExe, "exec-out",
                "while true; do toybox nc -u -l -p " + phoneListen + " 2>/dev/null; sleep 1; done")
                .directory(new File(adbDir)).redirectErrorStream(true).start();

        // 本机 UDP 桥端口：地面站连接 127.0.0.1:bridgePort
        final DatagramSocket sock = new DatagramSocket(new InetSocketAddress("127.0.0.1", bridgePort));
        sock.setSoTimeout(1000);
        final SocketAddress[] gcs = new SocketAddress[1];
        final byte[] b1 = new byte[65536];
        final byte[] b2 = new byte[65536];
        AtomicLong up = new AtomicLong(), down = new AtomicLong();
        System.out.println("[RELAY] udp 127.0.0.1:" + bridgePort + " <-> adb <-> " + droneHost + ":" + dronePort + " (phone listen :" + phoneListen + ")");

        Thread stdoutPump = new Thread(() -> {     // reader.stdout -> gcs
            try (InputStream in = reader.getInputStream()) {
                int n; SocketAddress dst = null;
                while ((n = in.read(b2)) > 0) {
                    for (int tries = 0; gcs[0] == null && tries < 100; tries++) Thread.sleep(100);
                    dst = gcs[0];
                    if (dst == null) continue;
                    sock.send(new DatagramPacket(b2, n, dst));
                    up.addAndGet(n);
                    System.out.println("[RELAY] drone->gcs " + n + "B (up=" + up.get() + "B)");
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
        Thread stdinPump = new Thread(() -> {      // gcs -> writer.stdin
            try (OutputStream out = writer.getOutputStream()) {
                while (true) {
                    DatagramPacket p = new DatagramPacket(b1, b1.length);
                    try { sock.receive(p); } catch (SocketTimeoutException e) { continue; }
                    if (gcs[0] == null) gcs[0] = p.getSocketAddress();
                    out.write(p.getData(), 0, p.getLength());
                    out.flush();
                    down.addAndGet(p.getLength());
                    System.out.println("[RELAY] gcs->drone " + p.getLength() + "B (down=" + down.get() + "B)");
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
        stdoutPump.start();
        stdinPump.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sock.close();
            writer.destroyForcibly();
            reader.destroyForcibly();
            System.out.println("\n[RELAY] 中继已停止（up=" + up.get() + "B down=" + down.get() + "B）");
        }));
        System.out.println("[RELAY] 运行中，Ctrl+C 停止");
    }
}