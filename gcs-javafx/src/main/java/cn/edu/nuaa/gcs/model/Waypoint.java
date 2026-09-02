package cn.edu.nuaa.gcs.model;

import cn.edu.nuaa.gcs.planner.GeoUtil;
import java.io.Serializable;
import java.util.Objects;

public class Waypoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private double lat;
    private double lon;
    private double alt;
    private double hold;
    private ActionMode action;
    private int priority;

    public Waypoint() {
        this.action = ActionMode.CRUISE;
        this.priority = 3;
    }

    public Waypoint(double lat, double lon, double alt, double hold, ActionMode action, int priority) {
        setLat(lat);
        setLon(lon);
        setAlt(alt);
        setHold(hold);
        setAction(action);
        setPriority(priority);
    }

    public double getLat() { return lat; }
    public void setLat(double lat) {
        if (lat < -90 || lat > 90)
            throw new IllegalArgumentException("纬度越界: " + lat + "，合法范围 [-90, 90]");
        this.lat = lat;
    }

    public double getLon() { return lon; }
    public void setLon(double lon) {
        if (lon < -180 || lon > 180)
            throw new IllegalArgumentException("经度越界: " + lon + "，合法范围 [-180, 180]");
        this.lon = lon;
    }

    public double getAlt() { return alt; }
    public void setAlt(double alt) {
        if (alt < 0)
            throw new IllegalArgumentException("高度不能为负: " + alt);
        this.alt = alt;
    }

    public double getHold() { return hold; }
    public void setHold(double hold) {
        if (hold < 0)
            throw new IllegalArgumentException("停留时间不能为负: " + hold);
        this.hold = hold;
    }

    public ActionMode getAction() { return action; }
    public void setAction(ActionMode action) { this.action = Objects.requireNonNullElse(action, ActionMode.CRUISE); }

    public int getPriority() { return priority; }
    public void setPriority(int priority) {
        if (priority < 1 || priority > 5)
            throw new IllegalArgumentException("优先级越界: " + priority + "，合法范围 [1, 5]");
        this.priority = priority;
    }

    public double distanceTo(Waypoint other) {
        return GeoUtil.haversine(this.lat, this.lon, other.lat, other.lon);
    }

    public void validate() {
        setLat(lat); setLon(lon); setAlt(alt); setHold(hold); setPriority(priority);
    }

    @Override
    public String toString() {
        return String.format("WP[%.5f,%.5f alt=%.0f hold=%.0f %s p%d]", lat, lon, alt, hold, action, priority);
    }
}
