package com.cherglow.gcs.ui.pages;

import com.cherglow.gcs.core.AppState;
import com.cherglow.gcs.core.ConnectionService;
import com.cherglow.gcs.core.RangeEstimator;
import com.cherglow.gcs.model.LiveVehicle;
import com.cherglow.gcs.model.Waypoint;
import com.cherglow.gcs.ui.ConfirmDialog;
import com.cherglow.gcs.ui.Toast;
import com.cherglow.gcs.ui.map.MapView;
import com.cherglow.gcs.ui.widget.AdiWidget;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 飞行页（三区域布局）：左侧控制面板（航点列表 + 任务状态）
 * + 中央地图核心区（滚轮缩放/左键平移 + HUD 叠加 + ADI 浮窗 + 任务横幅）
 * + 底部状态栏（连接状态 + 信号强度 + 系统时间）+ 指令条 + 消息条。
 */
public class FlyPage extends BasePage {

    @Override
    public String pageId() {
        return "fly";
    }

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final MapView map = new MapView();
    private final LiveVehicle lv = LiveVehicle.get();
    private final ObservableList<String> messages = FXCollections.observableArrayList();

    private final StackPane mapPane = new StackPane();
    private final Label banner = new Label("⚠ 任务已修改但未上传 · 点击前往规划页");
    private VBox adiFloat;
    private Button adiShowBtn;

    // HUD（精简 6 项）
    private final Label hudHdg = hudVal("—");
    private final Label hudAlt = hudVal("—");
    private final Label hudGs = hudVal("—");
    private final Label hudVs = hudVal("—");
    private final Label hudRoll = hudVal("—");
    private final Label hudPitch = hudVal("—");

    // 左侧面板 — 任务状态
    private final Label wpCountLabel = new Label("0");
    private final Label modeLabel = new Label("—");
    private final Label armedLabel = new Label("未解锁");
    private final Label battVoltLabel = new Label("—");
    private final Label estTimeLabel = new Label("—");
    private final Label estRangeLabel = new Label("—");
    private final Label lowBattFlag = new Label("⚠ 电量低，建议降落");
    private static final double LOW_BATT_VOLT = 3.5;
    private final Label armingDisabledLabel = new Label("");
    private VBox wpListBox;

    // 底部状态栏
    private final Label connDot = new Label();
    private final Label connDescLabel = new Label("未连接");
    private final HBox signalBars = new HBox(2);
    private final Label gnssDot = new Label("GNSS");
    private long lastGnssToastMs;
    private static final long GNSS_TOAST_COOLDOWN_MS = 3000;
    private final Label clockLabel = new Label("--:--:--");
    private final Timeline clock = new Timeline(new KeyFrame(
            javafx.util.Duration.seconds(1), e -> {
        clockLabel.setText(LocalTime.now().format(HM));
        refreshSignal();
    }));

    // 底部
    private final ObservableList<String> recent = messages;
    private final ListView<String> msgList = new ListView<>(recent);
    private final Label msgCount = new Label("0");
    private final Label msgLast = new Label("系统就绪");
    private final Button armBtn = new Button("解锁");
    private final Map<String, Button> modeButtons = new LinkedHashMap<>();

    private Runnable goToPlan;

    public FlyPage() {
        setLeft(buildLeftPanel());
        setCenter(buildCenter());
        setBottom(buildBottom());

        map.setCenter(28.6841, 115.8581, 13);
        map.setHome(28.6841, 115.8581);
        map.setWaypointDragEnabled(false); // 飞行页航点仅展示：避免误抓航点徽章导致地图无法平移
        bindLive();
        addMsg("系统就绪，等待连接飞控");

        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        AppState.get().missionWaypointsProperty().addListener((o, a, b) -> refreshWpList());
    }

    public void setGoToPlan(Runnable r) {
        this.goToPlan = r;
    }

    @Override
    public void onPageShown() {
        refreshWpList();
        refreshGps(); // 页面切换时刷新 GPS 标记
        // 不调用 fitWaypoints()，保持当前地图视图（避免 zoom 计算异常）
    }

    // ================= 左侧控制面板 =================

