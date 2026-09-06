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
 * 会话文件 = ~/.skylink/logs/session_<时间戳>.skylog
 * 首行为元数据 JSON（start/count），其后每行 CSV：tRelMs,roll,pitch,yaw,volt,m1,m2,m3,m4
 */
public final class FlightRecorder {

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
            double[] s = new double[9];
            s[0] = System.currentTimeMillis() - startMs;
            s[1] = roll;
            s[2] = pitch;
            s[3] = yaw;
            s[4] = volt;
            for (int i = 0; i < 4; i++) {
                s[5 + i] = mo != null && i < mo.length ? mo[i] * 100 : Double.NaN;
            }
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
            sb.append("{\"start\":").append(startMs).append(",\"count\":").append(n)
                    .append(",\"reason\":\"").append(reason.replace("\"", "'")).append("\"}\n");
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

    public record Session(Path file, long startMs, int count, List<double[]> samples) {
        public String displayName() {
            return file.getFileName().toString();
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

    /** 解析会话文件：首行元数据，其后 CSV 行（空字段=NaN） */
    public static Session load(Path f) {
        try {
            List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return new Session(f, 0, 0, List.of());
            }
            long start = 0;
            Matcher m = Pattern.compile("\"start\"\\s*:\\s*(\\d+)").matcher(lines.get(0));
            if (m.find()) {
                start = Long.parseLong(m.group(1));
            }
            List<double[]> samples = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",", -1);
                if (parts.length < 9) {
                    continue;
                }
                double[] s = new double[9];
                for (int j = 0; j < 9; j++) {
                    s[j] = parts[j].isEmpty() ? Double.NaN : Double.parseDouble(parts[j]);
                }
                samples.add(s);
            }
            return new Session(f, start, samples.size(), samples);
        } catch (Exception e) {
            return new Session(f, 0, 0, List.of());
        }
    }
}
