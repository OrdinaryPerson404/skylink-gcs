package cn.edu.nuaa.gcs.ui;

import javafx.scene.Scene;

public class ThemeManager {
    private static String currentTheme = "dark";

    public static void init(String theme) {
        currentTheme = theme != null ? theme : "dark";
    }

    public static void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        scene.getStylesheets().add(ThemeManager.class.getResource("/css/" + currentTheme + ".css").toExternalForm());
    }

    public static void toggle(Scene scene) {
        currentTheme = currentTheme.equals("dark") ? "light" : "dark";
        applyTheme(scene);
    }

    public static String getTheme() { return currentTheme; }
    public static boolean isDark() { return "dark".equals(currentTheme); }

    public static void setTheme(Scene scene, String theme) {
        currentTheme = theme;
        applyTheme(scene);
    }
}
