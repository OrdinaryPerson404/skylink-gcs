package cn.edu.nuaa.gcs.ui;

import cn.edu.nuaa.gcs.model.Waypoint;
import cn.edu.nuaa.gcs.planner.GeoUtil;
import cn.edu.nuaa.gcs.planner.AirspaceService;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public class MapCanvas extends Canvas {
    public enum MapStyle { STANDARD, SATELLITE, TERRAIN }

    private int zoom = 16;
    private double centerLon = 118.793;
    private double centerLat = 32.0612;
    private double dragStartX, dragStartY;
    private double offsetPixelX, offsetPixelY;
    private final HashMap<String, Image> tileCache = new HashMap<>();
    private String tilesDir = "tiles";
    private List<Waypoint> waypoints = new ArrayList<>();
    private double droneLat = 32.0612;
    private double droneLon = 118.793;
    private double droneHeading = 0;
    private List<double[]> trail = new ArrayList<>();
    private List<AirspaceService.Zone> airspaceZones = new ArrayList<>();
    private boolean airspaceVisible = true;
    private List<int[]> violatingSegments = new ArrayList<>();
    private BiConsumer<Double, Double> clickHandler;
    private BiConsumer<Integer, double[]> waypointMoveHandler;
    private IntConsumer waypointDoubleClickHandler;
    private BiConsumer<Double, Double> contextMenuHandler;
    private IntConsumer waypointContextMenuHandler;

    // Waypoint dragging state
    private int draggingWaypointIndex = -1;
    private boolean isDraggingWaypoint = false;

    // Enhanced map controls
    private boolean followEnabled = true;
    private boolean trajectoryVisible = true;
    private MapStyle mapStyle = MapStyle.STANDARD;
    private Runnable zoomChangeListener;
    private Runnable followChangeListener;
    private BiConsumer<Double, Double> mouseMoveHandler;

    public MapCanvas() {
        widthProperty().addListener(e -> draw());
        heightProperty().addListener(e -> draw());
        setOnMousePressed(this::onMousePressed);
        setOnMouseDragged(this::onMouseDragged);
        setOnMouseReleased(this::onMouseReleased);
        setOnScroll(this::onScroll);
        setOnMouseClicked(this::onMouseClicked);
        setOnMouseMoved(this::onMouseMoved);
        setOnContextMenuRequested(this::onContextMenuRequested);
    }

    private void onContextMenuRequested(javafx.scene.input.ContextMenuEvent e) {
        int wpIdx = findWaypointAt(e.getX(), e.getY());
        if (wpIdx >= 0 && waypointContextMenuHandler != null) {
            waypointContextMenuHandler.accept(wpIdx);
        } else if (contextMenuHandler != null) {
            double[] lonLat = pixelToLonLat(e.getX(), e.getY());
            contextMenuHandler.accept(lonLat[1], lonLat[0]);
        }
        e.consume();
    }

    private void onMouseMoved(MouseEvent e) {
        if (mouseMoveHandler != null) {
            double[] lonLat = pixelToLonLat(e.getX(), e.getY());
            mouseMoveHandler.accept(lonLat[1], lonLat[0]);
        }
    }

    private double[] pixelToLonLat(double px, double py) {
        double[] center = GeoUtil.lonLatToPixel(centerLon, centerLat, zoom);
        return GeoUtil.pixelToLonLat(
                center[0] + px - getWidth() / 2 - offsetPixelX,
                center[1] + py - getHeight() / 2 - offsetPixelY,
                zoom);
    }

    private int findWaypointAt(double px, double py) {
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint wp = waypoints.get(i);
            double[] wpx = GeoUtil.lonLatToPixel(wp.getLon(), wp.getLat(), zoom);
            double sx = wpx[0] + getWidth() / 2 - GeoUtil.lonLatToPixel(centerLon, centerLat, zoom)[0] + offsetPixelX;
            double sy = wpx[1] + getHeight() / 2 - GeoUtil.lonLatToPixel(centerLon, centerLat, zoom)[1] + offsetPixelY;
            double dx = px - sx;
            double dy = py - sy;
            if (dx * dx + dy * dy <= 100) { // 10px radius hit test
                return i;
            }
        }
        return -1;
    }

    public void setWaypoints(List<Waypoint> wps) { this.waypoints = wps; draw(); }
    public void setDronePosition(double lat, double lon) {
        this.droneLat = lat; this.droneLon = lon;
        trail.add(new double[]{lon, lat});
        if (trail.size() > 600) trail.remove(0);
        if (followEnabled) {
            centerLat = lat;
            centerLon = lon;
            offsetPixelX = 0;
            offsetPixelY = 0;
        }
        draw();
    }
    /** 批量更新无人机位置/高度/航向，只触发一次 draw()，避免高频遥测时 3 次重绘。 */
    public void setDroneState(double lat, double lon, double alt, double heading) {
        this.droneLat = lat;
        this.droneLon = lon;
        this.droneAlt = alt;
        this.droneHeading = heading;
        trail.add(new double[]{lon, lat});
        if (trail.size() > 600) trail.remove(0);
        if (followEnabled) {
            centerLat = lat;
            centerLon = lon;
            offsetPixelX = 0;
            offsetPixelY = 0;
        }
        draw();
    }
    public void setDroneHeading(double h) { this.droneHeading = h; draw(); }
    public void setAirspaceZones(List<AirspaceService.Zone> zones) { this.airspaceZones = zones; draw(); }
    public void setAirspaceVisible(boolean v) { this.airspaceVisible = v; draw(); }
    public void setViolatingSegments(List<int[]> segs) { this.violatingSegments = segs; draw(); }
    public void setClickHandler(BiConsumer<Double, Double> h) { this.clickHandler = h; }
    public void setWaypointMoveHandler(BiConsumer<Integer, double[]> h) { this.waypointMoveHandler = h; }
    public void setWaypointDoubleClickHandler(IntConsumer h) { this.waypointDoubleClickHandler = h; }
    public void setContextMenuHandler(BiConsumer<Double, Double> h) { this.contextMenuHandler = h; }
    public void setWaypointContextMenuHandler(IntConsumer h) { this.waypointContextMenuHandler = h; }

    // Enhanced map controls
    public void setFollowEnabled(boolean v) { this.followEnabled = v; draw(); }
    public boolean isFollowEnabled() { return followEnabled; }
    public void setFollowChangeListener(Runnable l) { this.followChangeListener = l; }
    public void setTrajectoryVisible(boolean v) { this.trajectoryVisible = v; draw(); }
    public boolean isTrajectoryVisible() { return trajectoryVisible; }
    public void setMapStyle(MapStyle style) { this.mapStyle = style; tileCache.clear(); draw(); }
    public MapStyle getMapStyle() { return mapStyle; }
    public int getZoom() { return zoom; }
    public void setZoomChangeListener(Runnable listener) { this.zoomChangeListener = listener; }
    public void setMouseMoveHandler(BiConsumer<Double, Double> h) { this.mouseMoveHandler = h; }
    public void zoomIn() { if (zoom < 18) { zoom++; notifyZoomChange(); draw(); } }
    public void zoomOut() { if (zoom > 1) { zoom--; notifyZoomChange(); draw(); } }

    private void notifyZoomChange() {
        if (zoomChangeListener != null) zoomChangeListener.run();
    }

    @Override
    public boolean isResizable() { return true; }

    @Override
    public double prefWidth(double h) { return getWidth(); }
    @Override
    public double prefHeight(double w) { return getHeight(); }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Background color based on map style and theme
        Color bgColor = switch (mapStyle) {
            case STANDARD -> ThemeManager.isDark() ? Color.web("#0d1117") : Color.web("#e8e8e8");
            case SATELLITE -> Color.web("#1a1a2e");
            case TERRAIN -> ThemeManager.isDark() ? Color.web("#1a2e1a") : Color.web("#d4e6d4");
        };
        gc.setFill(bgColor);
        gc.fillRect(0, 0, w, h);

        double[] centerPixel = GeoUtil.lonLatToPixel(centerLon, centerLat, zoom);
        double offsetX = w / 2 - centerPixel[0] + offsetPixelX;
        double offsetY = h / 2 - centerPixel[1] + offsetPixelY;

        drawTiles(gc, offsetX, offsetY, w, h);
        if (airspaceVisible) drawAirspace(gc, offsetX, offsetY);
        if (trajectoryVisible) drawTrail(gc, offsetX, offsetY);
        drawWaypoints(gc, offsetX, offsetY);
        drawViolatingSegments(gc, offsetX, offsetY);
        drawDrone(gc, offsetX, offsetY);
        drawScaleBar(gc, w, h);
        drawCompass(gc, w, h);
    }

    private void drawCompass(GraphicsContext gc, double w, double h) {
        double cx = w - 30;
        double cy = 30;
        double r = 16;

        // Compass background
        gc.setFill(ThemeManager.isDark() ? Color.web("#161b22cc") : Color.web("#ffffffcc"));
        gc.setStroke(ThemeManager.isDark() ? Color.web("#30363d") : Color.web("#d1d1d1"));
        gc.setLineWidth(1);
        gc.beginPath();
        gc.arc(cx, cy, r, r, 0, 360);
        gc.fill();
        gc.stroke();

        // North arrow (red)
        gc.setFill(Color.web("#ef4444"));
        gc.beginPath();
        gc.moveTo(cx, cy - r + 2);
        gc.lineTo(cx - 4, cy + 2);
        gc.lineTo(cx + 4, cy + 2);
        gc.closePath();
        gc.fill();

        // South arrow (muted)
        gc.setFill(ThemeManager.isDark() ? Color.web("#6e7681") : Color.web("#8c8c8c"));
        gc.beginPath();
        gc.moveTo(cx, cy + r - 2);
        gc.lineTo(cx - 4, cy - 2);
        gc.lineTo(cx + 4, cy - 2);
        gc.closePath();
        gc.fill();

        // N label
        gc.setFill(Color.web("#ef4444"));
        gc.setFont(javafx.scene.text.Font.font("JetBrains Mono", 9));
        gc.fillText("N", cx - 3, cy - r - 2);
    }

    private void drawScaleBar(GraphicsContext gc, double w, double h) {
        // Draw scale bar at bottom-left
        double metersPerPixel = 156543.03392 * Math.cos(centerLat * Math.PI / 180) / Math.pow(2, zoom);
        double[] scales = {10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000};
        double targetPx = 100;
        double chosenScale = scales[0];
        for (double s : scales) {
            if (s / metersPerPixel <= targetPx) chosenScale = s;
        }
        double barWidth = chosenScale / metersPerPixel;
        double barY = h - 20;
        double barX = 12;

        gc.setFill(ThemeManager.isDark() ? Color.web("#ffffffcc") : Color.web("#000000cc"));
        gc.fillRect(barX, barY, barWidth, 3);
        gc.fillRect(barX, barY - 4, 1, 7);
        gc.fillRect(barX + barWidth, barY - 4, 1, 7);

        String label = chosenScale >= 1000 ? (chosenScale / 1000) + " km" : chosenScale + " m";
        gc.setFont(javafx.scene.text.Font.font(10));
        gc.setFill(ThemeManager.isDark() ? Color.web("#ffffff") : Color.web("#000000"));
        gc.fillText(label, barX, barY - 6);
    }

    private void drawTiles(GraphicsContext gc, double offsetX, double offsetY, double w, double h) {
        int minTileX = (int) Math.floor((-offsetX) / 256);
        int maxTileX = (int) Math.ceil((w - offsetX) / 256);
        int minTileY = (int) Math.floor((-offsetY) / 256);
        int maxTileY = (int) Math.ceil((h - offsetY) / 256);
        for (int tx = minTileX; tx <= maxTileX; tx++) {
            for (int ty = minTileY; ty <= maxTileY; ty++) {
                if (tx < 0 || ty < 0 || tx >= (1 << zoom) || ty >= (1 << zoom)) continue;
                String key = zoom + "/" + tx + "/" + ty;
                Image tile = tileCache.computeIfAbsent(key, k -> {
                    File f = new File(tilesDir + "/" + k + ".png");
                    return f.exists() ? new Image(f.toURI().toString()) : null;
                });
                double px = tx * 256 + offsetX;
                double py = ty * 256 + offsetY;
                if (tile != null) {
                    gc.drawImage(tile, px, py, 256, 256);
                } else {
                    // Style-specific placeholder tiles
                    Color fillColor, strokeColor;
                    switch (mapStyle) {
                        case SATELLITE -> {
                            fillColor = ThemeManager.isDark() ? Color.web("#1a1a2e") : Color.web("#2d3a4a");
                            strokeColor = ThemeManager.isDark() ? Color.web("#2a2a4a") : Color.web("#3d4a5a");
                        }
                        case TERRAIN -> {
                            fillColor = ThemeManager.isDark() ? Color.web("#1a2e1a") : Color.web("#c8dcc8");
                            strokeColor = ThemeManager.isDark() ? Color.web("#2a4a2a") : Color.web("#b0c8b0");
                        }
                        default -> {
                            fillColor = ThemeManager.isDark() ? Color.web("#161b22") : Color.web("#d0d0d0");
                            strokeColor = ThemeManager.isDark() ? Color.web("#21262d") : Color.web("#c0c0c0");
                        }
                    }
                    gc.setFill(fillColor);
                    gc.fillRect(px, py, 256, 256);
                    gc.setStroke(strokeColor);
                    gc.strokeRect(px, py, 256, 256);

                    // Map style label on center tile
                    if (tx == (int)Math.floor((w/2 - offsetX) / 256)
                        && ty == (int)Math.floor((h/2 - offsetY) / 256)) {
                        gc.setFill(ThemeManager.isDark() ? Color.web("#6e7681") : Color.web("#8c8c8c"));
                        gc.setFont(javafx.scene.text.Font.font(11));
                        String styleName = switch (mapStyle) {
                            case STANDARD -> "标准地图";
                            case SATELLITE -> "卫星地图";
                            case TERRAIN -> "地形地图";
                        };
                        gc.fillText(styleName + " · 离线瓦片未加载", px + 20, py + 128);
                    }
                }
            }
        }
    }

    private void drawAirspace(GraphicsContext gc, double offsetX, double offsetY) {
        for (AirspaceService.Zone z : airspaceZones) {
            double[] rgb = switch (z.type) {
                case NOFLY -> new double[]{1, 0.27, 0.27, 0.2, 1, 0.27, 0.27, 0.8};
                case RESTRICTION -> new double[]{0.96, 0.62, 0.04, 0.1, 0.96, 0.62, 0.04, 0.7};
                case TEMPORARY -> new double[]{0.65, 0.55, 0.98, 0.12, 0.65, 0.55, 0.98, 0.7};
                case CUSTOM -> new double[]{0.23, 0.51, 0.96, 0.08, 0.23, 0.51, 0.96, 0.6};
            };
            gc.setFill(new Color(rgb[0], rgb[1], rgb[2], rgb[3]));
            gc.setStroke(new Color(rgb[4], rgb[5], rgb[6], rgb[7]));
            gc.setLineWidth(z.type == AirspaceService.ZoneType.NOFLY ? 2 : 1.5);
            double[] prev = null;
            for (double[] c : z.coords) {
                double[] px = GeoUtil.lonLatToPixel(c[0], c[1], zoom);
                double sx = px[0] + offsetX, sy = px[1] + offsetY;
                if (prev == null) { prev = new double[]{sx, sy}; gc.beginPath(); gc.moveTo(sx, sy); }
                else { gc.lineTo(sx, sy); }
            }
            if (z.coords.size() > 2) {
                double[] first = GeoUtil.lonLatToPixel(z.coords.get(0)[0], z.coords.get(0)[1], zoom);
                gc.lineTo(first[0] + offsetX, first[1] + offsetY);
            }
            gc.closePath();
            gc.fill();
            gc.stroke();
            prev = null;
        }
    }

    private void drawTrail(GraphicsContext gc, double offsetX, double offsetY) {
        if (trail.size() < 2) return;
        gc.setStroke(Color.web("#3b82f6", 0.85));
        gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i < trail.size(); i++) {
            double[] px = GeoUtil.lonLatToPixel(trail.get(i)[0], trail.get(i)[1], zoom);
            double sx = px[0] + offsetX, sy = px[1] + offsetY;
            if (i == 0) gc.moveTo(sx, sy);
            else gc.lineTo(sx, sy);
        }
        gc.stroke();
    }

    private void drawWaypoints(GraphicsContext gc, double offsetX, double offsetY) {
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint wp = waypoints.get(i);
            double[] px = GeoUtil.lonLatToPixel(wp.getLon(), wp.getLat(), zoom);
            double sx = px[0] + offsetX, sy = px[1] + offsetY;
            boolean isHome = i == 0;
            gc.setFill(isHome ? Color.web("#22c55e") : Color.web("#3b82f6"));
            gc.fillOval(sx - 8, sy - 8, 16, 16);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(sx - 8, sy - 8, 16, 16);
            gc.setFill(Color.WHITE);
            gc.fillText(String.valueOf(i), sx - 3, sy + 4);
        }
        if (waypoints.size() > 1) {
            gc.setStroke(Color.web("#3b82f6", 0.5));
            gc.setLineWidth(1.5);
            gc.setLineDashes(5, 5);
            gc.beginPath();
            for (int i = 0; i < waypoints.size(); i++) {
                double[] px = GeoUtil.lonLatToPixel(waypoints.get(i).getLon(), waypoints.get(i).getLat(), zoom);
                double sx = px[0] + offsetX, sy = px[1] + offsetY;
                if (i == 0) gc.moveTo(sx, sy);
                else gc.lineTo(sx, sy);
            }
            gc.stroke();
            gc.setLineDashes();
        }
    }

    private void drawViolatingSegments(GraphicsContext gc, double offsetX, double offsetY) {
        for (int[] seg : violatingSegments) {
            if (seg[0] >= waypoints.size() || seg[1] >= waypoints.size()) continue;
            double[] p1 = GeoUtil.lonLatToPixel(waypoints.get(seg[0]).getLon(), waypoints.get(seg[0]).getLat(), zoom);
            double[] p2 = GeoUtil.lonLatToPixel(waypoints.get(seg[1]).getLon(), waypoints.get(seg[1]).getLat(), zoom);
            gc.setStroke(Color.web("#ef4444", 0.9));
            gc.setLineWidth(4);
            gc.strokeLine(p1[0] + offsetX, p1[1] + offsetY, p2[0] + offsetX, p2[1] + offsetY);
        }
    }

    private void drawDrone(GraphicsContext gc, double offsetX, double offsetY) {
        double[] px = GeoUtil.lonLatToPixel(droneLon, droneLat, zoom);
        double sx = px[0] + offsetX, sy = px[1] + offsetY;
        gc.save();
        gc.translate(sx, sy);
        gc.rotate(droneHeading);
        gc.setFill(Color.web("#ef4444"));
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.2);
        gc.beginPath();
        gc.moveTo(0, -16);
        gc.lineTo(11, 10);
        gc.lineTo(3, 4);
        gc.lineTo(0, 14);
        gc.lineTo(-3, 4);
        gc.lineTo(-11, 10);
        gc.closePath();
        gc.fill();
        gc.stroke();
        gc.restore();
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font(10));
        gc.fillText(String.format("%.1fm", droneAlt), sx + 12, sy - 12);
    }

    private double droneAlt = 0;
    public void setDroneAlt(double alt) { this.droneAlt = alt; draw(); }

    private void onMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            dragStartX = e.getX();
            dragStartY = e.getY();
            // Check if clicking on a waypoint
            int wpIdx = findWaypointAt(e.getX(), e.getY());
            if (wpIdx >= 0) {
                isDraggingWaypoint = true;
                draggingWaypointIndex = wpIdx;
            }
        }
    }

    private void onMouseDragged(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY && followEnabled && !isDraggingWaypoint) {
            followEnabled = false;
            if (followChangeListener != null) followChangeListener.run();
        }
        if (isDraggingWaypoint && draggingWaypointIndex >= 0) {
            // Update waypoint position
            double[] lonLat = pixelToLonLat(e.getX(), e.getY());
            Waypoint wp = waypoints.get(draggingWaypointIndex);
            wp.setLat(lonLat[1]);
            wp.setLon(lonLat[0]);
            if (waypointMoveHandler != null) {
                waypointMoveHandler.accept(draggingWaypointIndex, new double[]{lonLat[1], lonLat[0]});
            }
            draw();
        } else {
            offsetPixelX += e.getX() - dragStartX;
            offsetPixelY += e.getY() - dragStartY;
            dragStartX = e.getX();
            dragStartY = e.getY();
            draw();
        }
    }

    private void onMouseReleased(MouseEvent e) {
        if (isDraggingWaypoint) {
            isDraggingWaypoint = false;
            draggingWaypointIndex = -1;
        }
    }

    private void onMouseClicked(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
            int wpIdx = findWaypointAt(e.getX(), e.getY());
            if (wpIdx >= 0 && waypointDoubleClickHandler != null) {
                waypointDoubleClickHandler.accept(wpIdx);
                return;
            }
        }
        if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1 && !isDraggingWaypoint && clickHandler != null) {
            // Single click on empty area adds waypoint
            int wpIdx = findWaypointAt(e.getX(), e.getY());
            if (wpIdx < 0) {
                double[] lonLat = pixelToLonLat(e.getX(), e.getY());
                clickHandler.accept(lonLat[1], lonLat[0]);
            }
        }
    }

    private void onScroll(ScrollEvent e) {
        int oldZoom = zoom;
        if (e.getDeltaY() > 0 && zoom < 18) zoom++;
        else if (e.getDeltaY() < 0 && zoom > 1) zoom--;
        if (oldZoom != zoom) {
            double factor = Math.pow(2, zoom - oldZoom);
            offsetPixelX *= factor;
            offsetPixelY *= factor;
            notifyZoomChange();
            draw();
        }
    }

    public void fitWaypoints() {
        if (waypoints.isEmpty()) return;
        double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
        for (Waypoint wp : waypoints) {
            if (wp.getLat() < minLat) minLat = wp.getLat();
            if (wp.getLat() > maxLat) maxLat = wp.getLat();
            if (wp.getLon() < minLon) minLon = wp.getLon();
            if (wp.getLon() > maxLon) maxLon = wp.getLon();
        }
        centerLat = (minLat + maxLat) / 2;
        centerLon = (minLon + maxLon) / 2;
        offsetPixelX = 0; offsetPixelY = 0;
        draw();
    }
}
