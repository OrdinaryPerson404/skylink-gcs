# 航点起点/终点模式与贪婪最短路径规划 — 实施计划

## 概述

在现有固定顺序航点添加方式基础上，新增"起点-终点"模式：支持设置唯一起点和终点航点，在两者之间添加任意中间航点，并通过自行实现的贪婪最近邻算法优化中间航点访问顺序，使总飞行距离最小化。两种模式可通过工具栏切换按钮并存。

## 当前状态分析

### 现有架构

| 文件 | 角色 | 关键结构 |
|------|------|----------|
| `model/Waypoint.java` | 航点实体 | `id, lat, lon, altM, staySec, action, priority`；已有 `distanceTo()` / `haversineM()` |
| `ui/pages/PlanPage.java` | 规划页 | `LinkedHashMap<Integer, Waypoint> waypoints`；地图点击添加航点；`refreshPanel()` 同步列表+AppState；`updateStats()` 计算总距离和冲突检测 |
| `ui/map/MapView.java` | 地图引擎 | `Map<Integer, double[]> waypoints`；`addWaypoint/removeWaypoint/clearWaypoints`；`draw()` 按LinkedHashMap顺序绘制连线 |
| `core/AppState.java` | 全局状态 | `missionWaypoints` ObjectProperty，PlanPage写入，FlyPage读取 |
| `src/test/.../WaypointTest.java` | 测试参考 | JUnit 5，haversine距离验证模式 |
| `app.css` | 样式 | `.plan-panel`, `.wp-item`, `.wp-badge-sm`, `.btn-soft`, `.conflict` 等 |

### 当前航点添加方式

- 地图点击 → `nextId++` → 按固定顺序 1,2,3,... 存入 `LinkedHashMap`
- "中心加航点"按钮同理
- `updateStats()` 按 LinkedHashMap 迭代顺序累加相邻距离
- 地图 `draw()` 按同样顺序绘制路径连线

## 实施方案

### 新增文件

#### 1. `src/main/java/com/cherglow/gcs/core/PathOptimizer.java`

贪婪最近邻最短路径规划器，纯算法类，无 JavaFX 依赖。

```java
package com.cherglow.gcs.core;

import com.cherglow.gcs.model.Waypoint;
import java.util.ArrayList;
import java.util.List;

/**
 * 贪婪最近邻路径优化器。
 * 输入：起点 + 终点 + 中间航点集合
 * 输出：优化后的访问顺序（起点→航点1→…→终点）及最小总距离
 */
public class PathOptimizer {

    public static class Result {
        public final List<Waypoint> orderedWaypoints;
        public final double totalDistanceM;
        public Result(List<Waypoint> ordered, double dist) {
            this.orderedWaypoints = ordered;
            this.totalDistanceM = dist;
        }
    }

    /**
     * 贪婪最近邻算法：从起点出发，每步选择未访问的最近航点，
     * 最后回到终点。时间复杂度 O(n²)，可处理20+中间航点。
     */
    public static Result optimize(Waypoint start, Waypoint end, List<Waypoint> middles) { ... }

    /** 计算有序列表的总距离（相邻航点距离之和）。 */
    public static double totalDistance(List<Waypoint> ordered) { ... }
}
```

**算法逻辑：**
1. 当前点 = start，未访问集合 = middles 副本
2. 循环：在未访问集合中找距当前点最近的航点，加入结果列表，从未访问集合移除
3. 未访问集合空后，加入 end
4. 计算结果列表总距离

#### 2. `src/test/java/com/cherglow/gcs/core/PathOptimizerTest.java`

测试用例：
- 空中间航点：结果 = [start, end]
- 单中间航点：结果 = [start, middle, end]
- 已有序列为最优时不重排
- 随机散布的航点验证总距离 ≤ 顺序排列的距离
- 20个中间航点性能测试（断言 < 100ms）

### 修改文件

#### 3. `model/Waypoint.java` — 新增角色字段

```java
public enum Role { NONE, START, END }
private Role role = Role.NONE;
// getter/setter
```

