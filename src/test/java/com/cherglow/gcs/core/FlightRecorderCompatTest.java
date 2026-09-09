package com.cherglow.gcs.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** S16：会话文件 v1/v2 兼容读取 + GPS 列解析。 */
class FlightRecorderCompatTest {

    @TempDir
    Path tmp;

    @Test
    void loadV2WithGpsColumns() throws Exception {
        Path f = tmp.resolve("session_v2.skylog");
        Files.writeString(f,
                "{\"version\":2,\"start\":1000,\"count\":1,\"reason\":\"解锁\"}\n"
                        + "500,1.5,2.5,3.5,4.0,20,30,40,50,31.1774276,121.5272106,1.8\n",
                StandardCharsets.UTF_8);
        FlightRecorder.Session s = FlightRecorder.load(f);
        assertEquals(2, s.version());
        assertTrue(s.hasGps());
        assertEquals(1, s.count());
        double[] row = s.samples().get(0);
        assertEquals(500, row[0]);
        assertEquals(31.1774276, row[9], 1e-9, "v2 GPS 纬度列");
        assertEquals(121.5272106, row[10], 1e-9, "v2 GPS 经度列");
        assertEquals(1.8, row[11], 1e-9, "v2 GPS 地速列");
    }

    @Test
    void loadV2MissingGpsCellsAsNaN() throws Exception {
        Path f = tmp.resolve("session_v2_nogps.skylog");
        Files.writeString(f,
                "{\"version\":2,\"start\":2000,\"count\":1}\n"
                        + "100,0,0,0,3.8,10,10,10,10,,,\n", // 后三列 GPS 空 = NaN
                StandardCharsets.UTF_8);
        FlightRecorder.Session s = FlightRecorder.load(f);
        double[] row = s.samples().get(0);
        assertTrue(Double.isNaN(row[9]), "无 GPS 应解析为 NaN");
        assertTrue(Double.isNaN(row[10]));
        assertTrue(Double.isNaN(row[11]));
    }

    /** 旧格式（9 列，无 version 字段）必须能兼容读取，GPS 字段为 NaN */
    @Test
    void loadV1LegacyNineColumns() throws Exception {
        Path f = tmp.resolve("session_legacy.skylog");
        Files.writeString(f,
                "{\"start\":100,\"count\":2}\n"
                        + "0,1,2,3,4.0,10,20,30,40\n"
                        + "100,1,2,3,4.0,11,21,31,41\n",
                StandardCharsets.UTF_8);
        FlightRecorder.Session s = FlightRecorder.load(f);
        assertEquals(1, s.version(), "无 version=旧 v1");
        assertFalse(s.hasGps());
        assertEquals(2, s.count());
        // 9 列长度：第 9 个索引（GPS lat）不存在 → 越界语义：v1 无 GPS
        double[] row = s.samples().get(0);
        assertEquals(9, row.length, "v1 采样应为 9 列");
    }

    @Test
    void loadParsesModelAndDateKey() throws Exception {
        Path f = tmp.resolve("session_20260908_121500.skylog");
        Files.writeString(f,
                "{\"version\":2,\"start\":1,\"count\":1,\"model\":\"SkyLink-X\"}\n"
                        + "0,1,2,3,4.0,10,20,30,40,28.68,115.88,2.5\n",
                StandardCharsets.UTF_8);
        FlightRecorder.Session s = FlightRecorder.load(f);
        assertEquals("SkyLink-X", s.model(), "元数据应解析型号");
        assertEquals("20260908", s.dateKey(), "文件名应提取日期键");
    }

    @Test
    void exportCsvWritesBomAndGpsHeader() throws Exception {
        Path f = tmp.resolve("session_export.skylog");
        Files.writeString(f,
                "{\"version\":2,\"start\":1,\"count\":1}\n"
                        + "250,1,2,3,4.0,10,20,30,40,28.68,115.88,2.5\n",
                StandardCharsets.UTF_8);
        FlightRecorder.Session s = FlightRecorder.load(f);
        java.util.Optional<Path> out = FlightRecorder.exportCsv(s, 80);
        assertTrue(out.isPresent());
        String csv = Files.readString(out.get(), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF"), "应含 UTF-8 BOM（Excel 中文兼容）");
        assertTrue(csv.contains("lat_deg,lon_deg,speed_mps"));
        assertTrue(csv.contains("28.68,115.88,2.5\n"));
    }
}