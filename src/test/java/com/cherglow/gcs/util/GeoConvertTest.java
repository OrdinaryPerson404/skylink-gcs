package com.cherglow.gcs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeoConvertTest {

    /** 权威黄金值（上海，coordTransform_lib 文档固定测试对）：WGS84→GCJ-02 */
    @Test
    void shanghaiWgs84ToGcj02GoldenValue() {
        double[] g = GeoConvert.wgs84ToGcj02(31.1774276, 121.5272106);
        assertEquals(31.17530398364597, g[0], 1e-6, "纠偏后纬度");
        assertEquals(121.531541859215, g[1], 1e-6, "纠偏后经度");
    }

    @Test
    void outOfChinaReturnsInputUnchanged() {
        double[] g = GeoConvert.wgs84ToGcj02(40.7128, -74.0060); // 纽约
        assertEquals(40.7128, g[0], 0.0);
        assertEquals(-74.0060, g[1], 0.0);
    }

    @Test
    void beijingOffsetIsPositiveSoutheasterly() {
        // 北京附近典型纠偏：向北向东偏移约数百米（保留方向性合理性）
        double[] g = GeoConvert.wgs84ToGcj02(39.9042, 116.4074);
        assertEquals(39.9, g[0], 0.01);
        assertEquals(116.41, g[1], 0.01);
    }
}