package cn.edu.nuaa.gcs.comm;

import cn.edu.nuaa.gcs.model.Drone;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 电池充电实时监控服务。
 *
 * <p>聚合两类真实数据源，实现"当前电脑对无人机电池充电状态"的实时数据交互与验证：
 * <ol>
 *   <li><b>系统电池（WMI）</b>：通过 Windows PowerShell 读取 {@code root/wmi:BatteryStatus}
 *       与 {@code Win32_Battery}，获取笔记本/地面站主机自身电池的电压、容量、充电速率、
 *       是否在充电、是否接入电源等。这是"当前电脑已连接电池并正在充电"最直接的真实来源。</li>
 *   <li><b>无人机电池（MAVLink）</b>：复用 {@link CommunicationService} 已解析的
 *       {@link MavlinkParser.Telemetry}（SYS_STATUS(1) / BATTERY_STATUS(147)），
 *       经 {@link Drone} 模型获取电压/电流/温度/电芯电压/剩余容量。</li>
 * </ol>
 *
 * <p>内置 {@link ChargingValidator} 对充电过程做一致性校验与异常检测（过压/欠压/过温/
 * 充电停滞/电流异常/电压-容量失配），异常通过 {@link Anomaly} 列表暴露给 UI。
 *
 * <p>设计为守护线程定时采样（默认 2s），UI 在 JavaFX EDT 周期调用 {@link #snapshot()}
 * 获取不可变快照，避免线程安全问题。非 Windows 平台 WMI 不可用时优雅降级。
 */
public class BatteryChargingMonitor {

    /** 单个电池数据源快照（不可变）。 */
    public static final class BatteryInfo {
        public final String id;
        public final String name;
        public final String source;          // "wmi" | "mavlink"
        public final boolean connected;
        public final String batteryId;
        public final double capacity;        // % (-1 = 未知)
        public final double voltage;         // V (-1 = 未知)
        public final double current;         // A (-1 = 未知)
        public final Double temperature;     // °C (null = 未知)
        public final Double power;           // W (null = 不适用)
        public final boolean charging;
        public final boolean powerOnline;
        public final int timeToFullMin;      // 分钟 (-1 = 未知)
        public final boolean valid;
        public final String note;            // 附加说明（如固件未上报）

        public BatteryInfo(String id, String name, String source, boolean connected, String batteryId,
                           double capacity, double voltage, double current, Double temperature, Double power,
                           boolean charging, boolean powerOnline, int timeToFullMin, boolean valid, String note) {
            this.id = id; this.name = name; this.source = source; this.connected = connected;
            this.batteryId = batteryId; this.capacity = capacity; this.voltage = voltage;
            this.current = current; this.temperature = temperature; this.power = power;
            this.charging = charging; this.powerOnline = powerOnline; this.timeToFullMin = timeToFullMin;
            this.valid = valid; this.note = note;
        }
    }

    /** 充电异常项。 */
    public static final class Anomaly {
        public final String code;
        public final String level;   // "warn" | "err"
        public final String msg;
        public final String batteryId;

        public Anomaly(String code, String level, String msg, String batteryId) {
            this.code = code; this.level = level; this.msg = msg; this.batteryId = batteryId;
        }
    }

    /** 验证摘要。 */
    public static final class ValidationSummary {
        public final int checkedBatteries;
        public final int totalAnomalies;
        public final int errors;
        public final int warnings;
        public final String status;   // "pass" | "warn" | "err"

        public ValidationSummary(int checked, int total, int errors, int warnings, String status) {
            this.checkedBatteries = checked; this.totalAnomalies = total;
            this.errors = errors; this.warnings = warnings; this.status = status;
        }
    }

    /** 完整快照（不可变）。 */
    public static final class Snapshot {
        public final long timestampMs;
        public final List<BatteryInfo> batteries;
        public final List<Anomaly> anomalies;
        public final ValidationSummary validation;
        public final boolean wmiAvailable;
        public final boolean mavlinkConnected;
        public final String mavlinkPort;
        public final boolean mavlinkBatterySupported;

        public Snapshot(long ts, List<BatteryInfo> batteries, List<Anomaly> anomalies,
                        ValidationSummary validation, boolean wmiAvailable,
                        boolean mavlinkConnected, String mavlinkPort, boolean mavlinkBatterySupported) {
            this.timestampMs = ts; this.batteries = batteries; this.anomalies = anomalies;
            this.validation = validation; this.wmiAvailable = wmiAvailable;
            this.mavlinkConnected = mavlinkConnected; this.mavlinkPort = mavlinkPort;
            this.mavlinkBatterySupported = mavlinkBatterySupported;
        }
    }

    // ---- WMI 读取 ----
    private static final String WMI_PS =
        "$ErrorActionPreference='SilentlyContinue';" +
        "$bs=Get-CimInstance -Namespace root/wmi -Class BatteryStatus;" +
        "$wb=Get-CimInstance Win32_Battery;" +
        "$sd=Get-CimInstance -Namespace root/wmi -Class BatteryStaticData;" +
        "$fc=Get-CimInstance -Namespace root/wmi -Class BatteryFullChargedCapacity;" +
        "[ordered]@{" +
        "EstimatedChargeRemaining=$wb.EstimatedChargeRemaining;" +
        "BatteryStatus=$wb.BatteryStatus;" +
        "DesignVoltage=$wb.DesignVoltage;" +
        "Voltage=$bs.Voltage;" +
        "ChargeRate=$bs.ChargeRate;" +
        "DischargeRate=$bs.DischargeRate;" +
        "Charging=([bool]$bs.Charging);" +
        "Discharging=([bool]$bs.Discharging);" +
        "PowerOnline=([bool]$bs.PowerOnline);" +
        "RemainingCapacity=$bs.RemainingCapacity;" +
        "DesignedCapacity=$sd.DesignedCapacity;" +
        "FullChargeCapacity=$fc.FullChargedCapacity;" +
        "Name=$wb.Name;" +
        "DeviceID=$wb.DeviceID" +
        "} | ConvertTo-Json -Compress";

    private final Drone drone;
    private final CommunicationService comm;
    private final ScheduledExecutorService scheduler;
    private final AtomicReference<Snapshot> lastSnapshot = new AtomicReference<>();
    private final ChargingValidator validator = new ChargingValidator();
    private final boolean isWindows;

    public BatteryChargingMonitor(Drone drone, CommunicationService comm) {
        this.drone = drone;
        this.comm = comm;
        this.isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BatteryChargingMonitor");
            t.setDaemon(true);
            return t;
        });
    }

    /** 启动定时采样（默认 2 秒）。 */
    public void start() {
        scheduler.scheduleAtFixedRate(this::poll, 1, 2, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
        try { scheduler.awaitTermination(1, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
    }

    /** 获取最近一次快照（线程安全）。 */
    public Snapshot snapshot() {
        Snapshot s = lastSnapshot.get();
        if (s != null) return s;
        return buildSnapshot();
    }

    private void poll() {
        try {
            Snapshot s = buildSnapshot();
            lastSnapshot.set(s);
        } catch (Exception e) {
            // 静默：避免守护线程异常退出
        }
    }

    private Snapshot buildSnapshot() {
        List<BatteryInfo> batteries = new ArrayList<>();
        List<Anomaly> anomalies = new ArrayList<>();

        // 1. WMI 系统电池
        WmiData wmi = readWmi();
        if (wmi != null && wmi.chargeRemaining >= 0) {
            double voltage = (wmi.voltage > 0 ? wmi.voltage : wmi.designVoltage) / 1000.0;
            double powerW = wmi.chargeRate / 1000.0;
            double currentA = (voltage > 0 && powerW > 0) ? powerW / voltage : 0.0;
            boolean charging = wmi.charging;
            int ttf = 0;
            int fullMwh = wmi.remainingCapacity > 0 && wmi.chargeRemaining > 0
                    ? (int) (wmi.remainingCapacity / wmi.chargeRemaining * 100) : wmi.remainingCapacity;
            if (charging && wmi.chargeRate > 0 && wmi.chargeRemaining < 100) {
                ttf = (int) Math.round((fullMwh - wmi.remainingCapacity) / (double) wmi.chargeRate * 60.0);
            }
            int health = -1;
            if (wmi.designedCapacity > 0 && wmi.fullChargeCapacity > 0) {
                health = (int) Math.round(wmi.fullChargeCapacity * 100.0 / wmi.designedCapacity);
            }
            BatteryInfo bat = new BatteryInfo(
                    "wmi-system", "系统电池 (" + (wmi.name != null ? wmi.name : "主机") + ")", "wmi",
                    true, wmi.deviceId != null ? wmi.deviceId : "SYSTEM",
                    wmi.chargeRemaining, round2(voltage), round3(currentA), null,
                    round2(powerW), charging, wmi.powerOnline, ttf, true, null);
            batteries.add(bat);
            anomalies.addAll(validator.observe(bat.id, wmi.chargeRemaining, voltage, currentA,
                    null, charging, voltage, health));
        }

        // 2. MAVLink 无人机电池
        boolean mavConnected = comm != null && comm.isLinkActive();
        String mavPort = comm != null ? comm.getPortName() : null;
        boolean mavBatSupported = drone != null && drone.isBatteryStatusAvailable();
        if (drone != null && drone.isBatteryAvailable()) {
            double cap = drone.getBatteryPct();
            double v = drone.getVoltage();
            double cur = drone.getBatteryCurrent();
            double temp = drone.getBatteryTemp();
            boolean charging = cur > 0.01;
            int[] cells = drone.getCellVoltages();
            int validCells = 0;
            double cellSum = 0;
            if (cells != null) {
                for (int c : cells) {
                    if (c > 0 && c < 0xFFFF) { validCells++; cellSum += c / 1000.0; }
                }
            }
            double mavV = (cellSum > 0) ? cellSum : v;
            BatteryInfo mavBat = new BatteryInfo(
                    "mavlink-drone", "无人机电池 (MAVLink" + (mavPort != null ? " " + mavPort : "") + ")",
                    "mavlink", true, "MAV_BAT",
                    cap >= 0 ? cap : -1, round2(mavV), round3(cur),
                    temp > -100 ? temp : null, null,
                    charging, true, 0, true, null);
            batteries.add(mavBat);
            anomalies.addAll(validator.observe(mavBat.id, cap, mavV, cur, temp, charging, 12.6, -1));
        } else if (mavConnected) {
            String note = mavBatSupported
                    ? null
                    : "飞控链路已连接，但固件未上报 BATTERY_STATUS(147)/SYS_STATUS(1) 电池遥测";
            BatteryInfo mavBat = new BatteryInfo(
                    "mavlink-drone", "无人机电池 (MAVLink" + (mavPort != null ? " " + mavPort : "") + ")",
                    "mavlink", false, null, -1, -1, -1, null, null,
                    false, false, 0, false, note);
            batteries.add(mavBat);
        }

        // 验证摘要
        int checked = 0, errs = 0, warns = 0;
        for (BatteryInfo b : batteries) if (b.valid) checked++;
        for (Anomaly a : anomalies) {
            if ("err".equals(a.level)) errs++; else if ("warn".equals(a.level)) warns++;
        }
        String status = anomalies.isEmpty() ? "pass" : (errs > 0 ? "err" : "warn");
        ValidationSummary val = new ValidationSummary(checked, anomalies.size(), errs, warns, status);

        return new Snapshot(System.currentTimeMillis(), batteries, anomalies, val,
                wmi != null, mavConnected, mavPort, mavBatSupported);
    }

    // ---- WMI 读取实现 ----
    private static final class WmiData {
        int chargeRemaining = -1;
        int batteryStatus = 0;
        int designVoltage = 0;
        int voltage = 0;
        int chargeRate = 0;
        int dischargeRate = 0;
        boolean charging = false;
        boolean discharging = false;
        boolean powerOnline = false;
        int remainingCapacity = 0;
        int designedCapacity = 0;
        int fullChargeCapacity = 0;
        String name = null;
        String deviceId = null;
    }

    private WmiData readWmi() {
        if (!isWindows) return null;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", WMI_PS);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            p.waitFor(5, TimeUnit.SECONDS);
            if (p.isAlive()) p.destroyForcibly();
            String json = sb.toString().trim();
            if (json.isEmpty() || !json.startsWith("{")) return null;
            return parseWmiJson(json);
        } catch (Exception e) {
            return null;
        }
    }

    private WmiData parseWmiJson(String json) {
        WmiData d = new WmiData();
        d.chargeRemaining = jsonInt(json, "EstimatedChargeRemaining", -1);
        d.batteryStatus = jsonInt(json, "BatteryStatus", 0);
        d.designVoltage = jsonInt(json, "DesignVoltage", 0);
        d.voltage = jsonInt(json, "Voltage", 0);
        d.chargeRate = jsonInt(json, "ChargeRate", 0);
        d.dischargeRate = jsonInt(json, "DischargeRate", 0);
        d.charging = jsonBool(json, "Charging");
        d.discharging = jsonBool(json, "Discharging");
        d.powerOnline = jsonBool(json, "PowerOnline");
        d.remainingCapacity = jsonInt(json, "RemainingCapacity", 0);
        d.designedCapacity = jsonInt(json, "DesignedCapacity", 0);
        d.fullChargeCapacity = jsonInt(json, "FullChargeCapacity", 0);
        d.name = jsonStr(json, "Name");
        d.deviceId = jsonStr(json, "DeviceID");
        return d;
    }

    private static int jsonInt(String json, String key, int def) {
        String s = jsonVal(json, key);
        if (s == null || "null".equals(s)) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
    private static boolean jsonBool(String json, String key) {
        return "true".equals(jsonVal(json, key));
    }
    private static String jsonStr(String json, String key) {
        String s = jsonVal(json, key);
        if (s == null || "null".equals(s)) return null;
        if (s.startsWith("\"") && s.endsWith("\"")) return s.substring(1, s.length() - 1);
        return s;
    }
    private static String jsonVal(String json, String key) {
        String pat = "\"" + key + "\":";
        int i = json.indexOf(pat);
        if (i < 0) return null;
        int start = i + pat.length();
        if (start >= json.length()) return null;
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return end > 0 ? json.substring(start, end + 1) : null;
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(start, end).trim();
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private static double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }

    // ---- 充电验证器 ----
    private static final class ChargingValidator {
        private final java.util.Map<String, Deque<double[]>> history = new java.util.concurrent.ConcurrentHashMap<>();

        List<Anomaly> observe(String bid, double capacity, double voltage, double current,
                              Double temperature, boolean charging, double maxVoltage, int health) {
            List<Anomaly> anomalies = new ArrayList<>();
            long now = System.currentTimeMillis();
            Deque<double[]> hist = history.computeIfAbsent(bid, k -> new ConcurrentLinkedDeque<>());
            hist.addLast(new double[]{now, capacity});
            while (hist.size() > 120) hist.pollFirst();

            if (temperature != null) {
                double t = temperature;
                if (t > 60) anomalies.add(new Anomaly("over_temp_crit", "err",
                        "电池温度严重过高 (" + fmt1(t) + "°C > 60°C)", bid));
                else if (t > 45) anomalies.add(new Anomaly("over_temp", "warn",
                        "充电温度过高 (" + fmt1(t) + "°C)", bid));
            }
            if (voltage > 0 && maxVoltage > 0 && voltage > maxVoltage * 1.03) {
                anomalies.add(new Anomaly("over_voltage", "err",
                        "电压超限 (" + fmt2(voltage) + "V > " + fmt2(maxVoltage * 1.03) + "V)", bid));
            }
            if (voltage > 0 && voltage < 9.0 && capacity > 5) {
                anomalies.add(new Anomaly("under_voltage", "err",
                        "电压过低 (" + fmt2(voltage) + "V) 但容量未耗尽，疑似电芯异常", bid));
            }
            if (charging && hist.size() >= 6) {
                Object[] arr = hist.toArray();
                double[] first = (double[]) arr[0];
                double[] last = (double[]) arr[arr.length - 1];
                double dt = (last[0] - first[0]) / 1000.0;
                if (dt > 30 && (last[1] - first[1]) < 0.05) {
                    anomalies.add(new Anomaly("charging_stalled", "warn",
                            "充电停滞：" + (int) dt + "s 内容量增幅 <0.05%", bid));
                }
            }
            if (charging && current >= 0 && current > 5.0) {
                anomalies.add(new Anomaly("over_current", "err",
                        "充电电流过大 (" + fmt2(current) + "A)", bid));
            }
            if (charging && current >= 0 && current < 0.01 && capacity >= 0 && capacity < 99) {
                anomalies.add(new Anomaly("current_anomaly", "warn",
                        "标注充电中但电流≈0 (" + fmt2(current) + "A)", bid));
            }
            if (voltage > 0 && capacity >= 0 && maxVoltage > 0) {
                double est = Math.max(0, Math.min(100, (voltage - 10.5) / (maxVoltage - 10.5) * 100));
                if (Math.abs(est - capacity) > 20) {
                    anomalies.add(new Anomaly("voltage_capacity_mismatch", "warn",
                            "电压-容量失配：电压估算 " + (int) est + "% vs 上报 " + (int) capacity + "%", bid));
                }
            }
            return anomalies;
        }
    }

    private static String fmt1(double v) { return String.format("%.1f", v); }
    private static String fmt2(double v) { return String.format("%.2f", v); }
}
