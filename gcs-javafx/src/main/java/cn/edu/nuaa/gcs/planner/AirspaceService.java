package cn.edu.nuaa.gcs.planner;

import cn.edu.nuaa.gcs.model.Waypoint;
import java.util.ArrayList;
import java.util.List;

public class AirspaceService {

    public enum ZoneType { NOFLY, RESTRICTION, TEMPORARY, CUSTOM }
    public enum CheckResult { PASS, WARN, BLOCK, UNKNOWN, IDLE }

    public static class Zone {
        public String name;
        public ZoneType type;
        public Double maxAlt;
        public List<double[]> coords;
        public long timeStart;
        public long timeEnd;
    }

    public static class AirspacePackage {
        public String id;
        public String name;
        public String coverage;
        public String version;
        public String updatedAt;
        public String expiresAt;
        public List<Zone> zones = new ArrayList<>();
    }

    public static class ValidationResult {
        public CheckResult result;
        public String message;
        public List<int[]> violatingSegments = new ArrayList<>();
    }

    private final List<AirspacePackage> packages = new ArrayList<>();
    private final boolean[] layerVisible = {true, true, true, true};

    public void addPackage(AirspacePackage pkg) { packages.add(pkg); }
    public void removePackage(String id) { packages.removeIf(p -> p.id.equals(id)); }
    public void clearAll() { packages.clear(); }
    public List<AirspacePackage> getPackages() { return packages; }

    public void setLayerVisible(ZoneType type, boolean visible) {
        layerVisible[type.ordinal()] = visible;
    }
    public boolean isLayerVisible(ZoneType type) { return layerVisible[type.ordinal()]; }

    public List<Zone> getActiveZones() {
        List<Zone> zones = new ArrayList<>();
        for (AirspacePackage pkg : packages)
            for (Zone z : pkg.zones)
                if (layerVisible[z.type.ordinal()])
                    zones.add(z);
        return zones;
    }

