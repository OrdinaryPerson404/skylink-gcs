# JavaFX 无人机地面站 UI 代码审查报告

**审查日期**: 2026-09-02
**审查范围**: 6 个 UI 相关文件
**严重级别**: 🔴 严重 | 🟠 高 | 🟡 中 | 🟢 低

---

## 一、严重问题（编译错误 / 运行时崩溃）

### 1. 🔴 MainController.java - 重复字段声明 `sensor`

**位置**: 第 76 行 和 第 109 行

```java
// 第 76 行
private SensorChart sensor;

// 第 109 行
private SensorChart sensor;
```

**问题**: 同一个类中声明了两次 `sensor` 字段，会导致编译错误。

**修复**: 删除第 76 行的重复声明（保留第 109 行的声明即可，因为它位于字段声明区域）。

---

### 2. 🔴 MapCanvas.java - 单击地图添加两个航点（事件重复触发）

**位置**: `onMouseReleased` (第 454-464 行) 和 `onMouseClicked` (第 466-482 行)

**问题分析**:
- `onMouseReleased`: 当鼠标未移动时调用 `clickHandler`（添加航点）
- `onMouseClicked`: 当单击且不在航点上时也调用 `clickHandler`（添加航点）
- JavaFX 中，鼠标释放后会同时触发 `MOUSE_RELEASED` 和 `MOUSE_CLICKED` 事件
- 对于完全静止的点击（`dragStartX/Y == e.getX/Y`），两个处理器都会执行，导致一次点击添加两个航点

**相关代码**:
```java
// onMouseReleased 中 (第 460-463 行)
if (e.getX() == dragStartX && e.getY() == dragStartY && clickHandler != null) {
    double[] lonLat = pixelToLonLat(e.getX(), e.getY());
    clickHandler.accept(lonLat[1], lonLat[0]);
}

// onMouseClicked 中 (第 474-480 行)
if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1 && !isDraggingWaypoint && clickHandler != null) {
    int wpIdx = findWaypointAt(e.getX(), e.getY());
    if (wpIdx < 0) {
        double[] lonLat = pixelToLonLat(e.getX(), e.getY());
        clickHandler.accept(lonLat[1], lonLat[0]);
    }
}
```

**修复**: 删除 `onMouseReleased` 中的点击处理逻辑，只保留 `onMouseClicked` 中的处理。或将 `onMouseReleased` 中的点击处理移除，因为 `onMouseClicked` 已经覆盖了该功能。

---

## 二、高级问题（功能错误 / 逻辑缺陷）

### 3. 🟠 MainController.java - `brandLabel` 字段无对应 FXML 元素

**位置**: 第 29 行

```java
@FXML private Label brandLabel;
```

**问题**: 控制器中声明了 `brandLabel`，但 FXML 中第 11 行的品牌 Label 没有 `fx:id="brandLabel"`:

```xml
<Label text="SkyLink 天链" styleClass="brand"/>
```

导致 `brandLabel` 始终为 `null`，如果后续代码尝试使用会抛出 NPE。

**修复**: 在 FXML 中添加 `fx:id="brandLabel"`，或删除控制器中未使用的字段。

---

### 4. 🟠 MainController.java - 遗留字段指向不存在的 FXML 元素

**位置**: 第 96-99 行

```java
@FXML private TableView<LogRecord> logTableView;
@FXML private DatePicker logDatePicker;
@FXML private ComboBox<String> logModelCombo;
@FXML private ComboBox<String> timeRangeCombo;
```

**问题**: 这 4 个字段在当前 FXML 布局中不存在，是旧版日志表格布局的遗留代码。虽然不会导致编译错误（JavaFX 会将它们设为 null），但：
- `initLogTable()` 方法中有 null 检查，所以不会崩溃
- 这些是死代码，增加维护成本

**建议**: 如确认不再使用日志表格视图，可删除这些字段和 `initLogTable()` 方法。

---

### 5. 🟠 CSS - 使用未定义的变量 `-fx-text-primary`

**位置**: 
- `dark.css` 第 179 行
- `light.css` 第 179 行

```css
.map-ctrl-btn { 
    -fx-text-fill: -fx-text-primary; 
    /* ... */ 
}
```

**问题**: `.root` 中定义的文本颜色变量是 `-fx-text-color`，但此处使用了 `-fx-text-primary`，该变量未定义。JavaFX 会回退到默认颜色，导致地图控制栏按钮文本颜色不正确。

**修复**: 将 `-fx-text-primary` 改为 `-fx-text-color`。

---

### 6. 🟠 MapCanvas.java - `fitWaypoints()` 不调整缩放级别

**位置**: 第 497-510 行

```java
public void fitWaypoints() {
    if (waypoints.isEmpty()) return;
    // ... 计算边界 ...
    centerLat = (minLat + maxLat) / 2;
    centerLon = (minLon + maxLon) / 2;
    offsetPixelX = 0; offsetPixelY = 0;
    draw();
}
```

