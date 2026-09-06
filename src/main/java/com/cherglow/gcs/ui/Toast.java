package com.cherglow.gcs.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * 轻量 Toast 通知：顶部居中叠加层，自动淡出。
 * 由 MainShell 挂载覆盖容器；全应用通过 {@link #show(String, Type)} 调用。
 */
public final class Toast {

    public enum Type { INFO, SUCCESS, WARNING, ERROR }

    private static final Color BORDER = Color.web("#30363d");
    private static Pane overlay;

    private Toast() {
    }

    /** 由 MainShell 在场景构建时注册顶层覆盖容器 */
    public static void bind(Pane overlayPane) {
        overlay = overlayPane;
    }

    public static void show(String message, Type type) {
        if (overlay == null || overlay.getScene() == null) {
            return;
        }
        Label label = new Label(message);
        label.getStyleClass().add("toast");
        label.setBorder(accentBorder(accentOf(type)));
        label.setTextFill(Color.web("#e6edf3"));

        var box = new StackPane(label);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(8, 0, 0, 0));
        box.setMouseTransparent(true);
        overlay.getChildren().add(box);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(120), box);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.millis(2400));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(260), box);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        SequentialTransition seq = new SequentialTransition(fadeIn, hold, fadeOut);
        seq.setOnFinished(e -> overlay.getChildren().remove(box));
        seq.play();

        // 同屏最多 4 条，防刷屏
        while (overlay.getChildren().size() > 4) {
            overlay.getChildren().remove(0);
        }
    }

    private static Color accentOf(Type type) {
        return switch (type) {
            case SUCCESS -> Color.web("#22c55e");
            case WARNING -> Color.web("#f59e0b");
            case ERROR -> Color.web("#ef4444");
            case INFO -> Color.web("#4b8bf5");
        };
    }

    private static Border accentBorder(Color accent) {
        return new Border(new BorderStroke(
                BORDER, BORDER, BORDER, accent,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                new CornerRadii(8), new BorderWidths(1, 1, 1, 3),
                Insets.EMPTY));
    }
}
