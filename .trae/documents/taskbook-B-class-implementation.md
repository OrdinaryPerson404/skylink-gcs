# B 类缺失项 — 详细实施方案

> 依据：《面向对象课程设计任务书》B 类缺失功能 7 项 + 已完成的差距清单（`.trae/documents/taskbook-gap-analysis.md`）
> 范围：仅 B 类（B1–B7），不涉及 A 类新类、C 类动画/越界、D/E 类整改。
> 准则：全部基于已探明的真实文件与现有模式，增量式接入，**不破坏现有验收（S8/S10/S11/S12）**；所有改动以"现有单测全绿"为准绳。

---

## 现状关键事实（探索结论）

| 项 | 现状关键点 |
|---|---|
| 航点模型 | `PlanPage` 用 `Map<Integer,double[]> waypoints`（值= `[lat,lng]`），JSON 存取，无高度/停留/动作/优先级 |
| 飞行模式 | `FlyPage` 仅 ACRO/STAB（固件模式按钮）；任务书"巡航/悬停/拍照"指**航点动作指令**，非固件模式 |
| 距离计算 | `PlanPage.haversineM(lat1,lng1,lat2,lng2)` 已有（私有静态） |
| 地图动画能力 | `MapView` 已有航点连线绘制、无人机三角标记（`draw()` 内）、`AnimationTimer pulse`；可扩展预览播放 |
| 波形采样 | `DataPage.SAMPLE_MS=250` 固定（`sampler` Timeline 用 `Duration.millis(SAMPLE_MS)`） |
| 日志列表 | `DataPage.refreshSessions()` 列出所有 `session_<ts>.skylog` 文件名，无筛选 |
| 日志元数据 | `FlightRecorder` 首行 JSON：`{version,start,count,reason}`，**无型号字段** |
| 低电量 | `LiveVehicle.batteryPct`（1S 3.3~4.2V 线性映射）；`FlyPage.refreshRange()` 已监听 batteryPct |
| 预警样式 | `app.css` 已有 `.status-val.danger`/`.chip.danger`/`.warn-strip`，**无闪烁动画** |
| 设置面板 | `SetupPage` 左侧分组 + 右侧面板模式（`ITEMS` 列表 + `buildSection` switch） |

---

## 实施顺序（建议 B1→B2 合并，然后 B4→B3→B7→B6→B5）

---

### B1+B2：航点高级属性（高度/停留/优先级）+ 航点动作（巡航/悬停/拍照）

**目标**：航点从 `[lat,lng]` 扩展为含 **高度 `alt`、停留时间 `staySec`、动作 `action`（巡航/悬停/拍照）、优先级 `priority`** 的完整模型；规划面板可编辑这些属性。

**涉及文件**
1. `src/main/java/com/cherglow/gcs/model/Waypoint.java` —— **新增实体类**（任务书点名语义）
2. `src/main/java/com/cherglow/gcs/ui/pages/PlanPage.java` —— 数据结构与 UI
3. `src/main/java/com/cherglow/gcs/ui/map/MapView.java` —— `addWaypoint` 保持 lat/lng 兼容（地图只画坐标）

**具体改动**

a) **新建 `model/Waypoint.java`**：
```java
public final class Waypoint {
    private final int id;
    private double lat, lon;
    private double altM = 0;
    private int staySec = 0;          // 停留时间（秒）
    private Action action = Action.CRUISE;   // 巡航/悬停/拍照
    private int priority = 0;         // 数值越小越靠前（或按任务书自定义语义）

    public enum Action { CRUISE("巡航"), HOLD("悬停"), PHOTO("拍照"); ... }
    // getters/setters；distanceTo(Waypoint o) 用 haversine
    public double distanceTo(Waypoint o) { /* 复用 haversine 公式 */ }
}
```

