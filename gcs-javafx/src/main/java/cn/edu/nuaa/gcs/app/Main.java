package cn.edu.nuaa.gcs.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import cn.edu.nuaa.gcs.data.AppSettings;
import cn.edu.nuaa.gcs.ui.MainController;
import cn.edu.nuaa.gcs.ui.ThemeManager;

public class Main extends Application {
    private final AppSettings settings = new AppSettings();

    @Override
    public void start(Stage stage) throws Exception {
        settings.load();
        ThemeManager.init(settings.theme);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1400, 900);
        ThemeManager.applyTheme(scene);

        stage.setTitle("SkyLink 天链 · 无人机地面控制站 v2.1");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();

        MainController ctrl = loader.getController();
        ctrl.init(stage, settings);
    }

    @Override
    public void stop() {
        settings.save();
    }

    public static void main(String[] args) {
        // WebView 内高德暗色底图使用 CSS filter 实现；Windows Direct3D 管线合成
        // 滤镜时会触发 Prism RTTexture 渲染缺陷（瓦片整层不绘制）。
        // 软件渲染管线可稳定渲染滤镜内容；若用户已手动指定 prism.order 则尊重之。
        if (System.getProperty("prism.order") == null
                && System.getProperty("os.name", "").toLowerCase().contains("win")) {
            System.setProperty("prism.order", "sw");
        }
        launch(args);
    }
}
