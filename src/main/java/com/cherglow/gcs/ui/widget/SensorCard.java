package com.cherglow.gcs.ui.widget;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 传感器小卡：图标字符 + 名称 + 两行数值。
 * 未配备传感器显示「未检测到 / —」空态（数据源 status 探测位）。
 */
public class SensorCard extends VBox {

    private final Label val1 = new Label("—");
    private final Label val2 = new Label("—");
    private final Label state = new Label("未配备");

    public SensorCard(String icon, String title) {
        getStyleClass().add("sensor-card");
        setSpacing(3);

        HBox head = new HBox(6);
        head.setAlignment(Pos.CENTER_LEFT);
        Label ic = new Label(icon);
        ic.getStyleClass().add("sensor-icon");
        Label t = new Label(title);
        t.getStyleClass().add("sensor-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        state.getStyleClass().add("sensor-state");
        head.getChildren().addAll(ic, t, sp, state);

        val1.getStyleClass().add("sensor-val");
        val2.getStyleClass().add("sensor-val");

        getChildren().addAll(head, val1, val2);
    }

    /** 设置在线状态（在线则正常配色，否则空态变暗） */
    public void setOnline(boolean online, String stateText) {
        if (online) {
            state.setText(stateText == null ? "在线" : stateText);
            state.getStyleClass().remove("offline");
            val1.getStyleClass().remove("dim");
            val2.getStyleClass().remove("dim");
        } else {
            state.setText(stateText == null ? "未配备" : stateText);
            if (!state.getStyleClass().contains("offline")) {
                state.getStyleClass().add("offline");
            }
            if (!val1.getStyleClass().contains("dim")) {
                val1.getStyleClass().add("dim");
            }
            if (!val2.getStyleClass().contains("dim")) {
                val2.getStyleClass().add("dim");
            }
            val1.setText("—");
            val2.setText("—");
        }
    }

    public Label val1() {
        return val1;
    }

    public Label val2() {
        return val2;
    }
}
