package com.cherglow.gcs.core;

import com.cherglow.gcs.model.Waypoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 基于贪婪最近邻策略的最短路径规划器。
 * 给定起点、终点和一组中间航点，计算"起点→中间航点(优化后顺序)→终点"的访问顺序，
 * 使相邻航点间直线距离总和最小。不依赖任何第三方路径规划库或 API。
 *
 * 算法：从起点出发，每步从未访问的中间航点中选择距离当前位置最近的，
 * 移动到该航点并标记已访问，重复直到所有中间航点访问完毕，最后前往终点。
 * 时间复杂度 O(n²)，n = 中间航点数量，可处理 20+ 航点。
 */
public final class RoutePlanner {

    private RoutePlanner() {
    }

    /** 路径规划结果：优化后的有序航点列表 + 最小总飞行距离（米）。 */
    public static final class RouteResult {
        private final List<Waypoint> orderedWaypoints;
        private final double totalDistanceMeters;

        public RouteResult(List<Waypoint> orderedWaypoints, double totalDistanceMeters) {
            this.orderedWaypoints = orderedWaypoints;
            this.totalDistanceMeters = totalDistanceMeters;
        }

        public List<Waypoint> getOrderedWaypoints() {
            return orderedWaypoints;
        }

        public double getTotalDistanceMeters() {
            return totalDistanceMeters;
        }
    }

    /**
     * 贪婪最近邻路径规划。
     *
     * @param start         起点航点（非 null）
     * @param end           终点航点（非 null）
     * @param intermediates 中间航点列表（可为空，不包含 start 和 end）
     * @return RouteResult 含优化后的访问顺序和总距离
     * @throws IllegalArgumentException 如果 start 或 end 为 null
     */
    public static RouteResult planGreedy(Waypoint start, Waypoint end, List<Waypoint> intermediates) {
        if (start == null) {
            throw new IllegalArgumentException("start waypoint must not be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("end waypoint must not be null");
        }

        List<Waypoint> result = new ArrayList<>();
        result.add(start);
        double total = 0;

        List<Waypoint> remaining = intermediates == null
                ? new ArrayList<>() : new ArrayList<>(intermediates);
        Set<Waypoint> visited = new HashSet<>();

        Waypoint current = start;
        while (!remaining.isEmpty()) {
            Waypoint nearest = null;
            double minDist = Double.MAX_VALUE;
            int nearestIdx = -1;
            for (int i = 0; i < remaining.size(); i++) {
                Waypoint candidate = remaining.get(i);
                if (visited.contains(candidate)) {
                    continue;
                }
                double d = current.distanceTo(candidate);
                if (d < minDist) {
                    minDist = d;
                    nearest = candidate;
                    nearestIdx = i;
                }
            }
            if (nearest == null) {
                break;
            }
            total += minDist;
            result.add(nearest);
            visited.add(nearest);
            remaining.remove(nearestIdx);
            current = nearest;
        }

        total += current.distanceTo(end);
        result.add(end);

        return new RouteResult(result, total);
    }
}
