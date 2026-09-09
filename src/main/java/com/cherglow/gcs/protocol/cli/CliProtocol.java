package com.cherglow.gcs.protocol.cli;

import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CF-Drone CLI 文本协议（COM5@115200 实测口径，黄金样本=真机抓包）。
 * 解析器对标签内中文用 \([^)]*\) 通配，对缺失字段返回 null/NaN，容忍 \r\n 与行间空格差异。
 */
public final class CliProtocol {

    private CliProtocol() {
    }

    // ---- 命令 ----
    public static final String CMD_PS = "ps";
    public static final String CMD_PSQ = "psq";
    public static final String CMD_RC = "rc";
    public static final String CMD_MOT = "mot";
    public static final String CMD_IMU = "imu";
    public static final String CMD_STATUS = "status";
    public static final String CMD_PARAMS = "p";
    public static final String CMD_SYS = "sys";
    public static final String CMD_WIFI = "wifi";
    public static final String CMD_TIME = "time";

    // ---- 数据载体 ----
    public record Euler(double rollDeg, double pitchDeg, double yawDeg) {
    }

    public record Quat(double qw, double qx, double qy, double qz) {
    }

    public record Motors(double frontRight, double frontLeft, double rearRight, double rearLeft) {
    }

    public record RcData(int[] channels, double roll, double pitch, double yaw, double throttle,
                         String modeName, String controlSource, double time) {
    }

    public record ImuData(String status, String model, double[] gyro, double[] acc, boolean landed) {
    }

    public record StatusData(boolean armed, String controlSource, boolean rcLinkUp, boolean webLinkUp,
                             boolean mavLinkUp, int loopRate, String armingDisabled, String phase,
                             boolean isAirborne, boolean landed, double batteryVoltage, Double altitude,
                             Integer tofStatus, boolean baro, boolean range, boolean hasAltitude, boolean magOk,
                             int crsfLinkQuality, Double temperatureC, Double humidityPct, Double pressureHpa) {
    }

    public record SysData(String chip, Double temperatureC, Long freeHeap, Integer loopRate,
                          Double humidityPct, Double pressureHpa) {
    }

    public record WifiData(String mode, String ssid, String ip, boolean mavlinkConnected) {
    }

