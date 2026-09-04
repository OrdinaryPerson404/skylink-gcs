package cn.edu.nuaa.gcs.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

/**
 * 人工地平仪（Attitude Indicator / ADI）—— JavaFX Canvas 绘制。
 *
 * <p>典型航空仪表视觉：
 * <ul>
 *   <li>上半部分天空（深蓝→蓝渐变），下半部分地面（土黄→棕渐变）；</li>
 *   <li>白色地平线、俯仰刻度（每 10° 一长、5° 一短，±90°）、偏航刻度（顶部 0°/±90°/±180°）；</li>
 *   <li>黄色飞机固定符号（非随动），用于读取相对姿态；</li>
 *   <li>支持输入 roll / pitch / yaw（单位弧度或角度，通过 {@link #setInputRadians} /
 *       {@link #setInputDegrees} 控制）。</li>
 * </ul>
 *
 * <p>工程约束：
 * <ul>
 *   <li>继承 {@link Region} 而非 Canvas，自动随父布局缩放并重绘；</li>
 *   <li>内部 Canvas 尺寸与父区域绑定，避免 FXML 上死尺寸；</li>
 *   <li>所有数值属性 DoubleProperty，方便 UI 绑定；无效值（NaN / Infinite）不触发重绘。</li>
 * </ul>
 */
public class AttitudeIndicator extends Region {

    /** 滚转角（弧度，右滚为正）。 */
    private final DoubleProperty rollRad = new SimpleDoubleProperty(0);
    /** 俯仰角（弧度，抬头为正）。 */
    private final DoubleProperty pitchRad = new SimpleDoubleProperty(0);
    /** 偏航角（弧度，仅用于顶部方位指针显示）。 */
    private final DoubleProperty yawRad = new SimpleDoubleProperty(0);

    private final Canvas canvas = new Canvas();

    public AttitudeIndicator() {
        getChildren().add(canvas);
        // 尺寸随父布局变化
        widthProperty().addListener((obs, o, n) -> {
            canvas.setWidth(n.doubleValue());
            requestLayout();
        });
        heightProperty().addListener((obs, o, n) -> {
            canvas.setHeight(n.doubleValue());
            requestLayout();
        });
        javafx.beans.InvalidationListener redraw = obs -> draw();
        rollRad.addListener(redraw);
        pitchRad.addListener(redraw);
        yawRad.addListener(redraw);
        // 初始大小
        setPrefSize(280, 280);
        setMinSize(160, 160);
    }

    // ===== 属性访问 =====

    public DoubleProperty rollRadProperty() { return rollRad; }
    public DoubleProperty pitchRadProperty() { return pitchRad; }
    public DoubleProperty yawRadProperty() { return yawRad; }

    public double getRollRad() { return rollRad.get(); }
    public double getPitchRad() { return pitchRad.get(); }
    public double getYawRad() { return yawRad.get(); }

    /** 一次性设置输入（弧度）。返回 false 表示输入无效（含 NaN/Inf），未重绘。 */
    public boolean setInputRadians(double roll, double pitch, double yaw) {
        if (!Double.isFinite(roll) || !Double.isFinite(pitch) || !Double.isFinite(yaw)) return false;
        rollRad.set(roll);
        pitchRad.set(pitch);
        yawRad.set(yaw);
        return true;
    }

    /** 一次性设置输入（角度）。 */
    public boolean setInputDegrees(double rollDeg, double pitchDeg, double yawDeg) {
        return setInputRadians(
            Math.toRadians(rollDeg), Math.toRadians(pitchDeg), Math.toRadians(yawDeg));
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        canvas.setLayoutX(snappedLeftInset());
        canvas.setLayoutY(snappedTopInset());
        canvas.setWidth(Math.max(1, w - snappedLeftInset() - snappedRightInset()));
        canvas.setHeight(Math.max(1, h - snappedTopInset() - snappedBottomInset()));
        draw();
    }

    // ================================================================
    // 绘图
    // ================================================================

