package cn.edu.nuaa.gcs.ui;

import cn.edu.nuaa.gcs.app.Main;
import cn.edu.nuaa.gcs.comm.*;
import cn.edu.nuaa.gcs.data.*;
import cn.edu.nuaa.gcs.model.*;
import cn.edu.nuaa.gcs.planner.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MainController {
    @FXML private Label brandLabel;
    @FXML private ToggleGroup modeGroup;
    @FXML private ToggleButton e1Btn, phoneBtn;
    @FXML private Region connDot;
    @FXML private Label connDetail;
    @FXML private Label clockLabel;
    @FXML private TabPane workspaceTabs;
    @FXML private Tab missionTab, dataTab, settingsTab, algoTab;
    @FXML private VBox leftPanel;
    @FXML private SplitPane mainSplit;

    // Mission workspace - left panel
    @FXML private VBox e1StatusBox;
    @FXML private VBox phoneStatusBox;
    @FXML private Label e1ConnBanner, phoneConnBanner;
    @FXML private Label e1IpVal, e1PortVal, e1LatencyVal, e1ArmedVal, e1ThrottleVal, e1CtrlSrcVal, e1HeartbeatVal, e1LinkQualityVal;
    @FXML private Label phoneGpsVal, phoneGpsAccVal, phonePosVal, phoneCompassVal, phoneBaroVal, phoneLastUpdateVal;
    @FXML private GridPane droneStatusGrid;
    @FXML private ListView<Waypoint> wpListView;
    @FXML private Label uploadStatusBadge;
    @FXML private Label wpCountLabel;
    @FXML private Button uploadE1Btn, autoMissionBtn;
    @FXML private Label totalDistLabel, estTimeLabel, conflictLabel;
    @FXML private Label airspaceStatusLabel;
    @FXML private VBox ctrlPanel;
    @FXML private Button armBtn, disarmBtn;
    @FXML private Label ctrlStatusLabel;
    @FXML private HBox safeBanner;
    @FXML private Button btnEnterCtrl, btnExitCtrl;
    private boolean controlTestActive = false;

    // Attitude + battery detail + telemetry record + WiFi banner (P6/P5/P3)
    @FXML private VBox attitudeBox;
    @FXML private StackPane attitudeHost;
    @FXML private Label attitudeUnavailableLabel;
    @FXML private ToggleButton btnRecordTelemetry;
    @FXML private Label recordingStatusLabel;
    @FXML private Label networkBannerLabel;
    private AttitudeIndicator attitudeIndicator;
    private cn.edu.nuaa.gcs.util.NetworkMonitor networkMonitor;
    private cn.edu.nuaa.gcs.data.TelemetryRecorder telemetryRecorder;
    private final java.util.concurrent.atomic.AtomicBoolean recordingOn = new java.util.concurrent.atomic.AtomicBoolean(false);
    /** 电池详情 Label（initDroneStatusGrid 中构建）。 */
    private Label lblBatCurrent, lblBatTemp, lblCells, lblBatTimeLeft;

    // ================================================================
    // UI 节流机制（避免高频遥测帧淹没 JavaFX EDT）
    // ================================================================
    /** 最新待处理遥测帧（listen 线程写，EDT 读，volatile 保证可见性）。 */
    private volatile MavlinkParser.Telemetry pendingTelemetry;
    /** UI 是否有积压数据需要刷新。 */
    private volatile boolean uiDirty = false;
    /** UI 刷新 Timeline，10fps（100ms），合并多帧遥测为一次 UI 更新。 */
    private Timeline uiRefreshTimeline;
    /** 上次地图 Canvas 重绘时间戳（ns），用于 Canvas 节流。 */
    private volatile long lastMapDrawNs = 0;
    /** Canvas 地图重绘最小间隔（ns），100ms = 10fps。 */
    private static final long MAP_THROTTLE_NS = 100_000_000L;

    // Mission workspace - map
    @FXML private AnchorPane mapContainer;
    @FXML private ToggleButton btnFollow, btnTrajectory;
    @FXML private Button btnDataSource;
    @FXML private ToggleButton btnMapStyle, btnAirspace;
    @FXML private Label mapHint;
    @FXML private Label seTemp, seHum, sePres;
    @FXML private Label hudAlt, hudSpeed, hudHdg, hudBat;
    @FXML private VBox dsDrawer;
    @FXML private Label dsE1Conn, dsE1Hb, dsE1Loss, dsPhoneGps, dsPhoneAcc;
    @FXML private HBox statusBar;
    @FXML private Label sbPort, sbBaud, sbSample, sbLastUpdate, sbLat, sbLon, sbZoom, sbWp, sbGps, sbHdop, sbMouse;

    // Data workspace
    @FXML private LineChart<Number, Number> sensorChart;
    @FXML private ToggleGroup dataTabGroup;
    @FXML private ToggleGroup timeRangeGroup;
    @FXML private Label waveStat;
    @FXML private VBox waveSidebar;
    @FXML private Button btnWavePause;
    @FXML private VBox waveformContent;
    @FXML private VBox flightLogContent;
    @FXML private ListView<cn.edu.nuaa.gcs.model.FlightLog> flListView;
    @FXML private Label flCountLabel;
    @FXML private VBox flDetailEmpty;
    @FXML private VBox flDetailContent;
    @FXML private Label flDetailId, flDetailMeta;
    @FXML private Label flStatPoints, flStatAlt, flStatSpeed, flStatVolt, flStatDist;
    @FXML private Canvas flMapCanvas;
    @FXML private StackPane flMapContainer;
    @FXML private TextField flSearchField;
    @FXML private ComboBox<String> flModelCombo;

    // Settings
    @FXML private ComboBox<String> setRunMode;
    @FXML private TextField e1IpField, e1PortField, hbTimeoutField;
    @FXML private ToggleButton phoneGpsToggle, phoneCompassToggle, phoneBaroToggle;
    @FXML private ComboBox<String> serialPortCombo;
    @FXML private ComboBox<Integer> baudRateCombo;
    @FXML private Slider sampleIntervalSlider;
    @FXML private ComboBox<Integer> mapZoomCombo;
    @FXML private ListView<String> aspacePkgList;
    @FXML private ComboBox<String> droneModelCombo;
    @FXML private TextField droneRangeField;
    @FXML private ComboBox<String> aspaceSrcType;

    // Algorithm
    @FXML private Label algoOrigDist, algoOptDist, algoWpCount, algoDelta, algoRate;
    @FXML private TextField distInput, loadInput, windInput;
    @FXML private Label enduranceResult, costResult;
    @FXML private HBox algoFlow;
    @FXML private Label windInfoLabel;

    // Battery charging workspace (Tab 5)
    @FXML private Tab chargingTab;
    @FXML private Region bbbDot;
    @FXML private Label bbbText, bbbMeta;
    @FXML private Label chgSumCharging, chgSumChargingSub, chgSumFull, chgSumFullSub,
            chgSumPower, chgSumPowerSub, chgSumAvgCap, chgSumAvgSub, chgSumEta, chgSumEtaSub;
    @FXML private VBox chgValidationBox, chgValBody;
    @FXML private Label chgValStatus, chgValChecked, chgValAnomalies, chgValErrors, chgValWarnings, chgValSources;
    @FXML private VBox chgGridHost;

    // Log table (kept for compatibility)
    @FXML private TableView<LogRecord> logTableView;
    @FXML private DatePicker logDatePicker;
    @FXML private ComboBox<String> logModelCombo;
    @FXML private ComboBox<String> timeRangeCombo;

    private Stage stage;
    private AppSettings settings;
    private final Drone drone = new Drone();
    private final Mission mission = new Mission();
    private final AirspaceService airspace = new AirspaceService();
    private final BatteryPredictor batteryPredictor = new BatteryPredictor();
    private final CommunicationService comm = new CommunicationService();
    private final ParamService paramService = new ParamService();
    private final BatteryChargingMonitor chargingMonitor = new BatteryChargingMonitor(drone, comm);
    private boolean paramListRequested = false;
    private String lastStatusText = "";
    private long lastStatusTextMs = 0;
    private long lastLowBatMs = 0;
    private MapCanvas mapCanvas;
    private AMapWebView amapView;
    private AMapWebView flAmapView;
    private SensorChart sensor;
    private boolean missionDirty = false;

    // Flight logs
    private final List<cn.edu.nuaa.gcs.model.FlightLog> allFlightLogs = new ArrayList<>();
    private cn.edu.nuaa.gcs.model.FlightLog selectedFlight = null;

    // Toast stack
    private VBox toastStack;

    public void init(Stage stage, AppSettings settings) {
        this.stage = stage;
        this.settings = settings;
        Scene scene = stage.getScene();
        initToastStack();
        initMap();
        initSensorChart();
        initLogTable();
        initFlightLog();
        initSettings();
        initDroneStatusGrid();
        initAttitudeAndNetMon();
        initWaypoints();
        applyModeGating();
        startClock();
        updateWpCount();
        // 启动电池充电实时监控（WMI 系统电池 + MAVLink 无人机电池）
        chargingMonitor.start();
        // Load demo airspace on startup
        onLoadDemoAirspace();
        // Initialize algorithm tab with mission data
        updateAlgoStats();
        // Setup keyboard shortcuts
        setupKeyboardShortcuts(scene);
        // Setup responsive design
        setupResponsiveLayout(stage);
    }

    private void setupResponsiveLayout(Stage stage) {
        // Set minimum window size
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        // Listen for window resize
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            double width = newVal.doubleValue();
            applyResponsiveWidth(width);
        });

        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            double height = newVal.doubleValue();
            applyResponsiveHeight(height);
        });
    }

    private void applyResponsiveWidth(double width) {
        // Toggle compact mode for small windows
        if (root != null) {
            ObservableList<String> rootStyles = root.getStyleClass();
            if (width < 1100) {
                if (!rootStyles.contains("compact-mode")) {
                    rootStyles.add("compact-mode");
                }
            } else {
                rootStyles.remove("compact-mode");
            }
        }

        // Adjust left panel width based on window size
        if (leftPanel != null) {
            if (width < 1100) {
                // Narrow: compact left panel
                leftPanel.setPrefWidth(240);
                leftPanel.setMinWidth(220);
            } else if (width < 1400) {
                // Medium: standard left panel
                leftPanel.setPrefWidth(280);
                leftPanel.setMinWidth(260);
            } else {
                // Wide: wider left panel
                leftPanel.setPrefWidth(320);
                leftPanel.setMinWidth(280);
            }
        }

        // Adjust split pane divider position
        if (mainSplit != null) {
            double ratio;
            if (width < 1100) {
                ratio = 0.27; // More space for map on narrow windows
            } else if (width < 1400) {
                ratio = 0.22; // Standard
            } else {
                ratio = 0.20; // Wider left panel on big screens
            }
            mainSplit.setDividerPosition(0, ratio);
        }
    }

    private void applyResponsiveHeight(double height) {
        // Adjust UI elements based on height
        // Can be extended for specific height-based adjustments
    }

    private void setupKeyboardShortcuts(Scene scene) {
        scene.setOnKeyPressed(e -> {
            // Skip if typing in text field
            if (e.getTarget() instanceof TextField || e.getTarget() instanceof ComboBox) return;

            switch (e.getCode()) {
                case DELETE, BACK_SPACE -> onDeleteWaypoint();
                case PLUS, EQUALS -> { if (e.isControlDown()) onZoomIn(); }
                case MINUS -> { if (e.isControlDown()) onZoomOut(); }
                case F -> { if (!e.isControlDown()) onFitWaypoints(); }
                case SPACE -> {
                    if (btnFollow != null) {
                        btnFollow.setSelected(!btnFollow.isSelected());
                        onToggleFollow();
                    }
                    e.consume();
                }
                case S -> { if (e.isControlDown()) { onExportXml(); e.consume(); } }
                case O -> { if (e.isControlDown()) { onImportXml(); e.consume(); } }
                case N -> { if (e.isControlDown()) { onAddWaypoint(); e.consume(); } }
                case DIGIT1, NUMPAD1 -> { onGoMissionTab(); e.consume(); }
                case DIGIT2, NUMPAD2 -> { onGoDataTab(); e.consume(); }
                case DIGIT3, NUMPAD3 -> { onGoSettingsTab(); e.consume(); }
                case DIGIT4, NUMPAD4 -> { onGoAlgoTab(); e.consume(); }
                case DIGIT5, NUMPAD5 -> { onGoChargingTab(); e.consume(); }
                default -> {}
            }
        });
    }

    private void initToastStack() {
        toastStack = new VBox(8);
        toastStack.setStyle("-fx-padding: 20;");
        toastStack.setManaged(false);
    }

    // ===== Toast notifications =====
    private void showToast(String message, String type) {
        Platform.runLater(() -> {
            Label toast = new Label(message);
            toast.getStyleClass().add("toast");
            toast.getStyleClass().add(type);
            toast.setMaxWidth(360);
            toast.setWrapText(true);

            // Add to top-right of root
            if (toastStack.getParent() == null && root != null) {
                toastStack.setLayoutX(root.getWidth() - 400);
                toastStack.setLayoutY(60);
                root.getChildren().add(toastStack);
            }
            toastStack.getChildren().add(toast);

            FadeTransition ft = new FadeTransition(Duration.millis(300), toast);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

            // Auto remove after 3s
            Timeline tl = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
                FadeTransition out = new FadeTransition(Duration.millis(300), toast);
                out.setFromValue(1);
                out.setToValue(0);
                out.setOnFinished(ev -> toastStack.getChildren().remove(toast));
                out.play();
            }));
            tl.setCycleCount(1);
            tl.play();
        });
    }

    // Root reference for toast
    @FXML private VBox root;

    private void initMap() {
        mapCanvas = new MapCanvas();
        // Add map to the bottom of stack (under floating controls)
        mapContainer.getChildren().add(0, mapCanvas);
        mapCanvas.widthProperty().bind(mapContainer.widthProperty());
        mapCanvas.heightProperty().bind(mapContainer.heightProperty());
        mapCanvas.setClickHandler((lat, lon) -> {
            addWaypoint(lat, lon);
        });
        // Zoom change listener
        mapCanvas.setZoomChangeListener(() -> {
            if (sbZoom != null) sbZoom.setText("Z:" + mapCanvas.getZoom());
        });
        // Initialize zoom display
        if (sbZoom != null) sbZoom.setText("Z:" + mapCanvas.getZoom());
        // Mouse move handler for coordinate display
        mapCanvas.setMouseMoveHandler((lat, lon) -> {
            if (sbMouse != null) {
                sbMouse.setText(String.format("%.3f, %.3f", lat, lon));
            }
        });
        // Follow change listener (update button when user drags)
        mapCanvas.setFollowChangeListener(() -> {
            if (btnFollow != null) btnFollow.setSelected(mapCanvas.isFollowEnabled());
        });
        // Waypoint drag handler
        mapCanvas.setWaypointMoveHandler((idx, latLon) -> {
            missionDirty = true;
            refreshWaypointList();
            updateMissionStats();
            updateUploadStatus();
        });
        // Waypoint double-click handler
        mapCanvas.setWaypointDoubleClickHandler(this::onEditWaypoint);
        // Map context menu
        mapCanvas.setContextMenuHandler((lat, lon) -> showMapContextMenu(lat, lon));
        mapCanvas.setWaypointContextMenuHandler(this::showWaypointContextMenu);

        // 高德在线地图（WebView）叠在离线瓦片地图之上；
        // 高德加载失败时自动隐藏 WebView，底层离线地图无缝接管
        initAMapView();

        // 飞行日志详情地图：高德轨迹回放（离线 Canvas 作降级）
        initFlightLogMap();
    }

    private void initAMapView() {
        amapView = new AMapWebView(true);
        amapView.setDark(ThemeManager.isDark());
        AnchorPane.setTopAnchor(amapView, 0.0);
        AnchorPane.setBottomAnchor(amapView, 0.0);
        AnchorPane.setLeftAnchor(amapView, 0.0);
        AnchorPane.setRightAnchor(amapView, 0.0);
        // 置于离线 Canvas(0) 之上、悬浮控件之下
        mapContainer.getChildren().add(1, amapView);
        amapView.setClickHandler((lat, lon) -> addWaypoint(lat, lon));
        amapView.setWaypointMoveHandler((idx, latLon) -> {
            if (idx >= 0 && idx < mission.size()) {
                Waypoint wp = mission.getWaypoint(idx);
                wp.setLat(latLon[0]);
                wp.setLon(latLon[1]);
                missionDirty = true;
                refreshWaypointList();
                updateMissionStats();
                updateUploadStatus();
            }
        });
        amapView.setMapRightClickHandler(this::showMapContextMenu);
        amapView.setWaypointRightClickHandler(this::showWaypointContextMenu);
        amapView.setWaypointDblClickHandler(this::onEditWaypoint);
        amapView.setMouseMoveHandler((lat, lon) -> {
            if (sbMouse != null) sbMouse.setText(String.format("%.3f, %.3f", lat, lon));
        });
        amapView.setFailHandler(() ->
            showToast("高德地图加载失败，已切换为离线瓦片地图", "warn"));
        amapView.setReadyListener(() -> {
            amapView.setWaypoints(mission.getWaypoints());
            amapView.setAirspaceZones(airspace.getActiveZones());
            amapView.setFollow(btnFollow == null || btnFollow.isSelected());
            amapView.setTrajectoryVisible(btnTrajectory == null || btnTrajectory.isSelected());
            amapView.setAirspaceVisible(btnAirspace == null || btnAirspace.isSelected());
        });
    }

    private void initFlightLogMap() {
        if (flMapContainer == null) return;
        // 离线 Canvas 需要显式绑定尺寸（Canvas 不参与布局自适应）
        flMapCanvas.widthProperty().bind(flMapContainer.widthProperty());
        flMapCanvas.heightProperty().bind(flMapContainer.heightProperty());

        flAmapView = new AMapWebView(false);
        flAmapView.setDark(ThemeManager.isDark());
        flMapContainer.getChildren().add(flAmapView);
        flAmapView.setFailHandler(() -> {
            if (selectedFlight != null) drawFlightTrajectoryNow(selectedFlight);
        });
        flAmapView.setReadyListener(() -> flAmapView.setDark(ThemeManager.isDark()));
    }

    private void initSensorChart() {
        sensor = new SensorChart();
        if (sensorChart != null && sensorChart.getParent() instanceof HBox parent) {
            int idx = parent.getChildren().indexOf(sensorChart);
            HBox.setHgrow(sensor.getNode(), HBox.getHgrow(sensorChart));
            parent.getChildren().set(idx, sensor.getNode());
            sensorChart = sensor.getNode();
        }
    }

    // ===== Sensor Chart Interactions =====
    @FXML
    private void onTimeRange5s() { sensor.setTimeWindow(5); updateWaveStat(); }
    @FXML
    private void onTimeRange10s() { sensor.setTimeWindow(10); updateWaveStat(); }
    @FXML
    private void onTimeRange30s() { sensor.setTimeWindow(30); updateWaveStat(); }
    @FXML
    private void onTimeRange60s() { sensor.setTimeWindow(60); updateWaveStat(); }

    @FXML
    private void onToggleWavePause() {
        sensor.togglePause();
        if (btnWavePause != null) {
            String text = sensor.isPaused() ? "继续" : "暂停";
            btnWavePause.setText(text);
        }
    }

    @FXML
    private void onClearWave() {
        sensor.clear();
        updateWaveStat();
        showToast("曲线已清空", "info");
    }

    @FXML
    private void onExportCsv() {
        showToast("CSV 导出功能开发中", "info");
    }

    @FXML
    private void onToggleChannel(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Label label) {
            String channel = label.getText();
            sensor.toggleChannel(channel);
            // Update visual state
            ObservableList<String> styleClasses = label.getStyleClass();
            if (styleClasses.contains("enabled")) {
                styleClasses.remove("enabled");
                if (label.getGraphic() != null) {
                    label.getGraphic().setStyle(
                        label.getGraphic().getStyle().replace("-fx-opacity: 1", "-fx-opacity: 0.3")
                    );
                }
            } else {
                styleClasses.add("enabled");
                if (label.getGraphic() != null) {
                    label.getGraphic().setStyle(
                        label.getGraphic().getStyle().replace("-fx-opacity: 0.3", "-fx-opacity: 1")
                    );
                }
            }
            updateWaveStat();
        }
    }

    private void updateWaveStat() {
        if (waveStat != null && sensor != null) {
            int enabled = sensor.getEnabledChannelCount();
            int samples = sensor.getSampleCount();
            waveStat.setText(enabled + " 通道 · " + samples + " 样本");
        }
    }

    @SuppressWarnings("unchecked")
    private void initLogTable() {
        // logTableView may be null in new FXML layout, keep for compatibility
        if (logTableView == null) return;
        ObservableList<LogRecord> logs = FXCollections.observableArrayList(
            createDemoLog(1, "DJI-M300", "2026-08-31 14:19:00", 980, 102.5, 4250),
            createDemoLog(2, "DJI-M300", "2026-08-29 15:42:00", 1790, 120.0, 8650),
            createDemoLog(3, "PX4-Vtol", "2026-08-26 08:30:00", 2120, 150.0, 12500),
            createDemoLog(4, "PX4-Vtol", "2026-08-25 10:05:00", 650, 85.0, 3100),
            createDemoLog(5, "DJI-M300", "2026-08-24 16:20:00", 1450, 110.0, 7200)
        );
        logTableView.setItems(logs);
        var cols = logTableView.getColumns();
        if (cols.size() >= 6) {
            cols.get(0).setCellValueFactory(new PropertyValueFactory<>("id"));
            cols.get(1).setCellValueFactory(new PropertyValueFactory<>("droneModel"));
            cols.get(2).setCellValueFactory(new PropertyValueFactory<>("formattedTime"));
            cols.get(3).setCellValueFactory(new PropertyValueFactory<>("duration"));
            cols.get(4).setCellValueFactory(new PropertyValueFactory<>("alt"));
            cols.get(5).setCellValueFactory(new PropertyValueFactory<>("distance"));
        }
    }

    private LogRecord createDemoLog(int id, String model, String start, int dur, double alt, double dist) {
        long ts;
        try {
            ts = java.time.LocalDateTime.parse(start,
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            ts = System.currentTimeMillis();
        }
        return new LogRecord(id, ts, 32.0612, 118.793, alt, 12.3, 11.2, model, dur, dist);
    }

    // ===== Flight Log =====
    private void initFlightLog() {
        if (flListView == null) return;

        // Generate demo flight logs
        allFlightLogs.clear();
        allFlightLogs.add(createDemoFlight("FL-20260831-001", "DJI-M300", "2026-08-31 14:19:00", 980, 102.5, 4250, 12.5, 11.1));
        allFlightLogs.add(createDemoFlight("FL-20260829-002", "DJI-M300", "2026-08-29 15:42:00", 1790, 120.0, 8650, 18.3, 10.8));
        allFlightLogs.add(createDemoFlight("FL-20260826-003", "PX4-Vtol", "2026-08-26 08:30:00", 2120, 150.0, 12500, 22.1, 10.5));
        allFlightLogs.add(createDemoFlight("FL-20260825-004", "PX4-Vtol", "2026-08-25 10:05:00", 650, 85.0, 3100, 9.8, 11.5));
        allFlightLogs.add(createDemoFlight("FL-20260824-005", "DJI-M300", "2026-08-24 16:20:00", 1450, 110.0, 7200, 15.6, 11.0));
        allFlightLogs.add(createDemoFlight("FL-20260822-006", "DJI-M300", "2026-08-22 09:15:00", 720, 95.0, 3600, 11.2, 11.3));

        // Setup model filter combo
        if (flModelCombo != null) {
            Set<String> models = allFlightLogs.stream()
                .map(cn.edu.nuaa.gcs.model.FlightLog::getDroneModel)
                .collect(Collectors.toCollection(TreeSet::new));
            flModelCombo.setItems(FXCollections.observableArrayList("全部"));
            flModelCombo.getItems().addAll(models);
            flModelCombo.getSelectionModel().select("全部");
        }

        // Setup list cell factory
        flListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(cn.edu.nuaa.gcs.model.FlightLog item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                VBox box = new VBox(2);
                Label idLabel = new Label(item.getId());
                idLabel.setStyle("-fx-text-fill: -fx-text-color; -fx-font-size: 12px; -fx-font-weight: bold;");
                Label metaLabel = new Label(item.getDroneModel() + " · " + item.getFormattedDuration());
                metaLabel.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 10px;");
                Label timeLabel = new Label(item.getFormattedStartTime());
                timeLabel.setStyle("-fx-text-fill: -fx-info; -fx-font-size: 10px;");
                box.getChildren().addAll(idLabel, metaLabel, timeLabel);
                setGraphic(box);
                setStyle("-fx-padding: 0;");
            }
        });

        // Setup selection listener
        flListView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                selectedFlight = sel;
                showFlightDetail(sel);
            }
        });

        // Setup search
        if (flSearchField != null) {
            flSearchField.textProperty().addListener((obs, old, val) -> filterFlightLogs());
        }
        if (flModelCombo != null) {
            flModelCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> filterFlightLogs());
        }

        // Populate list
        filterFlightLogs();
    }

    private void filterFlightLogs() {
        if (flListView == null) return;
        String search = flSearchField != null ? flSearchField.getText().toLowerCase() : "";
        String modelFilter = flModelCombo != null && flModelCombo.getValue() != null ? flModelCombo.getValue() : "全部";

        List<cn.edu.nuaa.gcs.model.FlightLog> filtered = allFlightLogs.stream()
            .filter(fl -> "全部".equals(modelFilter) || fl.getDroneModel().equals(modelFilter))
            .filter(fl -> search.isEmpty() || fl.getId().toLowerCase().contains(search)
                || fl.getDroneModel().toLowerCase().contains(search))
            .collect(Collectors.toList());

        flListView.setItems(FXCollections.observableArrayList(filtered));
        if (flCountLabel != null) {
            flCountLabel.setText("共 " + filtered.size() + " 次飞行");
        }
    }

    private cn.edu.nuaa.gcs.model.FlightLog createDemoFlight(String id, String model, String start,
            int dur, double maxAlt, double dist, double maxSpeed, double minVolt) {
        cn.edu.nuaa.gcs.model.FlightLog fl = new cn.edu.nuaa.gcs.model.FlightLog();
        fl.setId(id);
        fl.setDroneModel(model);
        try {
            long startTs = java.time.LocalDateTime.parse(start,
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            fl.setStartTime(startTs);
            fl.setEndTime(startTs + dur * 1000L);
        } catch (Exception e) {
            fl.setStartTime(System.currentTimeMillis() - dur * 1000L);
            fl.setEndTime(System.currentTimeMillis());
        }
        fl.setTotalDistance(dist);
        fl.setMaxAltitude(maxAlt);
        fl.setMaxSpeed(maxSpeed);
        fl.setMinVoltage(minVolt);

        // Generate trajectory points
        List<double[]> traj = new ArrayList<>();
        int points = Math.max(20, dur / 5);
        fl.setDataPoints(points);
        double baseLat = 32.0603 + (Math.random() - 0.5) * 0.02;
        double baseLon = 118.7921 + (Math.random() - 0.5) * 0.02;
        for (int i = 0; i < points; i++) {
            double t = (double) i / (points - 1);
            double lat = baseLat + Math.sin(t * Math.PI * 2) * 0.005 + (Math.random() - 0.5) * 0.001;
            double lon = baseLon + Math.cos(t * Math.PI * 1.5) * 0.005 + (Math.random() - 0.5) * 0.001;
            traj.add(new double[]{lon, lat});
        }
        fl.setTrajectory(traj);
        return fl;
    }

    private void showFlightDetail(cn.edu.nuaa.gcs.model.FlightLog fl) {
        if (flDetailEmpty != null) { flDetailEmpty.setVisible(false); flDetailEmpty.setManaged(false); }
        if (flDetailContent != null) { flDetailContent.setVisible(true); flDetailContent.setManaged(true); }

        if (flDetailId != null) flDetailId.setText(fl.getId());
        if (flDetailMeta != null) flDetailMeta.setText(
            fl.getDroneModel() + " · " + fl.getFormattedStartTime() + " · 时长 " + fl.getFormattedDuration());
        if (flStatPoints != null) flStatPoints.setText(String.valueOf(fl.getDataPoints()));
        if (flStatAlt != null) flStatAlt.setText(String.format("%.0f m", fl.getMaxAltitude()));
        if (flStatSpeed != null) flStatSpeed.setText(String.format("%.1f m/s", fl.getMaxSpeed()));
        if (flStatVolt != null) flStatVolt.setText(String.format("%.1f V", fl.getMinVoltage()));
        if (flStatDist != null) flStatDist.setText(fl.getDistanceKm());

        // 高德在线轨迹回放（WebView 可见时优先）
        if (flAmapView != null && fl.getTrajectory() != null && !fl.getTrajectory().isEmpty()) {
            flAmapView.showFlightTrajectory(fl.getTrajectory());
        }
        // Redraw after layout is computed (canvas may have been hidden)
        Platform.runLater(() -> {
            Platform.runLater(() -> drawFlightTrajectoryNow(fl));
        });
    }

    private void drawFlightTrajectory(cn.edu.nuaa.gcs.model.FlightLog fl) {
        if (flMapCanvas == null || fl.getTrajectory() == null || fl.getTrajectory().isEmpty()) return;

        double w = flMapCanvas.getWidth();
        double h = flMapCanvas.getHeight();
        if (w <= 0 || h <= 0) {
            // Redraw when canvas is sized
            flMapCanvas.widthProperty().addListener((obs, old, val) -> drawFlightTrajectoryNow(fl));
            flMapCanvas.heightProperty().addListener((obs, old, val) -> drawFlightTrajectoryNow(fl));
            return;
        }
        drawFlightTrajectoryNow(fl);
    }

    private void drawFlightTrajectoryNow(cn.edu.nuaa.gcs.model.FlightLog fl) {
        if (flMapCanvas == null) return;
        double w = flMapCanvas.getWidth();
        double h = flMapCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        var gc = flMapCanvas.getGraphicsContext2D();
        gc.setFill(javafx.scene.paint.Color.web("#0d1117"));
        gc.fillRect(0, 0, w, h);

        // Grid
        gc.setStroke(javafx.scene.paint.Color.web("#21262d"));
        gc.setLineWidth(1);
        double gridSize = 50;
        for (double x = 0; x < w; x += gridSize) { gc.strokeLine(x, 0, x, h); }
        for (double y = 0; y < h; y += gridSize) { gc.strokeLine(0, y, w, y); }

        List<double[]> traj = fl.getTrajectory();
        if (traj.isEmpty()) return;

        // Compute bounds
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (double[] p : traj) {
            minLon = Math.min(minLon, p[0]); maxLon = Math.max(maxLon, p[0]);
            minLat = Math.min(minLat, p[1]); maxLat = Math.max(maxLat, p[1]);
        }

        // Add padding
        double latPad = (maxLat - minLat) * 0.15;
        double lonPad = (maxLon - minLon) * 0.15;
        minLat -= latPad; maxLat += latPad;
        minLon -= lonPad; maxLon += lonPad;

        double latRange = maxLat - minLat;
        double lonRange = maxLon - minLon;
        if (latRange < 0.0001) latRange = 0.001;
        if (lonRange < 0.0001) lonRange = 0.001;

        // Map to canvas coordinates
        double scaleX = w / lonRange;
        double scaleY = h / latRange;
        double scale = Math.min(scaleX, scaleY);

        double offsetX = (w - lonRange * scale) / 2 - minLon * scale;
        double offsetY = (h - latRange * scale) / 2 - minLat * scale;

        // Draw trajectory
        gc.setStroke(javafx.scene.paint.Color.web("#3b82f6"));
        gc.setLineWidth(2.5);
        gc.beginPath();
        boolean first = true;
        for (double[] p : traj) {
            double sx = p[0] * scale + offsetX;
            double sy = h - (p[1] * scale + offsetY); // flip Y
            if (first) { gc.moveTo(sx, sy); first = false; }
            else { gc.lineTo(sx, sy); }
        }
        gc.stroke();

        // Start point (green)
        double[] start = traj.get(0);
        double sx = start[0] * scale + offsetX;
        double sy = h - (start[1] * scale + offsetY);
        gc.setFill(javafx.scene.paint.Color.web("#22c55e"));
        gc.fillOval(sx - 5, sy - 5, 10, 10);
        gc.setFill(javafx.scene.paint.Color.web("#e6edf3"));
        gc.setFont(javafx.scene.text.Font.font(10));
        gc.fillText("起点", sx + 8, sy + 4);

        // End point (red)
        if (traj.size() > 1) {
            double[] end = traj.get(traj.size() - 1);
            double ex = end[0] * scale + offsetX;
            double ey = h - (end[1] * scale + offsetY);
            gc.setFill(javafx.scene.paint.Color.web("#ef4444"));
            gc.fillOval(ex - 5, ey - 5, 10, 10);
            gc.setFill(javafx.scene.paint.Color.web("#e6edf3"));
            gc.fillText("终点", ex + 8, ey + 4);
        }
    }

    @FXML
    private void onShowWaveform() {
        if (waveformContent != null) { waveformContent.setVisible(true); waveformContent.setManaged(true); }
        if (flightLogContent != null) { flightLogContent.setVisible(false); flightLogContent.setManaged(false); }
    }

    @FXML
    private void onShowFlightLog() {
        if (waveformContent != null) { waveformContent.setVisible(false); waveformContent.setManaged(false); }
        if (flightLogContent != null) { flightLogContent.setVisible(true); flightLogContent.setManaged(true); }
        // 视图首次显示时地图容器尺寸才从 0 变为实际值，延迟一帧重绘/重载轨迹
        Platform.runLater(() -> {
            if (selectedFlight != null) {
                if (flAmapView != null && selectedFlight.getTrajectory() != null) {
                    flAmapView.showFlightTrajectory(selectedFlight.getTrajectory());
                }
                Platform.runLater(() -> drawFlightTrajectoryNow(selectedFlight));
            }
        });
    }

    @FXML
    private void onExportFlightCsv() {
        if (selectedFlight == null) { showToast("请先选择飞行记录", "warn"); return; }
        showToast("导出 " + selectedFlight.getId() + " CSV · 功能开发中", "info");
    }

    private void initSettings() {
        if (setRunMode != null) {
            setRunMode.setItems(FXCollections.observableArrayList(
                "E1 实机模式（真实飞控遥测）",
                "手机辅助模式（GPS 定位补充）"
            ));
            setRunMode.setValue(settings.runMode.equals("e1_real") ? "E1 实机模式（真实飞控遥测）"
                : "手机辅助模式（GPS 定位补充）");
        }
        if (e1IpField != null) e1IpField.setText(settings.e1Ip);
        if (e1PortField != null) e1PortField.setText(String.valueOf(settings.e1Port));
        if (hbTimeoutField != null) hbTimeoutField.setText(String.valueOf(settings.heartbeatTimeout));
        if (phoneGpsToggle != null) phoneGpsToggle.setSelected(settings.phoneGps);
        if (phoneCompassToggle != null) phoneCompassToggle.setSelected(settings.phoneCompass);
        if (phoneBaroToggle != null) phoneBaroToggle.setSelected(settings.phoneBaro);
        if (serialPortCombo != null) {
            var portList = new java.util.ArrayList<String>(java.util.Arrays.asList(SerialLink.listPorts()));
            portList.add("UDP 127.0.0.1:14550");
            if (!portList.contains(settings.serialPort)) portList.add(0, settings.serialPort);
            serialPortCombo.setItems(FXCollections.observableArrayList(portList));
            serialPortCombo.setValue(settings.serialPort);
            serialPortCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) settings.serialPort = newVal;
            });
        }
        if (baudRateCombo != null) {
            baudRateCombo.setItems(FXCollections.observableArrayList(9600, 38400, 57600, 115200));
            baudRateCombo.setValue(settings.baudRate);
            baudRateCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) settings.baudRate = newVal;
            });
        }
        if (sampleIntervalSlider != null) sampleIntervalSlider.setValue(settings.sampleInterval);
        if (mapZoomCombo != null) {
            mapZoomCombo.setItems(FXCollections.observableArrayList(14, 16, 18));
            mapZoomCombo.setValue(settings.mapZoom);
        }
        if (droneModelCombo != null) {
            droneModelCombo.setItems(FXCollections.observableArrayList("CF-Drone E1 Mini", "DJI Matrice 300 RTK", "PX4 VTOL", "自定义"));
            droneModelCombo.setValue(settings.droneModel);
        }
        if (droneRangeField != null) droneRangeField.setText(String.valueOf(settings.droneMaxRange));
        if (aspaceSrcType != null) {
            aspaceSrcType.setItems(FXCollections.observableArrayList("GeoJSON", "JSON", "Shapefile"));
            aspaceSrcType.setValue("GeoJSON");
        }
    }

    // ===== Enhanced Drone Status Grid - Card Style (H-02) =====
    private void initDroneStatusGrid() {
        if (droneStatusGrid == null) return;
        droneStatusGrid.getChildren().clear();
        // field name, default state class, format, source tag (e1/phone/system)
        String[][] fields = {
            {"飞行模式", "info", "AUTO", "e1"},
            {"阶段", "off", "待命", "e1"},
            {"纬度 LAT", "", "%.5f°", "e1"},
            {"经度 LON", "", "%.5f°", "e1"},
            {"高度 ALT", "", "%.1f m", "e1"},
            {"地速 GS", "", "%.1f m/s", "e1"},
            {"航向 HDG", "", "%.0f°", "e1"},
            {"横滚 ROLL", "", "%.1f°", "e1"},
            {"俯仰 PITCH", "", "%.1f°", "e1"},
            {"偏航 YAW", "", "%.0f°", "e1"},
            {"电量 BAT", "ok", "%.0f%%", "e1"},
            {"电压 V", "", "%.1fV", "e1"},
            {"信号 RSSI", "ok", "%.0f%%", "e1"},
            {"卫星 SAT", "ok", "%d", "e1"},
            {"HDOP", "ok", "%.1f", "e1"},
            {"解锁", "off", "未解锁", "e1"}
        };
        for (int i = 0; i < fields.length; i++) {
            int col = i % 2;
            int row = i / 2;
            VBox cell = new VBox(2);
            cell.getStyleClass().add("us-cell");

            Label lbl = new Label(fields[i][0]);
            lbl.getStyleClass().add("us-label");

            HBox valBox = new HBox(3);
            valBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label val = new Label("--");
            val.getStyleClass().add("us-val");
            val.setId("status_val_" + i);
            if (!fields[i][1].isEmpty()) {
                val.getStyleClass().add(fields[i][1]);
            }

            Label srcTag = new Label(getSourceTagText(fields[i][3]));
            srcTag.getStyleClass().add("src-tag");
            srcTag.getStyleClass().add(fields[i][3]);
            srcTag.setId("status_src_" + i);

            valBox.getChildren().addAll(val, srcTag);

            cell.getChildren().addAll(lbl, valBox);
            droneStatusGrid.add(cell, col, row);
        }

        // 追加电池详细信息 + 剩余时间（P1/P2 新增，BATTERY_STATUS + 动态预测）
        // 额外 2 行 2 列 = 4 个 cell，紧跟原有 16 字段
        int startRow = (fields.length + 1) / 2;
        lblBatCurrent = addBatteryCell("放电电流", 0, startRow, "e1");
        lblBatTemp    = addBatteryCell("电池温度", 1, startRow, "e1");
        lblCells      = addBatteryCell("电芯电压", 0, startRow + 1, "e1");
        lblBatTimeLeft= addBatteryCell("剩余时间", 1, startRow + 1, "e1");
    }

    private Label addBatteryCell(String label, int col, int row, String srcTagKey) {
        VBox cell = new VBox(2);
        cell.getStyleClass().add("us-cell");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("us-label");
        HBox valBox = new HBox(3);
        valBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label val = new Label("—");
        val.getStyleClass().addAll("us-val", "off");
        Label src = new Label(getSourceTagText(srcTagKey));
        src.getStyleClass().addAll("src-tag", srcTagKey);
        valBox.getChildren().addAll(val, src);
        cell.getChildren().addAll(lbl, valBox);
        droneStatusGrid.add(cell, col, row);
        return val;
    }

    // ================================================================
    // P3/P5/P6 UI + 服务 初始化 / 释放
    // ================================================================

    private void initAttitudeAndNetMon() {
        if (attitudeHost != null && attitudeIndicator == null) {
            attitudeIndicator = new AttitudeIndicator();
            attitudeHost.getChildren().add(attitudeIndicator);
            javafx.scene.layout.StackPane.setAlignment(attitudeIndicator, javafx.geometry.Pos.CENTER);
            attitudeIndicator.prefWidthProperty().bind(attitudeHost.widthProperty().subtract(16));
            attitudeIndicator.prefHeightProperty().bind(attitudeHost.heightProperty().subtract(16));
            attitudeIndicator.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        if (networkMonitor == null) {
            networkMonitor = new cn.edu.nuaa.gcs.util.NetworkMonitor(state -> {
                javafx.application.Platform.runLater(() -> applyNetworkState(state));
            });
            try { networkMonitor.start(); } catch (Exception ignored) {}
        }
        // UI 节流 Timeline：100ms = 10fps，合并多帧遥测为一次 UI 刷新
        if (uiRefreshTimeline == null) {
            uiRefreshTimeline = new Timeline(new KeyFrame(Duration.millis(100), e -> flushUiUpdates()));
            uiRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
            uiRefreshTimeline.play();
        }
    }

    private void applyNetworkState(cn.edu.nuaa.gcs.util.NetworkMonitor.State state) {
        if (networkBannerLabel == null) return;
        switch (state) {
            case DRONE_WIFI:
                networkBannerLabel.setVisible(true);
                networkBannerLabel.setManaged(true);
                networkBannerLabel.setText("[WiFi] 已连接无人机 WiFi 网段 192.168.4.x，互联网不可用，地图需离线瓦片。");
                networkBannerLabel.getStyleClass().remove("on");
                networkBannerLabel.getStyleClass().add("off");
                break;
            case OFFLINE:
                networkBannerLabel.setVisible(true);
                networkBannerLabel.setManaged(true);
                networkBannerLabel.setText("[网络] 当前无可用 IPv4 接口，请检查连接。");
                networkBannerLabel.getStyleClass().remove("on");
                networkBannerLabel.getStyleClass().add("off");
                break;
            case ONLINE:
                networkBannerLabel.setVisible(false);
                networkBannerLabel.setManaged(false);
                break;
            default:
                networkBannerLabel.setVisible(false);
                networkBannerLabel.setManaged(false);
        }
    }

    /**
     * 通过 TCP 连接无人机 WiFi（默认 192.168.4.1，设置页 e1IpField/e1PortField 可覆盖）。
     * 纯链路层操作：不修改飞控，只打开连接并让 CommunicationService 开始监听。
     */
    @FXML
    private void onConnectTcp() {
        String ip = (e1IpField != null && !e1IpField.getText().isBlank())
                ? e1IpField.getText().trim() : cn.edu.nuaa.gcs.comm.TcpLink.DEFAULT_HOST;
        int port = 5760;
        try {
            if (e1PortField != null && !e1PortField.getText().isBlank())
                port = Integer.parseInt(e1PortField.getText().trim());
        } catch (NumberFormatException ignored) { /* 回退默认 */ }

        final String fIp = ip;
        final int fPort = port;
        new Thread(() -> {
            try {
                TcpLink link = new TcpLink(fIp, fPort);
                comm.setLink(link);
                comm.setCallback(this::onTelemetrySafe);
                comm.start();
                Platform.runLater(() -> {
                    if (e1ConnBanner != null) {
                        e1ConnBanner.setText("TCP 连接中…");
                        e1ConnBanner.getStyleClass().remove("off");
                        e1ConnBanner.getStyleClass().add("on");
                    }
                    if (e1IpVal != null) e1IpVal.setText(fIp + ":" + fPort);
                    paramListRequested = false;
                });
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                Platform.runLater(() -> showToast("TCP 连接失败: " + msg, "error"));
            }
        }, "GCS-TcpConn").start();
    }

    /**
     * 读取电池数据（只读，安全）：主动请求 BATTERY_STATUS(147) 消息，
     * 同时触发 requestParamList 让飞控回传所有参数（含 BATT_CAPACITY 等）。
     * 不发送任何 PARAM_SET。
     */
    @FXML
    private void onRequestBattery() {
        if (!comm.isConnected()) {
            showToast("请先连接 E1 飞控（串口或 WiFi）。", "warn");
            return;
        }
        batteryPredictor.reset(); // 重置旧滑动窗口（保证当前飞控数据不被旧历史污染）
        // 优先：主动请求 BATTERY_STATUS(147) 消息
        comm.requestBatteryStatus();
        // 兼容：部分固件通过 SYS_STATUS(1) 上报电池，请求一次
        comm.requestMessage(1);
        // 兼容：请求 HIGHRES_IMU(105)（含气压高度、温度等）
        comm.requestMessage(105);
        // 请求飞控参数列表（含 BATT_CAPACITY / BATT_N_CELLS / VBAT_* 等）
        comm.requestParamList();
        // 请求 AUTOPILOT_VERSION 便于诊断固件能力
        comm.requestAutopilotVersion();
        showToast("已请求 BATTERY_STATUS / SYS_STATUS / HIGHRES_IMU / 参数列表（只读），等待飞控回传 [E1]。", "info");
    }

    /** 启动/停止遥测录制（CSV）。 */
    @FXML
    private void onToggleRecord() {
        if (btnRecordTelemetry.isSelected()) {
            try {
                String stamp = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                java.nio.file.Path dir = java.nio.file.Paths.get("telemetry");
                java.nio.file.Files.createDirectories(dir);
                telemetryRecorder = new cn.edu.nuaa.gcs.data.TelemetryRecorder(dir.resolve("gcs_" + stamp + ".csv"));
                recordingOn.set(true);
                if (btnRecordTelemetry != null) btnRecordTelemetry.setText("停止录制");
                if (recordingStatusLabel != null) {
                    recordingStatusLabel.setText("录制中: " + telemetryRecorder.getFile().getFileName());
                    recordingStatusLabel.getStyleClass().remove("off");
                    recordingStatusLabel.getStyleClass().add("on");
                }
            } catch (Exception e) {
                recordingOn.set(false);
                if (btnRecordTelemetry != null) {
                    btnRecordTelemetry.setSelected(false);
                    btnRecordTelemetry.setText("开始录制");
                }
                showToast("开启录制失败: " + e.getMessage(), "error");
            }
        } else {
            stopRecording();
        }
    }

    private void stopRecording() {
        recordingOn.set(false);
        cn.edu.nuaa.gcs.data.TelemetryRecorder rec = telemetryRecorder;
        telemetryRecorder = null;
        int rows = 0;
        if (rec != null) {
            rows = rec.getWrittenRows();
            try { rec.close(); } catch (Exception ignored) {}
        }
        if (btnRecordTelemetry != null) btnRecordTelemetry.setText("开始录制");
        if (recordingStatusLabel != null) {
            recordingStatusLabel.getStyleClass().remove("on");
            recordingStatusLabel.getStyleClass().add("off");
            recordingStatusLabel.setText(rows > 0 ? "已保存 " + rows + " 行: " + rec.getFile().getFileName() : "录制未开始");
        }
    }

    /** 安全的 onTelemetry 包装（跨线程 → Platform.runLater），供回调入口调用。 */
    private void onTelemetrySafe(MavlinkParser.Telemetry t) {
        Platform.runLater(() -> onTelemetry(t));
    }

    /** 释放新增 P3/P5/P6 相关资源（供窗口关闭时调用）。 */
    private void stopExtraServices() {
        stopRecording();
        if (networkMonitor != null) { networkMonitor.stop(); networkMonitor = null; }
        if (batteryPredictor != null) batteryPredictor.reset();
        if (chargingMonitor != null) chargingMonitor.stop();
        if (uiRefreshTimeline != null) { uiRefreshTimeline.stop(); uiRefreshTimeline = null; }
    }

    // ================================================================
    // 遥测统一入口（Serial/TCP 共用）
    // ================================================================

    /**
     * 遥测到达入口（由 listen 线程通过 Platform.runLater 调用）。
     *
     * 拆分为两阶段：
     * 1. 本方法（每帧执行）：轻量数据采集 — drone 模型更新、电池预测观测、CSV 录制、参数累积。
     *    这些操作要么是 JavaFX Property set（EDT 安全），要么是内部线程安全。
     * 2. {@link #flushUiUpdates()}（Timeline 10fps 节流）：重 UI — 状态网格、E1/手机详情、
     *    地图重绘、姿态仪表。多帧合并为一次刷新，避免 EDT 过载。
     */
    private void onTelemetry(MavlinkParser.Telemetry t) {
        if (t == null || !t.valid) return;
        // 单点翻译：更新 Drone 模型的 JavaFX 可观察属性
        drone.updateFromTelemetry(t);
        // 累积飞控参数（只读，安全）
        if (t.hasParam) {
            paramService.onParamValue(t);
            // 电池容量参数 → 更新动态预测器
            if (t.paramName != null) {
                String pn = t.paramName.toUpperCase();
                if ((pn.startsWith("BATT_CAPACITY") || pn.equals("BATT_CAPACITY_MAH")
                        || pn.startsWith("BATT_") && pn.contains("CAPACITY"))
                        && t.paramValue > 0) {
                    batteryPredictor.setTotalCapacityMah(t.paramValue);
                    System.out.println("[BAT] 从参数 " + t.paramName + "=" + t.paramValue
                            + " 更新电池容量 → BatteryPredictor");
                }
            }
        }
        // 连接建立收到首个心跳后，自动请求飞控参数列表一次
        if (!paramListRequested && t.hasHeartbeat) {
            paramListRequested = true;
            comm.requestParamList();
            System.out.println("[E1] 收到首个心跳，已请求飞控参数列表（CF-Drone 84 项）");
        }
        // P2：电池动态预测观测（仅 BATTERY_STATUS 帧）
        if (t.hasBatteryStatus) {
            System.out.println("[E1] 收到 BATTERY_STATUS(147): current=" + t.batteryCurrent
                    + "A temp=" + t.batteryTemp + "°C consumed=" + t.capacityConsumed
                    + "mAh remaining=" + t.batteryRemaining2 + "% cells="
                    + t.cellVoltages[0] + "+" + t.cellVoltages[1] + "mV");
            double epoch = System.currentTimeMillis() / 1000.0;
            int remainingPct = (t.batteryRemaining2 >= 0 && t.batteryRemaining2 <= 100)
                    ? t.batteryRemaining2 : (t.hasBattery ? (int) Math.round(t.batteryPct) : -1);
            double v = (t.hasBattery && t.voltage > 0) ? t.voltage : 0.0;
            batteryPredictor.observe(epoch, t.batteryCurrent, v,
                    t.capacityConsumed, remainingPct, t.batteryTemp);
        }
        // COMMAND_ACK 诊断日志（判断飞控是否支持请求的消息）
        if (t.hasAck) {
            String[] resultNames = {"ACCEPTED", "TEMPORARILY_REJECTED", "DENIED",
                    "UNSUPPORTED", "FAILED", "IN_PROGRESS"};
            String resultName = (t.ackResult >= 0 && t.ackResult < resultNames.length)
                    ? resultNames[t.ackResult] : ("UNKNOWN(" + t.ackResult + ")");
            System.out.println("[E1] COMMAND_ACK: command=" + t.ackCommand
                    + " result=" + t.ackResult + "(" + resultName + ")");
        }
        // STATUSTEXT 告警 → toast 显示（500ms 去重，连续相同文本合并）
        if (t.hasStatusText && t.statusText != null && !t.statusText.isBlank()) {
            String txt = t.statusText.trim();
            long now = System.currentTimeMillis();
            boolean dup = txt.equals(lastStatusText) && (now - lastStatusTextMs) < 2000;
            if (!dup) {
                lastStatusText = txt;
                lastStatusTextMs = now;
                // severity: 0=EMERGENCY 1=ALERT 2=CRITICAL 3=ERROR 4=WARNING 5=NOTICE 6=INFO 7=DEBUG
                String type = (t.statusSeverity <= 3) ? "error" : (t.statusSeverity <= 4) ? "warn" : "info";
                String prefix = switch (t.statusSeverity) {
                    case 0 -> "[紧急] ";
                    case 1 -> "[告警] ";
                    case 2 -> "[严重] ";
                    case 3 -> "[错误] ";
                    case 4 -> "[警告] ";
                    default -> "";
                };
                showToast(prefix + txt, type);
            }
        }
        // P5：遥测 CSV 录制（开启时才写）
        if (recordingOn.get() && telemetryRecorder != null) {
            try { telemetryRecorder.record(t); }
            catch (IOException e) {
                System.err.println("[REC] 写入失败: " + e.getMessage());
                Platform.runLater(this::stopRecording);
                Platform.runLater(() -> showToast("遥测录制已停止（写入失败）", "warn"));
            }
        }
        // 标记 UI 有积压，等 Timeline 刷新
        pendingTelemetry = t;
        uiDirty = true;
    }

    /**
     * UI 节流刷新（由 Timeline 100ms 周期调用，在 EDT 上执行）。
     * 合并多帧遥测为一次 UI 更新，消除 EDT 过载。
     */
    private void flushUiUpdates() {
        // 即使无遥测，也定期刷新连接状态（检测断线 → 横幅自动切换为"离线"）
        if (!uiDirty) {
            // 无积压数据时，仅刷新连接横幅与状态点（开销极小）
            updateConnStatus();
            // 检测链路超时：5 秒无数据 → 标记离线
            if (comm != null && !comm.isLinkActive() && comm.getLastReceivedMs() > 0) {
                // 最后一帧超过 5s 前收到，需要刷新状态横幅
                updateDroneStatus();
                updateE1StatusDetail();
            }
            // 刷新 WebView 积压
            if (amapView != null) amapView.flushDroneUpdate();
            // 电池充电工作区（WMI 系统电池独立于飞控遥测，仍需刷新）
            updateChargingWorkspace();
            return;
        }
        uiDirty = false;
        MavlinkParser.Telemetry t = pendingTelemetry;
        if (t == null) return;

        // 重 UI 更新（节流后每 100ms 执行一次，而非每帧）
        updateDroneStatus();
        updateE1StatusDetail();
        updatePhoneStatusDetail();
        updateConnStatus();

        // P6：姿态仪表（Canvas 重绘节流到 10fps）
        if (attitudeIndicator != null && t.hasAttitude) {
            attitudeIndicator.setInputRadians(t.roll, t.pitch, t.yaw);
        }

        // 地图：批量更新（1 次 draw 替代 3 次），Canvas 节流 100ms
        if (t.hasPosition && mapCanvas != null) {
            long now = System.nanoTime();
            if (now - lastMapDrawNs >= MAP_THROTTLE_NS) {
                lastMapDrawNs = now;
                mapCanvas.setDroneState(t.lat, t.lon, t.alt, t.heading);
            }
            // WebView JS 节流（内部 200ms = 5fps）
            if (amapView != null) {
                amapView.setDronePosition(t.lat, t.lon, t.heading, t.alt);
                amapView.flushDroneUpdate();
            }
        }

        // 状态栏
        if (sbLat != null) sbLat.setText(t.hasPosition ? String.format("%.5f", t.lat) : "—");
        if (sbLon != null) sbLon.setText(t.hasPosition ? String.format("%.5f", t.lon) : "—");
        if (seTemp != null) {
            seTemp.setText(drone.isImuAvailable() ? String.format("%.1f°C", drone.getImuTemp()) : "—");
        }
        if (sbLastUpdate != null) sbLastUpdate.setText(
            new Date().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalTime().toString());

        // 电池充电工作区刷新（节流到 10fps，仅在 Tab 可见时更新 UI）
        updateChargingWorkspace();
    }

    private String getSourceTagText(String src) {
        return switch (src) {
            case "e1" -> "飞控";
            case "phone" -> "手机";
            case "system" -> "系统";
            case "api" -> "API";
            default -> "飞控";
        };
    }

    private void initWaypoints() {
        mission.addWaypoint(new Waypoint(32.0612, 118.793, 80, 0, ActionMode.CRUISE, 3));
        mission.addWaypoint(new Waypoint(32.0628, 118.7955, 100, 0, ActionMode.CRUISE, 3));
        mission.addWaypoint(new Waypoint(32.0635, 118.7988, 100, 5, ActionMode.TAKEPHOTO, 2));
        mission.addWaypoint(new Waypoint(32.0620, 118.8012, 120, 0, ActionMode.CRUISE, 3));
        mission.addWaypoint(new Waypoint(32.0605, 118.7990, 100, 5, ActionMode.TAKEPHOTO, 2));
        mission.addWaypoint(new Waypoint(32.0612, 118.7960, 80, 0, ActionMode.LOITER, 3));
        mission.setUploaded(true);
        missionDirty = false;
        refreshWaypointList();
        updateMap();
        updateMissionStats();
        updateUploadStatus();
        updateWpCount();
    }

    @FXML
    private void onE1Mode() {
        settings.runMode = "e1_real";
        applyModeGating();
        connectE1Serial();
    }
    @FXML
    private void onPhoneMode() { settings.runMode = "phone_assist"; applyModeGating(); }

    private void applyModeGating() {
        boolean isE1 = "e1_real".equals(settings.runMode);
        boolean isPhone = "phone_assist".equals(settings.runMode);
        if (e1StatusBox != null) {
            e1StatusBox.setVisible(isE1 || isPhone);
            e1StatusBox.setManaged(isE1 || isPhone);
        }
        if (phoneStatusBox != null) {
            phoneStatusBox.setVisible(isPhone || isE1);
            phoneStatusBox.setManaged(isPhone || isE1);
        }
        if (e1ConnBanner != null) {
            // 修复：根据实际连接状态而非模式开关显示在线/离线
            boolean e1Connected = comm != null && comm.isConnected();
            boolean linkActive = comm != null && comm.isLinkActive();
            String e1Text;
            String e1Style;
            if (!isE1) {
                e1Text = "● 未启用 E1 模式";
                e1Style = "offline";
            } else if (e1Connected && linkActive) {
                e1Text = "● E1 飞控在线";
                e1Style = "online";
            } else if (e1Connected) {
                e1Text = "● E1 链路已连接，等待数据…";
                e1Style = "standby";
            } else {
                e1Text = "● E1 飞控离线";
                e1Style = "offline";
            }
            e1ConnBanner.setText(e1Text);
            e1ConnBanner.getStyleClass().clear();
            e1ConnBanner.getStyleClass().add("dev-conn-banner");
            e1ConnBanner.getStyleClass().add(e1Style);
        }
        if (phoneConnBanner != null) {
            boolean phoneConnected = comm != null && comm.isConnected();
            phoneConnBanner.setText(isPhone && phoneConnected ? "● 手机伴随已连接" : "● 未连接");
            phoneConnBanner.getStyleClass().clear();
            phoneConnBanner.getStyleClass().add("dev-conn-banner");
            phoneConnBanner.getStyleClass().add(isPhone && phoneConnected ? "online" : "offline");
        }
        if (safeBanner != null) {
            safeBanner.setVisible(isE1);
            safeBanner.setManaged(isE1);
        }
        if (ctrlPanel != null) {
            ctrlPanel.setVisible(isE1 && controlTestActive);
            ctrlPanel.setManaged(isE1 && controlTestActive);
        }
        if (uploadE1Btn != null) {
            uploadE1Btn.setDisable(!isE1);
        }
        if (autoMissionBtn != null) {
            autoMissionBtn.setDisable(!isE1);
        }
        updateE1StatusDetail();
        updatePhoneStatusDetail();
        updateConnStatus();
        updateStatusGridSourceTags();
    }

    /** 更新 E1 飞控状态区各行（IP/端口/延迟/解锁/油门/控制来源/心跳/链路质量） */
    private void updateE1StatusDetail() {
        boolean isE1 = "e1_real".equals(settings.runMode);
        boolean connected = comm != null && comm.isConnected();
        if (e1IpVal != null) e1IpVal.setText(settings.e1Ip != null ? settings.e1Ip : "—");
        if (e1PortVal != null) e1PortVal.setText(String.valueOf(settings.e1Port));
        // 通信延迟/油门/控制来源/心跳：飞控不上报时诚实标注 "—"
        if (e1LatencyVal != null) e1LatencyVal.setText(connected ? "—" : "—");
        if (e1ThrottleVal != null) e1ThrottleVal.setText(connected ? "—" : "0%");
        if (e1CtrlSrcVal != null) e1CtrlSrcVal.setText(connected ? "—" : "—");
        if (e1HeartbeatVal != null) {
            if (connected) {
                long total = comm.getTotalReceived();
                e1HeartbeatVal.setText(total > 0 ? total + " 帧" : "—");
            } else {
                e1HeartbeatVal.setText("—");
            }
        }
        if (e1LinkQualityVal != null) {
            if (!connected || comm == null) {
                e1LinkQualityVal.setText("—");
                e1LinkQualityVal.getStyleClass().removeAll("ok", "warn", "err", "off");
                e1LinkQualityVal.getStyleClass().add("off");
            } else {
                double q = comm.getLinkQualityPct();
                if (!Double.isFinite(q) || q < 0) {
                    e1LinkQualityVal.setText("计算中");
                    e1LinkQualityVal.getStyleClass().removeAll("ok", "warn", "err", "off");
                    e1LinkQualityVal.getStyleClass().add("off");
                } else {
                    e1LinkQualityVal.setText(String.format(Locale.ROOT, "%.0f%%", q));
                    e1LinkQualityVal.getStyleClass().removeAll("ok", "warn", "err", "off");
                    if (q >= 70) e1LinkQualityVal.getStyleClass().add("ok");
                    else if (q >= 30) e1LinkQualityVal.getStyleClass().add("warn");
                    else e1LinkQualityVal.getStyleClass().add("err");
                }
            }
        }
        // 解锁状态
        if (e1ArmedVal != null) {
            boolean armed = drone.isHeartbeatAvailable() && drone.isArmed();
            e1ArmedVal.setText(armed ? "已解锁" : "未解锁");
            e1ArmedVal.getStyleClass().removeAll("ok", "off", "warn", "err");
            e1ArmedVal.getStyleClass().add(armed ? "ok" : "off");
        }
        // 同步刷新数据源抽屉 E1 卡片
        if (dsE1Conn != null) {
            dsE1Conn.setText(connected ? "已连接" : "未连接");
            dsE1Conn.getStyleClass().removeAll("ok", "err");
            dsE1Conn.getStyleClass().add(connected ? "ok" : "err");
        }
        if (dsE1Hb != null) dsE1Hb.setText(connected ? "1 Hz" : "-- Hz");
        if (dsE1Loss != null) dsE1Loss.setText("--");
    }

    /** 更新手机伴随定位状态区各行（GPS/精度/位置/罗盘/气压计/最后更新） */
    private void updatePhoneStatusDetail() {
        boolean gpsOn = phoneGpsToggle != null && phoneGpsToggle.isSelected();
        boolean compassOn = phoneCompassToggle != null && phoneCompassToggle.isSelected();
        boolean baroOn = phoneBaroToggle != null && phoneBaroToggle.isSelected();
        if (phoneGpsVal != null) {
            phoneGpsVal.setText(gpsOn ? "已接入" : "未接入");
            phoneGpsVal.getStyleClass().removeAll("ok", "off");
            phoneGpsVal.getStyleClass().add(gpsOn ? "ok" : "off");
        }
        if (phoneGpsAccVal != null) phoneGpsAccVal.setText(gpsOn ? "—" : "—");
        if (phonePosVal != null) {
            boolean posOk = drone.isPositionAvailable();
            phonePosVal.setText(posOk
                ? String.format("%.5f, %.5f", drone.getLat(), drone.getLon())
                : "—");
        }
        if (phoneCompassVal != null) {
            phoneCompassVal.setText(compassOn ? "已启用" : "未启用");
            phoneCompassVal.getStyleClass().removeAll("ok", "off");
            phoneCompassVal.getStyleClass().add(compassOn ? "ok" : "off");
        }
        if (phoneBaroVal != null) {
            phoneBaroVal.setText(baroOn ? "已启用" : "未启用");
            phoneBaroVal.getStyleClass().removeAll("ok", "off");
            phoneBaroVal.getStyleClass().add(baroOn ? "ok" : "off");
        }
        if (phoneLastUpdateVal != null) {
            boolean hbOk = drone.isHeartbeatAvailable();
            phoneLastUpdateVal.setText(hbOk
                ? java.time.LocalTime.now().toString().substring(0, 8)
                : "—");
        }
        // 同步刷新数据源抽屉 手机卡片
        if (dsPhoneGps != null) {
            dsPhoneGps.setText(gpsOn ? "已接入" : "未接入");
            dsPhoneGps.getStyleClass().removeAll("ok", "err");
            dsPhoneGps.getStyleClass().add(gpsOn ? "ok" : "err");
        }
        if (dsPhoneAcc != null) dsPhoneAcc.setText("--");
    }

    private void updateStatusGridSourceTags() {
        if (droneStatusGrid == null) return;
        String tag = switch (settings.runMode) {
            case "e1_real" -> "飞控";
            case "phone_assist" -> "手机";
            default -> "飞控";
        };
        String tagClass = switch (settings.runMode) {
            case "e1_real" -> "e1";
            case "phone_assist" -> "phone";
            default -> "e1";
        };
        // Update all status source tags (indices 0-15)
        for (int i = 0; i < 16; i++) {
            var node = droneStatusGrid.lookup("#status_src_" + i);
            if (node instanceof Label lbl) {
                lbl.setText(tag);
                lbl.getStyleClass().removeAll("e1", "phone", "system", "api");
                lbl.getStyleClass().add(tagClass);
            }
        }
    }

    private void updateConnStatus() {
        if (connDot == null || connDetail == null) return;
        connDot.getStyleClass().removeAll("on", "off", "warn");
        boolean connected = comm != null && comm.isConnected();
        boolean linkActive = comm != null && comm.isLinkActive();
        // 低电量告警（≤20% 弹出一次）
        if (drone.isBatteryAvailable() && drone.getBatteryPct() <= 20) {
            long now = System.currentTimeMillis();
            if (now - lastLowBatMs > 30000) {
                lastLowBatMs = now;
                showToast("[警告] 电池电量低 (" + Math.round(drone.getBatteryPct()) + "%)", "warn");
            }
        }
        switch (settings.runMode) {
            case "e1_real" -> {
                if (linkActive) {
                    connDot.getStyleClass().add("on");
                    String link = settings.e1Ip != null && !settings.e1Ip.isEmpty()
                        ? ("TCP " + settings.e1Ip + ":" + settings.e1Port)
                        : (settings.serialPort + " " + settings.baudRate);
                    connDetail.setText("已连接 · " + link);
                } else if (connected) {
                    // 串口已开但 5s 无遥测 → 链路异常
                    connDot.getStyleClass().add("warn");
                    connDetail.setText("链路超时 · 等待遥测…");
                } else {
                    connDot.getStyleClass().add("off");
                    connDetail.setText("未连接 · 请选择串口或 WiFi");
                }
            }
            case "phone_assist" -> {
                if (linkActive) {
                    connDot.getStyleClass().add("on");
                    connDetail.setText("已连接 · 手机辅助");
                } else if (connected) {
                    connDot.getStyleClass().add("warn");
                    connDetail.setText("链路超时 · 手机辅助");
                } else {
                    connDot.getStyleClass().add("off");
                    connDetail.setText("未连接 · 手机辅助");
                }
            }
            default -> {
                connDot.getStyleClass().add("off");
                connDetail.setText("未选择模式");
            }
        }
    }

    // ===== Control Test Flow =====
    @FXML
    private void onEnterCtrlTest() {
        controlTestActive = true;
        if (ctrlPanel != null) {
            ctrlPanel.setVisible(true);
            ctrlPanel.setManaged(true);
        }
        if (btnEnterCtrl != null) {
            btnEnterCtrl.setVisible(false);
            btnEnterCtrl.setManaged(false);
        }
        if (btnExitCtrl != null) {
            btnExitCtrl.setVisible(true);
            btnExitCtrl.setManaged(true);
        }
        if (ctrlStatusLabel != null) {
            ctrlStatusLabel.setText("控制测试已激活 · 请谨慎操作");
        }
        showToast("已进入控制测试模式，请确保无桨/架台测试", "warn");
    }

    @FXML
    private void onExitCtrlTest() {
        controlTestActive = false;
        if (ctrlPanel != null) {
            ctrlPanel.setVisible(false);
            ctrlPanel.setManaged(false);
        }
        if (btnEnterCtrl != null) {
            btnEnterCtrl.setVisible(true);
            btnEnterCtrl.setManaged(true);
        }
        if (btnExitCtrl != null) {
            btnExitCtrl.setVisible(false);
            btnExitCtrl.setManaged(false);
        }
        showToast("已退出控制测试模式", "info");
    }

    // ===== Map Controls =====
    @FXML
    private void onToggleDataSourceDrawer() {
        if (dsDrawer != null) {
            boolean show = !dsDrawer.isVisible();
            dsDrawer.setVisible(show);
            dsDrawer.setManaged(show);
        }
    }

    @FXML
    private void onFitWaypoints() {
        if (mapCanvas != null) mapCanvas.fitWaypoints();
        if (amapView != null) amapView.fitWaypoints();
    }

    @FXML
    private void onToggleFollow() {
        if (btnFollow != null) {
            if (mapCanvas != null) mapCanvas.setFollowEnabled(btnFollow.isSelected());
            if (amapView != null) amapView.setFollow(btnFollow.isSelected());
            if (btnFollow.isSelected()) {
                showToast("已开启跟随模式", "info");
            }
        }
    }

    @FXML
    private void onToggleTrajectory() {
        if (btnTrajectory != null) {
            if (mapCanvas != null) mapCanvas.setTrajectoryVisible(btnTrajectory.isSelected());
            if (amapView != null) amapView.setTrajectoryVisible(btnTrajectory.isSelected());
        }
    }

    @FXML
    private void onToggleMapStyle() {
        if (mapCanvas != null) {
            MapCanvas.MapStyle current = mapCanvas.getMapStyle();
            MapCanvas.MapStyle next = switch (current) {
                case STANDARD -> MapCanvas.MapStyle.SATELLITE;
                case SATELLITE -> MapCanvas.MapStyle.TERRAIN;
                case TERRAIN -> MapCanvas.MapStyle.STANDARD;
            };
            mapCanvas.setMapStyle(next);
            // 高德地图：标准 ↔ 卫星（高德 JS 样式无地形，卫星与离线端保持同步）
            if (amapView != null) amapView.setSatellite(next == MapCanvas.MapStyle.SATELLITE);
            String name = switch (next) {
                case STANDARD -> "标准地图";
                case SATELLITE -> "卫星地图";
                case TERRAIN -> "地形地图";
            };
            showToast("已切换至" + name, "info");
        }
    }

    @FXML
    private void onToggleAirspaceLayer() {
        if (btnAirspace != null) {
            if (mapCanvas != null) mapCanvas.setAirspaceVisible(btnAirspace.isSelected());
            if (amapView != null) amapView.setAirspaceVisible(btnAirspace.isSelected());
        }
    }

    @FXML
    private void onZoomIn() {
        if (mapCanvas != null) mapCanvas.zoomIn();
        if (amapView != null) amapView.zoomIn();
    }

    @FXML
    private void onZoomOut() {
        if (mapCanvas != null) mapCanvas.zoomOut();
        if (amapView != null) amapView.zoomOut();
    }

    @FXML
    private void onSensorEntryClick() {
        if (workspaceTabs != null && dataTab != null) {
            workspaceTabs.getSelectionModel().select(dataTab);
        }
    }

    // ===== Theme setters =====
    @FXML
    private void onSetDarkTheme() {
        ThemeManager.setTheme(stage.getScene(), "dark");
        syncMapTheme();
    }

    @FXML
    private void onSetLightTheme() {
        ThemeManager.setTheme(stage.getScene(), "light");
        syncMapTheme();
    }

    private void syncMapTheme() {
        boolean dark = ThemeManager.isDark();
        if (amapView != null) amapView.setDark(dark);
        if (flAmapView != null) flAmapView.setDark(dark);
    }

    @FXML
    private void onAddWaypoint() {
        addWaypoint(drone.getLat(), drone.getLon());
    }

    private void addWaypoint(double lat, double lon) {
        try {
            Waypoint wp = new Waypoint(lat, lon, 100, 0, ActionMode.CRUISE, 3);
            mission.addWaypoint(wp);
            missionDirty = true;
            refreshWaypointList();
            updateMap();
            updateMissionStats();
            updateUploadStatus();
            updateWpCount();
            mapCanvas.fitWaypoints();
            if (amapView != null) amapView.fitWaypoints();
        } catch (IllegalArgumentException e) {
            showToast("坐标越界: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void onDeleteWaypoint() {
        int idx = wpListView != null ? wpListView.getSelectionModel().getSelectedIndex() : -1;
        if (idx >= 0 && idx < mission.size()) {
            mission.removeWaypoint(idx);
            missionDirty = true;
            refreshWaypointList();
            updateMap();
            updateMissionStats();
            updateUploadStatus();
            updateWpCount();
        }
    }

    // ===== Waypoint Edit Dialog =====
    private void onEditWaypoint(int idx) {
        if (idx < 0 || idx >= mission.size()) return;
        Waypoint wp = mission.getWaypoint(idx);

        Dialog<Waypoint> dialog = new Dialog<>();
        dialog.setTitle("编辑航点 #" + (idx + 1));
        dialog.setHeaderText("修改航点属性");
        dialog.initOwner(stage);

        // Set the button types
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField latField = new TextField(String.format("%.6f", wp.getLat()));
        TextField lonField = new TextField(String.format("%.6f", wp.getLon()));
        TextField altField = new TextField(String.format("%.0f", wp.getAlt()));
        ComboBox<ActionMode> actionCombo = new ComboBox<>();
        actionCombo.getItems().addAll(ActionMode.values());
        actionCombo.setValue(wp.getAction());
        TextField holdField = new TextField(String.format("%.0f", wp.getHold()));
        ComboBox<Integer> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll(1, 2, 3, 4, 5);
        priorityCombo.setValue(wp.getPriority());

        grid.add(new Label("纬度 LAT:"), 0, 0);
        grid.add(latField, 1, 0);
        grid.add(new Label("经度 LON:"), 0, 1);
        grid.add(lonField, 1, 1);
        grid.add(new Label("高度 ALT (m):"), 0, 2);
        grid.add(altField, 1, 2);
        grid.add(new Label("飞行动作:"), 0, 3);
        grid.add(actionCombo, 1, 3);
        grid.add(new Label("停留时间 (s):"), 0, 4);
        grid.add(holdField, 1, 4);
        grid.add(new Label("优先级:"), 0, 5);
        grid.add(priorityCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double lat = Double.parseDouble(latField.getText());
                    double lon = Double.parseDouble(lonField.getText());
                    double alt = Double.parseDouble(altField.getText());
                    double hold = Double.parseDouble(holdField.getText());
                    wp.setLat(lat);
                    wp.setLon(lon);
                    wp.setAlt(alt);
                    wp.setAction(actionCombo.getValue());
                    wp.setHold(hold);
                    wp.setPriority(priorityCombo.getValue());
                    return wp;
                } catch (NumberFormatException e) {
                    showToast("输入格式错误，请检查数值", "error");
                    return null;
                } catch (IllegalArgumentException e) {
                    showToast("参数错误: " + e.getMessage(), "error");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            missionDirty = true;
            refreshWaypointList();
            updateMap();
            updateMissionStats();
            updateUploadStatus();
            showToast("航点 #" + (idx + 1) + " 已更新", "success");
        });
    }

    // ===== Context Menus =====
    private void showMapContextMenu(double lat, double lon) {
        ContextMenu menu = new ContextMenu();

        MenuItem addWpItem = new MenuItem("➕ 在此添加航点");
        addWpItem.setOnAction(e -> addWaypoint(lat, lon));

        MenuItem fitItem = new MenuItem("🎯 适配视图");
        fitItem.setOnAction(e -> onFitWaypoints());

        MenuItem followItem = new MenuItem(
            (mapCanvas.isFollowEnabled() ? "✓ " : "") + "📍 跟随无人机"
        );
        followItem.setOnAction(e -> {
            boolean newState = !mapCanvas.isFollowEnabled();
            mapCanvas.setFollowEnabled(newState);
            if (btnFollow != null) btnFollow.setSelected(newState);
        });

        SeparatorMenuItem sep1 = new SeparatorMenuItem();

        Menu importMenu = new Menu("📥 导入");
        MenuItem importXmlItem = new MenuItem("导入 XML...");
        importXmlItem.setOnAction(e -> onImportXml());
        importMenu.getItems().add(importXmlItem);

        Menu exportMenu = new Menu("📤 导出");
        MenuItem exportXmlItem = new MenuItem("导出 XML...");
        exportXmlItem.setOnAction(e -> onExportXml());
        exportMenu.getItems().add(exportXmlItem);

        SeparatorMenuItem sep2 = new SeparatorMenuItem();

        MenuItem airspaceItem = new MenuItem(
            (btnAirspace != null && btnAirspace.isSelected() ? "✓ " : "") + "🛡 空域图层"
        );
        airspaceItem.setOnAction(e -> onToggleAirspaceLayer());

        menu.getItems().addAll(addWpItem, fitItem, followItem, sep1,
            importMenu, exportMenu, sep2, airspaceItem);
        double sx = mapCanvas.getLocalToSceneTransform().getTx() + mapCanvas.getScene().getX()
            + mapCanvas.getScene().getWindow().getX() + mapCanvas.getWidth() / 2;
        double sy = mapCanvas.getLocalToSceneTransform().getTy() + mapCanvas.getScene().getY()
            + mapCanvas.getScene().getWindow().getY() + mapCanvas.getHeight() / 2;
        menu.show(mapCanvas.getScene().getWindow(), sx, sy);
    }

    private void showWaypointContextMenu(int idx) {
        ContextMenu menu = new ContextMenu();

        MenuItem editItem = new MenuItem("✎ 编辑航点");
        editItem.setOnAction(e -> onEditWaypoint(idx));

        MenuItem deleteItem = new MenuItem("🗑 删除航点");
        deleteItem.setStyle("-fx-text-fill: #ef4444;");
        deleteItem.setOnAction(e -> {
            if (idx >= 0 && idx < mission.size()) {
                mission.removeWaypoint(idx);
                missionDirty = true;
                refreshWaypointList();
                updateMap();
                updateMissionStats();
                updateUploadStatus();
                updateWpCount();
                showToast("已删除航点 #" + (idx + 1), "info");
            }
        });

        SeparatorMenuItem sep = new SeparatorMenuItem();

        MenuItem insertBefore = new MenuItem("⬆ 在上方插入");
        insertBefore.setOnAction(e -> {
            Waypoint wp = mission.getWaypoint(idx);
            Waypoint newWp = new Waypoint(wp.getLat() + 0.0005, wp.getLon() + 0.0005,
                wp.getAlt(), wp.getHold(), wp.getAction(), wp.getPriority());
            mission.getWaypoints().add(idx, newWp);
            missionDirty = true;
            refreshWaypointList();
            updateMap();
            updateMissionStats();
            updateUploadStatus();
            updateWpCount();
        });

        MenuItem insertAfter = new MenuItem("⬇ 在下方插入");
        insertAfter.setOnAction(e -> {
            Waypoint wp = mission.getWaypoint(idx);
            Waypoint newWp = new Waypoint(wp.getLat() - 0.0005, wp.getLon() - 0.0005,
                wp.getAlt(), wp.getHold(), wp.getAction(), wp.getPriority());
            mission.getWaypoints().add(idx + 1, newWp);
            missionDirty = true;
            refreshWaypointList();
            updateMap();
            updateMissionStats();
            updateUploadStatus();
            updateWpCount();
        });

        menu.getItems().addAll(editItem, deleteItem, sep, insertBefore, insertAfter);
        double sx2 = mapCanvas.getLocalToSceneTransform().getTx() + mapCanvas.getScene().getX()
            + mapCanvas.getScene().getWindow().getX() + mapCanvas.getWidth() / 2;
        double sy2 = mapCanvas.getLocalToSceneTransform().getTy() + mapCanvas.getScene().getY()
            + mapCanvas.getScene().getWindow().getY() + mapCanvas.getHeight() / 2;
        menu.show(mapCanvas.getScene().getWindow(), sx2, sy2);
    }

    @FXML
    private void onImportXml() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML", "*.xml"));
        File file = fc.showOpenDialog(stage);
        if (file == null) return;
        try {
            Mission loaded = MissionXmlCodec.load(file);
            mission.clearWaypoints();
            for (Waypoint wp : loaded.getWaypoints()) mission.addWaypoint(wp);
            mission.setDroneModel(loaded.getDroneModel());
            mission.setMaxRange(loaded.getMaxRange());
            missionDirty = false;
            refreshWaypointList();
            updateMap();
            updateMissionStats();
            updateUploadStatus();
            updateWpCount();
            showToast("导入成功 · 已导入 " + mission.size() + " 个航点", "success");
        } catch (Exception e) {
            showToast("导入失败: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void onExportXml() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML", "*.xml"));
        fc.setInitialFileName("mission.xml");
        File file = fc.showSaveDialog(stage);
        if (file == null) return;
        try {
            MissionXmlCodec.save(mission, file);
            showToast("导出成功 · 已保存到 " + file.getName(), "success");
        } catch (Exception e) {
            showToast("导出失败: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void onPreviewPath() {
        if (mission.size() < 2) { showToast("路径预览 · 至少需要 2 个航点", "warn"); return; }
        showToast("路径预览 · 航点序列动画预览已启动", "info");
        new Timer().scheduleAtFixedRate(new TimerTask() {
            int idx = 0;
            @Override
            public void run() {
                if (idx >= mission.size()) { cancel(); return; }
                Waypoint wp = mission.getWaypoint(idx);
                Platform.runLater(() -> {
                    mapCanvas.setDronePosition(wp.getLat(), wp.getLon());
                    mapCanvas.fitWaypoints();
                    if (amapView != null) {
                        amapView.setDronePosition(wp.getLat(), wp.getLon(), 0, wp.getAlt());
                        amapView.fitWaypoints();
                    }
                });
                idx++;
            }
        }, 0, 500);
    }

    @FXML
    private void onCheckConflicts() {
        List<String> violations = ConflictChecker.checkAll(
            mission.getWaypoints(), mission.getMaxRange(), mission.getWaypoint(0));
        if (conflictLabel != null) {
            if (violations.isEmpty()) {
                conflictLabel.setText("无冲突");
                conflictLabel.setStyle("-fx-text-fill: #22c55e;");
                showToast("冲突检测 · 未发现冲突", "success");
            } else {
                conflictLabel.setText(String.join("; ", violations));
                conflictLabel.setStyle("-fx-text-fill: #ef4444;");
                showToast("冲突检测 · 发现 " + violations.size() + " 项问题", "warn");
            }
        }
    }

    @FXML
    private void onAirspaceCheck() {
        AirspaceService.ValidationResult vr = airspace.validateMission(mission.getWaypoints());
        if (airspaceStatusLabel != null) {
            airspaceStatusLabel.getStyleClass().clear();
            airspaceStatusLabel.getStyleClass().add("aspace-status");
            airspaceStatusLabel.getStyleClass().add(vr.result.name().toLowerCase());
            airspaceStatusLabel.setText(vr.result + (vr.message.isEmpty() ? "" : ": " + vr.message));
            String level = switch (vr.result) {
                case PASS -> "success";
                case WARN -> "warn";
                case BLOCK -> "error";
                default -> "info";
            };
            showToast("空域校验 · " + vr.result, level);
        }
        mapCanvas.setViolatingSegments(vr.violatingSegments);
    }

    @FXML
    private void onOptimizePath() {
        if (mission.size() < 3) { showToast("路径优化 · 至少需要 3 个航点", "warn"); return; }
        double origDist = PathOptimizer.totalDistance(mission.getWaypoints());
        List<Waypoint> optimized = PathOptimizer.optimize(
            mission.getWaypoints().subList(1, mission.size()), mission.getWaypoint(0));
        List<Waypoint> newOrder = new ArrayList<>();
        newOrder.add(mission.getWaypoint(0));
        newOrder.addAll(optimized);
        mission.clearWaypoints();
        for (Waypoint wp : newOrder) mission.addWaypoint(wp);
        missionDirty = true;
        refreshWaypointList();
        updateMap();
        updateMissionStats();
        updateUploadStatus();
        updateWpCount();
        double optDist = PathOptimizer.totalDistance(mission.getWaypoints());
        updateAlgoStatsUI(origDist, optDist);
        showToast("路径优化完成 · 优化率 " + String.format("%.1f%%", (origDist > 0 ? (1 - optDist / origDist) * 100 : 0)), "success");
    }

    private void updateAlgoStats() {
        if (mission.size() >= 2) {
            double origDist = PathOptimizer.totalDistance(mission.getWaypoints());
            updateAlgoStatsUI(origDist, origDist);
        }
    }

    private void updateAlgoStatsUI(double origDist, double optDist) {
        if (algoOrigDist != null) algoOrigDist.setText(String.format("%.2f km", origDist / 1000));
        if (algoOptDist != null) algoOptDist.setText(String.format("%.2f km", optDist / 1000));
        // 飞行距离由当前任务总航程自动回填（只读，与 HTML 原型一致）
        if (distInput != null) distInput.setText(String.format("%.2f", optDist / 1000));
        double rate = origDist > 0 ? (1 - optDist / origDist) * 100 : 0;
        if (algoWpCount != null) algoWpCount.setText(String.valueOf(mission.size()));
        if (algoDelta != null) algoDelta.setText(String.format("%.2f km", (origDist - optDist) / 1000));
        if (algoRate != null) {
            algoRate.setText(String.format("%.1f%%", rate));
            algoRate.getStyleClass().clear();
            algoRate.getStyleClass().add("algo-stat-val");
            algoRate.getStyleClass().add(rate >= 0 ? "improved" : "worse");
        }
    }

    @FXML
    private void onPredictBattery() {
        try {
            double dist = Double.parseDouble(distInput.getText());
            double load = Double.parseDouble(loadInput.getText());
            double wind = Double.parseDouble(windInput.getText());
            double[] result = batteryPredictor.predict(dist, load, wind);
            enduranceResult.setText(String.format("%.1f min", result[0]));
            costResult.setText(String.format("%.1f%%", result[1]));
        } catch (NumberFormatException e) {
            showToast("输入错误 · 请输入有效数字", "error");
        }
    }

    @FXML
    private void onFetchWindApi() {
        showToast("正在获取 Open-Meteo 实时风速数据...", "info");
        double lat = drone.isPositionAvailable() ? drone.getLat() : 32.06;
        double lon = drone.isPositionAvailable() ? drone.getLon() : 118.79;
        String apiUrl = String.format(
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=wind_speed_10m,wind_direction_10m", lat, lon);
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(apiUrl))
            .timeout(java.time.Duration.ofSeconds(8))
            .GET()
            .build();
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .build();
        client.sendAsync(req, java.net.http.HttpResponse.BodyHandlers.ofString())
            .thenApply(java.net.http.HttpResponse::body)
            .thenAccept(body -> {
                Platform.runLater(() -> {
                    try {
                        int idx = body.indexOf("\"wind_speed_10m\":");
                        int idxDir = body.indexOf("\"wind_direction_10m\":");
                        if (idx >= 0) {
                            String sub = body.substring(idx + 17);
                            String valStr = sub.split("[,}\\s]")[0].trim();
                            double windSpeed = Double.parseDouble(valStr);
                            String dirText = "";
                            if (idxDir >= 0) {
                                String subDir = body.substring(idxDir + 23);
                                String dirStr = subDir.split("[,}\\s]")[0].trim();
                                try {
                                    double dir = Double.parseDouble(dirStr);
                                    dirText = " · 风向 " + windDirName(dir) + " " + Math.round(dir) + "°";
                                } catch (NumberFormatException ignored) {}
                            }
                            if (windInput != null) {
                                windInput.setText(String.format("%.1f", windSpeed));
                            }
                            if (windInfoLabel != null) {
                                windInfoLabel.setText("Open-Meteo 实时数据 · " + String.format("%.1f", windSpeed) + " m/s" + dirText + " [API]");
                            }
                            showToast(String.format("风速获取成功 %.1f m/s [API]", windSpeed), "success");
                        } else {
                            fallbackWind("API 响应格式异常");
                        }
                    } catch (Exception e) {
                        fallbackWind("解析异常: " + e.getMessage());
                    }
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> fallbackWind("网络请求失败"));
                return null;
            });
    }

    private void fallbackWind(String reason) {
        double windSpeed = 3.0;
        if (windInput != null) {
            windInput.setText(String.format("%.1f", windSpeed));
        }
        if (windInfoLabel != null) {
            windInfoLabel.setText("实时气象获取失败，使用默认估算风速 " + String.format("%.1f", windSpeed) + " m/s（" + reason + "）");
        }
        showToast(String.format("风速估算 %.1f m/s（%s）", windSpeed, reason), "warn");
    }

    /** 气象风向角（度，北风=0，顺时针）转中文方位 */
    private String windDirName(double deg) {
        String[] names = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};
        int i = (int) Math.round(deg / 45.0) % 8;
        return names[i] + "风";
    }

    @FXML
    private void onToggleTheme() {
        ThemeManager.toggle(stage.getScene());
        syncMapTheme();
    }

    @FXML
    private void onSaveSettings() {
        // 将设置页字段值回写到 settings 对象
        if (e1IpField != null) settings.e1Ip = e1IpField.getText();
        if (e1PortField != null) {
            try { settings.e1Port = Integer.parseInt(e1PortField.getText()); }
            catch (NumberFormatException ignored) {}
        }
        if (hbTimeoutField != null) {
            try { settings.heartbeatTimeout = Integer.parseInt(hbTimeoutField.getText().trim()); }
            catch (NumberFormatException ignored) {}
        }
        if (phoneGpsToggle != null) settings.phoneGps = phoneGpsToggle.isSelected();
        if (phoneCompassToggle != null) settings.phoneCompass = phoneCompassToggle.isSelected();
        if (phoneBaroToggle != null) settings.phoneBaro = phoneBaroToggle.isSelected();
        if (droneRangeField != null) {
            try { settings.droneMaxRange = Double.parseDouble(droneRangeField.getText()); }
            catch (NumberFormatException ignored) {}
        }
        settings.save();
        // 刷新左侧 E1/手机状态区，使新设置即时生效
        updateE1StatusDetail();
        updatePhoneStatusDetail();
        updateConnStatus();
        showToast("设置已保存", "success");
    }

    /** 设置页运行模式下拉即时联动 —— 切换 E1/手机模式 */
    @FXML
    private void onSettingsRunModeChanged() {
        if (setRunMode == null) return;
        int idx = setRunMode.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        String mode = (idx == 0) ? "e1_real" : "phone_assist";
        if (!mode.equals(settings.runMode)) {
            settings.runMode = mode;
            settings.save();
            if ("e1_real".equals(mode)) {
                if (e1Btn != null) e1Btn.setSelected(true);
                if (phoneBtn != null) phoneBtn.setSelected(false);
            } else {
                if (e1Btn != null) e1Btn.setSelected(false);
                if (phoneBtn != null) phoneBtn.setSelected(true);
            }
            applyModeGating();
            showToast("已切换至" + ("e1_real".equals(mode) ? "E1 实机" : "手机辅助") + "模式", "info");
        }
    }

    /** 空域数据文件导入 —— 打开文件选择器 */
    @FXML
    private void onImportAirspaceFile() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("选择空域数据文件");
        String srcType = aspaceSrcType != null ? aspaceSrcType.getValue() : "GeoJSON";
        fc.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter(srcType + " 文件", "*." + srcType.toLowerCase().replace("shapefile", "shp")));
        fc.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("所有文件", "*.*"));
        java.io.File file = fc.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            showToast("已选择文件: " + file.getName() + "（解析功能开发中）", "info");
        }
    }

    /** 系统设置菜单 onShowing —— 消费事件防空弹出，直接跳转 */
    @FXML
    private void onMenuSettingsShowing(javafx.event.Event event) {
        onGoSettingsTab();
        event.consume();
    }

    @FXML
    private void onLoadDemoAirspace() {
        AirspaceService.AirspacePackage pkg = new AirspaceService.AirspacePackage();
        pkg.id = "demo-nj-" + System.currentTimeMillis();
        pkg.name = "南京仙林空域数据包";
        pkg.coverage = "南京市栖霞区仙林片区";
        pkg.version = "2026.09 v1.0";
        pkg.updatedAt = "2026-09-01";
        pkg.expiresAt = "2026-12-31";
        pkg.zones.add(createZone("军事管制区A", AirspaceService.ZoneType.NOFLY, 0,
            new double[][]{{118.797,32.063},{118.802,32.063},{118.802,32.058},{118.797,32.058}}));
        pkg.zones.add(createZone("栖霞限飞区", AirspaceService.ZoneType.RESTRICTION, 120,
            new double[][]{{118.780,32.070},{118.810,32.070},{118.810,32.045},{118.780,32.045}}));
        pkg.zones.add(createZone("仙林临时管制区", AirspaceService.ZoneType.TEMPORARY, 150,
            new double[][]{{118.785,32.065},{118.800,32.065},{118.800,32.055},{118.785,32.055}}));
        pkg.zones.add(createZone("用户测试围栏", AirspaceService.ZoneType.CUSTOM, 200,
            new double[][]{{118.785,32.065},{118.800,32.065},{118.800,32.050},{118.785,32.050}}));
        airspace.addPackage(pkg);
        mapCanvas.setAirspaceZones(airspace.getActiveZones());
        if (amapView != null) amapView.setAirspaceZones(airspace.getActiveZones());
        refreshAirspacePkgList();
        showToast("已加载南京仙林示例空域数据包（4 区域）", "success");
    }

    private AirspaceService.Zone createZone(String name, AirspaceService.ZoneType type, double maxAlt, double[][] coords) {
        AirspaceService.Zone z = new AirspaceService.Zone();
        z.name = name; z.type = type; z.maxAlt = maxAlt > 0 ? maxAlt : null;
        z.coords = new ArrayList<>();
        for (double[] c : coords) z.coords.add(c);
        if (type == AirspaceService.ZoneType.TEMPORARY) {
            z.timeStart = System.currentTimeMillis() - 86400000;
            z.timeEnd = System.currentTimeMillis() + 86400000;
        }
        return z;
    }

    private void refreshAirspacePkgList() {
        if (aspacePkgList == null) return;
        ObservableList<String> items = FXCollections.observableArrayList();
        for (AirspaceService.AirspacePackage pkg : airspace.getPackages())
            items.add(pkg.name + " (" + pkg.zones.size() + " 区域)");
        aspacePkgList.setItems(items);
    }

    // ===== Enhanced Waypoint List (H-04) =====
    private void refreshWaypointList() {
        if (wpListView == null) return;
        ObservableList<Waypoint> wps = FXCollections.observableArrayList(mission.getWaypoints());
        wpListView.setItems(wps);
        wpListView.setCellFactory(lv -> new ListCell<>() {
            private HBox root;
            private Label seqLabel;
            private Label coordsLabel;
            private Label altLabel;
            private Label actLabel;
            private Button delBtn;

            {
                root = new HBox(8);
                root.getStyleClass().add("wp-item");

                seqLabel = new Label();
                seqLabel.getStyleClass().add("wp-seq");
                seqLabel.setMinSize(18, 18);
                seqLabel.setAlignment(javafx.geometry.Pos.CENTER);

                VBox infoBox = new VBox(2);
                infoBox.getStyleClass().add("wp-info");
                HBox.setHgrow(infoBox, Priority.ALWAYS);

                coordsLabel = new Label();
                coordsLabel.getStyleClass().add("wp-coords");

                HBox metaBox = new HBox(8);
                metaBox.getStyleClass().add("wp-meta");
                altLabel = new Label();
                altLabel.getStyleClass().add("wp-meta-val");
                actLabel = new Label();
                actLabel.getStyleClass().add("wp-act-tag");
                metaBox.getChildren().addAll(
                    new Label("H=") {{ getStyleClass().add("wp-meta-label"); }},
                    altLabel,
                    actLabel
                );

                infoBox.getChildren().addAll(coordsLabel, metaBox);

                Button editBtn = new Button("✎");
                editBtn.getStyleClass().add("wp-edit-btn");
                editBtn.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < mission.size()) {
                        onEditWaypoint(idx);
                    }
                });

                delBtn = new Button("✕");
                delBtn.getStyleClass().add("wp-del-btn");
                delBtn.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < mission.size()) {
                        mission.removeWaypoint(idx);
                        missionDirty = true;
                        refreshWaypointList();
                        updateMap();
                        updateMissionStats();
                        updateUploadStatus();
                        updateWpCount();
                    }
                });

                root.getChildren().addAll(seqLabel, infoBox, editBtn, delBtn);
                root.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                // Double-click to edit
                root.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) {
                        int idx = getIndex();
                        if (idx >= 0 && idx < mission.size()) {
                            onEditWaypoint(idx);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Waypoint wp, boolean empty) {
                super.updateItem(wp, empty);
                if (empty || wp == null) {
                    setGraphic(null);
                } else {
                    seqLabel.setText(String.valueOf(getIndex() + 1));
                    coordsLabel.setText(String.format("%.5f, %.5f", wp.getLat(), wp.getLon()));
                    altLabel.setText(String.format("%.0fm", wp.getAlt()));
                    actLabel.setText(wp.getAction().name());
                    setGraphic(root);
                }
            }
        });
    }

    private void updateMap() {
        mapCanvas.setWaypoints(mission.getWaypoints());
        if (amapView != null) amapView.setWaypoints(mission.getWaypoints());
    }

    private void updateMissionStats() {
        double total = mission.totalDistance();
        if (totalDistLabel != null)
            totalDistLabel.setText(String.format("%.2f km", total / 1000));
        double speed = 5.0;
        if (estTimeLabel != null)
            estTimeLabel.setText(String.format("%.1f min", total / 1000 / speed * 60));
    }

    private void updateUploadStatus() {
        if (uploadStatusBadge == null) return;
        uploadStatusBadge.getStyleClass().clear();
        uploadStatusBadge.getStyleClass().add("upload-status");
        if (missionDirty) {
            uploadStatusBadge.setText("未同步");
            uploadStatusBadge.getStyleClass().add("dirty");
        } else {
            uploadStatusBadge.setText("已同步");
            uploadStatusBadge.getStyleClass().add("synced");
        }
    }

    private void updateWpCount() {
        int count = mission.size();
        if (wpCountLabel != null) wpCountLabel.setText(count + " 航点");
        if (sbWp != null) sbWp.setText(count + "/" + count);
    }

    private void startClock() {
        Timer clockTimer = new Timer(true);
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    String timeStr = new Date().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime().toString();
                    if (clockLabel != null) clockLabel.setText(timeStr);
                    if (sbLastUpdate != null) sbLastUpdate.setText(timeStr);
                });
            }
        }, 0, 1000);
    }

    private Timer e1ConnTimer;

    private void connectE1Serial() {
        String port = settings.serialPort;
        System.out.println("[E1] 连接串口: " + port + " @ " + settings.baudRate);
        // 切换模式时清理旧数据与可用性标识，确保 E1 模式仅显示飞控真实上报的数据
        drone.markDisconnected();
        paramService.clear();
        paramListRequested = false;
        if (e1ConnTimer != null) { e1ConnTimer.cancel(); e1ConnTimer = null; }
        comm.stop();
        updateDroneStatus();
        if (port == null || port.startsWith("UDP")) {
            int localPort = 14550;
            String remoteHost = "127.0.0.1";
            int remotePort = 14550;
            comm.setLink(new UdpLink(localPort, remoteHost, remotePort));
        } else {
            comm.setLink(new SerialLink(port, settings.baudRate));
        }
        comm.setCallback(t -> Platform.runLater(() -> onTelemetry(t)));
        comm.start();
        updateConnStatus();

        e1ConnTimer = new Timer(true);
        e1ConnTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> updateConnStatus());
            }
        }, 0, 1000);
    }

    private void disconnectSerial() {
        if (e1ConnTimer != null) { e1ConnTimer.cancel(); e1ConnTimer = null; }
        comm.stop();
        drone.markDisconnected();
        paramService.clear();
        paramListRequested = false;
        updateConnStatus();
        updateDroneStatus();
        updateE1StatusDetail();
        updatePhoneStatusDetail();
    }

    @SuppressWarnings("unchecked")
    private void updateDroneStatus() {
        if (droneStatusGrid == null) return;
        // 诚实标注：飞控/固件不上报的数据用 "—" 表示，避免默认值 0 误导用户
        boolean posOk = drone.isPositionAvailable();
        boolean gpsOk = drone.isGpsAvailable();
        boolean vfrOk = drone.isVfrHudAvailable();
        boolean attOk = drone.isAttitudeAvailable();
        boolean battOk = drone.isBatteryAvailable();
        boolean hbOk = drone.isHeartbeatAvailable();
        boolean rcOk = drone.isRcAvailable();
        boolean landOk = drone.isLandedStateAvailable();
        // 速度/高度/航向：GLOBAL_POSITION_INT 优先，回退 VFR_HUD
        boolean spdOk = posOk || vfrOk;
        Object[] values = {
            hbOk ? drone.getCustomModeName() : "—", "info",
            landOk ? drone.getLandedStateName() : "—", landOk && drone.getLandedState() == 1 ? "off" : "info",
            posOk ? drone.getLat() : "—", "",
            posOk ? drone.getLon() : "—", "",
            (posOk || vfrOk) ? drone.getAlt() : "—", "",
            spdOk ? drone.getSpeed() : "—", "",
            spdOk ? drone.getHeading() : "—", "",
            attOk ? drone.getRoll() : "—", "",
            attOk ? drone.getPitch() : "—", "",
            attOk ? drone.getYaw() : "—", "",
            battOk ? drone.getBatteryPct() : "—", "battery",
            battOk ? drone.getVoltage() : "—", "",
            rcOk ? drone.getRssi() : "—", "ok",
            gpsOk ? drone.getSatellites() : "—", "ok",
            gpsOk ? drone.getHdop() : "—", "ok",
            hbOk ? (drone.isArmed() ? "已解锁" : "未解锁") : "—", drone.isArmed() ? "ok" : "off"
        };
        String[] formats = {
            "%s", "%s",
            "%s", "%s",
            "%.5f°", "%s",
            "%.5f°", "%s",
            "%.1f m", "%s",
            "%.1f m/s", "%s",
            "%.0f°", "%s",
            "%.1f°", "%s",
            "%.1f°", "%s",
            "%.0f°", "%s",
            "%.0f%%", "battery",
            "%.1fV", "%s",
            "%.0f%%", "ok",
            "%d", "ok",
            "%.1f", "ok",
            "%s", "%s"
        };
        for (int i = 0; i < 16; i++) {
            int col = i % 2;
            int row = i / 2;
            final int idx = i;
            var cellNode = droneStatusGrid.getChildren().stream()
                .filter(n -> GridPane.getColumnIndex(n) != null && GridPane.getColumnIndex(n) == col
                    && GridPane.getRowIndex(n) != null && GridPane.getRowIndex(n) == row
                    && n instanceof VBox)
                .findFirst().orElse(null);
            if (cellNode instanceof VBox cell && cell.getChildren().size() >= 2) {
                var valNode = cell.getChildren().get(1);
                Label val = null;
                if (valNode instanceof Label) {
                    val = (Label) valNode;
                } else if (valNode instanceof HBox hbox && hbox.getChildren().size() >= 1 && hbox.getChildren().get(0) instanceof Label) {
                    val = (Label) hbox.getChildren().get(0);
                }
                if (val != null) {
                    Object v = values[i * 2];
                    String fmt = formats[i * 2];
                    String stateClass = (String) values[i * 2 + 1];
                    // String 类型（如 "—"）直接显示，避免被 "%.5f°" 等格式串误转
                    if (v instanceof String s) {
                        val.setText(s);
                    } else if (v instanceof Double d) {
                        val.setText(String.format(fmt, d));
                    } else if (v instanceof Integer in) {
                        val.setText(String.format(fmt, in));
                    } else {
                        val.setText(String.format(fmt, v));
                    }
                    // Update style class
                    val.getStyleClass().removeIf(s -> s.equals("ok") || s.equals("warn") || s.equals("err") || s.equals("info") || s.equals("off") || s.equals("disabled"));
                    // Battery special handling
                    if ("battery".equals(stateClass)) {
                        if (drone.isBatteryAvailable()) {
                            double batt = drone.getBatteryPct();
                            if (batt <= 20) val.getStyleClass().add("err");
                            else if (batt <= 40) val.getStyleClass().add("warn");
                            else val.getStyleClass().add("ok");
                        } else {
                            val.getStyleClass().add("disabled");
                        }
                    } else if (!stateClass.isEmpty() && !"%s".equals(stateClass)) {
                        val.getStyleClass().add(stateClass);
                    }
                }
            }
        }
        // 状态栏诚实标注：位置不可用时显示 "—"
        if (sbLat != null) sbLat.setText(posOk ? String.format("%.5f", drone.getLat()).substring(0, Math.min(8, String.format("%.5f", drone.getLat()).length())) : "—");
        if (sbLon != null) sbLon.setText(posOk ? String.format("%.5f", drone.getLon()).substring(0, Math.min(9, String.format("%.5f", drone.getLon()).length())) : "—");
        if (sbGps != null) sbGps.setText(gpsOk ? (drone.getFixTypeName() + "·" + drone.getSatellites()) : "—");
        if (sbHdop != null) sbHdop.setText(gpsOk ? String.format("%.1f", drone.getHdop()) : "—");

        // ================================================================
        // P1/P2 新增：电池详情（电流/温度/电芯/剩余时间） + 姿态仪表可用性标签
        // ================================================================
        boolean bsOk = drone.isBatteryStatusAvailable();
        setBatteryDetail(lblBatCurrent, bsOk,
            String.format(Locale.ROOT, "%.2f A", drone.getBatteryCurrent()),
            drone.getBatteryCurrent() >= 6.0 ? "err" : (drone.getBatteryCurrent() >= 3.0 ? "warn" : "ok"));
        setBatteryDetail(lblBatTemp, bsOk,
            String.format(Locale.ROOT, "%.1f °C", drone.getBatteryTemp()),
            drone.getBatteryTemp() >= 55.0 ? "err" : (drone.getBatteryTemp() >= 45.0 ? "warn" : "ok"));
        if (lblCells != null) {
            int[] cv = drone.getCellVoltages();
            int count = 0;
            int minMv = Integer.MAX_VALUE;
            int maxMv = Integer.MIN_VALUE;
            for (int x : cv) {
                if (x > 0) {
                    count++;
                    if (x < minMv) minMv = x;
                    if (x > maxMv) maxMv = x;
                }
            }
            if (!bsOk || count == 0) {
                lblCells.setText("—");
                lblCells.getStyleClass().removeAll("ok", "warn", "err", "off");
                lblCells.getStyleClass().add("off");
            } else {
                int diff = maxMv - minMv;
                lblCells.setText(String.format(Locale.ROOT, "%dS %d/%d mV", count, minMv, maxMv));
                lblCells.getStyleClass().removeAll("ok", "warn", "err", "off");
                if (diff <= 15) lblCells.getStyleClass().add("ok");
                else if (diff <= 50) lblCells.getStyleClass().add("warn");
                else lblCells.getStyleClass().add("err");
            }
        }
        if (lblBatTimeLeft != null) {
            double sec = batteryPredictor.predictDynamic();
            if (!Double.isFinite(sec) || sec <= 0) {
                int have = batteryPredictor.sampleCount();
                if (have < 2) {
                    lblBatTimeLeft.setText("观测中(" + have + "/" + 2 + ")");
                } else {
                    lblBatTimeLeft.setText("—");
                }
                lblBatTimeLeft.getStyleClass().removeAll("ok", "warn", "err", "off");
                lblBatTimeLeft.getStyleClass().add("off");
            } else {
                long m = (long) (sec / 60);
                long s = (long) (sec % 60);
                lblBatTimeLeft.setText(String.format("%02d:%02d", m, s));
                lblBatTimeLeft.getStyleClass().removeAll("ok", "warn", "err", "off");
                if (sec >= 600) lblBatTimeLeft.getStyleClass().add("ok");
                else if (sec >= 240) lblBatTimeLeft.getStyleClass().add("warn");
                else lblBatTimeLeft.getStyleClass().add("err");
            }
        }
        // 姿态不可用标签
        if (attitudeUnavailableLabel != null) {
            attitudeUnavailableLabel.setVisible(!attOk);
            attitudeUnavailableLabel.setManaged(!attOk);
            attitudeUnavailableLabel.setText(attOk ? "" : "姿态数据未接入 [E1]");
        }
    }

    private void setBatteryDetail(Label lbl, boolean ok, String text, String stateIfOk) {
        if (lbl == null) return;
        if (!ok) {
            lbl.setText("—");
            lbl.getStyleClass().removeAll("ok", "warn", "err", "off");
            lbl.getStyleClass().add("off");
            return;
        }
        lbl.setText(text);
        lbl.getStyleClass().removeAll("ok", "warn", "err", "off");
        lbl.getStyleClass().add(stateIfOk);
    }

    // ===== Menu / Navigation =====
    @FXML
    private void onGoMissionTab() { if (workspaceTabs != null) workspaceTabs.getSelectionModel().select(missionTab); }
    @FXML
    private void onGoDataTab() { if (workspaceTabs != null) workspaceTabs.getSelectionModel().select(dataTab); }
    @FXML
    private void onGoSettingsTab() { if (workspaceTabs != null) workspaceTabs.getSelectionModel().select(settingsTab); }
    @FXML
    private void onGoAlgoTab() { if (workspaceTabs != null) workspaceTabs.getSelectionModel().select(algoTab); }
    @FXML
    private void onGoChargingTab() { if (workspaceTabs != null && chargingTab != null) workspaceTabs.getSelectionModel().select(chargingTab); }

    // ===== Battery Charging Workspace =====
    /** 请求飞控上报电池遥测（BATTERY_STATUS/SYS_STATUS/HIGHRES_IMU）。 */
    @FXML
    private void onChargingRequestBattery() {
        if (comm == null || !comm.isLinkActive()) {
            showToast("飞控链路未连接，无法请求电池遥测", "warn");
            return;
        }
        batteryPredictor.reset();
        comm.requestBatteryStatus();
        comm.requestMessage(1);
        comm.requestMessage(105);
        showToast("已请求 BATTERY_STATUS(147) / SYS_STATUS(1) / HIGHRES_IMU(105)", "info");
    }

    /** 立即刷新电池充电工作区（手动触发）。 */
    @FXML
    private void onChargingRefresh() {
        updateChargingWorkspace(true);
        showToast("已刷新电池充电数据", "info");
    }

    /**
     * 刷新电池充电工作区 UI。从 flushUiUpdates() 每 100ms 调用，
     * 仅在 chargingTab 可见时执行重 UI 构建，否则只更新后端状态条。
     */
    private void updateChargingWorkspace() {
        updateChargingWorkspace(false);
    }

    private void updateChargingWorkspace(boolean force) {
        if (chargingTab == null) return;
        boolean visible = force || (workspaceTabs != null
                && workspaceTabs.getSelectionModel().getSelectedItem() == chargingTab);
        BatteryChargingMonitor.Snapshot snap = chargingMonitor.snapshot();
        if (snap == null) return;

        // 后端状态条（始终更新，开销小）
        updateChargingBackendBar(snap);

        if (!visible) return;

        // 摘要卡片
        List<BatteryChargingMonitor.BatteryInfo> connected = new ArrayList<>();
        int chargingCount = 0, fullCount = 0;
        double totalPower = 0, capSum = 0;
        int minEta = Integer.MAX_VALUE;
        for (BatteryChargingMonitor.BatteryInfo b : snap.batteries) {
            if (!b.connected) continue;
            connected.add(b);
            if (b.charging) chargingCount++;
            if (b.capacity >= 100) fullCount++;
            if (b.power != null) totalPower += b.power;
            else if (b.voltage > 0 && b.current > 0) totalPower += b.voltage * b.current;
            if (b.capacity >= 0) capSum += b.capacity;
            if (b.charging && b.timeToFullMin > 0 && b.timeToFullMin < minEta) minEta = b.timeToFullMin;
        }
        if (chgSumCharging != null) {
            chgSumCharging.setText(String.valueOf(chargingCount));
            chgSumCharging.getStyleClass().setAll("bat-summary-val", chargingCount > 0 ? "ok" : "");
        }
        if (chgSumChargingSub != null) chgSumChargingSub.setText("共 " + connected.size() + " 个数据源");
        if (chgSumFull != null) chgSumFull.setText(String.valueOf(fullCount));
        if (chgSumFullSub != null) chgSumFullSub.setText(fullCount > 0 ? "可拔出" : "无");
        if (chgSumPower != null) chgSumPower.setText(String.format("%.1f W", totalPower));
        if (chgSumPowerSub != null) chgSumPowerSub.setText(snap.wmiAvailable ? "系统供电" : "—");
        if (chgSumAvgCap != null)
            chgSumAvgCap.setText(connected.isEmpty() ? "--%" : Math.round(capSum / connected.size()) + "%");
        if (chgSumAvgSub != null) chgSumAvgSub.setText("所有已连接电池");
        if (chgSumEta != null) chgSumEta.setText(minEta != Integer.MAX_VALUE ? formatChgTime(minEta) : "--");
        if (chgSumEtaSub != null) chgSumEtaSub.setText(minEta != Integer.MAX_VALUE ? "剩余时间最短" : "无充电中");

        // 验证面板
        updateChargingValidation(snap);

        // 电池卡片网格
        if (chgGridHost != null) {
            chgGridHost.getChildren().setAll(buildChargingCards(snap));
        }
    }

    private void updateChargingBackendBar(BatteryChargingMonitor.Snapshot snap) {
        boolean hasReal = snap.wmiAvailable || snap.mavlinkConnected;
        String dotStyle = hasReal ? "online" : "offline";
        String text = hasReal ? "电池监控运行中" : "无可用数据源";
        StringBuilder meta = new StringBuilder();
        if (snap.wmiAvailable) meta.append("WMI:可用");
        if (snap.mavlinkConnected) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append("MAVLink:").append(snap.mavlinkPort != null ? snap.mavlinkPort : "已连接");
            if (!snap.mavlinkBatterySupported) meta.append("(无电池遥测)");
        }
        if (bbbDot != null) {
            bbbDot.getStyleClass().setAll("bbb-dot", dotStyle);
        }
        if (bbbText != null) bbbText.setText(text);
        if (bbbMeta != null) bbbMeta.setText(meta.length() > 0 ? meta.toString() : "无数据源");
    }

    private void updateChargingValidation(BatteryChargingMonitor.Snapshot snap) {
        BatteryChargingMonitor.ValidationSummary val = snap.validation;
        if (chgValidationBox != null) {
            chgValidationBox.getStyleClass().setAll("bat-validation-section",
                    "err".equals(val.status) ? "err" : "warn".equals(val.status) ? "warn" : "");
        }
        if (chgValStatus != null) {
            chgValStatus.getStyleClass().setAll("bat-validation-status", val.status);
            chgValStatus.setText("pass".equals(val.status) ? "通过"
                    : "warn".equals(val.status) ? "警告" : "异常");
        }
        if (chgValBody != null) {
            if (snap.anomalies.isEmpty()) {
                chgValBody.getChildren().setAll(new Label("✓ 所有已校验电池充电状态正常"));
                chgValBody.getChildren().get(0).getStyleClass().add("bat-validation-empty");
            } else {
                List<javafx.scene.Node> items = new ArrayList<>();
                for (BatteryChargingMonitor.Anomaly a : snap.anomalies) {
                    HBox row = new HBox(8);
                    row.getStyleClass().addAll("bat-anomaly-item", a.level);
                    Label code = new Label(a.code);
                    code.getStyleClass().add("bat-anomaly-code");
                    Label msg = new Label(a.msg);
                    msg.getStyleClass().add("bat-anomaly-msg");
                    msg.setWrapText(true);
                    row.getChildren().addAll(code, msg);
                    items.add(row);
                }
                chgValBody.getChildren().setAll(items);
            }
        }
        if (chgValChecked != null) chgValChecked.setText(String.valueOf(val.checkedBatteries));
        if (chgValAnomalies != null) chgValAnomalies.setText(String.valueOf(val.totalAnomalies));
        if (chgValErrors != null) chgValErrors.setText(String.valueOf(val.errors));
        if (chgValWarnings != null) chgValWarnings.setText(String.valueOf(val.warnings));
        if (chgValSources != null) {
            StringBuilder sb = new StringBuilder();
            for (BatteryChargingMonitor.BatteryInfo b : snap.batteries) {
                if (sb.length() > 0) sb.append("+");
                sb.append("wmi".equals(b.source) ? "WMI" : "mavlink".equals(b.source) ? "MAVLink" : b.source);
            }
            chgValSources.setText(sb.length() > 0 ? sb.toString() : "--");
        }
    }

    private List<javafx.scene.Node> buildChargingCards(BatteryChargingMonitor.Snapshot snap) {
        List<javafx.scene.Node> cards = new ArrayList<>();
        for (BatteryChargingMonitor.BatteryInfo b : snap.batteries) {
            cards.add(buildChargingCard(b));
        }
        return cards;
    }

    private VBox buildChargingCard(BatteryChargingMonitor.BatteryInfo b) {
        VBox card = new VBox();
        card.getStyleClass().addAll("bat-card", b.source);
        if (b.charging) card.getStyleClass().add("charging");
        if (b.capacity >= 100) card.getStyleClass().add("full");

        // 头部
        HBox head = new HBox(8);
        head.getStyleClass().add("bat-card-head");
        head.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(b.name);
        title.getStyleClass().add("bat-card-title");
        Label pill = new Label("wmi".equals(b.source) ? "WMI" : "MAVLink");
        pill.getStyleClass().addAll("bat-source-pill", b.source);
        head.getChildren().addAll(title, pill);
        card.getChildren().add(head);

        // 主体
        VBox body = new VBox(10);
        body.getStyleClass().add("bat-card-body");
        body.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // 容量条
        HBox capRow = new HBox(6);
        capRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Region barWrap = new Region();
        barWrap.getStyleClass().add("bat-cap-bar-wrap");
        HBox.setHgrow(barWrap, javafx.scene.layout.Priority.ALWAYS);
        Region barFill = new Region();
        barFill.getStyleClass().addAll("bat-cap-bar-fill",
                b.charging ? "charging" : b.capacity >= 100 ? "full" : "");
        double capPct = Math.max(0, Math.min(100, b.capacity >= 0 ? b.capacity : 0));
        barFill.setPrefWidth(capPct * 2.5);
        barFill.setMaxWidth(capPct * 2.5);
        // 用 StackPane 叠放进度条
        StackPane barStack = new StackPane();
        barStack.getChildren().addAll(barWrap, barFill);
        HBox.setHgrow(barStack, javafx.scene.layout.Priority.ALWAYS);
        Label capPctLabel = new Label(b.capacity >= 0 ? Math.round(b.capacity) + "%" : "--");
        capPctLabel.getStyleClass().add("bat-cap-pct");
        capRow.getChildren().addAll(barStack, capPctLabel);
        body.getChildren().add(capRow);

        // 信息行
        if (b.valid) {
            body.getChildren().add(infoRow("电池编号", b.batteryId != null ? b.batteryId : "--", null));
            body.getChildren().add(infoRow("电压", b.voltage > 0 ? String.format("%.2f V", b.voltage) : "—",
                    b.voltage > 0 ? "ok" : "pending"));
            body.getChildren().add(infoRow("电流", b.current >= 0 ? String.format("%.2f A", b.current) : "—",
                    b.current > 5 ? "err" : b.current > 3 ? "warn" : "ok"));
            if (b.power != null && b.power > 0)
                body.getChildren().add(infoRow("充电功率", String.format("%.2f W", b.power), "ok"));
            if (b.temperature != null)
                body.getChildren().add(infoRow("温度", String.format("%.1f °C", b.temperature),
                        b.temperature > 60 ? "err" : b.temperature > 45 ? "warn" : "ok"));
            else
                body.getChildren().add(infoRow("温度", "—", "pending"));
            body.getChildren().add(infoRow("电源接入", b.powerOnline ? "是" : "否",
                    b.powerOnline ? "ok" : "err"));
            body.getChildren().add(infoRow("剩余时间", b.charging && b.timeToFullMin > 0
                    ? formatChgTime(b.timeToFullMin) : "—", b.charging ? "ok" : null));
        } else if (b.note != null) {
            Label noteLabel = new Label(b.note);
            noteLabel.setWrapText(true);
            noteLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -fx-text-muted; -fx-padding: 14 0;");
            body.getChildren().add(noteLabel);
        }
        card.getChildren().add(body);

        // 状态行
        Label status = new Label(b.valid ? (b.charging ? "⚡ 充电中"
                + (b.current > 0 ? " " + String.format("%.1f", b.current) + "A" : "")
                : b.capacity >= 100 ? "✓ 已充满" : "⏸ 待机")
                : "⚙ 等待遥测");
        status.getStyleClass().add("bat-card-status");
        card.getChildren().add(status);
        return card;
    }

    private HBox infoRow(String label, String value, String styleClass) {
        HBox row = new HBox(8);
        row.getStyleClass().add("bat-info-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.getStyleClass().add("bat-info-label");
        Label v = new Label(value);
        v.getStyleClass().add("bat-info-val");
        if (styleClass != null) v.getStyleClass().add(styleClass);
        row.getChildren().addAll(l, v);
        return row;
    }

    private static String formatChgTime(int minutes) {
        if (minutes <= 0) return "—";
        if (minutes < 60) return Math.round(minutes) + " 分";
        int h = minutes / 60, m = minutes % 60;
        return h + "时" + (m < 10 ? "0" : "") + m + "分";
    }

    @FXML
    private void onClearWaypoints() {
        if (mission.size() == 0) { showToast("航点列表已为空", "info"); return; }
        mission.clearWaypoints();
        missionDirty = true;
        refreshWaypointList();
        updateMap();
        updateMissionStats();
        updateUploadStatus();
        updateWpCount();
        showToast("已清空所有航点", "info");
    }

    @FXML
    private void onUploadToE1() {
        showToast("上传至 E1 · 当前模式不支持", "warn");
    }

    @FXML
    private void onStartAutoMission() {
        showToast("自动任务 · 当前模式不支持", "warn");
    }

    @FXML
    private void onExportLogCsv() {
        showToast("导出日志 CSV · 功能开发中", "info");
    }

    @FXML
    private void onImportLogCsv() {
        showToast("导入日志 CSV · 功能开发中", "info");
    }

    @FXML
    private void onShowAbout() {
        showToast("SkyLink 天链 v2.1.0 · 南京航空航天大学课设项目", "info");
    }

    @FXML
    private void onShowShortcuts() {
        showToast("快捷键：1-4 切换工作区 · Ctrl+S 导出XML", "info");
    }
}
