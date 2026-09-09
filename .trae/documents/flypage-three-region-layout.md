# 飞行界面三区域布局优化方案

## 概述

将 FlyPage 从当前的「全幅地图 + 叠加层」布局重构为三区域划分设计：
- **左侧**：控制面板（航点列表 + 任务状态）
- **中央**：地图核心区（交互已存在，优化体验）
- **底部**：状态栏（连接状态 + 系统时间）

## 当前状态分析

### FlyPage 现状（`src/main/java/com/cherglow/gcs/ui/pages/FlyPage.java`）

| 区域 | 当前布局 | 问题 |
|---|---|---|
| 布局根 | `BorderPane`，仅用 `setCenter` + `setBottom` | 无左侧面板 |
| 中央 | `StackPane` 内含 MapView + HUD(右上) + ADI(左下) + banner(顶部) | 地图交互已存在但无独立区域 |
| 底部 | 系统消息条 + 指令条（arm/disarm + ACRO/STAB） | 无连接状态、无系统时间 |
| 航点 | 飞行页无航点列表，仅在 PlanPage 有 | 飞行时无法查看/编辑航点 |
| 任务状态 | 仅 banner 提示「任务已修改」 | 无进度/执行/异常信息 |

### MapView 现状（`src/main/java/com/cherglow/gcs/ui/map/MapView.java`）

| 交互 | 实现位置 | 状态 |
|---|---|---|
| 滚轮缩放（锚点） | `setOnScroll` L170-184 | **已存在**，锚定鼠标地理坐标 |
| 拖拽平移 | `setOnMouseDragged` L138-155 | **已存在**，按住左键拖动 |
| 航点拖拽 | 同上 dragWp 分支 | **已存在** |
| 点击回调 | `setOnMouseReleased` L156-169 | **已存在** |

**结论：MapView 交互已完整，无需重写，仅需在 FlyPage 中正确布局让地图占满中央区域。**

### 可复用资源

| 资源 | 来源文件 | 复用方式 |
|---|---|---|
| `Waypoint` 模型 | `model/Waypoint.java` | 直接引用 |
| 航点列表渲染逻辑 | `PlanPage.refreshPanel()` L255-336 | 提取为公共方法或简化复制 |
| `MapView` 全部 API | `ui/map/MapView.java` | FlyPage 已持有实例 |
| `AppState.connStatusProperty()` | `core/AppState.java` | 状态栏绑定 |
| `ConnectionService.get().isConnected()` | `core/ConnectionService.java` | 连接状态 |
| `LiveVehicle` 遥测属性 | `model/LiveVehicle.java` | 任务状态绑定 |
| CSS 设计令牌 | `app.css` `-c-*` 变量 | 新样式沿用 |

## 提议改动

### 改动 1：FlyPage 布局重构

**文件**: `src/main/java/com/cherglow/gcs/ui/pages/FlyPage.java`

**改什么**: 将 `BorderPane` 的 `left/center/bottom` 三个区域重新划分：

```
BorderPane:
  ├─ left   → buildLeftPanel()   (VBox, 280px 固定宽)
  ├─ center → buildCenter()      (StackPane: MapView + HUD + ADI + banner)
  └─ bottom → buildBottom()      (HBox: 状态栏 + 指令条)
```

**为什么**: 实现三区域划分，左侧控制面板独立于地图区域。

**怎么做**:

1. 新增 `buildLeftPanel()` 方法，返回 `VBox`：
   - **航点列表区**（上半）：
     - 标题行「航点列表」+ 航点数徽章
     - `ScrollPane` 内含 `VBox wpListBox`，每个航点一行：序号徽章 + 坐标 + 删除按钮
     - 底部工具行：「添加」「清空」按钮（添加=在地图中心加航点）
     - 数据源：从 `AppState` 或 PlanPage 共享的 `waypoints` Map 读取
   - **任务状态区**（下半）：
     - 标题行「任务状态」
     - 任务进度行：航点总数 / 当前航点（基于 `lv.currentWp` 如果有，否则显示「—」）
     - 执行状态行：模式徽章（绑定 `lv.modeName`）+ 解锁徽章（绑定 `lv.armed`）
     - 异常提示行：低电量预警（复用 `lowBattFlag`）+ 禁止解锁原因（绑定 `lv.armingDisabled`）
     - 航程预测行：剩余航时 + 剩余航程（复用 `hudEstTime` / `hudEstRange`）

2. 修改 `buildCenter()`：移除 HUD 中已移到左侧面板的字段（航程/纬度/经度/低电量/模式/解锁），保留核心 HUD（航向/高度/地速/爬升/横滚/俯仰）作为地图叠加层。

