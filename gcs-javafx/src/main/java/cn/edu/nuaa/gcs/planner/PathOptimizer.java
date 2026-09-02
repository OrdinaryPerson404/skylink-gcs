package cn.edu.nuaa.gcs.planner;

import cn.edu.nuaa.gcs.model.Waypoint;
import java.util.ArrayList;
import java.util.List;

public class PathOptimizer {

    public static List<Waypoint> optimize(List<Waypoint> waypoints, Waypoint home) {
        if (waypoints.size() < 3)
            throw new IllegalArgumentException("至少需要 3 个航点");
        List<Waypoint> remaining = new ArrayList<>(waypoints);
        List<Waypoint> result = new ArrayList<>();
        result.add(home);
        remaining.remove(home);
        Waypoint current = home;
        while (!remaining.isEmpty()) {
            Waypoint nearest = null;
            double minDist = Double.MAX_VALUE;
            for (Waypoint wp : remaining) {
                double d = current.distanceTo(wp);
                if (d < minDist) { minDist = d; nearest = wp; }
            }
            result.add(nearest);
            remaining.remove(nearest);
            current = nearest;
        }
        return result;
    }

    public static double totalDistance(List<Waypoint> waypoints) {
        double total = 0;
        for (int i = 0; i < waypoints.size() - 1; i++)
            total += waypoints.get(i).distanceTo(waypoints.get(i + 1));
        return total;
    }

    public static List<Waypoint> optimize2opt(List<Waypoint> waypoints) {
        List<Waypoint> best = new ArrayList<>(waypoints);
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < best.size() - 2; i++) {
                for (int j = i + 1; j < best.size() - 1; j++) {
                    double before = best.get(i - 1).distanceTo(best.get(i)) +
                            best.get(j).distanceTo(best.get(j + 1));
                    double after = best.get(i - 1).distanceTo(best.get(j)) +
                            best.get(i).distanceTo(best.get(j + 1));
                    if (after < before) {
                        reverseSegment(best, i, j);
                        improved = true;
                    }
                }
            }
        }
        return best;
    }

    private static void reverseSegment(List<Waypoint> list, int i, int j) {
        while (i < j) {
            Waypoint tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
            i++; j--;
        }
    }
}