**问题**: 方法只将地图中心移动到航点中心，但不调整缩放级别。如果航点分布范围很大或很小，用户仍需手动缩放才能看到所有航点。

**建议**: 根据航点范围和画布尺寸自动计算合适的 zoom 级别。

---

### 7. 🟠 SensorChart.java - `setTimeWindow()` 不重置 X 轴下界

**位置**: 第 78-82 行

```java
public void setTimeWindow(int seconds) {
    this.timeWindow = seconds;
    xAxis.setUpperBound(seconds);
    xAxis.setTickUnit(seconds / 6.0);
}
```

**问题**:
1. 只设置了上界，没有设置下界。如果曲线已经滚动（`lowerBound > 0`），切换时间窗口后 X 轴范围会错乱。
2. 已有数据不会根据新窗口大小进行裁剪。

**修复**: 添加 `xAxis.setLowerBound(0);` 或根据当前数据位置调整。

---

### 8. 🟠 SensorChart.java - 通道重新启用后历史数据丢失

**位置**: 第 84-92 行

```java
public void toggleChannel(String name) {
    if (enabledChannels.contains(name)) {
        enabledChannels.remove(name);
        XYChart.Series<Number, Number> s = series.get(name);
        if (s != null) s.getData().clear();  // 禁用时清空数据
    } else {
        enabledChannels.add(name);
        // 重新启用时不恢复历史数据
    }
}
```

**问题**: 禁用通道时清空了该序列的所有数据点，重新启用后曲线从空白开始，用户体验不一致。

**建议**: 禁用时不从序列中移除数据，只是将序列从图表中移除或设置透明度；或者保留数据缓存以便恢复。

---

## 三、中级问题（代码质量 / 不一致）

### 9. 🟡 MapCanvas.java - `droneAlt` 字段声明在方法之间

**位置**: 第 414 行

```java
// 在 drawDrone() 方法之后
private double droneAlt = 0;
public void setDroneAlt(double alt) { this.droneAlt = alt; draw(); }
```

**问题**: 字段声明夹杂在方法之间，不符合 Java 编码规范，可读性差。

**修复**: 将 `droneAlt` 字段移到类顶部的字段声明区域（与 `droneLat`、`droneLon` 等放在一起，第 32-34 行附近）。

---

### 10. 🟡 MainController.java - 未使用的导入

**位置**: 
- 第 9 行: `import javafx.beans.binding.Bindings;`
- 第 18 行: `import javafx.scene.shape.Circle;`

**问题**: 这两个导入在代码中没有被使用。

---

### 11. 🟡 MapCanvas.java - 未使用的导入

**位置**: 第 13 行

```java
import javafx.scene.paint.Paint;
```

**问题**: `Paint` 在代码中未被使用。

---

### 12. 🟡 FXML 中有 fx:id 但控制器中无对应 @FXML 字段

以下 FXML 元素有 `fx:id` 但控制器中没有声明对应字段（不会导致错误，因为 JavaFX 只注入声明了的字段）：

| fx:id | FXML 行号 | 说明 |
|-------|----------|------|
| `mapCtrlBar` | 168 | 地图控制栏 HBox |
| `mapToolbar` | 174 | 地图工具栏 HBox |
| `droneHud` | 214 | 无人机 HUD 卡片 VBox |
| `waveformContent` | 303 | 波形内容区 VBox |
| `chTemp` | 331 | 温度通道标签 |
| `chHum` | 336 | 湿度通道标签 |
| `chPres` | 341 | 气压通道标签 |
| `chRoll` | 346 | 横滚通道标签 |
| `chPitch` | 351 | 俯仰通道标签 |
| `chYaw` | 356 | 偏航通道标签 |
| `chAlt` | 361 | 高度通道标签 |
| `chVolt` | 366 | 电压通道标签 |
| `chBatt` | 371 | 电量通道标签 |

**说明**: 这些元素通过 `onMouseClicked` 事件的 `event.getSource()` 获取，因此不需要在控制器中声明字段。但如果后续需要通过代码直接操作这些元素，需要添加对应的 @FXML 字段。

---

### 13. 🟡 按钮有 fx:id 但无 onAction 处理方法

以下按钮在 FXML 和控制器中都有字段声明，但缺少事件处理方法：

| 按钮 | FXML 行号 | 说明 |
|------|----------|------|
| `uploadE1Btn` | 135 | 上传至 E1 - 实机模式功能 |
| `autoMissionBtn` | 136 | 开始自动任务 - 实机模式功能 |
| `armBtn` | 150 | 解锁 - 控制测试面板 |
| `disarmBtn` | 151 | 急停/上锁 - 控制测试面板 |

**说明**: 这些可能是预留的实机交互功能，目前尚未实现。建议添加空的事件处理方法并显示"功能开发中"提示，或添加 TODO 注释。

