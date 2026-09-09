package com.cherglow.gcs.core;

import com.cherglow.gcs.model.Waypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 电池电压预测模型（多元线性回归）。
 *
 * 输入: 航点列表 + 负载重量 + 风速 + 风向
 * 输出: 每个航点处的预测电压、任务可行性判定、风力分析
 *
 * 模型在类加载时一次性训练（合成数据 ~800 样本），后续预测仅做向量点积。
 */
public final class BatteryPredictor {

    public static final double START_VOLTAGE = 4.2;
    public static final double LOCK_VOLTAGE = 3.31;
    public static final double WARN_VOLTAGE = 3.5;
    public static final double CRUISE_SPEED_MPS = 5.0;
    public static final int TRAINING_SAMPLES = 800;

    // 合成数据物理系数（用于生成训练数据的"真实"关系）
    private static final double COEF_DISTANCE  = 0.00040;  // V/m
    private static final double COEF_PAYLOAD   = 0.00006;  // V/g (全程)
    // 负号：顺风(effWind>0)减少压降，逆风(effWind<0)增加压降
    private static final double COEF_WIND       = -0.00080; // V/(m/s)
    private static final double COEF_STAY      = 0.00190;  // V/s
    private static final double COEF_ALTITUDE  = 0.00001;  // V/m
    private static final double NOISE_RATIO    = 0.03;     // ±3% 高斯噪声

    private static final int NUM_FEATURES = 5;

    private static final double[] beta;
    private static double firmwareAdjustFactor = 1.0;

    static {
        beta = train();
    }

    private BatteryPredictor() {
    }

    // ===================== 预测结果数据结构 =====================

    public static final class VoltagePoint {
        public final int waypointId;
        public final double distanceM;
        public final double timeSec;
        public final double voltage;

        public VoltagePoint(int waypointId, double distanceM, double timeSec, double voltage) {
            this.waypointId = waypointId;
            this.distanceM = distanceM;
            this.timeSec = timeSec;
            this.voltage = voltage;
        }
    }

    public static final class SegmentWindInfo {
        public final int fromId;
        public final int toId;
        public final double distanceM;
        public final double bearingDeg;
        public final double effectiveWindMps;
        public final String windType; // "顺风" / "逆风" / "侧风"

        public SegmentWindInfo(int fromId, int toId, double distanceM,
                               double bearingDeg, double effectiveWindMps) {
            this.fromId = fromId;
            this.toId = toId;
            this.distanceM = distanceM;
            this.bearingDeg = bearingDeg;
            this.effectiveWindMps = effectiveWindMps;
            if (effectiveWindMps > 0.5) {
                this.windType = "顺风";
            } else if (effectiveWindMps < -0.5) {
                this.windType = "逆风";
            } else {
                this.windType = "侧风";
            }
        }
    }

    public static final class PredictionResult {
        public final List<VoltagePoint> curve;
        public final List<SegmentWindInfo> windAnalysis;
        public final double totalDistanceM;
        public final double estimatedTimeSec;
        public final double minVoltage;
        public final double finalVoltage;
        public final boolean feasible;

        public PredictionResult(List<VoltagePoint> curve, List<SegmentWindInfo> windAnalysis,
                                double totalDistanceM, double estimatedTimeSec,
                                double minVoltage, double finalVoltage, boolean feasible) {
            this.curve = curve;
            this.windAnalysis = windAnalysis;
            this.totalDistanceM = totalDistanceM;
            this.estimatedTimeSec = estimatedTimeSec;
            this.minVoltage = minVoltage;
            this.finalVoltage = finalVoltage;
            this.feasible = feasible;
        }
    }

    // ===================== 合成数据生成与训练 =====================

    private static double[] train() {
        Random rnd = new Random(42);
        double[][] X = new double[TRAINING_SAMPLES][NUM_FEATURES];
        double[] y = new double[TRAINING_SAMPLES];

        for (int i = 0; i < TRAINING_SAMPLES; i++) {
            double distance  = rnd.nextDouble() * 2000;          // 0-2000 m
            double payload   = rnd.nextDouble() * 300;            // 0-300 g
            double effWind   = (rnd.nextDouble() * 2 - 1) * 10;  // -10 ~ +10 m/s
            double stayTime  = rnd.nextDouble() * 300;           // 0-300 s
            double altitude  = rnd.nextDouble() * 100;           // 0-100 m

            double voltageDrop = COEF_DISTANCE * distance
                    + COEF_PAYLOAD * payload
                    + COEF_WIND * effWind
                    + COEF_STAY * stayTime
                    + COEF_ALTITUDE * altitude;

            double noise = voltageDrop * NOISE_RATIO * (rnd.nextGaussian());
            voltageDrop = Math.max(0, voltageDrop + noise);

            X[i][0] = distance;
            X[i][1] = payload;
            X[i][2] = effWind;
            X[i][3] = stayTime;
            X[i][4] = altitude;
            y[i] = voltageDrop;
        }
        return LinearRegression.fit(X, y);
    }

    // ===================== 方位角计算 =====================

