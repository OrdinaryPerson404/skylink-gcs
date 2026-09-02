package cn.edu.nuaa.gcs.planner;

import cn.edu.nuaa.gcs.model.Waypoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AirspaceService 单元测试
 * 覆盖：点在多边形内、线段穿过多边形、四项空域校验
 */
class AirspaceServiceTest {

    private AirspaceService service;

    @BeforeEach
    void setUp() {
        service = new AirspaceService();
        // 添加一个正方形禁飞区 (100m x 100m，中心在 32.0600, 118.7900)
        AirspaceService.AirspacePackage pkg = new AirspaceService.AirspacePackage();
        pkg.id = "test-pkg";
        pkg.name = "测试空域";
        pkg.coverage = "测试区域";
        pkg.version = "1.0";
        pkg.updatedAt = "2026-01-01";
        pkg.expiresAt = "2099-12-31";

        AirspaceService.Zone nofly = new AirspaceService.Zone();
        nofly.name = "测试禁飞区";
        nofly.type = AirspaceService.ZoneType.NOFLY;
        nofly.maxAlt = 150.0;
        nofly.coords = new ArrayList<>(Arrays.asList(
            new double[]{118.7890, 32.0610},
            new double[]{118.7910, 32.0610},
            new double[]{118.7910, 32.0590},
            new double[]{118.7890, 32.0590}
        ));
        pkg.zones.add(nofly);

        AirspaceService.Zone restriction = new AirspaceService.Zone();
        restriction.name = "测试限飞区";
        restriction.type = AirspaceService.ZoneType.RESTRICTION;
        restriction.maxAlt = 100.0;
        restriction.coords = new ArrayList<>(Arrays.asList(
            new double[]{118.7920, 32.0620},
            new double[]{118.7940, 32.0620},
            new double[]{118.7940, 32.0600},
            new double[]{118.7920, 32.0600}
        ));
        pkg.zones.add(restriction);

        service.addPackage(pkg);
    }

    @Test
    @DisplayName("点在多边形内：禁飞区内的点")
    void testPointInPolygonInside() {
        double[] pt = {118.7900, 32.0600}; // 在禁飞区中心
        List<double[]> poly = service.getActiveZones().get(0).coords;
        assertTrue(AirspaceService.pointInPolygon(pt, poly), "中心点应在多边形内");
    }

    @Test
    @DisplayName("点在多边形内：禁飞区外的点")
    void testPointInPolygonOutside() {
        double[] pt = {118.7950, 32.0650}; // 远离禁飞区
        List<double[]> poly = service.getActiveZones().get(0).coords;
        assertFalse(AirspaceService.pointInPolygon(pt, poly), "远点应在多边形外");
    }

    @Test
    @DisplayName("线段穿过多边形：横穿禁飞区")
    void testSegmentCrossesPolygon() {
        double[] p1 = {118.7880, 32.0600}; // 禁飞区西侧
        double[] p2 = {118.7920, 32.0600}; // 禁飞区东侧
        List<double[]> poly = service.getActiveZones().get(0).coords;
        assertTrue(AirspaceService.segmentCrossesPolygon(p1, p2, poly),
            "横穿禁飞区的线段应被检测为穿越");
    }

    @Test
    @DisplayName("线段穿过多边形：完全在外侧")
    void testSegmentOutsidePolygon() {
        double[] p1 = {118.7950, 32.0650};
        double[] p2 = {118.7960, 32.0660};
        List<double[]> poly = service.getActiveZones().get(0).coords;
        assertFalse(AirspaceService.segmentCrossesPolygon(p1, p2, poly),
            "完全在外侧的线段不应穿越多边形");
    }

    @Test
    @DisplayName("空域校验：航点落入禁飞区 → BLOCK")
    void testValidateWpInNoFly() {
        List<Waypoint> wps = new ArrayList<>();
        Waypoint w = new Waypoint();
        w.setLat(32.0600);
        w.setLon(118.7900); // 在禁飞区内
        w.setAlt(80);
        wps.add(w);

        AirspaceService.ValidationResult vr = service.validateMission(wps);
        assertEquals(AirspaceService.CheckResult.BLOCK, vr.result,
            "航点在禁飞区内应为 BLOCK");
        assertTrue(vr.message.contains("禁飞区"), "消息应包含禁飞区提示");
    }

    @Test
    @DisplayName("空域校验：航段穿越禁飞区 → BLOCK")
    void testValidateSegmentCrossesNoFly() {
        List<Waypoint> wps = new ArrayList<>();
        Waypoint w1 = new Waypoint();
        w1.setLat(32.0600);
        w1.setLon(118.7880); // 禁飞区西
        w1.setAlt(80);
        wps.add(w1);

        Waypoint w2 = new Waypoint();
        w2.setLat(32.0600);
        w2.setLon(118.7920); // 禁飞区东
        w2.setAlt(80);
        wps.add(w2);

        AirspaceService.ValidationResult vr = service.validateMission(wps);
        assertEquals(AirspaceService.CheckResult.BLOCK, vr.result,
            "航段穿越禁飞区应为 BLOCK");
        assertFalse(vr.violatingSegments.isEmpty(),
            "应包含违规航段");
    }

    @Test
    @DisplayName("空域校验：高度超过限制 → WARN")
    void testValidateAltitudeExceed() {
        List<Waypoint> wps = new ArrayList<>();
        Waypoint w = new Waypoint();
        w.setLat(32.0610);
        w.setLon(118.7930); // 在限飞区内
        w.setAlt(200); // 限飞区最大高度 100m，超了
        wps.add(w);

        AirspaceService.ValidationResult vr = service.validateMission(wps);
        assertEquals(AirspaceService.CheckResult.WARN, vr.result,
            "高度超限应为 WARN");
    }

    @Test
    @DisplayName("空域校验：无航点 → IDLE")
    void testValidateEmpty() {
        List<Waypoint> wps = new ArrayList<>();
        AirspaceService.ValidationResult vr = service.validateMission(wps);
        assertEquals(AirspaceService.CheckResult.IDLE, vr.result,
            "空列表应为 IDLE");
    }

    @Test
    @DisplayName("空域校验：安全航点 → PASS")
    void testValidateSafeWaypoints() {
        List<Waypoint> wps = new ArrayList<>();
        Waypoint w = new Waypoint();
        w.setLat(32.0650);
        w.setLon(118.7850); // 远离所有空域
        w.setAlt(50);
        wps.add(w);

        AirspaceService.ValidationResult vr = service.validateMission(wps);
        assertEquals(AirspaceService.CheckResult.PASS, vr.result,
            "安全航点应为 PASS");
    }
}
