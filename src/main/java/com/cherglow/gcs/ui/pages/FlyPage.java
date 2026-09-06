package com.cherglow.gcs.ui.pages;

import com.cherglow.gcs.core.AppState;
import com.cherglow.gcs.core.ConnectionService;
import com.cherglow.gcs.model.LiveVehicle;
import com.cherglow.gcs.ui.ConfirmDialog;
import com.cherglow.gcs.ui.Toast;
import com.cherglow.gcs.ui.map.MapView;
import com.cherglow.gcs.ui.widget.AdiWidget;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
 * 飞行页（S7）：全幅地图 + 叠加层。
 * 右上 HUD 数据卡（HDG/ALT/GS/VS/ROLL/PITCH + 模式徽章 + WP）；
 * 左下迷你 ADI 浮窗（可关闭）；顶部任务横幅（与规划页脏标记联动）；
 * 底部：系统消息条 + 指令条（解锁/上锁 + ACRO/STAB/ALTHOLD/POSHOLD）+ 状态栏。
 * 指令经 CLI 链路下发真机：解锁需二次确认，禁止解锁时置灰并透传原因。
 */
public class FlyPage extends BorderPane {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final MapView map = new MapView();
    private final LiveVehicle lv = LiveVehicle.get();
    private final ObservableList<String> messages = FXCollections.observableArrayList();

    private final StackPane mapPane = new StackPane();
    private final Label banner = new Label("⚠ 任务已修改但未上传 · 点击前往规划页");
    private VBox adiFloat;
    private Button adiShowBtn;

    // HUD
    private final Label hudHdg = hudVal("—");
    private final Label hudAlt = hudVal("—");
    private final Label hudGs = hudVal("—");
    private final Label hudVs = hudVal("—");
    private final Label hudRoll = hudVal("—");
    private final Label hudPitch = hudVal("—");
    private final Label modeBadge = hudVal("—");
    private final Label armedBadge = hudVal("—");

    // 底部
    private final ObservableList<String> recent = messages;
    private final ListView<String> msgList = new ListView<>(recent);
    private final Label msgCount = new Label("0");
    private final Label msgLast = new Label("系统就绪");
    private final Button armBtn = new Button("解锁");
    private final Map<String, Button> modeButtons = new LinkedHashMap<>();

    private Runnable goToPlan;

    public FlyPage() {
        getStyleClass().add("page-root");
        setCenter(buildCenter());
        setBottom(buildBottom());

        map.setCenter(28.6841, 115.8581, 13);
        map.setHome(28.6841, 115.8581);
        bindLive();
        addMsg("系统就绪，等待连接飞控");
    }

    /** 注册「前往规划页」跳转（由 MainShell 注入） */
    public void setGoToPlan(Runnable r) {
        this.goToPlan = r;
    }

    // ================= 布局 =================

    private StackPane buildCenter() {
        // HUD 卡（右上）——锁定首选尺寸，防止 StackPane 拉伸
        VBox hud = new VBox(6);
        hud.getStyleClass().addAll("card", "fly-hud");
        hud.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        hud.getChildren().addAll(
                hudRow("航向", hudHdg), hudRow("高度", hudAlt),
                hudRow("地速", hudGs), hudRow("爬升", hudVs),
                hudRow("横滚", hudRoll), hudRow("俯仰", hudPitch));
        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER);
        Label mTitle = new Label("模式");
        mTitle.getStyleClass().add("stat-label");
        modeBadge.getStyleClass().addAll("badge-mode");
        Label aTitle = new Label("解锁");
        aTitle.getStyleClass().add("stat-label");
        armedBadge.getStyleClass().addAll("badge-mode");
        badges.getChildren().addAll(mTitle, modeBadge, aTitle, armedBadge);
        hud.getChildren().add(badges);

        // 迷你 ADI 浮窗（左下，可关闭）——锁定尺寸防止被 StackPane 撑满全屏
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

        // 重新显示按钮（默认隐藏）
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

