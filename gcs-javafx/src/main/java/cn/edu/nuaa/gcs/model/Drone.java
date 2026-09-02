package cn.edu.nuaa.gcs.model;

import cn.edu.nuaa.gcs.comm.MavlinkParser;
import javafx.beans.property.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 无人机数据模型（JavaFX 可观察属性）。
 *
 * 设计要点：
 * - 与 MavlinkParser.Telemetry 字段对齐，提供 updateFromTelemetry(t) 单点翻译，
 *   避免上层（MainController）承担 30+ 字段的逐条映射而膨胀。
 * - 引入一组 *Available 可用性标识（positionAvailable / batteryAvailable / ...），
 *   与 Telemetry 的 has* 标志对应；UI 层据此"诚实标注"哪些数据真实可用、
 *   哪些因飞控/固件不上报而不可用（例如 CF-Drone 不上报电池与 GPS）。
 * - 保留旧 updateTelemetry(14 个标量) 方法以兼容旧调用路径，不破坏现有功能。
 */
public class Drone {
    private final StringProperty model = new SimpleStringProperty("DJI-M300");
    private final DoubleProperty maxRange = new SimpleDoubleProperty(5000);
    private final ListProperty<String> sensorTypes = new SimpleListProperty<>(javafx.collections.FXCollections.observableArrayList());

    // 位置 / 速度
    private final DoubleProperty lat = new SimpleDoubleProperty(32.0612);
    private final DoubleProperty lon = new SimpleDoubleProperty(118.793);
    private final DoubleProperty alt = new SimpleDoubleProperty(0);
    private final DoubleProperty speed = new SimpleDoubleProperty(0);
    private final DoubleProperty heading = new SimpleDoubleProperty(0);
    private final IntegerProperty satellites = new SimpleIntegerProperty(0);
    private final DoubleProperty hdop = new SimpleDoubleProperty(0);

    // 电池
    private final DoubleProperty batteryPct = new SimpleDoubleProperty(100);
    private final DoubleProperty voltage = new SimpleDoubleProperty(12.6);
    private final DoubleProperty signalPct = new SimpleDoubleProperty(0);

    // 姿态（弧度）+ 角速度（rad/s）
    private final DoubleProperty roll = new SimpleDoubleProperty(0);
    private final DoubleProperty pitch = new SimpleDoubleProperty(0);
    private final DoubleProperty yaw = new SimpleDoubleProperty(0);
    private final DoubleProperty rollSpeed = new SimpleDoubleProperty(0);
    private final DoubleProperty pitchSpeed = new SimpleDoubleProperty(0);
    private final DoubleProperty yawSpeed = new SimpleDoubleProperty(0);

    // IMU（来自 SCALED_IMU）
    private final DoubleProperty accX = new SimpleDoubleProperty(0);
    private final DoubleProperty accY = new SimpleDoubleProperty(0);
    private final DoubleProperty accZ = new SimpleDoubleProperty(0);
    private final DoubleProperty gyroX = new SimpleDoubleProperty(0);
    private final DoubleProperty gyroY = new SimpleDoubleProperty(0);
    private final DoubleProperty gyroZ = new SimpleDoubleProperty(0);
    private final DoubleProperty imuTemp = new SimpleDoubleProperty(0);

    // RC 通道（8 通道）+ RSSI
    private final int[] rcChannels = new int[8];
    private final IntegerProperty rssi = new SimpleIntegerProperty(0);

    // 电机输出（前 4 个为四电机推力）
    private final double[] motorOutputs = new double[8];

    // 扩展系统状态
    private final IntegerProperty landedState = new SimpleIntegerProperty(0); // 1=地面 2=空中

    // 心跳 / 飞行模式（CF-Drone: 0=RAW 1=ACRO 2=STAB 3=ALTHOLD 4=AUTO）
    private final IntegerProperty customMode = new SimpleIntegerProperty(0);
    private final IntegerProperty systemStatus = new SimpleIntegerProperty(0); // MAV_STATE

