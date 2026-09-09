package com.cherglow.gcs.ui.pages;

import com.cherglow.gcs.core.ConnectionService;
import com.cherglow.gcs.core.FlightRecorder;
import com.cherglow.gcs.model.LiveVehicle;
import com.cherglow.gcs.ui.ConfirmDialog;
import com.cherglow.gcs.ui.Toast;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 数据页（S10）：三个 tab（波形监视器 / 控制台 / 飞行日志）。
 * 本 story 实现波形监视器：250ms 固定采样 → 环形缓冲 → Canvas 多通道绘制；
 * 时间窗 5/10/30/60s、暂停/清空、通道开关侧栏、十字光标 + tooltip。
 * 控制台与飞行日志为 S11 占位。
 */
public class DataPage extends BasePage {

    @Override
    public String pageId() {
        return "data";
    }

    private long sampleMs = 250; // B6：采样间隔（默认 250ms=4Hz，可配置 1-10s）
    private static final int RING_CAP = 260; // 覆盖 60s 窗口（4Hz）+ 余量

    private static final String[] PALETTE = {
            "#f0a500", "#4b8bf5", "#22c55e", "#ef4444",
            "#a855f7", "#06b6d4", "#f97316", "#ec4899",
            "#84cc16", "#eab308", "#14b8a6", "#8b5cf6",
            "#f43f5e", "#0ea5e9", "#d946ef", "#94a3b8"};

    /** 通道定义 */
    private record Chan(String id, String group, String name, String unit,
                        Color color, java.util.function.DoubleSupplier value) {
    }

    private final List<Chan> channels = new ArrayList<>();
    private final Map<String, Ring> rings = new java.util.LinkedHashMap<>();
    private final Map<String, Boolean> enabled = new java.util.LinkedHashMap<>();

    private long sampleCount;
    private boolean paused;
    private int windowSec = 10;

    private final Canvas chart = new Canvas(800, 400);
    private final Label counter = new Label("0 通道 · 0 样本");
    private Timeline sampler = new Timeline(new KeyFrame(Duration.millis(sampleMs), e -> sample()));
    private final Timeline pulse = new Timeline(new KeyFrame(
            Duration.millis(120), e -> {
        if (!paused) {
            draw();
        }
    }));
    private double crossX = -1;
    private final List<Label> tabLabels = new ArrayList<>();
    private final List<Region> tabPages = new ArrayList<>();

    // CLI 控制台
    private final ObservableList<String> consoleLines = FXCollections.observableArrayList();
    private final javafx.beans.property.BooleanProperty consoleAutoScroll =
            new javafx.beans.property.SimpleBooleanProperty(this, "consoleAutoScroll", true);
    private boolean consolePaused;
    private javafx.scene.control.ListView<String> consoleView;
    private FilteredList<String> consoleVisible;
    private TextField consoleFilter;
    private TextField consoleInput;

    // 飞行日志
    private final FlightRecorder recorder = FlightRecorder.get();
    private javafx.scene.control.ListView<String> logList;
    private final List<Path> logPaths = new ArrayList<>(); // 与列表项一一对应（记录中占位=null）
    private ComboBox<String> modelFilter;
    private ComboBox<String> dateFilter;
    private Label logMeta;
    private final Canvas logCanvas = new Canvas(600, 260);
    private javafx.scene.control.Slider logSlider;
    private Label logReadout;
    private FlightRecorder.Session loadedSession;

    public DataPage() {
        buildChannels(); // 先建通道定义，再构建含数据源侧栏的界面
        setTop(tabBar());
        setCenter(tabHost());
        sampler.setCycleCount(Timeline.INDEFINITE);
        sampler.play();
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }

    // ================= Tab 容器 =================

    private HBox tabBar() {
        HBox bar = new HBox(4);
        bar.getStyleClass().add("map-toolbar");
        String[] tabs = {"波形监视器", "CLI 控制台", "飞行日志"};
        for (int i = 0; i < tabs.length; i++) {
            Label l = new Label(tabs[i]);
            l.getStyleClass().add("dlg-tab");
            if (i == 0) {
                l.getStyleClass().add("active");
            }
            int idx = i;
            l.setOnMouseClicked(e -> selectTab(idx));
            tabLabels.add(l);
            bar.getChildren().add(l);
        }
        return bar;
    }

    private void selectTab(int idx) {
        for (int i = 0; i < tabLabels.size(); i++) {
            tabLabels.get(i).getStyleClass().remove("active");
            if (i == idx) {
                tabLabels.get(i).getStyleClass().add("active");
            }
            tabPages.get(i).setVisible(i == idx);
            tabPages.get(i).setManaged(i == idx);
        }
    }

