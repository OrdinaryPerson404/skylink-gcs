package com.cherglow.gcs.ui.pages;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * 占位页基类：居中卡片显示页面名与所属 story，后续 story 逐页替换实现。
 */
public abstract class BasePage extends StackPane {

    protected BasePage(String title, String note) {
        getStyleClass().add("page-root");
        var card = new VBox(8);
        card.getStyleClass().add("ph-card");
        card.setAlignment(Pos.CENTER);

        var t = new Label(title);
        t.getStyleClass().add("ph-title");
        var n = new Label(note);
        n.getStyleClass().add("ph-note");

        card.getChildren().addAll(t, n);
        getChildren().add(card);
    }
}
