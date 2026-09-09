package com.cherglow.gcs.tools;

import com.cherglow.gcs.core.ConnectionService;
import com.cherglow.gcs.ui.MainShell;
import com.cherglow.gcs.ui.ThemeManager;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 全页面 UI 快照工具（开发辅助，不参与主程序）：
 * 依次切换五个主页面并整屏导出 PNG，供文档/验收使用。
 * 可选先连接真机串口，让页面呈现真实遥测数据（禁假数据铁律：无连接即为真实空态）。
 * 运行：mvn compile 后
 * java -Dsnap.out=<输出目录> [-Dsnap.port=COM5] [-Dsnap.wait=12000]
 *      --module-path <javafx模块jar> --add-modules javafx.controls
 *      -cp "target/classes;<jSerialComm.jar>" com.cherglow.gcs.tools.UiSnapshot
 */
public class UiSnapshot extends Application {

    private static final String OUT_DIR = System.getProperty("snap.out", ".");
    private static final String PORT = System.getProperty("snap.port", "");
    private static final long CONNECT_WAIT_MS = Long.getLong("snap.wait", 12000L);

    /** {文件名, pageId, 停留毫秒}：数据页停留久以积累波形采样 */
    private static final String[][] PAGES = {
            {"01_overview", "overview", "1500"},
            {"02_fly", "fly", "2500"},
            {"03_plan", "plan", "3000"},
            {"04_setup", "setup", "1500"},
            {"05_data_wave", "data", "12000"},
    };

    private Stage stage;
    private int idx = 0;
    private Scene scene;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("UI Snapshot");
        stage.setX(40);
        stage.setY(40);
        if (!PORT.isEmpty()) {
            try {
                ConnectionService.get().connectSerial(PORT, 115200);
                System.out.println("[snap] connecting " + PORT + " @115200, warm " + CONNECT_WAIT_MS + "ms");
            } catch (Exception ex) {
                System.out.println("[snap] connect failed (offline mode): " + ex.getMessage());
            }
        }
        PauseTransition warm = new PauseTransition(Duration.millis(PORT.isEmpty() ? 1500 : CONNECT_WAIT_MS));
        warm.setOnFinished(e -> {
            MainShell shell = new MainShell();
            scene = new Scene(shell, 1440, 860);
            ThemeManager.setMainScene(scene);
            ThemeManager.apply(scene);
            shell.switchPage("overview");
            stage.setScene(scene);
            stage.show();
            next(shell);
        });
        warm.play();
    }

    private void next(MainShell shell) {
        if (idx >= PAGES.length) {
            finish();
            return;
        }
        String[] spec = PAGES[idx];
        try {
            shell.switchPage(spec[1]);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        PauseTransition p = new PauseTransition(Duration.millis(Long.parseLong(spec[2])));
        p.setOnFinished(e -> {
            try {
                save(spec[0], scene.getRoot());
                System.out.println("[snap] saved " + spec[0] + ".png");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            idx++;
            next(shell);
        });
        p.play();
    }

    private void finish() {
        try {
            if (!PORT.isEmpty()) {
                ConnectionService.get().disconnect();
            }
        } catch (Throwable ignored) {
        }
        PauseTransition bye = new PauseTransition(Duration.millis(300));
        bye.setOnFinished(e -> Platform.exit());
        bye.play();
    }

    private void save(String name, javafx.scene.Parent root) throws Exception {
        WritableImage img = root.snapshot(null, null);
        int iw = (int) img.getWidth(), ih = (int) img.getHeight();
        int[] buf = new int[iw * ih];
        img.getPixelReader().getPixels(0, 0, iw, ih, PixelFormat.getIntArgbInstance(), buf, 0, iw);
        BufferedImage bi = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        bi.setRGB(0, 0, iw, ih, buf, 0, iw);
        File dir = new File(OUT_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("cannot create out dir: " + OUT_DIR);
        }
        ImageIO.write(bi, "png", new File(dir, name + ".png"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