    private StackPane tabHost() {
        StackPane host = new StackPane();
        BorderPane wave = new BorderPane();
        wave.setTop(waveToolbar());
        wave.setCenter(wrapChart());
        wave.setRight(sourceSidebar());
        tabPages.add(wave);
        tabPages.add(consolePane());
        tabPages.add(logPane());
        for (int i = 0; i < tabPages.size(); i++) {
            Region p = tabPages.get(i);
            VBox.setVgrow(p, Priority.ALWAYS);
            host.getChildren().add(p);
            p.setVisible(i == 0);
            p.setManaged(i == 0);
        }
        return host;
    }

    private Region placeholder(String text) {
        VBox v = new VBox(8);
        v.setAlignment(Pos.CENTER);
        Label l = new Label(text);
        l.getStyleClass().add("section-note");
        v.getChildren().add(l);
        return v;
    }

    // ================= 波形监视器 =================

    private void buildChannels() {
        int ci = 0;
        channels.add(new Chan("roll", "姿态", "横滚", "°", color(ci++),
                () -> LiveVehicle.get().rollDeg.get()));
        channels.add(new Chan("pitch", "姿态", "俯仰", "°", color(ci++),
                () -> LiveVehicle.get().pitchDeg.get()));
        channels.add(new Chan("yaw", "姿态", "偏航", "°", color(ci++),
                () -> LiveVehicle.get().yawDeg.get()));
        channels.add(new Chan("batv", "电池", "电压", "V", color(ci++),
                () -> LiveVehicle.get().batteryVoltage.get()));
        for (int m = 0; m < 4; m++) {
            final int idx = m;
            channels.add(new Chan("mot" + m, "电机", "M" + (m + 1), "%", color(ci++),
                    () -> {
                        float[] mo = LiveVehicle.get().motors.get();
                        return mo == null ? Double.NaN : mo[idx] * 100;
                    }));
        }
        // 环境组：温度(已有) + 湿度 + 气压
        channels.add(new Chan("temp", "环境", "温度", "°C", color(ci++),
                () -> LiveVehicle.get().temperatureC.get()));
        channels.add(new Chan("humi", "环境", "湿度", "%", color(ci++),
                () -> LiveVehicle.get().humidityPct.get()));
        channels.add(new Chan("press", "环境", "气压", "hPa", color(ci++),
                () -> LiveVehicle.get().pressureHpa.get()));
        // 默认启用：姿态 3 + 电压 + 环境 3
        for (Chan c : channels) {
            enabled.put(c.id(), c.group().equals("姿态") || c.group().equals("环境")
                    || c.id().equals("batv"));
        }
    }

    private static Color color(int i) {
        return Color.web(PALETTE[i % PALETTE.length]);
    }

    private HBox waveToolbar() {
        HBox bar = new HBox(8);
        bar.getStyleClass().add("map-toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        int[] windows = {5, 10, 30, 60};
        List<Label> wBtns = new ArrayList<>();
        for (int w : windows) {
            Label l = new Label(w + "s");
            l.getStyleClass().add("dlg-tab");
            if (w == 10) {
                l.getStyleClass().add("active");
            }
            l.setOnMouseClicked(e -> {
                windowSec = w;
                for (Label b : wBtns) {
                    b.getStyleClass().remove("active");
                }
                l.getStyleClass().add("active");
                draw();
            });
            wBtns.add(l);
            bar.getChildren().add(l);
        }

        Button pause = new Button("⏸ 暂停");
        pause.getStyleClass().add("btn-soft");
        pause.setOnAction(e -> {
            paused = !paused;
            pause.setText(paused ? "▶ 继续" : "⏸ 暂停");
        });

        Button clear = new Button("清空");
        clear.getStyleClass().add("btn-soft");
        clear.setOnAction(e -> {
            rings.values().forEach(Ring::clear);
            sampleCount = 0;
            counter.setText("0 通道 · 0 样本");
            draw();
        });

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        counter.getStyleClass().add("card-sub");
        bar.getChildren().addAll(pause, clear, sampleCombo(), sp, counter);
        return bar;
    }

    /** B6：波形采样间隔设置（1-10s），默认不选中保持 250ms。 */
    private ComboBox<String> sampleCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getStyleClass().add("dark-combo");
        combo.getItems().addAll("采样 1s", "采样 2s", "采样 3s", "采样 5s", "采样 10s");
        combo.setValue(null);
        combo.setPromptText("采样间隔");
        combo.setMaxWidth(120);
        combo.setOnAction(e -> {
            String v = combo.getValue();
            if (v == null) {
                return;
            }
            int sec = Integer.parseInt(v.replaceAll("\\D", ""));
            setSampleMs(sec * 1000L);
        });
        return combo;
    }

    private void setSampleMs(long ms) {
        sampleMs = ms;
        sampler.stop();
        sampler = new Timeline(new KeyFrame(Duration.millis(sampleMs), e -> sample()));
        sampler.setCycleCount(Timeline.INDEFINITE);
        sampler.play();
        Toast.show("波形采样间隔设为 " + (ms / 1000) + "s", Toast.Type.INFO);
    }

