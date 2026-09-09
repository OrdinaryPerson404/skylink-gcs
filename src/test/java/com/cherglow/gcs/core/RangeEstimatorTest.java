package com.cherglow.gcs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S16：航程预测边界（0%/满电/无速度）。 */
class RangeEstimatorTest {

    @Test
    void fullBatteryGivesNominalFlightTime() {
        double mins = RangeEstimator.remainingMinutes(100);
        assertEquals(RangeEstimator.NOMINAL_FLIGHT_MINUTES, mins, 1e-9, "满电=标称续航");
    }

    @Test
    void zeroBatteryGivesZeroTime() {
        assertEquals(0, RangeEstimator.remainingMinutes(0), 0.0);
        assertEquals(0, RangeEstimator.remainingMinutes(-5), 0.0, "越界钳到 0");
    }

    @Test
    void fiftyPercentGivesHalfTime() {
        double mins = RangeEstimator.remainingMinutes(50);
        assertEquals(RangeEstimator.NOMINAL_FLIGHT_MINUTES / 2, mins, 1e-9);
    }

    @Test
    void distanceUsesGroundSpeedTimesRemainingTime() {
        double mins = RangeEstimator.remainingMinutes(50); // 4 分钟
        double spd = 5.0; // m/s
        double dist = RangeEstimator.remainingDistanceMeters(50, spd);
        assertEquals(spd * mins * 60.0, dist, 1e-9, "距离 = 地速 × 剩余分钟 × 60");
        assertEquals(1200.0, dist, 0.01, "4min * 5m/s = 1200m");
    }

    @Test
    void zeroSpeedGivesZeroDistance() {
        assertEquals(0, RangeEstimator.remainingDistanceMeters(80, 0), 0.0, "0 速=悬停");
        assertEquals(0, RangeEstimator.remainingDistanceMeters(80, -3), 0.0, "负速按无速处理");
        assertEquals(0, RangeEstimator.remainingDistanceMeters(0, 5), 0.0, "0 电=0 距");
    }

    @Test
    void overflowBatteryClampsTo100() {
        assertTrue(RangeEstimator.remainingMinutes(150) <= RangeEstimator.NOMINAL_FLIGHT_MINUTES);
    }
}