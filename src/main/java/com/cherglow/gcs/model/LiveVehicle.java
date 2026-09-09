package com.cherglow.gcs.model;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.util.Duration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FX 线程遥测镜像（单例）：页面直接绑定这些只读属性。
 * 遥测经 {@link #updateFrom(VehicleSnapshot)} 刷入，GNSS 经 {@link #updateGps} 写入，
 * 连接状态经 {@link #setConnected} 写入；内部保证在 FX 线程执行。
 * 所有属性对外只读（ReadOnlyXxxProperty），写入通道全部收口在本类方法内。
 */
public final class LiveVehicle {

    private static final LiveVehicle INSTANCE = new LiveVehicle();

    public static LiveVehicle get() {
        return INSTANCE;
    }

    private LiveVehicle() {
    }

    // ---- 连接 / 飞行状态 ----
    private final ReadOnlyBooleanWrapper connectedWrapper = new ReadOnlyBooleanWrapper(this, "connected", false);
    public final ReadOnlyBooleanProperty connected = connectedWrapper.getReadOnlyProperty();
    private final ReadOnlyBooleanWrapper armedWrapper = new ReadOnlyBooleanWrapper(this, "armed", false);
    public final ReadOnlyBooleanProperty armed = armedWrapper.getReadOnlyProperty();
    private final ReadOnlyStringWrapper phaseWrapper = new ReadOnlyStringWrapper(this, "phase", "");
    public final ReadOnlyStringProperty phase = phaseWrapper.getReadOnlyProperty();
    private final ReadOnlyStringWrapper modeNameWrapper = new ReadOnlyStringWrapper(this, "modeName", "");
    public final ReadOnlyStringProperty modeName = modeNameWrapper.getReadOnlyProperty();
    private final ReadOnlyStringWrapper controlSourceWrapper = new ReadOnlyStringWrapper(this, "controlSource", "");
    public final ReadOnlyStringProperty controlSource = controlSourceWrapper.getReadOnlyProperty();
    /** 非空表示禁止解锁原因（如 电量低）；空串=允许 */
    private final ReadOnlyStringWrapper armingDisabledWrapper = new ReadOnlyStringWrapper(this, "armingDisabled", "");
    public final ReadOnlyStringProperty armingDisabled = armingDisabledWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper batteryVoltageWrapper = new ReadOnlyDoubleWrapper(this, "batteryVoltage", Double.NaN);
    public final ReadOnlyDoubleProperty batteryVoltage = batteryVoltageWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper altitudeWrapper = new ReadOnlyDoubleWrapper(this, "altitude", Double.NaN);
    public final ReadOnlyDoubleProperty altitude = altitudeWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper rollDegWrapper = new ReadOnlyDoubleWrapper(this, "rollDeg", 0);
    public final ReadOnlyDoubleProperty rollDeg = rollDegWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper pitchDegWrapper = new ReadOnlyDoubleWrapper(this, "pitchDeg", 0);
    public final ReadOnlyDoubleProperty pitchDeg = pitchDegWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper yawDegWrapper = new ReadOnlyDoubleWrapper(this, "yawDeg", 0);
    public final ReadOnlyDoubleProperty yawDeg = yawDegWrapper.getReadOnlyProperty();
    private final ReadOnlyIntegerWrapper loopRateWrapper = new ReadOnlyIntegerWrapper(this, "loopRate", -1);
    public final ReadOnlyIntegerProperty loopRate = loopRateWrapper.getReadOnlyProperty();
    private final ReadOnlyObjectWrapper<float[]> motorsWrapper = new ReadOnlyObjectWrapper<>(this, "motors");
    public final ReadOnlyObjectProperty<float[]> motors = motorsWrapper.getReadOnlyProperty();
    private final ReadOnlyObjectWrapper<int[]> rcChannelsWrapper = new ReadOnlyObjectWrapper<>(this, "rcChannels");
    public final ReadOnlyObjectProperty<int[]> rcChannels = rcChannelsWrapper.getReadOnlyProperty();
    private final ReadOnlyLongWrapper lastUpdateMsWrapper = new ReadOnlyLongWrapper(this, "lastUpdateMs", 0);
    public final ReadOnlyLongProperty lastUpdateMs = lastUpdateMsWrapper.getReadOnlyProperty();

    // ---- S6 仪表盘扩展 ----
    private final ReadOnlyDoubleWrapper qwWrapper = new ReadOnlyDoubleWrapper(this, "qw", Double.NaN);
    public final ReadOnlyDoubleProperty qw = qwWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper qxWrapper = new ReadOnlyDoubleWrapper(this, "qx", Double.NaN);
    public final ReadOnlyDoubleProperty qx = qxWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper qyWrapper = new ReadOnlyDoubleWrapper(this, "qy", Double.NaN);
    public final ReadOnlyDoubleProperty qy = qyWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper qzWrapper = new ReadOnlyDoubleWrapper(this, "qz", Double.NaN);
    public final ReadOnlyDoubleProperty qz = qzWrapper.getReadOnlyProperty();
    private final ReadOnlyStringWrapper imuModelWrapper = new ReadOnlyStringWrapper(this, "imuModel", "");
    public final ReadOnlyStringProperty imuModel = imuModelWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper accXWrapper = new ReadOnlyDoubleWrapper(this, "accX", Double.NaN);
    public final ReadOnlyDoubleProperty accX = accXWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper accYWrapper = new ReadOnlyDoubleWrapper(this, "accY", Double.NaN);
    public final ReadOnlyDoubleProperty accY = accYWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper accZWrapper = new ReadOnlyDoubleWrapper(this, "accZ", Double.NaN);
    public final ReadOnlyDoubleProperty accZ = accZWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper gyroXWrapper = new ReadOnlyDoubleWrapper(this, "gyroX", Double.NaN);
    public final ReadOnlyDoubleProperty gyroX = gyroXWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper gyroYWrapper = new ReadOnlyDoubleWrapper(this, "gyroY", Double.NaN);
    public final ReadOnlyDoubleProperty gyroY = gyroYWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper gyroZWrapper = new ReadOnlyDoubleWrapper(this, "gyroZ", Double.NaN);
    public final ReadOnlyDoubleProperty gyroZ = gyroZWrapper.getReadOnlyProperty();
    /** 全量参数（p 拉取累积，键=参数名；仅内容变化时才触发属性；发布值为不可变 Map） */
    private final ReadOnlyObjectWrapper<Map<String, Double>> paramsWrapper =
            new ReadOnlyObjectWrapper<>(this, "params", null);
    public final ReadOnlyObjectProperty<Map<String, Double>> params = paramsWrapper.getReadOnlyProperty();
    private final java.util.Map<String, Double> paramsAcc = new LinkedHashMap<>();
    private int paramsHash;
    private final ReadOnlyStringWrapper chipWrapper = new ReadOnlyStringWrapper(this, "chip", "");
    public final ReadOnlyStringProperty chip = chipWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper temperatureCWrapper = new ReadOnlyDoubleWrapper(this, "temperatureC", Double.NaN);
    public final ReadOnlyDoubleProperty temperatureC = temperatureCWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper humidityPctWrapper = new ReadOnlyDoubleWrapper(this, "humidityPct", Double.NaN);
    public final ReadOnlyDoubleProperty humidityPct = humidityPctWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper pressureHpaWrapper = new ReadOnlyDoubleWrapper(this, "pressureHpa", Double.NaN);
    public final ReadOnlyDoubleProperty pressureHpa = pressureHpaWrapper.getReadOnlyProperty();
    /** 传感器在线位（status 输出；false=未配备/未检测到） */
    private final ReadOnlyBooleanWrapper baroOkWrapper = new ReadOnlyBooleanWrapper(this, "baroOk", false);
    public final ReadOnlyBooleanProperty baroOk = baroOkWrapper.getReadOnlyProperty();
    private final ReadOnlyBooleanWrapper rangeOkWrapper = new ReadOnlyBooleanWrapper(this, "rangeOk", false);
    public final ReadOnlyBooleanProperty rangeOk = rangeOkWrapper.getReadOnlyProperty();
    private final ReadOnlyBooleanWrapper hasAltitudeWrapper = new ReadOnlyBooleanWrapper(this, "hasAltitude", false);
    public final ReadOnlyBooleanProperty hasAltitude = hasAltitudeWrapper.getReadOnlyProperty();
    private final ReadOnlyBooleanWrapper magOkWrapper = new ReadOnlyBooleanWrapper(this, "magOk", false);
    public final ReadOnlyBooleanProperty magOk = magOkWrapper.getReadOnlyProperty();
    /** 链路在线位（status 输出） */
    private final ReadOnlyBooleanWrapper rcLinkUpWrapper = new ReadOnlyBooleanWrapper(this, "rcLinkUp", false);
    public final ReadOnlyBooleanProperty rcLinkUp = rcLinkUpWrapper.getReadOnlyProperty();
    private final ReadOnlyBooleanWrapper webLinkUpWrapper = new ReadOnlyBooleanWrapper(this, "webLinkUp", false);
    public final ReadOnlyBooleanProperty webLinkUp = webLinkUpWrapper.getReadOnlyProperty();
    private final ReadOnlyBooleanWrapper mavLinkUpWrapper = new ReadOnlyBooleanWrapper(this, "mavLinkUp", false);
    public final ReadOnlyBooleanProperty mavLinkUp = mavLinkUpWrapper.getReadOnlyProperty();
    /** CRSF 上行链路质量（-1=无 RC 链路） */
    private final ReadOnlyIntegerWrapper linkQualityWrapper = new ReadOnlyIntegerWrapper(this, "linkQuality", -1);
    public final ReadOnlyIntegerProperty linkQuality = linkQualityWrapper.getReadOnlyProperty();
    /** 电池电量估算百分比（1S 锂电 3.3~4.2V 线性映射，仅显示用） */
    private final ReadOnlyDoubleWrapper batteryPctWrapper = new ReadOnlyDoubleWrapper(this, "batteryPct", Double.NaN);
    public final ReadOnlyDoubleProperty batteryPct = batteryPctWrapper.getReadOnlyProperty();

    /** 外部 GNSS 位置（S15，GPSLogger 推流；NaN=无源）。gpsFix=true 表示收到过有效定位（不复位）。 */
    private final ReadOnlyDoubleWrapper latDegWrapper = new ReadOnlyDoubleWrapper(this, "latDeg", Double.NaN);
    public final ReadOnlyDoubleProperty latDeg = latDegWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper lonDegWrapper = new ReadOnlyDoubleWrapper(this, "lonDeg", Double.NaN);
    public final ReadOnlyDoubleProperty lonDeg = lonDegWrapper.getReadOnlyProperty();
    private final ReadOnlyDoubleWrapper spdMpsWrapper = new ReadOnlyDoubleWrapper(this, "spdMps", Double.NaN);
    public final ReadOnlyDoubleProperty spdMps = spdMpsWrapper.getReadOnlyProperty();
    private final ReadOnlyBooleanWrapper gpsFixWrapper = new ReadOnlyBooleanWrapper(this, "gpsFix", false);
    public final ReadOnlyBooleanProperty gpsFix = gpsFixWrapper.getReadOnlyProperty();

    /** S17：外部 GNSS 在线状态 — 5 秒内有有效定位为 true，超时自动复位为 false。
     *  与 gpsFix 语义不同：gpsFix 表示"曾有定位"不复位，gnssOnline 表示"当前在线"。 */
    private final ReadOnlyBooleanWrapper gnssOnlineWrapper = new ReadOnlyBooleanWrapper(this, "gnssOnline", false);
    public final ReadOnlyBooleanProperty gnssOnline = gnssOnlineWrapper.getReadOnlyProperty();
    private long gnssLastFixMs;
    private static final long GNSS_TIMEOUT_MS = 5000;
    private Timeline gnssWatchdog;

    private void ensureGnssWatchdog() {
        if (gnssWatchdog != null) {
            return;
        }
        gnssWatchdog = new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            if (gnssOnline.get() && System.currentTimeMillis() - gnssLastFixMs > GNSS_TIMEOUT_MS) {
                gnssOnlineWrapper.set(false);
            }
        }));
        gnssWatchdog.setCycleCount(Animation.INDEFINITE);
        gnssWatchdog.play();
    }

    /** 连接状态写入（由 ConnectionService 在 FX 线程调用） */
    public void setConnected(boolean value) {
        connectedWrapper.set(value);
    }

    /** 仅供快照工具（tools.AdiSnapshot / tools.View3DSnapshot）注入固定姿态渲染；yawDeg 传 null 表示保持当前值 */
    public void injectPoseForTool(double rollDeg, double pitchDeg, Double yawDeg) {
        dispatch(() -> {
            rollDegWrapper.set(rollDeg);
            pitchDegWrapper.set(pitchDeg);
            if (yawDeg != null) {
                yawDegWrapper.set(yawDeg);
            }
        });
    }

    /** S15 多源融合：写入外部 GNSS 位置。lat/lon 之一为 NaN 时视为无源，不覆盖已锁定的有源值。 */
    public void updateGps(double lat, double lon, double spd) {
        Runnable apply = () -> {
            boolean hasFix = !Double.isNaN(lat) && !Double.isNaN(lon);
            if (!hasFix) {
                return; // 无源不清空有源
            }
            latDegWrapper.set(lat);
            lonDegWrapper.set(lon);
            if (!Double.isNaN(spd)) {
                spdMpsWrapper.set(spd);
            }
            gpsFixWrapper.set(true);
            gnssLastFixMs = System.currentTimeMillis();
            if (!gnssOnline.get()) {
                gnssOnlineWrapper.set(true);
            }
            ensureGnssWatchdog();
        };
        dispatch(apply);
    }

    public void updateFrom(VehicleSnapshot s) {
        Runnable apply = () -> {
            if (s.armed != null) {
                armedWrapper.set(s.armed);
            }
            if (s.phase != null) {
                phaseWrapper.set(s.phase);
            }
            if (s.modeName != null) {
                modeNameWrapper.set(s.modeName);
            }
            if (s.controlSource != null) {
                controlSourceWrapper.set(s.controlSource);
            }
            armingDisabledWrapper.set(s.armingDisabled == null ? "" : s.armingDisabled);
            if (!Double.isNaN(s.batteryVoltage)) {
                batteryVoltageWrapper.set(s.batteryVoltage);
            }
            if (s.altitude != null) {
                altitudeWrapper.set(s.altitude);
            }
            if (!Double.isNaN(s.rollDeg)) {
                rollDegWrapper.set(s.rollDeg);
            }
            if (!Double.isNaN(s.pitchDeg)) {
                pitchDegWrapper.set(s.pitchDeg);
            }
            if (!Double.isNaN(s.yawDeg)) {
                yawDegWrapper.set(s.yawDeg);
            }
            if (s.loopRate >= 0) {
                loopRateWrapper.set(s.loopRate);
            }
            if (s.motorFR == s.motorFR) { // 非 NaN
                motorsWrapper.set(new float[]{(float) s.motorFR, (float) s.motorFL,
                        (float) s.motorRR, (float) s.motorRL});
            }
            if (s.rcChannels != null) {
                rcChannelsWrapper.set(s.rcChannels);
            }
            // S6 扩展：四元数 / IMU / 传感器在线位
            if (s.qw == s.qw) {
                qwWrapper.set(s.qw);
                qxWrapper.set(s.qx);
                qyWrapper.set(s.qy);
                qzWrapper.set(s.qz);
            }
            if (s.imuModel != null) {
                imuModelWrapper.set(s.imuModel);
            }
            if (s.imuAcc != null && s.imuAcc.length == 3) {
                accXWrapper.set(s.imuAcc[0]);
                accYWrapper.set(s.imuAcc[1]);
                accZWrapper.set(s.imuAcc[2]);
            }
            if (s.imuGyro != null && s.imuGyro.length == 3) {
                gyroXWrapper.set(s.imuGyro[0]);
                gyroYWrapper.set(s.imuGyro[1]);
                gyroZWrapper.set(s.imuGyro[2]);
            }
            if (s.rcLinkUp != null) {
                rcLinkUpWrapper.set(s.rcLinkUp);
            }
            if (s.webLinkUp != null) {
                webLinkUpWrapper.set(s.webLinkUp);
            }
            if (s.mavLinkUp != null) {
                mavLinkUpWrapper.set(s.mavLinkUp);
            }
            if (s.crsfLinkQuality != null) {
                linkQualityWrapper.set(s.crsfLinkQuality);
            }
            if (s.batteryVoltage == s.batteryVoltage) {
                batteryPctWrapper.set(Math.max(0, Math.min(100, (s.batteryVoltage - 3.3) / 0.9 * 100)));
            }
            // S15：快照带 GNSS 位置时融合（NaN 不覆盖有源）
            if (s.latDeg == s.latDeg && s.lonDeg == s.lonDeg) {
                latDegWrapper.set(s.latDeg);
                lonDegWrapper.set(s.lonDeg);
                if (s.spdMps == s.spdMps) {
                    spdMpsWrapper.set(s.spdMps);
                }
                gpsFixWrapper.set(true);
                gnssLastFixMs = System.currentTimeMillis();
                if (!gnssOnline.get()) {
                    gnssOnlineWrapper.set(true);
                }
                ensureGnssWatchdog();
            }
            if (s.chip != null) {
                chipWrapper.set(s.chip);
            }
            if (s.temperatureC != null && s.temperatureC == s.temperatureC) {
                temperatureCWrapper.set(s.temperatureC);
            }
            if (s.humidityPct != null) {
                humidityPctWrapper.set(s.humidityPct);
            }
            if (s.pressureHpa != null) {
                pressureHpaWrapper.set(s.pressureHpa);
            }
            if (s.params != null && s.params.hashCode() != paramsHash) {
                paramsHash = s.params.hashCode();
                paramsAcc.putAll(s.params);
                paramsWrapper.set(Collections.unmodifiableMap(new LinkedHashMap<>(paramsAcc)));
            }
            if (s.sensorsBaro != null) {
                baroOkWrapper.set(s.sensorsBaro);
            }
            if (s.sensorsRange != null) {
                rangeOkWrapper.set(s.sensorsRange);
            }
            if (s.hasAltitude != null) {
                hasAltitudeWrapper.set(s.hasAltitude);
            }
            if (s.magOk != null) {
                magOkWrapper.set(s.magOk);
            }
            lastUpdateMsWrapper.set(s.timestampMs);
        };
        dispatch(apply);
    }

    private void dispatch(Runnable apply) {
        if (Platform.isFxApplicationThread()) {
            apply.run();
        } else {
            Platform.runLater(apply);
        }
    }
}