    private VBox buildLeftPanel() {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("fly-left-panel");

        // ---- 航点列表区 ----
        Label wpTitle = new Label("航点列表");
        wpTitle.getStyleClass().add("plan-title");
        HBox wpHead = new HBox(8);
        wpHead.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        wpCountLabel.getStyleClass().addAll("status-val", "mono");
        wpHead.getChildren().addAll(wpTitle, sp, wpCountLabel);

        wpListBox = new VBox(4);
        ScrollPane wpScroll = new ScrollPane(wpListBox);
        wpScroll.getStyleClass().add("wp-scroll");
        wpScroll.setFitToWidth(true);
        wpScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(wpScroll, Priority.ALWAYS);

        HBox wpTools = new HBox(6);
        Button addBtn = new Button("添加航点");
        addBtn.getStyleClass().add("btn-soft");
        addBtn.setOnAction(e -> {
            double[] c = map.getMapCenter();
            int nextId = 1;
            while (AppState.get().getMissionWaypoints().containsKey(nextId)) nextId++;
            Waypoint wp = new Waypoint(nextId, c[0], c[1]);
            Map<Integer, Waypoint> wps = new LinkedHashMap<>(AppState.get().getMissionWaypoints());
            wps.put(nextId, wp);
            AppState.get().setMissionWaypoints(wps);
            addMsg("航点 " + nextId + " 已添加（请在规划页编辑属性）");
        });
        Button clearBtn = new Button("清空");
        clearBtn.getStyleClass().add("btn-soft");
        clearBtn.setOnAction(e -> {
            AppState.get().setMissionWaypoints(new LinkedHashMap<>());
            addMsg("航点已清空");
        });
        wpTools.getChildren().addAll(addBtn, clearBtn);

        // ---- 任务状态区 ----
        Label statusTitle = new Label("任务状态");
        statusTitle.getStyleClass().add("plan-title");
        VBox statusBox = new VBox(6);
        statusBox.getStyleClass().add("fly-status-row");
        statusBox.getChildren().addAll(
                statusRow("模式", modeLabel),
                statusRow("解锁", armedLabel),
                statusRow("电压", battVoltLabel),
                statusRow("剩余航时", estTimeLabel),
                statusRow("剩余航程", estRangeLabel));

        lowBattFlag.getStyleClass().add("low-batt-blink");
        lowBattFlag.setAlignment(Pos.CENTER);
        lowBattFlag.setVisible(false);
        lowBattFlag.setManaged(false);

        armingDisabledLabel.getStyleClass().add("msg-last");
        armingDisabledLabel.setWrapText(true);
        armingDisabledLabel.setVisible(false);
        armingDisabledLabel.setManaged(false);

        panel.getChildren().addAll(wpHead, wpScroll, wpTools,
                statusTitle, statusBox, lowBattFlag, armingDisabledLabel);
        refreshWpList();
        return panel;
    }

