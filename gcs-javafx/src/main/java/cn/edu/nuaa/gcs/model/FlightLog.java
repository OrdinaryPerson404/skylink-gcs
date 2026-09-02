package cn.edu.nuaa.gcs.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 飞行记录 —— 一次完整飞行的元数据与轨迹点集合
 */
public class FlightLog {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String id;
    private String droneModel;
    private long startTime;
    private long endTime;
    private double totalDistance; // meters
    private double maxAltitude;    // meters
    private double maxSpeed;       // m/s
    private double minVoltage;     // V
    private int dataPoints;

    // 轨迹采样点（简化：仅保存经纬度用于地图绘制）
    private List<double[]> trajectory = new ArrayList<>();

    public FlightLog() {}

    public String getId() { return id; }
    public void setId(String v) { id = v; }

    public String getDroneModel() { return droneModel; }
    public void setDroneModel(String v) { droneModel = v; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long v) { startTime = v; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long v) { endTime = v; }

    public int getDurationSeconds() { return (int)((endTime - startTime) / 1000); }

    public String getFormattedDuration() {
        int s = getDurationSeconds();
        int m = s / 60;
        int sec = s % 60;
        return String.format("%02d:%02d", m, sec);
    }

    public String getFormattedStartTime() {
        return LocalDateTime.ofEpochSecond(startTime / 1000, 0,
                java.time.ZoneId.systemDefault().getRules().getOffset(java.time.Instant.ofEpochMilli(startTime)))
                .format(FMT);
    }

    public double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(double v) { totalDistance = v; }

    public double getMaxAltitude() { return maxAltitude; }
    public void setMaxAltitude(double v) { maxAltitude = v; }

    public double getMaxSpeed() { return maxSpeed; }
    public void setMaxSpeed(double v) { maxSpeed = v; }

    public double getMinVoltage() { return minVoltage; }
    public void setMinVoltage(double v) { minVoltage = v; }

    public int getDataPoints() { return dataPoints; }
    public void setDataPoints(int v) { dataPoints = v; }

    public List<double[]> getTrajectory() { return trajectory; }
    public void setTrajectory(List<double[]> v) { trajectory = v; }

    public String getDistanceKm() {
        return String.format("%.2f km", totalDistance / 1000.0);
    }
}
