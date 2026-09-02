package cn.edu.nuaa.gcs.model;

import javafx.beans.property.*;
import java.util.ArrayList;
import java.util.List;

public class Drone {
    private final StringProperty model = new SimpleStringProperty("DJI-M300");
    private final DoubleProperty maxRange = new SimpleDoubleProperty(5000);
    private final ListProperty<String> sensorTypes = new SimpleListProperty<>(javafx.collections.FXCollections.observableArrayList());
    private final DoubleProperty lat = new SimpleDoubleProperty(32.0612);
    private final DoubleProperty lon = new SimpleDoubleProperty(118.793);
    private final DoubleProperty alt = new SimpleDoubleProperty(0);
    private final DoubleProperty speed = new SimpleDoubleProperty(0);
    private final DoubleProperty batteryPct = new SimpleDoubleProperty(100);
    private final DoubleProperty voltage = new SimpleDoubleProperty(12.6);
    private final DoubleProperty signalPct = new SimpleDoubleProperty(0);
    private final DoubleProperty heading = new SimpleDoubleProperty(0);
    private final DoubleProperty roll = new SimpleDoubleProperty(0);
    private final DoubleProperty pitch = new SimpleDoubleProperty(0);
    private final DoubleProperty yaw = new SimpleDoubleProperty(0);
    private final IntegerProperty satellites = new SimpleIntegerProperty(0);
    private final DoubleProperty hdop = new SimpleDoubleProperty(0);
    private final StringProperty status = new SimpleStringProperty("DISCONNECTED");
    private final BooleanProperty armed = new SimpleBooleanProperty(false);

    public Drone() {
        sensorTypes.addAll("IMU", "COMPASS", "BARO", "GPS");
    }

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
}
