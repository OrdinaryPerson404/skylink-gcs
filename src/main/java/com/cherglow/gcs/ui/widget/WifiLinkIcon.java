package com.cherglow.gcs.ui.widget;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.ArcType;
import javafx.scene.paint.Color;

/**
 * WiFi 扇形链路图标（Windows WiFi 信号样式）：三条弧 + 底部圆点。
 * 链路在线 → 主题金色；离线 → 暗灰（视觉上「信号不可用」）。
 */
public class WifiLinkIcon extends Canvas {

    private static final double W = 36, H = 26;

    public WifiLinkIcon() {
        super(W, H);
        draw(false);
    }

    public void setOn(boolean on) {
        draw(on);
    }

    private void draw(boolean on) {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, W, H);
        Color c = on ? Color.web("#f0a500") : Color.web("#39414b");
        double cx = W / 2.0, baseY = H - 4;
        // 底部圆点
        g.setFill(c);
        g.fillOval(cx - 2.5, baseY - 2.5, 5, 5);
        // 三层弧：半径 6/11/16，跨 -45°..45°（Canvas 角度：0=3 点钟，逆时针为正）
        double[] radii = {6, 11, 16};
        g.setStroke(c);
        g.setLineWidth(2.2);
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        for (double r : radii) {
            g.strokeArc(cx - r, baseY - r, r * 2, r * 2, -45, 90, ArcType.OPEN);
        }
    }
}
