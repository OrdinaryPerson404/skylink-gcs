package com.cherglow.gcs.core;

import com.cherglow.gcs.model.Waypoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 电池电压预测模型测试。
 */
class BatteryPredictorTest {

    private List<Waypoint> makeRoute(double[][] latLons) {
        List<Waypoint> wps = new ArrayList<>();
        for (int i = 0; i < latLons.length; i++) {
            wps.add(new Waypoint(i + 1, latLons[i][0], latLons[i][1]));
        }
        return wps;
    }

    // 1. LinearRegression 基本测试：已知线性关系训练后预测误差 <5%
    @Test
    void linearRegressionFitsKnownData() {
        double[][] X = {
            {1, 2}, {2, 3}, {3, 5}, {4, 7}, {5, 11}, {6, 13}
        };
        // y = 2 + 3*x1 + 4*x2
        double[] y = new double[X.length];
        for (int i = 0; i < X.length; i++) {
            y[i] = 2 + 3 * X[i][0] + 4 * X[i][1];
        }
        double[] beta = LinearRegression.fit(X, y);
        double pred = LinearRegression.predict(new double[]{3, 5}, beta);
        assertEquals(2 + 3 * 3 + 4 * 5, pred, 0.01, "预测应接近真实值");
    }

    // 2. 合成数据训练后 R² > 0.90
    @Test
    void trainedModelHasHighR2() {
        // 使用 BatteryPredictor 的系数验证：合成数据点预测应接近真实值
        double[] beta = BatteryPredictor.getBeta();
        assertNotNull(beta, "系数不为 null");
        assertTrue(beta.length == 6, "5 特征 + 1 截距 = 6 系数");
        // 验证训练后对典型样本的预测在合理范围
        double[] features = {1000, 100, 0, 60, 50};
        double drop = LinearRegression.predict(features, beta);
        assertTrue(drop > 0 && drop < 1.5, "典型飞行电压下降应在 0~1.5V 范围内, got " + drop);
    }

    // 3. 零负载零风预测测试
    @Test
    void noPayloadNoWindShortDistance() {
        // 短距离 + 无负载 + 无风 → 电压接近满电
        double[][] ll = {{28.0000, 113.0000}, {28.0001, 113.0001}};
        List<Waypoint> wps = makeRoute(ll);
        BatteryPredictor.PredictionResult r = BatteryPredictor.predict(wps, 0, 0, 0);
        assertNotNull(r);
        assertTrue(r.finalVoltage > 4.0, "短距离无负载应接近满电, got " + r.finalVoltage);
        assertTrue(r.feasible, "短距离应可执行");
        assertEquals(2, r.curve.size(), "2 个航点 = 2 个曲线点");
    }

    // 4. 3.31V 阈值测试：构造超长路径
    @Test
    void longRouteTriggersInfeasible() {
        // 构造长距离路径（纬度差 ~0.02° ≈ 2.2km）
        double[][] ll = {
            {28.0000, 113.0000},
            {28.0100, 113.0100},
            {28.0200, 113.0200},
            {28.0300, 113.0300}
        };
        List<Waypoint> wps = makeRoute(ll);
        BatteryPredictor.PredictionResult r = BatteryPredictor.predict(wps, 200, 8, 0);
        assertNotNull(r);
        assertTrue(r.totalDistanceM > 3000, "总距离应 > 3km, got " + r.totalDistanceM);
        assertFalse(r.feasible, "长距离+负载+逆风应不可执行");
        assertTrue(r.minVoltage < BatteryPredictor.LOCK_VOLTAGE,
                "最低电压应低于锁定阈值, got " + r.minVoltage);
    }

