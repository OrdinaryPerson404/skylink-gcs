package cn.edu.nuaa.gcs.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int id;
    private long timestamp;
    private double lat;
    private double lon;
    private double alt;
    private double speed;
    private double voltage;
    private String droneModel;
    private int duration;
    private double distance;

    public LogRecord() {}

    public LogRecord(int id, long timestamp, double lat, double lon, double alt,
                     double speed, double voltage, String droneModel,
                     int duration, double distance) {
        this.id = id;
        this.timestamp = timestamp;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
        this.speed = speed;
        this.voltage = voltage;
        this.droneModel = droneModel;
        this.duration = duration;
        this.distance = distance;
    }

    public String getFormattedTime() {
        return LocalDateTime.ofEpochSecond(timestamp / 1000, 0,
                java.time.ZoneId.systemDefault().getRules().getOffset(java.time.Instant.ofEpochMilli(timestamp)))
                .format(FMT);
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long v) { timestamp = v; }
    public double getLat() { return lat; }
    public void setLat(double v) { lat = v; }
    public double getLon() { return lon; }
    public void setLon(double v) { lon = v; }
    public double getAlt() { return alt; }
    public void setAlt(double v) { alt = v; }
    public double getSpeed() { return speed; }
    public void setSpeed(double v) { speed = v; }
    public double getVoltage() { return voltage; }
    public void setVoltage(double v) { voltage = v; }
    public String getDroneModel() { return droneModel; }
    public void setDroneModel(String v) { droneModel = v; }
    public int getId() { return id; }
    public void setId(int v) { id = v; }
    public int getDuration() { return duration; }
    public void setDuration(int v) { duration = v; }
    public double getDistance() { return distance; }
    public void setDistance(double v) { distance = v; }
}
