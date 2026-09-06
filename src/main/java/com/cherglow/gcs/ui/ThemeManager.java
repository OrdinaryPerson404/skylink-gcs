package com.cherglow.gcs.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;

/**
 * 主题管理：深色（默认）/ 浅色。
 * app.css 使用 looked-up colors（-c-*），theme-light.css 仅覆盖颜色定义即可整体换肤。
 */
public final class ThemeManager {

    private static final BooleanProperty dark = new SimpleBooleanProperty(ThemeManager.class, "dark", true);
    private static Scene mainScene;

    private ThemeManager() {
    }

    /** App 启动时登记主场景，供快捷切换 */
    public static void setMainScene(Scene scene) {
        mainScene = scene;
    }

    public static boolean isDark() {
        return dark.get();
    }

    public static BooleanProperty darkProperty() {
        return dark;
    }

    /** 按当前主题为指定场景装配样式表（对话框等次级场景也调用此方法） */
    public static void apply(Scene scene) {
        String base = url("app.css");
        if (dark.get()) {
            scene.getStylesheets().setAll(base);
        } else {
            scene.getStylesheets().setAll(base, url("theme-light.css"));
        }
    }

    /** 切换主窗口主题（菜单项调用） */
    public static void toggleMain() {
        dark.set(!dark.get());
        if (mainScene != null) {
            apply(mainScene);
        }
    }

    private static String url(String name) {
        var res = ThemeManager.class.getResource("/com/cherglow/gcs/" + name);
        return res == null ? null : res.toExternalForm();
    }
}
