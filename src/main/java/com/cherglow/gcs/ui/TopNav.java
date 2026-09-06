package com.cherglow.gcs.ui;

import com.cherglow.gcs.core.AppState;
import com.cherglow.gcs.util.Browse;
import javafx.beans.binding.Bindings;
import javafx.geometry.Side;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 顶部导航（48px）：品牌 + 5 页签 + 铃铛 + 连接 chip + 品牌 + 用户。
 * 视觉规格对齐原型 .topnav / .nav-tab / .conn-dot。
 */
public class TopNav extends HBox {

    public record Tab(String id, String label, String iconPath) {
    }

    public static final List<Tab> TABS = List.of(
            new Tab("overview", "仪表盘", Icons.DASHBOARD),
            new Tab("fly", "飞行", Icons.FLY),
            new Tab("plan", "规划", Icons.PLAN),
            new Tab("setup", "调参", Icons.SETUP),
            new Tab("data", "数据", Icons.DATA));

    private static final Color STROKE_IDLE = Color.web("#8b949e");
    private static final Color STROKE_ACTIVE = Color.web("#e6edf3");

    private final Map<Button, List<Node>> tabIcons = new LinkedHashMap<>();
    private final Map<Button, String> tabIds = new LinkedHashMap<>();

    private final Circle connDot = new Circle(4);
    private final Label connLabel = new Label("未连接");
    private Button activeTab;
    private ContextMenu userMenu;

    public TopNav(Consumer<String> onPageSelected, Runnable onBellClick, Runnable onUserClick,
                  Runnable onConnClick) {
        getStyleClass().add("topnav");
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(48);
        setMinHeight(48);

        // ---- 品牌 ----
        var brandIcon = Icons.filled(Icons.HEX, Color.web("#f0a500"), 22);
        var rotors = Icons.icon(Icons.ROTORS, 22);
        Icons.tint(rotors, Color.web("#0d1117"));
        var brandStack = new javafx.scene.layout.StackPane(brandIcon, rotors);
        var brandText = new VBox(0);
        var t1 = new Label("CHERGLOW");
        t1.getStyleClass().add("brand-title");
        var t2 = new Label("GROUND CONTROL");
        t2.getStyleClass().add("brand-sub");
        brandText.getChildren().addAll(t1, t2);
        var brand = new HBox(8, brandStack, brandText);
        brand.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(brand);

        // ---- 页签 ----
        var tabs = new HBox();
        tabs.setAlignment(Pos.CENTER_LEFT);
        for (Tab tab : TABS) {
            Button b = new Button(tab.label());
            b.getStyleClass().add("nav-tab");
            Node icon = Icons.icon(tab.iconPath(), 14);
            b.setGraphic(icon);
            b.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
            b.setGraphicTextGap(6);
            b.setOnAction(e -> onPageSelected.accept(tab.id()));
            tabIcons.put(b, List.of(icon));
            tabIds.put(b, tab.id());
            tabs.getChildren().add(b);
        }
        getChildren().add(tabs);

        // ---- 右侧 ----
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);

        Button bell = ghostButton(Icons.BELL, onBellClick);
        getChildren().add(bell);
        Button chip = connChip();
        chip.setOnAction(e -> {
            if (onConnClick != null) {
                onConnClick.run();
            }
        });
        getChildren().add(chip);
        getChildren().add(brandGcc());
        Button user = ghostButton(Icons.USER, null);
        user.setOnAction(e -> showUserMenu(user));
        getChildren().add(user);

        setActive("overview");
        bindConnState();
    }

    private Button ghostButton(String iconPath, Runnable action) {
        Button b = new Button();
        b.getStyleClass().add("icon-btn");
        Node icon = Icons.icon(iconPath, 16);
        b.setGraphic(icon);
        b.setOnAction(e -> {
            if (action != null) {
                action.run();
            }
        });
        return b;
    }

    private Button connChip() {
        Button chip = new Button();
        chip.getStyleClass().add("conn-chip");
        connLabel.getStyleClass().add("conn-label");
        var box = new HBox(6, connDot, connLabel);
        box.setAlignment(Pos.CENTER);
        chip.setGraphic(box);
        return chip;
    }

    /** 右上角项目品牌：四旋翼 LOGO + skylink-gcc（对齐图四/更名要求） */
    private HBox brandGcc() {
        var logo = Icons.droneLogo(18, Color.web("#c9d1d9"), Color.web("#3b82f6"));
        Label l = new Label("skylink-gcc");
        l.getStyleClass().add("brand-gcc");
        var box = new HBox(6, logo, l);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /** 用户菜单：关于 CGC（跳转仓库）+ 主题切换（深色/浅色文案联动） */
    private void showUserMenu(Button anchor) {
        if (userMenu == null) {
            MenuItem about = new MenuItem("关于 CGC");
            about.setOnAction(e ->
                    Browse.open("https://github.com/OrdinaryPerson404/skylink-gcs"));
            MenuItem theme = new MenuItem();
            theme.textProperty().bind(Bindings.createStringBinding(
                    () -> ThemeManager.isDark() ? "深色主题" : "浅色主题",
                    ThemeManager.darkProperty()));
            theme.setOnAction(e -> ThemeManager.toggleMain());
            userMenu = new ContextMenu(about, theme);
        }
        if (userMenu.isShowing()) {
            userMenu.hide();
        } else {
            userMenu.show(anchor, Side.BOTTOM, 0, 4);
        }
    }

    private void bindConnState() {
        Runnable apply = () -> {
            var s = AppState.get().getConnStatus();
            Color dot;
            String text;
            switch (s) {
                case CONNECTED -> {
                    dot = Color.web("#22c55e");
                    text = "已连接";
                }
                case CONNECTING -> {
                    dot = Color.web("#f59e0b");
                    text = "连接中";
                }
                case ERROR -> {
                    dot = Color.web("#ef4444");
                    text = "连接失败";
                }
                default -> {
                    dot = Color.web("#484f58");
                    text = "未连接";
                }
            }
            String detail = AppState.get().connDetailProperty().get();
            connLabel.setText(detail.isEmpty() ? text : text + " · " + detail);
            connDot.setFill(dot);
        };
        AppState.get().connStatusProperty().addListener((o, a, b) -> apply.run());
        AppState.get().connDetailProperty().addListener((o, a, b) -> apply.run());
        apply.run();
    }

    /** 高亮指定页签；无匹配 id 时全部取消高亮 */
    public void setActive(String pageId) {
        for (var e : tabIcons.entrySet()) {
            boolean active = tabIds.get(e.getKey()).equals(pageId);
            e.getKey().getStyleClass().remove("active");
            if (active) {
                e.getKey().getStyleClass().add("active");
            }
            e.getValue().forEach(n -> Icons.tint(n, active ? STROKE_ACTIVE : STROKE_IDLE));
            if (active) {
                activeTab = e.getKey();
            }
        }
    }

    public Button getActiveTab() {
        return activeTab;
    }
}