#### 4. `ui/pages/PlanPage.java` — 核心改造

**新增字段：**
```java
private boolean optimizeMode = false;  // 贪婪优化模式开关
private Waypoint startWp = null;       // 起点航点
private Waypoint endWp = null;         // 终点航点
private final Label optimizedDistVal = new Label("—"); // 优化后距离
```

**工具栏新增：**
- "优化模式" 切换按钮（ToggleGroup 风格，与"中心加航点"等并列）
- 切换为优化模式后，地图点击行为变化：第一次点击设为起点，第二次设为终点，之后添加中间航点

**面板新增区域（在统计区下方）：**
- "起点" 行：显示起点坐标 + "清除" 按钮
- "终点" 行：显示终点坐标 + "清除" 按钮
- "优化路径" 按钮：触发贪婪算法
- "优化后距离" 统计行
- 优化结果说明文本

**refreshPanel() 修改：**
- 航点列表中，起点航点显示绿色 "S" 徽章，终点航点显示红色 "E" 徽章，中间航点显示序号徽章
- 优化模式下，列表按优化后的顺序显示（起点→优化序列→终点）

**updateStats() 修改：**
- 优化模式下，总距离使用 `PathOptimizer.totalDistance(optimizedList)` 而非顺序累加
- 显示"原始距离"和"优化后距离"对比

**地图点击逻辑修改：**
```java
map.setOnMapClick(ll -> {
    if (optimizeMode) {
        if (startWp == null) {
            startWp = new Waypoint(nextId++, ll[0], ll[1]);
            startWp.setRole(Waypoint.Role.START);
            waypoints.put(startWp.getId(), startWp);
            map.addWaypoint(startWp.getId(), ll[0], ll[1]);
        } else if (endWp == null) {
            endWp = new Waypoint(nextId++, ll[0], ll[1]);
            endWp.setRole(Waypoint.Role.END);
            waypoints.put(endWp.getId(), endWp);
            map.addWaypoint(endWp.getId(), ll[0], ll[1]);
        } else {
            int id = nextId++;
            Waypoint wp = new Waypoint(id, ll[0], ll[1]);
            waypoints.put(id, wp);
            map.addWaypoint(id, ll[0], ll[1]);
        }
        markDirty();
        refreshPanel();
    } else {
        // 现有逻辑不变
    }
});
```

**优化路径按钮事件：**
```java
optimizeBtn.setOnAction(e -> {
    if (startWp == null || endWp == null) {
        Toast.show("请先设置起点和终点", Toast.Type.WARNING);
        return;
    }
    List<Waypoint> middles = new ArrayList<>();
    for (Waypoint wp : waypoints.values()) {
        if (wp.getRole() != Waypoint.Role.START && wp.getRole() != Waypoint.Role.END) {
            middles.add(wp);
        }
    }
    PathOptimizer.Result result = PathOptimizer.optimize(startWp, endWp, middles);
    // 更新 waypoints 的 LinkedHashMap 顺序为优化后顺序
    waypoints.clear();
    map.clearWaypoints();
    for (Waypoint wp : result.orderedWaypoints) {
        waypoints.put(wp.getId(), wp);
        map.addWaypoint(wp.getId(), wp.getLat(), wp.getLon());
    }
    optimizedDistVal.setText(formatDist(result.totalDistanceM));
    markDirty();
    refreshPanel();
    Toast.show("路径优化完成，总距离 " + formatDist(result.totalDistanceM), Toast.Type.SUCCESS);
});
```

#### 5. `ui/map/MapView.java` — 起终点视觉区分

**draw() 方法修改 — 航点徽章绘制部分：**

新增方法区分航点角色：
```java
// 需要传入航点角色信息。由于 MapView 的 waypoints 是 Map<Integer, double[]>，
// 新增一个 role Map:
private final Map<Integer, String> waypointRoles = new LinkedHashMap<>();

public void setWaypointRole(int id, String role) { ... }  // "START", "END", null
```

