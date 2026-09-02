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
    @FXML private FlowPane algoFlow;

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
    private boolean paramListRequested = false;
    private MapCanvas mapCanvas;
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
        initWaypoints();
        applyModeGating();
        startClock();
        updateWpCount();
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
            e1ConnBanner.setText(isE1 ? "● E1 飞控在线" : "● 已连接");
            e1ConnBanner.getStyleClass().clear();
            e1ConnBanner.getStyleClass().add("dev-conn-banner");
            e1ConnBanner.getStyleClass().add(isE1 ? "online" : "offline");
        }
        if (phoneConnBanner != null) {
            phoneConnBanner.setText(isPhone ? "● 手机伴随已连接" : "● 未连接");
            phoneConnBanner.getStyleClass().clear();
            phoneConnBanner.getStyleClass().add("dev-conn-banner");
            phoneConnBanner.getStyleClass().add(isPhone ? "online" : "offline");
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
        // 通信延迟/油门/控制来源/心跳/链路质量：飞控不上报时诚实标注 "—"
        if (e1LatencyVal != null) e1LatencyVal.setText(connected ? "—" : "—");
        if (e1ThrottleVal != null) e1ThrottleVal.setText(connected ? "—" : "0%");
        if (e1CtrlSrcVal != null) e1CtrlSrcVal.setText(connected ? "—" : "—");
        if (e1HeartbeatVal != null) e1HeartbeatVal.setText(connected ? "—" : "—");
        if (e1LinkQualityVal != null) e1LinkQualityVal.setText(connected ? "—" : "—");
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
        switch (settings.runMode) {
            case "e1_real" -> {
                connDot.getStyleClass().add(comm.isConnected() ? "on" : "warn");
                connDetail.setText(comm.isConnected()
                    ? "已连接 · " + settings.serialPort + " " + settings.baudRate
                    : "连接中... · " + settings.serialPort);
            }
            case "phone_assist" -> {
                connDot.getStyleClass().add("warn");
                connDetail.setText("已连接 · 手机辅助");
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
    }

    @FXML
    private void onToggleFollow() {
        if (mapCanvas != null && btnFollow != null) {
            mapCanvas.setFollowEnabled(btnFollow.isSelected());
            if (btnFollow.isSelected()) {
                showToast("已开启跟随模式", "info");
            }
        }
    }

    @FXML
    private void onToggleTrajectory() {
        if (mapCanvas != null && btnTrajectory != null) {
            mapCanvas.setTrajectoryVisible(btnTrajectory.isSelected());
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
        if (mapCanvas != null && btnAirspace != null) {
            mapCanvas.setAirspaceVisible(btnAirspace.isSelected());
        }
    }

    @FXML
    private void onZoomIn() {
        if (mapCanvas != null) mapCanvas.zoomIn();
    }

    @FXML
    private void onZoomOut() {
        if (mapCanvas != null) mapCanvas.zoomOut();
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
    }

    @FXML
    private void onSetLightTheme() {
        ThemeManager.setTheme(stage.getScene(), "light");
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
                        if (idx >= 0) {
                            String sub = body.substring(idx + 17);
                            String valStr = sub.split("[,}\\s]")[0].trim();
                            double windSpeed = Double.parseDouble(valStr);
                            if (windInput != null) {
                                windInput.setText(String.format("%.1f", windSpeed));
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
        showToast(String.format("风速估算 %.1f m/s（%s）", windSpeed, reason), "warn");
    }

    @FXML
    private void onToggleTheme() {
        ThemeManager.toggle(stage.getScene());
    }

    @FXML
    private void onSaveSettings() {
        // 将设置页字段值回写到 settings 对象
        if (e1IpField != null) settings.e1Ip = e1IpField.getText();
        if (e1PortField != null) {
            try { settings.e1Port = Integer.parseInt(e1PortField.getText()); }
            catch (NumberFormatException ignored) {}
        }
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
        comm.setCallback(t -> Platform.runLater(() -> {
            if (!t.valid) return;
            System.out.println("[E1] 收到遥测: msgId=" + t.msgId + " hasAtt=" + t.hasAttitude + " hasIMU=" + t.hasImu
                + " hasParam=" + t.hasParam + " hasAck=" + t.hasAck);
            // 单点翻译：根据 has* 标志选择性更新 Drone 字段及可用性标识，
            // 避免飞控不上报的数据被默认值 0 误导 UI
            drone.updateFromTelemetry(t);
            // 累积飞控参数（只读，安全；写入受硬约束限制不下发）
            if (t.hasParam) paramService.onParamValue(t);
            // 连接建立收到首个心跳后，自动请求飞控参数列表一次（只读，安全）
            if (!paramListRequested && t.hasHeartbeat) {
                paramListRequested = true;
                comm.requestParamList();
                System.out.println("[E1] 收到首个心跳，已请求飞控参数列表（CF-Drone 84 项）");
            }
            updateDroneStatus();
            updateE1StatusDetail();
            updatePhoneStatusDetail();
            // 仅在飞控上报位置时更新地图与状态栏经纬度，避免无 GPS 时显示 0
            if (t.hasPosition && mapCanvas != null) {
                mapCanvas.setDronePosition(t.lat, t.lon);
                mapCanvas.setDroneAlt(t.alt);
                mapCanvas.setDroneHeading(t.heading);
            }
            if (sbLat != null) sbLat.setText(t.hasPosition ? String.format("%.5f", t.lat) : "—");
            if (sbLon != null) sbLon.setText(t.hasPosition ? String.format("%.5f", t.lon) : "—");
            // 温度：仅 IMU 上报时显示真实值，否则诚实标注 "—"
            if (seTemp != null) {
                seTemp.setText(drone.isImuAvailable() ? String.format("%.1f°C", drone.getImuTemp()) : "—");
            }
            if (sbLastUpdate != null) sbLastUpdate.setText(
                new Date().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalTime().toString());
        }));
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
        boolean attOk = drone.isAttitudeAvailable();
        boolean battOk = drone.isBatteryAvailable();
        boolean hbOk = drone.isHeartbeatAvailable();
        boolean rcOk = drone.isRcAvailable();
        boolean landOk = drone.isLandedStateAvailable();
        Object[] values = {
            hbOk ? drone.getCustomModeName() : "—", "info",
            landOk ? drone.getLandedStateName() : "—", landOk && drone.getLandedState() == 1 ? "off" : "info",
            posOk ? drone.getLat() : "—", "",
            posOk ? drone.getLon() : "—", "",
            posOk ? drone.getAlt() : "—", "",
            posOk ? drone.getSpeed() : "—", "",
            posOk ? drone.getHeading() : "—", "",
            attOk ? drone.getRoll() : "—", "",
            attOk ? drone.getPitch() : "—", "",
            attOk ? drone.getYaw() : "—", "",
            battOk ? drone.getBatteryPct() : "—", "battery",
            battOk ? drone.getVoltage() : "—", "",
            rcOk ? drone.getRssi() : "—", "ok",
            posOk ? drone.getSatellites() : "—", "ok",
            posOk ? drone.getHdop() : "—", "ok",
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
        if (sbGps != null) sbGps.setText(posOk ? ("3D·" + drone.getSatellites()) : "—");
        if (sbHdop != null) sbHdop.setText(posOk ? String.format("%.1f", drone.getHdop()) : "—");
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