    private HBox statusRow(String name, Label val) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(name);
        l.getStyleClass().add("hud-k");
        l.setMinWidth(64);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        val.getStyleClass().addAll("hud-v", "mono");
        row.getChildren().addAll(l, sp, val);
        return row;
    }

    private void refreshWpList() {
        if (wpListBox == null) {
            return;
        }
        wpListBox.getChildren().clear();
        Map<Integer, Waypoint> wps = AppState.get().getMissionWaypoints();
        wpCountLabel.setText(String.valueOf(wps.size()));

        map.clearWaypoints();
        for (var e : wps.entrySet()) {
            Waypoint wp = e.getValue();
            map.addWaypoint(wp.getId(), wp.getLat(), wp.getLon());
            if (wp.getRole() != Waypoint.Role.WAYPOINT) {
                map.setWaypointRole(wp.getId(), wp.getRole().name());
            }

            HBox item = new HBox(6);
            item.getStyleClass().add("fly-wp-item");
            item.setAlignment(Pos.CENTER_LEFT);
            String badgeText = wp.getRole() == Waypoint.Role.START ? "S"
                    : wp.getRole() == Waypoint.Role.END ? "E"
                    : String.valueOf(wp.getId());
            Label badge = new Label(badgeText);
            badge.getStyleClass().add("wp-badge-sm");
            Label coord = new Label(String.format("%.5f, %.5f", wp.getLat(), wp.getLon()));
            coord.getStyleClass().add("sensor-val");
            HBox.setHgrow(coord, Priority.ALWAYS);
            item.getChildren().addAll(badge, coord);
            wpListBox.getChildren().add(item);
        }
        if (wps.isEmpty()) {
            Label empty = new Label("暂无航点 · 在规划页添加");
            empty.getStyleClass().add("section-note");
            wpListBox.getChildren().add(empty);
        }
    }

    // ================= 中央地图区 =================

    private StackPane buildCenter() {
        VBox hud = new VBox(6);
        hud.getStyleClass().addAll("card", "fly-hud");
        hud.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        hud.setMouseTransparent(true); // HUD 纯显示，不拦截鼠标事件
        hud.getChildren().addAll(
                hudRow("航向", hudHdg), hudRow("高度", hudAlt),
                hudRow("地速", hudGs), hudRow("爬升", hudVs),
                hudRow("横滚", hudRoll), hudRow("俯仰", hudPitch));

        adiFloat = new VBox(0);
        adiFloat.getStyleClass().add("card");
        adiFloat.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        HBox adiHead = new HBox(8);
        adiHead.getStyleClass().add("card-head");
        adiHead.setAlignment(Pos.CENTER_LEFT);
        Label adiTitle = new Label("ADI");
        adiTitle.getStyleClass().add("card-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button close = new Button("✕");
        close.getStyleClass().add("dialog-close");
        close.setOnAction(e -> {
            adiFloat.setVisible(false);
            adiFloat.setManaged(false);
            adiShowBtn.setVisible(true);
            adiShowBtn.setManaged(true);
        });
        adiHead.getChildren().addAll(adiTitle, sp, close);
        AdiWidget adi = new AdiWidget();
        adi.setPrefSize(160, 150);
        adi.setMaxSize(160, 150);
        adiFloat.getChildren().addAll(adiHead, adi);

        adiShowBtn = new Button("ADI");
        adiShowBtn.getStyleClass().add("btn-soft");
        adiShowBtn.setVisible(false);
        adiShowBtn.setManaged(false);
        adiShowBtn.setOnAction(e -> {
            adiFloat.setVisible(true);
            adiFloat.setManaged(true);
            adiShowBtn.setVisible(false);
            adiShowBtn.setManaged(false);
        });

        HBox bannerBox = new HBox(banner);
        banner.getStyleClass().add("fly-banner");
        bannerBox.setAlignment(Pos.TOP_CENTER);
        bannerBox.setPadding(new Insets(10, 0, 0, 0));
        banner.setOnMouseClicked(e -> {
            if (goToPlan != null) {
                goToPlan.run();
            }
        });
        banner.setMouseTransparent(false);
        bannerBox.visibleProperty().bind(AppState.get().missionDirtyProperty());
        bannerBox.managedProperty().bind(AppState.get().missionDirtyProperty());

        mapPane.getChildren().addAll(map, hud, adiFloat, adiShowBtn, bannerBox);
        StackPane.setAlignment(hud, Pos.TOP_RIGHT);
        StackPane.setMargin(hud, new Insets(12));
        StackPane.setAlignment(adiFloat, Pos.BOTTOM_LEFT);
        StackPane.setMargin(adiFloat, new Insets(12));
        StackPane.setAlignment(adiShowBtn, Pos.BOTTOM_LEFT);
        StackPane.setMargin(adiShowBtn, new Insets(12));
        StackPane.setAlignment(bannerBox, Pos.TOP_CENTER);
        return mapPane;
    }

    private HBox hudRow(String name, Label val) {
        HBox row = new HBox(10);
        Label l = new Label(name);
        l.getStyleClass().add("hud-k");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        val.getStyleClass().addAll("hud-v", "mono");
        row.getChildren().addAll(l, sp, val);
        return row;
    }

    private static Label hudVal(String v) {
        return new Label(v);
    }

    // ================= 底部：消息条 + 指令条 + 状态栏 =================

    private VBox buildBottom() {
        VBox bottom = new VBox(0);

        HBox msgBar = new HBox(8);
        msgBar.getStyleClass().add("msg-bar");
        msgBar.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("◉ 系统消息");
        icon.getStyleClass().add("stat-label");
        msgCount.getStyleClass().add("msg-count");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        msgLast.getStyleClass().add("msg-last");
        msgLast.setMaxWidth(600);
        HBox.setHgrow(msgLast, Priority.ALWAYS);
        msgLast.setAlignment(Pos.CENTER_RIGHT);
        HBox bar = new HBox(8, icon, msgCount, sp, msgLast);
        HBox.setHgrow(bar, Priority.ALWAYS);
        msgBar.getChildren().add(bar);
        msgList.getStyleClass().add("msg-list");
        msgList.setMaxHeight(140);
        msgList.setVisible(false);
        msgList.setManaged(false);
        VBox msgBlock = new VBox(msgBar, msgList);

        HBox cmdBar = new HBox(8);
        cmdBar.getStyleClass().add("cmd-bar");
        cmdBar.setAlignment(Pos.CENTER);
        armBtn.getStyleClass().addAll("btn-danger", "cmd-btn");
        armBtn.setText("解锁");
        armBtn.setOnAction(e -> confirmArm());
        Button acro = modeBtn("ACRO");
        acro.setText("特技 ACRO");
        Button stab = modeBtn("STAB");
        stab.setText("自稳 STAB");
        stab.getStyleClass().add("active");
        modeButtons.put("ACRO", acro);
        modeButtons.put("STAB", stab);
        Label hint = new Label("指令需连接飞控 · Ctrl+A 解锁");
        hint.getStyleClass().add("stat-label");
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        cmdBar.getChildren().addAll(armBtn, sep(), acro, stab, sp2, hint);

        // ---- 状态栏 ----
        HBox statusBar = new HBox(12);
        statusBar.getStyleClass().add("fly-status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        connDot.getStyleClass().add("fly-conn-dot");
        connDot.getStyleClass().add("offline");
        connDescLabel.getStyleClass().add("msg-last");
        signalBars.getStyleClass().add("fly-signal-bar");
        buildSignalBars();
        gnssDot.getStyleClass().add("fly-gnss-dot");
        gnssDot.getStyleClass().add("offline");
        Region sp3 = new Region();
        HBox.setHgrow(sp3, Priority.ALWAYS);
        clockLabel.getStyleClass().add("fly-clock");
        statusBar.getChildren().addAll(connDot, connDescLabel, signalBars, gnssDot, sp3, clockLabel);

        bottom.getChildren().addAll(msgBlock, cmdBar, statusBar);
        return bottom;
    }

    private void buildSignalBars() {
        signalBars.getChildren().clear();
        for (int i = 0; i < 3; i++) {
            Region bar = new Region();
            bar.getStyleClass().add("bar");
            bar.setPrefSize(4, 4 + i * 4);
            signalBars.getChildren().add(bar);
        }
    }

    private void refreshSignal() {
        boolean connected = ConnectionService.get().isConnected();
        connDot.getStyleClass().removeAll("online", "offline");
        connDot.getStyleClass().add(connected ? "online" : "offline");
        String desc = ConnectionService.get().getDesc();
        connDescLabel.setText(connected && !desc.isEmpty() ? desc : (connected ? "已连接" : "未连接"));

        long lastUpd = lv.lastUpdateMs.get();
        long age = lastUpd > 0 ? System.currentTimeMillis() - lastUpd : Long.MAX_VALUE;
        signalBars.getChildren().clear();
        int strength = age < 1000 ? 3 : age < 3000 ? 2 : age < 5000 ? 1 : 0;
        for (int i = 0; i < 3; i++) {
            Region bar = new Region();
            bar.getStyleClass().add("bar");
            bar.setPrefSize(4, 4 + i * 4);
            if (i < strength) {
                bar.getStyleClass().add(strength >= 3 ? "on" : "weak");
            } else if (!connected) {
                bar.getStyleClass().add("off");
            }
            signalBars.getChildren().add(bar);
        }
    }

    private Region sep() {
        Region r = new Region();
        r.getStyleClass().add("cmd-sep");
        return r;
    }

    private Button modeBtn(String mode) {
        Button b = new Button(mode);
        b.getStyleClass().addAll("btn-soft", "cmd-btn");
        b.setOnAction(e -> {
            if (!ConnectionService.get().isConnected()) {
                Toast.show("未连接飞控，无法切换模式", Toast.Type.WARNING);
                return;
            }
            ConnectionService.get().sendCommand(mode.toLowerCase(), out ->
                    addMsg("模式切换 → " + modeCn(mode) + (out.isEmpty() ? " ✓" : " · " + out)));
        });
        return b;
    }

    private static String modeCn(String code) {
        if (code == null || code.isEmpty()) {
            return "—";
        }
        return switch (code.toUpperCase()) {
            case "RAW" -> "RAW";
            case "ACRO" -> "特技";
            case "STAB" -> "自稳";
            case "ALTHOLD" -> "定高";
            case "POSHOLD" -> "定点";
            default -> code;
        };
    }

    // ================= 指令 =================

    public void confirmArm() {
        if (!ConnectionService.get().isConnected()) {
            Toast.show("未连接飞控，无法发送指令", Toast.Type.WARNING);
            return;
        }
        String disabled = lv.armingDisabled.get();
        if (!lv.armed.get() && disabled != null && !disabled.isEmpty()) {
            Toast.show("禁止解锁 — " + disabled, Toast.Type.WARNING);
            return;
        }
        if (lv.armed.get()) {
            ConfirmDialog.show("上锁确认", "确认上锁？电机将立即停转。", () -> send("disarm", "上锁指令"));
        } else {
            ConfirmDialog.show("解锁确认",
                    "确认解锁电机？\n⚠ 请务必拆除桨叶，并远离人群与障碍物！", () -> send("arm", "解锁指令"));
        }
    }

    private void refreshArmBlocked() {
        String reason = lv.armingDisabled.get();
        boolean blocked = !lv.armed.get() && reason != null && !reason.isEmpty();
        armBtn.setOpacity(blocked ? 0.55 : 1.0);
        armingDisabledLabel.setVisible(blocked);
        armingDisabledLabel.setManaged(blocked);
        armingDisabledLabel.setText(blocked ? "⚠ 禁止解锁：" + reason : "");
    }

    private void send(String cmd, String label) {
        ConnectionService.get().sendCommand(cmd, out ->
                addMsg(label + (out.isEmpty() ? " → 已发送" : " → " + out)));
    }

    // ================= 消息 =================

    private void addMsg(String text) {
        javafx.application.Platform.runLater(() -> {
            recent.add(0, "[" + LocalTime.now().format(HM) + "] " + text);
            while (recent.size() > 50) {
                recent.remove(recent.size() - 1);
            }
            msgCount.setText(String.valueOf(recent.size()));
            msgLast.setText(text);
        });
    }

    // ================= 数据绑定 =================

    private void refreshGps() {
        boolean fix = lv.gpsFix.get();
        double lat = lv.latDeg.get(), lon = lv.lonDeg.get();
        if (fix && lat == lat && lon == lon) {
            map.setVehiclePosition(lat, lon, lv.yawDeg.get());
        } else {
            map.setVehiclePosition(null, null, null);
        }
    }

    /** S17：更新 HUD 地速显示（GNSS 在线且速度有效时显示，否则 "—"） */
    private void refreshGpsHud() {
        boolean online = lv.gnssOnline.get();
        double spd = lv.spdMps.get();
        if (online && spd == spd) {
            hudGs.setText(String.format("%.1f m/s", spd));
        } else {
            hudGs.setText("—");
        }
    }

    /** S17：更新状态栏 GNSS 指示点样式（在线/离线） */
    private void refreshGnssStatus() {
        boolean online = lv.gnssOnline.get();
        gnssDot.getStyleClass().removeAll("online", "offline");
        gnssDot.getStyleClass().add(online ? "online" : "offline");
    }

    /** S17：GNSS 在线状态变化时弹 Toast（带 3s 防抖，避免频繁切换刷屏） */
    private void showGnssToast(boolean online) {
        long now = System.currentTimeMillis();
        if (now - lastGnssToastMs < GNSS_TOAST_COOLDOWN_MS) {
            return;
        }
        lastGnssToastMs = now;
        if (online) {
            Toast.show("GNSS 推流已恢复", Toast.Type.INFO);
        } else {
            // 只有曾经有过 fix 才弹"中断"（首次启动本来就是离线，不弹）
            if (lv.gpsFix.get()) {
                Toast.show("GNSS 推流中断", Toast.Type.WARNING);
            }
        }
    }

    private void refreshRange() {
        double volt = lv.batteryVoltage.get();
        battVoltLabel.setText(volt == volt ? String.format("%.2f V", volt) : "—");
        double pct = lv.batteryPct.get();
        if (pct != pct) {
            estTimeLabel.setText("—");
            estRangeLabel.setText("—");
            updateLowBattFlag();
            return;
        }
        int ipct = Math.max(0, Math.min(100, (int) Math.round(pct)));
        estTimeLabel.setText(String.format("%.1f min", RangeEstimator.remainingMinutes(ipct)));
        double spd = lv.spdMps.get();
        double dist = spd == spd ? RangeEstimator.remainingDistanceMeters(ipct, spd) : 0;
        estRangeLabel.setText(dist > 0 ? String.format("%.0f m", dist) : "—");
        updateLowBattFlag();
    }

    private void updateLowBattFlag() {
        double volt = lv.batteryVoltage.get();
        boolean low = volt == volt && volt < LOW_BATT_VOLT;
        lowBattFlag.setVisible(low);
        lowBattFlag.setManaged(low);
    }

    private void bindLive() {
        lv.yawDeg.addListener((o, a, b) -> hudHdg.setText(String.format("%03.0f°", b.doubleValue())));
        lv.altitude.addListener((o, a, b) -> hudAlt.setText(
                b.doubleValue() == b.doubleValue() ? String.format("%.1f m", b.doubleValue()) : "—"));
        refreshGpsHud();
        hudVs.setText("—");
        lv.rollDeg.addListener((o, a, b) -> hudRoll.setText(String.format("%.1f°", b.doubleValue())));
        lv.pitchDeg.addListener((o, a, b) -> hudPitch.setText(String.format("%.1f°", b.doubleValue())));

        lv.gpsFix.addListener((o, a, b) -> {
            // 首次定位时，自动更新 Home 点到无人机位置（避免 Home 点与当前位置分离）
            if (b && !a) {
                double lat = lv.latDeg.get();
                double lon = lv.lonDeg.get();
                if (lat == lat && lon == lon) {
                    map.setHome(lat, lon);
                }
            }
            refreshGps();
        });
        lv.latDeg.addListener((o, a, b) -> refreshGps());
        lv.lonDeg.addListener((o, a, b) -> refreshGps());
        lv.gnssOnline.addListener((o, a, b) -> {
            refreshGps();
            refreshGnssStatus();
            showGnssToast(b);
        });
        lv.spdMps.addListener((o, a, b) -> refreshGpsHud());
        refreshGps();
        refreshGnssStatus();

        lv.armed.addListener((o, a, b) -> refreshRange());
        lv.batteryPct.addListener((o, a, b) -> refreshRange());
        lv.batteryVoltage.addListener((o, a, b) -> refreshRange());
        lv.spdMps.addListener((o, a, b) -> refreshRange());
        refreshRange();

        lv.modeName.addListener((o, a, b) -> {
            modeLabel.setText(modeCn(b));
            for (var e : modeButtons.entrySet()) {
                e.getValue().getStyleClass().remove("active");
                if (e.getKey().equalsIgnoreCase(b)) {
                    e.getValue().getStyleClass().add("active");
                }
            }
        });

        lv.armed.addListener((o, a, b) -> {
            armBtn.setText(b ? "上锁" : "解锁");
            armBtn.getStyleClass().removeAll("btn-danger", "btn-primary");
            armBtn.getStyleClass().add(b ? "btn-primary" : "btn-danger");
            armedLabel.setText(b ? "已解锁" : "未解锁");
            refreshArmBlocked();
        });

        lv.armingDisabled.addListener((o, a, b) -> {
            if (b != null && !b.isEmpty()) {
                addMsg("安全提示：禁止解锁 — " + b);
            }
            refreshArmBlocked();
        });
        refreshArmBlocked();

        AppState.get().connStatusProperty().addListener((o, a, b) -> {
            switch (b) {
                case CONNECTED -> addMsg("飞控已连接");
                case CONNECTING -> addMsg("正在连接飞控…");
                case ERROR -> addMsg("连接失败");
                default -> addMsg("已断开连接");
            }
            refreshSignal();
        });
    }
}
