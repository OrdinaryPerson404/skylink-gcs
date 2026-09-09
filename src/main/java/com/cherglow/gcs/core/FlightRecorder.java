package com.cherglow.gcs.core;

import com.cherglow.gcs.model.LiveVehicle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 飞行记录器（S11）：解锁(armed=true)自动开始记录，上锁/断开自动停止并落盘。
 * 会话文件 = ~/.skylink/logs/session_<时间戳>.skylink
 * 首行为元数据 JSON（version/start/count），其后每行 CSV：
 *   v3: tRelMs,roll,pitch,yaw,volt,m1..m4,lat,lon,spd,temp_c,humidity_pct,pressure_hpa （环境三列）
 *   v2: ...12列 + GPS 列
 *   v1: 9 列（tRelMs..m4，无 GPS），旧文件兼容读取
 */
public final class FlightRecorder {

    /** 当前会话文件格式版本号（S16 起为 2，引入 GPS 列；本次升版 3，追加环境列） */
    public static final int FORMAT_VERSION = 3;

    public static final Path LOG_DIR = Path.of(System.getProperty("user.home"), ".skylink", "logs");

    private static final FlightRecorder INSTANCE = new FlightRecorder();

    public static FlightRecorder get() {
        return INSTANCE;
    }

    private final LiveVehicle lv = LiveVehicle.get();
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private final BooleanProperty recording = new SimpleBooleanProperty(this, "recording", false);

    private Timeline sampler;
    private boolean writing;
    private long startMs;
    private final List<double[]> samples = new ArrayList<>();

