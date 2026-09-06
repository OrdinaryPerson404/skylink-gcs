package com.cherglow.gcs.ui.widget;

import com.cherglow.gcs.model.LiveVehicle;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 3D 四旋翼（Canvas 软件三维投影，按官方三视图建模）：
 * 机身长方块（沿机头方向拉长）+ 前挂绿色电池块（HY 802540，带黄色 LED）+
 * 四角机臂 + 电机座 + 轴对齐十字桨（对角黄/蓝），姿态由 LiveVehicle 横滚/俯仰/航向实时驱动。
 * 视角固定不可拖拽。坐标：Y 上、X 机头、Z 右；机臂半径=1 单位。
 */
public class Drone3DView extends StackPane {

    private final Canvas canvas = new Canvas(320, 240);

    // 固定视角（度）
    private static final double VIEW_X = -28;
    private static final double VIEW_Y = 30;

    private static final Color C_ARM = Color.web("#4a525c");
    private static final Color C_BODY = Color.web("#3d444d");
    private static final Color C_BODY_DARK = Color.web("#333a42");
    private static final Color C_POD = Color.web("#373d46");
    private static final Color C_BATT = Color.web("#3f8f6f");
    private static final Color C_YELLOW = Color.web("#f0a500");
    private static final Color C_BLUE = Color.web("#4b8bf5");
    private static final Color C_LED = Color.web("#ffd24a");

    public Drone3DView() {
        getChildren().add(canvas);
        setMinSize(240, 190);
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
        draw();
    }

    // ---- 三维变换 ----

    /** 姿态变换（口径：yaw(-Y)→pitch(-X)→roll(+Z)） */
    private double[] attitude(double[] p) {
        var lv = LiveVehicle.get();
        p = ry(p, Math.toRadians(-lv.yawDeg.get()));
        p = rx(p, Math.toRadians(-lv.pitchDeg.get()));
        p = rz(p, Math.toRadians(lv.rollDeg.get()));
        return p;
    }

    private double[] view(double[] p) {
        p = ry(p, Math.toRadians(VIEW_Y));
        p = rx(p, Math.toRadians(VIEW_X));
        return p;
    }

    private static double[] rx(double[] p, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new double[]{p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c};
    }

    private static double[] ry(double[] p, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new double[]{p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c};
    }

    private static double[] rz(double[] p, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new double[]{p[0] * c - p[1] * s, p[0] * s + p[1] * c, p[2]};
    }

    // ---- 面片 ----

    private static final class Face {
        final double[] xs, ys, zs;
        final Color color;

        Face(double[] xs, double[] ys, double[] zs, Color color) {
            this.xs = xs;
            this.ys = ys;
            this.zs = zs;
            this.color = color;
        }

        double depth() {
            double z = 0;
            for (double v : zs) {
                z += v;
            }
            return z / zs.length;
        }
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        if (w < 60 || h < 60) {
            return;
        }
        g.setFill(Color.web("#0d1117"));
        g.fillRect(0, 0, w, h);

        double cx = w / 2.0, cy = h / 2.0 + h * 0.04;
        double S = Math.min(w, h) * 0.33;
        double D = 4.0;

        class Proj {
            double[] apply(double[] p) {
                double[] q = view(attitude(p));
                double s = D / (D - q[2]);
                return new double[]{cx + q[0] * s * S, cy - q[1] * s * S, q[2], s};
            }
        }
        Proj proj = new Proj();

        List<Face> faces = new ArrayList<>();

        /** 轴对齐长方体：顶面 + 四侧面 */
        class BoxGen {
            void add(double cx0, double cy0, double cz0, double hx, double hy, double hz, Color c) {
                double[][] v = {
                        {cx0 - hx, cy0 + hy, cz0 - hz}, {cx0 + hx, cy0 + hy, cz0 - hz},
                        {cx0 + hx, cy0 + hy, cz0 + hz}, {cx0 - hx, cy0 + hy, cz0 + hz}, // 顶
                        {cx0 - hx, cy0 - hy, cz0 - hz}, {cx0 + hx, cy0 - hy, cz0 - hz},
                        {cx0 + hx, cy0 - hy, cz0 + hz}, {cx0 - hx, cy0 - hy, cz0 + hz}};
                int[][] f = {
                        {0, 1, 2, 3}, // 顶
                        {4, 5, 1, 0}, // 后
                        {5, 6, 2, 1}, // 右
                        {6, 7, 3, 2}, // 前
                        {7, 4, 0, 3}};// 左
                for (int[] quad : f) {
                    double[] xs = new double[4], ys = new double[4], zs = new double[4];
                    for (int i = 0; i < 4; i++) {
                        double[] pr = proj.apply(v[quad[i]]);
                        xs[i] = pr[0];
                        ys[i] = pr[1];
                        zs[i] = pr[2];
                    }
                    faces.add(new Face(xs, ys, zs, c));
                }
            }
        }
        BoxGen box = new BoxGen();