b) **`PlanPage`**：
- 字段由 `Map<Integer,double[]> waypoints` → `LinkedHashMap<Integer, Waypoint> waypoints`（保留 id 主键，最小扰动）。
- 点击地图/中心加航点处：`waypoints.put(id, new Waypoint(id, lat, lng))`（默认 alt=0/stay=0/CRUISE/priority=0）。
- 拖拽回调：`map.setOnWaypointMoved((id,ll)->{ wp.setLat(ll[0]); wp.setLon(ll[1]); ... })`（保持坐标同步）。
- `refreshPanel()` 每个航点项：在现有坐标后追加四个可编辑控件：
  - 高度：`TextField`（米，默认 0）
  - 停留：`TextField`（秒，默认 0）
  - 动作：`ComboBox<String>`（`CRUISE/HOLD/PHOTO`，复用现有 `.dark-combo` 样式）
  - 优先级：`TextField`（整数）
  - 均绑定 `Waypoint` 属性并 `markDirty()`。
- `saveMission()` JSON：每个航点输出 `{lat,lon,alt,stay,action,priority}` 对象数组（替换现有 `[lat,lng]`）。
- `loadMission()`：兼容旧 `[lat,lng]`（缺字段走默认值）+ 新对象格式解析。
- `updateStats()` 距离计算改用 `waypoint.distanceTo()`。

c) **`MapView`**：`addWaypoint(int,double,double)`、`hitWaypoint`、航点绘制均不依赖 Waypoint 高级字段，**无需改动**（地图仅坐标）。

**验证**：新增 `WaypointTest`（distanceTo 精度 + 默认/设置值）；`PlanPage` 手测：加航点→填高度/动作/优先级→保存 JSON→重载数据一致；`mvn -q test` 全绿。

---

### B4：冲突检测（航点距离过近 / 超出航程）

**目标**：规划时检测①相邻/任意航点间距离过近（< 阈值，如 2m）②总航程超过无人机最大航程，超限时 Toast 警告 + 面板红点提示。

**涉及文件**
- `src/main/java/com/cherglow/gcs/ui/pages/PlanPage.java`
- `src/main/java/com/cherglow/gcs/model/Waypoint.java`（提供 `distanceTo`）
- `src/main/resources/com/cherglow/gcs/app.css`（新增 `.conflict` 警示样式，复用 `.warn-strip` 或新类）

**具体改动**
- 新增常量：`MIN_WP_GAP_M = 2.0`（相邻最小间距）、`MAX_RANGE_KM = /* 取自 Drone/配置，默认 e.g. 2.0 */`。
- 新方法 `List<String> checkConflicts()`：遍历航点，相邻距离 < 2m → 记录"#i-#j 距离过近"；总路径长度 > 最大航程 → 记录"预计航程 X km 超出 Y km"。
- 在 `updateStats()` 末尾调用，超限则在面板距离行加 `.conflict`（红）样式 + 一次 `Toast.show(警告, WARNING)`（可加防抖，避免每次移动都弹）。
- 最大航程来源：任务书要求"超出无人机航程"，现无航程参数。**决策**：用 `Waypoint`/`PlanPage` 内固定默认 `MAX_RANGE_KM=2.0` 常量（可后续接 Drone 参数），在注释与文档注明可配。

**验证**：手测：两航点紧邻放置 → 出现冲突警告；添加足够多航点使总距超 2km → 超航程警告。Unit 逻辑若抽出可加 `ConflictCheckerTest`（建议抽出纯逻辑便于单测）。

---

### B3：航点序列预览模拟动画

**目标**：在地图上沿航点顺序播放"飞机标记移动"的模拟动画，完成后回到地图交互态。

**涉及文件**
- `src/main/java/com/cherglow/gcs/ui/map/MapView.java` —— 加预览动画状态与绘制
- `src/main/java/com/cherglow/gcs/ui/pages/PlanPage.java` —— 工具行加"预览路径"按钮

