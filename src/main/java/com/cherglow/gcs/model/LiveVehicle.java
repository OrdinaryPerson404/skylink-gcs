package com.cherglow.gcs.model;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * FX 线程遥测镜像（单例）：页面直接绑定这些属性。
 * 通过 {@link #updateFrom(VehicleSnapshot)} 刷入，内部保证在 FX 线程执行。
 */
public final class LiveVehicle {

    private static final LiveVehicle INSTANCE = new LiveVehicle();

    public static LiveVehicle get() {
        return INSTANCE;
    }

    private LiveVehicle() {
    }

    public final BooleanProperty connected = new SimpleBooleanProperty(this, "connected", false);
    public final BooleanProperty armed = new SimpleBooleanProperty(this, "armed", false);
    public final StringProperty phase = new SimpleStringProperty(this, "phase", "");
    public final StringProperty modeName = new SimpleStringProperty(this, "modeName", "");
    public final StringProperty controlSource = new SimpleStringProperty(this, "controlSource", "");
    /** 非空表示禁止解锁原因（如 电量低）；空串=允许 */
    public final StringProperty armingDisabled = new SimpleStringProperty(this, "armingDisabled", "");
    public final DoubleProperty batteryVoltage = new SimpleDoubleProperty(this, "batteryVoltage", Double.NaN);
    public final DoubleProperty altitude = new SimpleDoubleProperty(this, "altitude", Double.NaN);
    public final DoubleProperty rollDeg = new SimpleDoubleProperty(this, "rollDeg", 0);
    public final DoubleProperty pitchDeg = new SimpleDoubleProperty(this, "pitchDeg", 0);
    public final DoubleProperty yawDeg = new SimpleDoubleProperty(this, "yawDeg", 0);
    public final IntegerProperty loopRate = new SimpleIntegerProperty(this, "loopRate", -1);
    public final ObjectProperty<float[]> motors = new SimpleObjectProperty<>(this, "motors");
    public final ObjectProperty<int[]> rcChannels = new SimpleObjectProperty<>(this, "rcChannels");
    public final LongProperty lastUpdateMs = new SimpleLongProperty(this, "lastUpdateMs", 0);

    // ---- S6 仪表盘扩展 ----
    public final DoubleProperty qw = new SimpleDoubleProperty(this, "qw", Double.NaN);
    public final DoubleProperty qx = new SimpleDoubleProperty(this, "qx", Double.NaN);
    public final DoubleProperty qy = new SimpleDoubleProperty(this, "qy", Double.NaN);
    public final DoubleProperty qz = new SimpleDoubleProperty(this, "qz", Double.NaN);
    public final StringProperty imuModel = new SimpleStringProperty(this, "imuModel", "");
    public final DoubleProperty accX = new SimpleDoubleProperty(this, "accX", Double.NaN);
    public final DoubleProperty accY = new SimpleDoubleProperty(this, "accY", Double.NaN);
    public final DoubleProperty accZ = new SimpleDoubleProperty(this, "accZ", Double.NaN);
    public final DoubleProperty gyroX = new SimpleDoubleProperty(this, "gyroX", Double.NaN);
    public final DoubleProperty gyroY = new SimpleDoubleProperty(this, "gyroY", Double.NaN);
    public final DoubleProperty gyroZ = new SimpleDoubleProperty(this, "gyroZ", Double.NaN);
    /** 全量参数（p 拉取累积，键=参数名；仅内容变化时才触发属性） */
    public final javafx.beans.property.ObjectProperty<java.util.Map<String, Double>> params =
            new javafx.beans.property.SimpleObjectProperty<>(this, "params", null);
    private final java.util.Map<String, Double> paramsAcc = new java.util.LinkedHashMap<>();
    private int paramsHash;
    public final javafx.beans.property.StringProperty chip =
            new javafx.beans.property.SimpleStringProperty(this, "chip", "");
    public final javafx.beans.property.DoubleProperty temperatureC =
            new javafx.beans.property.SimpleDoubleProperty(this, "temperatureC", Double.NaN);
    /** 传感器在线位（status 输出；false=未配备/未检测到） */
    public final BooleanProperty baroOk = new SimpleBooleanProperty(this, "baroOk", false);
    public final BooleanProperty rangeOk = new SimpleBooleanProperty(this, "rangeOk", false);
    public final BooleanProperty hasAltitude = new SimpleBooleanProperty(this, "hasAltitude", false);
    public final BooleanProperty magOk = new SimpleBooleanProperty(this, "magOk", false);
    /** 链路在线位（status 输出） */
    public final BooleanProperty rcLinkUp = new SimpleBooleanProperty(this, "rcLinkUp", false);
    public final BooleanProperty webLinkUp = new SimpleBooleanProperty(this, "webLinkUp", false);
    public final BooleanProperty mavLinkUp = new SimpleBooleanProperty(this, "mavLinkUp", false);
    /** CRSF 上行链路质量（-1=无 RC 链路） */
    public final IntegerProperty linkQuality = new SimpleIntegerProperty(this, "linkQuality", -1);
    /** 电池电量估算百分比（1S 锂电 3.3~4.2V 线性映射，仅显示用） */
    public final DoubleProperty batteryPct = new SimpleDoubleProperty(this, "batteryPct", Double.NaN);

    public void updateFrom(VehicleSnapshot s) {
        Runnable apply = () -> {
            if (s.armed != null) {
                armed.set(s.armed);
            }
            if (s.phase != null) {
                phase.set(s.phase);
            }
            if (s.modeName != null) {
                modeName.set(s.modeName);
            }
            if (s.controlSource != null) {
                controlSource.set(s.controlSource);
            }
            armingDisabled.set(s.armingDisabled == null ? "" : s.armingDisabled);
            if (!Double.isNaN(s.batteryVoltage)) {
                batteryVoltage.set(s.batteryVoltage);
            }
            if (s.altitude != null) {
                altitude.set(s.altitude);
            }
            if (!Double.isNaN(s.rollDeg)) {
                rollDeg.set(s.rollDeg);
            }
            if (!Double.isNaN(s.pitchDeg)) {
                pitchDeg.set(s.pitchDeg);
            }
            if (!Double.isNaN(s.yawDeg)) {
                yawDeg.set(s.yawDeg);
            }
            if (s.loopRate >= 0) {
                loopRate.set(s.loopRate);
            }
            if (s.motorFR == s.motorFR) { // 非 NaN
                motors.set(new float[]{(float) s.motorFR, (float) s.motorFL,
                        (float) s.motorRR, (float) s.motorRL});
            }
            if (s.rcChannels != null) {
                rcChannels.set(s.rcChannels);
            }
            // S6 扩展：四元数 / IMU / 传感器在线位
            if (s.qw == s.qw) {
                qw.set(s.qw);
                qx.set(s.qx);
                qy.set(s.qy);
                qz.set(s.qz);
            }
            if (s.imuModel != null) {
                imuModel.set(s.imuModel);
            }
            if (s.imuAcc != null && s.imuAcc.length == 3) {
                accX.set(s.imuAcc[0]);
                accY.set(s.imuAcc[1]);
                accZ.set(s.imuAcc[2]);
            }
            if (s.imuGyro != null && s.imuGyro.length == 3) {
                gyroX.set(s.imuGyro[0]);
                gyroY.set(s.imuGyro[1]);
                gyroZ.set(s.imuGyro[2]);
            }
            if (s.rcLinkUp != null) {
                rcLinkUp.set(s.rcLinkUp);
            }
            if (s.webLinkUp != null) {
                webLinkUp.set(s.webLinkUp);
            }
            if (s.mavLinkUp != null) {
                mavLinkUp.set(s.mavLinkUp);
            }
            if (s.crsfLinkQuality != null) {
                linkQuality.set(s.crsfLinkQuality);
            }
            if (s.batteryVoltage == s.batteryVoltage) {
                batteryPct.set(Math.max(0, Math.min(100, (s.batteryVoltage - 3.3) / 0.9 * 100)));
            }
            if (s.chip != null) {
                chip.set(s.chip);
            }
            if (s.temperatureC != null && s.temperatureC == s.temperatureC) {
                temperatureC.set(s.temperatureC);
            }
            if (s.params != null && s.params.hashCode() != paramsHash) {
                paramsHash = s.params.hashCode();
                paramsAcc.putAll(s.params);
                params.set(new java.util.LinkedHashMap<>(paramsAcc));
            }
            if (s.sensorsBaro != null) {
                baroOk.set(s.sensorsBaro);
            }
            if (s.sensorsRange != null) {
                rangeOk.set(s.sensorsRange);
            }
            if (s.hasAltitude != null) {
                hasAltitude.set(s.hasAltitude);
            }
            if (s.magOk != null) {
                magOk.set(s.magOk);
            }
            lastUpdateMs.set(s.timestampMs);
        };
        if (Platform.isFxApplicationThread()) {
            apply.run();
        } else {
            Platform.runLater(apply);
        }
    }
}