绘制时：
- START：绿色圆形 + "S" 字母
- END：红色圆形 + "E" 字母
- 普通：现有黄色圆形 + 序号

#### 6. `core/AppState.java` — 无需修改

`missionWaypoints` 已存储 `Map<Integer, Waypoint>`，Waypoint 的新 Role 字段自动随序列化传递。

#### 7. `app.css` — 新增样式

```css
/* 起点终点徽章 */
.wp-badge-start {
    -fx-background-color: #22c55e;
    -fx-text-fill: #fff;
    -fx-font: bold 10px "Consolas, monospace";
    -fx-min-width: 18; -fx-min-height: 18;
    -fx-alignment: center;
    -fx-background-radius: 9;
}
.wp-badge-end {
    -fx-background-color: #ef4444;
    -fx-text-fill: #fff;
    -fx-font: bold 10px "Consolas, monospace";
    -fx-min-width: 18; -fx-min-height: 18;
    -fx-alignment: center;
    -fx-background-radius: 9;
}

/* 优化模式按钮激活态 */
.btn-optimize-active {
    -fx-background-color: -c-primary;
    -fx-text-fill: #161b22;
}

/* 优化结果区域 */
.optimize-section {
    -fx-background-color: -c-bg;
    -fx-border-color: -c-border;
    -fx-border-width: 1;
    -fx-background-radius: 6;
    -fx-border-radius: 6;
    -fx-padding: 8;
    -fx-spacing: 6;
}
```

#### 8. `ui/pages/FlyPage.java` — 航点列表角色显示

`refreshWpList()` 方法中，根据 `wp.getRole()` 显示不同徽章：
- START → 绿色 "S" 徽章
- END → 红色 "E" 徽章
- NONE → 现有序号徽章

## 数据流

```
固定模式（现有）:
  地图点击 → waypoints.put(id, wp) → refreshPanel() → updateStats() → AppState

优化模式（新增）:
  地图点击 → 判断角色(start/end/middle) → waypoints.put(id, wp with role)
  → 点击"优化路径" → PathOptimizer.optimize(start, end, middles)
  → 重排 waypoints LinkedHashMap 顺序 → map.clearWaypoints + 逐个addWaypoint
  → refreshPanel() → updateStats() → AppState
  → FlyPage 监听 → refreshWpList() 同步显示
```

## 设计决策

1. **两种模式并存而非替换**：通过工具栏切换按钮切换，默认仍为固定顺序模式，确保向后兼容
2. **Waypoint.Role 枚举而非整数标记**：语义清晰，类型安全
3. **PathOptimizer 为静态工具类**：纯算法无状态，便于测试和复用
4. **优化后重排 LinkedHashMap 顺序**：MapView 的 draw() 天然按 Map 迭代顺序绘制连线，无需额外修改绘制逻辑
5. **起点/终点唯一性**：在地图点击逻辑中用 null 检查保证只能设置一个起点和一个终点
6. **保存/加载兼容**：JSON 序列化新增 role 字段，加载时如无 role 字段默认为 NONE

## 验证步骤

1. **编译验证**：`mvn compile` 无错误
2. **单元测试**：`mvn test` 全部通过，包括新增的 PathOptimizerTest
3. **功能验证**：
   - 启动应用 → 规划页 → 切换到"优化模式"
   - 地图点击设置起点（绿色S标记）
   - 地图点击设置终点（红色E标记）
   - 地图点击添加3-5个中间航点
   - 点击"优化路径"按钮 → 航点列表重排 → 地图连线更新 → 距离显示更新
   - 切换到飞行页 → 验证航点同步显示（含角色徽章）
   - 切换回固定模式 → 按现有方式添加航点 → 验证功能正常
4. **边界测试**：
   - 优化模式下未设起点就点击优化 → Toast 警告
   - 优化模式下未设终点就点击优化 → Toast 警告
   - 只有起点和终点无中间航点 → 直接连线，距离正常计算
   - 20个中间航点 → 验证响应时间 < 1s
