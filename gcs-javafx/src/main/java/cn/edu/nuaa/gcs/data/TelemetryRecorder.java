package cn.edu.nuaa.gcs.data;

import cn.edu.nuaa.gcs.comm.MavlinkParser;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 遥测数据录制器：把 {@link MavlinkParser.Telemetry} 逐条写入 CSV 文件，
 * 可用于飞行后回放、诊断与离线分析。
 *
 * <p>特性：
 * <ul>
 *   <li>线程安全：{@link #record(MavlinkParser.Telemetry)} 可从任意线程调用（BufferedWriter 内部同步）。</li>
 *   <li>自动写表头（首次 record 时）；</li>
 *   <li>周期性 flush（每 N 条或 close 时），避免 IO 阻塞导致遥测丢失；</li>
 *   <li>不可用字段写空字符串，符合 "不可用显示 —" 原则（后续 Excel 分析时对应空单元格）。</li>
 *   <li>实现 {@link Closeable}，可在 try-with-resources 中自动关闭。</li>
 * </ul>
 *
 * <p>硬约束：本类只读遥测数据，不读取文件外的系统资源，不修改硬件配置。
 */
public class TelemetryRecorder implements Closeable {

    /** 每写入 N 条后 flush 一次（兼顾 IO 吞吐与数据安全性）。 */
    private static final int FLUSH_EVERY_N = 32;

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)
                             .withZone(ZoneId.systemDefault());

    private final Path file;
    private BufferedWriter out;
    private final long startedAtMs;
    private int writtenRows = 0;
    private volatile boolean closed = false;
    private final Object lock = new Object();

    /**
     * 创建录制器（但不立即打开文件，首次 record 时打开并写表头）。
     *
     * @param file 输出 CSV 路径；父目录需存在，否则抛出 IO 异常
     */
    public TelemetryRecorder(Path file) {
        this.file = file;
        this.startedAtMs = System.currentTimeMillis();
    }

    /** 输出文件路径。 */
    public Path getFile() { return file; }

    /** 已写入的行数（不含表头）。 */
    public int getWrittenRows() {
        synchronized (lock) { return writtenRows; }
    }

    /** 启动后经过的毫秒（诊断用）。 */
    public long getElapsedMs() { return System.currentTimeMillis() - startedAtMs; }

    public boolean isClosed() { return closed; }

    /**
     * 记录一帧遥测。首次调用会创建文件并写入 CSV 表头。
     *
     * @param t 遥测对象；非有效帧（t.valid=false）被忽略
     */
    public void record(MavlinkParser.Telemetry t) throws IOException {
        if (t == null || !t.valid || closed) return;
        synchronized (lock) {
            if (out == null) openAndWriteHeader();
            out.write(formatRow(t));
            out.newLine();
            writtenRows++;
            if (writtenRows % FLUSH_EVERY_N == 0) out.flush();
        }
    }

    /** 强制刷盘（用户主动调用，例如 UI 上 "保存" 按钮）。 */
    public void flush() throws IOException {
        synchronized (lock) {
            if (out != null) out.flush();
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (closed) return;
            closed = true;
            if (out != null) {
                out.flush();
                out.close();
                out = null;
            }
        }
    }

    // ================================================================
    // 内部实现
    // ================================================================

    private void openAndWriteHeader() throws IOException {
        boolean exists = Files.exists(file);
        Files.createDirectories(file.getParent() == null
                ? Path.of(".") : file.getParent());
        StandardOpenOption[] opts = exists
                ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND}
                : new StandardOpenOption[]{StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE};
        out = Files.newBufferedWriter(file, StandardCharsets.UTF_8, opts);
        if (!exists) {
            out.write(HEADER);
            out.newLine();
            out.flush();
        }
    }

    /** CSV 表头（与 formatRow 一一对应）。 */
    static final String HEADER = String.join(",",
        "epoch_s", "timestamp", "msg_id", "sys_id", "comp_id",
        "lat_deg", "lon_deg", "alt_m", "speed_mps", "heading_deg",
        "voltage_V", "battery_pct", "battery_current_A", "battery_temp_C",
        "capacity_consumed_mAh", "cell0_mV", "cell1_mV", "cell2_mV", "cell3_mV",
        "cell4_mV", "cell5_mV", "cell6_mV", "cell7_mV", "cell8_mV", "cell9_mV",
        "roll_rad", "pitch_rad", "yaw_rad",
        "rollspeed_rps", "pitchspeed_rps", "yawspeed_rps",
        "accx_ms2", "accy_ms2", "accz_ms2",
        "gyrox_rps", "gyroy_rps", "gyroz_rps", "imu_temp_C",
        "pressure_hpa", "pressure_alt_m", "air_temp_C",
        "magx_T", "magy_T", "magz_T", "imu2_temp_C",
        "rssi_pct", "landed_state",
        "custom_mode", "system_status", "status_severity", "status_text"
    );

    /**
     * 将单帧 Telemetry 格式化为 CSV 行。
     * 不可用字段写空串（null → ""）。
     */
    private String formatRow(MavlinkParser.Telemetry t) {
        double epoch = System.currentTimeMillis() / 1000.0;
        String ts = TS_FMT.format(Instant.ofEpochMilli(System.currentTimeMillis()));
        return String.join(",",
            fmt(epoch), ts,
            fmt(t.msgId), fmt(t.sysId), fmt(t.compId),
            t.hasPosition ? fmt(t.lat) : "",
            t.hasPosition ? fmt(t.lon) : "",
            t.hasPosition ? fmt(t.alt) : "",
            t.hasPosition ? fmt(t.speed) : "",
            t.hasPosition ? fmt(t.heading) : "",
            t.hasBattery ? fmt(t.voltage) : (t.hasBatteryStatus ? fmt(t.voltage) : ""),
            t.hasBattery ? fmt(t.batteryPct) : "",
            t.hasBatteryStatus ? fmt(t.batteryCurrent) : "",
            t.hasBatteryStatus ? fmt(t.batteryTemp) : "",
            t.hasBatteryStatus ? fmt(t.capacityConsumed) : "",
            t.hasBatteryStatus ? fmt(t.cellVoltages[0]) : "",
            t.hasBatteryStatus ? fmt(t.cellVoltages[1]) : "",
            t.hasBatteryStatus ? fmt(t.cellVoltages[2]) : "",
            t.hasBatteryStatus ? fmt(t.cellVoltages[3]) : "",
            t.hasBatteryStatus ? fmt(t.cellVoltages[4]) : "",
            t.hasBatteryStatus ? fmt(t.cellVoltages[5]) : "",
            t.hasBatteryStatus ? fmt(t.cellVoltages[6]) : "",
            t.hasBatteryStatus ? fmt(t.cellVoltages[7]) : "",
            t.hasBatteryStatus ? fmt(t.cellVoltages[8]) : "",
            t.hasBatteryStatus ? fmt(t.cellVoltages[9]) : "",
            t.hasAttitude ? fmt(t.roll) : "",
            t.hasAttitude ? fmt(t.pitch) : "",
            t.hasAttitude ? fmt(t.yaw) : "",
            t.hasAttitude ? fmt(t.rollSpeed) : "",
            t.hasAttitude ? fmt(t.pitchSpeed) : "",
            t.hasAttitude ? fmt(t.yawSpeed) : "",
            t.hasImu ? fmt(t.accX) : "",
            t.hasImu ? fmt(t.accY) : "",
            t.hasImu ? fmt(t.accZ) : "",
            t.hasImu ? fmt(t.gyroX) : "",
            t.hasImu ? fmt(t.gyroY) : "",
            t.hasImu ? fmt(t.gyroZ) : "",
            t.hasImu ? fmt(t.imuTemp) : "",
            t.hasHighresImu ? fmt(t.absPressure) : "",
            t.hasHighresImu ? fmt(t.pressureAlt) : "",
            t.hasHighresImu ? fmt(t.airTemp) : "",
            t.hasHighresImu ? fmt(t.magX) : "",
            t.hasHighresImu ? fmt(t.magY) : "",
            t.hasHighresImu ? fmt(t.magZ) : "",
            t.hasHighresImu ? fmt(t.imuTemp2) : "",
            t.hasRc ? fmt(t.rssi) : "",
            t.hasLandedState ? fmt(t.landedState) : "",
            fmt(t.customMode), fmt(t.systemStatus),
            t.hasStatusText ? fmt(t.statusSeverity) : "",
            csvStr(t.statusText)
        );
    }

    private static String fmt(double v) {
        if (!Double.isFinite(v)) return "";
        // 避免科学计数法导致 CSV 解析歧义
        return String.format(Locale.ROOT, "%.6f", v);
    }

    private static String fmt(int v) { return Integer.toString(v); }

    /** 字符串转 CSV 安全字段（含逗号/引号时包引号）。 */
    private static String csvStr(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.indexOf(',') < 0 && s.indexOf('"') < 0 && s.indexOf('\n') < 0) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
