package com.cherglow.gcs;

import com.cherglow.gcs.core.ConnectionService;
import com.cherglow.gcs.map.TileServer;
import com.cherglow.gcs.model.LiveVehicle;
import com.cherglow.gcs.transport.GpsHttpServer;
import com.cherglow.gcs.util.FxSafe;
import com.cherglow.gcs.ui.Toast;
import java.util.concurrent.atomic.AtomicBoolean;
import com.cherglow.gcs.ui.MainShell;
import com.cherglow.gcs.ui.ThemeManager;
import com.cherglow.gcs.ui.dialog.ConnectDialog;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * CHERGLOW GROUND CONTROL 主应用。
 * 运行：mvn javafx:run（S1 验收：5 页切换 / 快捷键 1-5 / 连接 chip / Toast）
 */
public class App extends Application {

    private final GpsHttpServer gpsServer = new GpsHttpServer();
    private final TileServer tileServer = new TileServer();
    private final AtomicBoolean firstGpsFix = new AtomicBoolean(false);

    /** S14：瓦片服务（0.0.0.0:8135，手机连通性验证）与 GNSS HTTP 推流端点（0.0.0.0:8080） */
    private void startLocalServers() {
        try {
            tileServer.start(TileServer.defaultRoot());
        } catch (Exception e) {
            System.out.println("[tiles] 启动失败: " + e);
        }
        try {
            gpsServer.start(8080, (lat, lon, alt, spd) -> {
                System.out.printf("[gnss] lat=%.6f lon=%.6f alt=%.1f spd=%.2f%n", lat, lon, alt, spd);
                LiveVehicle.get().updateGps(lat, lon, spd);
                if (firstGpsFix.compareAndSet(false, true)) {
                    FxSafe.run(() -> Toast.show("GNSS 推流已接入：lat=" + lat + ", lon=" + lon, Toast.Type.SUCCESS));
                }
            });
            System.out.println("[gnss] HTTP 推流端点 http://0.0.0.0:8080/gps 已启动");
        } catch (Exception e) {
            System.out.println("[gnss] 8080 启动失败（端口被占用?）: " + e);
        }
    }

    @Override
    public void start(Stage stage) {
        var shell = new MainShell();
        Scene scene = new Scene(shell, 1600, 900);
        ThemeManager.setMainScene(scene);
        ThemeManager.apply(scene);
        shell.switchPage("overview");
        shell.installKeyboardShortcuts(scene);

        stage.setTitle("CHERGLOW GROUND CONTROL");
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();

        // S4：连接对话框（需要主 Stage 定位）
        shell.setConnectOpener(() -> new ConnectDialog().show());

        startLocalServers();
    }

    @Override
    public void stop() {
        gpsServer.stop();
        tileServer.stop(); // 释放 8135，避免残留进程导致下次启动瓦片服务绑端口失败（地图黑屏）
        ConnectionService.get().shutdown();
    }
}