**具体改动**
- `MapView`：
  - 新增字段 `private double[] previewPos;`（当前动画点位）、`private boolean previewPlaying;`、`private final List<double[]> previewPath = new ArrayList<>();`。
  - 新方法 `void playPreview()`：按航点顺序（含编号）逐点推进，用现有 `pulse` AnimationTimer 在 `draw()` 里把当前 `previewPos` 画成橙色无人机标记；点间按固定时长（如每段 400ms）线性插值。
  - `stopPreview()`：清空动画态。现有 `setVehiclePosition`/瓦片交互时若播放则先停。
  - `draw()` 增：若 `previewPlaying` 且 `previewPos != null`，在地图上方绘制移动的橙色圆点 + 序号。
- `PlanPage` 工具行：在"适配视图"旁加 `softBtn("预览路径")` → `map.playPreview()`（航点≥2 才启用）。
- 复用现有 cx/cv 换算（`lngToPx/latToPx`）与已有点的 marker 绘制逻辑。

**验证**：手测：加 ≥2 航点 → 点"预览路径" → 橙色标记沿航点顺序动画移动，可被点击/拖拽打断。无单测（动画主 UI）。

---

### B7：低电量红色闪烁预警

**目标**：电量低于阈值（如 20%）时，飞行页 HUD 电池相关项（剩余航时）红色闪烁提示。

**涉及文件**
- `src/main/resources/com/cherglow/gcs/app.css` —— 新增闪烁动画与样式类
- `src/main/java/com/cherglow/gcs/ui/pages/FlyPage.java` —— 绑定与触发
- `src/main/resources/com/cherglow/gcs/theme-light.css` ——（如需浅色场景一致性，通常无需）

**具体改动**
- `app.css` 追加：
```css
@keyframes low-batt-blink {
    from { -fx-text-fill: #ef4444; }
    50%  { -fx-text-fill: rgba(239,68,68,0.15); }
    to   { -fx-text-fill: #ef4444; }
}
.low-batt-blink { -fx-animation: low-batt-blink 0.8s infinite; -fx-text-fill: #ef4444; }
```
- `FlyPage`：新增阈值 `LOW_BATT_PCT = 20.0` 与字段 `Label`（可复用 `hudEstTime` 或加 `Label lowBattFlag`）。
  - 在 `refreshRange()` 内：`boolean low = batteryPct != NaN && batteryPct < LOW_BATT_PCT;`
  - `lowBattFlag`: 低电量时 `getStyleClass().add("low-batt-blink")` + 显示"⚠ 电量低"；恢复时移除样式并隐藏。
  - 绑定：沿用现有 `lv.batteryPct.addListener`。

**验证**：手测（注入低 volt 或 mock）：`batteryPct<20` → 红字闪烁；回升 → 停闪。如需单测可抽 `isLowBattery(pct, threshold)` 纯函数测试。

---

### B6：数据采样间隔设置（1–10 秒）

**目标**：波形监视器采样间隔从固定 250ms 改为**可配置 1–10 秒**（任务书要求设置项），供用户在设置面板选择。

**涉及文件**
- `src/main/java/com/cherglow/gcs/ui/pages/DataPage.java` —— 采样间隔可调
- `src/main/java/com/cherglow/gcs/ui/pages/SetupPage.java` ——"设置"分组加"数据采样间隔"项（或 DataPage 工具栏加下拉）

**具体改动**
- `DataPage`：
  - 将 `SAMPLE_MS` 由常量改为**可变字段** `private long sampleMs = 250;`，`sampler` 用 `Duration.millis(sampleMs)`。
  - 新方法 `setSampleMs(long ms)`：重建采样 Timeline（`sampler.stop(); sampler = new Timeline(...); sampler.play()`），并提示用户。
  - 工具栏在时间窗按钮旁加 `ComboBox<Integer>`（`[1,2,3,5,10]` 秒，复用 `.dark-combo`），选中即 `setSampleMs(v*1000)`。
