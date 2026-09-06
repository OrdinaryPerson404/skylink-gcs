package com.cherglow.gcs.ui.dialog;

import com.cherglow.gcs.core.AppState;
import com.cherglow.gcs.core.ConnectionService;
import com.cherglow.gcs.serial.SerialTransport;
import com.cherglow.gcs.ui.Icons;
import com.cherglow.gcs.ui.ThemeManager;
import com.cherglow.gcs.ui.Toast;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;

/**
 * 连接飞控设备对话框（对齐截图：深色圆角卡片 + 双 tab + 右下角操作按钮）。
 * 串口/USB 为主通道（CF-Drone CLI 链路）；WebSocket / UDP 为预留通道（S13 启用）。
 */
public class ConnectDialog {

    private final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private final StackPane contentStack = new StackPane();
    private final Button serialTab = tabButton("串口 / USB");
    private final Button wsTab = tabButton("WebSocket");
    private final Button udpTab = tabButton("UDP（预留）");
    private final ComboBox<String> portCombo = new ComboBox<>();
    private final ComboBox<String> baudCombo = new ComboBox<>();
    private final Button connectBtn = primaryButton("连接");
    private final Button disconnectBtn = dangerButton("断开连接");
    private final Label statusLabel = new Label();

    public ConnectDialog() {
        stage.initModality(Modality.NONE);

        VBox card = new VBox(14);
        card.getStyleClass().add("dialog-card");
        card.setPrefWidth(560);

        // ---- 标题行 ----
        Label title = new Label("连接飞控设备");
        title.getStyleClass().add("dialog-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("dialog-close");
        closeBtn.setOnAction(e -> stage.hide());
        HBox header = new HBox(8, title, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // ---- Tabs ----
        serialTab.getStyleClass().add("active");
        serialTab.setOnAction(e -> switchTab("serial"));
        wsTab.setOnAction(e -> switchTab("ws"));
        udpTab.setOnAction(e -> switchTab("udp"));
        HBox tabs = new HBox(4, serialTab, wsTab, udpTab);

        // ---- 串口面板 ----
        VBox serialPanel = new VBox(12);
        serialPanel.setPadding(new Insets(4, 0, 0, 0));

        HBox portRow = new HBox(10);
        portRow.setAlignment(Pos.CENTER_LEFT);
        Label portLabel = new Label("端口");
        portLabel.getStyleClass().add("field-label");
        portCombo.getStyleClass().addAll("text-field-dark", "dark-combo");
        portCombo.setPrefWidth(240);
        portCombo.setMaxWidth(240);
        Button refreshBtn = softButton("刷新");
        refreshBtn.setOnAction(e -> refreshPorts());
        portRow.getChildren().addAll(portLabel, portCombo, refreshBtn);

        HBox baudRow = new HBox(10);
        baudRow.setAlignment(Pos.CENTER_LEFT);
        Label baudLabel = new Label("波特率");
        baudLabel.getStyleClass().add("field-label");
        baudCombo.getStyleClass().addAll("text-field-dark", "dark-combo");
        baudCombo.getItems().addAll("9600", "57600", "115200 (常用)", "230400", "460800");
        baudCombo.getSelectionModel().select(2);
        baudCombo.setPrefWidth(240);
        baudCombo.setMaxWidth(240);
        baudRow.getChildren().addAll(baudLabel, baudCombo);

        Label note = new Label("通过 USB 数据线连接飞控（CF-Drone CLI 链路），适用于调试和真机联调场景。");
        note.getStyleClass().add("section-note");
        note.setWrapText(true);

        HBox statusRow = new HBox(8, statusLabel);
        statusLabel.getStyleClass().add("section-note");
        bindStatus();

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        connectBtn.setOnAction(e -> {
            String port = portCombo.getValue();
            if (port == null || port.isBlank()) {
                Toast.show("请先选择串口端口", Toast.Type.WARNING);
                return;
            }
            int baud = Integer.parseInt(baudCombo.getValue().replaceAll("[^0-9]", ""));
            ConnectionService.get().connectSerial(port, baud);
        });
        disconnectBtn.setOnAction(e -> ConnectionService.get().disconnect());
        btnRow.getChildren().addAll(disconnectBtn, connectBtn);

        serialPanel.getChildren().addAll(portRow, baudRow, note, statusRow, btnRow);

        // ---- WebSocket 面板（预留） ----
        VBox wsPanel = reservedPanel(
                "192.168.4.1", "8765",
                "通过 WiFi 直连飞控 WebSocket 服务。飞控需支持 WebSocket MAVLink 输出，或经 UDP 转 WebSocket 桥接器实现连接。当前固件版本请使用串口直连，该通道将在 S13 启用。");

        // ---- UDP 面板（预留） ----
        VBox udpPanel = reservedPanel(
                "192.168.4.1", "14550",
                "加入无人机 AP（Drone_WiFi）后经 MAVLink UDP 直连，可获得 10Hz 高频遥测。该通道将在 S13 启用。");

        contentStack.getChildren().addAll(serialPanel, wsPanel, udpPanel);
        contentStack.setAlignment(Pos.TOP_LEFT);
        switchTab("serial");

        card.getChildren().addAll(header, tabs, contentStack);

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root, 600, 380, Color.TRANSPARENT);
        ThemeManager.apply(scene); // 跟随当前主题
        stage.setScene(scene);
        refreshPorts();
    }

    public void show() {
        if (!stage.isShowing()) {
            stage.show();
            stage.centerOnScreen();
        }
    }

    private void switchTab(String tab) {
        mark(serialTab, tab.equals("serial"));
        mark(wsTab, tab.equals("ws"));
        mark(udpTab, tab.equals("udp"));
        contentStack.getChildren().get(0).setVisible(tab.equals("serial"));
        contentStack.getChildren().get(0).setManaged(tab.equals("serial"));
        contentStack.getChildren().get(1).setVisible(tab.equals("ws"));
        contentStack.getChildren().get(1).setManaged(tab.equals("ws"));
        contentStack.getChildren().get(2).setVisible(tab.equals("udp"));
        contentStack.getChildren().get(2).setManaged(tab.equals("udp"));
    }

    private static void mark(Button b, boolean active) {
        b.getStyleClass().remove("active");
        if (active) {
            b.getStyleClass().add("active");
        }
    }

    private void refreshPorts() {
        try {
            List<String> ports = SerialTransport.listPorts();
            portCombo.getItems().setAll(ports);
            if (!ports.isEmpty() && portCombo.getValue() == null) {
                portCombo.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            Toast.show("枚举串口失败：" + e.getMessage(), Toast.Type.ERROR);
        }
    }

    private void bindStatus() {
        Runnable apply = () -> {
            var s = AppState.get().getConnStatus();
            String detail = AppState.get().connDetailProperty().get();
            String text = switch (s) {
                case CONNECTED -> "● 已连接 · " + detail;
                case CONNECTING -> "● 连接中…";
                case ERROR -> "● 连接失败 · " + detail;
                default -> "● 未连接";
            };
            statusLabel.setText(text);
        };
        AppState.get().connStatusProperty().addListener((o, a, b) -> apply.run());
        AppState.get().connDetailProperty().addListener((o, a, b) -> apply.run());
        apply.run();

        var busy = Bindings.createBooleanBinding(
                () -> AppState.get().getConnStatus() == AppState.ConnStatus.CONNECTING
                        || AppState.get().getConnStatus() == AppState.ConnStatus.CONNECTED,
                AppState.get().connStatusProperty());
        connectBtn.disableProperty().bind(busy);
        disconnectBtn.disableProperty().bind(busy.not());
    }

    private VBox reservedPanel(String ip, String port, String noteText) {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(4, 0, 0, 0));

        HBox ipRow = new HBox(10);
        ipRow.setAlignment(Pos.CENTER_LEFT);
        Label ipLabel = new Label("IP 地址");
        ipLabel.getStyleClass().add("field-label");
        TextField ipField = new TextField(ip);
        ipField.getStyleClass().add("text-field-dark");
        ipField.setPrefWidth(240);
        ipRow.getChildren().addAll(ipLabel, ipField);

        HBox portRow = new HBox(10);
        portRow.setAlignment(Pos.CENTER_LEFT);
        Label portLabel = new Label("端口");
        portLabel.getStyleClass().add("field-label");
        TextField portField = new TextField(port);
        portField.getStyleClass().add("text-field-dark");
        portField.setPrefWidth(160);
        portRow.getChildren().addAll(portLabel, portField);

        Label note = new Label(noteText);
        note.getStyleClass().add("section-note");
        note.setWrapText(true);

        Label reservedBadge = new Label("预留通道 · S13 启用");
        reservedBadge.getStyleClass().add("reserved-badge");

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button d = dangerButton("断开连接");
        Button r = primaryButton("重新连接");
        d.setDisable(true);
        r.setOnAction(e -> Toast.show("预留通道：请在 S13 启用后使用", Toast.Type.INFO));
        btnRow.getChildren().addAll(d, r);

        panel.getChildren().addAll(ipRow, portRow, note, reservedBadge, btnRow);
        return panel;
    }

    // ---- 小部件工厂 ----

    private static Button tabButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("dlg-tab");
        return b;
    }

    private static Button primaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-primary");
        return b;
    }

    private static Button dangerButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-danger");
        return b;
    }

    private static Button softButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-soft");
        return b;
    }
}
