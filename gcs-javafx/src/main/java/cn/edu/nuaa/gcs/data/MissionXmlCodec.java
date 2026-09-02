package cn.edu.nuaa.gcs.data;

import cn.edu.nuaa.gcs.model.*;
import java.io.*;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MissionXmlCodec {

    public static void save(Mission mission, File file) throws IOException {
        try (XMLEncoder enc = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(file)))) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("droneModel", mission.getDroneModel());
            data.put("maxRange", mission.getMaxRange());
            data.put("sensorTypes", new ArrayList<>(mission.getSensorTypes()));
            data.put("waypoints", new ArrayList<>(mission.getWaypoints()));
            enc.writeObject(data);
        }
    }

    public static Mission load(File file) throws Exception {
        try (XMLDecoder dec = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) dec.readObject();
            Mission mission = new Mission();
            mission.setDroneModel((String) data.getOrDefault("droneModel", "DJI-M300"));
            mission.setMaxRange(((Number) data.getOrDefault("maxRange", 5000)).doubleValue());
            @SuppressWarnings("unchecked")
            List<String> sensors = (List<String>) data.getOrDefault("sensorTypes", new ArrayList<>());
            mission.setSensorTypes(sensors);
            @SuppressWarnings("unchecked")
            List<Waypoint> wps = (List<Waypoint>) data.getOrDefault("waypoints", new ArrayList<>());
            for (Waypoint wp : wps) {
                wp.validate();
                mission.addWaypoint(wp);
            }
            return mission;
        } catch (Exception e) {
            throw new Exception("任务文件格式错误: " + e.getMessage(), e);
        }
    }
}
