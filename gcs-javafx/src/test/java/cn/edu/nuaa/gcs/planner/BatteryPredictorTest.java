package cn.edu.nuaa.gcs.planner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * BatteryPredictor 单元测试
 * 覆盖：线性回归模型训练、预测输出合理性、参数边界
 */
class BatteryPredictorTest {

    @Test
    @DisplayName("默认模型可正常预测")
    void testDefaultPrediction() {
        BatteryPredictor bp = new BatteryPredictor();
        double[] result = bp.predict(5.0, 1.0, 3.0);
        assertNotNull(result, "预测结果不应为 null");
        assertEquals(2, result.length, "结果应包含续航和耗电两个值");

        double endurance = result[0];
        double battery = result[1];

        assertTrue(endurance > 0 && endurance < 60,
            "续航应在合理范围 (0-60min), 实际: " + endurance);
        assertTrue(battery >= 0 && battery <= 100,
            "耗电应在 0-100% 之间, 实际: " + battery);
    }

    @Test
    @DisplayName("距离越远，耗电越多")
    void testDistanceEffect() {
        BatteryPredictor bp = new BatteryPredictor();
        double[] shortTrip = bp.predict(2.0, 1.0, 3.0);
        double[] longTrip = bp.predict(10.0, 1.0, 3.0);

        assertTrue(longTrip[1] > shortTrip[1],
            "长途耗电应更多: 短=" + shortTrip[1] + "% 长=" + longTrip[1] + "%");
    }

    @Test
    @DisplayName("负载越重，耗电越多")
    void testPayloadEffect() {
        BatteryPredictor bp = new BatteryPredictor();
        double[] light = bp.predict(5.0, 0.5, 3.0);
        double[] heavy = bp.predict(5.0, 2.0, 3.0);

        assertTrue(heavy[1] > light[1],
            "重载耗电应更多: 轻=" + light[1] + "% 重=" + heavy[1] + "%");
    }

    @Test
    @DisplayName("风速越大，耗电越多")
    void testWindEffect() {
        BatteryPredictor bp = new BatteryPredictor();
        double[] calm = bp.predict(5.0, 1.0, 1.0);
        double[] windy = bp.predict(5.0, 1.0, 10.0);

        assertTrue(windy[1] >= calm[1],
            "大风耗电应更多或相等: 无风=" + calm[1] + "% 有风=" + windy[1] + "%");
    }

    @Test
    @DisplayName("零公里飞行耗电接近 0")
    void testZeroDistance() {
        BatteryPredictor bp = new BatteryPredictor();
        double[] result = bp.predict(0, 0, 0);
        assertTrue(result[1] < 20, "零公里耗电应较小, 实际: " + result[1] + "%");
    }

    // ================================================================
    // 动态模型测试：observe / predictDynamic / reset / sampleCount
    // ================================================================

    @Test
    @DisplayName("动态模型：样本不足时 predictDynamic 返回 NaN")
    void testInsufficientSamplesReturnsNaN() {
        BatteryPredictor bp = new BatteryPredictor(1500.0);
        assertTrue(Double.isNaN(bp.predictDynamic()), "空窗口应返回 NaN");
        bp.observe(1.0, 1.5, 12.0, 10, 99, 25.0);
        assertTrue(Double.isNaN(bp.predictDynamic()), "单样本应返回 NaN");
    }

    @Test
    @DisplayName("动态模型：恒定电流下双路外推结果一致且合理")
    void testConstantCurrentPrediction() {
        BatteryPredictor bp = new BatteryPredictor(1500.0);
        // 1A 恒定放电，每秒消耗约 1000/3600 ≈ 0.278 mAh，剩余 90%
        bp.observe(1.0, 1.0, 12.0, 150,  90, 25.0);
        bp.observe(2.0, 1.0, 12.0, 150 + 1000.0 / 3600.0, 90, 25.0);
        double t = bp.predictDynamic();
        assertTrue(Double.isFinite(t), "应有有限预测值");
        // 剩余 1350 mAh / 1A * 3600 ≈ 4860 s（电流法）
        // 容量率法：rate≈0.278 mAh/s, 剩余 1350 mAh → ≈4860 s
        assertTrue(t > 4000 && t < 6000,
            "1A 恒流下剩余 90% 应约 4860s, 实际: " + t);
    }

    @Test
    @DisplayName("动态模型：电流越大剩余时间越短")
    void testCurrentMonotonicity() {
        BatteryPredictor low  = new BatteryPredictor(1500.0);
        BatteryPredictor high = new BatteryPredictor(1500.0);
        for (int i = 0; i < 5; i++) {
            low.observe (i + 1, 0.5, 12.0, 10 + i * 0.139,  90, 25.0);
            high.observe(i + 1, 3.0, 12.0, 10 + i * 0.833, 90, 25.0);
        }
        double tLow  = low.predictDynamic();
        double tHigh = high.predictDynamic();
        assertTrue(tLow > tHigh,
            "低电流应续航更长: 低=" + tLow + "s 高=" + tHigh + "s");
    }

