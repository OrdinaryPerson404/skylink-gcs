package com.cherglow.gcs.transport;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 外部 GNSS 载荷 HTTP 接入（S14，多源遥测）：
 * 绑 0.0.0.0:8080，接收手机 GPSLogger 的 Custom URL 推流：
 *   GET /gps?lat=%LAT&lon=%LON&alt=%ALT&spd=%SPD
 * 手机与电脑同入无人机 AP（Drone_WiFi）。解析语义：缺参/非法 → NaN；同秒重复推流防抖。
 * 根路径 / 返回连通性自检页（含已收定位计数），供手机浏览器直接验证链路。
 */
public final class GpsHttpServer {

    public interface GpsListener {
        /** lat/lon 必为有效数值；alt/spd 可能为 NaN（GPSLogger 未推该字段） */
        void accept(double lat, double lon, double alt, double spd);
    }

    private static final long DEBOUNCE_MS = 300;

    private HttpServer server;
    private long lastAcceptMs;
    private volatile int fixCount;

    /** 解析 GPSLogger 查询串 → {lat, lon, alt, spd}，缺参/非法为 NaN */
    public static double[] parseGpsQuery(String query) {
        double lat = Double.NaN, lon = Double.NaN, alt = Double.NaN, spd = Double.NaN;
        if (query != null && !query.isBlank()) {
            for (String kv : query.split("&")) {
                int eq = kv.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String k = kv.substring(0, eq).trim().toLowerCase();
                double v;
                try {
                    v = Double.parseDouble(kv.substring(eq + 1).trim());
                } catch (NumberFormatException e) {
                    continue; // 非法值 → 该字段 NaN
                }
                switch (k) {
                    case "lat" -> lat = v;
                    case "lon" -> lon = v;
                    case "alt" -> alt = v;
                    case "spd" -> spd = v;
                    default -> {
                    }
                }
            }
        }
        return new double[]{lat, lon, alt, spd};
    }

    /** 绑 0.0.0.0:port 并启动；port=0 时系统分配（测试用），实际端口经 getPort() 获取 */
    public synchronized void start(int port, GpsListener listener) throws IOException {
        if (server != null) {
            return;
        }
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/gps", ex -> {
            try {
                double[] v = parseGpsQuery(ex.getRequestURI().getRawQuery());
                respond(ex, 200, "ok");
                long now = System.currentTimeMillis();
                if (Double.isNaN(v[0]) || Double.isNaN(v[1])) {
                    return; // 无有效坐标不上报
                }
                if (now - lastAcceptMs < DEBOUNCE_MS && lastAcceptMs > 0) {
                    return; // 同秒重复推流防抖
                }
                lastAcceptMs = now;
                fixCount++;
                listener.accept(v[0], v[1], v[2], v[3]);
            } finally {
                ex.close();
            }
        });
        server.createContext("/", ex -> {
            try {
                respond(ex, 200, "SkyLink GCS GNSS link OK\n"
                        + "received fixes: " + fixCount + "\n"
                        + "usage: GET /gps?lat=<deg>&lon=<deg>&alt=<m>&spd=<m/s>");
            } finally {
                ex.close();
            }
        });
        server.setExecutor(Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "gnss-http");
            t.setDaemon(true);
            return t;
        }));
        server.start();
    }

    private void respond(HttpExchange ex, int code, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    /** 实际绑定端口（start(port=0) 后有效） */
    public synchronized int getPort() {
        return server != null ? server.getAddress().getPort() : -1;
    }

    public synchronized boolean isOpen() {
        return server != null;
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}
