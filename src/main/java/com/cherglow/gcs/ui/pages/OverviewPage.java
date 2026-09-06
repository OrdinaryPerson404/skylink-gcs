package com.cherglow.gcs.ui.pages;

import com.cherglow.gcs.model.LiveVehicle;
import com.cherglow.gcs.ui.widget.AdiWidget;
import com.cherglow.gcs.ui.widget.Drone3DView;
import com.cherglow.gcs.ui.widget.JoystickWidget;
import com.cherglow.gcs.ui.widget.SensorCard;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * 仪表盘页（S6，对齐截图一布局）：
 * 单一 GridPane 三列（列边严格对齐）× 两行（上排 370 / 下排 235，底部留白）。
 * 上排：3D 模型 / ADI 姿态仪 / 传感器 2x3；下排：遥控输入 CH1-12 / 遥控器模拟 Mode 2 / 电机输出 M1-12。
 * 遥控器模拟杆位由电机实际输出按 X 布局混控反向推演（非假数据）。
 */
public class OverviewPage extends BorderPane {

    private static final double TOP_H = 370;
    private static final double BOTTOM_H = 235;

    private final LiveVehicle lv = LiveVehicle.get();

    // 3D 卡底部统计
    private final Label statRoll = statVal();
    private final Label statPitch = statVal();
    private final Label statYaw = statVal();
    // ADI 卡底部统计
    private final Label statAlt = statVal();
    private final Label statGs = statVal();
    private final Label statVs = statVal();

    // 传感器卡
    private final SensorCard imuCard = new SensorCard("◎", "IMU");
    private final SensorCard magCard = new SensorCard("✦", "罗盘");
    private final SensorCard baroCard = new SensorCard("≋", "气压计");
    private final SensorCard rangeCard = new SensorCard("⇓", "测距仪");
    private final SensorCard flowCard = new SensorCard("∘", "光流");
    private final SensorCard gpsCard = new SensorCard("⊕", "GPS");
    private final SensorCard battCard = new SensorCard("⚡", "电池");
    private final SensorCard sigCard = new SensorCard("≋", "信号");

    // 遥控器模拟（电机反推）
    private final JoystickWidget joyLeft = new JoystickWidget();
    private final JoystickWidget joyRight = new JoystickWidget();
    private final Label joyLeftVal = new Label("THR — · YAW —");
    private final Label joyRightVal = new Label("ROLL — · PITCH —");

    // 遥控输入条形（12 路，双极性）
    private final RcBar[] rcBars = new RcBar[12];
    // 电机条形（12 路，单极性，M1-4 有效）
    private final MotorBar[] motorBars = new MotorBar[12];

    public OverviewPage() {
        getStyleClass().add("page-root");
        setPadding(new javafx.geometry.Insets(12));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(33.34);
        grid.getColumnConstraints().addAll(col, col, col);
        RowConstraints rTop = new RowConstraints(TOP_H);
        RowConstraints rBottom = new RowConstraints(BOTTOM_H);
        grid.getRowConstraints().addAll(rTop, rBottom);

        grid.add(card("3D 模型", "实时姿态",
                statBar(statCell(statRoll, "横滚"), statCell(statPitch, "俯仰"), statCell(statYaw, "航向")),
                new Drone3DView()), 0, 0);
        grid.add(card("姿态仪", "ADI",
                statBar(statCell(statAlt, "相对高度"), statCell(statGs, "地速"), statCell(statVs, "爬升率")),
                new AdiWidget()), 1, 0);
        grid.add(card("传感器", "探测自 status", sensorGrid()), 2, 0);

        grid.add(card("遥控输入", "CH1-12", rcPanel()), 0, 1);
        grid.add(card("遥控器模拟", "Mode 2", joyPanel()), 1, 1);
        grid.add(card("电机输出", "M1-12", motorPanel()), 2, 1);

        setCenter(grid);
        bindLive();
    }

    // ================= 卡片骨架 =================

    private VBox card(String title, String sub, Region body) {
        return card(title, sub, null, body);
    }

    private VBox card(String title, String sub, Region bottomBar, Region body) {
        VBox card = new VBox(0);
        card.getStyleClass().add("card");

        HBox head = new HBox(8);
        head.getStyleClass().add("card-head");
        head.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title);
        t.getStyleClass().add("card-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label s = new Label(sub);
        s.getStyleClass().add("card-sub");
        head.getChildren().addAll(t, sp, s);

        card.getChildren().add(head);
        VBox.setVgrow(body, Priority.ALWAYS);
        card.getChildren().add(body);
        if (bottomBar != null) {
            card.getChildren().add(bottomBar);
        }
        return card;
    }

