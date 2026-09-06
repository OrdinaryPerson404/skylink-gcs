package com.cherglow.gcs.ui.setup;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CF-Drone 参数字典：参数名 → 中文描述（依据真机 p 全量清单与固件语义）。
 * 未收录参数由调用方回退通用渲染。
 */
public final class ParamDict {

    public record Info(String desc, double min, double max) {
    }

    private static final Map<String, Info> DICT = new LinkedHashMap<>();

    private static void put(String name, String desc, double min, double max) {
        DICT.put(name, new Info(desc, min, max));
    }

    static {
        // ---- 姿态环（外环，角度控制） ----
        put("CTL_P_P", "俯仰外环 P", 0, 20);
        put("CTL_P_I", "俯仰外环 I", 0, 10);
        put("CTL_P_D", "俯仰外环 D", 0, 5);
        put("CTL_P_WU", "俯仰外环抗饱和", 0, 1);
        put("CTL_R_P", "横滚外环 P", 0, 20);
        put("CTL_R_I", "横滚外环 I", 0, 10);
        put("CTL_R_D", "横滚外环 D", 0, 5);
        put("CTL_R_WU", "横滚外环抗饱和", 0, 1);
        put("CTL_Y_P", "偏航外环 P", 0, 20);
        put("CTL_TILT_MAX", "最大倾斜角", 0, 1.5708);
        put("CTL_TRIM_ROLL", "横滚微调", -0.2, 0.2);
        put("CTL_TRIM_PITCH", "俯仰微调", -0.2, 0.2);
        put("CTL_STICK_DZ", "摇杆死区", 0, 0.3);

        // ---- 角速度环（内环） ----
        put("CTL_P_RATE_P", "俯仰角速度 P", 0, 1);
        put("CTL_P_RATE_I", "俯仰角速度 I", 0, 2);
        put("CTL_P_RATE_D", "俯仰角速度 D", 0, 0.1);
        put("CTL_P_RATE_WU", "俯仰角速度抗饱和", 0, 1);
        put("CTL_P_RATE_D_HZ", "俯仰角速度 D 滤波", 10, 200);
        put("CTL_R_RATE_P", "横滚角速度 P", 0, 1);
        put("CTL_R_RATE_I", "横滚角速度 I", 0, 2);
        put("CTL_R_RATE_D", "横滚角速度 D", 0, 0.1);
        put("CTL_R_RATE_WU", "横滚角速度抗饱和", 0, 1);
        put("CTL_R_RATE_D_HZ", "横滚角速度 D 滤波", 10, 200);
        put("CTL_Y_RATE_P", "偏航角速度 P", 0, 2);
        put("CTL_Y_RATE_I", "偏航角速度 I", 0, 1);
        put("CTL_Y_RATE_D", "偏航角速度 D", 0, 0.1);
        put("CTL_P_RATE_MAX", "俯仰角速度上限", 0, 12.6);
        put("CTL_R_RATE_MAX", "横滚角速度上限", 0, 12.6);
        put("CTL_Y_RATE_MAX", "偏航角速度上限", 0, 12.6);

        // ---- 高度控制 ----
        put("ALT_ALT_P", "高度环 P", 0, 5);
        put("ALT_VEL_SLEW", "垂直速度斜率", 0, 10);
        put("ALT_VEL_P", "垂直速度 P", 0, 2);
        put("ALT_VEL_I", "垂直速度 I", 0, 1);
        put("ALT_VEL_D", "垂直速度 D", 0, 0.1);
        put("ALT_VEL_WU", "垂直速度抗饱和", 0, 1);
        put("ALT_VEL_D_HZ", "垂直速度 D 滤波", 10, 200);
        put("ALT_VEL_MAX", "垂直速度上限", 0, 5);
        put("ALT_HOVER_THR", "悬停油门", 0, 1);
        put("ALT_HOVER_TAU", "悬停油门学习时间", 1, 60);
        put("ALT_EST_KP", "高度估计 Kp", 0, 10);
        put("ALT_EST_KV", "高度估计 Kv", 0, 20);
        put("ALT_EST_KB", "气压计融合 Kb", 0, 2);
        put("ALT_EST_KB_M", "气压计融合 Kb-M", 0, 10);
        put("ALT_EST_KV_B", "气压计融合 Kv-B", 0, 5);
        put("ALT_EST_KB_B", "气压计融合 Kb-B", 0, 1);
        put("ALT_BARO_INNO", "气压计新息门限", 0, 5);
        put("ALT_LAND_BLEED", "降落推力衰减", 0, 1);
        put("ALT_VELZ_TAU", "垂直速度滤波", 0, 2);
        put("BARO_LPF_HZ", "气压计低通滤波", 0.1, 20);

        // ---- 位置控制（光流/定点） ----
        put("PH_VEL_P", "定点速度 P", 0, 2);
        put("PH_VEL_I", "定点速度 I", 0, 0.5);
        put("PH_VEL_D", "定点速度 D", 0, 0.1);
        put("PH_VEL_WU", "定点速度抗饱和", 0, 1);
        put("PH_VEL_MAX", "定点速度上限", 0, 5);
        put("PH_POS_P", "定点位置 P", 0, 5);
        put("PH_ACC_MAX", "定点最大加速度", 0, 10);
        put("PH_ACC_STD", "定点加速度标准差", 0, 2);
        put("PH_VEL_EST_KB", "定点速度估计系数", 0, 5);

        // ---- IMU / 估计 ----
        put("IMU_ROT_ROLL", "IMU 安装旋转 横滚", -3.1416, 3.1416);
        put("IMU_ROT_PITCH", "IMU 安装旋转 俯仰", -3.1416, 3.1416);
        put("IMU_ROT_YAW", "IMU 安装旋转 偏航", -3.1416, 3.1416);
        put("GYRO_BIAS_HZ", "陀螺零偏学习频率", 0, 1);
        put("IMU_ACC_BIAS_X", "加计零偏 X", -2, 2);
        put("IMU_ACC_BIAS_Y", "加计零偏 Y", -2, 2);
        put("IMU_ACC_BIAS_Z", "加计零偏 Z", -2, 2);
        put("IMU_ACC_SCALE_X", "加计缩放 X", 0.9, 1.1);
        put("IMU_ACC_SCALE_Y", "加计缩放 Y", 0.9, 1.1);
        put("IMU_ACC_SCALE_Z", "加计缩放 Z", 0.9, 1.1);
        put("MAHONY_KP", "姿态融合 Kp", 0, 5);
        put("MAHONY_KI", "姿态融合 Ki", 0, 1);
        put("EST_RATE_LPF_HZ", "角速度低通滤波", 5, 200);
        put("EST_ACC_LPF_HZ", "加速度低通滤波", 5, 100);
        put("EST_LVL_BIAS_GAIN", "水平零偏增益", 0, 1);
        put("EST_LVL_GATE_THR", "水平门限", 0, 1);

        // ---- 电机 ----
        put("MOT_PIN_FL", "前左电机引脚", 0, 48);
        put("MOT_PIN_FR", "前右电机引脚", 0, 48);
        put("MOT_PIN_RL", "后左电机引脚", 0, 48);
        put("MOT_PIN_RR", "后右电机引脚", 0, 48);
        put("MOT_PWM_FREQ", "PWM 频率 Hz", 100, 50000);
        put("MOT_PWM_RES", "PWM 分辨率 bit", 8, 16);
        put("MOT_PWM_MIN", "PWM 最小值", 0, 65535);
        put("MOT_PWM_MAX", "PWM 最大值", -1, 65535);
        put("MOT_PWM_STOP", "停转 PWM", 0, 65535);
        put("MOT_THR_MIN", "油门下限", 0, 0.5);
        put("MOT_THR_MAX", "油门上限", 0.5, 1);

        // ---- 遥控 ----
        put("RC_ROLL", "横滚通道映射", -1, 16);
        put("RC_PITCH", "俯仰通道映射", -1, 16);
        put("RC_THROTTLE", "油门通道映射", -1, 16);
        put("RC_YAW", "偏航通道映射", -1, 16);
        put("RC_MODE", "模式通道映射", -1, 16);
        put("RC_RX_PIN", "RC 接收引脚", 0, 48);
        put("RC_TX_PIN", "RC 发送引脚", -1, 48);
        put("RC_PROTOCOL", "RC 协议", 0, 5);
        put("RC_BAUD", "RC 波特率", 9600, 1000000);
        for (int i = 0; i <= 7; i++) {
            put("RC_ZERO_" + i, "通道 " + (i + 1) + " 中点", 800, 2200);
            put("RC_MAX_" + i, "通道 " + (i + 1) + " 行程", 800, 2200);
        }

        // ---- WiFi / MAVLink ----
        put("WIFI_MODE", "WiFi 模式（1=AP）", 0, 2);
        put("WIFI_LOC_PORT", "本地 UDP 端口", 1000, 65535);
        put("WIFI_REM_PORT", "远程 UDP 端口", 1000, 65535);
        put("MAV_SYS_ID", "MAVLink 系统编号", 1, 255);
        put("MAV_RATE_SLOW", "慢速遥测 Hz", 1, 50);
        put("MAV_RATE_FAST", "快速遥测 Hz", 1, 100);

        // ---- 安全 / 其它 ----
        put("SF_RC_LOSS_TIME", "失控保护时间 s", 0.5, 10);
        put("SF_DESCEND_TIME", "自动降落时间 s", 1, 60);
        put("ARM_CHK_LEVEL", "解锁检查等级", 0, 3);
        put("FLOW_ROT_YAW", "光流安装偏航", -3.1416, 3.1416);
    }

    public static Info info(String name) {
        return DICT.get(name);
    }

    public static String desc(String name) {
        Info i = DICT.get(name);
        return i != null ? i.desc() : "参数 " + name;
    }

    /** 数值格式化：整数去小数点，其余保留有效位 */
    public static String fmt(double v) {
        if (v != v) {
            return "nan";
        }
        if (v == Math.rint(v) && Math.abs(v) < 1e9) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    private ParamDict() {
    }
}
