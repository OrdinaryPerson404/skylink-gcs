package com.cherglow.gcs.transport;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S14：GNSS HTTP 推流端点测试 —— 解析语义（正常/缺参/非法）+ 真实 HTTP 回环推流 + 防抖。
 */
class GpsHttpServerTest {

    @Test
    void parseFullQuery() {
        double[] v = GpsHttpServer.parseGpsQuery("lat=31.123&lon=121.456&alt=25.5&spd=1.8");
        assertEquals(31.123, v[0], 1e-9);
        assertEquals(121.456, v[1], 1e-9);
        assertEquals(25.5, v[2], 1e-9);
        assertEquals(1.8, v[3], 1e-9);
    }

    @Test
    void parseMissingFieldsAreNaN() {
        double[] v = GpsHttpServer.parseGpsQuery("lat=31.1");
        assertTrue(Double.isNaN(v[1]));
        assertTrue(Double.isNaN(v[2]));
        assertTrue(Double.isNaN(v[3]));
    }

    @Test
    void parseInvalidValuesAreNaN() {
        double[] v = GpsHttpServer.parseGpsQuery("lat=abc&lon=&spd=xyz");
        assertTrue(Double.isNaN(v[0]));
        assertTrue(Double.isNaN(v[3]));
    }

    @Test
    void parseNullOrBlank() {
        assertEquals(4, GpsHttpServer.parseGpsQuery(null).length);
        double[] v = GpsHttpServer.parseGpsQuery("");
        for (double x : v) {
            assertTrue(Double.isNaN(x));
        }
    }

    @Test
    void httpRoundtripAndDebounce() throws Exception {
        GpsHttpServer server = new GpsHttpServer();
        CountDownLatch first = new CountDownLatch(1);
        AtomicReference<double[]> received = new AtomicReference<>();
        server.start(0, (lat, lon, alt, spd) -> {
            received.set(new double[]{lat, lon, alt, spd});
            first.countDown();
        });
        int port = server.getPort();
        assertTrue(port > 0);

        HttpClient http = HttpClient.newHttpClient();
        // 1) 根路径自检页
        HttpResponse<String> root = http.send(HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + "/")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, root.statusCode());
        assertTrue(root.body().contains("GNSS"));
        // 2) 首次推流 → 回调收到
        http.send(HttpRequest.newBuilder(URI.create(
                "http://127.0.0.1:" + port + "/gps?lat=28.68&lon=115.88&alt=25.5&spd=1.8")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertTrue(first.await(3, TimeUnit.SECONDS));
        assertEquals(28.68, received.get()[0], 1e-9);
        assertEquals(115.88, received.get()[1], 1e-9);
        assertEquals(25.5, received.get()[2], 1e-9);
        assertEquals(1.8, received.get()[3], 1e-9);
        // 3) 紧随其后的重复推流 → 防抖忽略（300ms 窗口内）
        HttpResponse<String> dup = http.send(HttpRequest.newBuilder(URI.create(
                "http://127.0.0.1:" + port + "/gps?lat=99&lon=99")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, dup.statusCode());
        Thread.sleep(100);
        assertEquals(28.68, received.get()[0], 1e-9); // 未被 99 覆盖
        server.stop();
    }

    @Test
    void uriRawQueryParsesCoordinates() {
        // 模拟 GPSLogger 实际请求形态（占位符已在手机端替换为数值）
        URI uri = URI.create("http://192.168.4.2:8080/gps?lat=-28.5&lon=115.5&alt=10&spd=0.5");
        double[] v = GpsHttpServer.parseGpsQuery(uri.getRawQuery());
        assertEquals(-28.5, v[0], 1e-9);
        assertEquals(115.5, v[1], 1e-9);
        assertEquals(10.0, v[2], 1e-9);
        assertEquals(0.5, v[3], 1e-9);
    }
}
