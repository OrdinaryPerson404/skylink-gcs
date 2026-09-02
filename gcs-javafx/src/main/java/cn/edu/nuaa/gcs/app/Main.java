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
        launch(args);
    }
}
