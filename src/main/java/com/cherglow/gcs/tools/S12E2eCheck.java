package com.cherglow.gcs.tools;

import com.cherglow.gcs.core.CliLink;
import com.cherglow.gcs.core.ConnectionManager;
import com.cherglow.gcs.model.VehicleSnapshot;
import com.fazecast.jSerialComm.SerialPort;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * S12 端到端自检（真机，只读+安全操作）：
 * 枚举串口 → 连接 → 遥测流断言（ps/psq/mot/rc/imu/status）→ p 全量拉取 →
 * 单参数读回 → 模式切换回读（acro→stab，仅限未解锁落地）→ 断开清理。
 * 安全硬约束：绝不发送 arm/电机测试/参数写入/reboot；模式切换前校验 armed=false。
 * 运行：mvn -q compile 后
 * java -cp "target/classes;<jSerialComm.jar>;<javafx-base.jar>;<javafx-graphics.jar>"
 *      com.cherglow.gcs.tools.S12E2eCheck [COM5] [115200]
 * javafx 依赖仅为 FxSafe 引用的 Platform 类；无工具链时 FxSafe 退化为同步执行。
 */
public final class S12E2eCheck {

    private static final List<String> FAILS = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        String want = args.length > 0 ? args[0] : "COM5";
        int baud = args.length > 1 ? Integer.parseInt(args[1]) : 115200;
        System.out.println("== S12E2eCheck " + want + "@" + baud + " ==");