- `SetupPage`：在 `ITEMS` "设置"组加一项 `new SectionItem("sampling","设置","数据采样")`，`buildSection` 加分支，卡片放下拉说明 "波形采样间隔 1–10 秒"（可选，避免重复入口——**决策**：以 DataPage 工具栏为主入口，SetupPage 不再重复加，降低冗余）。
- 注意：任务书"1–10 秒"指**采样间隔**，与现有 250ms（4Hz 高采样）冲突属教学要求；保留 250ms 为默认，下拉仅按任务书暴露 1–10s 配置。在注释/文档注明。

**验证**：手测：下拉选 5s → 采样计数增速变慢至 2×/秒左右；选 10s → 更慢。无专门单测（Timeline 主 UI）。

---

### B5：飞行日志按日期 / 型号筛选

**目标**：飞行日志列表可按**日期范围**、**无人机型号**筛选。

**涉及文件**
- `src/main/java/com/cherglow/gcs/core/FlightRecorder.java` —— 会话元数据加型号、暴露日期
- `src/main/java/com/cherglow/gcs/ui/pages/DataPage.java` —— 列表筛选 UI

**具体改动**
- `FlightRecorder`：
  - 写会话时元数据 JSON 追加 `"model":"..."`（型号来源：可从 `LiveVehicle` 型号/`chip` 或默认 `"SkyLink"`；**决策**：新增 `model` 字段，默认空串，为保持多功能扩展点）。
  - `Session` record 增 `String model`；`load()` 解析 `"model"`；`sessions()` 返回的可附带文件名解析出日期（文件名 `session_<ts>` 即时间戳）。
  - 新方法 `static List<Path> sessionsFiltered(String dateFilter, String modelFilter)`（或由 UI 过滤）。
- `DataPage.logPane()`：
  - 左侧列表头部加两个筛选控件：`ComboBox<String>`（型号，从已加载会话去重；含"全部"）+ `ComboBox`/`DatePicker`（日期，按文件名日期去重；含"全部"）。
  - `refreshSessions()` 应用两个过滤器生成显示列表。
  - 交互保持：点选记录 → `drawLog()` 回放（不改现有逻辑）。

**验证**：手测：有 ≥2 个不同日期/型号的会话 → 按型号筛选只显示对应项，按日期筛选只显示当日项。"全部"恢复。`FlightRecorder` 元数据单测补 `model` 字段读写断言。

---

## 验证总则

1. `mvn -q test`：现有 50 + 新增（WaypointTest、ConflictCheckerTest、FlightRecorderModelTest 等）全绿。
2. 逐项手测边界如上表（B1 保存/重载一致、B2 动作/优先级编辑、B3 动画、B4 冲突、B5 筛选、B6 采样、B7 闪烁）。
3. 不回归：PlanPage JSON 旧格式兼容加载、DataPage 波形/日志原功能、FlyPage HUD 原字段仍显示。

## 风险与缓解

| 风险 | 缓解 |
|---|---|
| PlanPage 数据结构重构波及拖拽/保存/加载 | 保留 id 主键 `LinkedHashMap`，JSON 兼容旧 `[lat,lng]`，单测覆盖 |
| 低电量阈值/最大航程为硬编码常量 | 常量集中定义 + 注释可配，不引入外部配置复杂度 |
| 动画/闪烁与现有 `AnimationTimer pulse` 交互冲突 | 预览播放仅在未拖拽/未 GNSS 跟随时生效，停止条件明确 |

## 建议里程碑（可按需拆分）

- **M1（本轮核心）**：B1+B2 航点高级模型 + B4 冲突检测 + B3 预览动画（三者共享 Waypoint/PlanPage，一起做最顺）
- **M2**：B5 日志筛选 + B6 采样间隔（均属"数据/设置"收尾）
- **M3**：B7 低电量闪烁（独立简单，CSS + FlyPage 绑定）