package com.cherglow.gcs.ui.pages;

import com.cherglow.gcs.core.AppState;
import com.cherglow.gcs.core.BatteryPredictor;
import com.cherglow.gcs.core.MissionXml;
import com.cherglow.gcs.core.RoutePlanner;
import com.cherglow.gcs.map.TileServer;
import com.cherglow.gcs.model.LiveVehicle;
import com.cherglow.gcs.model.Waypoint;
import com.cherglow.gcs.tools.TileDownloader;
import com.cherglow.gcs.ui.Toast;
import com.cherglow.gcs.ui.dialog.PredictDialog;
import com.cherglow.gcs.ui.map.MapView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规划页（S8）：全幅地图 + 右侧任务规划面板（对齐截图一）。
 * 面板：任务名 / 航点列表（序号徽章+经纬度+删除）/ 航点数与预计距离（haversine）/
 * 上传到飞控（CF-Drone 无任务协议，降级提示）/ 从飞控读取 / 保存 / 加载（XML）/ 脏标记 / 可折叠。
 * 点击地图加航点，航点可拖拽；数据与地图实时同步。
 */
public class PlanPage extends BasePage {

    @Override
    public String pageId() {
        return "plan";
    }

    private static final Path MISSION_DIR = Path.of(System.getProperty("user.home"), ".skylink", "missions");
    /** B4 冲突检测阈值：相邻航点最小间距（米）。 */
    private static final double MIN_WP_GAP_M = 2.0;
    /** B4 冲突检测阈值：无人机最大航程（公里）。 */
    private static final double MAX_RANGE_KM = 2.0;

    private final MapView map = new MapView();
    private final Map<Integer, Waypoint> waypoints = new LinkedHashMap<>();
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
    private final Label conflictVal = new Label();
    private boolean conflictNotified;

    private boolean routeMode = false;
    private boolean optimized = false;
    private List<Integer> originalOrder = new ArrayList<>();
    private ComboBox<String> modeCombo;
    private TextField payloadField;
    private TextField windSpeedField;
    private ComboBox<String> windDirCombo;
    private Button planBtn;
    private Button restoreBtn;

    public PlanPage() {
        setCenter(map);
        setTop(toolbar());
        setRight(buildPanel());
        map.setCenter(28.6841, 115.8581, 12); // 初始视野：南昌（离线库覆盖区）
        map.setHome(28.6841, 115.8581);

        // S15：绑定 GPS 推流，显示无人机位置
        LiveVehicle lv = LiveVehicle.get();
        lv.gpsFix.addListener((o, a, b) -> refreshDronePosition(lv));
        lv.latDeg.addListener((o, a, b) -> refreshDronePosition(lv));
        lv.lonDeg.addListener((o, a, b) -> refreshDronePosition(lv));
        lv.yawDeg.addListener((o, a, b) -> refreshDronePosition(lv));
        refreshDronePosition(lv);

        map.setOnMapClick(ll -> {
            int id = nextId++;
            waypoints.put(id, new Waypoint(id, ll[0], ll[1]));
            map.addWaypoint(id, ll[0], ll[1]);
            markDirty();
            refreshPanel();
        });
        map.setOnWaypointMoved((id, ll) -> {
            Waypoint wp = waypoints.get(id);
            if (wp != null) {
                wp.setLat(ll[0]);
                wp.setLon(ll[1]);
                markDirty();
                refreshPanel();
            }
        });
    }

    /** S15：更新规划页无人机位置标记 */
    private void refreshDronePosition(LiveVehicle lv) {
        boolean fix = lv.gpsFix.get();
        double lat = lv.latDeg.get(), lon = lv.lonDeg.get();
        if (fix && lat == lat && lon == lon) {
            map.setVehiclePosition(lat, lon, lv.yawDeg.get());
        } else {
            map.setVehiclePosition(null, null, null);
        }
    }

