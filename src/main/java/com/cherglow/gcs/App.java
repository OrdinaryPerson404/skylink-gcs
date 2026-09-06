package com.cherglow.gcs;

import com.cherglow.gcs.core.ConnectionService;
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
    }

    @Override
    public void stop() {
        ConnectionService.get().shutdown();
    }
}