    // 命令应答
    private final IntegerProperty ackCommand = new SimpleIntegerProperty(0);
    private final IntegerProperty ackResult = new SimpleIntegerProperty(0); // 0=ACCEPTED ... 5=IN_PROGRESS

    // 任务数量
    private final IntegerProperty missionCount = new SimpleIntegerProperty(0);

    // Shell 输出
    private final StringProperty shellText = new SimpleStringProperty("");

    // 状态总览
    private final StringProperty status = new SimpleStringProperty("DISCONNECTED");
    private final BooleanProperty armed = new SimpleBooleanProperty(false);

    // ===== 数据可用性标识（与 Telemetry.has* 对应） =====
    private final BooleanProperty positionAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty batteryAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty heartbeatAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty attitudeAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty imuAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty rcAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty motorsAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty landedStateAvailable = new SimpleBooleanProperty(false);

    public Drone() {
        sensorTypes.addAll("IMU", "COMPASS", "BARO", "GPS");
    }

    /**
     * 单点翻译：把 MavlinkParser 解析出的 Telemetry 写入 Drone 可观察属性。
     * 仅在对应 has* 标志为 true 时更新对应字段并置可用性为 true，
     * 避免飞控不上报的数据被默认值 0 误导 UI。
     */
    public void updateFromTelemetry(MavlinkParser.Telemetry t) {
        if (t == null || !t.valid) return;

        if (t.hasHeartbeat) {
            customMode.set(t.customMode);
            systemStatus.set(t.systemStatus);
            armed.set(t.armed);
            heartbeatAvailable.set(true);
        }

        if (t.hasAttitude) {
            roll.set(t.roll);
            pitch.set(t.pitch);
            yaw.set(t.yaw);
            rollSpeed.set(t.rollSpeed);
            pitchSpeed.set(t.pitchSpeed);
            yawSpeed.set(t.yawSpeed);
            attitudeAvailable.set(true);
        }

        if (t.hasImu) {
            accX.set(t.accX);
            accY.set(t.accY);
            accZ.set(t.accZ);
            gyroX.set(t.gyroX);
            gyroY.set(t.gyroY);
            gyroZ.set(t.gyroZ);
            imuTemp.set(t.imuTemp);
            imuAvailable.set(true);
        }

        if (t.hasPosition) {
            lat.set(t.lat);
            lon.set(t.lon);
            alt.set(t.alt);
            speed.set(t.speed);
            heading.set(t.heading);
            positionAvailable.set(true);
        }

        if (t.hasBattery) {
            voltage.set(t.voltage);
            batteryPct.set(t.batteryPct);
            batteryAvailable.set(true);
        }

        if (t.hasRc) {
            System.arraycopy(t.rcChannels, 0, rcChannels, 0, Math.min(rcChannels.length, t.rcChannels.length));
            rssi.set(t.rssi);
            rcAvailable.set(true);
        }

        if (t.hasMotors) {
            System.arraycopy(t.motorOutputs, 0, motorOutputs, 0, Math.min(motorOutputs.length, t.motorOutputs.length));
            motorsAvailable.set(true);
        }

        if (t.hasLandedState) {
            landedState.set(t.landedState);
            landedStateAvailable.set(true);
        }

        if (t.hasAck) {
            ackCommand.set(t.ackCommand);
            ackResult.set(t.ackResult);
        }

        if (t.hasMissionCount) {
            missionCount.set(t.missionCount);
        }

        if (t.hasShell && t.shellText != null) {
            // 追加 shell 输出，保留历史
            String prev = shellText.get();
            String next = prev + t.shellText;
            // 限制长度，避免无限增长
            if (next.length() > 8192) next = next.substring(next.length() - 8192);
            shellText.set(next);
        }

        status.set("CONNECTED");
    }