    /** 页面切换到前台时同步 GPS 位置 */
    @Override
    public void onPageShown() {
        refreshDronePosition(LiveVehicle.get());
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
            waypoints.put(id, new Waypoint(id, c[0], c[1]));
            map.addWaypoint(id, c[0], c[1]);
            markDirty();
            refreshPanel();
        });

        modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll("固定顺序", "路径优化");
        modeCombo.setValue("固定顺序");
        modeCombo.getStyleClass().add("dark-combo");
        modeCombo.setPrefWidth(96);
        modeCombo.setOnAction(e -> {
            routeMode = "路径优化".equals(modeCombo.getValue());
            planBtn.setVisible(routeMode);
            restoreBtn.setVisible(routeMode);
            if (!routeMode && optimized) {
                restoreOrder();
            }
        });

        planBtn = softBtn("优化路径");
        planBtn.setVisible(false);
        planBtn.setOnAction(e -> optimizeRoute());

        restoreBtn = softBtn("还原顺序");
        restoreBtn.setVisible(false);
        restoreBtn.setOnAction(e -> restoreOrder());

        Button fitBtn = softBtn("适配视图");
        fitBtn.setOnAction(e -> map.fitWaypoints());

        Button prevBtn = softBtn("预览路径");
        prevBtn.setOnAction(e -> {
            map.playPreview();
            triggerPrediction();
        });

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
            nextId = 1;
            optimized = false;
            originalOrder.clear();
            markDirty();
            refreshPanel();
        });

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label hint = new Label("点击地图添加航点 · 拖动调整位置");
        hint.getStyleClass().add("card-sub");
        bar.getChildren().addAll(baseBtn, addCenter, modeCombo, planBtn, restoreBtn, fitBtn, prevBtn, cacheBtn, clearBtn, sp, hint);
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
        conflictVal.getStyleClass().add("conflict");
        conflictVal.setWrapText(true);
        conflictVal.setVisible(false);

        // ---- 飞行环境参数 ----
        VBox envBox = new VBox(6);
        Label envTitle = new Label("飞行环境");
        envTitle.getStyleClass().add("stat-label");
        payloadField = new TextField("0");
        payloadField.getStyleClass().add("text-field-dark");
        payloadField.setPrefWidth(80);
        payloadField.setPromptText("克");
        Label payloadLabel = new Label("负载(g)");
        payloadLabel.getStyleClass().add("stat-label");
        payloadLabel.setMinWidth(48);
        windSpeedField = new TextField("0");
        windSpeedField.getStyleClass().add("text-field-dark");
        windSpeedField.setPrefWidth(80);
        windSpeedField.setPromptText("m/s");
        Label windLabel = new Label("风速");
        windLabel.getStyleClass().add("stat-label");
        windLabel.setMinWidth(48);
        HBox envRow1 = new HBox(8, payloadLabel, payloadField, windLabel, windSpeedField);
        envRow1.setAlignment(Pos.CENTER_LEFT);

        windDirCombo = new ComboBox<>();
        windDirCombo.getItems().addAll("北", "东北", "东", "东南", "南", "西南", "西", "西北");
        windDirCombo.setValue("北");
        windDirCombo.getStyleClass().add("dark-combo");
        windDirCombo.setPrefWidth(80);
        Label dirLabel = new Label("风向");
        dirLabel.getStyleClass().add("stat-label");
        dirLabel.setMinWidth(48);
        HBox envRow2 = new HBox(8, dirLabel, windDirCombo);
        envRow2.setAlignment(Pos.CENTER_LEFT);
        envBox.getChildren().addAll(envTitle, envRow1, envRow2);

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

        panel.getChildren().addAll(head, nameField, listScroll, stat1, stat2, conflictVal, envBox, upload, read, fileRow);
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
            Waypoint wp = e.getValue();
            VBox item = new VBox(6);
            item.getStyleClass().add("wp-item");
            item.setPadding(new Insets(6, 8, 6, 8));

            HBox top = new HBox(6);
            top.setAlignment(Pos.CENTER_LEFT);
            Label badge = new Label(String.valueOf(wp.getId()));
            badge.getStyleClass().add("wp-badge-sm");
            ComboBox<String> roleBox = new ComboBox<>();
            for (Waypoint.Role r : Waypoint.Role.values()) {
                roleBox.getItems().add(r.label());
            }
            roleBox.setValue(wp.getRole().label());
            roleBox.getStyleClass().add("dark-combo");
            roleBox.setPrefWidth(62);
            Label coord = new Label(String.format("%.5f, %.5f", wp.getLat(), wp.getLon()));
            coord.getStyleClass().add("sensor-val");
            HBox.setHgrow(coord, Priority.ALWAYS);
            Button del = new Button("✕");
            del.getStyleClass().add("del-btn");
            int id = wp.getId();
            del.setOnAction(ev -> {
                waypoints.remove(id);
                map.removeWaypoint(id);
                markDirty();
                refreshPanel();
            });
            top.getChildren().addAll(badge, roleBox, coord, del);

            // 底行：高度 / 停留 / 动作 / 优先级
            HBox attrs = new HBox(6);
            attrs.setAlignment(Pos.CENTER_LEFT);
            TextField alt = field(String.valueOf(wp.getAltM()));
            TextField stay = field(String.valueOf(wp.getStaySec()));
            TextField pri = field(String.valueOf(wp.getPriority()));
            alt.setPrefWidth(56);
            alt.setPromptText("高度m");
            stay.setPrefWidth(50);
            stay.setPromptText("停留s");
            pri.setPrefWidth(44);
            pri.setPromptText("优先");
            ComboBox<String> actionBox = new ComboBox<>();
            for (Waypoint.Action a : Waypoint.Action.values()) {
                actionBox.getItems().add(a.label());
            }
            actionBox.setValue(wp.getAction().label());
            actionBox.getStyleClass().add("dark-combo");
            actionBox.setPrefWidth(76);
            attrs.getChildren().addAll(alt, stay, actionBox, pri);
            item.getChildren().addAll(top, attrs);

            Runnable bindAttr = () -> {
                Double a = parseD(alt.getText());
                if (a != null) {
                    wp.setAltM(a);
                }
                Integer s = parseInt(stay.getText());
                if (s != null) {
                    wp.setStaySec(s);
                }
                Integer p = parseInt(pri.getText());
                if (p != null) {
                    wp.setPriority(p);
                }
                Waypoint.Action act = Waypoint.Action.from(actionBox.getValue());
                if (act != null) {
                    wp.setAction(act);
                }
                markDirty();
                updateStats();
            };
            alt.textProperty().addListener((o, a, b) -> bindAttr.run());
            stay.textProperty().addListener((o, a, b) -> bindAttr.run());
            pri.textProperty().addListener((o, a, b) -> bindAttr.run());
            actionBox.valueProperty().addListener((o, a, b) -> bindAttr.run());
            roleBox.valueProperty().addListener((o, a, b) -> {
                Waypoint.Role newRole = Waypoint.Role.from(roleBox.getValue());
                if (newRole == null) {
                    return;
                }
                if (newRole == Waypoint.Role.START || newRole == Waypoint.Role.END) {
                    for (Waypoint other : waypoints.values()) {
                        if (other.getId() != wp.getId() && other.getRole() == newRole) {
                            other.setRole(Waypoint.Role.WAYPOINT);
                        }
                    }
                }
                wp.setRole(newRole);
                syncWaypointRolesToMap();
                markDirty();
                updateStats();
            });

            wpListBox.getChildren().add(item);
        }
        if (waypoints.isEmpty()) {
            Label empty = new Label("点击地图添加航点");
            empty.getStyleClass().add("section-note");
            wpListBox.getChildren().add(empty);
        }
        syncWaypointRolesToMap();
        updateStats();
        AppState.get().setMissionWaypoints(new LinkedHashMap<>(waypoints));
    }

    private TextField field(String v) {
        TextField t = new TextField(v);
        t.getStyleClass().add("text-field-dark");
        return t;
    }

    private static Double parseD(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void updateStats() {
        countVal.setText(String.valueOf(waypoints.size()));
        AppState.get().missionCountProperty().set(waypoints.size());
        double total = 0;
        Waypoint prev = null;
        for (Waypoint wp : waypoints.values()) {
            if (prev != null) {
                total += prev.distanceTo(wp);
            }
            prev = wp;
        }
        distVal.setText(total >= 1000 ? String.format("%.2f km", total / 1000)
                : String.format("%.0f m", total));

        // B4 冲突检测
        List<String> errs = checkConflicts(total);
        if (errs.isEmpty()) {
            conflictVal.setVisible(false);
            distVal.getStyleClass().remove("conflict");
            conflictNotified = false;
        } else {
            conflictVal.setText(String.join(" · ", errs));
            conflictVal.setVisible(true);
            distVal.getStyleClass().add("conflict");
            if (!conflictNotified) {
                conflictNotified = true;
                Toast.show(String.join(" · ", errs), Toast.Type.WARNING);
            }
        }
    }

    private List<String> checkConflicts(double totalM) {
        List<String> errs = new ArrayList<>();
        List<Waypoint> list = new ArrayList<>(waypoints.values());
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                if (list.get(i).distanceTo(list.get(j)) < MIN_WP_GAP_M) {
                    errs.add("#" + list.get(i).getId() + "-#" + list.get(j).getId()
                            + " 距离过近( <" + ((int) MIN_WP_GAP_M) + "m)");
                }
            }
        }
        if (totalM / 1000.0 > MAX_RANGE_KM) {
            errs.add(String.format("预计航程 %.2f km 超出最大 %.0f km", totalM / 1000.0, MAX_RANGE_KM));
        }
        return errs;
    }

    // ================= 路径优化 =================

    private void syncWaypointRolesToMap() {
        map.clearWaypointRoles();
        for (Waypoint wp : waypoints.values()) {
            if (wp.getRole() != Waypoint.Role.WAYPOINT) {
                map.setWaypointRole(wp.getId(), wp.getRole().name());
            }
        }
    }

    private void optimizeRoute() {
        if (waypoints.size() < 2) {
            Toast.show("至少需要 2 个航点才能优化路径", Toast.Type.WARNING);
            return;
        }

        List<Waypoint> all = new ArrayList<>(waypoints.values());

        Waypoint start = null;
        Waypoint end = null;
        List<Waypoint> intermediates = new ArrayList<>();
        for (Waypoint wp : all) {
            if (wp.getRole() == Waypoint.Role.START) {
                start = wp;
            } else if (wp.getRole() == Waypoint.Role.END) {
                end = wp;
            } else {
                intermediates.add(wp);
            }
        }
        boolean autoStart = (start == null);
        boolean autoEnd = (end == null);
        if (start == null) {
            start = all.get(0);
            intermediates.remove(start);
        }
        if (end == null) {
            end = all.get(all.size() - 1);
            intermediates.remove(end);
        }

        if (intermediates.isEmpty()) {
            Toast.show("无中间航点可优化（起点→终点直连）", Toast.Type.INFO);
            return;
        }

        originalOrder = new ArrayList<>();
        for (Waypoint wp : waypoints.values()) {
            originalOrder.add(wp.getId());
        }

        RoutePlanner.RouteResult result = RoutePlanner.planGreedy(start, end, intermediates);

        List<Integer> orderedIds = new ArrayList<>();
        for (Waypoint wp : result.getOrderedWaypoints()) {
            orderedIds.add(wp.getId());
        }

        boolean changed = false;
        List<Integer> currentOrder = new ArrayList<>(waypoints.keySet());
        if (orderedIds.size() != currentOrder.size()) {
            changed = true;
        } else {
            for (int i = 0; i < orderedIds.size(); i++) {
                if (!orderedIds.get(i).equals(currentOrder.get(i))) {
                    changed = true;
                    break;
                }
            }
        }

        Map<Integer, Waypoint> backup = new LinkedHashMap<>(waypoints);
        waypoints.clear();
        for (int id : orderedIds) {
            Waypoint wp = backup.get(id);
            if (wp != null) {
                waypoints.put(id, wp);
            }
        }
        map.reorderWaypoints(orderedIds);

        optimized = true;
        distVal.setText(result.getTotalDistanceMeters() >= 1000
                ? String.format("%.2f km", result.getTotalDistanceMeters() / 1000)
                : String.format("%.0f m", result.getTotalDistanceMeters()));
        markDirty();
        refreshPanel();
        String startInfo = autoStart ? "首航点" : "起点";
        String endInfo = autoEnd ? "末航点" : "终点";
        if (changed) {
            Toast.show(String.format("路径优化完成 · %s→中间%d点→%s · 总距离 %.0f m",
                    startInfo, intermediates.size(), endInfo, result.getTotalDistanceMeters()),
                    Toast.Type.SUCCESS);
        } else {
            Toast.show(String.format("当前已是最优顺序 · %s→中间%d点→%s · 总距离 %.0f m",
                    startInfo, intermediates.size(), endInfo, result.getTotalDistanceMeters()),
                    Toast.Type.INFO);
        }
    }

    private void restoreOrder() {
        if (originalOrder.isEmpty()) {
            Toast.show("无原始顺序可还原", Toast.Type.INFO);
            return;
        }
        Map<Integer, Waypoint> backup = new LinkedHashMap<>(waypoints);
        waypoints.clear();
        for (int id : originalOrder) {
            Waypoint wp = backup.get(id);
            if (wp != null) {
                waypoints.put(id, wp);
            }
        }
        map.reorderWaypoints(originalOrder);
        optimized = false;
        originalOrder.clear();
        markDirty();
        refreshPanel();
        Toast.show("已还原到原始顺序", Toast.Type.INFO);
    }

    // ================= 保存 / 加载（XML） =================

    private void saveMission() {
        FileChooser fc = new FileChooser();
        fc.setTitle("保存任务");
        if (Files.isDirectory(MISSION_DIR)) {
            fc.setInitialDirectory(MISSION_DIR.toFile());
        }
        fc.setInitialFileName(nameField.getText() + ".xml");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("任务文件", "*.xml"));
        File f = fc.showSaveDialog(getWindow());
        if (f == null) {
            return;
        }
        try {
            MissionXml.save(f.toPath(), nameField.getText(), waypoints.values());
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
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("任务文件", "*.xml"));
        File f = fc.showOpenDialog(getWindow());
        if (f == null) {
            return;
        }
        try {
            MissionXml.MissionFile mission = MissionXml.load(f.toPath());
            nameField.setText(mission.name());
            waypoints.clear();
            map.clearWaypoints();
            nextId = 1;
            for (MissionXml.MissionWaypoint entry : mission.waypoints()) {
                Waypoint wp = new Waypoint(entry.id(), entry.lat(), entry.lon());
                wp.setAltM(entry.altM());
                wp.setStaySec(entry.staySec());
                wp.setAction(entry.action());
                wp.setPriority(entry.priority());
                wp.setRole(entry.role());
                waypoints.put(wp.getId(), wp);
                map.addWaypoint(wp.getId(), wp.getLat(), wp.getLon());
                nextId = Math.max(nextId, wp.getId() + 1);
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

    // ================= 电池电压预测 =================

    private void triggerPrediction() {
        if (waypoints.size() < 2) {
            Toast.show("至少需要 2 个航点才能预测", Toast.Type.WARNING);
            return;
        }
        Double payload = parseD(payloadField.getText());
        if (payload == null) payload = 0.0;
        Double windSpeed = parseD(windSpeedField.getText());
        if (windSpeed == null) windSpeed = 0.0;
        int windDir = windDirToDegrees(windDirCombo.getValue());

        List<Waypoint> wps = new ArrayList<>(waypoints.values());
        BatteryPredictor.PredictionResult result =
                BatteryPredictor.predict(wps, payload, windSpeed, windDir);
        if (result == null) {
            Toast.show("预测失败：航点数据不足", Toast.Type.WARNING);
            return;
        }
        new PredictDialog(result).show();
    }

    private static int windDirToDegrees(String dir) {
        if (dir == null) return 0;
        return switch (dir) {
            case "北" -> 0;
            case "东北" -> 45;
            case "东" -> 90;
            case "东南" -> 135;
            case "南" -> 180;
            case "西南" -> 225;
            case "西" -> 270;
            case "西北" -> 315;
            default -> 0;
        };
    }
}
