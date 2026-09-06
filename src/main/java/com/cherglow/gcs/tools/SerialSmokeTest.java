package com.cherglow.gcs.tools;

import com.cherglow.gcs.core.CliLink;
import com.cherglow.gcs.core.ConnectionManager;
import com.cherglow.gcs.model.VehicleSnapshot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * S2 验收 smoke：真机 COM5 连接 → N 秒遥测流 → 断开清理。
 * 运行：java -cp "target/classes;<jSerialComm.jar>" com.cherglow.gcs.tools.SerialSmokeTest COM5 115200 6
 * 安全：仅轮询只读命令，绝不发送 arm/电机测试。
 */
public final class SerialSmokeTest {

    public static void main(String[] args) throws Exception {
        String port = args.length > 0 ? args[0] : "COM5";
        int baud = args.length > 1 ? Integer.parseInt(args[1]) : 115200;
        int seconds = args.length > 2 ? Integer.parseInt(args[2]) : 6;

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cli-link");
            t.setDaemon(true);
            return t;
        });

        CliLink link = new CliLink(port, baud, exec, snapshot -> printSnapshot(snapshot));
        link.setErrorListener(err -> System.out.println("[serial-error] " + err));
        ConnectionManager cm = new ConnectionManager(link, exec, System::currentTimeMillis);
        cm.addStateListener(s -> System.out.println("[state] " + s
                + (cm.isRetrying() ? " (retrying)" : "")
                + (cm.getDetail().isEmpty() ? "" : " — " + cm.getDetail())));
        cm.startWatchdog();

        System.out.println("== SerialSmokeTest " + port + "@" + baud + " for " + seconds + "s ==");
        cm.connect();
        Thread.sleep(seconds * 1000L);
        System.out.println("== disconnecting ==");
        cm.disconnect();
        Thread.sleep(300);
        exec.shutdownNow();
        System.out.println("== done, final state = " + cm.getState() + " ==");
    }

    private static void printSnapshot(VehicleSnapshot s) {
        StringBuilder sb = new StringBuilder(160);
        sb.append(String.format("roll=%7.2f pitch=%7.2f yaw=%7.2f",
                s.rollDeg, s.pitchDeg, s.yawDeg));
        if (s.motorFR == s.motorFR) {
            sb.append(String.format(" mot=[%.2f %.2f %.2f %.2f]", s.motorFR, s.motorFL, s.motorRR, s.motorRL));
        }
        if (s.batteryVoltage == s.batteryVoltage) {
            sb.append(String.format(" bat=%.2fV", s.batteryVoltage));
        }
        if (s.armed != null) {
            sb.append(" armed=").append(s.armed ? 1 : 0);
        }
        if (s.phase != null) {
            sb.append(" phase=").append(s.phase);
        }
        if (s.modeName != null) {
            sb.append(" mode=").append(s.modeName);
        }
        if (s.loopRate > 0) {
            sb.append(" loop=").append(s.loopRate).append("Hz");
        }
        if (s.armingDisabled != null) {
            sb.append(" 禁止解锁[").append(s.armingDisabled).append(']');
        }
        System.out.println(sb);
    }

    private SerialSmokeTest() {
    }
}
