package com.cherglow.gcs.ui.pages;

import com.cherglow.gcs.core.AppState;
import com.cherglow.gcs.core.ConnectionService;
import com.cherglow.gcs.model.LiveVehicle;
import com.cherglow.gcs.ui.ConfirmDialog;
import com.cherglow.gcs.ui.Toast;
import com.cherglow.gcs.ui.setup.ParamDict;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 调参页（S9，对齐目标设计）：
 * 左侧 设置/传感器/参数/固件 分组侧栏；右侧各分组面板：
 * 机架类型（卡片墙 + 当前参数条 + CW/CCW 说明）、电机设置（M1-M12 实时输出条 + 参数表）、
 * 遥控器（CH1-8 通道卡 + 等待信号/RSSI + 参数表）、端口设置（SERIAL1-6 表格 + WiFi/MAV 参数）、
 * PID 调参（四 tab + 已加载计数 + 范围列 + 步进器 + 批量读取）、安全设置、传感器校准与空态、
 * 参数列表（三态：未连接/待拉取/已加载表格 + 搜索 + 导出/导入 JSON）、固件升级（占位）。
 */
public class SetupPage extends BasePage {

    @Override
    public String pageId() {
        return "setup";
    }

    private record SectionItem(String id, String group, String title) {
    }

    private static final List<SectionItem> ITEMS = List.of(
            new SectionItem("frame", "设置", "机架类型"),
            new SectionItem("motors", "设置", "电机设置"),
            new SectionItem("rc", "设置", "遥控器"),
            new SectionItem("ports", "设置", "端口设置"),
            new SectionItem("pid", "设置", "PID 调参"),
            new SectionItem("safety", "设置", "安全设置"),
            new SectionItem("imu", "传感器", "IMU 校准"),
            new SectionItem("compass", "传感器", "罗盘校准"),
            new SectionItem("gps", "传感器", "GPS"),
            new SectionItem("baro", "传感器", "气压计"),
            new SectionItem("flow", "传感器", "光流传感器"),
            new SectionItem("range", "传感器", "激光测距仪"),
            new SectionItem("paramlist", "参数", "参数列表"),
            new SectionItem("firmware", "固件", "固件升级"));

    private final LiveVehicle lv = LiveVehicle.get();
    private final ConnectionService conn = ConnectionService.get();

    private final VBox sidebar = new VBox(2);
    private final StackPane content = new StackPane();
    private final Map<String, Region> sectionCache = new LinkedHashMap<>();
    private final Map<String, Label> sideLabels = new LinkedHashMap<>();
    private final List<ParamRow> allRows = new ArrayList<>();

    // PID 计数
    private final List<String> pidAll = new ArrayList<>();
    private Label pidCountLabel;

    // 电机输出
    private final List<MotorBar> motorBars = new ArrayList<>();
    private Label motorWarn;

    // 参数列表
    private final List<String> paramOrder = new ArrayList<>();
    private VBox paramListBody;
    private TextField paramSearch;
    private Label paramCounter;
    private final Timeline paramRenderDebouncer = new Timeline(new KeyFrame(
            Duration.millis(250), e -> renderParamState()));

    public SetupPage() {
        sidebar.setPadding(new Insets(12, 8, 12, 12));
        sidebar.setPrefWidth(190);
        buildSidebar();

        ScrollPane sideScroll = new ScrollPane(sidebar);
        sideScroll.getStyleClass().add("side-scroll");
        sideScroll.setFitToWidth(true);
        sideScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sideScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setLeft(sideScroll);

        content.setPadding(new Insets(12));
        setCenter(content);

        lv.params.addListener((o, a, b) -> {
            if (b != null) {
                allRows.forEach(r -> r.refresh(b));
                refreshPidCount();
                paramRenderDebouncer.stop();
                paramRenderDebouncer.playFromStart();
            }
        });
        AppState.get().connStatusProperty().addListener((o, a, b) -> renderParamState());

        select("frame");
        if (lv.params.get() != null) {
            allRows.forEach(r -> r.refresh(lv.params.get()));
        } else {
            conn.requestParams();
        }
    }

    // ================= 侧栏 =================

    private void buildSidebar() {
        String lastGroup = null;
        for (SectionItem item : ITEMS) {
            if (!item.group().equals(lastGroup)) {
                Label g = new Label(item.group());
                g.getStyleClass().add("side-group");
                sidebar.getChildren().add(g);
                lastGroup = item.group();
            }
            Label l = new Label(item.title());
            l.getStyleClass().add("side-item");
            l.setMaxWidth(Double.MAX_VALUE);
            l.setOnMouseClicked(e -> select(item.id()));
            sideLabels.put(item.id(), l);
            sidebar.getChildren().add(l);
        }
    }

