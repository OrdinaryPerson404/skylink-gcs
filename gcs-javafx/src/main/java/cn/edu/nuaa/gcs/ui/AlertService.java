package cn.edu.nuaa.gcs.ui;

import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class AlertService {
    public static void info(Stage owner, String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static void warn(Stage owner, String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static void error(Stage owner, String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    static Alert createConfirm(Stage owner, String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        return a;
    }
}
