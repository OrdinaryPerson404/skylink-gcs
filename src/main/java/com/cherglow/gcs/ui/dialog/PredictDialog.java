package com.cherglow.gcs.ui.dialog;

import com.cherglow.gcs.core.BatteryPredictor;
import com.cherglow.gcs.ui.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 电池电压预测结果弹窗。
 * 展示: 电压下降曲线图 + 阈值标记 + 汇总信息 + 风力分析。
 */
public class PredictDialog {

    private static final double CHART_W = 600;
    private static final double CHART_H = 260;
    private static final double PAD_L = 44;
    private static final double PAD_R = 16;
    private static final double PAD_T = 12;
    private static final double PAD_B = 28;
    private static final double V_MIN = 3.0;
    private static final double V_MAX = 4.3;

    private final Stage stage = new Stage(StageStyle.TRANSPARENT);

    public PredictDialog(BatteryPredictor.PredictionResult result) {
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox card = new VBox(12);
        card.getStyleClass().add("dialog-card");
        card.setPrefWidth(660);

        // ---- 标题行 ----
        Label title = new Label("电池电压预测");
        title.getStyleClass().add("dialog-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("dialog-close");
        closeBtn.setOnAction(e -> stage.hide());
        HBox header = new HBox(8, title, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // ---- 电压曲线 Canvas ----
        Canvas chart = new Canvas(CHART_W, CHART_H);
        drawChart(chart.getGraphicsContext2D(), result);

        // ---- 汇总信息 ----
        VBox summary = buildSummary(result);

        // ---- 风力分析 ----
        VBox windAnalysis = buildWindAnalysis(result);

        // ---- 关闭按钮 ----
        Button doneBtn = new Button("关闭");
        doneBtn.getStyleClass().add("btn-primary");
        doneBtn.setPrefWidth(120);
        doneBtn.setOnAction(e -> stage.hide());
        HBox btnRow = new HBox(doneBtn);
        btnRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(header, chart, summary, windAnalysis, btnRow);

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root, 700, 600, Color.TRANSPARENT);
        ThemeManager.apply(scene);
        stage.setScene(scene);
    }

    // ===================== 汇总信息 =====================

    private VBox buildSummary(BatteryPredictor.PredictionResult r) {
        VBox box = new VBox(6);

        HBox row1 = new HBox(20);
        row1.setAlignment(Pos.CENTER_LEFT);
        row1.getChildren().addAll(
                statLabel("总距离", fmtDist(r.totalDistanceM)),
                statLabel("预计耗时", fmtTime(r.estimatedTimeSec)),
                statLabel("终点电压", String.format("%.2f V", r.finalVoltage)),
                statLabel("最低电压", String.format("%.2f V", r.minVoltage))
        );

        HBox row2 = new HBox(10);
        row2.setAlignment(Pos.CENTER_LEFT);
        Label verdictLabel = new Label();
        String verdict;
        String verdictStyle;
        if (!r.feasible) {
            verdict = "✗ 任务不可执行 — 预测电压低于 " + BatteryPredictor.LOCK_VOLTAGE + "V";
            verdictStyle = "-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px;";
        } else if (r.minVoltage < BatteryPredictor.WARN_VOLTAGE) {
            verdict = "⚠ 接近极限 — 建议减载或缩短航程";
            verdictStyle = "-fx-text-fill: #f0a500; -fx-font-weight: bold; -fx-font-size: 14px;";
        } else {
            verdict = "✓ 任务可执行";
            verdictStyle = "-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 14px;";
        }
        verdictLabel.setText(verdict);
        verdictLabel.setStyle(verdictStyle);
        row2.getChildren().add(verdictLabel);

        box.getChildren().addAll(row1, row2);
        return box;
    }

    // ===================== 风力分析 =====================

    private VBox buildWindAnalysis(BatteryPredictor.PredictionResult r) {
        VBox box = new VBox(4);
        Label header = new Label("风力分析");
        header.getStyleClass().add("field-label");
        box.getChildren().add(header);

        for (BatteryPredictor.SegmentWindInfo seg : r.windAnalysis) {
            String arrow = seg.windType.equals("顺风") ? "↓" : seg.windType.equals("逆风") ? "↑" : "→";
            String color = seg.windType.equals("顺风") ? "#22c55e" : seg.windType.equals("逆风") ? "#ef4444" : "#8b949e";
            Label segLabel = new Label(String.format(
                    "  WP%d→WP%d: %s %s  有效风力 %.1f m/s  (方位 %.0f°, 距离 %s)",
                    seg.fromId, seg.toId, arrow, seg.windType, seg.effectiveWindMps,
                    seg.bearingDeg, fmtDist(seg.distanceM)));
            segLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
            box.getChildren().add(segLabel);
        }
        return box;
    }

    // ===================== Canvas 电压曲线绘制 =====================

    private void drawChart(GraphicsContext g, BatteryPredictor.PredictionResult r) {
        double w = CHART_W;
        double h = CHART_H;
        g.clearRect(0, 0, w, h);

        double chartW = w - PAD_L - PAD_R;
        double chartH = h - PAD_T - PAD_B;

        // 背景
        g.setFill(Color.web("#0d1117"));
        g.fillRect(PAD_L, PAD_T, chartW, chartH);

        // Y 轴网格 + 刻度
        g.setFont(Font.font("Consolas", 10));
        for (int i = 0; i <= 13; i++) {
            double v = V_MIN + (V_MAX - V_MIN) * i / 13.0;
            double y = PAD_T + chartH - chartH * (v - V_MIN) / (V_MAX - V_MIN);
            g.setStroke(Color.web("#21262d"));
            g.setLineWidth(0.5);
            g.strokeLine(PAD_L, y, PAD_L + chartW, y);
            g.setFill(Color.web("#484f58"));
            g.fillText(String.format("%.1f", v), 4, y + 3);
        }

        // X 轴刻度
        double maxDist = Math.max(r.totalDistanceM, 1);
        int xTicks = 5;
        for (int i = 0; i <= xTicks; i++) {
            double d = maxDist * i / xTicks;
            double x = PAD_L + chartW * i / xTicks;
            g.setStroke(Color.web("#161b22"));
            g.setLineWidth(0.5);
            g.strokeLine(x, PAD_T, x, PAD_T + chartH);
            g.setFill(Color.web("#484f58"));
            g.fillText(fmtDist(d), x - 20, h - 8);
        }

        // 阈值线: 3.31V 红色虚线
        drawThresholdLine(g, BatteryPredictor.LOCK_VOLTAGE, chartW, chartH, "#ef4444", "3.31V 不可执行");
        // 阈值线: 3.5V 黄色虚线
        drawThresholdLine(g, BatteryPredictor.WARN_VOLTAGE, chartW, chartH, "#f0a500", "3.5V 预警");
        // 起飞电压参考线
        drawThresholdLine(g, BatteryPredictor.START_VOLTAGE, chartW, chartH, "#22c55e", null);

        // 电压曲线
        if (r.curve.size() >= 2) {
            g.setStroke(Color.web("#3b82f6"));
            g.setLineWidth(2);
            g.beginPath();
            boolean first = true;
            for (BatteryPredictor.VoltagePoint pt : r.curve) {
                double x = PAD_L + chartW * pt.distanceM / maxDist;
                double y = PAD_T + chartH - chartH * (clampV(pt.voltage) - V_MIN) / (V_MAX - V_MIN);
                if (first) {
                    g.moveTo(x, y);
                    first = false;
                } else {
                    g.lineTo(x, y);
                }
            }
            g.stroke();

            // 低于锁定阈值的段用红色高亮
            g.setStroke(Color.web("#ef4444"));
            g.setLineWidth(2.5);
            boolean inDanger = false;
            double prevX = 0, prevY = 0;
            for (int i = 0; i < r.curve.size(); i++) {
                BatteryPredictor.VoltagePoint pt = r.curve.get(i);
                double x = PAD_L + chartW * pt.distanceM / maxDist;
                double y = PAD_T + chartH - chartH * (clampV(pt.voltage) - V_MIN) / (V_MAX - V_MIN);
                if (pt.voltage < BatteryPredictor.LOCK_VOLTAGE) {
                    if (inDanger && i > 0) {
                        g.strokeLine(prevX, prevY, x, y);
                    } else {
                        g.beginPath();
                        g.moveTo(x, y);
                    }
                    inDanger = true;
                } else {
                    inDanger = false;
                }
                prevX = x;
                prevY = y;
            }

            // 航点标记
            for (BatteryPredictor.VoltagePoint pt : r.curve) {
                double x = PAD_L + chartW * pt.distanceM / maxDist;
                double y = PAD_T + chartH - chartH * (clampV(pt.voltage) - V_MIN) / (V_MAX - V_MIN);

                g.setFill(pt.voltage < BatteryPredictor.LOCK_VOLTAGE ? Color.web("#ef4444")
                        : pt.voltage < BatteryPredictor.WARN_VOLTAGE ? Color.web("#f0a500")
                        : Color.web("#3b82f6"));
                g.fillOval(x - 4, y - 4, 8, 8);
                g.setStroke(Color.WHITE);
                g.setLineWidth(1);
                g.strokeOval(x - 4, y - 4, 8, 8);

                // 编号标签
                g.setFill(Color.web("#c9d1d9"));
                g.setFont(Font.font("Consolas", 9));
                g.fillText("WP" + pt.waypointId, x + 6, y - 6);
            }
        }

        // 轴标签
        g.setFill(Color.web("#8b949e"));
        g.setFont(Font.font("Consolas", 10));
        g.fillText("V", 8, PAD_T + 4);
        g.fillText("距离", (int) (w / 2), h - 2);
    }

    private void drawThresholdLine(GraphicsContext g, double volt, double chartW, double chartH,
                                   String color, String label) {
        double y = PAD_T + chartH - chartH * (clampV(volt) - V_MIN) / (V_MAX - V_MIN);
        g.setStroke(Color.web(color));
        g.setLineWidth(1);
        g.setLineDashes(6, 4);
        g.strokeLine(PAD_L, y, PAD_L + chartW, y);
        g.setLineDashes();
        if (label != null) {
            g.setFill(Color.web(color));
            g.setFont(Font.font("Consolas", 9));
            g.fillText(label, PAD_L + chartW - 80, y - 3);
        }
    }

    // ===================== 辅助方法 =====================

    private static double clampV(double v) {
        return Math.max(V_MIN, Math.min(V_MAX, v));
    }

    private static String fmtDist(double m) {
        if (m >= 1000) return String.format("%.1fkm", m / 1000);
        return String.format("%.0fm", m);
    }

    private static String fmtTime(double sec) {
        if (sec >= 60) return String.format("%.1fmin", sec / 60);
        return String.format("%.0fs", sec);
    }

    private static HBox statLabel(String name, String value) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        Label n = new Label(name + ":");
        n.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 13px; -fx-font-weight: bold;");
        row.getChildren().addAll(n, v);
        return row;
    }

    public void show() {
        if (!stage.isShowing()) {
            stage.show();
            stage.centerOnScreen();
        }
    }
}