    /** 标记断开：所有可用性标识清零，状态置 DISCONNECTED。 */
    public void markDisconnected() {
        positionAvailable.set(false);
        batteryAvailable.set(false);
        heartbeatAvailable.set(false);
        attitudeAvailable.set(false);
        imuAvailable.set(false);
        rcAvailable.set(false);
        motorsAvailable.set(false);
        landedStateAvailable.set(false);
        armed.set(false);
        status.set("DISCONNECTED");
    }

    /** 旧版兼容接口：仅更新 14 个标量字段（不更新可用性标识）。 */
    public void updateTelemetry(double lat, double lon, double alt, double speed,
                                 double batteryPct, double voltage, double signalPct,
                                 double heading, double roll, double pitch, double yaw,
                                 int satellites, double hdop) {
        this.lat.set(lat);
        this.lon.set(lon);
        this.alt.set(alt);
        this.speed.set(speed);
        this.batteryPct.set(batteryPct);
        this.voltage.set(voltage);
        this.signalPct.set(signalPct);
        this.heading.set(heading);
        this.roll.set(roll);
        this.pitch.set(pitch);
        this.yaw.set(yaw);
        this.satellites.set(satellites);
        this.hdop.set(hdop);
        this.status.set("CONNECTED");
    }

    // ===== Getter / Setter / Property =====

    public String getModel() { return model.get(); }
    public void setModel(String v) { model.set(v); }
    public StringProperty modelProperty() { return model; }

    public double getMaxRange() { return maxRange.get(); }
    public void setMaxRange(double v) { maxRange.set(v); }
    public DoubleProperty maxRangeProperty() { return maxRange; }

    public double getLat() { return lat.get(); }
    public DoubleProperty latProperty() { return lat; }

    public double getLon() { return lon.get(); }
    public DoubleProperty lonProperty() { return lon; }

    public double getAlt() { return alt.get(); }
    public DoubleProperty altProperty() { return alt; }

    public double getSpeed() { return speed.get(); }
    public DoubleProperty speedProperty() { return speed; }

    public double getBatteryPct() { return batteryPct.get(); }
    public DoubleProperty batteryPctProperty() { return batteryPct; }

    public double getVoltage() { return voltage.get(); }
    public DoubleProperty voltageProperty() { return voltage; }

    public double getSignalPct() { return signalPct.get(); }
    public DoubleProperty signalPctProperty() { return signalPct; }

    public double getHeading() { return heading.get(); }
    public DoubleProperty headingProperty() { return heading; }

    public double getRoll() { return roll.get(); }
    public DoubleProperty rollProperty() { return roll; }

    public double getPitch() { return pitch.get(); }
    public DoubleProperty pitchProperty() { return pitch; }

    public double getYaw() { return yaw.get(); }
    public DoubleProperty yawProperty() { return yaw; }

    public int getSatellites() { return satellites.get(); }
    public IntegerProperty satellitesProperty() { return satellites; }

    public double getHdop() { return hdop.get(); }
    public DoubleProperty hdopProperty() { return hdop; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }

    public boolean isArmed() { return armed.get(); }
    public BooleanProperty armedProperty() { return armed; }
    public void setArmed(boolean v) { armed.set(v); }

    public List<String> getSensorTypes() { return sensorTypes.get(); }
    public void setSensorTypes(List<String> v) { sensorTypes.setAll(v); }

    // ----- 新增字段访问器 -----

    public double getRollSpeed() { return rollSpeed.get(); }
    public DoubleProperty rollSpeedProperty() { return rollSpeed; }

    public double getPitchSpeed() { return pitchSpeed.get(); }
    public DoubleProperty pitchSpeedProperty() { return pitchSpeed; }

    public double getYawSpeed() { return yawSpeed.get(); }
    public DoubleProperty yawSpeedProperty() { return yawSpeed; }

    public double getAccX() { return accX.get(); }
    public DoubleProperty accXProperty() { return accX; }

