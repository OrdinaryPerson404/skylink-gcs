package com.cherglow.gcs.ui.pages;

import com.cherglow.gcs.core.AppState;
import com.cherglow.gcs.map.TileServer;
import com.cherglow.gcs.tools.TileDownloader;
import com.cherglow.gcs.ui.Toast;
import com.cherglow.gcs.ui.map.MapView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规划页（S8）：全幅地图 + 右侧任务规划面板（对齐截图一）。
 * 面板：任务名 / 航点列表（序号徽章+经纬度+删除）/ 航点数与预计距离（haversine）/
 * 上传到飞控（CF-Drone 无任务协议，降级提示）/ 从飞控读取 / 保存 / 加载（JSON）/ 脏标记 / 可折叠。
 * 点击地图加航点，航点可拖拽；数据与地图实时同步。
 */
public class PlanPage extends BorderPane {

    private static final Path MISSION_DIR = Path.of(System.getProperty("user.home"), ".skylink", "missions");

    private final MapView map = new MapView();
    private final Map<Integer, double[]> waypoints = new LinkedHashMap<>();
    private int nextId = 1;
    private boolean dirty;
    private boolean expanded = true;

    private VBox panel;
    private VBox wpListBox;
    private TextField nameField;
    private Label dirtyDot;
    private Button collapseBtn;
    private final Label countVal = new Label("0");
    private final Label distVal = new Label("0.00 km");

    public PlanPage() {
        getStyleClass().add("page-root");
        setCenter(map);
        setTop(toolbar());
        setRight(buildPanel());
        map.setCenter(28.6841, 115.8581, 12); // 初始视野：南昌（离线库覆盖区）
        map.setHome(28.6841, 115.8581);

        map.setOnMapClick(ll -> {
            int id = nextId++;
            waypoints.put(id, ll);
            map.addWaypoint(id, ll[0], ll[1]);
            markDirty();
            refreshPanel();
        });
        map.setOnWaypointMoved((id, ll) -> {
            waypoints.put(id, ll);
            markDirty();
            refreshPanel();
        });
    }

    // ================= 工具行（地图上方） =================

