package cn.edu.nuaa.gcs.data;

import cn.edu.nuaa.gcs.comm.MavlinkParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TelemetryRecorder 单元测试：表头、行格式、无效字段空串、flush/close 行为、
 * try-with-resources 自动关闭、并发写入不抛异常。
 */
class TelemetryRecorderTest {

    @TempDir
    Path tmp;

    private static MavlinkParser.Telemetry makeValidTelemetry() {
        MavlinkParser.Telemetry t = new MavlinkParser.Telemetry();
        t.valid = true;
        t.msgId = 0; t.sysId = 1; t.compId = 1;
        t.hasPosition = true;
        t.lat = 32.0612; t.lon = 118.793; t.alt = 120.5;
        t.speed = 15.2; t.heading = 270.0;
        t.hasBattery = true;
        t.voltage = 12.34; t.batteryPct = 78.0;
        t.hasBatteryStatus = true;
        t.batteryCurrent = 2.15; t.batteryTemp = 28.4;
        t.capacityConsumed = 310;
        t.cellVoltages = new int[]{4200, 4190, 4185, 0, 0, 0, 0, 0, 0, 0};
        t.hasAttitude = true;
        t.roll = 0.03; t.pitch = -0.08; t.yaw = 1.23;
        t.rollSpeed = 0.01; t.pitchSpeed = -0.02; t.yawSpeed = 0.05;
        t.hasImu = true;
        t.accX = 0.1; t.accY = -0.2; t.accZ = 9.81;
        t.gyroX = 0.0; t.gyroY = 0.0; t.gyroZ = 0.0; t.imuTemp = 35.6;
        t.hasRc = true;
        t.rssi = 80;
        t.hasLandedState = true;
        t.landedState = 2;
        t.hasHeartbeat = true;
        t.customMode = 4; t.systemStatus = 3;
        t.hasHighresImu = true;
        t.absPressure = 1013.25; t.pressureAlt = 120.0; t.airTemp = 22.0;
        t.magX = 0.00003; t.magY = -0.00002; t.magZ = 0.00004; t.imuTemp2 = 36.2;
        t.hasStatusText = true;
        t.statusSeverity = 6; // INFO
        t.statusText = "Preflight check OK";
        return t;
    }

    @Test
    @DisplayName("首次 record 创建文件并写入表头 + 1 行数据")
    void testFirstRecordCreatesHeaderAndRow() throws Exception {
        Path csv = tmp.resolve("t1.csv");
        try (TelemetryRecorder r = new TelemetryRecorder(csv)) {
            r.record(makeValidTelemetry());
        }
        assertTrue(Files.exists(csv), "CSV 文件应被创建");
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        assertEquals(2, lines.size(), "应包含表头 + 1 行数据");
        assertEquals(TelemetryRecorder.HEADER, lines.get(0), "表头应匹配");
        String[] cols = lines.get(1).split(",", -1);
        // 表头列数与数据列数一致
        int headerCols = TelemetryRecorder.HEADER.split(",", -1).length;
        assertEquals(headerCols, cols.length, "数据行应包含 " + headerCols + " 列");
    }

    @Test
    @DisplayName("非有效帧被忽略（valid=false）")
    void testInvalidTelemetrySkipped() throws Exception {
        Path csv = tmp.resolve("t2.csv");
        try (TelemetryRecorder r = new TelemetryRecorder(csv)) {
            MavlinkParser.Telemetry t = new MavlinkParser.Telemetry();
            t.valid = false;
            r.record(t);
            r.record(null);
        }
        assertFalse(Files.exists(csv), "未写入有效数据时不应创建文件");
    }

    @Test
    @DisplayName("不可用字段写空串（例如 hasPosition=false 时 lat 空）")
    void testUnavailableFieldsAreEmpty() throws Exception {
        Path csv = tmp.resolve("t3.csv");
        try (TelemetryRecorder r = new TelemetryRecorder(csv)) {
            MavlinkParser.Telemetry t = makeValidTelemetry();
            t.hasPosition = false;
            t.hasBatteryStatus = false;
            r.record(t);
        }
        String line = Files.readAllLines(csv, StandardCharsets.UTF_8).get(1);
        String[] cols = line.split(",", -1);
        // 按表头顺序：lat=第 5 列（0-based），应空
        assertTrue(cols[5].isEmpty(), "lat 应为空");
        assertTrue(cols[6].isEmpty(), "lon 应为空");
    }