    private Region wrapChart() {
        StackPane pane = new StackPane(chart);
        pane.setPadding(new Insets(8));
        chart.setWidth(600);
        chart.setHeight(300);
        widthProperty().addListener((o, a, b) -> {
            chart.setWidth(Math.max(200, b.doubleValue() - 220));
            draw();
        });
        heightProperty().addListener((o, a, b) -> {
            chart.setHeight(Math.max(150, b.doubleValue() - 120));
            draw();
        });

        chart.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            crossX = e.getX();
            draw();
        });
        chart.setOnMouseExited(e -> {
            crossX = -1;
            draw();
        });
        return pane;
    }

    private ScrollPane sourceSidebar() {
        VBox box = new VBox(2);
        box.setPadding(new Insets(8));
        String lastGroup = null;
        int ci = 0;
        for (Chan c : channels) {
            if (!c.group().equals(lastGroup)) {
                Label g = new Label(c.group());
                g.getStyleClass().add("side-group");
                box.getChildren().add(g);
                lastGroup = c.group();
            }
            HBox item = new HBox(8);
            item.getStyleClass().add("src-item");
            item.setAlignment(Pos.CENTER_LEFT);
            Region dot = new Region();
            dot.setMinSize(8, 8);
            dot.setMaxSize(8, 8);
            dot.setBackground(new javafx.scene.layout.Background(
                    new javafx.scene.layout.BackgroundFill(c.color, null, null)));
            Label name = new Label(c.name + " " + c.unit);
            name.getStyleClass().add("side-item");
            name.setMaxWidth(Double.MAX_VALUE);
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Boolean en = enabled.get(c.id());
            name.setOpacity(en != null && en ? 1 : 0.4);
            item.getChildren().addAll(dot, name, sp);
            item.setOnMouseClicked(e -> {
                boolean now = !enabled.get(c.id());
                enabled.put(c.id(), now);
                name.setOpacity(now ? 1 : 0.4);
                draw();
            });
            box.getChildren().add(item);
            ci++;
        }
        ScrollPane sp = new ScrollPane(box);
        sp.getStyleClass().add("side-scroll");
        sp.setPrefWidth(190);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    // ================= 采样 =================

    private void sample() {
        if (paused) {
            return;
        }
        long now = System.currentTimeMillis();
        sampleCount++;
        for (Chan c : channels) {
            double v = c.value().getAsDouble();
            rings.computeIfAbsent(c.id(), k -> new Ring(RING_CAP)).add(now, v);
        }
        int en = (int) channels.stream().filter(c -> enabled.get(c.id())).count();
        counter.setText(en + " 通道 · " + sampleCount + " 样本");
    }

    // ================= 绘制 =================

    private void draw() {
        GraphicsContext g = chart.getGraphicsContext2D();
        double w = chart.getWidth(), h = chart.getHeight();
        if (w < 60 || h < 60) {
            return;
        }
        g.clearRect(0, 0, w, h);
        long now = System.currentTimeMillis();
        long windowMs = windowSec * 1000L;

        // 网格与时间刻度
        g.setFont(Font.font("Consolas", 10));
        for (int i = 0; i <= 5; i++) {
            double y = h * i / 5.0;
            g.setStroke(Color.web("#21262d"));
            g.setLineWidth(1);
            g.strokeLine(0, y, w, y);
        }
        int tickStep = windowSec <= 10 ? 2 : (windowSec <= 30 ? 5 : 10);
        for (int s = 0; s <= windowSec; s += tickStep) {
            double x = w * (1.0 - (double) s / windowSec);
            String lbl = java.time.LocalTime.now().minusSeconds(s)
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            g.setFill(Color.web("#484f58"));
            g.fillText(lbl, Math.max(2, Math.min(w - 56, x - 26)), h - 4);
            if (s > 0) {
                g.setStroke(Color.web("#161b22"));
                g.strokeLine(x, 0, x, h - 16);
            }
        }

        // 各启用通道折线（每通道自适应纵轴）
        for (Chan c : channels) {
            if (!enabled.get(c.id())) {
                continue;
            }
            Ring ring = rings.get(c.id());
            if (ring == null || ring.size() < 2) {
                continue;
            }
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (int i = 0; i < ring.size(); i++) {
                double v = ring.val(i);
                if (v == v) {
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
            }
            if (min > max) {
                continue;
            }
            // 近平稳数据防噪声放大：span 不足时以中点对称扩展至最小跨度
            double minSpan = minSpanFor(c.unit());
            if (max - min < minSpan) {
                double mid = (min + max) / 2;
                min = mid - minSpan / 2;
                max = mid + minSpan / 2;
            }
            double pad = (max - min) * 0.1 + 0.001;
            min -= pad;
            max += pad;

            g.setStroke(c.color);
            g.setLineWidth(1.6);
            boolean first = true;
            double lastX = 0, lastY = 0, lastVal = Double.NaN;
            for (int i = 0; i < ring.size(); i++) {
                long t = ring.time(i);
                double v = ring.val(i);
                if (v != v || now - t > windowMs) {
                    continue;
                }
                double x = w * (1.0 - (now - t) / (double) windowMs);
                double y = h - h * (v - min) / (max - min);
                if (first) {
                    g.beginPath();
                    g.moveTo(x, y);
                    first = false;
                } else {
                    g.lineTo(x, y);
                }
                lastX = x;
                lastY = y;
                lastVal = v;
            }
            g.stroke();
            if (!first) {
                g.setFill(c.color);
                g.fillText(c.name + " " + fmtVal(lastVal) + c.unit,
                        Math.min(w - 90, lastX + 4), Math.max(10, lastY - 4));
            }
        }

        // 十字光标 + tooltip
        if (crossX >= 0 && crossX <= w) {
            g.setStroke(Color.web("#8b949e"));
            g.setLineWidth(1);
            g.setLineDashes(4, 4);
            g.strokeLine(crossX, 0, crossX, h);
            g.setLineDashes(null);

            long t = now - (long) ((1.0 - crossX / w) * windowMs);
            StringBuilder tip = new StringBuilder(java.time.LocalTime.ofSecondOfDay(
                    java.time.Duration.ofMillis(t).toSeconds() % 86400)
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            for (Chan c : channels) {
                if (!enabled.get(c.id())) {
                    continue;
                }
                Ring ring = rings.get(c.id());
                if (ring == null) {
                    continue;
                }
                double v = ring.at(t, 700);
                if (v == v) {
                    tip.append('\n').append(c.name).append(' ').append(fmtVal(v)).append(c.unit);
                }
            }
            String[] lines = tip.toString().split("\n");
            double bw = 110, bh = 16 * lines.length + 8;
            double bx = Math.min(w - bw - 4, crossX + 8);
            g.setFill(Color.web("rgba(13,17,23,0.92)"));
            g.fillRect(bx, 8, bw, bh);
            g.setStroke(Color.web("#30363d"));
            g.strokeRect(bx, 8, bw, bh);
            g.setFill(Color.web("#e6edf3"));
            g.setFont(Font.font("Consolas", 10));
            for (int i = 0; i < lines.length; i++) {
                g.fillText(lines[i], bx + 6, 22 + i * 13);
            }
        }
    }

    private static String fmtVal(double v) {
        if (v != v) {
            return "—";
        }
        return Math.abs(v) >= 100 ? String.format("%.0f", v) : String.format("%.1f", v);
    }

    /** 各单位纵轴最小显示跨度；span 不足时以中点对称扩展，避免微噪声被放大至满屏 */
    private static double minSpanFor(String unit) {
        if (unit == null) {
            return 1.0;
        }
        return switch (unit) {
            case "V" -> 0.5;
            case "°", "%", "°C", "hPa" -> 5.0;
            default -> 1.0;
        };
    }

    /** 从日志 defs 标签名后缀推导单位（多字符后缀须先于单字符判断） */
    private static String unitOf(String label) {
        if (label.endsWith("hPa")) {
            return "hPa";
        }
        if (label.endsWith("°C")) {
            return "°C";
        }
        if (label.endsWith("°")) {
            return "°";
        }
        if (label.endsWith("%")) {
            return "%";
        }
        if (label.endsWith("V")) {
            return "V";
        }
        return "";
    }

    // ================= CLI 控制台 =================

    private static final List<String> DANGER_CMDS = List.of(
            "arm", "mfr", "mfl", "mrr", "mrl", "preset", "reboot");

    private Region consolePane() {
        VBox root = new VBox(8);
        root.setPadding(new Insets(8));

        HBox tools = new HBox(8);
        tools.setAlignment(Pos.CENTER_LEFT);
        consoleFilter = new TextField();
        consoleFilter.getStyleClass().add("text-field-dark");
        consoleFilter.setPrefWidth(200);
        consoleFilter.setPromptText("过滤输出...");
        consoleFilter.textProperty().addListener((o, a, b) ->
                consoleVisible.setPredicate(s -> b == null || b.isEmpty() || s.contains(b)));

        Button pauseBtn = new Button("\u23f8 暂停");
        pauseBtn.getStyleClass().add("btn-soft");
        pauseBtn.setOnAction(e -> {
            consolePaused = !consolePaused;
            pauseBtn.setText(consolePaused ? "\u25b6 继续" : "\u23f8 暂停");
        });

        Button autoBtn = new Button(consoleAutoScroll.get() ? "自动滚动：开" : "自动滚动：关");
        autoBtn.getStyleClass().add("btn-soft");
        autoBtn.setOnAction(e -> {
            consoleAutoScroll.set(!consoleAutoScroll.get());
            autoBtn.setText(consoleAutoScroll.get() ? "自动滚动：开" : "自动滚动：关");
            if (consoleAutoScroll.get()) {
                consoleView.scrollTo(Math.max(0, consoleLines.size() - 1));
            }
        });

        Button clear = new Button("清空");
        clear.getStyleClass().add("btn-soft");
        clear.setOnAction(e -> consoleLines.clear());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        tools.getChildren().addAll(consoleFilter, pauseBtn, autoBtn, clear, sp);
        HBox.setHgrow(tools, Priority.ALWAYS);

        consoleVisible = new FilteredList<>(consoleLines, s -> true);
        consoleView = new ListView<>(consoleVisible);
        consoleView.getStyleClass().add("console-out");
        consoleView.setCellFactory(v -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setFont(Font.font("Consolas", 11));
                // 命令回显橙 / 警告琥珀 / 其余主文本色（深浅主题均随背景令牌保证对比度）
                setTextFill(item == null ? Color.web("#e6edf3")
                        : item.startsWith(">>>") ? Color.web("#f0a500")
                        : item.startsWith("⚠") ? Color.web("#f59e0b")
                        : Color.web("#e6edf3"));
            }
        });
        VBox.setVgrow(consoleView, Priority.ALWAYS);
        consoleLines.addListener((javafx.collections.ListChangeListener<String>) c -> {
            if (consoleAutoScroll.get() && consoleView != null) {
                consoleView.scrollTo(consoleLines.size() - 1);
            }
        });

        HBox inputRow = new HBox(8);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        consoleInput = new TextField();
        consoleInput.getStyleClass().add("text-field-dark");
        consoleInput.setPromptText("输入 CLI 指令（如 status / ps / p / wifi，危险指令需确认）");
        HBox.setHgrow(consoleInput, Priority.ALWAYS);
        Button send = new Button("发送");
        send.getStyleClass().add("btn-primary");
        send.setOnAction(e -> submitConsole());
        consoleInput.setOnAction(e -> submitConsole());
        inputRow.getChildren().addAll(consoleInput, send);

        root.getChildren().addAll(tools, consoleView, inputRow);
        consoleAdd("CLI 控制台就绪。连接飞控后可发送只读/安全指令。");
        return root;
    }

    private void submitConsole() {
        String cmd = consoleInput.getText().trim();
        if (cmd.isEmpty()) {
            return;
        }
        consoleInput.clear();
        if (!ConnectionService.get().isConnected()) {
            consoleAdd("\u26a0 未连接飞控，指令未发送");
            return;
        }
        String low = cmd.toLowerCase();
        if (DANGER_CMDS.contains(low)) {
            ConfirmDialog.show("危险指令确认",
                    "指令「" + cmd + "」可能影响飞行安全（电机/参数复位/重启）。\n确认发送？",
                    () -> doSend(cmd));
            return;
        }
        doSend(cmd);
    }

    private void doSend(String cmd) {
        consoleLines.add(">>> " + cmd);
        ConnectionService.get().sendCommand(cmd, out -> {
            for (String l : out.split("\n")) {
                if (!l.isBlank()) {
                    consoleAdd("  " + l);
                }
            }
        });
    }

    private void consoleAdd(String line) {
        if (consolePaused) {
            return;
        }
        consoleLines.add(line);
        while (consoleLines.size() > 800) {
            consoleLines.remove(0);
        }
    }

    // ================= 飞行日志 =================

    private Region logPane() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8));

        VBox left = new VBox(8);
        left.setPrefWidth(280);
        left.setMinWidth(240);
        HBox listHead = new HBox(8);
        listHead.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label("飞行记录");
        t.getStyleClass().add("plan-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button refresh = new Button("刷新");
        refresh.getStyleClass().add("btn-soft");
        refresh.setOnAction(e -> refreshSessions());
        listHead.getChildren().addAll(t, sp, refresh);
        logList = new ListView<>();
        logList.getStyleClass().add("console-out");
        VBox.setVgrow(logList, Priority.ALWAYS);
        logList.setOnMouseClicked(ev -> {
            int idx = logList.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx >= logPaths.size()) {
                return;
            }
            Path p = logPaths.get(idx);
            if (p == null) {
                return; // 记录中占位
            }
            loadedSession = FlightRecorder.load(p);
            logSlider.setMax(Math.max(0, loadedSession.count() - 1));
            logSlider.setValue(0);
            drawLog();
        });

        // B5：按型号 / 日期筛选
        HBox filterRow = new HBox(6);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        modelFilter = new ComboBox<>();
        dateFilter = new ComboBox<>();
        modelFilter.getStyleClass().add("dark-combo");
        dateFilter.getStyleClass().add("dark-combo");
        modelFilter.setPromptText("型号");
        dateFilter.setPromptText("日期");
        modelFilter.setMaxWidth(Double.MAX_VALUE);
        dateFilter.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(modelFilter, Priority.ALWAYS);
        HBox.setHgrow(dateFilter, Priority.ALWAYS);
        modelFilter.setOnAction(e -> { if (!updatingFilters) refreshSessions(); });
        dateFilter.setOnAction(e -> { if (!updatingFilters) refreshSessions(); });
        filterRow.getChildren().addAll(modelFilter, dateFilter);

        left.getChildren().addAll(listHead, filterRow, logList);

        VBox detail = new VBox(8);
        detail.setPadding(new Insets(0, 0, 0, 12));
        logMeta = new Label("\u2190 选择左侧飞行记录查看详情");
        logMeta.getStyleClass().add("section-note");
        logMeta.setWrapText(true);

        HBox logTools = new HBox(8);
        logTools.setAlignment(Pos.CENTER_LEFT);
        Button backupBtn = new Button("备份 CSV");
        backupBtn.getStyleClass().add("btn-soft");
        backupBtn.setOnAction(e -> exportLog());
        Button restoreBtn = new Button("恢复 CSV");
        restoreBtn.getStyleClass().add("btn-soft");
        restoreBtn.setOnAction(e -> restoreLog());
        logTools.getChildren().addAll(backupBtn, restoreBtn);

        logCanvas.setWidth(640);
        logCanvas.setHeight(260);
        logSlider = new Slider(0, 1, 0);
        logSlider.setMaxWidth(Double.MAX_VALUE);
        logSlider.valueProperty().addListener((o, a, b) -> drawLog());
        logReadout = new Label(" ");
        logReadout.getStyleClass().add("mono");
        detail.getChildren().addAll(logTools, logMeta, logCanvas, logSlider, logReadout);
        HBox.setHgrow(detail, Priority.ALWAYS);

        root.setLeft(left);
        root.setCenter(detail);

        refreshSessions();
        recorder.addListener(msg -> javafx.application.Platform.runLater(() -> {
            consoleAdd(msg);
            refreshSessions();
        }));
        return root;
    }

    private void refreshSessions() {
        if (logList == null) {
            return;
        }
        refreshFilterOptions();
        var items = FXCollections.<String>observableArrayList();
        logPaths.clear();
        if (recorder.isRecording()) {
            items.add("\u25cf 记录中\u2026（解锁后自动记录）");
            logPaths.add(null);
        }
        for (Path f : FlightRecorder.sessions()) {
            FlightRecorder.Session s = FlightRecorder.load(f);
            if (!matchFilter(s)) {
                continue;
            }
            logPaths.add(f);
            items.add(s.displayName());
        }
        if (items.isEmpty()) {
            items.add("暂无飞行记录 · 连接飞控后自动记录");
            loadedSession = null;
            logMeta.setText("← 选择左侧飞行记录查看详情");
            logSlider.setMax(1);
            logSlider.setValue(0);
            logReadout.setText(" ");
            drawLog();
        }
        logList.setItems(items);
    }

    private boolean updatingFilters;

    /** 从所有会话构建型号/日期筛选下拉选项（含"全部"）。 */
    private void refreshFilterOptions() {
        if (modelFilter == null || dateFilter == null) {
            return;
        }
        updatingFilters = true;
        java.util.Set<String> models = new java.util.LinkedHashSet<>(List.of("全部"));
        java.util.Set<String> dates = new java.util.LinkedHashSet<>(List.of("全部"));
        for (Path f : FlightRecorder.sessions()) {
            FlightRecorder.Session s = FlightRecorder.load(f);
            models.add(s.model().isBlank() ? "未知" : s.model());
            dates.add(s.dateKey());
        }
        String sm = modelFilter.getValue(), sd = dateFilter.getValue();
        modelFilter.getItems().setAll(models);
        dateFilter.getItems().setAll(dates);
        modelFilter.setValue(models.contains(sm) ? sm : "全部");
        dateFilter.setValue(dates.contains(sd) ? sd : "全部");
        updatingFilters = false;
    }

    private boolean matchFilter(FlightRecorder.Session s) {
        String mv = modelFilter == null ? "全部" : modelFilter.getValue();
        if (mv != null && !mv.equals("全部")
                && !mv.equals(s.model().isBlank() ? "未知" : s.model())) {
            return false;
        }
        String dv = dateFilter == null ? "全部" : dateFilter.getValue();
        return dv == null || dv.equals("全部") || dv.equals(s.dateKey());
    }

    /** 备份：将当前选中的会话导出为本地 CSV（可选保存位置）。 */
    private void exportLog() {
        if (loadedSession == null) {
            Toast.show("请先在左侧选择一条飞行记录", Toast.Type.WARNING);
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("备份飞行日志 → 本地 CSV");
        fc.setInitialFileName(loadedSession.displayName()
                .replaceFirst("(?i)\\.skylog$", "") + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件", "*.csv"));
        File f = fc.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (f == null) {
            return;
        }
        int pct = (int) Math.round(LiveVehicle.get().batteryPct.get());
        java.util.Optional<Path> out = FlightRecorder.exportCsv(loadedSession, pct, f.toPath());
        if (out.isPresent()) {
            Toast.show("已备份 → " + f.getName(), Toast.Type.SUCCESS);
        } else {
            Toast.show("备份失败", Toast.Type.ERROR);
        }
    }

    /** 恢复：从备份的本地 CSV 读取并回放为会话。 */
    private void restoreLog() {
        FileChooser fc = new FileChooser();
        fc.setTitle("从备份 CSV 恢复飞行日志");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件", "*.csv"));
        File f = fc.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (f == null) {
            return;
        }
        FlightRecorder.Session s = FlightRecorder.loadCsv(f.toPath());
        if (s.samples().isEmpty()) {
            Toast.show("CSV 无有效数据或格式不支持", Toast.Type.ERROR);
            return;
        }
        loadedSession = s;
        logMeta.setText("恢复会话 " + f.getName() + "\n"
                + s.count() + " 点 · " + (s.hasGps() ? "含 GPS" : "无 GPS"));
        logSlider.setMax(Math.max(0, s.count() - 1));
        logSlider.setValue(0);
        logSlider.setMaxWidth(Double.MAX_VALUE);
        drawLog();
        Toast.show("已恢复 → " + f.getName() + "（" + s.count() + " 点）", Toast.Type.SUCCESS);
    }

    private void drawLog() {
        var g = logCanvas.getGraphicsContext2D();
        double w = logCanvas.getWidth(), h = logCanvas.getHeight();
        g.setFill(Color.web("#0d1117"));
        g.fillRect(0, 0, w, h);
        FlightRecorder.Session s = loadedSession;
        if (s == null || s.samples().isEmpty()) {
            logReadout.setText(" ");
            return;
        }
        List<double[]> sm = s.samples();
        int n = sm.size();
        int marker = (int) Math.round(logSlider.getValue());
        if (marker >= n) {
            marker = n - 1;
        }

        // ---- defs（v3 会话条件追加环境三曲线） ----
        List<Object[]> defList = new ArrayList<>(List.of(
                new Object[]{1, Color.web("#f0a500"), "横滚°"},
                new Object[]{2, Color.web("#4b8bf5"), "俯仰°"},
                new Object[]{3, Color.web("#22c55e"), "偏航°"},
                new Object[]{5, Color.web("#8b949e"), "M1%"},
                new Object[]{6, Color.web("#a371f7"), "M2%"},
                new Object[]{7, Color.web("#e3b341"), "M3%"},
                new Object[]{8, Color.web("#768390"), "M4%"},
                new Object[]{4, Color.web("#ef4444"), "电压V"}));
        if (s.version() >= 3) {
            defList.add(new Object[]{12, Color.web("#84cc16"), "温度°C"});
            defList.add(new Object[]{13, Color.web("#eab308"), "湿度%"});
            defList.add(new Object[]{14, Color.web("#14b8a6"), "气压hPa"});
        }
        Object[][] defs = defList.toArray(new Object[0][]);

        // ---- 绘制波形 ----
        double pad = 12;
        for (Object[] def : defs) {
            int ci = (Integer) def[0];
            Color col = (Color) def[1];
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (double[] p : sm) {
                if (p[ci] == p[ci]) {
                    min = Math.min(min, p[ci]);
                    max = Math.max(max, p[ci]);
                }
            }
            if (min > max) {
                continue;
            }
            // 近平稳数据防噪声放大：span 不足时以中点对称扩展至最小跨度
            double minSpan = minSpanFor(unitOf((String) def[2]));
            if (max - min < minSpan) {
                double mid = (min + max) / 2;
                min = mid - minSpan / 2;
                max = mid + minSpan / 2;
            }
            double p2 = (max - min) * 0.1 + 0.001;
            min -= p2;
            max += p2;
            g.setStroke(col);
            g.setLineWidth(1.4);
            boolean first = true;
            for (int i = 0; i < n; i++) {
                double v = sm.get(i)[ci];
                if (v != v) {
                    continue;
                }
                double x = pad + (w - 2 * pad) * i / (n - 1.0);
                double y = h - h * (v - min) / (max - min);
                if (first) {
                    g.beginPath();
                    g.moveTo(x, y);
                    first = false;
                } else {
                    g.lineTo(x, y);
                }
            }
            g.stroke();
        }

        // ---- 标记线 ----
        double markerX = pad + (w - 2 * pad) * marker / (n - 1.0);
        g.setStroke(Color.web("#f0a500", 0.5));
        g.setLineWidth(1);
        g.strokeLine(markerX, 0, markerX, h);

        // ---- 通道图例（左上角） ----
        g.setFont(Font.font("Segoe UI", 10));
        double lx = pad + 4, ly = pad + 12;
        for (Object[] def : defs) {
            Color col = (Color) def[1];
            String name = (String) def[2];
            g.setFill(col);
            g.fillRect(lx, ly - 8, 10, 10);
            g.setFill(Color.web("#c9d1d9"));
            g.fillText(name, lx + 14, ly);
            lx += 64;
            if (lx > w - 70) {
                lx = pad + 4;
                ly += 16;
            }
        }

        // ---- 当前数值读数 ----
        double[] mp = sm.get(marker);
        StringBuilder ro = new StringBuilder();
        ro.append("t=").append((long) mp[0]).append("ms");
        String[] labels = {"", "滚", "仰", "偏", "V", "M1", "M2", "M3", "M4"};
        for (int ci = 1; ci <= 8; ci++) {
            double v = mp[ci];
            if (v == v) {
                ro.append("  ").append(labels[ci]).append("=")
                        .append(ci == 4 ? String.format("%.2f", v) : String.format("%.1f", v));
            }
        }
        if (s.hasGps() && mp.length >= 12 && mp[9] == mp[9]) {
            ro.append("  GPS=").append(String.format("%.6f", mp[9]))
                    .append(",").append(String.format("%.6f", mp[10]));
        }
        if (s.version() >= 3 && mp.length >= 15) {
            if (mp[12] == mp[12]) ro.append("  温=").append(String.format("%.1f", mp[12]));
            if (mp[13] == mp[13]) ro.append("  湿=").append(String.format("%.0f", mp[13]));
            if (mp[14] == mp[14]) ro.append("  气=").append(String.format("%.1f", mp[14]));
        }
        logReadout.setText(ro.toString());

        // ---- 飞行统计摘要 ----
        StringBuilder stats = new StringBuilder();
        stats.append("会话 ").append(s.displayName());
        stats.append("\n型号 ").append(s.model().isBlank() ? "未知" : s.model());
        stats.append(" · ").append(s.count()).append(" 点");
        stats.append(" · ").append(s.hasGps() ? "含 GPS" : "无 GPS");
        long durSec = (long) mp[0] / 1000;
        stats.append(" · 时长 ").append(durSec / 60).append("m").append(durSec % 60).append("s");
        // 电压范围
        double vmin = Double.MAX_VALUE, vmax = -Double.MAX_VALUE;
        for (double[] p : sm) {
            if (p[4] == p[4]) {
                vmin = Math.min(vmin, p[4]);
                vmax = Math.max(vmax, p[4]);
            }
        }
        if (vmin <= vmax) {
            stats.append("\n电压 ").append(String.format("%.2f", vmin))
                    .append(" ~ ").append(String.format("%.2f", vmax)).append("V");
        }
        // 姿态范围
        for (int ci = 1; ci <= 3; ci++) {
            double dmin = Double.MAX_VALUE, dmax = -Double.MAX_VALUE;
            for (double[] p : sm) {
                if (p[ci] == p[ci]) {
                    dmin = Math.min(dmin, p[ci]);
                    dmax = Math.max(dmax, p[ci]);
                }
            }
            if (dmin <= dmax) {
                String nm = ci == 1 ? "滚" : ci == 2 ? "仰" : "偏";
                stats.append("  ").append(nm).append(" ")
                        .append(String.format("%.1f", dmin)).append("~")
                        .append(String.format("%.1f", dmax));
            }
        }
        logMeta.setText(stats.toString());
    }

    // ================= 环形缓冲 =================

    private static final class Ring {
        private final double[] vals;
        private final long[] times;
        private int head;
        private int size;

        Ring(int cap) {
            vals = new double[cap];
            times = new long[cap];
        }

        void add(long t, double v) {
            vals[head] = v;
            times[head] = t;
            head = (head + 1) % vals.length;
            size = Math.min(size + 1, vals.length);
        }

        int size() {
            return size;
        }

        double val(int i) {
            return vals[(head - size + i + vals.length) % vals.length];
        }

        long time(int i) {
            return times[(head - size + i + vals.length) % vals.length];
        }

        /** 最接近 t 的采样值；超出容差返回 NaN */
        double at(long t, long tolMs) {
            double best = Double.NaN;
            long bestDt = Long.MAX_VALUE;
            for (int i = 0; i < size; i++) {
                long dt = Math.abs(time(i) - t);
                if (dt < bestDt) {
                    bestDt = dt;
                    best = val(i);
                }
            }
            return bestDt <= tolMs ? best : Double.NaN;
        }

        void clear() {
            head = 0;
            size = 0;
        }
    }
}