        // ---- 几何（三视图口径：机身沿机头拉长，绿色电池前挂） ----
        // 机臂端点（X 布局：45/135/225/315，机头=+X）
        double[][] motorC = new double[4][3];
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(45 + 90 * k);
            motorC[k] = new double[]{Math.cos(a), 0.02, Math.sin(a)};
        }

        // 1) 机臂（中心→电机座，先画，被机身/桨座遮盖连接处）
        g.setStroke(C_ARM);
        g.setLineCap(StrokeLineCap.ROUND);
        double[] bodyC0 = proj.apply(new double[]{0, 0, 0});
        for (int k = 0; k < 4; k++) {
            double[] mc = proj.apply(motorC[k]);
            g.setLineWidth(Math.max(4, 8 * mc[3]));
            g.strokeLine(bodyC0[0], bodyC0[1], mc[0], mc[1]);
        }

        // 2) 面片：机身 + 电池 + 四个电机座
        box.add(0, 0.02, 0, 0.30, 0.22, 0.22, C_BODY);          // 机身（沿机头拉长）
        box.add(0.30, -0.06, 0, 0.10, 0.11, 0.17, C_BATT);      // 前挂绿色电池（HY 802540）
        Color[] podFace = {C_POD, C_BODY_DARK, C_POD, C_BODY_DARK, C_POD};
        for (int k = 0; k < 4; k++) {
            box.add(motorC[k][0], 0.16, motorC[k][2], 0.13, 0.09, 0.13, C_POD);
        }

        // 3) 桨叶：每电机两片轴对齐十字桨（对角同色：0/2 黄，1/3 蓝）
        Color[] propColor = {C_YELLOW, C_BLUE, C_YELLOW, C_BLUE};
        double propY = 0.30, hl = 0.50, hw = 0.055;
        double[][] dirs = {{1, 0, 0}, {0, 0, 1}}; // 轴对齐
        for (int k = 0; k < 4; k++) {
            double[] rc = motorC[k];
            rc = new double[]{rc[0], propY, rc[2]};
            for (double[] d : dirs) {
                double[] n = (d[0] == 1) ? new double[]{0, 0, 1} : new double[]{1, 0, 0};
                double[][] c3 = {
                        add(add(rc, mul(d, hl)), mul(n, hw)),
                        add(add(rc, mul(d, hl)), mul(n, -hw)),
                        add(add(rc, mul(d, -hl)), mul(n, -hw)),
                        add(add(rc, mul(d, -hl)), mul(n, hw))};
                double[] xs = new double[4], ys = new double[4], zs = new double[4];
                for (int i = 0; i < 4; i++) {
                    double[] pr = proj.apply(c3[i]);
                    xs[i] = pr[0];
                    ys[i] = pr[1];
                    zs[i] = pr[2];
                }
                faces.add(new Face(xs, ys, zs, propColor[k]));
            }
        }

        // 深度排序（远→近）绘制
        faces.sort(Comparator.comparingDouble(Face::depth));
        for (Face f : faces) {
            g.setFill(f.color);
            g.fillPolygon(f.xs, f.ys, f.xs.length);
        }

        // 电池 LED（黄点，电池前表面中心）
        double[] led = proj.apply(new double[]{0.41, -0.06, 0});
        g.setFill(C_LED);
        g.fillOval(led[0] - 2.5, led[1] - 2.5, 5, 5);
    }

    private static double[] add(double[] a, double[] b) {
        return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] mul(double[] a, double k) {
        return new double[]{a[0] * k, a[1] * k, a[2] * k};
    }
}
