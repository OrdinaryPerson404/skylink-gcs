package com.cherglow.gcs.core;

/**
 * S16 航程预测（标称模型，无电流传感器）。
 * 电池为 600mAh/2.22Wh 1S，标称续航 ~8 分钟起步（常量可调）。
 * 预测 = 剩余电量比例 × 标称续航 → 剩余飞行时间；× GPS 地速(≤0 视为悬停/无速) → 剩余可航距离。
 * 所有输出诚实标注「估算值」。
 */
public final class RangeEstimator {

    /** 标称续航（分钟），满电飞行可用时间的乐观下界（常量可调） */
    public static final double NOMINAL_FLIGHT_MINUTES = 8.0;

    private RangeEstimator() {
    }

    /** 剩余飞行时间（分钟）。batteryPct∈[0,100]，返回 ≥0（0%→0）。 */
    public static double remainingMinutes(int batteryPct) {
        double pct = Math.max(0, Math.min(100, batteryPct));
        return pct / 100.0 * NOMINAL_FLIGHT_MINUTES;
    }

    /** 剩余可航距离（米）。speedMps≤0（悬停/无速度 or 停地）→0。 */
    public static double remainingDistanceMeters(int batteryPct, double speedMps) {
        double mins = remainingMinutes(batteryPct);
        if (mins <= 0 || !(speedMps > 0)) {
            return 0;
        }
        return speedMps * mins * 60.0;
    }
}