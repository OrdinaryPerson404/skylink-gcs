package cn.edu.nuaa.gcs.comm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 飞控参数读写服务。
 *
 * CF-Drone 固件共暴露 84 个可调参数（CTL_、IMU_、EST_、MOT_、RC_、WIFI_、MAV_、SF_ 前缀），
 * 通过 MAVLink 的 PARAM_REQUEST_LIST / PARAM_VALUE 协议读取（只读，安全）。
 *
 * 写入（PARAM_SET）属于飞控配置变更，受用户硬约束“不得对无人机进行配置变更”限制，
 * 默认禁用；本服务保留写入口径，但实际是否下发由上层安全策略门控。
 *
 * 工作流程：comm.requestParamList() → 飞控逐条回传 PARAM_VALUE → onParamValue() 累积 →
 * isComplete() 为 true 后即可 getParams() 获取完整列表用于界面展示。
 */
public class ParamService {
    /** 单个参数项。 */
    public static class ParamItem {
        public final String name;
        public double value;
        public final int index;
        public final int type; // MAV_PARAM_TYPE_REAL32=9
        public boolean dirty;  // 是否已被本地修改待回写

        public ParamItem(String name, double value, int index, int type) {
            this.name = name;
            this.value = value;
            this.index = index;
            this.type = type;
        }
    }

    private final Map<Integer, ParamItem> params = new LinkedHashMap<>();
    private int totalCount = -1;
    private long lastUpdateMs = 0;

    /** 接收一条 PARAM_VALUE 遥测，累积到参数表。 */
    public void onParamValue(MavlinkParser.Telemetry t) {
        if (!t.hasParam || t.paramName == null || t.paramName.isEmpty()) return;
        params.put(t.paramIndex, new ParamItem(t.paramName, t.paramValue, t.paramIndex, t.paramType));
        if (t.paramCount > 0) totalCount = t.paramCount;
        lastUpdateMs = System.currentTimeMillis();
    }

    /** 参数总数（来自飞控 PARAM_VALUE.param_count）。 */
    public int getTotalCount() { return totalCount; }

    /** 已收到参数条数。 */
    public int getReceivedCount() { return params.size(); }

    /** 参数列表是否已完整接收。 */
    public boolean isComplete() {
        return totalCount > 0 && params.size() >= totalCount;
    }

    /** 按索引排序的参数列表。 */
    public List<ParamItem> getParams() {
        List<Integer> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        List<ParamItem> list = new ArrayList<>();
        for (Integer k : keys) list.add(params.get(k));
        return list;
    }

    /** 按名称前缀过滤参数（如 "CTL_"、"MOT_"）。 */
    public List<ParamItem> getParamsByPrefix(String prefix) {
        List<ParamItem> all = getParams();
        List<ParamItem> filtered = new ArrayList<>();
        for (ParamItem p : all) {
            if (prefix == null || prefix.isEmpty() || p.name.startsWith(prefix)) filtered.add(p);
        }
        return filtered;
    }

    /** 最近一次收到参数的时间戳（ms）。 */
    public long getLastUpdateMs() { return lastUpdateMs; }

    /** 清空已收参数（断开重连时调用）。 */
    public void clear() {
        params.clear();
        totalCount = -1;
        lastUpdateMs = 0;
    }
}
