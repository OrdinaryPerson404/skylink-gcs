package com.cherglow.gcs.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 深色确认对话框（解锁等安全指令的二次确认）。
 */
public final class ConfirmDialog {

    private ConfirmDialog() {
    }

    public static void show(String title, String message, Runnable onConfirm) {
        Stage st = new Stage(StageStyle.TRANSPARENT);
        st.initModality(Modality.APPLICATION_MODAL);

        VBox card = new VBox(14);
        card.getStyleClass().add("dialog-card");
        card.setPrefWidth(440);

        Label t = new Label(title);
        t.getStyleClass().add("dialog-title");
        Label msg = new Label(message);
        msg.getStyleClass().add("dialog-msg");
        msg.setWrapText(true);

        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button cancel = new Button("取消");
        cancel.getStyleClass().add("btn-soft");
        cancel.setOnAction(e -> st.close());
        Button ok = new Button("确认");
        ok.getStyleClass().add("btn-danger");
        ok.setOnAction(e -> {
            st.close();
            onConfirm.run();
        });
        btns.getChildren().addAll(cancel, ok);

        card.getChildren().addAll(t, msg, btns);
        StackPane root = new StackPane(card);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root, 480, 220, Color.TRANSPARENT);
        ThemeManager.apply(scene);
        st.setScene(scene);
        st.show();
        st.centerOnScreen();
    }
}
