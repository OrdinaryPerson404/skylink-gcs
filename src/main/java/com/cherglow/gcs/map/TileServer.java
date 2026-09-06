package com.cherglow.gcs.map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * 本地瓦片服务（S5 离线地图）：把 ~/.skylink/tiles/{source}/{z}/{x}/{y}.png
 * 以 HTTP 暴露给 WebView 内的 Leaflet（127.0.0.1 绑定，绕开 file:// 子资源限制）。
 * 瓦片缺失返回 404，JS 侧据此回退在线图源。
 */
public final class TileServer {

    public static final int PORT = 8135;

    private HttpServer server;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    /** 高德图源 URL（与 TileDownloader 口径一致） */
    static String amapUrl(String src, int x, int y, int z) {
        int sub = 1 + (x + y) % 4;
        if (src.equals("vec")) {
            return "https://webrd0" + sub + ".is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x=" + x + "&y=" + y + "&z=" + z;
        }
        return "https://webst0" + sub + ".is.autonavi.com/appmaptile?style=6&x=" + x + "&y=" + y + "&z=" + z;
    }

    public static Path defaultRoot() {
        return Path.of(System.getProperty("user.home"), ".skylink", "tiles");
    }

    public static String url() {
        return "http://127.0.0.1:" + PORT;
    }

    public void start(Path root) throws IOException {
        if (server != null) {
            return;
        }
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/", ex -> handle(ex, root));
        server.setExecutor(Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "tile-server");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        System.out.println("[tiles] 本地瓦片服务 http://127.0.0.1:" + PORT + " -> " + root);
    }

    private void handle(HttpExchange ex, Path root) throws IOException {
        try {
            String path = ex.getRequestURI().getPath(); // /{source}/{z}/{x}/{y}.png
            String[] seg = path.split("/");
            byte[] body = null;
            if (seg.length == 5 && (seg[1].equals("vec") || seg[1].equals("sat"))) {
                try {
                    int z = Integer.parseInt(seg[2]);
                    int x = Integer.parseInt(seg[3]);
                    int y = Integer.parseInt(seg[4].replace(".png", ""));
                    if (z >= 0 && z <= 19 && x >= 0 && y >= 0) {
                        Path f = root.resolve(seg[1]).resolve(String.valueOf(z))
                                .resolve(String.valueOf(x)).resolve(y + ".png").normalize();
                        if (f.startsWith(root) && Files.isRegularFile(f)) {
                            body = Files.readAllBytes(f);
                        }
                    }
                } catch (NumberFormatException ignored) {
                    // 非瓦片路径 → 404
                }
            }
            if (body == null && seg.length == 5) {
                // 本地未命中 → 服务端拉高德 → 返回并存盘（渐进式离线缓存）
                try {
                    int z = Integer.parseInt(seg[2]);
                    int x = Integer.parseInt(seg[3]);
                    int y = Integer.parseInt(seg[4].replace(".png", ""));
                    HttpRequest req = HttpRequest.newBuilder(
                            URI.create(amapUrl(seg[1], x, y, z)))
                            .timeout(Duration.ofSeconds(8))
                            .header("User-Agent", "SkyLinkGCS/0.1 (offline tiles; test use)")
                            .GET().build();
                    HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
                    if (resp.statusCode() == 200 && resp.body().length > 100) {
                        body = resp.body();
                        Path f = root.resolve(seg[1]).resolve(String.valueOf(z))
                                .resolve(String.valueOf(x)).resolve(y + ".png").normalize();
                        if (f.startsWith(root)) {
                            Files.createDirectories(f.getParent());
                            Files.write(f, body); // 渐进缓存：在线拉到的瓦片落盘供离线复用
                        }
                    }
                } catch (Exception ignored) {
                    // 离线且未预下载 → 404
                }
            }
            if (body == null) {
                ex.sendResponseHeaders(404, -1);
            } else {
                ex.getResponseHeaders().set("Content-Type", "image/png");
                ex.getResponseHeaders().set("Cache-Control", "max-age=86400");
                ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // 允许 Canvas 取像素做暗色化
                ex.sendResponseHeaders(200, body.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(body);
                }
            }
        } finally {
            ex.close();
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}
