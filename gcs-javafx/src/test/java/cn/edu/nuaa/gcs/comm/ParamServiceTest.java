package cn.edu.nuaa.gcs.comm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ParamService 参数读写服务单元测试。
 *
 * 覆盖：单条累积、isComplete 判定、按索引排序、前缀过滤、clear 清空、
 * 无效参数忽略、CF-Drone 84 参数场景模拟。
 */
class ParamServiceTest {

    /** 构造一条 PARAM_VALUE 类型的 Telemetry。 */
    private MavlinkParser.Telemetry makeParam(String name, double value, int index, int count, int type) {
        MavlinkParser.Telemetry t = new MavlinkParser.Telemetry();
        t.valid = true;
        t.hasParam = true;
        t.paramName = name;
        t.paramValue = value;
        t.paramIndex = index;
        t.paramCount = count;
        t.paramType = type;
        return t;
    }

    // ============================================================
    // 1. 单条累积与查询
    // ============================================================

    @Test
    @DisplayName("onParamValue 单条累积后可查询")
    void testSingleAccumulation() {
        ParamService svc = new ParamService();
        svc.onParamValue(makeParam("MOT_THR", 0.5, 0, 84, 9));

        assertEquals(1, svc.getReceivedCount());
        assertEquals(84, svc.getTotalCount());
        assertFalse(svc.isComplete(), "仅收到 1/84 不应判定完整");

        var params = svc.getParams();
        assertEquals(1, params.size());
        assertEquals("MOT_THR", params.get(0).name);
        assertEquals(0.5, params.get(0).value, 0.001);
        assertEquals(0, params.get(0).index);
        assertEquals(9, params.get(0).type);
        assertFalse(params.get(0).dirty, "新接收参数 dirty 应为 false");
    }

    // ============================================================
    // 2. isComplete 判定
    // ============================================================

    @Test
    @DisplayName("isComplete 在收齐后返回 true")
    void testIsCompleteTrue() {
        ParamService svc = new ParamService();
        svc.onParamValue(makeParam("P1", 1.0, 0, 3, 9));
        svc.onParamValue(makeParam("P2", 2.0, 1, 3, 9));
        svc.onParamValue(makeParam("P3", 3.0, 2, 3, 9));
        assertTrue(svc.isComplete(), "收到 3/3 应判定完整");
    }

    @Test
    @DisplayName("isComplete 在部分接收时返回 false")
    void testIsCompleteFalse() {
        ParamService svc = new ParamService();
        svc.onParamValue(makeParam("P1", 1.0, 0, 3, 9));
        svc.onParamValue(makeParam("P2", 2.0, 1, 3, 9));
        assertFalse(svc.isComplete(), "仅收到 2/3 不应判定完整");
    }

    @Test
    @DisplayName("未收到任何参数时 isComplete 返回 false")
    void testIsCompleteEmpty() {
        ParamService svc = new ParamService();
        assertFalse(svc.isComplete());
        assertEquals(0, svc.getReceivedCount());
        assertEquals(-1, svc.getTotalCount(), "totalCount 初始为 -1");
    }

    // ============================================================
    // 3. 按索引排序
    // ============================================================

    @Test
    @DisplayName("getParams 按索引升序返回")
    void testGetParamsOrderedByIndex() {
        ParamService svc = new ParamService();
        // 乱序注入
        svc.onParamValue(makeParam("P_C", 3.0, 2, 3, 9));
        svc.onParamValue(makeParam("P_A", 1.0, 0, 3, 9));
        svc.onParamValue(makeParam("P_B", 2.0, 1, 3, 9));

        var params = svc.getParams();
        assertEquals(0, params.get(0).index);
        assertEquals(1, params.get(1).index);
        assertEquals(2, params.get(2).index);
        assertEquals("P_A", params.get(0).name);
        assertEquals("P_C", params.get(2).name);
    }

    // ============================================================
    // 4. 前缀过滤
    // ============================================================

    @Test
    @DisplayName("getParamsByPrefix 正确过滤参数名前缀")
    void testGetParamsByPrefix() {
        ParamService svc = new ParamService();
        svc.onParamValue(makeParam("MOT_THR", 0.5, 0, 6, 9));
        svc.onParamValue(makeParam("MOT_SPD", 1.0, 1, 6, 9));
        svc.onParamValue(makeParam("IMU_GX", 2.0, 2, 6, 9));
        svc.onParamValue(makeParam("IMU_GY", 3.0, 3, 6, 9));
        svc.onParamValue(makeParam("CTL_PI", 4.0, 4, 6, 9));
        svc.onParamValue(makeParam("CTL_PD", 5.0, 5, 6, 9));

        var motParams = svc.getParamsByPrefix("MOT_");
        assertEquals(2, motParams.size(), "MOT_ 前缀应匹配 2 个");
        assertEquals("MOT_THR", motParams.get(0).name);

        var ctlParams = svc.getParamsByPrefix("CTL_");
        assertEquals(2, ctlParams.size());

        // 空前缀应返回全部
        var allParams = svc.getParamsByPrefix("");
        assertEquals(6, allParams.size());

        // 不存在的前缀返回空
        var noneParams = svc.getParamsByPrefix("WIFI_");
        assertTrue(noneParams.isEmpty());
    }

