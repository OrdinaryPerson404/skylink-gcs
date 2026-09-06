package com.cherglow.gcs.ui.widget;

import com.cherglow.gcs.model.LiveVehicle;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Insets;

/**
 * ADI 姿态仪（Canvas 自绘，对齐截图一）：
 * 圆形天地仪（滚转旋转 + 俯仰平移）、俯仰梯 ±10/±20、
 * 顶部滚转刻度弧 + 黄色指针 + 左侧/顶部 ±0.0° 数字框、
 * 底部航向带（刻度 340/350/000/10/20 口径 + 中央黄色航向框）。
 * 数据源 LiveVehicle（欧拉角，CLI ps 命令口径）。
 */
public class AdiWidget extends StackPane {

    private static final String SKY = "#3b82f6";
    private static final String GROUND = "#8b5a2b";
    private static final String LADDER = "rgba(255,255,255,0.92)";
    private static final Color BOX_BG = Color.web("#10151c");
    private static final Color BOX_BORDER = Color.web("#30363d");
    private static final Color YELLOW = Color.web("#f0a500");

    private final Canvas canvas = new Canvas(360, 300);

    public AdiWidget() {
        getChildren().add(canvas);
        setMinSize(220, 200);
        setPadding(Insets.EMPTY);
        widthProperty().addListener((o, a, b) -> {
            canvas.setWidth(b.doubleValue());
            draw();
        });
        heightProperty().addListener((o, a, b) -> {
            canvas.setHeight(b.doubleValue());
            draw();
        });
        var lv = LiveVehicle.get();
        lv.rollDeg.addListener((o, a, b) -> draw());
        lv.pitchDeg.addListener((o, a, b) -> draw());
        lv.yawDeg.addListener((o, a, b) -> draw());
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        if (w < 60 || h < 60) {
            return;
        }
        g.clearRect(0, 0, w, h);

        double tapeH = 42;                       // 底部航向带高度
        double cx = w / 2.0, cy = (h - tapeH) / 2.0;
        double r = Math.min(w / 2.0 - 6, (h - tapeH) / 2.0 - 2);
        double ppd = r / 25.0;                   // 俯仰像素/度

        double roll = LiveVehicle.get().rollDeg.get();
        double pitch = LiveVehicle.get().pitchDeg.get();
        double yaw = LiveVehicle.get().yawDeg.get();

        // ---- 天地仪（圆形裁剪，随滚转/俯仰变动的部分） ----
        g.save();
        g.beginPath();
        g.arc(cx, cy, r, r, 0, 360);
        g.closePath();
        g.clip();

        g.translate(cx, cy);
        g.rotate(-roll);
        // 固件口径 +pitch=低头（CF-Drone quaternion.h ZYX/FLU，2026-09-06 真机核实）：低头看到更多地面 → 地平线向上移
        g.translate(0, -pitch * ppd);

        double span = r * 2.6;
        g.setFill(Color.web(SKY));
        g.fillRect(-span, -span, span * 2, span);
        g.setFill(Color.web(GROUND));
        g.fillRect(-span, 0, span * 2, span);
        g.setStroke(Color.WHITE);
        g.setLineWidth(2);
        g.strokeLine(-span, 0, span, 0);

        g.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        g.setLineWidth(1.4);
        g.setStroke(Color.web(LADDER));
        g.setFill(Color.web(LADDER));
        for (int p : new int[]{-20, -10, 10, 20}) {
            double y = (-pitch - p) * ppd;   // 俯仰梯：p 为航空口径刻度值，随 −pitch 平移
            if (Math.abs(y) > r) {
                continue;
            }
            double half = Math.abs(p) == 20 ? 34 : 22;
            g.strokeLine(-half, y, half, y);
            int v = -p;   // 梯度标签统一固件口径（+pitch=低头），与左侧数字框/ps 输出一致
            String t = (v > 0 ? "+" : "") + v;
            g.fillText(t, half + 5, y + 3.5);
            g.fillText(t, -half - 19, y + 3.5);
        }
        g.restore();

        // ---- 中央飞机符号（黄） ----
        g.setStroke(YELLOW);
        g.setLineWidth(2.4);
        double s = r * 0.34;
        g.strokeLine(cx - s, cy, cx - s * 0.38, cy);
        g.strokeLine(cx + s * 0.38, cy, cx + s, cy);
        g.strokeLine(cx - s * 0.38, cy, cx - s * 0.30, cy + 5);
        g.strokeLine(cx + s * 0.38, cy, cx + s * 0.30, cy + 5);
        g.setFill(YELLOW);
        g.fillOval(cx - 2, cy - 2, 4, 4);

        // ---- 顶部滚转刻度弧（-20/-10/0/+10/+20 刻度与数字） ----
        double R2 = r - 10;
        g.setStroke(Color.web(LADDER));
        g.setLineWidth(1.4);
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        g.setFill(Color.web(LADDER));
        for (int a : new int[]{-20, -10, 0, 10, 20}) {
            double rad = Math.toRadians(a);
            double sinA = Math.sin(rad), cosA = Math.cos(rad);
            double x1 = cx + sinA * R2, y1 = cy - cosA * R2;
            double x2 = cx + sinA * (R2 - 8), y2 = cy - cosA * (R2 - 8);
            g.strokeLine(x1, y1, x2, y2);
            if (a != 0) {
                String t = String.valueOf(Math.abs(a));
                double tx = cx + sinA * (R2 - 20), ty2 = cy - cosA * (R2 - 20);
                g.fillText(t, tx - 5, ty2 + 3.5);
            }
        }
        // 滚转指针（黄三角，沿弧移动）
        double rr = Math.toRadians(roll);
        double tipX = cx + Math.sin(rr) * (R2 + 6);
        double tipY = cy - Math.cos(rr) * (R2 + 6);
        double baseX = cx + Math.sin(rr) * (R2 - 4);
        double baseY = cy - Math.cos(rr) * (R2 - 4);
        double px = -Math.cos(rr), py = Math.sin(rr); // 切向单位向量
        g.setFill(YELLOW);
        g.fillPolygon(
                new double[]{tipX, baseX + px * 5, baseX - px * 5},
                new double[]{tipY, baseY + py * 5, baseY - py * 5}, 3);

        // ---- 数字框：顶部滚转值 + 左侧俯仰值（图一补齐项） ----
        drawBox(g, cx - 28, cy - r + 6, 56, 19, String.format("%+.1f°", roll));
        drawBox(g, cx - r + 4, cy - 9.5, 56, 19, String.format("%+.1f°", pitch));

        // ---- 外圈 ----
        g.setStroke(BOX_BORDER);
        g.setLineWidth(2);
        g.strokeOval(cx - r, cy - r, r * 2, r * 2);

        // ---- 底部航向带 ----
        double ty = h - tapeH + 6;
        g.setFill(Color.web("#0d1117"));
        g.fillRect(cx - r, ty - 4, r * 2, tapeH - 2);
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        double pxPerDegH = r / 38.0;
        for (int d = -45; d <= 45; d += 5) {
            if (Math.abs(d) < 8) {
                continue; // 中央航向框区域留白
            }
            int hdg = norm((int) Math.round(yaw) + d);
            double x = cx + d * pxPerDegH;
            if (x < cx - r + 4 || x > cx + r - 4) {
                continue;
            }
            boolean major = hdg % 10 == 0;
            g.setStroke(Color.web(major ? "#c9d1d9" : "#484f58"));
            g.setLineWidth(1.2);
            g.strokeLine(x, ty + (major ? 8 : 13), x, ty + 18);
            if (major) {
                g.setFill(Color.web("#8b949e"));
                String t = hdg == 0 ? "000" : String.valueOf(hdg);
                g.fillText(t, x - t.length() * 3.2, ty + 31);
            }
        }
        // 中央黄色航向框（含 ° 后缀，图一口径）
        String hdgText = norm((int) Math.round(yaw)) + "°";
        double bw = 46, bh = 19;
        g.setFill(YELLOW);
        g.fillRect(cx - bw / 2, ty + 2, bw, bh);
        g.setFill(Color.web("#161b22"));
        g.fillText(hdgText, cx - hdgText.length() * 3.4, ty + 16);
        // 黄色小三角指向框
        g.setFill(YELLOW);
        g.fillPolygon(
                new double[]{cx, cx - 5, cx + 5},
                new double[]{ty - 2, ty - 9, ty - 9}, 3);
    }

    /** 黑底描边数字框（左侧俯仰 / 顶部滚转 / 航向中央统一口径） */
    private void drawBox(GraphicsContext g, double x, double y, double bw, double bh, String text) {
        g.setFill(BOX_BG);
        g.fillRect(x, y, bw, bh);
        g.setStroke(BOX_BORDER);
        g.setLineWidth(1);
        g.strokeRect(x, y, bw, bh);
        g.setFill(Color.web("#e6edf3"));
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        g.fillText(text, x + bw / 2 - text.length() * 3.4, y + bh / 2 + 4);
    }

    private static int norm(int deg) {
        return ((deg % 360) + 360) % 360;
    }
}