---

### 14. 🟡 手机传感器 ToggleButton 无 onAction 处理

**位置**: FXML 第 418-420 行

```xml
<ToggleButton fx:id="phoneGpsToggle" text="GPS" selected="true"/>
<ToggleButton fx:id="phoneCompassToggle" text="罗盘" selected="true"/>
<ToggleButton fx:id="phoneBaroToggle" text="气压计" selected="false"/>
```

**问题**: 这三个 ToggleButton 在控制器中有字段声明，但没有对应的 `onAction` 处理方法。用户点击后没有任何反馈。

---

### 15. 🟡 状态栏 `sbSample` 字段从未更新

**位置**: MainController 第 67 行声明，FXML 第 275 行初始值 "1s"

**问题**: `sbSample`（采样间隔）在状态栏中显示，但代码中从未通过编程方式更新其值。设置面板的采样间隔滑块改变后，状态栏不会同步更新。

---

## 四、低级问题（轻微 / 建议优化）

### 16. 🟢 FXML 中导入了 MapCanvas 但未使用

**位置**: main.fxml 第 6 行

```xml
<?import cn.edu.nuaa.gcs.ui.MapCanvas?>
```

**问题**: MapCanvas 是通过 Java 代码动态添加到 `mapContainer` 中的，FXML 中并没有直接使用 `<MapCanvas>` 标签。此 import 可以删除。

---

### 17. 🟢 `onEditWaypoint` 方法缺少 @FXML 注解但不影响功能

**位置**: MainController 第 738 行

**说明**: `onEditWaypoint(int idx)` 方法是 `private` 且无 `@FXML` 注解，但它是通过代码调用的（双击航点、右键菜单、列表编辑按钮），不是由 FXML 直接触发的，因此不需要 `@FXML`。这是正确的设计，但命名容易让人误解为 FXML 事件处理器。

---

### 18. 🟢 `sbWp` 显示格式不一致

**位置**: MainController 第 1252 行

```java
if (sbWp != null) sbWp.setText(count + "/" + count);
```

**问题**: 航点状态栏显示为 "6/6" 格式（当前/总数），但两个值相同，没有实际意义。建议改为 "6 航点" 或 "WP: 6"。

---

### 19. 🟢 FXML 中 `MapCanvas` 的 import 声明

虽然当前未在 FXML 中直接使用，但保留 import 也无害，不影响功能。

---

## 五、功能完整性检查

### 新增交互功能检查清单

| 功能 | 状态 | 备注 |
|------|------|------|
| 航点拖拽 | ✅ 基本可用 | 但有点击重复触发 bug |
| 编辑弹窗 | ✅ 可用 | `onEditWaypoint` 实现完整 |
| 右键菜单（地图） | ✅ 可用 | `showMapContextMenu` 实现完整 |
| 右键菜单（航点） | ✅ 可用 | `showWaypointContextMenu` 实现完整 |
| 键盘快捷键 | ✅ 可用 | DELETE/+/=/-/F/SPACE/Ctrl+S/O/N |
| 传感器曲线交互 | ⚠ 部分可用 | 时间窗口切换有 bug（不重置下界） |
| 响应式设计 | ✅ 基本可用 | compact-mode + 面板宽度调整 |

### 响应式设计检查

| 功能 | 状态 | 备注 |
|------|------|------|
| 最小窗口尺寸 | ✅ | 900x600 |
| 紧凑模式（<1100px） | ✅ | 添加 compact-mode 样式类 |
| 左侧面板宽度自适应 | ✅ | 240/280/320 三档 |
| 分割线位置自适应 | ✅ | 0.27/0.22/0.20 三档 |
| 高度响应 | ⚠ 空实现 | `applyResponsiveHeight` 方法体为空 |

---

## 六、修复优先级建议

| 优先级 | 问题编号 | 预估影响 |
|--------|---------|---------|
| P0 立即修复 | #1, #2 | 编译失败 / 核心功能异常 |
| P1 尽快修复 | #3, #5, #7 | 运行时错误 / 视觉异常 |
| P2 计划修复 | #4, #6, #8, #9 | 功能不完善 / 代码质量 |
| P3 后续优化 | #10~#19 | 代码清理 / 体验优化 |

---

## 七、总结

共发现 **19 个问题**，其中：
- 🔴 **严重 2 个**（重复字段声明、点击事件重复触发）
- 🟠 **高级 6 个**（FXML 不匹配、CSS 变量错误、功能缺陷）
- 🟡 **中级 7 个**（代码质量、未使用导入、缺失处理器）
- 🟢 **低级 4 个**（优化建议、轻微问题）

最关键的两个问题是：
1. **`sensor` 字段重复声明** —— 会导致编译失败，项目无法构建
2. **地图单击添加两个航点** —— 核心交互功能 bug，严重影响用户体验

建议优先修复 P0 和 P1 级别的问题。