    private FlightRecorder() {
        lv.armed.addListener((o, a, armed) -> {
            if (armed && !recording.get()) {
                start("解锁");
            } else if (!armed && recording.get()) {
                stopAndSave("上锁");
            }
        });
        AppState.get().connStatusProperty().addListener((o, a, b) -> {
            if ((b == AppState.ConnStatus.DISCONNECTED || b == AppState.ConnStatus.ERROR)
                    && recording.get()) {
                stopAndSave("连接断开");
            }
        });
        sampler = new Timeline(new KeyFrame(Duration.millis(250), e -> {
            if (!writing) {
                return;
            }
            double roll = lv.rollDeg.get();
            double pitch = lv.pitchDeg.get();
            double yaw = lv.yawDeg.get();
            double volt = lv.batteryVoltage.get();
            float[] mo = lv.motors.get();
            double lat = lv.latDeg.get();
            double lon = lv.lonDeg.get();
            double spd = lv.spdMps.get();
            double[] s = new double[15];
            s[0] = System.currentTimeMillis() - startMs;
            s[1] = roll;
            s[2] = pitch;
            s[3] = yaw;
            s[4] = volt;
            for (int i = 0; i < 4; i++) {
                s[5 + i] = mo != null && i < mo.length ? mo[i] * 100 : Double.NaN;
            }
            s[9] = lv.gpsFix.get() ? lat : Double.NaN;
            s[10] = lv.gpsFix.get() ? lon : Double.NaN;
            s[11] = lv.gpsFix.get() ? spd : Double.NaN;
            s[12] = lv.temperatureC.get();
            s[13] = lv.humidityPct.get();
            s[14] = lv.pressureHpa.get();
            samples.add(s);
        }));
        sampler.setCycleCount(Timeline.INDEFINITE);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (recording.get()) {
                stopAndSave("程序退出");
            }
        }));
    }

    public void addListener(Consumer<String> l) {
        listeners.add(l);
    }

    public BooleanProperty recordingProperty() {
        return recording;
    }

    public boolean isRecording() {
        return recording.get();
    }

    private void start(String reason) {
        if (recording.get()) {
            return;
        }
        writing = true;
        recording.set(true);
        startMs = System.currentTimeMillis();
        samples.clear();
        sampler.play();
        emit("开始记录飞行数据（" + reason + "）");
    }

    public void stopAndSave(String reason) {
        if (!recording.get()) {
            return;
        }
        writing = false;
        recording.set(false);
        sampler.stop();
        int n = samples.size();
        if (n == 0) {
            emit("记录数据为空，未保存（" + reason + "）");
            return;
        }
        try {
            Files.createDirectories(LOG_DIR);
            String ts = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path f = LOG_DIR.resolve("session_" + ts + ".skylog");
            StringBuilder sb = new StringBuilder();
            sb.append("{\"version\":").append(FORMAT_VERSION)
                    .append(",\"start\":").append(startMs).append(",\"count\":").append(n)
                    .append(",\"reason\":\"").append(reason.replace("\"", "'"))
                    .append("\",\"model\":\"").append(currentModel()).append("\"}\n");
            for (double[] s : samples) {
                sb.append((long) s[0]);
                for (int i = 1; i < s.length; i++) {
                    sb.append(',').append(s[i] != s[i] ? "" : String.valueOf(s[i]));
                }
                sb.append('\n');
            }
            Files.writeString(f, sb.toString(), StandardCharsets.UTF_8);
            emit("已保存飞行记录 " + n + " 点 → " + f.getFileName());
        } catch (IOException e) {
            emit("飞行记录保存失败：" + e.getMessage());
        }
    }

    private void emit(String msg) {
        for (Consumer<String> l : listeners) {
            l.accept(msg);
        }
    }

    // ---- 会话文件 ----

    public record Session(Path file, int version, long startMs, int count, String model,
                          List<double[]> samples) {
        public String displayName() {
            return file.getFileName().toString();
        }

        /** 是否为含 GPS 列(v2)的会话 */
        public boolean hasGps() {
            return version >= 2;
        }

        /** 是否为含环境参数列(v3)的会话 */
        public boolean hasEnv() {
            return version >= 3;
        }

        /** 由文件名 session_yyyyMMdd_HHmmss 提取日期键 yyyyMMdd，"未知"兜底。 */
        public String dateKey() {
            Matcher m = Pattern.compile("(\\d{8})").matcher(file.getFileName().toString());
            return m.find() ? m.group(1) : "未知";
        }
    }

    private static String currentModel() {
        String m = LiveVehicle.get().imuModel.get();
        return (m == null || m.isBlank()) ? "未知" : m;
    }

    /** 会话导出为 CSV（UTF-8 BOM，Excel 中文兼容）；含 GPS 列则附航程预测列。 */
    public static java.util.Optional<Path> exportCsv(Session session, int batteryPct) {
        return exportCsv(session, batteryPct, null);
    }

    /** 备份到本地 CSV：target 为空时写入日志目录默认名。 */
    public static java.util.Optional<Path> exportCsv(Session session, int batteryPct, Path target) {
        try {
            Files.createDirectories(LOG_DIR);
            String base = session.file().getFileName().toString().replaceFirst("(?i)\\.skylog$", "");
            Path out = target != null ? target : LOG_DIR.resolve(base + ".csv");
            StringBuilder sb = new StringBuilder("\uFEFF");
            String header = "t_rel_ms,roll_deg,pitch_deg,yaw_deg,battery_v,m1_pct,m2_pct,m3_pct,m4_pct";
            if (session.hasGps()) {
                header += ",lat_deg,lon_deg,speed_mps";
            }
            if (session.version() >= 3) {
                header += ",temp_c,humidity_pct,pressure_hpa";
            }
            sb.append(header).append('\n');
            for (double[] s : session.samples()) {
                sb.append((long) s[0]);
                for (int i = 1; i < s.length; i++) {
                    sb.append(',').append(s[i] == s[i] ? String.valueOf(s[i]) : "");
                }
                sb.append('\n');
            }
            Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
            return java.util.Optional.of(out);
        } catch (IOException e) {
            return java.util.Optional.empty();
        }
    }

    /** 从备份 CSV 恢复为会话（识别含 GPS 列与否；型号取文件名前缀，未知兜底）。 */
    public static Session loadCsv(Path f) {
        try {
            List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return new Session(f, FORMAT_VERSION, 0, 0, "CSV", List.of());
            }
            String head = lines.get(0);
            boolean env = head.contains("temp_c");
            boolean gps = head.contains("lat_deg");
            int cols = env ? 15 : gps ? 12 : 9;
            int version = env ? FORMAT_VERSION : gps ? 2 : 1;
            List<double[]> samples = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String ln = lines.get(i);
                if (ln.isBlank()) {
                    continue;
                }
                String[] parts = ln.split(",", -1);
                double[] s = new double[cols];
                for (int j = 0; j < cols; j++) {
                    s[j] = j < parts.length && !parts[j].isBlank()
                            ? Double.parseDouble(parts[j]) : Double.NaN;
                }
                samples.add(s);
            }
            long start = samples.isEmpty() ? 0 : (long) samples.get(0)[0];
            return new Session(f, version, start, samples.size(), "CSV", samples);
        } catch (Exception e) {
            return new Session(f, FORMAT_VERSION, 0, 0, "CSV", List.of());
        }
    }

    public static List<Path> sessions() {
        if (!Files.isDirectory(LOG_DIR)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(LOG_DIR)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".skylog"))
                    .sorted((a, b) -> b.compareTo(a))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** 解析会话文件：首行元数据，其后 CSV 行（空字段=NaN）。v2=12 列，v1=9 列（旧文件兼容）。 */
    public static Session load(Path f) {
        try {
            List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return new Session(f, FORMAT_VERSION, 0, 0, "", List.of());
            }
            long start = 0;
            int version = 1; // 老文件默认 v1（9 列）
            String model = "";
            Matcher m = Pattern.compile("\"start\"\\s*:\\s*(\\d+)").matcher(lines.get(0));
            if (m.find()) {
                start = Long.parseLong(m.group(1));
            }
            Matcher vm = Pattern.compile("\"version\"\\s*:\\s*(\\d+)").matcher(lines.get(0));
            if (vm.find()) {
                version = Integer.parseInt(vm.group(1));
            }
            Matcher mm = Pattern.compile("\"model\"\\s*:\\s*\"([^\"]*)\"").matcher(lines.get(0));
            if (mm.find()) {
                model = mm.group(1);
            }
            int cols = version >= 3 ? 15 : version >= 2 ? 12 : 9;
            List<double[]> samples = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",", -1);
                if (parts.length < 9) {
                    continue;
                }
                double[] s = new double[cols];
                for (int j = 0; j < cols; j++) {
                    s[j] = j < parts.length && !parts[j].isEmpty()
                            ? Double.parseDouble(parts[j]) : Double.NaN;
                }
                samples.add(s);
            }
            return new Session(f, version, start, samples.size(), model, samples);
        } catch (Exception e) {
            return new Session(f, FORMAT_VERSION, 0, 0, "", List.of());
        }
    }
}
