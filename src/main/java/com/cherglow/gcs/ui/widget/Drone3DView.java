package com.cherglow.gcs.ui.widget;

import com.cherglow.gcs.model.LiveVehicle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 3D 四旋翼（Canvas 软件三维投影 + 世界参考系）：
 * 机身长方块（沿机头拉长）+ 前挂绿色电池块（HY 802540，带黄色 LED）+ 四角机臂 +
 * 电机座 + 轴对齐十字桨（对角黄/蓝），姿态由 LiveVehicle 实时驱动。
 *
 * <p>参考系（口径对齐 CF-Drone 固件 quaternion.h / imu.ino / cli.ino）：
 * <ul>
 *   <li>机体系 FLU：X 机头、Y 左、Z 上（右手系）；世界系 = 上电时机体系；</li>
 *   <li>ps 姿态：R(世界←机体) = Rz(yaw)·Ry(pitch)·Rx(roll)，+roll=右倾、+pitch=低头、+yaw=左转（逆时针）；</li>
 *   <li>无磁力计 → yaw 为上电起陀螺积分的相对航向，罗盘环「N」代表上电时机头方向（非地理北）；</li>
 *   <li>相机固定（方位 AZ、仰角由 EL 决定），天空/地面/地平线/网格/罗盘环全部固定在世界上，
 *       无人机在参考系内转动 → 相对姿态一眼可读。</li>
 * </ul>
 * 视角固定不可拖拽；点击左上角「三轴」开关可显示机体系 X/Y/Z 轴箭头（IMU 调试用）。
 */
public class Drone3DView extends StackPane {

    private final Canvas canvas = new Canvas(320, 240);

    // 相机（转台）：方位角绕世界 Z；EL 为相对天底角，-73° = 相机在地平线上方约 17° 俯视
    private static final double AZ_DEG = 45;
    private static final double EL_DEG = -73;
    private static final double D = 4.0;    // 相机到原点距离
    private static final double AZ = Math.toRadians(AZ_DEG);
    private static final double EL = Math.toRadians(EL_DEG);

    private static final Color C_ARM = Color.web("#4a525c");
    private static final Color C_BODY = Color.web("#3d444d");
    private static final Color C_BODY_DARK = Color.web("#333a42");
    private static final Color C_POD = Color.web("#373d46");
    private static final Color C_BATT = Color.web("#3f8f6f");
    private static final Color C_YELLOW = Color.web("#f0a500");
    private static final Color C_BLUE = Color.web("#4b8bf5");
    private static final Color C_LED = Color.web("#ffd24a");
    private static final Color AX_X = Color.web("#ff6b6b");   // 机头
    private static final Color AX_Y = Color.web("#51cf66");   // 左
    private static final Color AX_Z = Color.web("#74c0fc");   // 上

    private Image bgLayer;                    // 静态天地层（按窗口尺寸缓存，逐像素光线投射）
    private double bgBuiltW = -1, bgBuiltH = -1;
    private boolean axesOn = false;
    private final Label axesChip = new Label("⊕ 三轴");
    private double lastRoll = Double.NaN, lastPitch = Double.NaN, lastYaw = Double.NaN;

