package com.cherglow.gcs.model;

import com.cherglow.gcs.model.Waypoint.Action;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WaypointTest {

    @Test
    void haversineDistanceKnownValue() {
        Waypoint a = new Waypoint(1, 28.6841, 115.8581);
        Waypoint b = new Waypoint(2, a.getLat(), a.getLon() + 0.01); // 约 0.01° 经度
        double d = a.distanceTo(b);
        // 纬度 28.68° 处，0.01° 经度 ≈ 975m；容差 ±30m
        assertEquals(975, d, 30);
    }

    @Test
    void samePointDistanceZero() {
        Waypoint a = new Waypoint(1, 28.6841, 115.8581);
        Waypoint b = new Waypoint(2, 28.6841, 115.8581);
        assertEquals(0, a.distanceTo(b), 0.001);
    }

    @Test
    void defaultAdvancedAttributes() {
        Waypoint wp = new Waypoint(3, 0, 0);
        assertEquals(0, wp.getAltM());
        assertEquals(0, wp.getStaySec());
        assertEquals(Action.CRUISE, wp.getAction());
        assertEquals(0, wp.getPriority());
    }

    @Test
    void settersRoundTrip() {
        Waypoint wp = new Waypoint(4, 0, 0);
        wp.setAltM(50);
        wp.setStaySec(10);
        wp.setPriority(2);
        wp.setAction(Action.PHOTO);
        assertEquals(50, wp.getAltM());
        assertEquals(10, wp.getStaySec());
        assertEquals(2, wp.getPriority());
        assertEquals(Action.PHOTO, wp.getAction());
    }

    @Test
    void actionFromNameAndLabel() {
        assertEquals(Action.CRUISE, Action.from("CRUISE"));
        assertEquals(Action.HOLD, Action.from("悬停"));
        assertEquals(Action.PHOTO, Action.from("PHOTO"));
        assertNull(Action.from("unknown"));
        assertNull(Action.from(null));
    }
}