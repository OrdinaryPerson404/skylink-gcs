package com.cherglow.gcs.model;

import java.util.Map;

/**
 * 遥测快照（不可变，合并自多次 CLI 命令响应）。
 * 缺失字段语义：数值=NaN、对象=null、布尔包装=null（未知）。
 */
public final class VehicleSnapshot {

    public final long timestampMs;

    // 姿态
    public final double rollDeg, pitchDeg, yawDeg;
    public final double qw, qx, qy, qz; // NaN 表示未取到

    // 电机（0~1 推力）
    public final double motorFR, motorFL, motorRR, motorRL;

    // 遥控
    public final int[] rcChannels;      // µs，可能为空
    public final double ctrlRoll, ctrlPitch, ctrlYaw, ctrlThrottle;
    public final String modeName;       // STAB/ACRO/ALTHOLD/POSHOLD
    public final String controlSource;  // 无/RC/WebRC/MAVLink

    // IMU
    public final String imuModel;
    public final double[] imuGyro;      // rad/s
    public final double[] imuAcc;       // m/s^2

    // 飞行状态（status）
    public final Boolean armed;
    public final Boolean landed;
    public final Boolean isAirborne;
    public final String phase;
    public final String armingDisabled; // null=允许解锁
    public final double batteryVoltage; // V
    public final Double altitude;       // m，null=无高度源
    public final Boolean rcLinkUp, webLinkUp, mavLinkUp;
    public final Integer crsfLinkQuality; // -1=无 RC 链路
    public final Boolean sensorsBaro, sensorsRange, hasAltitude, magOk;
    public final Integer tofStatus;
    public final int loopRate;          // -1 未知

    // 参数（p 全量）
    public final Map<String, Double> params;

    // 系统（sys）/ WiFi（wifi）/ 时间
    public final String chip;
    public final Double temperatureC;
    public final Long freeHeap;
    public final String wifiMode, wifiSsid, wifiIp;
    public final Boolean mavlinkConnected;
    public final double upTimeS;

    private VehicleSnapshot(Builder b) {
        this.timestampMs = b.timestampMs;
        this.rollDeg = b.rollDeg;
        this.pitchDeg = b.pitchDeg;
        this.yawDeg = b.yawDeg;
        this.qw = b.qw;
        this.qx = b.qx;
        this.qy = b.qy;
        this.qz = b.qz;
        this.motorFR = b.motorFR;
        this.motorFL = b.motorFL;
        this.motorRR = b.motorRR;
        this.motorRL = b.motorRL;
        this.rcChannels = b.rcChannels;
        this.ctrlRoll = b.ctrlRoll;
        this.ctrlPitch = b.ctrlPitch;
        this.ctrlYaw = b.ctrlYaw;
        this.ctrlThrottle = b.ctrlThrottle;
        this.modeName = b.modeName;
        this.controlSource = b.controlSource;
        this.imuModel = b.imuModel;
        this.imuGyro = b.imuGyro;
        this.imuAcc = b.imuAcc;
        this.armed = b.armed;
        this.landed = b.landed;
        this.isAirborne = b.isAirborne;
        this.phase = b.phase;
        this.armingDisabled = b.armingDisabled;
        this.batteryVoltage = b.batteryVoltage;
        this.altitude = b.altitude;
        this.rcLinkUp = b.rcLinkUp;
        this.webLinkUp = b.webLinkUp;
        this.mavLinkUp = b.mavLinkUp;
        this.crsfLinkQuality = b.crsfLinkQuality;
        this.sensorsBaro = b.sensorsBaro;
        this.sensorsRange = b.sensorsRange;
        this.hasAltitude = b.hasAltitude;
        this.magOk = b.magOk;
        this.tofStatus = b.tofStatus;
        this.loopRate = b.loopRate;
        this.params = b.params;
        this.chip = b.chip;
        this.temperatureC = b.temperatureC;
        this.freeHeap = b.freeHeap;
        this.wifiMode = b.wifiMode;
        this.wifiSsid = b.wifiSsid;
        this.wifiIp = b.wifiIp;
        this.mavlinkConnected = b.mavlinkConnected;
        this.upTimeS = b.upTimeS;
    }

    public static final class Builder {
        public long timestampMs;
        public double rollDeg = Double.NaN, pitchDeg = Double.NaN, yawDeg = Double.NaN;
        public double qw = Double.NaN, qx = Double.NaN, qy = Double.NaN, qz = Double.NaN;
        public double motorFR = Double.NaN, motorFL = Double.NaN, motorRR = Double.NaN, motorRL = Double.NaN;
        public int[] rcChannels;
        public double ctrlRoll = Double.NaN, ctrlPitch = Double.NaN, ctrlYaw = Double.NaN, ctrlThrottle = Double.NaN;
        public String modeName, controlSource;
        public String imuModel;
        public double[] imuGyro, imuAcc;
        public Boolean armed, landed, isAirborne;
        public String phase, armingDisabled;
        public double batteryVoltage = Double.NaN;
        public Double altitude;
        public Boolean rcLinkUp, webLinkUp, mavLinkUp;
        public Integer crsfLinkQuality;
        public Boolean sensorsBaro, sensorsRange, hasAltitude, magOk;
        public Integer tofStatus;
        public int loopRate = -1;
        public Map<String, Double> params;
        public String chip;
        public Double temperatureC;
        public Long freeHeap;
        public String wifiMode, wifiSsid, wifiIp;
        public Boolean mavlinkConnected;
        public double upTimeS = Double.NaN;

        public VehicleSnapshot build() {
            return new VehicleSnapshot(this);
        }
    }
}
