package com.cherglow.gcs.ui.widget;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 信号卡（图形化，S13 反馈）：三条链路（RC / Web / MAV）各一个 WiFi 扇形图标，
 * 在线金色 / 离线暗灰；头部状态文字随任一链路在线点亮。
 */
public class SignalCard extends VBox {

    private final WifiLinkIcon rcIcon = new WifiLinkIcon();
    private final WifiLinkIcon webIcon = new WifiLinkIcon();
    private final WifiLinkIcon mavIcon = new WifiLinkIcon();
    private final Label state = new Label("未配备");

    public SignalCard() {
        getStyleClass().add("sensor-card");
        setSpacing(3);

        HBox head = new HBox(6);
        head.setAlignment(Pos.CENTER_LEFT);
        Label ic = new Label("≋");
        ic.getStyleClass().add("sensor-icon");
        Label t = new Label("信号");
        t.getStyleClass().add("sensor-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        state.getStyleClass().add("sensor-state");
        head.getChildren().addAll(ic, t, sp, state);

        HBox links = new HBox(10);
        links.setAlignment(Pos.CENTER);
        links.getChildren().addAll(
                group(rcIcon, "RC"), group(webIcon, "Web"), group(mavIcon, "MAV"));

        getChildren().addAll(head, links);
    }

    private VBox group(WifiLinkIcon icon, String name) {
        Label l = new Label(name);
        l.getStyleClass().add("sensor-state");
        VBox v = new VBox(2, icon, l);
        v.setAlignment(Pos.CENTER);
        return v;
    }

    /** 更新三条链路状态；lq 为 CRSF 上行质量（-1=无），有值时并入状态文字 */
    public void update(boolean rc, boolean web, boolean mav, int lq) {
        rcIcon.setOn(rc);
        webIcon.setOn(web);
        mavIcon.setOn(mav);
        boolean any = rc || web || mav;
        state.setText(any
                ? (lq >= 0 ? "在线 · 上行 " + lq + "%" : "在线")
                : "无链路");
        if (any) {
            state.getStyleClass().remove("offline");
        } else if (!state.getStyleClass().contains("offline")) {
            state.getStyleClass().add("offline");
        }
    }
}
