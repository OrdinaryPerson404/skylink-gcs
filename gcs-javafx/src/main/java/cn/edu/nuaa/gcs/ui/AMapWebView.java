package cn.edu.nuaa.gcs.ui;

import cn.edu.nuaa.gcs.model.Waypoint;
import cn.edu.nuaa.gcs.planner.AirspaceService;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * 高德地图 WebView 封装。
 * 内嵌 /web/amap.html（高德 JS API 2.0），Java 与页面通过 window.gcsBridge 通信：
 *  - Java 调页面：executeScript("Gcs.xxx(...)")
 *  - 页面回调 Java：bridge 的 onMapClick(lat,lon) / onWaypointDrag(idx,lat,lon)
 * 主地图（interact=true，可点击加航点、拖拽航点）与飞行日志轨迹回放（interact=false）共用。
 */
public class AMapWebView extends StackPane {

    private final WebView webView = new WebView();
    private final WebEngine engine;
    private boolean ready = false;

    private boolean dark = true;
    private boolean interact;
    private boolean follow = true;
    private boolean trajectoryVisible = true;
    private boolean airspaceVisible = true;
    private boolean satellite = false;

    /** 页面加载完成前缓存的数据，ready 后一次性推送 */
    private String pendingWaypoints;
    private String pendingZones;
    private String pendingTrajectory;

    private BiConsumer<Double, Double> clickHandler;
    private BiConsumer<Integer, double[]> waypointMoveHandler;
    private BiConsumer<Double, Double> mapRightClickHandler;
    private IntConsumer waypointRightClickHandler;
    private IntConsumer waypointDblClickHandler;
    private BiConsumer<Double, Double> mouseMoveHandler;
    private Runnable failHandler;
    private Runnable readyListener;