        // 1) 枚举串口
        AtomicBoolean found = new AtomicBoolean(false);
        StringBuilder ports = new StringBuilder();
        for (SerialPort p : SerialPort.getCommPorts()) {
            ports.append(p.getSystemPortName()).append(' ');
            if (p.getSystemPortName().equalsIgnoreCase(want)) {
                found.set(true);
            }
        }
        System.out.println("  可用端口: " + ports);
        check("1 枚举到目标串口 " + want, found::get);

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cli-link");
            t.setDaemon(true);
            return t;
        });

        AtomicReference<VehicleSnapshot> latest = new AtomicReference<>();
        AtomicBoolean sawPs = new AtomicBoolean(), sawPsq = new AtomicBoolean(),
                sawMot = new AtomicBoolean(), sawRc = new AtomicBoolean(),
                sawImu = new AtomicBoolean(), sawStatus = new AtomicBoolean();

        CliLink link = new CliLink(want, baud, exec, s -> {
            latest.set(s);
            if (s.rollDeg == s.rollDeg && s.yawDeg == s.yawDeg) {
                sawPs.set(true);
            }
            if (s.qw == s.qw && !Double.isNaN(s.qw)) {
                sawPsq.set(true);
            }
            if (s.motorFR == s.motorFR) {
                sawMot.set(true);
            }
            if (s.rcChannels != null && s.rcChannels.length >= 4) {
                sawRc.set(true);
            }
            if (s.imuGyro != null) {
                sawImu.set(true);
            }
            if (s.armed != null && s.phase != null) {
                sawStatus.set(true);
            }
        });
        link.setErrorListener(err -> System.out.println("  [serial-error] " + err));
        ConnectionManager cm = new ConnectionManager(link, exec, System::currentTimeMillis);
        cm.startWatchdog();

        // 2) 连接
        cm.connect();
        boolean connected = await(() -> cm.getState() == ConnectionManager.State.CONNECTED, 10_000);
        if (!connected) {
            System.out.println("  连接失败: state=" + cm.getState() + " detail=" + cm.getDetail());
        }
        check("2 连接 " + want + "@" + baud, () -> connected);

        // 3) 遥测流（轮询 6s，覆盖完整轮询计划多轮）
        Thread.sleep(6_000);
        check("3a ps 姿态流", sawPs::get);
        check("3b psq 四元数流", sawPsq::get);
        check("3c mot 电机流", sawMot::get);
        check("3d rc 通道流", sawRc::get);
        check("3e imu 流", sawImu::get);
        check("3f status 状态流", sawStatus::get);
        VehicleSnapshot s = latest.get();
        if (s != null) {
            System.out.printf("  快照: loop=%dHz bat=%.3fV armed=%s phase=%s mode=%s 禁解锁=%s%n",
                    s.loopRate, s.batteryVoltage, s.armed, s.phase, s.modeName, s.armingDisabled);
            check("3g 主循环频率 > 500Hz", () -> latest.get().loopRate > 500);
            if (s.armed != null && s.armed) {
                System.out.println("  ⚠ 设备处于解锁状态，跳过模式切换项（安全）");
            } else {
                // 6) 模式切换回读（未解锁落地安全；rc 轮询回读 mode）
                send(link, "acro");
                boolean acro = await(() -> isMode(latest, "ACRO"), 5_000);
                check("6a 切 ACRO 回读", () -> acro);
                send(link, "stab");
                boolean stab = await(() -> isMode(latest, "STAB"), 5_000);
                check("6b 切回 STAB 回读", () -> stab);
            }
        } else {
            check("3 遥测快照非空", () -> false);
        }

        // 4) p 全量拉取（预算 2.6s）
        CountDownLatch all = new CountDownLatch(1);
        AtomicReference<String> allOut = new AtomicReference<>();
        link.sendCommand("p", out -> {
            allOut.set(out);
            all.countDown();
        });
        boolean allDone = all.await(8, TimeUnit.SECONDS);
        int paramCount = 0;
        String firstName = null;
        if (allDone && allOut.get() != null) {
            for (String line : allOut.get().split("\n")) {
                int eq = line.indexOf(" = ");
                if (eq > 0 && Character.isJavaIdentifierStart(line.charAt(0))) {
                    if (firstName == null) {
                        firstName = line.substring(0, eq).trim();
                    }
                    paramCount++;
                }
            }
        }
        int n = paramCount;
        String fn = firstName;
        System.out.println("  全量参数 " + n + " 项" + (fn != null ? "（首项 " + fn + "）" : ""));
        check("4 p 全量拉取 ≥ 60 项", () -> allDone && n >= 60);

        // 5) 单参数读回
        if (fn != null) {
            CountDownLatch one = new CountDownLatch(1);
            AtomicReference<String> oneOut = new AtomicReference<>();
            link.sendCommand("p " + fn, out -> {
                oneOut.set(out);
                one.countDown();
            });
            boolean oneDone = one.await(3, TimeUnit.SECONDS);
            String got = oneOut.get();
            System.out.println("  读回: " + (got == null ? "<超时>" : got.trim()));
            check("5 单参数读回 " + fn, () -> oneDone && got != null && got.contains(fn) && got.contains(" = "));
        }

        // 7) 断开清理
        cm.disconnect();
        boolean disc = await(() -> cm.getState() == ConnectionManager.State.DISCONNECTED, 3_000);
        check("7 断开清理", () -> disc);
        exec.shutdownNow();

        finish();
    }

    private static void send(CliLink link, String cmd) {
        CountDownLatch latch = new CountDownLatch(1);
        link.sendCommand(cmd, out -> latch.countDown());
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isMode(AtomicReference<VehicleSnapshot> latest, String mode) {
        VehicleSnapshot s = latest.get();
        return s != null && s.modeName != null && s.modeName.trim().equalsIgnoreCase(mode);
    }

    private static boolean await(BooleanSupplier cond, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (cond.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return cond.getAsBoolean();
    }

    private static void check(String name, BooleanSupplier ok) {
        boolean pass = ok.getAsBoolean();
        System.out.println((pass ? "  ✓ " : "  ✗ ") + name);
        if (!pass) {
            FAILS.add(name);
        }
    }

    private static void finish() {
        System.out.println("== 结果: " + (FAILS.isEmpty() ? "全部通过" : FAILS.size() + " 项失败 " + FAILS) + " ==");
        System.exit(FAILS.isEmpty() ? 0 : 1);
    }

    private S12E2eCheck() {
    }
}