    // ============================================================
    // 5. clear 清空
    // ============================================================

    @Test
    @DisplayName("clear 清空所有参数与计数")
    void testClear() {
        ParamService svc = new ParamService();
        svc.onParamValue(makeParam("P1", 1.0, 0, 3, 9));
        svc.onParamValue(makeParam("P2", 2.0, 1, 3, 9));
        assertEquals(2, svc.getReceivedCount());

        svc.clear();
        assertEquals(0, svc.getReceivedCount());
        assertEquals(-1, svc.getTotalCount());
        assertTrue(svc.getParams().isEmpty());
        assertEquals(0, svc.getLastUpdateMs());
    }

    // ============================================================
    // 6. 无效参数忽略
    // ============================================================

    @Test
    @DisplayName("onParamValue 忽略 paramName 为空的 Telemetry")
    void testIgnoreEmptyName() {
        ParamService svc = new ParamService();
        // hasParam=true 但 paramName 为空
        MavlinkParser.Telemetry t1 = new MavlinkParser.Telemetry();
        t1.valid = true;
        t1.hasParam = true;
        t1.paramName = "";
        t1.paramValue = 1.0;
        t1.paramIndex = 0;
        t1.paramCount = 3;
        t1.paramType = 9;
        svc.onParamValue(t1);
        assertEquals(0, svc.getReceivedCount(), "空名参数应被忽略");

        // hasParam=false 的也应被忽略
        MavlinkParser.Telemetry t2 = new MavlinkParser.Telemetry();
        t2.valid = true;
        t2.hasParam = false;
        svc.onParamValue(t2);
        assertEquals(0, svc.getReceivedCount(), "hasParam=false 应被忽略");

        // null paramName
        MavlinkParser.Telemetry t3 = new MavlinkParser.Telemetry();
        t3.valid = true;
        t3.hasParam = true;
        t3.paramName = null;
        svc.onParamValue(t3);
        assertEquals(0, svc.getReceivedCount(), "null 名参数应被忽略");
    }

    // ============================================================
    // 7. 同索引参数覆盖（重传场景）
    // ============================================================

    @Test
    @DisplayName("同索引参数重传时覆盖旧值")
    void testOverwriteOnRetransmit() {
        ParamService svc = new ParamService();
        svc.onParamValue(makeParam("P1", 1.0, 0, 1, 9));
        assertEquals(1.0, svc.getParams().get(0).value, 0.001);

        // 重传覆盖
        svc.onParamValue(makeParam("P1", 1.5, 0, 1, 9));
        assertEquals(1, svc.getReceivedCount(), "重传不应增加计数");
        assertEquals(1.5, svc.getParams().get(0).value, 0.001, "应覆盖为 1.5");
    }

    // ============================================================
    // 8. CF-Drone 84 参数场景模拟
    // ============================================================

    @Test
    @DisplayName("CF-Drone 84 参数累积场景")
    void testCfDrone84Params() {
        ParamService svc = new ParamService();
        // 模拟 CF-Drone 暴露的 84 个参数（前缀涵盖 CTL_/IMU_/EST_/MOT_/RC_/WIFI_/MAV_/SF_）
        String[] prefixes = {"CTL_", "IMU_", "EST_", "MOT_", "RC_", "WIFI_", "MAV_", "SF_"};
        for (int i = 0; i < 84; i++) {
            String prefix = prefixes[i % prefixes.length];
            svc.onParamValue(makeParam(prefix + "P" + i, i * 0.1, i, 84, 9));
        }

        assertEquals(84, svc.getReceivedCount());
        assertEquals(84, svc.getTotalCount());
        assertTrue(svc.isComplete(), "84/84 应判定完整");
        assertEquals(84, svc.getParams().size());

        // 验证各前缀数量（84/8 前缀 ≈ 10-11 个/前缀）
        for (String p : prefixes) {
            int n = svc.getParamsByPrefix(p).size();
            assertTrue(n >= 10 && n <= 11, "前缀 " + p + " 应有 10-11 个，实际 " + n);
        }
    }
}
