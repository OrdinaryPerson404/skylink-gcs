package com.cherglow.gcs.ui.map;

import com.cherglow.gcs.tools.TileDownloader;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 纯 JavaFX Canvas 瓦片地图引擎（S5 性能重构，替代 WebView+Leaflet）：
 * 单节点直绘（无 WebKit 合成开销）、内存 LRU 瓦片缓存、磁盘直读预下载瓦片、
 * 高德端点异步补块并自动落盘（渐进离线缓存）、缺块用上级瓦片放大补位（无黑洞）、
 * vec 源像素反相暗色化（加载时一次性处理并缓存）。
 * 交互：拖拽平移、滚轮锚点缩放、点击回调、航点徽章拖拽。
 */
public class MapView extends StackPane {

    private static final int TILE = 256;
    private static final Path TILE_ROOT = Path.of(System.getProperty("user.home"), ".skylink", "tiles");
    private static final int CACHE_MAX = 512;

    private final Canvas canvas = new Canvas(400, 300);
    private final Map<String, Image> tileCache = new LinkedHashMap<>(128, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> e) {
            return size() > CACHE_MAX;
        }
    };
    private final Set<String> loading = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> failedAt = new ConcurrentHashMap<>();
    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "tile-loader");
        t.setDaemon(true);
        return t;
    });
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();

    // 视口状态
    private double centerLat = 39.9042;
    private double centerLng = 116.4074;
    private int zoom = 11;
    private String base = "vec"; // vec=矢量暗色 | sat=卫星

    // 覆盖层数据
    private final Map<Integer, double[]> waypoints = new LinkedHashMap<>();
    private double[] home;
    private double[] dronePos;   // null=隐藏
    private Double droneHeading = 0.0;
    private boolean follow;

    // 交互状态
    private boolean panning;
    private double lastX, lastY, pressX, pressY;
    private boolean moved;
    private Integer dragWp;

    private Consumer<double[]> clickHandler;
    private BiConsumer<Integer, double[]> wpMoveHandler;

    private volatile boolean dirty = true;
    private final AnimationTimer pulse = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (dirty) {
                dirty = false;
                draw();
            }
        }
    };

    public MapView() {
        getChildren().add(canvas);
        setMinSize(240, 180);
        widthProperty().addListener((o, a, b) -> {
            canvas.setWidth(b.doubleValue());
            markDirty();
        });
        heightProperty().addListener((o, a, b) -> {
            canvas.setHeight(b.doubleValue());
            markDirty();
        });
        widthProperty().addListener((o, a, b) -> markDirty());
        heightProperty().addListener((o, a, b) -> markDirty());

        canvas.setOnMousePressed(e -> {
            pressX = lastX = e.getX();
            pressY = lastY = e.getY();
            moved = false;
            dragWp = hitWaypoint(e.getX(), e.getY());
            panning = dragWp == null;
        });
        canvas.setOnMouseDragged(e -> {
            double dx = e.getX() - lastX;
            double dy = e.getY() - lastY;
            lastX = e.getX();
            lastY = e.getY();
            if (Math.abs(e.getX() - pressX) + Math.abs(e.getY() - pressY) > 6) {
                moved = true;
            }
            if (dragWp != null && moved) {
                double[] geo = screenToGeo(e.getX(), e.getY());
                waypoints.put(dragWp, geo);
                markDirty();
            } else if (panning && moved) {
                centerLng = pxToLng(lngToPx(centerLng, zoom) - dx, zoom);
                centerLat = pxToLat(latToPx(centerLat, zoom) - dy, zoom);
                markDirty();
            }
        });
        canvas.setOnMouseReleased(e -> {
            if (dragWp != null) {
                if (moved && wpMoveHandler != null) {
                    double[] geo = waypoints.get(dragWp);
                    if (geo != null) {
                        wpMoveHandler.accept(dragWp, geo);
                    }
                }
                dragWp = null;
            } else if (!moved && clickHandler != null) {
                clickHandler.accept(screenToGeo(e.getX(), e.getY()));
            }
            panning = false;
        });
        canvas.setOnScroll(e -> {
            int dz = e.getDeltaY() > 0 ? 1 : -1;
            int nz = Math.max(3, Math.min(18, zoom + dz));
            if (nz == zoom) {
                return;
            }
            double[] mouseGeo = screenToGeo(e.getX(), e.getY());
            zoom = nz;
            // 锚定鼠标下的地理坐标
            double wx = lngToPx(mouseGeo[1], zoom) - (canvas.getWidth() / 2.0 - e.getX());
            double wy = latToPx(mouseGeo[0], zoom) - (canvas.getHeight() / 2.0 - e.getY());
            centerLng = pxToLng(wx, zoom);
            centerLat = pxToLat(wy, zoom);
            markDirty();
        });

        pulse.start();
    }

    // ================= 底层 API（与旧 WebView 版一致） =================

    public void setTileRoot(String root) {
        // 纯 Canvas 版直接读磁盘，兼容保留空实现
    }

    public void setDark() {
        base = "vec";
        markDirty();
    }

    public void setSatellite() {
        base = "sat";
        markDirty();
    }

    public void toggleBase(Consumer<String> onResult) {
        base = base.equals("vec") ? "sat" : "vec";
        markDirty();
        if (onResult != null) {
            onResult.accept(base.equals("vec") ? "dark" : "sat");
        }
    }

    public void addWaypoint(int id, double lat, double lng) {
        waypoints.put(id, new double[]{lat, lng});
        markDirty();
    }

    public void removeWaypoint(int id) {
        waypoints.remove(id);
        markDirty();
    }

    public void clearWaypoints() {
        waypoints.clear();
        markDirty();
    }

    public void fitWaypoints() {
        if (waypoints.isEmpty()) {
            return;
        }
        double minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;
        for (double[] p : waypoints.values()) {
            minLat = Math.min(minLat, p[0]);
            maxLat = Math.max(maxLat, p[0]);
            minLng = Math.min(minLng, p[1]);
            maxLng = Math.max(maxLng, p[1]);
        }
        centerLat = (minLat + maxLat) / 2;
        centerLng = (minLng + maxLng) / 2;
        if (waypoints.size() == 1) {
            zoom = 15;
        } else {
            int fitZ = 3;
            for (int z = 18; z >= 3; z--) {
                double wSpan = Math.abs(lngToPx(maxLng, z) - lngToPx(minLng, z));
                double hSpan = Math.abs(latToPx(minLat, z) - latToPx(maxLat, z));
                if (wSpan < canvas.getWidth() - 80 && hSpan < canvas.getHeight() - 80) {
                    fitZ = z;
                    break;
                }
            }
            zoom = fitZ;
        }
        markDirty();
    }

    public void setHome(double lat, double lng) {
        home = new double[]{lat, lng};
        markDirty();
    }

    public void setDronePosition(Double lat, Double lng, Double headingDeg) {
        if (lat == null || lng == null) {
            dronePos = null;
        } else {
            dronePos = new double[]{lat, lng};
            droneHeading = headingDeg;
            if (follow) {
                centerLat = lat;
                centerLng = lng;
            }
        }
        markDirty();
    }

    public void setFollow(boolean f) {
        follow = f;
    }

    public void setCenter(double lat, double lng, int zoomLevel) {
        centerLat = lat;
        centerLng = lng;
        zoom = Math.max(3, Math.min(18, zoomLevel));
        markDirty();
    }

    public double[] getMapCenter() {
        return new double[]{centerLat, centerLng};
    }

    /** 当前视口范围 {minLat, minLng, maxLat, maxLng, zoom}，供离线缓存按视野入库 */
    public double[] getViewportBBox() {
        double minLat = pxToLat(latToPx(centerLat, zoom) + canvas.getHeight() / 2.0, zoom);
        double maxLat = pxToLat(latToPx(centerLat, zoom) - canvas.getHeight() / 2.0, zoom);
        double minLng = pxToLng(lngToPx(centerLng, zoom) - canvas.getWidth() / 2.0, zoom);
        double maxLng = pxToLng(lngToPx(centerLng, zoom) + canvas.getWidth() / 2.0, zoom);
        return new double[]{minLat, minLng, maxLat, maxLng, zoom};
    }

    public void setOnMapClick(Consumer<double[]> handler) {
        this.clickHandler = handler;
    }

    public void setOnWaypointMoved(BiConsumer<Integer, double[]> handler) {
        this.wpMoveHandler = handler;
    }

    // ================= 墨卡托换算 =================

    private static double lngToPx(double lng, int z) {
        return (lng + 180) / 360 * (TILE << z);
    }

    private static double latToPx(double lat, int z) {
        double rad = Math.toRadians(Math.max(-85, Math.min(85, lat)));
        return (1 - Math.log(Math.tan(rad) + 1 / Math.cos(rad)) / Math.PI) / 2 * (TILE << z);
    }

    private static double pxToLng(double px, int z) {
        return px / (TILE << z) * 360 - 180;
    }

    private static double pxToLat(double py, int z) {
        double n = Math.PI - 2 * Math.PI * py / (TILE << z);
        return Math.toDegrees(Math.atan(0.5 * (Math.exp(n) - Math.exp(-n))));
    }

    private double[] screenToGeo(double sx, double sy) {
        double wx = lngToPx(centerLng, zoom) - canvas.getWidth() / 2.0 + sx;
        double wy = latToPx(centerLat, zoom) - canvas.getHeight() / 2.0 + sy;
        return new double[]{pxToLat(wy, zoom), pxToLng(wx, zoom)};
    }

    // ================= 瓦片加载 =================

    private static String key(String src, int z, int x, int y) {
        return src + "/" + z + "/" + x + "/" + y;
    }

    /** 取瓦片：内存 → （未命中）请求异步加载并触发重绘 */
    private Image tile(String src, int z, int x, int y) {
        String k = key(src, z, x, y);
        Image img = tileCache.get(k);
        if (img != null) {
            return img;
        }
        requestTile(src, z, x, y);
        return null;
    }

    private void requestTile(String src, int z, int x, int y) {
        String k = key(src, z, x, y);
        if (!loading.add(k)) {
            return;
        }
        Long failT = failedAt.get(k);
        if (failT != null && System.currentTimeMillis() - failT < 10_000) {
            return; // 失败冷却，避免反复请求
        }
        loader.submit(() -> {
            Image img = null;
            try {
                Path f = TILE_ROOT.resolve(src).resolve(String.valueOf(z))
                        .resolve(String.valueOf(x)).resolve(y + ".png");
                if (Files.isRegularFile(f)) {
                    img = new Image(f.toUri().toString(), false);
                }
                if (img == null || img.isError() || img.getWidth() <= 0) {
                    // 在线补块（高德端点），落盘供离线复用
                    HttpRequest req = HttpRequest.newBuilder(
                            URI.create(TileDownloader.url(src, x, y, z)))
                            .timeout(Duration.ofSeconds(8))
                            .header("User-Agent", "SkyLinkGCS/0.1 (offline tiles; test use)")
                            .GET().build();
                    HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
                    if (resp.statusCode() == 200 && resp.body().length > 100) {
                        Files.createDirectories(f.getParent());
                        Files.write(f, resp.body());
                        img = new Image(new ByteArrayInputStream(resp.body()));
                    }
                }
                if (img != null && !img.isError() && img.getWidth() > 0 && src.equals("vec")) {
                    img = invert(img);
                }
                Image finalImg = img;
                if (finalImg != null && !finalImg.isError()) {
                    Platform.runLater(() -> {
                        tileCache.put(k, finalImg);
                        markDirty();
                    });
                } else {
                    failedAt.put(k, System.currentTimeMillis());
                }
            } catch (Exception e) {
                failedAt.put(k, System.currentTimeMillis());
            } finally {
                loading.remove(k);
            }
        });
    }

    /** vec 源反相（加载时一次性处理并缓存结果） */
    private static Image invert(Image src) {
        int w = (int) src.getWidth(), h = (int) src.getHeight();
        if (w <= 0 || h <= 0) {
            return src;
        }
        PixelReader pr = src.getPixelReader();
        WritableImage out = new WritableImage(w, h);
        PixelWriter pw = out.getPixelWriter();
        int[] buf = new int[w * h];
        pr.getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), buf, 0, w);
        for (int i = 0; i < buf.length; i++) {
            int a = buf[i] & 0xFF000000;
            int r = 255 - ((buf[i] >> 16) & 0xFF);
            int g = 255 - ((buf[i] >> 8) & 0xFF);
            int b = 255 - (buf[i] & 0xFF);
            buf[i] = a | (b << 16) | (g << 8) | r; // 反相后 R/B 互换 ≈ 暗色化
        }
        pw.setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), buf, 0, w);
        return out;
    }

    // ================= 绘制 =================

    private void markDirty() {
        dirty = true;
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        if (w < 40 || h < 40) {
            return;
        }
        g.setFill(Color.web("#0d1117"));
        g.fillRect(0, 0, w, h);

        double topWorldX = lngToPx(centerLng, zoom) - w / 2.0;
        double topWorldY = latToPx(centerLat, zoom) - h / 2.0;

        // ---- 瓦片层（缺块用上级放大补位） ----
        int tx0 = (int) Math.floor(topWorldX / TILE);
        int ty0 = (int) Math.floor(topWorldY / TILE);
        int tx1 = (int) Math.floor((topWorldX + w) / TILE);
        int ty1 = (int) Math.floor((topWorldY + h) / TILE);
        for (int tx = tx0; tx <= tx1; tx++) {
            for (int ty = ty0; ty <= ty1; ty++) {
                double sx = tx * TILE - topWorldX;
                double sy = ty * TILE - topWorldY;
                int n = 1 << zoom;
                if (ty < 0 || ty >= n) {
                    continue;
                }
                int x = ((tx % n) + n) % n;
                Image img = tile(base, zoom, x, ty);
                if (img != null) {
                    g.drawImage(img, sx, sy);
                    continue;
                }
                // 上级瓦片补位（最多回退 3 级）
                for (int lvl = 1; lvl <= 3 && zoom - lvl >= 3; lvl++) {
                    int pz = zoom - lvl;
                    int px = x >> lvl, py = ty >> lvl;
                    Image p = tileCache.get(key(base, pz, px, py));
                    if (p == null) {
                        requestTile(base, pz, px, py);
                        continue;
                    }
                    int mask = (1 << lvl) - 1;
                    double sub = TILE / (1 << lvl);
                    double srcX = (x & mask) * sub;
                    double srcY = (ty & mask) * sub;
                    g.drawImage(p, srcX, srcY, sub, sub, sx, sy, TILE, TILE);
                    break;
                }
            }
        }

        // ---- 地理→屏幕 ----
        double originWx = topWorldX;
        double originWy = topWorldY;

        // ---- 航点连线 ----
        if (waypoints.size() >= 2) {
            g.setStroke(Color.web("#f0a500"));
            g.setLineWidth(2);
            double[] prev = null;
            for (double[] p : waypoints.values()) {
                double x = lngToPx(p[1], zoom) - originWx;
                double y = latToPx(p[0], zoom) - originWy;
                if (prev != null) {
                    g.strokeLine(prev[0], prev[1], x, y);
                }
                prev = new double[]{x, y};
            }
        }

        // ---- Home ----
        if (home != null) {
            double x = lngToPx(home[1], zoom) - originWx;
            double y = latToPx(home[0], zoom) - originWy;
            g.setFill(Color.web("#4b8bf5"));
            g.fillOval(x - 6, y - 6, 12, 12);
            g.setStroke(Color.WHITE);
            g.setLineWidth(2);
            g.strokeOval(x - 6, y - 6, 12, 12);
        }

        // ---- 航点徽章 ----
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        g.setTextAlign(TextAlignment.CENTER);
        for (var e : waypoints.entrySet()) {
            double x = lngToPx(e.getValue()[1], zoom) - originWx;
            double y = latToPx(e.getValue()[0], zoom) - originWy;
            if (x < -20 || y < -20 || x > w + 20 || y > h + 20) {
                continue;
            }
            g.setFill(Color.web("#f0a500"));
            g.fillOval(x - 9, y - 9, 18, 18);
            g.setStroke(Color.web("#161b22"));
            g.setLineWidth(2);
            g.strokeOval(x - 9, y - 9, 18, 18);
            g.setFill(Color.web("#161b22"));
            g.fillText(String.valueOf(e.getKey()), x, y + 3.5);
        }

        // ---- 无人机（有位置源才显示） ----
        if (dronePos != null) {
            double x = lngToPx(dronePos[1], zoom) - originWx;
            double y = latToPx(dronePos[0], zoom) - originWy;
            g.save();
            g.translate(x, y);
            g.rotate(droneHeading == null ? 0 : droneHeading);
            g.setFill(Color.web("#22c55e"));
            g.fillPolygon(new double[]{0, -6, 0, 6}, new double[]{-11, 8, 4, 8}, 4);
            g.restore();
        }
        g.setTextAlign(TextAlignment.LEFT);

        // ---- 比例尺（左下） ----
        double mpp = 156543.03392 * Math.cos(Math.toRadians(centerLat)) / (1 << zoom);
        double segM = nice(mpp * 110);
        double segPx = segM / mpp;
        double sy = h - 18;
        g.setStroke(Color.web("#c9d1d9"));
        g.setLineWidth(1.5);
        g.strokeLine(12, sy, 12 + segPx, sy);
        g.strokeLine(12, sy - 4, 12, sy);
        g.strokeLine(12 + segPx, sy - 4, 12 + segPx, sy);
        g.setFill(Color.web("#c9d1d9"));
        g.setFont(Font.font("Consolas", 10));
        g.fillText(segM >= 1000 ? (long) (segM / 1000) + " km" : (int) segM + " m", 14 + segPx / 2 - 14, sy - 6);
    }

    private static double nice(double v) {
        double[] steps = {50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000};
        for (double s : steps) {
            if (s >= v) {
                return s;
            }
        }
        return steps[steps.length - 1];
    }

    private Integer hitWaypoint(double sx, double sy) {
        double topWorldX = lngToPx(centerLng, zoom) - canvas.getWidth() / 2.0;
        double topWorldY = latToPx(centerLat, zoom) - canvas.getHeight() / 2.0;
        for (var e : waypoints.entrySet()) {
            double x = lngToPx(e.getValue()[1], zoom) - topWorldX;
            double y = latToPx(e.getValue()[0], zoom) - topWorldY;
            if (Math.abs(x - sx) <= 11 && Math.abs(y - sy) <= 11) {
                return e.getKey();
            }
        }
        return null;
    }
}
