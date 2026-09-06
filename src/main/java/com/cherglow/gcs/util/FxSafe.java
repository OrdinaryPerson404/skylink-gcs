package com.cherglow.gcs.util;

import javafx.application.Platform;

/**
 * FX 线程安全执行：无 FX 工具链（单测/工具进程）时退化为直接执行。
 */
public final class FxSafe {

    private FxSafe() {
    }

    public static void run(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            try {
                Platform.runLater(r);
            } catch (IllegalStateException notInitialized) {
                r.run(); // 无 FX 工具链环境
            }
        }
    }
}