    public Drone3DView() {
        getChildren().add(canvas);
        styleChip(false);
        axesChip.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        axesChip.setOnMouseClicked(e -> setAxesVisible(!axesOn));
        StackPane.setAlignment(axesChip, Pos.TOP_LEFT);
        StackPane.setMargin(axesChip, new Insets(6, 0, 0, 8));
        getChildren().add(axesChip);

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

    /** 三轴箭头开关（快照自检/调试用） */
    public void setAxesVisible(boolean on) {
        axesOn = on;
        styleChip(on);
        lastRoll = Double.NaN;    // 强制重绘（绕过姿态未变守卫）
        draw();
    }

    public boolean isAxesVisible() {
        return axesOn;
    }

    private void styleChip(boolean on) {
        axesChip.setText("⊕ 三轴 " + (on ? "开" : "关"));
        axesChip.setStyle(on
                ? "-fx-background-color: rgba(88,166,255,0.18); -fx-text-fill: #58a6ff; -fx-font-size: 11px;"
                + "-fx-padding: 2 8 2 8; -fx-background-radius: 10; -fx-cursor: hand;"
                : "-fx-background-color: rgba(13,17,23,0.72); -fx-text-fill: #8b949e; -fx-font-size: 11px;"
                + "-fx-padding: 2 8 2 8; -fx-background-radius: 10; -fx-cursor: hand;");
    }

    // ---- 三维变换 ----

    /** 机体系（FLU）→ 世界系：与固件 ps 同口径，先 roll（绕机头 X）→ pitch（绕左 Y）→ yaw（绕上 Z） */
    private double[] attitude(double[] p) {
        var lv = LiveVehicle.get();
        p = rx(p, Math.toRadians(lv.rollDeg.get()));
        p = ry(p, Math.toRadians(lv.pitchDeg.get()));
        p = rz(p, Math.toRadians(lv.yawDeg.get()));
        return p;
    }

    /** 世界系 → 视空间（转台相机：先方位后俯仰） */
    private double[] view(double[] p) {
        p = rz(p, AZ);
        p = rx(p, EL);
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

    private static double[] add(double[] a, double[] b) {
        return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] mul(double[] a, double k) {
        return new double[]{a[0] * k, a[1] * k, a[2] * k};
    }

    // ---- 投影 ----

    /** 投影器：apply=经姿态（机体元素），world=世界静态元素（背景类） */
    private final class Proj {
        final double cx, cy, S;

        Proj(double cx, double cy, double S) {
            this.cx = cx;
            this.cy = cy;
            this.S = S;
        }

        private double[] project(double[] q) {
            double s = D / (D - q[2]);
            return new double[]{cx + q[0] * s * S, cy - q[1] * s * S, q[2], s};
        }

        double[] apply(double[] p) {
            return project(view(attitude(p)));
        }

        double[] world(double[] p) {
            return project(view(p));
        }
    }

    // ---- 静态天地层（Phase 1：分色地平线） ----

    /** 逐像素光线投射天空/地面/地平线；相机固定 → 仅窗口尺寸变化时重建 */
    private void ensureBg(double w, double h) {
        int iw = (int) w, ih = (int) h;
        if (bgLayer != null && bgBuiltW == iw && bgBuiltH == ih) {
            return;
        }
        bgBuiltW = iw;
        bgBuiltH = ih;
        WritableImage img = new WritableImage(iw, ih);
        PixelWriter pw = img.getPixelWriter();
        double cx = iw / 2.0, cy = ih / 2.0 + ih * 0.04;
        double S = Math.min(iw, ih) * 0.33;
        double cA = Math.cos(AZ), sA = Math.sin(AZ), cE = Math.cos(EL), sE = Math.sin(EL);
        double camH = D * (-sE);              // 相机离地（原点水平面）高度
        // 天空：地平线浅蓝 → 天顶深蓝；地面：近处深棕 → 远处沙色（雾化）
        int skyHR = 0x9c, skyHG = 0xc0, skyHB = 0xe4, skyZR = 0x2a, skyZG = 0x5a, skyZB = 0x9e;
        int gndNR = 0x53, gndNG = 0x40, gndNB = 0x2d, gndFR = 0xb5, gndFG = 0x9a, gndFB = 0x72;
        int[] row = new int[iw];
        for (int j = 0; j < ih; j++) {
            double dyv = (cy - (j + 0.5)) / S;
            double y1 = dyv * cE - D * sE;    // view⁻¹ 第一步 rx(−EL)
            double z1 = -dyv * sE - D * cE;
            for (int i = 0; i < iw; i++) {
                double dxv = (i + 0.5 - cx) / S;
                double len = Math.sqrt(dxv * dxv + dyv * dyv + D * D);
                double zn = z1 / len;         // 世界 Z 分量（rz 不改 z）：>0 天空，<0 地面
                int r, g, b;
                if (zn >= 0) {
                    double t = Math.pow(Math.min(1, zn * 1.9), 0.8);
                    r = lerp8(skyHR, skyZR, t);
                    g = lerp8(skyHG, skyZG, t);
                    b = lerp8(skyHB, skyZB, t);
                } else {
                    double dist = camH * Math.sqrt(Math.max(0, 1 - zn * zn)) / -zn; // 平面交点水平距离
                    double t = Math.pow(Math.min(1, dist / 15.0), 0.7);
                    r = lerp8(gndNR, gndFR, t);
                    g = lerp8(gndNG, gndFG, t);
                    b = lerp8(gndNB, gndFB, t);
                }
                double aH = Math.max(0, 1 - Math.abs(zn) / 0.020);   // 白色地平线光带
                if (aH > 0) {
                    double k = Math.pow(aH, 1.6) * 0.9;
                    r = lerp8(r, 0xee, k);
                    g = lerp8(g, 0xf3, k);
                    b = lerp8(b, 0xf8, k);
                }
                row[i] = (255 << 24) | (r << 16) | (g << 8) | b;
            }
            pw.setPixels(0, j, iw, 1, PixelFormat.getIntArgbInstance(), row, 0, iw);
        }
        bgLayer = img;
    }

    private static int lerp8(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    // ---- 绘制 ----

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        if (w < 60 || h < 60) {
            return;
        }
        var lv = LiveVehicle.get();
        double roll = lv.rollDeg.get(), pitch = lv.pitchDeg.get(), yaw = lv.yawDeg.get();
        if (roll == lastRoll && pitch == lastPitch && yaw == lastYaw
                && bgLayer != null && bgBuiltW == (int) w && bgBuiltH == (int) h) {
            return;
        }
        lastRoll = roll;
        lastPitch = pitch;
        lastYaw = yaw;

        ensureBg(w, h);
        g.drawImage(bgLayer, 0, 0);

        double cx = w / 2.0, cy = h / 2.0 + h * 0.04;
        double S = Math.min(w, h) * 0.33;
        Proj proj = new Proj(cx, cy, S);

        drawGrid(g, proj);
        drawCompass(g, proj);

        class Face {
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

        List<Face> faces = new ArrayList<>();

        /** 轴对齐长方体：顶面 + 四侧面（机体 FLU 坐标） */
        class BoxGen {
            void add(double cx0, double cy0, double cz0, double hx, double hy, double hz, Color c) {
                double[][] v = {
                        {cx0 - hx, cy0 - hy, cz0 + hz}, {cx0 + hx, cy0 - hy, cz0 + hz},
                        {cx0 + hx, cy0 + hy, cz0 + hz}, {cx0 - hx, cy0 + hy, cz0 + hz}, // 顶（+Z 上）
                        {cx0 - hx, cy0 - hy, cz0 - hz}, {cx0 + hx, cy0 - hy, cz0 - hz},
                        {cx0 + hx, cy0 + hy, cz0 - hz}, {cx0 - hx, cy0 + hy, cz0 - hz}};
                int[][] f = {
                        {0, 1, 2, 3}, // 顶
                        {4, 5, 1, 0}, // 底
                        {5, 6, 2, 1}, // 右（−Y）
                        {6, 7, 3, 2}, // 后
                        {7, 4, 0, 3}};// 左（+Y）
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

        // ---- 几何（FLU：X 机头 / Y 左 / Z 上） ----
        // 机臂端点（X 布局：45°=右前, 135°=右后, 225°=左后, 315°=左前）
        double[][] motorC = new double[4][3];
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(45 + 90 * k);
            motorC[k] = new double[]{Math.cos(a), -Math.sin(a), 0.02};
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
        BoxGen box = new BoxGen();
        box.add(0, 0, 0.02, 0.30, 0.22, 0.22, C_BODY);        // 机身（沿机头拉长）
        box.add(0.30, 0, -0.06, 0.10, 0.17, 0.11, C_BATT);    // 前挂绿色电池（HY 802540）
        for (int k = 0; k < 4; k++) {
            box.add(motorC[k][0], motorC[k][1], 0.16, 0.13, 0.13, 0.09, C_POD);
        }

        // 3) 桨叶：每电机两片轴对齐十字桨（对角同色：FR/RL 黄，RR/FL 蓝）
        Color[] propColor = {C_YELLOW, C_BLUE, C_YELLOW, C_BLUE};
        double propZ = 0.30, hl = 0.50, hw = 0.055;
        double[][] dirs = {{1, 0, 0}, {0, 1, 0}};             // 机体 XY 平面内轴对齐
        for (int k = 0; k < 4; k++) {
            double[] rc = {motorC[k][0], motorC[k][1], propZ};
            for (double[] d : dirs) {
                double[] n = (d[0] == 1) ? new double[]{0, 1, 0} : new double[]{1, 0, 0};
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

        // 4) 三轴箭头（机体系，调试用）
        if (axesOn) {
            drawAxes(g, proj);
        }

        // 电池 LED（黄点，电池前表面中心）
        double[] led = proj.apply(new double[]{0.41, 0, -0.06});
        g.setFill(C_LED);
        g.fillOval(led[0] - 2.5, led[1] - 2.5, 5, 5);
    }

    /** 地面网格（世界固定 z=-0.8，按距离渐隐；分段绘制避免远处直线穿透地平线） */
    private void drawGrid(GraphicsContext g, Proj proj) {
        double gz = -0.8, EXT = 8;
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineWidth(1);
        for (int v = -(int) EXT; v <= EXT; v++) {
            for (int t = -(int) EXT; t < EXT; t += 2) {
                seg(g, proj, v, t, v, t + 2, gz);     // x=v 沿 Y
                seg(g, proj, t, v, t + 2, v, gz);     // y=v 沿 X
            }
        }
    }

    private void seg(GraphicsContext g, Proj proj, double x0, double y0, double x1, double y1, double z) {
        double[] p0 = proj.world(new double[]{x0, y0, z});
        double[] p1 = proj.world(new double[]{x1, y1, z});
        if (D - p0[2] < 0.6 || D - p1[2] < 0.6) {
            return;                                   // 近相机平面奇异区跳过
        }
        double mid = Math.hypot((x0 + x1) / 2, (y0 + y1) / 2);
        double a = 0.26 * Math.max(0, 1 - mid / 9.0) + 0.02;
        g.setStroke(Color.web("#e8dcc8", a));
        g.strokeLine(p0[0], p0[1], p1[0], p1[1]);
    }

    /** 罗盘刻度环（世界固定；N=上电航向，E=右转 90°方向，与固件 +yaw=左转相反侧） */
    private void drawCompass(GraphicsContext g, Proj proj) {
        double R = 1.45, Z = 0.82;
        g.setLineWidth(1.2);
        double[] prev = null;
        for (int i = 0; i <= 72; i++) {
            double b = Math.toRadians(i * 5);
            double[] p = proj.world(new double[]{Math.cos(b) * R, -Math.sin(b) * R, Z});
            if (prev != null) {
                g.setStroke(Color.web("#ffffff", 0.30));
                g.strokeLine(prev[0], prev[1], p[0], p[1]);
            }
            prev = p;
        }
        for (int bDeg = 0; bDeg < 360; bDeg += 30) {
            double b = Math.toRadians(bDeg);
            double[] p0 = proj.world(new double[]{Math.cos(b) * 1.35, -Math.sin(b) * 1.35, Z});
            double[] p1 = proj.world(new double[]{Math.cos(b) * R, -Math.sin(b) * R, Z});
            g.setStroke(Color.web("#ffffff", 0.42));
            g.strokeLine(p0[0], p0[1], p1[0], p1[1]);
        }
        String[] letters = {"N", "E", "S", "W"};
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        g.setLineWidth(3);
        for (int i = 0; i < 4; i++) {
            double b = Math.toRadians(i * 90);
            double[] p = proj.world(new double[]{Math.cos(b) * 1.58, -Math.sin(b) * 1.58, Z});
            g.setStroke(Color.web("#0d1117", 0.9));
            g.strokeText(letters[i], p[0] - 4.5, p[1] + 4);
            g.setFill(i == 0 ? C_YELLOW : Color.web("#9aa4ae"));
            g.fillText(letters[i], p[0] - 4.5, p[1] + 4);
        }
    }

    /** 机体系三轴箭头：红=X 机头，绿=Y 左，蓝=Z 上（随姿态转动，IMU 安装/欧拉角符号调试用） */
    private void drawAxes(GraphicsContext g, Proj proj) {
        double[][] u = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        double[][] e1 = {{0, 0, 1}, {1, 0, 0}, {1, 0, 0}};
        double[][] e2 = {{0, 1, 0}, {0, 0, 1}, {0, 1, 0}};
        Color[] cols = {AX_X, AX_Y, AX_Z};
        String[] names = {"X", "Y", "Z"};
        double[] o = proj.apply(new double[]{0, 0, 0});
        g.setLineCap(StrokeLineCap.ROUND);
        for (int i = 0; i < 3; i++) {
            double[] b3 = mul(u[i], 1.10), t3 = mul(u[i], 1.32);
            double[] B = proj.apply(b3), T = proj.apply(t3);
            g.setGlobalAlpha(0.92);
            g.setStroke(cols[i]);
            g.setLineWidth(Math.max(2.2, 3.5 * B[3]));
            g.strokeLine(o[0], o[1], B[0], B[1]);
            double r = 0.10;
            double[] h1 = proj.apply(add(b3, mul(e1[i], r)));
            double[] h2 = proj.apply(add(b3, mul(e1[i], -r)));
            double[] h3 = proj.apply(add(b3, mul(e2[i], r)));
            double[] h4 = proj.apply(add(b3, mul(e2[i], -r)));
            g.setFill(cols[i]);
            g.fillPolygon(new double[]{T[0], h1[0], h2[0]}, new double[]{T[1], h1[1], h2[1]}, 3);
            g.fillPolygon(new double[]{T[0], h3[0], h4[0]}, new double[]{T[1], h3[1], h4[1]}, 3);
            double[] lp = proj.apply(mul(u[i], 1.62));
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
            g.fillText(names[i], lp[0] - 3.5, lp[1] + 4);
            g.setGlobalAlpha(1.0);
        }
    }
}