    /**
     * 计算从 wp1 到 wp2 的方位角（度，0=正北，顺时针）。
     */
    public static double bearing(Waypoint wp1, Waypoint wp2) {
        double lat1 = Math.toRadians(wp1.getLat());
        double lat2 = Math.toRadians(wp2.getLat());
        double dLon = Math.toRadians(wp2.getLon() - wp1.getLon());
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }

    /**
     * 计算有效风力分量。
     * @param pathBearingDeg  路径方位角（度，0=北）
     * @param windFromDirDeg  风从此方向吹来（度，0=北）
     * @param windSpeed       风速（m/s）
     * @return 正值=顺风，负值=逆风
     */
    public static double effectiveWind(double pathBearingDeg, double windFromDirDeg, double windSpeed) {
        double angleDiff = Math.toRadians(pathBearingDeg - windFromDirDeg);
        return -windSpeed * Math.cos(angleDiff);
    }

    // ===================== 预测方法 =====================

    /**
     * 预测电池电压曲线。
     *
     * @param waypoints       航点列表（至少 2 个）
     * @param payloadGrams    负载重量（克）
     * @param windSpeed       风速（m/s）
     * @param windFromDirDeg  风向（度，0=北，风从此方向吹来）
     * @return 预测结果，航点不足时返回 null
     */
    public static PredictionResult predict(List<Waypoint> waypoints,
                                          double payloadGrams,
                                          double windSpeed,
                                          int windFromDirDeg) {
        if (waypoints == null || waypoints.size() < 2) {
            return null;
        }

        List<Waypoint> wps = new ArrayList<>(waypoints);
        int n = wps.size();

        // 逐段计算距离、方位角、有效风速
        double[] segDistance = new double[n - 1];
        double[] segBearing = new double[n - 1];
        double[] segEffWind = new double[n - 1];
        double[] segTime = new double[n - 1];

        double totalDistance = 0;
        double totalTime = 0;

        List<SegmentWindInfo> windAnalysis = new ArrayList<>(n - 1);

        for (int i = 0; i < n - 1; i++) {
            Waypoint a = wps.get(i);
            Waypoint b = wps.get(i + 1);
            double dist = a.distanceTo(b);
            double brg = bearing(a, b);
            double ew = effectiveWind(brg, windFromDirDeg, windSpeed);
            double segT = dist / CRUISE_SPEED_MPS;

            segDistance[i] = dist;
            segBearing[i] = brg;
            segEffWind[i] = ew;
            segTime[i] = segT;

            totalDistance += dist;
            totalTime += segT;

            windAnalysis.add(new SegmentWindInfo(a.getId(), b.getId(), dist, brg, ew));
        }

        // 计算每个航点处的累计值并预测电压
        List<VoltagePoint> curve = new ArrayList<>(n);
        double minVoltage = START_VOLTAGE;

        // 起点（航点 0）
        curve.add(new VoltagePoint(wps.get(0).getId(), 0, 0, START_VOLTAGE));

        double cumDistance = 0;
        double cumStayTime = 0;
        double cumTime = 0;
        double weightedEffWindSum = 0;
        double weightedAltSum = 0;
        double weightedCount = 0;

        for (int i = 1; i < n; i++) {
            int segIdx = i - 1;
            cumDistance += segDistance[segIdx];
            cumTime += segTime[segIdx];
            cumStayTime += wps.get(i).getStaySec();
            cumTime += wps.get(i).getStaySec();

            // 加权平均有效风速（按段距离加权）
            weightedEffWindSum += segEffWind[segIdx] * segDistance[segIdx];
            weightedAltSum += wps.get(segIdx).getAltM() * segDistance[segIdx];
            weightedCount += segDistance[segIdx];

            double avgEffWind = weightedCount > 0 ? weightedEffWindSum / weightedCount : 0;
            double avgAlt = weightedCount > 0 ? weightedAltSum / weightedCount : 0;

            double[] features = {
                cumDistance,
                payloadGrams,
                avgEffWind,
                cumStayTime,
                avgAlt
            };

            double voltageDrop = LinearRegression.predict(features, beta) * firmwareAdjustFactor;
            double voltage = START_VOLTAGE - voltageDrop;
            if (voltage < 0) voltage = 0;

            curve.add(new VoltagePoint(wps.get(i).getId(), cumDistance, cumTime, voltage));
            if (voltage < minVoltage) {
                minVoltage = voltage;
            }
        }

        double finalVoltage = curve.get(curve.size() - 1).voltage;
        boolean feasible = minVoltage > LOCK_VOLTAGE;

        return new PredictionResult(curve, windAnalysis, totalDistance, totalTime,
                minVoltage, finalVoltage, feasible);
    }

    /**
     * 设置固件调整因子（预留接口，默认 1.0）。
     */
    public static void setFirmwareAdjustFactor(double factor) {
        firmwareAdjustFactor = factor;
    }

    /**
     * 获取训练好的模型系数（含截距项）。
     */
    public static double[] getBeta() {
        return beta.clone();
    }
}
