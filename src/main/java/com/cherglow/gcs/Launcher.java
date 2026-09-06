package com.cherglow.gcs;

import javafx.application.Application;

/**
 * 普通 main 入口（供 IDE 运行与 S12 jpackage 打包使用）。
 * Maven 下用 mvn javafx:run（模块路径由插件处理）。
 */
public class Launcher {

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
