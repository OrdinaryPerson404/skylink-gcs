package cn.edu.nuaa.gcs.planner;

import cn.edu.nuaa.gcs.model.Waypoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PathOptimizer 单元测试
 * 覆盖：总距离计算、贪婪最近邻优化、2-opt 改进
 */
class PathOptimizerTest {

    private Waypoint wp(double lat, double lon) {
        Waypoint w = new Waypoint();
        w.setLat(lat);
        w.setLon(lon);
        w.setAlt(80);
        return w;
    }

    @Test
    @DisplayName("总距离计算：空列表返回 0")
    void testTotalDistanceEmpty() {
        List<Waypoint> list = new ArrayList<>();
        assertEquals(0, PathOptimizer.totalDistance(list), 0.001);
    }

    @Test
    @DisplayName("总距离计算：单点返回 0")
    void testTotalDistanceSingle() {
        List<Waypoint> list = new ArrayList<>();
        list.add(wp(32.0, 118.0));
        assertEquals(0, PathOptimizer.totalDistance(list), 0.001);
    }

    @Test
    @DisplayName("总距离计算：两点距离")
    void testTotalDistanceTwoPoints() {
        List<Waypoint> list = new ArrayList<>();
        list.add(wp(32.0, 118.0));
        list.add(wp(32.001, 118.001));
        double dist = PathOptimizer.totalDistance(list);
        assertTrue(dist > 100 && dist < 200, "两点距离应在 100-200m 之间，实际: " + dist);
    }

    @Test
    @DisplayName("贪婪优化：优化后距离不增加")
    void testGreedyOptimization() {
        List<Waypoint> list = new ArrayList<>();
        // 构造一条非最优路径的航点序列
        list.add(wp(32.0600, 118.7900));
        list.add(wp(32.0650, 118.8000));
        list.add(wp(32.0610, 118.7920));
        list.add(wp(32.0640, 118.7980));
        list.add(wp(32.0620, 118.7950));

        double origDist = PathOptimizer.totalDistance(list);

        Waypoint home = list.get(0);
        List<Waypoint> optimized = PathOptimizer.optimize(list, home);

        double optDist = PathOptimizer.totalDistance(optimized);
        assertTrue(optDist <= origDist + 0.001,
            "优化后距离不应增加: 原=" + origDist + " 优=" + optDist);
    }

    @Test
    @DisplayName("贪婪优化：保持航点数量不变")
    void testGreedyPreservesCount() {
        List<Waypoint> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(wp(32.06 + i * 0.001, 118.79 + i * 0.001));
        }
        Waypoint home = list.get(0);
        List<Waypoint> optimized = PathOptimizer.optimize(list, home);
        assertEquals(list.size(), optimized.size(), "优化后航点数量应不变");
    }

    @Test
    @DisplayName("2-opt 改进：距离不增加")
    void testTwoOptImprovement() {
        List<Waypoint> list = new ArrayList<>();
        list.add(wp(32.0600, 118.7900));
        list.add(wp(32.0650, 118.7900));
        list.add(wp(32.0650, 118.8000));
        list.add(wp(32.0600, 118.8000));

        double origDist = PathOptimizer.totalDistance(list);
        List<Waypoint> improved = PathOptimizer.optimize2opt(list);
        double improvedDist = PathOptimizer.totalDistance(improved);

        assertTrue(improvedDist <= origDist + 0.001,
            "2-opt 后距离不应增加: 原=" + origDist + " 优=" + improvedDist);
    }
}
