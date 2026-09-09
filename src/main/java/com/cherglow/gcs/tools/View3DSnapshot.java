package com.cherglow.gcs.tools;

import com.cherglow.gcs.model.LiveVehicle;
import com.cherglow.gcs.ui.widget.Drone3DView;
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
 * 3D 视图快照自检工具（开发辅助，不参与主程序）：
 * 依次设置固定姿态，把 Drone3DView 渲染结果导出 PNG，供肉眼核对
 * 地平线反应 / 罗盘方位 / 三轴方向是否与固件口径一致。
 * 运行：mvn compile 后
 * java -Dsnap.out=<输出目录> --module-path &lt;javafx模块&gt; --add-modules javafx.controls
 *      -cp target/classes com.cherglow.gcs.tools.View3DSnapshot
 */
public class View3DSnapshot extends Application {

    private static final String OUT_DIR = System.getProperty("snap.out", ".");

    /** {roll, pitch, yaw}（固件 ps 口径：+roll=右倾，+pitch=低头，+yaw=左转） */
    private static final double[][] CASES = {
            {0, 0, 0},        // 基准
            {30, 0, 0},       // 右倾 30°：右翼应朝地面一侧下沉
            {-30, 0, 0},      // 左倾 30°
            {0, 20, 0},       // 低头 20°：机头埋向地面
            {0, -20, 0},      // 抬头 20°：机头指天空
            {0, 0, 90},       // 左转 90°：机头指向左侧（E 应到机头右后方）
            {0, 0, -90},      // 右转 90°：机头指向 E
            {0, 0, 109.4},    // 用户示例航向
            {15, -12, 45},    // 组合姿态 + 三轴箭头
    };

    private final LiveVehicle lv = LiveVehicle.get();
    private Drone3DView view;

    @Override
    public void start(Stage stage) {
        view = new Drone3DView();
        view.setPrefSize(340, 280);
        StackPane root = new StackPane(view);
        stage.setScene(new Scene(root, 340, 280));
        stage.setTitle("3D 快照自检");
        stage.show();
        snapshotSeq(0);
    }

    private void snapshotSeq(int idx) {
        if (idx >= CASES.length) {
            Platform.exit();
            return;
        }
        double[] c = CASES[idx];
        lv.injectPoseForTool(c[0], c[1], c[2]);
        view.setAxesVisible(idx == CASES.length - 1);
        PauseTransition p = new PauseTransition(Duration.millis(250));
        p.setOnFinished(e -> {
            try {
                WritableImage img = view.snapshot(null, null);
                int iw = (int) img.getWidth(), ih = (int) img.getHeight();
                int[] buf = new int[iw * ih];
                img.getPixelReader().getPixels(0, 0, iw, ih, PixelFormat.getIntArgbInstance(), buf, 0, iw);
                BufferedImage bi = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
                bi.setRGB(0, 0, iw, ih, buf, 0, iw);
                File dir = new File(OUT_DIR);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new IllegalStateException("无法创建输出目录: " + OUT_DIR);
                }
                File out = new File(dir, String.format("case_%d_r%+.0f_p%+.0f_y%+.1f.png", idx, c[0], c[1], c[2]));
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
