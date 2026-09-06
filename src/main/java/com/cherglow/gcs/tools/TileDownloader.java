package com.cherglow.gcs.tools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * 瓦片预下载工具（S5 离线地图）：按 中心点+半径+缩放区间 从高德端点拉取瓦片到本地。
 * 用法：java -cp "target/classes;..." com.cherglow.gcs.tools.TileDownloader [vec|sat|both] lat lng radiusKm minZ maxZ [outDir]
 * 默认 outDir = ~/.skylink/tiles（与地图组件一致）。仅供测试用途。
 */
public final class TileDownloader {

    public static void main(String[] args) throws Exception {
        String source = args.length > 0 ? args[0] : "both";
        double lat = args.length > 1 ? Double.parseDouble(args[1]) : 39.9042;
        double lng = args.length > 2 ? Double.parseDouble(args[2]) : 116.4074;
        double radiusKm = args.length > 3 ? Double.parseDouble(args[3]) : 20;
        int minZ = args.length > 4 ? Integer.parseInt(args[4]) : 11;
        int maxZ = args.length > 5 ? Integer.parseInt(args[5]) : 15;
        Path out = Path.of(args.length > 6 ? args[6]
                : Path.of(System.getProperty("user.home"), ".skylink", "tiles").toString());

        List<String> sources = switch (source) {
            case "vec" -> List.of("vec");
            case "sat" -> List.of("sat");
            default -> List.of("vec", "sat");
        };
        double dLat = radiusKm / 111.32;
        double dLng = radiusKm / (111.32 * Math.cos(Math.toRadians(lat)));
        downloadRegion(sources, lat - dLat, lat + dLat, lng - dLng, lng + dLng, minZ, maxZ, out, null);
    }

    /** 按 bbox 下载瓦片（同步阻塞，调用方自行包线程）。progress = (done, total)。 */
    public static void downloadRegion(List<String> sources, double latMin, double latMax,
                                      double lngMin, double lngMax, int minZ, int maxZ,
                                      Path out, BiConsumer<Integer, Integer> progress) {
        minZ = Math.max(3, minZ);
        maxZ = Math.min(18, maxZ);
        List<int[]> jobs = new ArrayList<>();
        List<String> srcList = new ArrayList<>();
        for (int z = minZ; z <= maxZ; z++) {
            int n = 1 << z;
            int xMin = (int) Math.floor(lngToTileX(lngMin, z));
            int xMax = (int) Math.floor(lngToTileX(lngMax, z));
            int yMin = (int) Math.floor(latToTileY(latMax, z));
            int yMax = (int) Math.floor(latToTileY(latMin, z));
            for (String src : sources) {
                for (int x = Math.max(0, xMin); x <= Math.min(n - 1, xMax); x++) {
                    for (int y = Math.max(0, yMin); y <= Math.min(n - 1, yMax); y++) {
                        jobs.add(new int[]{srcList.size(), z, x, y});
                        srcList.add(src); // 与 jobs 平行
                    }
                }
            }
        }
        // 平行数组改为逐项记录源
        String[] srcOf = srcList.toArray(new String[0]);
        int total = jobs.size();
        System.out.println("[tiles] 计划下载 " + total + " 块（" + sources + "，z" + minZ + "-" + maxZ + "）→ " + out);

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        AtomicInteger ok = new AtomicInteger(), skip = new AtomicInteger(), fail = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < total; i++) {
            int[] t = jobs.get(i);
            String src = srcOf[i];
            int z = t[1], x = t[2], y = t[3];
            pool.submit(() -> {
                try {
                    Path f = out.resolve(src).resolve(String.valueOf(z))
                            .resolve(String.valueOf(x)).resolve(y + ".png");
                    if (Files.isRegularFile(f) && Files.size(f) > 0) {
                        skip.incrementAndGet();
                        return;
                    }
                    HttpRequest req = HttpRequest.newBuilder(URI.create(url(src, x, y, z)))
                            .timeout(Duration.ofSeconds(10))
                            .header("User-Agent", "SkyLinkGCS/0.1 (offline tiles; test use)")
                            .GET().build();
                    HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                    if (resp.statusCode() == 200 && resp.body().length > 100) {
                        Files.createDirectories(f.getParent());
                        Files.write(f, resp.body());
                        ok.incrementAndGet();
                    } else {
                        fail.incrementAndGet();
                    }
                } catch (IOException | InterruptedException e) {
                    fail.incrementAndGet();
                }
                int done = ok.get() + skip.get() + fail.get();
                if (progress != null && done % 50 == 0) {
                    progress.accept(done, total);
                }
            });
        }
        try {
            pool.shutdown();
            while (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                // 等待完成
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("[tiles] 完成 ok=" + ok.get() + " skip=" + skip.get() + " fail=" + fail.get());
    }

    public static String url(String src, int x, int y, int z) {
        int sub = 1 + (x + y) % 4;
        if (src.equals("vec")) {
            return "https://webrd0" + sub + ".is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x=" + x + "&y=" + y + "&z=" + z;
        }
        return "https://webst0" + sub + ".is.autonavi.com/appmaptile?style=6&x=" + x + "&y=" + y + "&z=" + z;
    }

    static double lngToTileX(double lng, int z) {
        return (lng + 180) / 360 * (1 << z);
    }

    static double latToTileY(double lat, int z) {
        double rad = Math.toRadians(lat);
        return (1 - Math.log(Math.tan(rad) + 1 / Math.cos(rad)) / Math.PI) / 2 * (1 << z);
    }

    private TileDownloader() {
    }
}