    @Test
    @DisplayName("动态模型：电流为零（仅悬停功耗<1e-6）返回 NaN")
    void testZeroCurrentReturnsNaN() {
        BatteryPredictor bp = new BatteryPredictor(1500.0);
        bp.observe(1.0, 0.0, 12.0, 0,  100, 25.0);
        bp.observe(2.0, 0.0, 12.0, 0,  100, 25.0);
        assertTrue(Double.isNaN(bp.predictDynamic()),
            "电流与容量均无变化时不应外推");
    }

    @Test
    @DisplayName("动态模型：observe 拒绝无效输入")
    void testObserveRejectsInvalid() {
        BatteryPredictor bp = new BatteryPredictor(1500.0);
        assertFalse(bp.observe(-1, 1.0, 12.0, 0, 100, 25), "负时间戳应拒绝");
        assertFalse(bp.observe(1.0, -0.5, 12.0, 0, 100, 25), "负电流应拒绝");
        assertFalse(bp.observe(1.0, 1.0, 0,    0, 100, 25), "零电压应拒绝");
        assertFalse(bp.observe(Double.NaN, 1.0, 12.0, 0, 100, 25), "NaN 时间应拒绝");
        assertTrue(bp.observe(1.0, 1.0, 12.0, 0, 100, 25), "首次合法样本应接受");
        assertFalse(bp.observe(1.2, 1.0, 12.0, 0, 100, 25),
            "小于 MIN_SAMPLE_INTERVAL 的二次样本应拒绝（防抖）");
        assertEquals(1, bp.sampleCount(), "窗口应仅含 1 个样本");
    }

    @Test
    @DisplayName("动态模型：滑动窗口上限溢出淘汰最旧样本")
    void testSlidingWindowEviction() {
        BatteryPredictor bp = new BatteryPredictor(1500.0);
        for (int i = 0; i < 50; i++) {
            bp.observe(i + 1, 1.0, 12.0, i * 0.278, 90, 25.0);
        }
        assertTrue(bp.sampleCount() <= 30,
            "窗口应不超过 30, 实际: " + bp.sampleCount());
        assertTrue(Double.isFinite(bp.predictDynamic()), "应仍可预测");
    }

    @Test
    @DisplayName("动态模型：reset 清空观测窗口")
    void testReset() {
        BatteryPredictor bp = new BatteryPredictor(1500.0);
        bp.observe(1.0, 1.0, 12.0, 10, 90, 25);
        bp.observe(2.0, 1.0, 12.0, 11, 90, 25);
        assertEquals(2, bp.sampleCount());
        bp.reset();
        assertEquals(0, bp.sampleCount(), "reset 后应为空");
        assertTrue(Double.isNaN(bp.predictDynamic()), "reset 后应返回 NaN");
    }

    @Test
    @DisplayName("动态模型：仅容量率法可用时退化为单路预测")
    void testFallbackToRateMethod() {
        // remainingPct=-1 但容量在变化 → 容量率法可工作
        BatteryPredictor bp = new BatteryPredictor(1500.0);
        bp.observe(1.0, 1.0, 12.0, 100,  -1, 25.0);
        bp.observe(2.0, 1.0, 12.0, 200,  -1, 25.0);
        double t = bp.predictDynamic();
        // rate=100mAh/s, remaining=1300 → 13s（注意：这里观测时间太短不代表真实场景）
        assertTrue(Double.isFinite(t), "容量率法应可工作");
        assertTrue(t > 0, "剩余时间应为正");
    }

    @Test
    @DisplayName("静态模型与动态模型可共存")
    void testBothModelsCoexist() {
        BatteryPredictor bp = new BatteryPredictor(1500.0);
        // 静态可用
        double[] s = bp.predict(5.0, 1.0, 3.0);
        assertTrue(s[0] > 0 && s[1] >= 0);
        // 动态未启用
        assertTrue(Double.isNaN(bp.predictDynamic()));
        // 动态可用后静态仍可用
        bp.observe(1.0, 1.0, 12.0, 10, 90, 25);
        bp.observe(2.0, 1.0, 12.0, 10.278, 90, 25);
        assertTrue(Double.isFinite(bp.predictDynamic()));
        double[] s2 = bp.predict(5.0, 1.0, 3.0);
        assertEquals(s[0], s2[0], 1e-9, "静态模型不应受动态观测影响");
    }
}
