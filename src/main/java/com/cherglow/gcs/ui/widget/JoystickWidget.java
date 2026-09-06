package com.cherglow.gcs.ui.widget;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * 遥控器模拟摇杆（Mode 2）：圆形刻度盘 + 十字线 + 发光杆位圆点。
 * 杆位并非假数据：由无人机电机实际输出按 X 布局混控反向推演（油门/横滚/俯仰/偏航）。
 * nx/ny ∈ [-1,1]，屏幕坐标 ny 向上为正。
 */
public class JoystickWidget extends StackPane {

    private final Canvas canvas = new Canvas(170, 132);
    private double nx, ny;

    public JoystickWidget() {
        getChildren().add(canvas);
        draw();
    }

    public void update(double nx, double ny) {
        this.nx = Math.max(-1, Math.min(1, nx));
        this.ny = Math.max(-1, Math.min(1, ny));
        draw();
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        g.clearRect(0, 0, w, h);

        double cx = w / 2.0, cy = h / 2.0, r = Math.min(w, h) / 2.0 - 8;

        // 表盘底 + 边框
        g.setFill(Color.web("#0d1117"));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setStroke(Color.web("#30363d"));
        g.setLineWidth(1.4);
        g.strokeOval(cx - r, cy - r, r * 2, r * 2);

        // 十字线
        g.setStroke(Color.web("#21262d"));
        g.setLineWidth(1);
        g.strokeLine(cx - r + 4, cy, cx + r - 4, cy);
        g.strokeLine(cx, cy - r + 4, cx, cy + r - 4);

        // 杆位（发光点）
        double dx = cx + nx * (r - 10);
        double dy = cy - ny * (r - 10);
        g.setFill(Color.web("rgba(240,165,0,0.22)"));
        g.fillOval(dx - 13, dy - 13, 26, 26);
        g.setFill(Color.web("#f0a500"));
        g.fillOval(dx - 5.5, dy - 5.5, 11, 11);
    }
}
