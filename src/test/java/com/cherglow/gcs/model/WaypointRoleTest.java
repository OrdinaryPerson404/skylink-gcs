package com.cherglow.gcs.model;

import com.cherglow.gcs.model.Waypoint.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WaypointRoleTest {

    @Test
    void defaultRoleIsWaypoint() {
        Waypoint wp = new Waypoint(1, 0, 0);
        assertEquals(Role.WAYPOINT, wp.getRole());
    }

    @Test
    void setRoleRoundTrip() {
        Waypoint wp = new Waypoint(1, 0, 0);
        wp.setRole(Role.START);
        assertEquals(Role.START, wp.getRole());
        wp.setRole(Role.END);
        assertEquals(Role.END, wp.getRole());
        wp.setRole(Role.WAYPOINT);
        assertEquals(Role.WAYPOINT, wp.getRole());
    }

    @Test
    void roleFromNameAndLabel() {
        assertEquals(Role.WAYPOINT, Role.from("WAYPOINT"));
        assertEquals(Role.START, Role.from("START"));
        assertEquals(Role.END, Role.from("END"));
        assertEquals(Role.WAYPOINT, Role.from("航点"));
        assertEquals(Role.START, Role.from("起点"));
        assertEquals(Role.END, Role.from("终点"));
    }

    @Test
    void roleFromUnknownReturnsNull() {
        assertNull(Role.from("unknown"));
        assertNull(Role.from(null));
    }

    @Test
    void roleLabels() {
        assertEquals("航点", Role.WAYPOINT.label());
        assertEquals("起点", Role.START.label());
        assertEquals("终点", Role.END.label());
    }
}