    // ---- 正则（全部按真机抓包校准） ----
    private static final Pattern P_PS = Pattern.compile(
            "roll:\\s*(-?[\\d.]+)\\s+pitch:\\s*(-?[\\d.]+)\\s+yaw:\\s*(-?[\\d.]+)");
    private static final Pattern P_PSQ = Pattern.compile(
            "qw:\\s*(-?[\\d.]+)\\s+qx:\\s*(-?[\\d.]+)\\s+qy:\\s*(-?[\\d.]+)\\s+qz:\\s*(-?[\\d.]+)");
    private static final Pattern P_MOT = Pattern.compile(
            "front-right\\s+(-?[\\d.]+)\\s+front-left\\s+(-?[\\d.]+)\\s+rear-right\\s+(-?[\\d.]+)\\s+rear-left\\s+(-?[\\d.]+)");
    private static final Pattern P_RC_CH = Pattern.compile("CH(\\d+)=(-?\\d+)");
    private static final Pattern P_RC_CTRL = Pattern.compile(
            "roll:\\s*(-?(?:\\d+\\.?\\d*|nan))\\s+pitch:\\s*(-?(?:\\d+\\.?\\d*|nan))\\s+yaw:\\s*(-?(?:\\d+\\.?\\d*|nan))\\s+throttle:\\s*(-?(?:\\d+\\.?\\d*|nan))\\s+mode:\\s*(\\S+)");
    private static final Pattern P_RC_MODE = Pattern.compile("(?m)^mode:\\s*(\\w+)\\s*$");
    private static final Pattern P_TIME_LINE = Pattern.compile("(?m)^time:\\s*([\\d.]+)\\s*$");
    private static final Pattern P_CONTROL_SOURCE = Pattern.compile("controlSource\\([^)]*\\):[ \\t]*(.+?)[ \\t]*$", Pattern.MULTILINE);
    private static final Pattern P_IMU_MODEL = Pattern.compile("model:\\s*(.+?)\\s*$", Pattern.MULTILINE);
    private static final Pattern P_IMU_GYRO = Pattern.compile(
            "(?m)^gyro:\\s*(-?[\\d.]+)\\s+(-?[\\d.]+)\\s+(-?[\\d.]+)\\s*$");
    private static final Pattern P_IMU_ACC = Pattern.compile(
            "(?m)^acc:\\s*(-?[\\d.]+)\\s+(-?[\\d.]+)\\s+(-?[\\d.]+)\\s*$");
    private static final Pattern P_LANDED = Pattern.compile("landed\\([^)]*\\):\\s*(\\d)");
    private static final Pattern P_IMU_LANDED = Pattern.compile("(?m)^landed:\\s*(\\d)");
    private static final Pattern P_IMU_STATUS = Pattern.compile("(?m)^status:\\s*(\\S+)");
    private static final Pattern P_ARMED = Pattern.compile("\\barmed\\([^)]*\\):\\s*(\\d)");
    private static final Pattern P_RC_LINK = Pattern.compile("rcLinkUp\\([^)]*\\):\\s*(\\d)");
    private static final Pattern P_CRSF_LQ = Pattern.compile("crsfLinkQuality\\([^)]*\\):\\s*(-?\\d+)");
    private static final Pattern P_WEB_LINK = Pattern.compile("webLinkUp\\([^)]*\\):\\s*(\\d)");
    private static final Pattern P_MAV_LINK = Pattern.compile("mavLinkUp\\([^)]*\\):\\s*(\\d)");
    private static final Pattern P_LOOP_RATE = Pattern.compile("loopRate\\([^)]*\\):\\s*(\\d+)");
    private static final Pattern P_ARMING_DISABLED = Pattern.compile("armingDisabled\\([^)]*\\):[ \\t]*(.*?)[ \\t]*$", Pattern.MULTILINE);
    private static final Pattern P_PHASE = Pattern.compile("phase\\([^)]*\\):\\s*(\\S+)");
    private static final Pattern P_AIRBORNE = Pattern.compile("isAirborne\\([^)]*\\):\\s*(\\d)");
    private static final Pattern P_BATTERY = Pattern.compile("batteryVoltage\\([^)]*\\):\\s*([\\d.]+)");
    private static final Pattern P_ALTITUDE = Pattern.compile("altitude\\([^)]*\\):\\s*(nan|-?[\\d.]+)");
    private static final Pattern P_TOF = Pattern.compile("tofStatus\\([^)]*\\):\\s*(-?\\d+)");
    private static final Pattern P_BARO = Pattern.compile("baro\\([^)]*\\)=\\s*(\\d)");
    private static final Pattern P_RANGE = Pattern.compile("range\\([^)]*\\)=\\s*(\\d)");
    private static final Pattern P_HAS_ALT = Pattern.compile("hasAltitude\\([^)]*\\)=\\s*(\\d)");
    private static final Pattern P_MAG = Pattern.compile("mag\\([^)]*\\):[ \\t]*(.+?)[ \\t]*$", Pattern.MULTILINE);
    private static final Pattern P_PARAM_LINE = Pattern.compile(
            "(?m)^([A-Z][A-Z0-9_]+)\\s*=\\s*(nan|-?[\\d.]+(?:[eE][-+]?\\d+)?)\\s*$");
    private static final Pattern P_CHIP = Pattern.compile("Chip:\\s*(.+?)\\s*$", Pattern.MULTILINE);
    private static final Pattern P_TEMP = Pattern.compile(
            "Temperature(?:\\([^)]*\\))?:\\s*(-?[\\d.]+)");
    private static final Pattern P_HUMIDITY = Pattern.compile(
            "Humidity(?:\\([^)]*\\))?:\\s*([\\d.]+)");
    private static final Pattern P_PRESSURE = Pattern.compile(
            "Pressure(?:\\([^)]*\\))?:\\s*([\\d.]+)");
    private static final Pattern P_HEAP = Pattern.compile("Free heap:\\s*(\\d+)");
    private static final Pattern P_WIFI_MODE = Pattern.compile("(?m)^Mode:\\s*(.+?)\\s*$");
    private static final Pattern P_WIFI_SSID = Pattern.compile("SSID:\\s*(.+?)\\s*$", Pattern.MULTILINE);
    private static final Pattern P_WIFI_IP = Pattern.compile("(?m)^IP:\\s*([\\d.]+)\\s*$");
    private static final Pattern P_MAVCONN = Pattern.compile("MAVLink connected:\\s*(\\d)");

    private static double n(String s) {
        return s == null || s.equals("nan") ? Double.NaN : Double.parseDouble(s);
    }

    private static boolean flag(String s) {
        return "1".equals(s);
    }

    // ---- 解析器 ----

    public static Euler parseEuler(String text) {
        Matcher m = P_PS.matcher(text);
        return m.find() ? new Euler(n(m.group(1)), n(m.group(2)), n(m.group(3))) : null;
    }

