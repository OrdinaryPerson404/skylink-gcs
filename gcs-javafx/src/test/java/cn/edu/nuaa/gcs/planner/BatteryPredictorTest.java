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
}