        // 任务横幅（顶部居中）
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
        Label l = new Label(v);
        return l;
    }

    private VBox buildBottom() {
        VBox bottom = new VBox(0);

        // ---- 系统消息条 ----
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
        HBox.setHgrow(bar, Priority.ALWAYS);
        msgBar.getChildren().add(bar);
        msgList.getStyleClass().add("msg-list");
        msgList.setMaxHeight(140);
        msgList.setVisible(false);
        msgList.setManaged(false);
        VBox msgBlock = new VBox(msgBar, msgList);

        // ---- 指令条 ----
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
        Button althold = modeBtn("ALTHOLD");
        althold.setText("定高 ALTHOLD");
        Button poshold = modeBtn("POSHOLD");
        poshold.setText("定点 POSHOLD");
        modeButtons.put("ACRO", acro);
        modeButtons.put("STAB", stab);
        modeButtons.put("ALTHOLD", althold);
        modeButtons.put("POSHOLD", poshold);

        Label hint = new Label("指令需连接飞控 · Ctrl+A 解锁");
        hint.getStyleClass().add("stat-label");
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        cmdBar.getChildren().addAll(armBtn, sep(), acro, stab, althold, poshold, sp2, hint);

        bottom.getChildren().addAll(msgBlock, cmdBar);
        return bottom;
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

    /** 固件模式代码 → 中文名 */
    private static String modeCn(String code) {
        if (code == null || code.isEmpty()) {
            return "—";
        }
        return switch (code.toUpperCase()) {
            case "RAW" -> "手动";
            case "ACRO" -> "特技";
            case "STAB" -> "自稳";
            case "ALTHOLD" -> "定高";
            case "POSHOLD" -> "定点";
            default -> code;
        };
    }

    // ================= 指令 =================

    /** 解锁/上锁（Ctrl+A 入口同此） */
    public void confirmArm() {
        if (!ConnectionService.get().isConnected()) {
            Toast.show("未连接飞控，无法发送指令", Toast.Type.WARNING);
            return;
        }
        if (lv.armed.get()) {
            ConfirmDialog.show("上锁确认", "确认上锁？电机将立即停转。", () -> send("disarm", "上锁指令"));
        } else {
            ConfirmDialog.show("解锁确认",
                    "确认解锁电机？\n⚠ 请务必拆除桨叶，并远离人群与障碍物！", () -> send("arm", "解锁指令"));
        }
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

    private void bindLive() {
        // HUD
        lv.yawDeg.addListener((o, a, b) -> hudHdg.setText(String.format("%03.0f°", b.doubleValue())));
        lv.altitude.addListener((o, a, b) -> hudAlt.setText(
                b.doubleValue() == b.doubleValue() ? String.format("%.1f m", b.doubleValue()) : "—"));
        hudGs.setText("—");
        hudVs.setText("—");
        lv.rollDeg.addListener((o, a, b) -> hudRoll.setText(String.format("%.1f°", b.doubleValue())));
        lv.pitchDeg.addListener((o, a, b) -> hudPitch.setText(String.format("%.1f°", b.doubleValue())));

        lv.modeName.addListener((o, a, b) -> {
            modeBadge.setText(modeCn(b));
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
            armedBadge.setText(b ? "已解锁" : "未解锁");
        });

        // 禁止解锁 → 置灰并透传原因
        lv.armingDisabled.addListener((o, a, b) -> {
            boolean blocked = b != null && !b.isEmpty();
            armBtn.setDisable(blocked && !lv.armed.get());
            if (blocked) {
                addMsg("安全提示：禁止解锁 — " + b);
            }
        });

        // 连接事件 → 消息
        AppState.get().connStatusProperty().addListener((o, a, b) -> {
            switch (b) {
                case CONNECTED -> addMsg("飞控已连接");
                case CONNECTING -> addMsg("正在连接飞控…");
                case ERROR -> addMsg("连接失败");
                default -> addMsg("已断开连接");
            }
        });
    }
}
