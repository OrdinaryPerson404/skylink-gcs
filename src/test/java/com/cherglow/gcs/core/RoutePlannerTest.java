package com.cherglow.gcs.core;

import com.cherglow.gcs.core.RoutePlanner.RouteResult;
import com.cherglow.gcs.model.Waypoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoutePlannerTest {

    @Test
    void emptyIntermediates() {
        Waypoint start = new Waypoint(1, 0, 0);
        Waypoint end = new Waypoint(2, 0, 0.01);
        RouteResult r = RoutePlanner.planGreedy(start, end, List.of());
        assertEquals(2, r.getOrderedWaypoints().size());
        assertEquals(start, r.getOrderedWaypoints().get(0));
        assertEquals(end, r.getOrderedWaypoints().get(1));
        assertEquals(start.distanceTo(end), r.getTotalDistanceMeters(), 1.0);
    }

    @Test
    void singleIntermediate() {
        Waypoint start = new Waypoint(1, 0, 0);
        Waypoint mid = new Waypoint(2, 0, 0.005);
        Waypoint end = new Waypoint(3, 0, 0.01);
        RouteResult r = RoutePlanner.planGreedy(start, end, List.of(mid));
        assertEquals(3, r.getOrderedWaypoints().size());
        assertEquals(start, r.getOrderedWaypoints().get(0));
        assertEquals(mid, r.getOrderedWaypoints().get(1));
        assertEquals(end, r.getOrderedWaypoints().get(2));
        double expected = start.distanceTo(mid) + mid.distanceTo(end);
        assertEquals(expected, r.getTotalDistanceMeters(), 1.0);
    }

    @Test
    void threeIntermediatesGreedyOrder() {
        // 赤道上经度每 0.001° ≈ 111m，航点共线分布
        Waypoint start = new Waypoint(1, 0, 0);
        Waypoint a = new Waypoint(2, 0, 0.003);    // 距 start ~333m
        Waypoint b = new Waypoint(3, 0, 0.001);    // 距 start ~111m（最近）
        Waypoint c = new Waypoint(4, 0, 0.002);    // 距 start ~222m
        Waypoint end = new Waypoint(5, 0, 0.004);

        // 输入顺序 A, B, C；贪婪应选择 B→C→A（最近邻）
        RouteResult r = RoutePlanner.planGreedy(start, end, List.of(a, b, c));

        assertEquals(5, r.getOrderedWaypoints().size());
        assertEquals(start, r.getOrderedWaypoints().get(0));
        assertEquals(b, r.getOrderedWaypoints().get(1));
        assertEquals(c, r.getOrderedWaypoints().get(2));
        assertEquals(a, r.getOrderedWaypoints().get(3));
        assertEquals(end, r.getOrderedWaypoints().get(4));
    }

    @Test
    void greedyBetterThanSequential() {
        // 贪婪路径应优于或等于按输入顺序的路径
        Waypoint start = new Waypoint(1, 0, 0);
        Waypoint a = new Waypoint(2, 0, 0.003);
        Waypoint b = new Waypoint(3, 0, 0.001);
        Waypoint c = new Waypoint(4, 0, 0.002);
        Waypoint end = new Waypoint(5, 0, 0.004);

        RouteResult greedy = RoutePlanner.planGreedy(start, end, List.of(a, b, c));

        double seqDist = start.distanceTo(a) + a.distanceTo(b) + b.distanceTo(c) + c.distanceTo(end);
        assertTrue(greedy.getTotalDistanceMeters() <= seqDist + 0.001);
    }

    @Test
    void twentyIntermediatesPerformance() {
        Waypoint start = new Waypoint(1, 0, 0);
        Waypoint end = new Waypoint(22, 0, 0.021);
        List<Waypoint> mids = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            mids.add(new Waypoint(i + 2, 0.001 * (i % 3), 0.001 * (i + 1)));
        }
        long t0 = System.nanoTime();
        RouteResult r = RoutePlanner.planGreedy(start, end, mids);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertEquals(22, r.getOrderedWaypoints().size());
        assertTrue(ms < 100, "20 个中间航点应在 100ms 内完成，实际 " + ms + "ms");
    }

    @Test
    void sameStartEndCoordinates() {
        Waypoint start = new Waypoint(1, 10, 20);
        Waypoint end = new Waypoint(2, 10, 20);
        Waypoint mid = new Waypoint(3, 10, 20.005);
        RouteResult r = RoutePlanner.planGreedy(start, end, List.of(mid));
        assertEquals(3, r.getOrderedWaypoints().size());
        // start→end 距离为 0，总距离 = start→mid + mid→end
        double expected = start.distanceTo(mid) + mid.distanceTo(end);
        assertEquals(expected, r.getTotalDistanceMeters(), 1.0);
    }

    @Test
    void resultStartEndFixed() {
        Waypoint start = new Waypoint(1, 0, 0);
        Waypoint end = new Waypoint(10, 0, 0.01);
        List<Waypoint> mids = new ArrayList<>();
        for (int i = 2; i <= 9; i++) {
            mids.add(new Waypoint(i, 0.001 * i, 0.005));
        }
        RouteResult r = RoutePlanner.planGreedy(start, end, mids);
        assertEquals(start, r.getOrderedWaypoints().get(0));
        assertEquals(end, r.getOrderedWaypoints().get(r.getOrderedWaypoints().size() - 1));
    }

    @Test
    void totalDistanceCorrect() {
        Waypoint start = new Waypoint(1, 0, 0);
        Waypoint m1 = new Waypoint(2, 0.001, 0);
        Waypoint m2 = new Waypoint(3, 0.002, 0);
        Waypoint end = new Waypoint(4, 0.003, 0);
        RouteResult r = RoutePlanner.planGreedy(start, end, List.of(m1, m2));
        double seg1 = r.getOrderedWaypoints().get(0).distanceTo(r.getOrderedWaypoints().get(1));
        double seg2 = r.getOrderedWaypoints().get(1).distanceTo(r.getOrderedWaypoints().get(2));
        double seg3 = r.getOrderedWaypoints().get(2).distanceTo(r.getOrderedWaypoints().get(3));
        assertEquals(seg1 + seg2 + seg3, r.getTotalDistanceMeters(), 0.001);
    }

    @Test
    void nullIntermediatesTreatedAsEmpty() {
        Waypoint start = new Waypoint(1, 0, 0);
        Waypoint end = new Waypoint(2, 0, 0.01);
        RouteResult r = RoutePlanner.planGreedy(start, end, null);
        assertEquals(2, r.getOrderedWaypoints().size());
        assertEquals(start.distanceTo(end), r.getTotalDistanceMeters(), 1.0);
    }

    @Test
    void nullStartThrows() {
        Waypoint end = new Waypoint(2, 0, 0);
        assertThrows(IllegalArgumentException.class,
                () -> RoutePlanner.planGreedy(null, end, List.of()));
    }

    @Test
    void nullEndThrows() {
        Waypoint start = new Waypoint(1, 0, 0);
        assertThrows(IllegalArgumentException.class,
                () -> RoutePlanner.planGreedy(start, null, List.of()));
    }
}