    private static final Color SKY_TOP = Color.rgb(20, 60, 140);
    private static final Color SKY_BOT = Color.rgb(60, 120, 210);
    private static final Color GND_TOP = Color.rgb(190, 160, 90);
    private static final Color GND_BOT = Color.rgb(110, 70, 35);

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w < 10 || h < 10) return;

        double cx = w / 2.0;
        double cy = h / 2.0;
        double r = Math.min(cx, cy) - 4; // 仪表圆半径

        // 清屏
        g.clearRect(0, 0, w, h);

        // ----- 外圈黑盘（圆盘裁剪背景，避免渐变溢出） -----
        g.save();
        g.beginPath();
        g.arc(cx, cy, r, r, 0, 360);
        g.closePath();
        g.clip();

        double roll = rollRad.get();
        double pitch = pitchRad.get();
        // 每度像素（约，视觉近似）
        double degPerPx = r / 90.0;      // 90° 占整个半径
        double pxPerDeg = 1.0 / degPerPx;

        // 平移画布：以 (cx, cy - pitch*radToDeg*pxPerDeg) 为中心后 roll
        Affine t = new Affine();
        t.appendTranslation(cx, cy);
        t.appendRotation(-Math.toDegrees(roll));
        // 俯仰偏移（抬头为正 → 地平线向下移 → 地面向上滚动 → 我们把绘制中心向下移 pitch>0 时画布上移）
        // 标准 ADI：机头上抬，背景滚下（天变多）。等价于：绘制天空/地面时，原点向上移 (pitchDeg*pxPerDeg)
        double pitchDeg = Math.toDegrees(pitch);
        t.appendTranslation(0, -pitchDeg * pxPerDeg);
        g.setTransform(t);

        // 天空
        g.save();
        javafx.scene.paint.LinearGradient skyGrad =
            new javafx.scene.paint.LinearGradient(0, -r, 0, 0, false,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, SKY_TOP),
                new javafx.scene.paint.Stop(1, SKY_BOT));
        g.setFill(skyGrad);
        g.fillRect(-w * 2, -h * 2, w * 4, h * 2); // 覆盖旋转后可能的上半
        // 地面
        javafx.scene.paint.LinearGradient gndGrad =
            new javafx.scene.paint.LinearGradient(0, 0, 0, r, false,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, GND_TOP),
                new javafx.scene.paint.Stop(1, GND_BOT));
        g.setFill(gndGrad);
        g.fillRect(-w * 2, 0, w * 4, h * 2);
        g.restore();

        // 地平线
        g.setStroke(Color.WHITE);
        g.setLineWidth(2);
        g.strokeLine(-r, 0, r, 0);

        // 俯仰刻度： ±90°，每 5° 短、10° 长、30° 数字
        g.setStroke(Color.WHITE);
        g.setFont(Font.font("SansSerif", 10));
        g.setTextAlign(TextAlignment.CENTER);
        for (int deg = -90; deg <= 90; deg += 5) {
            if (deg == 0) continue;
            double y = -deg * pxPerDeg;  // 正 pitch 对应刻度下移（在地面一侧）
            double half;
            if (deg % 30 == 0) half = r * 0.35;
            else if (deg % 10 == 0) half = r * 0.22;
            else half = r * 0.12;
            g.setLineWidth(deg % 10 == 0 ? 2 : 1);
            g.strokeLine(-half, y, half, y);
            if (deg % 30 == 0) {
                g.setFill(Color.WHITE);
                g.fillText(Integer.toString(deg), -half - 14, y + 4);
                g.fillText(Integer.toString(deg), half + 14, y + 4);
            }
        }
        g.restore();

        // ----- 顶部偏航刻度（不随 roll/pitch 变换，固定世界坐标） -----
        drawYawTape(g, cx, cy, r);

        // ----- 仪表边框（固定坐标，不受 roll 影响） -----
        g.save();
        g.setLineWidth(3);
        g.setStroke(Color.rgb(30, 30, 30));
        g.beginPath();
        g.arc(cx, cy, r, r, 0, 360);
        g.closePath();
        g.stroke();
        g.setLineWidth(1);
        g.setStroke(Color.rgb(140, 140, 140, 0.7));
        g.beginPath();
        g.arc(cx, cy, r - 3, r - 3, 0, 360);
        g.closePath();
        g.stroke();
        g.restore();

        // ----- 固定飞机符号（黄色翼型 + 中心点） -----
        drawFixedAircraftSymbol(g, cx, cy, r);

        // ----- 底部数值显示 -----
        drawReadout(g, cx, cy, r);
    }

    private void drawYawTape(GraphicsContext g, double cx, double cy, double r) {
        g.save();
        double topY = cy - r + 14;
        // 弧形切：顶部 30° 段内显示偏航
        g.beginPath();
        g.arc(cx, cy, r - 4, r - 4, -110, 40);
        g.arc(cx, cy, r - 22, r - 22, -70, -40);
        g.closePath();
        g.setFill(Color.rgb(20, 20, 20, 0.85));
        g.fill();
        g.setStroke(Color.rgb(255, 255, 255, 0.6));
        g.setLineWidth(1);
        g.stroke();

        double yawDeg = normalizeYaw(Math.toDegrees(yawRad.get()));
        // N/E/S/W 指针，角度每 30° 长刻度 + 每 10° 短刻度
        g.setStroke(Color.WHITE);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("SansSerif", 10));
        g.setTextAlign(TextAlignment.CENTER);
        for (int i = -3; i <= 3; i++) {
            int deg = (int) yawDeg + i * 10;
            double ang = yawDeg - deg;               // 当前顶部=0° → deg 值偏离多少
            double drawAng = -90 + ang;              // 顶部是 -90°，角度正值顺时针
            double rad = Math.toRadians(drawAng);
            double x1 = cx + (r - 6) * Math.cos(rad);
            double y1 = cy + (r - 6) * Math.sin(rad);
            double len = (deg % 30 == 0) ? 10 : 5;
            double x2 = cx + (r - 6 - len) * Math.cos(rad);
            double y2 = cy + (r - 6 - len) * Math.sin(rad);
            g.setLineWidth((deg % 30 == 0) ? 1.5 : 1);
            g.strokeLine(x1, y1, x2, y2);
            if (deg % 30 == 0) {
                String label = yawLabel(deg);
                double tx = cx + (r - 22) * Math.cos(rad);
                double ty = cy + (r - 22) * Math.sin(rad) + 4;
                g.fillText(label, tx, ty);
            }
        }
        // 顶部三角指针（固定指 N）
        double[] tx = {cx, cx - 6, cx + 6};
        double[] ty = {cy - r + 2, cy - r + 12, cy - r + 12};
        g.setFill(Color.YELLOW);
        g.beginPath();
        g.moveTo(tx[0], ty[0]);
        g.lineTo(tx[1], ty[1]);
        g.lineTo(tx[2], ty[1]);
        g.closePath();
        g.fill();
        g.restore();
    }

    private static String yawLabel(int deg) {
        int d = ((deg % 360) + 360) % 360;
        switch (d) {
            case 0:   return "N";
            case 90:  return "E";
            case 180: return "S";
            case 270: return "W";
            default:  return Integer.toString(d);
        }
    }

    private static double normalizeYaw(double d) {
        d = d % 360;
        if (d < 0) d += 360;
        return d;
    }

    private void drawFixedAircraftSymbol(GraphicsContext g, double cx, double cy, double r) {
        g.save();
        g.setStroke(Color.YELLOW);
        g.setFill(Color.YELLOW);
        g.setLineWidth(2.5);
        g.setLineCap(StrokeLineCap.ROUND);
        // 左机翼
        g.strokeLine(cx - r * 0.55, cy, cx - r * 0.18, cy);
        // 右机翼
        g.strokeLine(cx + r * 0.18, cy, cx + r * 0.55, cy);
        // 机翼末端小竖线
        g.strokeLine(cx - r * 0.55, cy - 4, cx - r * 0.55, cy + 4);
        g.strokeLine(cx + r * 0.55, cy - 4, cx + r * 0.55, cy + 4);
        // 机头小圆
        g.beginPath();
        g.arc(cx, cy, 3, 3, 0, 360);
        g.fill();
        // 垂尾（下）
        g.strokeLine(cx, cy, cx, cy + r * 0.18);
        // 上横梁下延伸线（左右翼尖上翘）
        g.strokeLine(cx - r * 0.18, cy, cx - r * 0.2, cy + 6);
        g.strokeLine(cx + r * 0.18, cy, cx + r * 0.2, cy + 6);
        g.restore();
    }

    private void drawReadout(GraphicsContext g, double cx, double cy, double r) {
        g.save();
        g.setFill(Color.rgb(15, 15, 15, 0.82));
        double bx = cx - r * 0.95;
        double by = cy + r * 0.55;
        double bw = r * 1.9;
        double bh = r * 0.28;
        roundRect(g, bx, by, bw, bh, 6);
        g.fill();
        g.setStroke(Color.rgb(255, 255, 255, 0.25));
        g.setLineWidth(1);
        roundRect(g, bx, by, bw, bh, 6);
        g.stroke();
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Consolas", 11));
        g.setTextAlign(TextAlignment.LEFT);
        String rollStr  = fmtDeg("R", Math.toDegrees(rollRad.get()));
        String pitchStr = fmtDeg("P", Math.toDegrees(pitchRad.get()));
        String yawStr   = fmtDeg("Y", Math.toDegrees(yawRad.get()));
        g.fillText(rollStr,  bx + 8, by + 15);
        g.fillText(pitchStr, bx + bw / 2 - 32, by + 15);
        g.fillText(yawStr,   bx + bw - 72, by + 15);
        g.restore();
    }

    private static String fmtDeg(String prefix, double deg) {
        if (!Double.isFinite(deg)) return prefix + ": —";
        return String.format("%s: %+6.1f°", prefix, deg);
    }

    private static void roundRect(GraphicsContext g, double x, double y, double w, double h, double r) {
        g.beginPath();
        g.moveTo(x + r, y);
        g.lineTo(x + w - r, y);
        g.arcTo(x + w, y, x + w, y + r, r);
        g.lineTo(x + w, y + h - r);
        g.arcTo(x + w, y + h, x + w - r, y + h, r);
        g.lineTo(x + r, y + h);
        g.arcTo(x, y + h, x, y + h - r, r);
        g.lineTo(x, y + r);
        g.arcTo(x, y, x + r, y, r);
        g.closePath();
    }
}