3. 修改 `buildBottom()`：在指令条下方新增状态栏行：
   - 左侧：连接状态指示灯（绿=已连接/红=断开）+ 端口描述（`ConnectionService.get().getDesc()`）
   - 中间：信号稳定性标识（基于 `lv.lastUpdateMs` 与当前时间差计算：<1s=强/1-3s=弱/>3s=断）
   - 右侧：系统时间（`HH:mm:ss`，每秒刷新）

4. 构造器中改为 `setLeft(buildLeftPanel())` + `setCenter(buildCenter())` + `setBottom(buildBottom())`。

### 改动 2：航点数据共享

**文件**: `src/main/java/com/cherglow/gcs/core/AppState.java`

**改什么**: 新增航点共享状态：

```java
private final ObjectProperty<Map<Integer, Waypoint>> missionWaypoints =
    new SimpleObjectProperty<>(this, "missionWaypoints", new LinkedHashMap<>());
private final IntegerProperty missionCount = new SimpleIntegerProperty(this, "missionCount", 0);
```

**为什么**: PlanPage 和 FlyPage 需要共享同一份航点数据，避免复制。

**怎么做**: 
- `AppState` 新增 `missionWaypointsProperty()` 和 `setMissionWaypoints(Map)` / `getMissionWaypoints()`
- PlanPage 在 `refreshPanel()` 末尾调用 `AppState.get().setMissionWaypoints(new LinkedHashMap<>(waypoints))`
- FlyPage `buildLeftPanel()` 的航点列表绑定 `AppState.get().missionWaypointsProperty()`，变化时刷新列表

### 改动 3：连接状态描述与信号稳定性

**文件**: `src/main/java/com/cherglow/gcs/core/ConnectionService.java`

**改什么**: 暴露连接描述字符串和上次遥测时间。

**为什么**: 状态栏需要显示端口信息和信号稳定性。

**怎么做**:
- 新增 `public String getDesc()` 返回 `desc`（已有字段，L28）
- `LiveVehicle.lastUpdateMs` 已有（L47），FlyPage 用 `System.currentTimeMillis() - lv.lastUpdateMs.get()` 计算信号稳定性

### 改动 4：系统时钟

**文件**: `src/main/java/com/cherglow/gcs/ui/pages/FlyPage.java`

**改什么**: 新增 `Timeline` 每秒刷新系统时间标签。

**为什么**: 底部状态栏需精确到秒的实时时钟。

**怎么做**:
```java
private final Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
    clockLabel.setText(LocalTime.now().format(HM));
}));
clock.setCycleCount(Timeline.INDEFINITE);
clock.play();
```

### 改动 5：CSS 样式

**文件**: `src/main/resources/com/cherglow/gcs/app.css`

**改什么**: 新增左侧面板和状态栏样式。

**怎么做**: 追加以下样式类：
- `.fly-left-panel`：左侧面板容器（背景 `-c-panel`、右边框、280px 宽）
- `.fly-wp-item`：航点列表项（紧凑行高、悬停高亮）
- `.fly-status-row`：任务状态行（标签+值水平排列）
- `.fly-conn-dot`：连接状态圆点（绿/红，8px 圆）
- `.fly-clock`：时钟文本（mono 字体、12px）
- `.fly-signal-bar`：信号强度条（3 段竖条，强/中/弱）

### 改动 6：HUD 精简

**文件**: `src/main/java/com/cherglow/gcs/ui/pages/FlyPage.java`

**改什么**: 右上 HUD 卡仅保留 6 项核心飞行数据。

**为什么**: 纬度/经度/航程/低电量/模式/解锁已移到左侧面板，HUD 需精简避免重复。

**保留**: 航向、高度、地速、爬升、横滚、俯仰（6 项）。

## 假设与决策

| 决策 | 理由 |
|---|---|
| 航点数据通过 AppState 共享而非 FlyPage 自持 | 飞行页和规划页需查看同一份任务，避免数据不一致 |
| 信号稳定性用遥测时间差代理 | 当前固件无 RSSI 字段，时间差是最实用的连接质量指标 |
| 状态栏放在指令条下方（最底部） | 用户明确要求底部状态栏，指令条保持在其上方 |
| ADI 浮窗保留在地图左下角 | 左侧面板是独立区域，ADI 仍叠在地图上不受影响 |
| 地图交互不改 | MapView 已完整实现滚轮缩放+拖拽平移，验证即可 |

## 验证步骤

1. `mvn -o test` — 全量测试不回归（56 项全绿）
2. 启动 GCS，切到「飞行」页：
   - 左侧面板可见航点列表 + 任务状态
   - 中央地图滚轮缩放流畅、左键拖拽平移正常
   - 底部状态栏显示连接状态 + 系统时间（每秒跳）
3. 在规划页添加航点 → 切到飞行页 → 左侧列表自动同步
4. 断开连接 → 状态栏指示灯变红、信号显示「断」
5. 确认 HUD 仍显示 6 项核心数据（航向/高度/地速/爬升/横滚/俯仰）