    private HBox statBar(HBox... cells) {
        HBox bar = new HBox();
        bar.getStyleClass().add("stat-bar");
        for (HBox c : cells) {
            HBox.setHgrow(c, Priority.ALWAYS);
            bar.getChildren().add(c);
        }
        return bar;
    }

    private HBox statCell(Label val, String name) {
        HBox cell = new HBox();
        cell.getStyleClass().add("stat-cell");
        cell.setAlignment(Pos.CENTER);
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        Label l = new Label(name);
        l.getStyleClass().add("stat-label");
        box.getChildren().addAll(val, l);
        cell.getChildren().add(box);
        return cell;
    }

    private static Label statVal() {
        Label l = new Label("—");
        l.getStyleClass().add("stat-val");
        return l;
    }

    // ================= 传感器（2x3 GridPane） =================

    private GridPane sensorGrid() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.setPadding(new javafx.geometry.Insets(10));
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(50);
        g.getColumnConstraints().addAll(c, c);
        RowConstraints r = new RowConstraints();
        r.setPercentHeight(25);
        g.getRowConstraints().addAll(r, r, r, r);

        SensorCard[][] cards = {
                {imuCard, magCard},
                {baroCard, rangeCard},
                {flowCard, gpsCard},
                {battCard, sigCard}};
        for (int row = 0; row < 4; row++) {
            for (int col2 = 0; col2 < 2; col2++) {
                SensorCard sc = cards[row][col2];
                GridPane.setHgrow(sc, Priority.ALWAYS);
                GridPane.setVgrow(sc, Priority.ALWAYS);
                g.add(sc, col2, row);
            }
        }
        return g;
    }

    // ================= 遥控器模拟（电机反推） =================

    private HBox joyPanel() {
        HBox box = new HBox(20);
        box.getStyleClass().add("joy-panel");
        box.setAlignment(Pos.TOP_CENTER);

        VBox left = new VBox(6, joyLeft, caption("左 · THR/YAW"), joyLeftVal);
        left.setAlignment(Pos.TOP_CENTER);
        VBox right = new VBox(6, joyRight, caption("右 · ROLL/PITCH"), joyRightVal);
        right.setAlignment(Pos.TOP_CENTER);
        for (VBox v : new VBox[]{left, right}) {
            HBox.setHgrow(v, Priority.ALWAYS);
            v.setMaxWidth(230);
        }
        joyLeftVal.getStyleClass().add("joy-values");
        joyRightVal.getStyleClass().add("joy-values");
        box.getChildren().addAll(left, right);
        return box;
    }

    private Label caption(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("joy-caption");
        return l;
    }

    // ================= 遥控输入 =================

    private HBox rcPanel() {
        HBox box = new HBox(6);
        box.getStyleClass().add("bar-panel");
        box.setAlignment(Pos.BOTTOM_CENTER);
        for (int i = 0; i < 12; i++) {
            rcBars[i] = new RcBar(String.valueOf(i + 1));
            HBox.setHgrow(rcBars[i], Priority.ALWAYS);
            box.getChildren().add(rcBars[i]);
        }
        return box;
    }

    // ================= 电机输出 =================

    private HBox motorPanel() {
        HBox box = new HBox(6);
        box.getStyleClass().add("bar-panel");
        box.setAlignment(Pos.BOTTOM_CENTER);
        for (int i = 0; i < 12; i++) {
            motorBars[i] = new MotorBar("M" + (i + 1), i < 4);
            HBox.setHgrow(motorBars[i], Priority.ALWAYS);
            box.getChildren().add(motorBars[i]);
        }
        return box;
    }

    // ================= 数据绑定 =================

    private void bindLive() {
        // 3D 统计
        lv.rollDeg.addListener((o, a, b) -> statRoll.setText(fmt(b.doubleValue(), 1) + "°"));
        lv.pitchDeg.addListener((o, a, b) -> statPitch.setText(fmt(b.doubleValue(), 1) + "°"));
        lv.yawDeg.addListener((o, a, b) -> statYaw.setText(fmt(b.doubleValue(), 1) + "°"));

        // ADI 统计
        lv.altitude.addListener((o, a, b) -> statAlt.setText(
                b.doubleValue() == b.doubleValue() ? fmt(b.doubleValue(), 1) + " m" : "—"));
        statAlt.setText("—");
        statGs.setText("—");
        statVs.setText("—");

        // IMU 卡
        Runnable imuUpd = () -> {
            boolean online = lv.accZ.get() == lv.accZ.get();
            imuCard.setOnline(online, online ? "在线" : "未检测到");
            if (online) {
                imuCard.val1().setText(String.format("acc %.2f %.2f %.2f",
                        lv.accX.get(), lv.accY.get(), lv.accZ.get()));
                imuCard.val2().setText(String.format("gyro %.3f %.3f %.3f",
                        lv.gyroX.get(), lv.gyroY.get(), lv.gyroZ.get()));
            }
        };
        lv.accX.addListener((o, a, b) -> imuUpd.run());
        lv.gyroX.addListener((o, a, b) -> imuUpd.run());
        lv.imuModel.addListener((o, a, b) -> imuUpd.run());
        imuUpd.run();

        // 传感器空态（探测位驱动，离线显示 未检测到）
        lv.magOk.addListener((o, a, b) -> magCard.setOnline(b, b ? "在线" : "未检测到"));
        magCard.setOnline(lv.magOk.get(), lv.magOk.get() ? "在线" : "未检测到");
        lv.baroOk.addListener((o, a, b) -> baroCard.setOnline(b, b ? "在线" : "未检测到"));
        baroCard.setOnline(lv.baroOk.get(), lv.baroOk.get() ? "在线" : "未检测到");
        lv.rangeOk.addListener((o, a, b) -> rangeCard.setOnline(b, b ? "在线" : "未检测到"));
        rangeCard.setOnline(lv.rangeOk.get(), lv.rangeOk.get() ? "在线" : "未检测到");
        flowCard.setOnline(false, "未配备");
        gpsCard.setOnline(false, "无 GPS 硬件");

        // 电池卡（电压 + 1S 锂电估算百分比）
        Runnable battUpd = () -> {
            boolean online = lv.batteryVoltage.get() == lv.batteryVoltage.get();
            battCard.setOnline(online, "HY 802540 · 3.7V");
            if (online) {
                battCard.val1().setText(String.format("电压 %.2f V / 3.7V", lv.batteryVoltage.get()));
                double pct = lv.batteryPct.get();
                battCard.val2().setText(pct == pct
                        ? String.format("600mAh · 2.22Wh · ~%.0f%%", pct)
                        : "600mAh · 2.22Wh");
            }
        };
        lv.batteryVoltage.addListener((o, a, b) -> battUpd.run());
        lv.batteryPct.addListener((o, a, b) -> battUpd.run());
        battUpd.run();

        // 信号卡（CRSF 链路质量 + 三条链路在线位）
        Runnable sigUpd = () -> {
            boolean any = lv.rcLinkUp.get() || lv.webLinkUp.get() || lv.mavLinkUp.get();
            sigCard.setOnline(any, any ? "在线" : "无链路");
            int lq = lv.linkQuality.get();
            sigCard.val1().setText(lq >= 0 ? String.format("上行质量 %d%%", lq) : "上行质量 —");
            sigCard.val2().setText(String.format("RC%s Web%s MAV%s",
                    lv.rcLinkUp.get() ? "✓" : "✗",
                    lv.webLinkUp.get() ? "✓" : "✗",
                    lv.mavLinkUp.get() ? "✓" : "✗"));
        };
        lv.linkQuality.addListener((o, a, b) -> sigUpd.run());
        lv.rcLinkUp.addListener((o, a, b) -> sigUpd.run());
        lv.webLinkUp.addListener((o, a, b) -> sigUpd.run());
        lv.mavLinkUp.addListener((o, a, b) -> sigUpd.run());
        sigUpd.run();

        // 遥控条形
        lv.rcChannels.addListener((o, a, b) -> {
            int[] ch = b;
            for (int i = 0; i < 12; i++) {
                if (ch != null && i < ch.length && ch[i] > 500) {
                    double pct = (ch[i] - 1500) / 500.0 * 100.0;
                    rcBars[i].update(clamp(pct, -100, 100), true);
                } else {
                    rcBars[i].update(0, false);
                }
            }
        });

        // 遥控器模拟：由电机实际输出按 X 布局混控反向推演
        lv.motors.addListener((o, a, m) -> {
            float[] mo = m;
            if (mo == null || mo.length != 4) {
                return;
            }
            // 数组序 FR, FL, RR, RL
            double thr = (mo[0] + mo[1] + mo[2] + mo[3]) / 4.0;
            double rollRaw = ((mo[1] + mo[3]) - (mo[0] + mo[2])) / 2.0;   // (FL+RL)-(FR+RR)
            double pitchRaw = ((mo[0] + mo[1]) - (mo[2] + mo[3])) / 2.0;  // (FR+FL)-(RR+RL)
            double yawRaw = ((mo[0] + mo[3]) - (mo[1] + mo[2])) / 2.0;    // (FR+RL)-(FL+RR)
            double S = 0.4; // 差分满量程（现场可校准）
            double rollPct = clamp(rollRaw / S, -1, 1) * 100;
            double pitchPct = clamp(-pitchRaw / S, -1, 1) * 100; // 杆前推=低头=前电机减速，取负
            double yawPct = clamp(yawRaw / S, -1, 1) * 100;
            joyLeft.update(clamp(yawPct / 100, -1, 1), clamp(thr * 2 - 1, -1, 1));
            joyRight.update(clamp(rollPct / 100, -1, 1), clamp(pitchPct / 100, -1, 1));
            joyLeftVal.setText(String.format("THR %3.0f%% · YAW %+.0f%%", thr * 100, yawPct));
            joyRightVal.setText(String.format("ROLL %+.0f%% · PITCH %+.0f%%", rollPct, pitchPct));

            // 电机条形（数组序 FR, FL, RR, RL → M1-M4）
            motorBars[0].update(mo[0] * 100);
            motorBars[1].update(mo[1] * 100);
            motorBars[2].update(mo[2] * 100);
            motorBars[3].update(mo[3] * 100);
        });
    }

    private static String fmt(double v, int digits) {
        return v == v ? String.format("%." + digits + "f", v) : "—";
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ================= 条形组件 =================

    /** 遥控通道：双极性条（中位为 0，±100%） */
    static final class RcBar extends VBox {
        private final Region fill = new Region();
        private final double trackH = 96;

        RcBar(String name) {
            setSpacing(3);
            setAlignment(Pos.BOTTOM_CENTER);
            StackPane track = new StackPane();
            track.getStyleClass().add("bar-track");
            track.setMinHeight(trackH);
            track.setMaxHeight(trackH);
            track.getChildren().add(fill);
            fill.getStyleClass().add("bar-fill-rc");
            Label label = new Label(name);
            label.getStyleClass().add("bar-label");
            getChildren().addAll(track, label);
            update(0, false);
        }

        void update(double pct, boolean valid) {
            double half = trackH / 2.0;
            if (!valid) {
                fill.setMaxHeight(0);
                if (!fill.getStyleClass().contains("dim")) {
                    fill.getStyleClass().add("dim");
                }
                return;
            }
            fill.getStyleClass().remove("dim");
            double h = Math.abs(pct) / 100.0 * half;
            fill.setMaxHeight(Math.max(2, h));
            // 中线起涨：正上负下
            StackPane.setAlignment(fill, Pos.CENTER);
            fill.setTranslateY(pct >= 0 ? -h / 2.0 : h / 2.0);
        }
    }

    /** 电机：单极性条（底部 0 → 顶部 100%） */
    static final class MotorBar extends VBox {
        private final Region fill = new Region();
        private final double trackH = 96;
        private final boolean active;

        MotorBar(String name, boolean active) {
            this.active = active;
            setSpacing(3);
            setAlignment(Pos.BOTTOM_CENTER);
            StackPane track = new StackPane();
            track.getStyleClass().add("bar-track");
            if (!active) {
                track.getStyleClass().add("disabled");
            }
            track.setMinHeight(trackH);
            track.setMaxHeight(trackH);
            fill.getStyleClass().add(active ? "bar-fill-mot" : "bar-fill-rc");
            track.getChildren().add(fill);
            Label label = new Label(name);
            label.getStyleClass().add("bar-label");
            if (!active) {
                label.getStyleClass().add("dim");
            }
            getChildren().addAll(track, label);
            StackPane.setAlignment(fill, Pos.BOTTOM_CENTER);
            update(0);
        }

        void update(double pct) {
            if (!active) {
                fill.setMaxHeight(0);
                return;
            }
            double h = clamp(pct, 0, 100) / 100.0 * trackH;
            fill.setMaxHeight(Math.max(2, h));
        }
    }
}
