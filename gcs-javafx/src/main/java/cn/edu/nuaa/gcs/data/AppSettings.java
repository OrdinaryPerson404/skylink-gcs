package cn.edu.nuaa.gcs.data;

import java.util.Properties;
import java.io.*;

public class AppSettings {
    private final Properties props = new Properties();
    private static final String FILE = "gcs_settings.properties";

    public String runMode = "e1_real";
    public String serialPort = "COM3";
    public int baudRate = 115200;
    public int txRate = 10;
    public int sampleInterval = 1;
    public int mapZoom = 16;
    public String theme = "dark";
    public String e1Ip = "192.168.4.1";
    public int e1Port = 14550;
    public int heartbeatTimeout = 3000;
    public boolean phoneGps = true;
    public boolean phoneCompass = true;
    public boolean phoneBaro = false;
    public String droneModel = "DJI-M300";
    public double droneMaxRange = 5000;

    public void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (InputStream is = new FileInputStream(f)) {
            props.load(is);
            runMode = props.getProperty("runMode", runMode);
            serialPort = props.getProperty("serialPort", serialPort);
            baudRate = Integer.parseInt(props.getProperty("baudRate", String.valueOf(baudRate)));
            txRate = Integer.parseInt(props.getProperty("txRate", String.valueOf(txRate)));
            sampleInterval = Integer.parseInt(props.getProperty("sampleInterval", String.valueOf(sampleInterval)));
            mapZoom = Integer.parseInt(props.getProperty("mapZoom", String.valueOf(mapZoom)));
            theme = props.getProperty("theme", theme);
            e1Ip = props.getProperty("e1Ip", e1Ip);
            e1Port = Integer.parseInt(props.getProperty("e1Port", String.valueOf(e1Port)));
            heartbeatTimeout = Integer.parseInt(props.getProperty("heartbeatTimeout", String.valueOf(heartbeatTimeout)));
            phoneGps = Boolean.parseBoolean(props.getProperty("phoneGps", String.valueOf(phoneGps)));
            phoneCompass = Boolean.parseBoolean(props.getProperty("phoneCompass", String.valueOf(phoneCompass)));
            phoneBaro = Boolean.parseBoolean(props.getProperty("phoneBaro", String.valueOf(phoneBaro)));
            droneModel = props.getProperty("droneModel", droneModel);
            droneMaxRange = Double.parseDouble(props.getProperty("droneMaxRange", String.valueOf(droneMaxRange)));
        } catch (Exception e) {
            System.err.println("Settings load failed: " + e.getMessage());
        }
    }

    public void save() {
        props.setProperty("runMode", runMode);
        props.setProperty("serialPort", serialPort);
        props.setProperty("baudRate", String.valueOf(baudRate));
        props.setProperty("txRate", String.valueOf(txRate));
        props.setProperty("sampleInterval", String.valueOf(sampleInterval));
        props.setProperty("mapZoom", String.valueOf(mapZoom));
        props.setProperty("theme", theme);
        props.setProperty("e1Ip", e1Ip);
        props.setProperty("e1Port", String.valueOf(e1Port));
        props.setProperty("heartbeatTimeout", String.valueOf(heartbeatTimeout));
        props.setProperty("phoneGps", String.valueOf(phoneGps));
        props.setProperty("phoneCompass", String.valueOf(phoneCompass));
        props.setProperty("phoneBaro", String.valueOf(phoneBaro));
        props.setProperty("droneModel", droneModel);
        props.setProperty("droneMaxRange", String.valueOf(droneMaxRange));
        try (OutputStream os = new FileOutputStream(FILE)) {
            props.store(os, "SkyLink GCS Settings");
        } catch (Exception e) {
            System.err.println("Settings save failed: " + e.getMessage());
        }
    }
}
