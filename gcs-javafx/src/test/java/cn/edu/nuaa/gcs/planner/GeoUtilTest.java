package cn.edu.nuaa.gcs.planner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GeoUtil 工具类单元测试
 * 覆盖：Haversine 距离、Web Mercator 坐标转换、瓦片坐标换算
 */
class GeoUtilTest {

    @Test
    @DisplayName("Haversine 距离：同点距离为 0")
    void testHaversineSamePoint() {
        double dist = GeoUtil.haversine(32.0, 118.0, 32.0, 118.0);
        assertEquals(0, dist, 0.001, "同点距离应为 0");
    }

    @Test
    @DisplayName("Haversine 距离：已知经纬度验证")
    void testHaversineKnownDistance() {
        // 北京天安门 (39.9042, 116.4074) 到 上海外滩 (31.2304, 121.4737)
        // 大圆距离约 1068 km
        double dist = GeoUtil.haversine(39.9042, 116.4074, 31.2304, 121.4737);
        assertTrue(dist > 1_000_000 && dist < 1_100_000,
            "北京到上海距离应约 1068km, 实际: " + dist + "m");
    }

    @Test
    @DisplayName("Haversine 距离：对称性")
    void testHaversineSymmetry() {
        double d1 = GeoUtil.haversine(32.0, 118.0, 33.0, 119.0);
        double d2 = GeoUtil.haversine(33.0, 119.0, 32.0, 118.0);
        assertEquals(d1, d2, 0.001, "距离应对称");
    }

    @Test
    @DisplayName("经纬度↔像素 互逆验证")
    void testLonLatPixelInverse() {
        double lon = 118.793;
        double lat = 32.0612;
        int zoom = 16;

        double[] px = GeoUtil.lonLatToPixel(lon, lat, zoom);
        double[] ll = GeoUtil.pixelToLonLat(px[0], px[1], zoom);

        assertEquals(lon, ll[0], 0.00001, "经度逆变换误差应 < 0.00001°");
        assertEquals(lat, ll[1], 0.00001, "纬度逆变换误差应 < 0.00001°");
    }

    @Test
    @DisplayName("像素转瓦片坐标")
    void testPixelToTile() {
        int[] tile = GeoUtil.pixelToTile(300, 500);
        assertEquals(1, tile[0], "x=300 应在瓦片 1");
        assertEquals(1, tile[1], "y=500 应在瓦片 1");

        tile = GeoUtil.pixelToTile(255, 255);
        assertEquals(0, tile[0], "x=255 应在瓦片 0");
        assertEquals(0, tile[1], "y=255 应在瓦片 0");

        tile = GeoUtil.pixelToTile(256, 256);
        assertEquals(1, tile[0], "x=256 应在瓦片 1");
        assertEquals(1, tile[1], "y=256 应在瓦片 1");
    }
}
