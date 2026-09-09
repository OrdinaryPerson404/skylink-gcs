package com.cherglow.gcs.core;

import com.cherglow.gcs.model.LiveVehicle;
import com.cherglow.gcs.util.FxSafe;
import com.cherglow.gcs.ui.Toast;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 应用级连接服务（单例）：持有 ConnectionManager/CliLink，
 * 桥接 状态机→AppState(顶栏 chip) 与 遥测快照→LiveVehicle(页面绑定)。
 */
public final class ConnectionService {

    private static final ConnectionService INSTANCE = new ConnectionService();

    public static ConnectionService get() {
        return INSTANCE;
    }

    private ConnectionService() {
    }

    private ScheduledExecutorService exec;
    private ConnectionManager cm;
    private CommandLink link;
    private volatile String desc = "";

    private synchronized ScheduledExecutorService ensureExec() {
        if (exec == null || exec.isShutdown()) {
            exec = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "connection");
                t.setDaemon(true);
                return t;
            });
        }
        return exec;
    }

    /** 串口（CLI 链路）连接；重复调用会先断开旧连接 */
    public synchronized void connectSerial(String port, int baud) {
        if (cm != null) {
            cm.disconnect();
        }
        ScheduledExecutorService ex = ensureExec();
        desc = port + "@" + baud;

        CliLink cli = new CliLink(port, baud, ex, snapshot -> FxSafe.run(() -> LiveVehicle.get().updateFrom(snapshot)));
        cli.setErrorListener(err -> FxSafe.run(() ->
                Toast.show("串口错误：" + err, Toast.Type.WARNING)));
        link = cli;

        ConnectionManager m = new ConnectionManager(cli, ex, System::currentTimeMillis);
        m.addStateListener(s -> FxSafe.run(() -> {
            AppState.ConnStatus ui = switch (s) {
                case DISCONNECTED -> AppState.ConnStatus.DISCONNECTED;
                case CONNECTING -> AppState.ConnStatus.CONNECTING;
                case CONNECTED -> AppState.ConnStatus.CONNECTED;
                case ERROR -> AppState.ConnStatus.ERROR;
            };
            AppState.get().connStatusProperty().set(ui);
            LiveVehicle.get().setConnected(s == ConnectionManager.State.CONNECTED);
            switch (s) {
                case CONNECTED -> {
                    AppState.get().connDetailProperty().set(desc);
                    Toast.show("已连接 " + desc, Toast.Type.SUCCESS);
                }
                case ERROR -> {
                    AppState.get().connDetailProperty().set(m.getDetail());
                    Toast.show("连接失败：" + m.getDetail(), Toast.Type.ERROR);
                }
                default -> AppState.get().connDetailProperty().set("");
            }
        }));
        cm = m;
        m.connect();
    }

    /** MAVLink/UDP（WiFi 直连，S13）连接；重复调用会先断开旧连接 */
    public synchronized void connectUdp(String remoteIp, int remotePort) {
        if (cm != null) {
            cm.disconnect();
        }
        ScheduledExecutorService ex = ensureExec();
        desc = remoteIp + ":" + remotePort + " (UDP)";

        MavLinkLink ml = new MavLinkLink(remoteIp, remotePort, ex,
                snapshot -> FxSafe.run(() -> LiveVehicle.get().updateFrom(snapshot)));
        link = ml;

        ConnectionManager m = new ConnectionManager(ml, ex, System::currentTimeMillis);
        m.addStateListener(s -> FxSafe.run(() -> {
            AppState.ConnStatus ui = switch (s) {
                case DISCONNECTED -> AppState.ConnStatus.DISCONNECTED;
                case CONNECTING -> AppState.ConnStatus.CONNECTING;
                case CONNECTED -> AppState.ConnStatus.CONNECTED;
                case ERROR -> AppState.ConnStatus.ERROR;
            };
            AppState.get().connStatusProperty().set(ui);
            LiveVehicle.get().setConnected(s == ConnectionManager.State.CONNECTED);
            switch (s) {
                case CONNECTED -> {
                    AppState.get().connDetailProperty().set(desc);
                    Toast.show("已连接 " + desc, Toast.Type.SUCCESS);
                }
                case ERROR -> {
                    AppState.get().connDetailProperty().set(m.getDetail());
                    Toast.show("连接失败：" + m.getDetail(), Toast.Type.ERROR);
                }
                default -> AppState.get().connDetailProperty().set("");
            }
        }));
        cm = m;
        m.startWatchdog();
        m.connect();
    }

    public synchronized void disconnect() {
        if (cm != null) {
            cm.disconnect();
        }
    }

    public boolean isConnected() {
        return cm != null && cm.getState() == ConnectionManager.State.CONNECTED;
    }

    /** 连接描述（如 "COM6@115200"），未连接时返回空串 */
    public String getDesc() {
        return desc;
    }

    /** 按需读取（p/sys/wifi 等只读命令），结果并入遥测快照 */
    public void requestOnce(String cmd) {
        if (!isConnected()) {
            return;
        }
        link.requestOnce(cmd);
    }

    /** 拉取全量参数 */
    public void requestParams() {
        requestOnce("p");
    }

    /** 发送 CLI 指令（未连接时提示并忽略）；out 在 FX 线程回调原始输出 */
    public void sendCommand(String cmd, java.util.function.Consumer<String> out) {
        if (!isConnected()) {
            FxSafe.run(() -> Toast.show("未连接飞控，无法发送指令", Toast.Type.WARNING));
            return;
        }
        link.sendCommand(cmd, out);
    }

    public String currentDesc() {
        return desc;
    }

    /** 应用退出时调用 */
    public synchronized void shutdown() {
        if (cm != null) {
            cm.disconnect();
        }
        if (exec != null) {
            exec.shutdownNow();
        }
    }
}