    public static Quat parseQuat(String text) {
        Matcher m = P_PSQ.matcher(text);
        return m.find() ? new Quat(n(m.group(1)), n(m.group(2)), n(m.group(3)), n(m.group(4))) : null;
    }

    public static Motors parseMotors(String text) {
        Matcher m = P_MOT.matcher(text);
        return m.find() ? new Motors(n(m.group(1)), n(m.group(2)), n(m.group(3)), n(m.group(4))) : null;
    }

    public static RcData parseRc(String text) {
        Matcher ch = P_RC_CH.matcher(text);
        int max = 0;
        int[] tmp = new int[16];
        while (ch.find()) {
            int idx = Integer.parseInt(ch.group(1));
            if (idx >= 1 && idx <= 16) {
                tmp[idx - 1] = Integer.parseInt(ch.group(2));
                max = Math.max(max, idx);
            }
        }
        if (max == 0) {
            return null;
        }
        int[] channels = new int[max];
        System.arraycopy(tmp, 0, channels, 0, max);
        Matcher c = P_RC_CTRL.matcher(text);
        double roll = Double.NaN, pitch = Double.NaN, yaw = Double.NaN, thr = Double.NaN;
        String ctrlMode = null;
        if (c.find()) {
            roll = n(c.group(1));
            pitch = n(c.group(2));
            yaw = n(c.group(3));
            thr = n(c.group(4));
            ctrlMode = c.group(5);
        }
        String modeName = null;
        Matcher mMode = P_RC_MODE.matcher(text);
        if (mMode.find()) {
            modeName = mMode.group(1);
        } else {
            modeName = ctrlMode;
        }
        double time = Double.NaN;
        Matcher t = P_TIME_LINE.matcher(text);
        if (t.find()) {
            time = Double.parseDouble(t.group(1));
        }
        String src = null;
        Matcher s = P_CONTROL_SOURCE.matcher(text);
        if (s.find()) {
            src = s.group(1);
        }
        return new RcData(channels, roll, pitch, yaw, thr, modeName, src, time);
    }

    public static ImuData parseImu(String text) {
        Matcher g = P_IMU_GYRO.matcher(text);
        Matcher a = P_IMU_ACC.matcher(text);
        Matcher model = P_IMU_MODEL.matcher(text);
        if (!g.find() || !a.find()) {
            return null;
        }
        String st = null;
        Matcher stM = P_IMU_STATUS.matcher(text);
        if (stM.find()) {
            st = stM.group(1);
        }
        boolean landed = false;
        Matcher l = P_IMU_LANDED.matcher(text);
        if (l.find()) {
            landed = flag(l.group(1));
        }
        String modelName = model.find() ? model.group(1) : null;
        return new ImuData(st, modelName,
                new double[]{Double.parseDouble(g.group(1)), Double.parseDouble(g.group(2)), Double.parseDouble(g.group(3))},
                new double[]{Double.parseDouble(a.group(1)), Double.parseDouble(a.group(2)), Double.parseDouble(a.group(3))},
                landed);
    }

