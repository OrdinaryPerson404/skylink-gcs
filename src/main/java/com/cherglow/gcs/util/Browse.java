package com.cherglow.gcs.util;

import com.cherglow.gcs.ui.Toast;

import java.awt.Desktop;
import java.net.URI;

/** 系统浏览器打开链接；失败降级为 toast 展示 URL */
public final class Browse {

    private Browse() {
    }

    public static void open(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception ignored) {
            // 降级到 toast
        }
        Toast.show(url, Toast.Type.INFO);
    }
}