    public double getAccY() { return accY.get(); }
    public DoubleProperty accYProperty() { return accY; }

    public double getAccZ() { return accZ.get(); }
    public DoubleProperty accZProperty() { return accZ; }

    public double getGyroX() { return gyroX.get(); }
    public DoubleProperty gyroXProperty() { return gyroX; }

    public double getGyroY() { return gyroY.get(); }
    public DoubleProperty gyroYProperty() { return gyroY; }

    public double getGyroZ() { return gyroZ.get(); }
    public DoubleProperty gyroZProperty() { return gyroZ; }

    public double getImuTemp() { return imuTemp.get(); }
    public DoubleProperty imuTempProperty() { return imuTemp; }

    public int[] getRcChannels() { return rcChannels; }
    public int getRcChannel(int idx) { return rcChannels[idx]; }

    public int getRssi() { return rssi.get(); }
    public IntegerProperty rssiProperty() { return rssi; }

    public double[] getMotorOutputs() { return motorOutputs; }
    public double getMotorOutput(int idx) { return motorOutputs[idx]; }

    public int getLandedState() { return landedState.get(); }
    public IntegerProperty landedStateProperty() { return landedState; }

    public int getCustomMode() { return customMode.get(); }
    public IntegerProperty customModeProperty() { return customMode; }

    public int getSystemStatus() { return systemStatus.get(); }
    public IntegerProperty systemStatusProperty() { return systemStatus; }

    public int getAckCommand() { return ackCommand.get(); }
    public IntegerProperty ackCommandProperty() { return ackCommand; }

    public int getAckResult() { return ackResult.get(); }
    public IntegerProperty ackResultProperty() { return ackResult; }

    public int getMissionCount() { return missionCount.get(); }
    public IntegerProperty missionCountProperty() { return missionCount; }

    public String getShellText() { return shellText.get(); }
    public StringProperty shellTextProperty() { return shellText; }
    public void setShellText(String v) { shellText.set(v); }

    // ----- 可用性标识访问器 -----

    public boolean isPositionAvailable() { return positionAvailable.get(); }
    public BooleanProperty positionAvailableProperty() { return positionAvailable; }

    public boolean isBatteryAvailable() { return batteryAvailable.get(); }
    public BooleanProperty batteryAvailableProperty() { return batteryAvailable; }

    public boolean isHeartbeatAvailable() { return heartbeatAvailable.get(); }
    public BooleanProperty heartbeatAvailableProperty() { return heartbeatAvailable; }

    public boolean isAttitudeAvailable() { return attitudeAvailable.get(); }
    public BooleanProperty attitudeAvailableProperty() { return attitudeAvailable; }

    public boolean isImuAvailable() { return imuAvailable.get(); }
    public BooleanProperty imuAvailableProperty() { return imuAvailable; }

    public boolean isRcAvailable() { return rcAvailable.get(); }
    public BooleanProperty rcAvailableProperty() { return rcAvailable; }

    public boolean isMotorsAvailable() { return motorsAvailable.get(); }
    public BooleanProperty motorsAvailableProperty() { return motorsAvailable; }

    public boolean isLandedStateAvailable() { return landedStateAvailable.get(); }
    public BooleanProperty landedStateAvailableProperty() { return landedStateAvailable; }

    /** CF-Drone 飞行模式名（customMode→字符串），未知返回 "?"。 */
    public String getCustomModeName() {
        return switch (customMode.get()) {
            case 0 -> "RAW";
            case 1 -> "ACRO";
            case 2 -> "STAB";
            case 3 -> "ALTHOLD";
            case 4 -> "AUTO";
            default -> "?";
        };
    }

    /** 落地状态名：1=地面 2=空中，未知返回 "—"。 */
    public String getLandedStateName() {
        return switch (landedState.get()) {
            case 1 -> "地面";
            case 2 -> "空中";
            default -> "—";
        };
    }
}
