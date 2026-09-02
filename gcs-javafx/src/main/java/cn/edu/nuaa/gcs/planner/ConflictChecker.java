package cn.edu.nuaa.gcs.planner;

import cn.edu.nuaa.gcs.model.Waypoint;
import java.util.ArrayList;
import java.util.List;

public class ConflictChecker {
    private static final double MIN_SEPARATION_M = 50.0;

    public static List<String> checkSeparation(List<Waypoint> waypoints) {
        List<String> violations = new ArrayList<>();
        for (int i = 0; i < waypoints.size(); i++) {
            for (int j = i + 1; j < waypoints.size(); j++) {
                double d = waypoints.get(i).distanceTo(waypoints.get(j));
                if (d < MIN_SEPARATION_M)
                    violations.add(String.format("航点 %d 与 %d 距离 %.0fm（< 50m）", i + 1, j + 1, d));
            }
        }
        return violations;
    }

    public static List<String> checkRange(List<Waypoint> waypoints, double maxRange, Waypoint home) {
        List<String> violations = new ArrayList<>();
        double total = PathOptimizer.totalDistance(waypoints);
        if (total > maxRange)
            violations.add(String.format("总航程 %.0fm 超过最大航程 %.0fm", total, maxRange));
        for (int i = 0; i < waypoints.size(); i++) {
            double d = home.distanceTo(waypoints.get(i));
            if (d > maxRange / 2)
                violations.add(String.format("航点 %d 距起飞点 %.0fm，超出单程航程", i + 1, d));
        }
        return violations;
    }

    public static List<String> checkAll(List<Waypoint> waypoints, double maxRange, Waypoint home) {
        List<String> all = new ArrayList<>();
        all.addAll(checkSeparation(waypoints));
        all.addAll(checkRange(waypoints, maxRange, home));
        return all;
    }
}