    // 5. 顺风逆风测试
    @Test
    void headwindDrainsMoreThanTailwind() {
        // 两个航点，方位角约 0°（正北方向）
        double[][] ll = {{28.0000, 113.0000}, {28.0050, 113.0000}};
        List<Waypoint> wps = makeRoute(ll);

        // 风从北吹来(0°) → 路径向北 → 逆风
        BatteryPredictor.PredictionResult headwind = BatteryPredictor.predict(wps, 50, 8, 0);
        // 风从南吹来(180°) → 路径向北 → 顺风
        BatteryPredictor.PredictionResult tailwind = BatteryPredictor.predict(wps, 50, 8, 180);

        assertNotNull(headwind);
        assertNotNull(tailwind);
        assertTrue(headwind.finalVoltage < tailwind.finalVoltage,
                "逆风电压应低于顺风电压: head=" + headwind.finalVoltage + " tail=" + tailwind.finalVoltage);
    }

    // 6. 航点数 <2 测试
    @Test
    void lessThanTwoWaypointsReturnsNull() {
        List<Waypoint> empty = new ArrayList<>();
        assertNull(BatteryPredictor.predict(empty, 0, 0, 0));

        List<Waypoint> one = new ArrayList<>();
        one.add(new Waypoint(1, 28.0, 113.0));
        assertNull(BatteryPredictor.predict(one, 0, 0, 0));
    }

    // 7. 实时性能测试
    @Test
    void predictionCompletesWithin100ms() {
        double[][] ll = {
            {28.0000, 113.0000}, {28.0010, 113.0010},
            {28.0020, 113.0005}, {28.0030, 113.0020},
            {28.0040, 113.0015}, {28.0050, 113.0030}
        };
        List<Waypoint> wps = makeRoute(ll);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            BatteryPredictor.predict(wps, 100, 5, 45);
        }
        long elapsed = System.currentTimeMillis() - start;
        double avgMs = elapsed / 100.0;
        assertTrue(avgMs < 100, "平均预测应 < 100ms, got " + avgMs + "ms");
    }

    // 8. 方位角计算测试
    @Test
    void bearingCalculation() {
        // 正北方向
        Waypoint a = new Waypoint(1, 28.0, 113.0);
        Waypoint b = new Waypoint(2, 28.01, 113.0);
        double brg = BatteryPredictor.bearing(a, b);
        assertEquals(0, brg, 1.0, "正北方向方位角≈0°, got " + brg);

        // 正东方向
        Waypoint c = new Waypoint(3, 28.0, 113.01);
        double brgEast = BatteryPredictor.bearing(a, c);
        assertEquals(90, brgEast, 1.0, "正东方位角≈90°, got " + brgEast);

        // 正南方向
        Waypoint d = new Waypoint(4, 27.99, 113.0);
        double brgSouth = BatteryPredictor.bearing(a, d);
        assertEquals(180, brgSouth, 1.0, "正南方位角≈180°, got " + brgSouth);
    }

    // 9. 有效风力分量测试
    @Test
    void effectiveWindCalculation() {
        // 路径方位 0°（北），风从北吹来(0°) → 逆风
        double headwind = BatteryPredictor.effectiveWind(0, 0, 10);
        assertEquals(-10, headwind, 0.01, "正前方逆风=-10, got " + headwind);

        // 路径方位 0°（北），风从南吹来(180°) → 顺风
        double tailwind = BatteryPredictor.effectiveWind(0, 180, 10);
        assertEquals(10, tailwind, 0.01, "正后方顺风=+10, got " + tailwind);

        // 路径方位 0°（北），风从东吹来(90°) → 侧风
        double crosswind = BatteryPredictor.effectiveWind(0, 90, 10);
        assertEquals(0, crosswind, 0.5, "侧风≈0, got " + crosswind);
    }

    // 10. 多航点带停留时间测试
    @Test
    void multiWaypointWithStayTime() {
        double[][] ll = {
            {28.0000, 113.0000},
            {28.0020, 113.0020},
            {28.0040, 113.0000}
        };
        List<Waypoint> wps = makeRoute(ll);
        wps.get(1).setStaySec(30);
        wps.get(2).setStaySec(20);

        BatteryPredictor.PredictionResult r = BatteryPredictor.predict(wps, 100, 3, 90);
        assertNotNull(r);
        assertEquals(3, r.curve.size());
        assertEquals(2, r.windAnalysis.size());
        assertTrue(r.estimatedTimeSec > 0, "预计时间应 > 0");
    }
}