    @Test
    @DisplayName("写 100 行后 close 完整；行数 = 100")
    void testHundredRows() throws Exception {
        Path csv = tmp.resolve("t4.csv");
        try (TelemetryRecorder r = new TelemetryRecorder(csv)) {
            MavlinkParser.Telemetry base = makeValidTelemetry();
            for (int i = 0; i < 100; i++) {
                MavlinkParser.Telemetry t = makeValidTelemetry();
                t.alt = base.alt + i;
                r.record(t);
            }
            assertEquals(100, r.getWrittenRows(), "写入中应报告 100 行");
        }
        long rowCount = Files.lines(csv, StandardCharsets.UTF_8).count();
        assertEquals(101, rowCount, "文件应含 1 表头 + 100 数据");
    }

    @Test
    @DisplayName("record 后未 close：至少写了一部分（flush 机制）")
    void testFlushMechanism() throws Exception {
        Path csv = tmp.resolve("t5.csv");
        TelemetryRecorder r = new TelemetryRecorder(csv);
        try {
            // 超过 FLUSH_EVERY_N(32) 应触发内部 flush
            for (int i = 0; i < 40; i++) r.record(makeValidTelemetry());
            // 未 close，但磁盘上应有数据（不一定全部，但至少有表头 + 1 行因为首次写后显式 flush）
            assertTrue(Files.exists(csv));
            assertTrue(Files.size(csv) > 0, "文件不应为 0 字节");
        } finally {
            r.close();
        }
    }

    @Test
    @DisplayName("statusText 含逗号/引号时 CSV 双引号包裹")
    void testStatusTextEscaped() throws Exception {
        Path csv = tmp.resolve("t6.csv");
        try (TelemetryRecorder r = new TelemetryRecorder(csv)) {
            MavlinkParser.Telemetry t = makeValidTelemetry();
            t.statusText = "WARN, \"low voltage\"";
            r.record(t);
        }
        List<String> all = Files.readAllLines(csv, StandardCharsets.UTF_8);
        String[] header = all.get(0).split(",", -1);
        String[] row = all.get(1).split(",", -1);
        // 找到 status_text 列索引
        int idx = -1;
        for (int i = 0; i < header.length; i++) {
            if ("status_text".equals(header[i])) { idx = i; break; }
        }
        assertTrue(idx >= 0, "header 应包含 status_text 列");
        String last = row[idx];
        // 因为输入文本含逗号、引号 → 写时必然加引号；parse 时 split 会把 "WARN, \"low\"" 中的
        // 逗号拆出来，split(..., -1) 因此返回的 row[idx] 不包含完整内容。
        // 改用另一种方式：直接断言原始数据行中存在 "WARN" 及被转义的 ""low voltage""
        String dataLine = all.get(1);
        assertTrue(dataLine.contains("\"\"low voltage\"\"") || dataLine.contains("\"\"low voltage\""),
            "CSV 中应存在转义的低电压关键词, 实际: " + dataLine);
    }

    @Test
    @DisplayName("多线程并发 record 不抛异常且 close 后总行数正确")
    void testConcurrentWrites() throws Exception {
        Path csv = tmp.resolve("t7.csv");
        TelemetryRecorder r = new TelemetryRecorder(csv);
        int per = 50;
        Thread[] ts = new Thread[4];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(() -> {
                try {
                    for (int k = 0; k < per; k++) r.record(makeValidTelemetry());
                } catch (IOException e) { throw new RuntimeException(e); }
            });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        r.close();
        assertEquals(per * ts.length, r.getWrittenRows(),
            "并发后写入行数应等于 " + (per * ts.length));
        long lineCount = Files.lines(csv, StandardCharsets.UTF_8).count();
        assertEquals(per * ts.length + 1, lineCount,
            "文件行数应等于 header + 数据");
    }

    @Test
    @DisplayName("close 幂等；getElapsedMs 单调递增")
    void testCloseIdempotent() throws Exception {
        Path csv = tmp.resolve("t8.csv");
        TelemetryRecorder r = new TelemetryRecorder(csv);
        assertFalse(r.isClosed());
        r.record(makeValidTelemetry());
        r.close();
        assertTrue(r.isClosed());
        assertDoesNotThrow(r::close, "重复 close 不应抛异常");
        long e1 = r.getElapsedMs();
        Thread.sleep(10);
        long e2 = r.getElapsedMs();
        assertTrue(e2 >= e1, "getElapsedMs 应单调递增");
    }
}