    private HBox toolbar() {
        HBox bar = new HBox(8);
        bar.getStyleClass().add("map-toolbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Button baseBtn = softBtn("卫星底图");
        baseBtn.setOnAction(e -> map.toggleBase(name ->
                baseBtn.setText("sat".equals(name) ? "矢量底图" : "卫星底图")));

        Button addCenter = softBtn("中心加航点");
        addCenter.setOnAction(e -> {
            double[] c = map.getMapCenter();
            int id = nextId++;
            waypoints.put(id, c);
            map.addWaypoint(id, c[0], c[1]);
            markDirty();
            refreshPanel();
        });

        Button fitBtn = softBtn("适配视图");
        fitBtn.setOnAction(e -> map.fitWaypoints());

        Button cacheBtn = softBtn("缓存当前视野");
        cacheBtn.setOnAction(e -> {
            double[] vb = map.getViewportBBox();
            int z0 = Math.max(3, (int) vb[4] - 1);
            int z1 = Math.min(15, (int) vb[4] + 1);
            Toast.show("开始缓存当前视野瓦片（z" + z0 + "-" + z1 + "）…", Toast.Type.INFO);
            new Thread(() -> {
                TileDownloader.downloadRegion(java.util.List.of("vec", "sat"),
                        vb[0], vb[2], vb[1], vb[3], z0, z1,
                        Path.of(System.getProperty("user.home"), ".skylink", "tiles"),
                        (done, total) -> { });
                javafx.application.Platform.runLater(() ->
                        Toast.show("视野缓存完成（z" + z0 + "-" + z1 + "）", Toast.Type.SUCCESS));
            }, "viewport-cache").start();
        });

        Button clearBtn = softBtn("清空");
        clearBtn.setOnAction(e -> {
            if (waypoints.isEmpty()) {
                return;
            }
            waypoints.clear();
            map.clearWaypoints();
            markDirty();
            refreshPanel();
        });

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label hint = new Label("点击地图添加航点 · 拖动调整位置");
        hint.getStyleClass().add("card-sub");
        bar.getChildren().addAll(baseBtn, addCenter, fitBtn, cacheBtn, clearBtn, sp, hint);
        return bar;
    }

    // ================= 任务规划面板 =================

    private VBox buildPanel() {
        panel = new VBox(10);
        panel.getStyleClass().add("plan-panel");
        panel.setPrefWidth(300);
        panel.setMinWidth(300);
        panel.setPadding(new Insets(14));

        HBox head = new HBox(8);
        head.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("任务规划");
        title.getStyleClass().add("plan-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        dirtyDot = new Label("●");
        dirtyDot.getStyleClass().add("dirty-dot");
        dirtyDot.setVisible(false);
        collapseBtn = new Button("◀");
        collapseBtn.getStyleClass().add("dialog-close");
        collapseBtn.setOnAction(e -> togglePanel());
        head.getChildren().addAll(title, sp, dirtyDot, collapseBtn);

        nameField = new TextField("未命名任务");
        nameField.getStyleClass().add("text-field-dark");
        nameField.textProperty().addListener((o, a, b) -> markDirty());

        wpListBox = new VBox(4);
        ScrollPane listScroll = new ScrollPane(wpListBox);
        listScroll.getStyleClass().add("wp-scroll");
        listScroll.setFitToWidth(true);
        listScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        HBox stat1 = statRow("航点数", countVal);
        HBox stat2 = statRow("预计距离", distVal);

        Button upload = new Button("↑ 上传到飞控");
        upload.getStyleClass().add("btn-primary");
        upload.setMaxWidth(Double.MAX_VALUE);
        upload.setOnAction(e -> Toast.show(
                "该机型（CF-Drone）固件不支持机载任务，上传功能将在支持任务协议的机型上启用",
                Toast.Type.WARNING));

        Button read = new Button("↓ 从飞控读取");
        read.getStyleClass().add("btn-soft");
        read.setMaxWidth(Double.MAX_VALUE);
        read.setOnAction(e -> Toast.show(
                "该机型固件无机载任务可读取（协议桩恒返回空任务）", Toast.Type.INFO));

        Button save = new Button("保存");
        save.getStyleClass().add("btn-soft");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(e -> saveMission());

        Button load = new Button("加载");
        load.getStyleClass().add("btn-soft");
        load.setMaxWidth(Double.MAX_VALUE);
        load.setOnAction(e -> loadMission());
        HBox fileRow = new HBox(8, save, load);
        for (var b : fileRow.getChildren()) {
            HBox.setHgrow(b, Priority.ALWAYS);
        }

        panel.getChildren().addAll(head, nameField, listScroll, stat1, stat2, upload, read, fileRow);
        refreshPanel();
        return panel;
    }

    private HBox statRow(String name, Label val) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(name);
        l.getStyleClass().add("stat-label");
        l.setMinWidth(64);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        val.getStyleClass().addAll("status-val", "mono");
        row.getChildren().addAll(l, sp, val);
        return row;
    }

    private void togglePanel() {
        expanded = !expanded;
        panel.setVisible(expanded);
        panel.setManaged(expanded);
        collapseBtn.setText(expanded ? "◀" : "▶");
    }

    private void markDirty() {
        if (!dirty) {
            dirty = true;
            dirtyDot.setVisible(true);
        }
        AppState.get().missionDirtyProperty().set(true);
    }

    // ================= 列表与统计 =================

    private void refreshPanel() {
        wpListBox.getChildren().clear();
        for (var e : waypoints.entrySet()) {
            double[] p = e.getValue();
            HBox item = new HBox(8);
            item.getStyleClass().add("wp-item");
            item.setAlignment(Pos.CENTER_LEFT);

            Label badge = new Label(String.valueOf(e.getKey()));
            badge.getStyleClass().add("wp-badge-sm");

            Label coord = new Label(String.format("%.5f, %.5f", p[0], p[1]));
            coord.getStyleClass().add("sensor-val");
            HBox.setHgrow(coord, Priority.ALWAYS);

            Button del = new Button("✕");
            del.getStyleClass().add("del-btn");
            int id = e.getKey();
            del.setOnAction(ev -> {
                waypoints.remove(id);
                map.removeWaypoint(id);
                markDirty();
                refreshPanel();
            });

            item.getChildren().addAll(badge, coord, del);
            wpListBox.getChildren().add(item);
        }
        if (waypoints.isEmpty()) {
            Label empty = new Label("点击地图添加航点");
            empty.getStyleClass().add("section-note");
            wpListBox.getChildren().add(empty);
        }
        updateStats();
    }

    private void updateStats() {
        countVal.setText(String.valueOf(waypoints.size()));
        AppState.get().missionCountProperty().set(waypoints.size());
        double total = 0;
        double[] prev = null;
        for (double[] p : waypoints.values()) {
            if (prev != null) {
                total += haversineM(prev[0], prev[1], p[0], p[1]);
            }
            prev = p;
        }
        distVal.setText(total >= 1000 ? String.format("%.2f km", total / 1000)
                : String.format("%.0f m", total));
    }

    private static double haversineM(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.asin(Math.sqrt(h));
    }

    // ================= 保存 / 加载（JSON） =================

    private void saveMission() {
        FileChooser fc = new FileChooser();
        fc.setTitle("保存任务");
        if (Files.isDirectory(MISSION_DIR)) {
            fc.setInitialDirectory(MISSION_DIR.toFile());
        }
        fc.setInitialFileName(nameField.getText() + ".json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("任务文件", "*.json"));
        File f = fc.showSaveDialog(getWindow());
        if (f == null) {
            return;
        }
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"name\": \"")
                .append(nameField.getText().replace("\\", "\\\\").replace("\"", "\\\""))
                .append("\",\n  \"waypoints\": [");
        boolean first = true;
        for (double[] p : waypoints.values()) {
            if (!first) {
                json.append(",");
            }
            json.append("\n    [").append(String.format("%.6f", p[0])).append(", ")
                    .append(String.format("%.6f", p[1])).append("]");
            first = false;
        }
        json.append("\n  ]\n}");
        try {
            Files.createDirectories(f.getParentFile().toPath());
            Files.writeString(f.toPath(), json.toString(), StandardCharsets.UTF_8);
            dirty = false;
            dirtyDot.setVisible(false);
            AppState.get().missionDirtyProperty().set(false);
            Toast.show("任务已保存：" + f.getName(), Toast.Type.SUCCESS);
        } catch (IOException e) {
            Toast.show("保存失败：" + e.getMessage(), Toast.Type.ERROR);
        }
    }

    private void loadMission() {
        FileChooser fc = new FileChooser();
        fc.setTitle("加载任务");
        if (Files.isDirectory(MISSION_DIR)) {
            fc.setInitialDirectory(MISSION_DIR.toFile());
        }
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("任务文件", "*.json"));
        File f = fc.showOpenDialog(getWindow());
        if (f == null) {
            return;
        }
        try {
            String json = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            Matcher nameM = Pattern.compile("\"name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
            if (nameM.find()) {
                nameField.setText(nameM.group(1).replace("\\\"", "\"").replace("\\\\", "\\"));
            }
            waypoints.clear();
            map.clearWaypoints();
            nextId = 1;
            Matcher wp = Pattern.compile("\\[\\s*(-?[\\d.]+)\\s*,\\s*(-?[\\d.]+)\\s*\\]").matcher(json);
            while (wp.find()) {
                int id = nextId++;
                double[] p = new double[]{Double.parseDouble(wp.group(1)),
                        Double.parseDouble(wp.group(2))};
                waypoints.put(id, p);
                map.addWaypoint(id, p[0], p[1]);
            }
            markDirty();
            refreshPanel();
            map.fitWaypoints();
            Toast.show("已加载 " + waypoints.size() + " 个航点", Toast.Type.SUCCESS);
        } catch (IOException | RuntimeException e) {
            Toast.show("加载失败：" + e.getMessage(), Toast.Type.ERROR);
        }
    }

    private javafx.stage.Window getWindow() {
        return panel.getScene() == null ? null : panel.getScene().getWindow();
    }

    private Button softBtn(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-soft");
        return b;
    }
}