    private void select(String id) {
        sideLabels.forEach((k, l) -> l.getStyleClass().remove("active"));
        Label l = sideLabels.get(id);
        if (l != null) {
            l.getStyleClass().add("active");
        }
        Region sec = sectionCache.computeIfAbsent(id, this::buildSection);
        content.getChildren().setAll(sec);
    }

    // ================= 通用组件 =================

    private VBox card(String title, String note, Region... bodies) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(12));
        Label t = new Label(title);
        t.getStyleClass().add("plan-title");
        Label n = new Label(note);
        n.getStyleClass().add("section-note");
        n.setWrapText(true);
        card.getChildren().addAll(t, n);
        for (Region b : bodies) {
            card.getChildren().add(b);
        }
        return card;
    }

    /** 面板头：标题 + 副标题 + 右侧动作 */
    private HBox panelHead(String title, String sub, Region action) {
        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title);
        t.getStyleClass().add("plan-title");
        Label s = new Label(sub);
        s.getStyleClass().add("card-sub");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        head.getChildren().addAll(t, s);
        if (action != null) {
            head.getChildren().add(action);
        }
        return head;
    }

    /** 参数行（含范围列与步进器） */
    private final class ParamRow extends HBox {
        private final String name;
        private final double min, max;
        private final TextField field = new TextField();

        ParamRow(String name) {
            this.name = name;
            ParamDict.Info info = ParamDict.info(name);
            this.min = info != null ? info.min() : 0;
            this.max = info != null ? info.max() : 100;
            getStyleClass().add("param-row");
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(10);

            Label desc = new Label(info != null ? info.desc() : ParamDict.desc(name));
            desc.getStyleClass().add("param-desc");
            desc.setMinWidth(130);
            Label pname = new Label(name);
            pname.getStyleClass().add("param-name");
            pname.setMinWidth(140);

            Button minus = new Button("−");
            minus.getStyleClass().add("step-btn");
            minus.setPrefWidth(30);
            Button plus = new Button("+");
            plus.getStyleClass().add("step-btn");
            plus.setPrefWidth(30);

            field.getStyleClass().addAll("text-field-dark", "param-field");
            field.setPrefWidth(90);
            field.setPromptText("—");

            Label range = new Label(rangeText());
            range.getStyleClass().add("param-range");
            range.setMinWidth(120);

            minus.setOnAction(e -> step(-1));
            plus.setOnAction(e -> step(1));

            Button write = new Button("写入");
            write.getStyleClass().add("btn-soft");
            Button read = new Button("读取");
            read.getStyleClass().add("btn-soft");
            write.setOnAction(e -> writeParam());
            read.setOnAction(e -> conn.requestOnce("p " + name));

            HBox stepper = new HBox(2, minus, field, plus);
            stepper.setAlignment(Pos.CENTER);

            getChildren().addAll(desc, pname, stepper, range, write, read);
            allRows.add(this);
        }

        private double stepSize() {
            return (max - min) > 20 ? 1 : 0.1;
        }

        private void step(int dir) {
            double cur;
            try {
                cur = Double.parseDouble(field.getText().trim());
            } catch (NumberFormatException ex) {
                cur = (min + max) / 2;
            }
            double v = Math.max(min, Math.min(max, cur + dir * stepSize()));
            field.setText(ParamDict.fmt(v));
        }

        private String rangeText() {
            return ParamDict.fmt(min) + " ~ " + ParamDict.fmt(max);
        }

        void refresh(Map<String, Double> params) {
            Double v = params.get(name);
            if (v == null) {
                return;
            }
            if (!field.isFocused()) {
                field.setText(ParamDict.fmt(v));
            }
        }

        private void writeParam() {
            String text = field.getText().trim();
            double v;
            try {
                v = Double.parseDouble(text);
            } catch (NumberFormatException ex) {
                Toast.show("参数值必须为数字：" + name, Toast.Type.WARNING);
                return;
            }
            conn.sendCommand("p " + name + " " + ParamDict.fmt(v), out -> {
                if (out.contains("not found")) {
                    Toast.show("固件拒绝：" + out, Toast.Type.ERROR);
                }
            });
        }
    }

    /** 带表头的参数网格 */
    private GridPane paramGrid(List<String> names) {
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(6);
        String[] heads = {"参数", "名称", "当前值", "范围", "操作"};
        double[] widths = {130, 140, 160, 120, 110};
        for (int i = 0; i < heads.length; i++) {
            Label h = new Label(heads[i]);
            h.getStyleClass().add("param-head");
            h.setMinWidth(widths[i]);
            g.add(h, i, 0);
        }
        int row = 1;
        for (String n : names) {
            ParamRow r = new ParamRow(n);
            GridPane.setHgrow(r, Priority.ALWAYS);
            g.add(r, 0, row, 5, 1);
            row++;
        }
        return g;
    }

    private VBox emptyState(String text) {
        VBox v = new VBox(6);
        v.getStyleClass().add("empty-card");
        v.setAlignment(Pos.CENTER);
        Label l = new Label(text);
        l.getStyleClass().add("section-note");
        v.getChildren().add(l);
        VBox.setVgrow(v, Priority.ALWAYS);
        return v;
    }

    // ================= 各 Section =================

    private Region buildSection(String id) {
        return switch (id) {
            case "frame" -> frameSection();
            case "motors" -> motorsSection();
            case "rc" -> rcSection();
            case "ports" -> portsSection();
            case "pid" -> pidSection();
            case "safety" -> safetySection();
            case "imu" -> imuSection();
            case "compass" -> emptyState("该机型未检测到磁力计，无法校准");
            case "gps" -> emptyState("该机型无 GPS 硬件");
            case "baro" -> emptyState("该机型未检测到气压计（BMP388）");
            case "flow" -> emptyState("该机型未检测到光流传感器（PMW3901）");
            case "range" -> emptyState("该机型未检测到激光测距仪（VL53L1X）");
            case "paramlist" -> paramListSection();
            case "firmware" -> firmwareSection();
            default -> emptyState("未知页面");
        };
    }

    // ---- 机架类型 ----

    private record FrameDef(String title, String desc, String clsType,
                            double[][] motors, boolean[] cw, int cols) {
    }

    private static final List<FrameDef> FRAMES = List.of(
            new FrameDef("四旋翼 X", "最常见机架，前右CW+前左CCW+后左CW+后右CCW",
                    "CLASS=1 TYPE=1", new double[][]{{45, 1}, {315, 1}, {225, 1}, {135, 1}},
                    new boolean[]{true, true, false, false}, 4),
            new FrameDef("四旋翼 +", "前后左右各一个电机", "CLASS=1 TYPE=0",
                    new double[][]{{0, 1}, {180, 1}, {90, 0}, {270, 0}},
                    new boolean[]{false, true, true, false}, 4),
            new FrameDef("四旋翼 H", "H 形机架，轴距较大，抗风性好", "CLASS=1 TYPE=3",
                    new double[][]{{45, 1}, {315, 1}, {225, 1}, {135, 1}},
                    new boolean[]{false, true, true, false}, 4),
            new FrameDef("四旋翼 BF-X", "BetaFlight X 布局（穿越机常用）", "CLASS=1 TYPE=10",
                    new double[][]{{45, 1}, {315, 1}, {225, 1}, {135, 1}},
                    new boolean[]{false, false, true, true}, 4),
            new FrameDef("六旋翼 X", "六电机 X 形布局，载重大", "CLASS=2 TYPE=1",
                    new double[][]{{0, 1}, {60, 0}, {120, 1}, {180, 0}, {240, 1}, {300, 0}},
                    new boolean[]{true, false, true, false, true, false}, 6),
            new FrameDef("八旋翼 X", "八电机，冗余高，适合大载重", "CLASS=3 TYPE=1",
                    new double[][]{{0, 1}, {45, 0}, {90, 1}, {135, 0}, {180, 1}, {225, 0}, {270, 1}, {315, 0}},
                    new boolean[]{true, false, true, false, true, false, true, false}, 8),
            new FrameDef("三旋翼 Y", "前两个固定电机，后一个可偏转电机", "CLASS=7 TYPE=0",
                    new double[][]{{0, 1}, {140, 0}, {220, 1}},
                    new boolean[]{false, true, true}, 3));

    /** 电机布局示意图（CW=蓝 CCW=红，连线到中心） */
    private javafx.scene.canvas.Canvas frameDiagram(FrameDef f) {
        javafx.scene.canvas.Canvas c = new javafx.scene.canvas.Canvas(110, 100);
        var g = c.getGraphicsContext2D();
        double cx = 55, cy = 50, r = 38;
        g.setStroke(Color.web("#484f58"));
        g.setLineWidth(2);
        for (double[] m : f.motors()) {
            double a = Math.toRadians(m[0]);
            g.strokeLine(cx, cy, cx + Math.sin(a) * r, cy - Math.cos(a) * r);
        }
        for (int i = 0; i < f.motors().length; i++) {
            double a = Math.toRadians(f.motors()[i][0]);
            double mx = cx + Math.sin(a) * r, my = cy - Math.cos(a) * r;
            g.setFill(f.cw()[i] ? Color.web("#4b8bf5") : Color.web("#ef4444"));
            g.fillOval(mx - 11, my - 11, 22, 22);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 7));
            g.fillText(f.cw()[i] ? "CW" : "CCW", mx - 7, my + 2.5);
        }
        g.setFill(Color.web("#f0a500"));
        g.fillOval(cx - 4, cy - 4, 8, 8);
        return c;
    }

    private Region frameSection() {
        Button readCur = new Button("读取当前");
        readCur.getStyleClass().add("btn-soft");
        readCur.setOnAction(e -> Toast.show(
                "CF-Drone 为固定四旋翼 X 布局（MOT_PIN_FL/FR/RL/RR 定义）", Toast.Type.INFO));

        Label cur = new Label();
        cur.getStyleClass().add("cur-param");
        Runnable updCur = () -> {
            Map<String, Double> ps = lv.params.get();
            if (ps == null || ps.isEmpty()) {
                cur.setText("当前参数：FRAME_CLASS = — FRAME_TYPE = —（请先获取参数列表）");
            } else {
                cur.setText("当前参数：机型 = 四旋翼 X · 电机引脚 FL/FR/RL/RR = "
                        + ParamDict.fmt(ps.getOrDefault("MOT_PIN_FL", Double.NaN)) + "/"
                        + ParamDict.fmt(ps.getOrDefault("MOT_PIN_FR", Double.NaN)) + "/"
                        + ParamDict.fmt(ps.getOrDefault("MOT_PIN_RL", Double.NaN)) + "/"
                        + ParamDict.fmt(ps.getOrDefault("MOT_PIN_RR", Double.NaN))
                        + " · FRAME_CLASS/TYPE 不适用（自研固件）");
            }
        };
        lv.params.addListener((o, a, b) -> updCur.run());
        updCur.run();

        FlowPane cards = new FlowPane(10, 10);
        for (FrameDef f : FRAMES) {
            VBox card = new VBox(4);
            card.getStyleClass().add("frame-card");
            card.setAlignment(Pos.CENTER);
            card.setPrefWidth(240);
            card.getChildren().add(frameDiagram(f));
            Label t = new Label(f.title());
            t.getStyleClass().add("frame-title");
            Label d = new Label(f.desc());
            d.getStyleClass().add("frame-desc");
            d.setWrapText(true);
            d.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            Label ct = new Label(f.clsType());
            ct.getStyleClass().add("frame-cls");
            card.getChildren().addAll(t, d, ct);
            card.setOnMouseClicked(e -> {
                if (f.title().equals("四旋翼 X")) {
                    Toast.show("当前机型即为四旋翼 X", Toast.Type.SUCCESS);
                } else {
                    Toast.show("CF-Drone 为固定四旋翼 X 布局，不支持更改机架", Toast.Type.WARNING);
                }
            });
            cards.getChildren().add(card);
        }
        ScrollPane cardsScroll = new ScrollPane(cards);
        cardsScroll.setFitToWidth(true);
        cardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cardsScroll.getStyleClass().add("body-scroll");

        Label footer = new Label("蓝色为顺时针（CW）电机，红色为逆时针（CCW）电机。更改机架类型后需要重启飞控才能生效。");
        footer.getStyleClass().add("section-note");

        VBox body = new VBox(10, panelHead("机架类型", "选择机架后点击应用，重启飞控生效", readCur),
                cur, cardsScroll, footer);
        VBox.setVgrow(cardsScroll, Priority.ALWAYS);
        return body;
    }

    // ---- 电机设置 ----

    private static final class MotorBar extends VBox {
        private final Region fill = new Region();
        private final double trackH = 90;

        MotorBar(String name) {
            setSpacing(3);
            setAlignment(Pos.BOTTOM_CENTER);
            StackPane track = new StackPane();
            track.getStyleClass().add("bar-track");
            track.setMinHeight(trackH);
            track.setMaxHeight(trackH);
            fill.getStyleClass().add("bar-fill-mot");
            track.getChildren().add(fill);
            Label label = new Label(name);
            label.getStyleClass().add("bar-label");
            getChildren().addAll(track, label);
            StackPane.setAlignment(fill, Pos.BOTTOM_CENTER);
        }

        void update(double pct) {
            fill.setMaxHeight(Math.max(2, Math.max(0, Math.min(100, pct)) / 100.0 * trackH));
        }
    }

    private Region motorsSection() {
        HBox bars = new HBox(8);
        bars.setAlignment(Pos.BOTTOM_CENTER);
        for (int i = 0; i < 12; i++) {
            MotorBar b = new MotorBar("M" + (i + 1));
            motorBars.add(b);
            HBox.setHgrow(b, Priority.ALWAYS);
            bars.getChildren().add(b);
        }

        motorWarn = new Label("⚠ 未收到电机输出数据，请确认飞控已发送电机输出（mot 轮询）");
        motorWarn.getStyleClass().add("warn-strip");

        lv.motors.addListener((o, a, m) -> {
            motorWarn.setVisible(false);
            motorWarn.setManaged(false);
            if (m != null && m.length == 4) {
                // 数组序 FR, FL, RR, RL → M1-M4
                motorBars.get(0).update(m[0] * 100);
                motorBars.get(1).update(m[1] * 100);
                motorBars.get(2).update(m[2] * 100);
                motorBars.get(3).update(m[3] * 100);
            }
        });

        VBox outCard = new VBox(10, panelHead("电机输出", "SERVO_OUTPUT_RAW · M1-M12", null), bars, motorWarn);
        outCard.getStyleClass().add("card");
        outCard.setPadding(new Insets(12));

        GridPane params = paramGrid(List.of("MOT_PIN_FL", "MOT_PIN_FR", "MOT_PIN_RL", "MOT_PIN_RR",
                "MOT_PWM_FREQ", "MOT_PWM_RES", "MOT_PWM_MIN", "MOT_PWM_MAX",
                "MOT_THR_MIN", "MOT_THR_MAX"));

        ScrollPane sp = new ScrollPane(new VBox(12, outCard,
                card("电机参数", "写入前请确认电机已停转", params)));
        sp.getStyleClass().add("body-scroll");
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    // ---- 遥控器 ----

    private static final class RcChannelCard extends VBox {
        private final Label value = new Label("—");
        private final Label pctLabel = new Label("+0%");
        private final Region fill = new Region();

        RcChannelCard(String ch, String func) {
            getStyleClass().add("rc-card");
            setSpacing(4);
            HBox head = new HBox(6);
            Label c = new Label(ch);
            c.getStyleClass().add("ch-badge");
            Label f = new Label(func);
            f.getStyleClass().add("ch-func");
            head.getChildren().addAll(c, f);

            StackPane track = new StackPane();
            track.getStyleClass().add("rc-track");
            track.setPrefHeight(6);
            fill.getStyleClass().add("rc-fill");
            fill.setMaxWidth(0);
            track.getChildren().add(fill);

            HBox valRow = new HBox(6);
            Region r = new Region();
            HBox.setHgrow(r, Priority.ALWAYS);
            valRow.getChildren().addAll(value, r, pctLabel);
            value.getStyleClass().add("rc-value");
            pctLabel.getStyleClass().add("rc-pct");

            getChildren().addAll(head, track, valRow);
        }

        void update(int us) {
            if (us < 0) {
                value.setText("—");
                pctLabel.setText("+0%");
                fill.setMaxWidth(0);
                return;
            }
            double pct = (us - 1500) / 500.0 * 100;
            value.setText(String.valueOf(us));
            pctLabel.setText(String.format("%+.0f%%", pct));
            double w = Math.min(100, Math.abs(pct)) / 100.0 * 140;
            fill.setMaxWidth(w);
            fill.setTranslateX(pct >= 0 ? 0 : -w);
        }
    }

    private Region rcSection() {
        Label rssi = new Label("RSSI 0%");
        rssi.getStyleClass().add("chip");
        Label waitBadge = new Label("等待信号");
        waitBadge.getStyleClass().add("wait-badge");

        FlowPane chCards = new FlowPane(10, 10);
        String[][] chNames = {
                {"CH1", "横滚 (Roll)"}, {"CH2", "俯仰 (Pitch)"}, {"CH3", "油门 (Throttle)"},
                {"CH4", "偏航 (Yaw)"}, {"CH5", "飞行模式"}, {"CH6", "辅助 1"},
                {"CH7", "辅助 2"}, {"CH8", "辅助 3"}};
        List<RcChannelCard> cards = new ArrayList<>();
        for (String[] cn : chNames) {
            RcChannelCard card = new RcChannelCard(cn[0], cn[1]);
            cards.add(card);
            chCards.getChildren().add(card);
        }
        lv.rcChannels.addListener((o, a, ch) -> {
            if (ch == null) {
                return;
            }
            boolean any = false;
            for (int i = 0; i < cards.size() && i < ch.length; i++) {
                cards.get(i).update(ch[i] > 500 ? ch[i] : -1);
                any |= ch[i] > 500;
            }
            waitBadge.setText(any ? "信号正常" : "等待信号");
        });

        Label note = new Label("该机型无实体遥控器时显示等待信号；RSSI 来自 CRSF 上行链路质量。");
        note.getStyleClass().add("section-note");

        ScrollPane sp = new ScrollPane(new VBox(12,
                card("遥控通道", "等待信号", chCards, note),
                card("RC 参数", "通道映射 / 协议 / 波特率（写入前请确认电机已停转）",
                        paramGrid(List.of("RC_ROLL", "RC_PITCH", "RC_YAW", "RC_THROTTLE",
                                "RC_MODE", "RC_PROTOCOL", "RC_BAUD", "RC_RX_PIN", "RC_TX_PIN")))));
        sp.getStyleClass().add("body-scroll");
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        HBox headRow = new HBox(10);
        headRow.setAlignment(Pos.CENTER_LEFT);
        headRow.getChildren().addAll(head(sp), rssi);
        HBox.setHgrow(head(sp), Priority.ALWAYS);
        return new VBox(headRow, sp);
    }

    /** 让 headRow 里的标题占满（配合右侧 RSSI 徽章） */
    private HBox head(Region content) {
        HBox h = new HBox(content);
        HBox.setHgrow(h, Priority.ALWAYS);
        return h;
    }

    // ---- 端口设置 ----

    private Region portsSection() {
        Button readAll = new Button("读取全部");
        readAll.getStyleClass().add("btn-soft");
        readAll.setOnAction(e -> conn.requestParams());

        GridPane table = new GridPane();
        table.setHgap(14);
        table.setVgap(10);
        String[] heads = {"串口", "协议", "波特率", "操作"};
        for (int i = 0; i < heads.length; i++) {
            Label h = new Label(heads[i]);
            h.getStyleClass().add("param-head");
            table.add(h, i, 0);
        }
        for (int i = 1; i <= 6; i++) {
            int row = i;
            table.add(new Label("SERIAL" + i), 0, row);
            TextField proto = new TextField("—");
            proto.getStyleClass().add("text-field-dark");
            proto.setPrefWidth(300);
            proto.setEditable(false);
            table.add(proto, 1, row);
            TextField baud = new TextField("—");
            baud.getStyleClass().add("text-field-dark");
            baud.setPrefWidth(120);
            baud.setEditable(false);
            table.add(baud, 2, row);
            HBox ops = new HBox(8);
            Button w = new Button("写入");
            w.getStyleClass().add("btn-soft");
            w.setDisable(true);
            Button r = new Button("读取");
            r.getStyleClass().add("btn-soft");
            r.setDisable(true);
            ops.getChildren().addAll(w, r);
            table.add(ops, 3, row);
        }
        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setMinWidth(90);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPrefWidth(320);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPrefWidth(140);
        table.getColumnConstraints().addAll(cc0, cc1, cc2);

        Label note = new Label("修改端口设置后需重启飞控生效。SERIAL0 为飞控主 USB，不在此配置。\n"
                + "该机型（CF-Drone）无 SERIAL1-6 配置；WiFi 链路参数见下方。");
        note.getStyleClass().add("section-note");
        note.setWrapText(true);

        ScrollPane sp = new ScrollPane(new VBox(12,
                panelHead("端口设置", "SERIAL1-6 协议/波特率配置", readAll),
                table, note,
                card("WiFi / MAVLink 参数", "该机型的链路参数",
                        paramGrid(List.of("WIFI_MODE", "WIFI_LOC_PORT", "WIFI_REM_PORT",
                                "MAV_SYS_ID", "MAV_RATE_SLOW", "MAV_RATE_FAST")))));
        sp.getStyleClass().add("body-scroll");
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    // ---- PID 调参 ----

    private Region pidSection() {
        pidAll.clear();
        List<String> att = List.of("CTL_P_P", "CTL_P_I", "CTL_P_D", "CTL_P_WU",
                "CTL_R_P", "CTL_R_I", "CTL_R_D", "CTL_R_WU",
                "CTL_Y_P", "CTL_TILT_MAX", "CTL_TRIM_ROLL", "CTL_TRIM_PITCH", "CTL_STICK_DZ");
        List<String> rate = List.of(
                "CTL_P_RATE_P", "CTL_P_RATE_I", "CTL_P_RATE_D", "CTL_P_RATE_WU", "CTL_P_RATE_D_HZ",
                "CTL_R_RATE_P", "CTL_R_RATE_I", "CTL_R_RATE_D", "CTL_R_RATE_WU", "CTL_R_RATE_D_HZ",
                "CTL_Y_RATE_P", "CTL_Y_RATE_I", "CTL_Y_RATE_D",
                "CTL_P_RATE_MAX", "CTL_R_RATE_MAX", "CTL_Y_RATE_MAX");
        List<String> alt = List.of(
                "ALT_ALT_P", "ALT_VEL_SLEW", "ALT_VEL_P", "ALT_VEL_I", "ALT_VEL_D",
                "ALT_VEL_WU", "ALT_VEL_D_HZ", "ALT_VEL_MAX", "ALT_HOVER_THR", "ALT_HOVER_TAU",
                "ALT_EST_KP", "ALT_EST_KV", "ALT_EST_KB", "ALT_EST_KB_M", "ALT_EST_KV_B", "ALT_EST_KB_B",
                "ALT_BARO_INNO", "ALT_LAND_BLEED", "ALT_VELZ_TAU", "BARO_LPF_HZ");
        List<String> pos = List.of(
                "PH_VEL_P", "PH_VEL_I", "PH_VEL_D", "PH_VEL_WU", "PH_VEL_MAX",
                "PH_POS_P", "PH_ACC_MAX", "PH_ACC_STD", "PH_VEL_EST_KB");
        pidAll.addAll(att);
        pidAll.addAll(rate);
        pidAll.addAll(alt);
        pidAll.addAll(pos);

        Button fetchAll = new Button("获取全部参数");
        fetchAll.getStyleClass().add("btn-primary");
        fetchAll.setOnAction(e -> conn.requestParams());

        pidCountLabel = new Label("已加载 0/" + pidAll.size() + " 个参数");
        pidCountLabel.getStyleClass().add("card-sub");
        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label("PID 调参");
        t.getStyleClass().add("plan-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        head.getChildren().addAll(t, pidCountLabel, sp, fetchAll);

        HBox batch1 = batchRow("控制角度响应（外环）", att);
        HBox batch2 = batchRow("控制角速度响应（内环）", rate);
        HBox batch3 = batchRow("高度控制", alt);
        HBox batch4 = batchRow("位置控制", pos);

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("setup-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab("姿态环", new VBox(8, batch1, paramGrid(att))),
                new Tab("角速度环", new VBox(8, batch2, paramGrid(rate))),
                new Tab("高度控制", new VBox(8, batch3, paramGrid(alt))),
                new Tab("位置控制", new VBox(8, batch4, paramGrid(pos))));

        refreshPidCount();
        VBox body = new VBox(10, head, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        return body;
    }

    private HBox batchRow(String title, List<String> names) {
        Label l = new Label(title);
        l.getStyleClass().add("param-desc");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button b = new Button("批量读取");
        b.getStyleClass().add("btn-soft");
        b.setOnAction(e -> names.forEach(n -> conn.requestOnce("p " + n)));
        HBox row = new HBox(10, l, sp, b);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void refreshPidCount() {
        if (pidCountLabel == null) {
            return;
        }
        Map<String, Double> ps = lv.params.get();
        long loaded = pidAll.stream().filter(n -> ps != null && ps.containsKey(n)).count();
        pidCountLabel.setText("已加载 " + loaded + "/" + pidAll.size() + " 个参数");
    }

    // ---- 安全 / 校准 / 固件 ----

    private Region safetySection() {
        return card("安全设置", "失控保护与解锁检查（修改前请阅读固件安全说明；写入前确认电机停转）",
                paramGrid(List.of("SF_RC_LOSS_TIME", "SF_DESCEND_TIME", "ARM_CHK_LEVEL")));
    }

    private Region imuSection() {
        Label note = new Label("加速度计校准（ca）：将无人机水平放置，点击开始后保持绝对静止约 2 秒。\n"
                + "校准结果自动写入 IMU_ACC_BIAS_* 参数。");
        note.getStyleClass().add("section-note");
        note.setWrapText(true);

        Button start = new Button("开始加计校准（ca）");
        start.getStyleClass().add("btn-primary");
        start.setMaxWidth(Double.MAX_VALUE);
        start.setOnAction(e -> ConfirmDialog.show("IMU 校准",
                "校准期间请保持无人机水平静止。\n确认开始？",
                () -> conn.sendCommand("ca", out -> Toast.show(
                        out.isEmpty() ? "校准指令已发送" : out, Toast.Type.INFO))));

        VBox rows = new VBox(6);
        for (String n : List.of("IMU_ACC_BIAS_X", "IMU_ACC_BIAS_Y", "IMU_ACC_BIAS_Z",
                "IMU_ACC_SCALE_X", "IMU_ACC_SCALE_Y", "IMU_ACC_SCALE_Z")) {
            rows.getChildren().add(new ParamRow(n));
        }
        return card("IMU 校准", "MPU-6500 校准数据（ca 后自动更新）", note, start, rows);
    }

    private Region firmwareSection() {
        return card("固件升级", "通过 USB / OTA 升级 CF-Drone 固件",
                emptyState("固件升级功能规划中（S12 交付）"));
    }

    // ---- 参数列表（三态） ----

    private Region paramListSection() {
        VBox root = new VBox(10);

        Button fetch = new Button("获取全部参数");
        fetch.getStyleClass().add("btn-primary");
        fetch.setOnAction(e -> {
            if (!conn.isConnected()) {
                Toast.show("请先连接飞控", Toast.Type.WARNING);
                return;
            }
            Toast.show("正在获取参数列表…", Toast.Type.INFO);
            conn.requestParams();
        });

        paramSearch = new TextField();
        paramSearch.getStyleClass().add("text-field-dark");
        paramSearch.setPrefWidth(260);
        paramSearch.setPromptText("搜索参数名...");
        paramSearch.textProperty().addListener((o, a, b) -> renderParamState());

        Button export = new Button("↓ 导出 JSON");
        export.getStyleClass().add("btn-soft");
        export.setOnAction(e -> exportParams());

        Button importB = new Button("↑ 导入 JSON");
        importB.getStyleClass().add("btn-soft");
        importB.setOnAction(e -> importParams());

        paramCounter = new Label("0 / — 个参数");
        paramCounter.getStyleClass().add("card-sub");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox toolbar = new HBox(10, fetch, paramSearch, export, importB, sp, paramCounter);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        paramListBody = new VBox();
        VBox.setVgrow(paramListBody, Priority.ALWAYS);
        root.getChildren().addAll(toolbar, paramListBody);
        VBox.setVgrow(root, Priority.ALWAYS);
        renderParamState();
        return root;
    }

    /** 参数列表三态渲染：未连接 / 已连接待拉取 / 已加载表格 */
    private void renderParamState() {
        if (paramListBody == null) {
            return;
        }
        Map<String, Double> ps = lv.params.get();
        paramListBody.getChildren().clear();
        if (!conn.isConnected()) {
            paramCounter.setText("0 / — 个参数");
            paramListBody.getChildren().add(emptyState("请先连接飞控"));
            return;
        }
        if (ps == null || ps.isEmpty()) {
            paramCounter.setText("0 / — 个参数");
            paramListBody.getChildren().add(emptyState("点击「获取全部参数」加载飞控参数"));
            return;
        }
        paramCounter.setText(ps.size() + " / " + ps.size() + " 个参数");
        ScrollPane table = buildParamTable(ps, paramSearch == null ? "" : paramSearch.getText());
        VBox.setVgrow(table, Priority.ALWAYS);
        paramListBody.getChildren().add(table);
    }

    /** 参数表格（参数名/当前值/类型/#/操作），按名称排序 + 搜索过滤 */
    private ScrollPane buildParamTable(Map<String, Double> ps, String filter) {
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(4);
        g.setPadding(new Insets(6));
        String[] heads = {"参数名", "当前值", "类型", "#", "操作"};
        for (int i = 0; i < heads.length; i++) {
            Label h = new Label(heads[i]);
            h.getStyleClass().add("param-head");
            g.add(h, i, 0);
        }
        List<String> names = ps.keySet().stream().sorted().toList();
        String f = filter == null ? "" : filter.trim().toUpperCase();
        int row = 1;
        for (String n : names) {
            if (!f.isEmpty() && !n.toUpperCase().contains(f)) {
                continue;
            }
            Label name = new Label(n);
            name.getStyleClass().add("param-link");
            g.add(name, 0, row);

            Label val = new Label(ParamDict.fmt(ps.get(n)));
            val.getStyleClass().add("sensor-val");
            g.add(val, 1, row);

            Label type = new Label("FLOAT");
            type.getStyleClass().add("type-badge");
            g.add(type, 2, row);

            g.add(new Label("-"), 3, row);

            Button rf = new Button("⟳ 刷新");
            rf.getStyleClass().add("btn-soft");
            String nm = n;
            rf.setOnAction(e -> conn.requestOnce("p " + nm));
            g.add(rf, 4, row);

            row++;
        }
        ScrollPane sp = new ScrollPane(g);
        sp.getStyleClass().add("body-scroll");
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    private void exportParams() {
        Map<String, Double> ps = lv.params.get();
        if (ps == null || ps.isEmpty()) {
            Toast.show("暂无参数可导出，请先获取参数", Toast.Type.WARNING);
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("导出参数 JSON");
        fc.setInitialFileName("params.json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File f = fc.showSaveDialog(getWindow());
        if (f == null) {
            return;
        }
        StringBuilder json = new StringBuilder("{\n");
        boolean first = true;
        for (var e : ps.entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            json.append("  \"").append(e.getKey()).append("\": ").append(ParamDict.fmt(e.getValue()));
            first = false;
        }
        json.append("\n}");
        try {
            Files.writeString(f.toPath(), json.toString(), StandardCharsets.UTF_8);
            Toast.show("已导出 " + ps.size() + " 个参数", Toast.Type.SUCCESS);
        } catch (IOException e) {
            Toast.show("导出失败：" + e.getMessage(), Toast.Type.ERROR);
        }
    }

    private void importParams() {
        if (!conn.isConnected()) {
            Toast.show("请先连接飞控", Toast.Type.WARNING);
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("导入参数 JSON");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File f = fc.showOpenDialog(getWindow());
        if (f == null) {
            return;
        }
        try {
            String json = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("\"([A-Z0-9_]+)\"\\s*:\\s*(-?[\\d.]+)").matcher(json);
            List<String[]> entries = new ArrayList<>();
            while (m.find()) {
                entries.add(new String[]{m.group(1), m.group(2)});
            }
            if (entries.isEmpty()) {
                Toast.show("文件中没有可识别的参数", Toast.Type.WARNING);
                return;
            }
            ConfirmDialog.show("导入参数",
                    "将向飞控写入 " + entries.size() + " 个参数（写入前请确认电机停转）。\n确认导入？",
                    () -> new Thread(() -> {
                        for (String[] e : entries) {
                            conn.sendCommand("p " + e[0] + " " + e[1], null);
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                        conn.requestParams();
                        javafx.application.Platform.runLater(() ->
                                Toast.show("导入完成，共写入 " + entries.size() + " 个参数", Toast.Type.SUCCESS));
                    }, "param-import").start());
        } catch (IOException e) {
            Toast.show("读取失败：" + e.getMessage(), Toast.Type.ERROR);
        }
    }

    private javafx.stage.Window getWindow() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