    public static StatusData parseStatus(String text) {
        Matcher armed = P_ARMED.matcher(text);
        if (!armed.find()) {
            return null; // 无 armed 行视为非 status 输出
        }
        String src = null;
        Matcher s = P_CONTROL_SOURCE.matcher(text);
        if (s.find()) {
            src = s.group(1);
        }
        String armDis = null;
        Matcher ad = P_ARMING_DISABLED.matcher(text);
        if (ad.find()) {
            String v = ad.group(1);
            if (!v.isEmpty() && !v.startsWith("无")) {
                armDis = v;
            }
        }
        String phase = null;
        Matcher p = P_PHASE.matcher(text);
        if (p.find()) {
            phase = p.group(1);
        }
        double batt = Double.NaN;
        Matcher b = P_BATTERY.matcher(text);
        if (b.find()) {
            batt = Double.parseDouble(b.group(1));
        }
        Double altitude = null;
        Matcher alt = P_ALTITUDE.matcher(text);
        if (alt.find() && !alt.group(1).equals("nan")) {
            altitude = Double.parseDouble(alt.group(1));
        }
        Integer tof = null;
        Matcher tf = P_TOF.matcher(text);
        if (tf.find()) {
            tof = Integer.parseInt(tf.group(1));
        }
        int loopRate = -1;
        Matcher lr = P_LOOP_RATE.matcher(text);
        if (lr.find()) {
            loopRate = Integer.parseInt(lr.group(1));
        }
        boolean landed = false, airborne = false;
        Matcher l = P_LANDED.matcher(text);
        if (l.find()) {
            landed = flag(l.group(1));
        }
        Matcher ab = P_AIRBORNE.matcher(text);
        if (ab.find()) {
            airborne = flag(ab.group(1));
        }
        Boolean baro = null, range = null, hasAlt = null;
        Matcher bb = P_BARO.matcher(text);
        if (bb.find()) {
            baro = flag(bb.group(1));
        }
        Matcher rr = P_RANGE.matcher(text);
        if (rr.find()) {
            range = flag(rr.group(1));
        }
        Matcher ha = P_HAS_ALT.matcher(text);
        if (ha.find()) {
            hasAlt = flag(ha.group(1));
        }
        boolean magOk = false;
        String magDetail = null;
        Matcher mg = P_MAG.matcher(text);
        if (mg.find()) {
            magDetail = mg.group(1);
            magOk = magDetail != null && !magDetail.startsWith("未检测到");
        }
        boolean rcUp = false, webUp = false, mavUp = false;
        Matcher rc = P_RC_LINK.matcher(text);
        if (rc.find()) {
            rcUp = flag(rc.group(1));
        }
        Matcher wb = P_WEB_LINK.matcher(text);
        if (wb.find()) {
            webUp = flag(wb.group(1));
        }
        Matcher mv = P_MAV_LINK.matcher(text);
        if (mv.find()) {
            mavUp = flag(mv.group(1));
        }
        int lq = -1;
        Matcher lqM = P_CRSF_LQ.matcher(text);
        if (lqM.find()) {
            lq = Integer.parseInt(lqM.group(1));
        }
        Double t = null, h = null, pressureVal = null;
        Matcher tm = P_TEMP.matcher(text);
        if (tm.find()) { t = Double.parseDouble(tm.group(1)); }
        Matcher hm = P_HUMIDITY.matcher(text);
        if (hm.find()) { h = Double.parseDouble(hm.group(1)); }
        Matcher pm = P_PRESSURE.matcher(text);
        if (pm.find()) { pressureVal = Double.parseDouble(pm.group(1)); }
        return new StatusData(flag(armed.group(1)), src, rcUp, webUp, mavUp, loopRate, armDis, phase,
                airborne, landed, batt, altitude, tof, baro != null && baro, range != null && range,
                hasAlt != null && hasAlt, magOk, lq, t, h, pressureVal);
    }

    /** 'p' 全量参数：NAME = value 行（nan → NaN） */
    public static LinkedHashMap<String, Double> parseParams(String text) {
        LinkedHashMap<String, Double> out = null;
        Matcher m = P_PARAM_LINE.matcher(text);
        while (m.find()) {
            if (out == null) {
                out = new LinkedHashMap<>();
            }
            out.put(m.group(1), n(m.group(2)));
        }
        return out;
    }

    public static SysData parseSys(String text) {
        Matcher chip = P_CHIP.matcher(text);
        if (!chip.find()) {
            return null;
        }
        Double temp = null;
        Matcher t = P_TEMP.matcher(text);
        if (t.find()) {
            temp = Double.parseDouble(t.group(1));
        }
        Long heap = null;
        Matcher h = P_HEAP.matcher(text);
        if (h.find()) {
            heap = Long.parseLong(h.group(1));
        }
        Integer lrVal = null;
        Matcher lrM = P_LOOP_RATE.matcher(text);
        if (lrM.find()) {
            lrVal = Integer.parseInt(lrM.group(1));
        }
        Double hum = null, pres = null;
        Matcher hu = P_HUMIDITY.matcher(text);
        if (hu.find()) { hum = Double.parseDouble(hu.group(1)); }
        Matcher pr = P_PRESSURE.matcher(text);
        if (pr.find()) { pres = Double.parseDouble(pr.group(1)); }
        return new SysData(chip.group(1), temp, heap, lrVal, hum, pres);
    }

    public static WifiData parseWifi(String text) {
        Matcher ssid = P_WIFI_SSID.matcher(text);
        if (!ssid.find()) {
            return null;
        }
        String mode = null;
        Matcher m = P_WIFI_MODE.matcher(text);
        if (m.find()) {
            mode = m.group(1);
        }
        String ipAddr = null;
        Matcher ipM = P_WIFI_IP.matcher(text);
        if (ipM.find()) {
            ipAddr = ipM.group(1);
        }
        boolean conn = false;
        Matcher c = P_MAVCONN.matcher(text);
        if (c.find()) {
            conn = flag(c.group(1));
        }
        return new WifiData(mode, ssid.group(1), ipAddr, conn);
    }
}