    public boolean isDataExpired() {
        long now = System.currentTimeMillis();
        for (AirspacePackage pkg : packages) {
            if (pkg.expiresAt != null && !pkg.expiresAt.isEmpty()) {
                try {
                    long exp = java.sql.Date.valueOf(pkg.expiresAt).getTime();
                    if (exp < now) return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    public ValidationResult validateMission(List<Waypoint> waypoints) {
        ValidationResult vr = new ValidationResult();
        if (waypoints.isEmpty()) {
            vr.result = CheckResult.IDLE;
            vr.message = "无航点";
            return vr;
        }
        List<Zone> active = getActiveZones();
        if (active.isEmpty()) {
            vr.result = CheckResult.UNKNOWN;
            vr.message = "未导入空域数据，无法判定";
            return vr;
        }
        if (isDataExpired()) {
            vr.result = CheckResult.WARN;
            vr.message = "空域数据可能过期";
            return vr;
        }
        List<String> wpInNoFly = new ArrayList<>();
        List<String> segCross = new ArrayList<>();
        List<String> altExceed = new ArrayList<>();
        List<String> timeConflict = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint wp = waypoints.get(i);
            double[] pt = {wp.getLon(), wp.getLat()};
            for (Zone z : active) {
                if (z.type == ZoneType.NOFLY && pointInPolygon(pt, z.coords))
                    wpInNoFly.add(String.valueOf(i + 1));
                if (z.maxAlt != null && wp.getAlt() > z.maxAlt)
                    altExceed.add(String.format("%d (%.0fm > %.0fm)", i + 1, wp.getAlt(), z.maxAlt));
                if (z.type == ZoneType.TEMPORARY && z.timeStart > 0 && z.timeEnd > 0
                        && now >= z.timeStart && now <= z.timeEnd
                        && pointInPolygon(pt, z.coords))
                    timeConflict.add(z.name + " (" + (i + 1) + ")");
            }
        }

        for (int i = 0; i < waypoints.size() - 1; i++) {
            double[] p1 = {waypoints.get(i).getLon(), waypoints.get(i).getLat()};
            double[] p2 = {waypoints.get(i + 1).getLon(), waypoints.get(i + 1).getLat()};
            for (Zone z : active) {
                if (z.type == ZoneType.NOFLY && segmentCrossesPolygon(p1, p2, z.coords)) {
                    segCross.add((i + 1) + "-" + (i + 2));
                    vr.violatingSegments.add(new int[]{i, i + 1});
                }
            }
        }

        if (!wpInNoFly.isEmpty() || !segCross.isEmpty()) {
            vr.result = CheckResult.BLOCK;
            List<String> msgs = new ArrayList<>();
            if (!wpInNoFly.isEmpty()) msgs.add("航点 " + String.join(",", wpInNoFly) + " 落入禁飞区");
            if (!segCross.isEmpty()) msgs.add("航段 " + String.join(",", segCross) + " 穿越禁飞区");
            vr.message = String.join("; ", msgs);
        } else if (!altExceed.isEmpty()) {
            vr.result = CheckResult.WARN;
            vr.message = "高度超限: " + String.join("; ", altExceed);
        } else if (!timeConflict.isEmpty()) {
            vr.result = CheckResult.WARN;
            vr.message = "临时限制时段冲突: " + String.join("; ", timeConflict);
        } else {
            boolean allCovered = true;
            for (Waypoint wp : waypoints) {
                if (!isPointInCoverage(wp.getLat(), wp.getLon())) {
                    allCovered = false;
                    break;
                }
            }
            if (!allCovered) {
                vr.result = CheckResult.UNKNOWN;
                vr.message = "部分航点不在已下载空域数据范围内";
            } else {
                vr.result = CheckResult.PASS;
                vr.message = "未发现空域冲突";
            }
        }
        return vr;
    }

    private boolean isPointInCoverage(double lat, double lon) {
        for (Zone z : getActiveZones()) {
            double[] bounds = getBounds(z.coords);
            double margin = 0.01;
            if (lon >= bounds[2] - margin && lon <= bounds[3] + margin &&
                lat >= bounds[0] - margin && lat <= bounds[1] + margin)
                return true;
        }
        return false;
    }

    private double[] getBounds(List<double[]> coords) {
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (double[] c : coords) {
            if (c[1] < minLat) minLat = c[1];
            if (c[1] > maxLat) maxLat = c[1];
            if (c[0] < minLon) minLon = c[0];
            if (c[0] > maxLon) maxLon = c[0];
        }
        return new double[]{minLat, maxLat, minLon, maxLon};
    }

    public static boolean pointInPolygon(double[] pt, List<double[]> poly) {
        double x = pt[0], y = pt[1];
        boolean inside = false;
        int j = poly.size() - 1;
        for (int i = 0; i < poly.size(); j = i++) {
            double xi = poly.get(i)[0], yi = poly.get(i)[1];
            double xj = poly.get(j)[0], yj = poly.get(j)[1];
            if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi))
                inside = !inside;
        }
        return inside;
    }

    public static boolean segmentCrossesPolygon(double[] p1, double[] p2, List<double[]> poly) {
        int j = poly.size() - 1;
        for (int i = 0; i < poly.size(); j = i++) {
            if (segmentsIntersect(p1, p2, poly.get(i), poly.get(j)))
                return true;
        }
        return pointInPolygon(p1, poly) || pointInPolygon(p2, poly);
    }

    private static boolean segmentsIntersect(double[] p1, double[] p2, double[] p3, double[] p4) {
        double d1 = ccw(p3, p4, p1), d2 = ccw(p3, p4, p2);
        double d3 = ccw(p1, p2, p3), d4 = ccw(p1, p2, p4);
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
               ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
    }

    private static double ccw(double[] a, double[] b, double[] c) {
        return (c[1] - a[1]) * (b[0] - a[0]) - (b[1] - a[1]) * (c[0] - a[0]);
    }
}
