package cn.edu.nuaa.gcs.planner;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 电池续航预测器（双模型）。
 *
 * 1) 静态模型：基于训练样本的多变量线性回归，用于飞行前预估
 *    （距离 / 载荷 / 风速 → 续航分钟与电量成本），保留以兼容既有调用。
 *
 * 2) 动态模型：基于 BATTERY_STATUS(147) 在线观测的滑动窗口预测，
 *    使用真实放电电流、容量消耗、剩余电量估算剩余飞行时间（秒）。
 *    双路冗余外推（电流法 + 容量率法），取一致者，单路可用时退化为单路。
 *
 * 项目硬约束：
 * - 不修改飞控固件或参数，仅读取遥测。
 * - 当飞控不上报电池数据（如 CF-Drone 默认不上报 BATTERY_STATUS）时，
 *   {@link #predictDynamic()} 返回 NaN，UI 必须显示 "—" 而非默认值。
 * - 容量上限、电流为零等无法外推的情形一律返回 NaN，禁止给出误导性数字。
 */
public class BatteryPredictor {

    /** 默认电池总容量（mAh），仅用于动态预测；真实值应通过参数读取覆盖。 */
    private static final double DEFAULT_TOTAL_CAPACITY_MAH = 1500.0;
    /** 滑动窗口最大样本数（约对应 30 秒@1Hz 或 15 秒@2Hz 的遥测周期）。 */
    private static final int MAX_SAMPLES = 30;
    /** 最小观测时间间隔（秒），低于此值视为同帧重复，忽略以防止过采样偏置。 */
    private static final double MIN_SAMPLE_INTERVAL_S = 0.5;
    /** 样本量不足此数时返回 NaN（避免单点外推）。 */
    private static final int MIN_SAMPLES_FOR_PREDICTION = 2;
    /** 电流法/容量率法结果相差 5 倍以上视为离群，取较小者（保守估计）。 */
    private static final double OUTLIER_RATIO = 5.0;

    private double totalCapacityMah;
    private final Deque<Sample> samples = new ArrayDeque<>();

    private double[] theta;

    public BatteryPredictor() {
        this(DEFAULT_TOTAL_CAPACITY_MAH);
    }

    public BatteryPredictor(double totalCapacityMah) {
        this.totalCapacityMah = Math.max(1.0, totalCapacityMah);
        trainDefault();
    }

    /**
     * 更新电池总容量（mAh）。从飞控 BATT_CAPACITY / BATT_CAPACITY_MAH 等参数读取后调用。
     * 影响动态预测（电流法/容量率法）的剩余时间估算精度。
     */
    public void setTotalCapacityMah(double mah) {
        if (mah > 0 && Math.abs(mah - totalCapacityMah) > 0.5) {
            totalCapacityMah = mah;
            samples.clear();  // 容量变更后重置滑动窗口，避免基于旧容量的外推
        }
    }

    public double getTotalCapacityMah() { return totalCapacityMah; }

    // ==================================================================
    // 动态模型：在线观测 + 滑动窗口外推
    // ==================================================================

    /**
     * 观测一次真实电池状态，加入滑动窗口。
     *
     * @param epochSeconds        时间戳（秒）；可用 {@code System.currentTimeMillis()/1000.0}
     * @param currentA            放电电流（A，&gt;=0 表示放电；&lt;0 视为无效）
     * @param voltageV            电压（V，&lt;=0 视为无效）
     * @param capacityConsumedMah 已消耗容量（mAh，&lt;0 归零）
     * @param remainingPct        剩余电量百分比（0-100，-1=未知）
     * @param tempC               电池温度（°C）
     * @return {@code true} 表示样本被采纳（时间间隔足够、字段有效）
     */
    public boolean observe(double epochSeconds, double currentA, double voltageV,
                           double capacityConsumedMah, int remainingPct, double tempC) {
        if (!Double.isFinite(epochSeconds) || epochSeconds < 0) return false;
        if (!Double.isFinite(currentA) || currentA < 0) return false;
        if (!Double.isFinite(voltageV) || voltageV <= 0) return false;

        Sample last = samples.peekLast();
        if (last != null && epochSeconds - last.t < MIN_SAMPLE_INTERVAL_S) {
            return false; // 防抖：同帧重复或时间倒流
        }

        Sample s = new Sample(epochSeconds, currentA, voltageV,
                Math.max(0, capacityConsumedMah),
                remainingPct, tempC);
        samples.addLast(s);
        while (samples.size() > MAX_SAMPLES) {
            samples.pollFirst();
        }
        return true;
    }

    /**
     * 基于在线观测样本动态预测剩余飞行时间（秒）。
     *
     * <p>双路冗余外推：
     * <ul>
     *   <li><b>电流法</b>：avgI = mean(positive currents)；
     *       剩余容量 = totalCap &times; remainingPct/100；
     *       t = 剩余容量(mAh) / avgI(A) &times; 3600</li>
     *   <li><b>容量率法</b>：rate = (last.consumed - first.consumed) / (last.t - first.t)；
     *       剩余 = max(0, totalCap - last.consumed)；t = 剩余 / rate</li>
     * </ul>
     * 两路均可用且偏差在 {@value #OUTLIER_RATIO} 倍以内时取平均；
     * 偏差过大取较小者（保守）；仅一路可用时退化为单路。
     *
     * @return 剩余飞行秒数；样本不足 / 电流为零 / 容量未变化时返回 {@code Double.NaN}
     */
    public double predictDynamic() {
        if (samples.size() < MIN_SAMPLES_FOR_PREDICTION) {
            return Double.NaN;
        }
        Sample first = samples.peekFirst();
        Sample last = samples.peekLast();
        double dt = last.t - first.t;
        if (dt <= 0) return Double.NaN;

        // ---- 电流法 ----
        double sumI = 0;
        int n = 0;
        for (Sample s : samples) {
            if (s.current > 0) { sumI += s.current; n++; }
        }
        double tCurrent = Double.NaN;
        if (n >= MIN_SAMPLES_FOR_PREDICTION) {
            double avgI = sumI / n;
            int remainingPct = pickRemainingPct(last);
            if (remainingPct >= 0 && avgI > 1e-6) {
                double remainingMah = totalCapacityMah * remainingPct / 100.0;
                tCurrent = remainingMah / avgI * 3600.0;
            }
        }

        // ---- 容量率法 ----
        double dConsumed = last.consumedMah - first.consumedMah;
        double tRate = Double.NaN;
        if (dConsumed > 1e-6) {
            double rateMahPerS = dConsumed / dt;
            double remainingMah = Math.max(0, totalCapacityMah - last.consumedMah);
            tRate = remainingMah / rateMahPerS;
        }

        // ---- 合并 ----
        if (Double.isFinite(tCurrent) && Double.isFinite(tRate)) {
            double min = Math.min(tCurrent, tRate);
            double max = Math.max(tCurrent, tRate);
            if (max / Math.max(1.0, min) > OUTLIER_RATIO) {
                return min; // 离群：保守取小
            }
            return (tCurrent + tRate) / 2.0;
        }
        return Double.isFinite(tCurrent) ? tCurrent : tRate;
    }

    /**
     * 当前滑动窗口样本数（UI 可用于显示 "数据不足" 提示）。
     */
    public int sampleCount() {
        return samples.size();
    }

    /**
     * 重置在线观测窗口。
     *
     * <p>调用时机：通信断连恢复后、换电后、飞行任务结束后，避免历史样本污染新预测。
     */
    public void reset() {
        samples.clear();
    }

    /**
     * 取最近一次样本的剩余电量百分比。
     * 优先使用飞控上报值；无效时按已消耗容量估算。
     */
    private int pickRemainingPct(Sample last) {
        if (last.remainingPct >= 0 && last.remainingPct <= 100) {
            return last.remainingPct;
        }
        double usedFraction = last.consumedMah / totalCapacityMah;
        int est = (int) Math.round((1 - usedFraction) * 100);
        return Math.max(0, Math.min(100, est));
    }

    /** 单次电池观测样本（不可变）。 */
    private static final class Sample {
        final double t;             // epoch seconds
        final double current;       // A
        final double voltage;       // V
        final double consumedMah;   // mAh
        final int remainingPct;     // 0-100, -1 unknown
        final double tempC;         // °C
        Sample(double t, double current, double voltage, double consumedMah,
               int remainingPct, double tempC) {
            this.t = t;
            this.current = current;
            this.voltage = voltage;
            this.consumedMah = consumedMah;
            this.remainingPct = remainingPct;
            this.tempC = tempC;
        }
    }

    // ==================================================================
    // 静态模型（保留兼容）
    // ==================================================================

    public double[] predict(double distanceKm, double loadKg, double windMs) {
        double endurance = theta[0] + theta[1] * distanceKm + theta[2] * loadKg + theta[3] * windMs;
        endurance = Math.max(1, endurance);
        double cost = Math.min(95, 18 + distanceKm * 9 + loadKg * 6 + windMs * 2);
        return new double[]{endurance, cost};
    }

    private void trainDefault() {
        double[][] X = {
            {1, 2.0, 0.5, 3},
            {1, 3.5, 1.0, 5},
            {1, 5.0, 1.5, 4},
            {1, 1.5, 0.3, 2},
            {1, 4.2, 0.8, 6},
            {1, 6.0, 2.0, 7},
            {1, 3.0, 0.0, 3},
            {1, 7.5, 1.2, 8},
        };
        double[] y = {16, 12, 8, 19, 10, 5, 15, 3};
        theta = normalEquation(X, y);
    }

    private static double[] normalEquation(double[][] X, double[] y) {
        int n = X[0].length;
        double[][] XtX = new double[n][n];
        double[] Xty = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < X.length; k++)
                    XtX[i][j] += X[k][i] * X[k][j];
            }
            for (int k = 0; k < X.length; k++)
                Xty[i] += X[k][i] * y[k];
        }
        double[][] inv = invert(XtX);
        double[] result = new double[n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                result[i] += inv[i][j] * Xty[j];
        return result;
    }

    /**
     * NxN 矩阵求逆（高斯-若尔当消元，带部分主元选择）。
     * 用于 {@link #normalEquation} 求解正规方程。
     * 矩阵奇异时抛出 {@link RuntimeException}。
     */
    private static double[][] invert(double[][] m) {
        int n = m.length;
        double[][] aug = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++)
                aug[i][j] = m[i][j];
            aug[i][n + i] = 1;
        }
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++)
                if (Math.abs(aug[r][col]) > Math.abs(aug[pivot][col]))
                    pivot = r;
            if (Math.abs(aug[pivot][col]) < 1e-12)
                throw new RuntimeException("矩阵不可逆（行列式≈0）");
            double[] tmp = aug[col]; aug[col] = aug[pivot]; aug[pivot] = tmp;
            double pivVal = aug[col][col];
            for (int j = 0; j < 2 * n; j++)
                aug[col][j] /= pivVal;
            for (int r = 0; r < n; r++) {
                if (r == col) continue;
                double factor = aug[r][col];
                for (int j = 0; j < 2 * n; j++)
                    aug[r][j] -= factor * aug[col][j];
            }
        }
        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                inv[i][j] = aug[i][n + j];
        return inv;
    }

    public double[] getTheta() { return theta; }
}