    public AMapWebView(boolean interact) {
        this.interact = interact;
        webView.setStyle("-fx-background-color: #0d1117;");
        getChildren().add(webView);
        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.setOnError(e -> System.err.println("[WebEngine] " + e.getMessage()));
        engine.setOnAlert(e -> System.out.println("[WebAlert] " + e.getData()));
        engine.load(getClass().getResource("/web/amap.html").toExternalForm());
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("gcsBridge", new Bridge());
                ready = true;
                js("Gcs.setInteract(" + interact + ")");
                js("Gcs.setDark(" + dark + ")");
                if (pendingWaypoints != null) js("Gcs.setWaypoints('" + pendingWaypoints + "')");
                if (pendingZones != null) js("Gcs.setAirspace('" + pendingZones + "')");
                if (pendingTrajectory != null) js("Gcs.showFlightTrajectory('" + pendingTrajectory + "')");
                if (readyListener != null) readyListener.run();
            }
        });
    }

    public void setReadyListener(Runnable r) { this.readyListener = r; }
    public void setClickHandler(BiConsumer<Double, Double> h) { this.clickHandler = h; }
    public void setWaypointMoveHandler(BiConsumer<Integer, double[]> h) { this.waypointMoveHandler = h; }
    public void setMapRightClickHandler(BiConsumer<Double, Double> h) { this.mapRightClickHandler = h; }
    public void setWaypointRightClickHandler(IntConsumer h) { this.waypointRightClickHandler = h; }
    public void setWaypointDblClickHandler(IntConsumer h) { this.waypointDblClickHandler = h; }
    public void setMouseMoveHandler(BiConsumer<Double, Double> h) { this.mouseMoveHandler = h; }
    public void setFailHandler(Runnable h) { this.failHandler = h; }

    /** 高德页面加载失败 → 隐藏 WebView，露出底层离线瓦片地图作为降级 */
    private void handleFail() {
        Platform.runLater(() -> {
            setVisible(false);
            setManaged(false);
            if (failHandler != null) failHandler.run();
        });
    }

    // ===== Java -> JS =====

    public void setDark(boolean d) {
        this.dark = d;
        js("Gcs.setDark(" + d + ")");
    }

    public void setSatellite(boolean s) {
        this.satellite = s;
        js("Gcs.setSatellite(" + s + ")");
    }

    public void setFollow(boolean f) {
        this.follow = f;
        js("Gcs.setFollow(" + f + ")");
    }

    public void setTrajectoryVisible(boolean v) {
        this.trajectoryVisible = v;
        js("Gcs.setTrajectoryVisible(" + v + ")");
    }

    public void setAirspaceVisible(boolean v) {
        this.airspaceVisible = v;
        js("Gcs.setAirspaceVisible(" + v + ")");
    }

    public void setWaypoints(List<Waypoint> wps) {
        StringBuilder sb = new StringBuilder("[");
        if (wps != null) {
            for (int i = 0; i < wps.size(); i++) {
                Waypoint wp = wps.get(i);
                if (i > 0) sb.append(',');
                sb.append("{\"lat\":").append(wp.getLat())
                  .append(",\"lng\":").append(wp.getLon()).append('}');
            }
        }
        sb.append(']');
        pendingWaypoints = sb.toString();
        js("Gcs.setWaypoints('" + pendingWaypoints + "')");
    }

    /** 上次 JS 无人机位置更新的时间戳（ns），用于节流。 */
    private volatile long lastDroneJsNs = 0;
    /** JS 无人机位置更新最小间隔（ns），200ms = 5fps，避免高频遥测淹没 WebView。 */
    private static final long DRONE_JS_THROTTLE_NS = 200_000_000L;
    /** 缓存最新无人机位置，节流到期后由 flush 刷新。 */
    private volatile double pendingLat, pendingLon, pendingHeading, pendingAlt;
    private volatile boolean hasPendingDrone = false;

    public void setDronePosition(double lat, double lon, double heading, double alt) {
        pendingLat = lat; pendingLon = lon; pendingHeading = heading; pendingAlt = alt;
        hasPendingDrone = true;
        long now = System.nanoTime();
        if (now - lastDroneJsNs >= DRONE_JS_THROTTLE_NS) {
            lastDroneJsNs = now;
            hasPendingDrone = false;
            js(String.format("Gcs.setDrone(%.7f,%.7f,%.1f,%.1f)", lat, lon, heading, alt));
        }
        // 否则：节流窗口内，等待下次 flushDroneUpdate() 或下次到期
    }

    /** 刷新积压的无人机位置到 WebView（由 UI 定时器 200ms 调用一次）。 */
    public void flushDroneUpdate() {
        if (!hasPendingDrone) return;
        long now = System.nanoTime();
        if (now - lastDroneJsNs < DRONE_JS_THROTTLE_NS) return;
        lastDroneJsNs = now;
        hasPendingDrone = false;
        js(String.format("Gcs.setDrone(%.7f,%.7f,%.1f,%.1f)",
            pendingLat, pendingLon, pendingHeading, pendingAlt));
    }

    public void setAirspaceZones(List<AirspaceService.Zone> zones) {
        StringBuilder sb = new StringBuilder("[");
        if (zones != null) {
            boolean first = true;
            for (AirspaceService.Zone z : zones) {
                if (z.coords == null || z.coords.size() < 3) continue;
                if (!first) sb.append(',');
                first = false;
                sb.append("{\"type\":\"").append(z.type.name()).append("\",\"path\":[");
                for (int i = 0; i < z.coords.size(); i++) {
                    double[] c = z.coords.get(i);
                    if (i > 0) sb.append(',');
                    // coords 存储顺序为 [lon, lat]，页面需要 [lng, lat]
                    sb.append('[').append(c[0]).append(',').append(c[1]).append(']');
                }
                sb.append("]}");
            }
        }
        sb.append(']');
        pendingZones = sb.toString();
        js("Gcs.setAirspace('" + pendingZones + "')");
    }

    public void fitWaypoints() { js("Gcs.fitWaypoints()"); }
    public void zoomIn() { js("Gcs.zoomIn()"); }
    public void zoomOut() { js("Gcs.zoomOut()"); }

    /** 飞行日志轨迹回放：path 为 [lon, lat] 点序列 */
    public void showFlightTrajectory(List<double[]> path) {
        StringBuilder sb = new StringBuilder("[");
        if (path != null) {
            for (int i = 0; i < path.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append('[').append(path.get(i)[0]).append(',').append(path.get(i)[1]).append(']');
            }
        }
        sb.append(']');
        pendingTrajectory = sb.toString();
        js("Gcs.showFlightTrajectory('" + pendingTrajectory + "')");
    }

    public void resetFlightView() { js("Gcs.resetFlightView()"); }

    public boolean isReady() { return ready; }

    private void js(String script) {
        if (!ready) return;
        if (Platform.isFxApplicationThread()) {
            exec(script);
        } else {
            Platform.runLater(() -> exec(script));
        }
    }

    private void exec(String script) {
        try {
            engine.executeScript(script);
        } catch (Exception ignored) {
            // 页面未就绪或 JS 异常时静默降级，不影响离线地图
        }
    }

    // ===== JS -> Java =====

    public class Bridge {
        public void onMapClick(double lat, double lon) {
            if (clickHandler != null) clickHandler.accept(lat, lon);
        }

        public void onWaypointDrag(int idx, double lat, double lon) {
            if (waypointMoveHandler != null) waypointMoveHandler.accept(idx, new double[]{lat, lon});
        }

        public void onMapRightClick(double lat, double lon) {
            if (mapRightClickHandler != null) mapRightClickHandler.accept(lat, lon);
        }

        public void onWaypointRightClick(int idx) {
            if (waypointRightClickHandler != null) waypointRightClickHandler.accept(idx);
        }

        public void onWaypointDblClick(int idx) {
            if (waypointDblClickHandler != null) waypointDblClickHandler.accept(idx);
        }

        public void onMouseMove(double lat, double lon) {
            if (mouseMoveHandler != null) mouseMoveHandler.accept(lat, lon);
        }

        public void onMapFailed() {
            handleFail();
        }

        public void onMapError(String msg) {
            System.err.println("[AMap JS] " + msg);
        }
    }
}
