package cn.edu.nuaa.gcs.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Mission implements Serializable {
    private static final long serialVersionUID = 1L;

    private String droneModel = "DJI-M300";
    private double maxRange = 5000;
    private List<String> sensorTypes = new ArrayList<>();
    private LinkedList<Waypoint> waypoints = new LinkedList<>();
    private boolean uploaded = false;

    public Mission() {
        sensorTypes.add("IMU");
        sensorTypes.add("COMPASS");
        sensorTypes.add("BARO");
        sensorTypes.add("GPS");
    }

    public void addWaypoint(Waypoint wp) {
        waypoints.add(wp);
    }

    public void addWaypoint(int index, Waypoint wp) {
        waypoints.add(index, wp);
    }

    public void removeWaypoint(int index) {
        if (index >= 0 && index < waypoints.size())
            waypoints.remove(index);
    }

    public void clearWaypoints() {
        waypoints.clear();
        uploaded = false;
    }

    public Waypoint getWaypoint(int index) {
        return waypoints.get(index);
    }

    public List<Waypoint> getWaypoints() {
        return Collections.unmodifiableList(waypoints);
    }

    public int size() {
        return waypoints.size();
    }

    public double totalDistance() {
        double total = 0;
        for (int i = 0; i < waypoints.size() - 1; i++)
            total += waypoints.get(i).distanceTo(waypoints.get(i + 1));
        return total;
    }

    public String getDroneModel() { return droneModel; }
    public void setDroneModel(String v) { droneModel = v; }
    public double getMaxRange() { return maxRange; }
    public void setMaxRange(double v) { maxRange = v; }
    public List<String> getSensorTypes() { return sensorTypes; }
    public void setSensorTypes(List<String> v) { sensorTypes = new ArrayList<>(v); }
    public boolean isUploaded() { return uploaded; }
    public void setUploaded(boolean v) { uploaded = v; }
}
