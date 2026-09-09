package com.cherglow.gcs.core;

import com.cherglow.gcs.model.Waypoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MissionXmlTest {

    @TempDir
    Path tempDir;

    @Test
    void roundTripPreservesOrderAndFields() throws IOException {
        Waypoint w1 = new Waypoint(7, 28.1, 115.2);
        w1.setAltM(120.5);
        w1.setStaySec(15);
        w1.setAction(Waypoint.Action.PHOTO);
        w1.setPriority(3);
        w1.setRole(Waypoint.Role.START);

        Waypoint w2 = new Waypoint(9, 28.2, 115.3);
        w2.setAltM(88.0);
        w2.setStaySec(2);
        w2.setAction(Waypoint.Action.HOLD);
        w2.setPriority(1);
        w2.setRole(Waypoint.Role.END);

        Path file = tempDir.resolve("mission.xml");
        MissionXml.save(file, "测试任务", List.of(w1, w2));

        MissionXml.MissionFile loaded = MissionXml.load(file);
        assertEquals("测试任务", loaded.name());
        assertEquals(1, loaded.version());
        assertEquals(2, loaded.waypoints().size());

        MissionXml.MissionWaypoint l1 = loaded.waypoints().get(0);
        assertEquals(7, l1.id());
        assertEquals(28.1, l1.lat(), 1e-9);
        assertEquals(115.2, l1.lon(), 1e-9);
        assertEquals(120.5, l1.altM(), 1e-9);
        assertEquals(15, l1.staySec());
        assertEquals(Waypoint.Action.PHOTO, l1.action());
        assertEquals(3, l1.priority());
        assertEquals(Waypoint.Role.START, l1.role());

        MissionXml.MissionWaypoint l2 = loaded.waypoints().get(1);
        assertEquals(9, l2.id());
        assertEquals(28.2, l2.lat(), 1e-9);
        assertEquals(115.3, l2.lon(), 1e-9);
        assertEquals(88.0, l2.altM(), 1e-9);
        assertEquals(2, l2.staySec());
        assertEquals(Waypoint.Action.HOLD, l2.action());
        assertEquals(1, l2.priority());
        assertEquals(Waypoint.Role.END, l2.role());
    }

    @Test
    void malformedXmlIsRejected() throws IOException {
        Path file = tempDir.resolve("broken.xml");
        Files.writeString(file,
                "<mission version=\"1\" waypointCount=\"1\"><name>x</name><waypoints><waypoint></mission>",
                StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> MissionXml.load(file));
    }

    @Test
    void missingRequiredFieldsAreRejected() throws IOException {
        Path file = tempDir.resolve("missing-field.xml");
        Files.writeString(file,
                """
                        <mission version="1" waypointCount="1">
                          <name>x</name>
                          <waypoints>
                            <waypoint id="1" lat="28.1" lon="115.2" alt="0" stay="0" priority="0"/>
                          </waypoints>
                        </mission>
                        """,
                StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> MissionXml.load(file));
    }
}
