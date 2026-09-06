package com.cherglow.gcs.tools;

import com.cherglow.gcs.model.LiveVehicle;
import com.cherglow.gcs.ui.widget.AdiWidget;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * ADI 姿态仪快照自检工具（开发辅助，不参与主程序）：
 * 依次设置固定姿态导出 PNG，核对天地仪/俯仰梯方向是否与固件口径一致
 * （+pitch=低头 → 地平线上移、飞机符号指向「+」梯度线）。
 * 运行：mvn compile 后
 * java -Dsnap.out=<输出目录> --module-path &lt;javafx模块&gt; --add-modules javafx.controls
 *      -cp target/classes com.cherglow.gcs.tools.AdiSnapshot
 */
public class AdiSnapshot extends Application {

    private static final String OUT_DIR = System.getProperty("snap.out", ".");

    /** {roll, pitch}（固件 ps 口径：+roll=右倾，+pitch=低头） */
    private static final double[][] CASES = {
            {0, 0},      // 基准：地平线居中，+10 线在上（固件 -10 抬头）
            {0, 20},     // 低头 20°：地平线明显上移，飞机符号指在「+20」线附近
            {0, -20},    // 抬头 20°：地平线下移，飞机符号指在「-20」线附近
            {30, 0},     // 右倾 30°：地平线绕中心反向倾斜
    };

    private final LiveVehicle lv = LiveVehicle.get();
    private AdiWidget adi;

    @Override
    public void start(Stage stage) {
        adi = new AdiWidget();
        adi.setPrefSize(360, 300);
        StackPane root = new StackPane(adi);
        stage.setScene(new Scene(root, 360, 300));
        stage.setTitle("ADI 快照自检");
        stage.show();
        snapshotSeq(0);
    }

    private void snapshotSeq(int idx) {
        if (idx >= CASES.length) {
            Platform.exit();
            return;
        }
        double[] c = CASES[idx];
        lv.rollDeg.set(c[0]);
        lv.pitchDeg.set(c[1]);
        PauseTransition p = new PauseTransition(Duration.millis(250));
        p.setOnFinished(e -> {
            try {
                WritableImage img = adi.snapshot(null, null);
                int iw = (int) img.getWidth(), ih = (int) img.getHeight();
                int[] buf = new int[iw * ih];
                img.getPixelReader().getPixels(0, 0, iw, ih, PixelFormat.getIntArgbInstance(), buf, 0, iw);
                BufferedImage bi = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
                bi.setRGB(0, 0, iw, ih, buf, 0, iw);
                File dir = new File(OUT_DIR);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new IllegalStateException("无法创建输出目录: " + OUT_DIR);
                }
                File out = new File(dir, String.format("adi_%d_r%+.0f_p%+.0f.png", idx, c[0], c[1]));
                ImageIO.write(bi, "png", out);
                System.out.println("saved: " + out.getAbsolutePath());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            snapshotSeq(idx + 1);
        });
        p.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
