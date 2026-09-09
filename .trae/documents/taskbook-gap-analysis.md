# 面向对象课程设计任务书 — 差距清单（只读分析）

> 对照对象：《软件学院课程设计任务书（无人机地面站系统，25-26-01）》
> 对照项目：SkyLink GCS（Java 17 + JavaFX 17.0.2 + Maven + jSerialComm，已完成 S1–S16）
> 本文件为**纯差距清单 + 命名映射**，不涉及任何代码改动。

---

## 一、总体结论

当前项目是远超任务书的成熟专业地面站。按任务书逐条比对，结果分四档：

- **A 硬缺失（教学点名且完全无对应）**：4 项
- **B 缺失功能（任务书点名需求，现无）**：7 项
- **D 部分/替代实现**（由更强或不同方案承担）：8 项
- **E 已满足（含超出）**：7 项

命名映射策略：任务书点名的 4 个类虽无同名，但职责均已由更成熟的类承担，**以现有实现为准并在类图/文档中说明映射**，不新增类。电池预测保留现有标称模型。本次只出清单，不改代码。

---

## 二、A 类：硬缺失（任务书点名类/功能，项目内无对应实现）

| # | 任务书条款 | 项目现状 | 差距性质 |
|---|---|---|---|
| A1 | **CommunicationService 类**（串口数据收发与解析） | 无此名。职责由 `core/ConnectionService` + `core/CliLink` + `core/MavLinkLink` + `serial/SerialTransport` 分担 | 缺名为 easyfix（命名映射） |
| A2 | **XML 任务文件导入/导出**（XMLDecoder/XMLEncoder） | 无。规划页用 JSON（`~/.skylink/missions/`） | 硬缺失 |
| A3 | **传感器动态曲线图**（温度/湿度/气压 LineChart） | 无 LineChart。`DataPage` 有自定义 Canvas 折线（roll/pitch/yaw/电池/M1-4/RC通道），非温度/湿度/气压 | 硬缺失 |
| A4 | **时空类点名数据要求** | — | 见 C/D |

## 三、B 类：缺失功能（任务书点名需求，现无）

| # | 任务书条款 | 项目现状 |
|---|---|---|
| B1 | 航点含 **高度、停留时间**、**优先级** | 航点仅 `[lat,lng]`，无 alt/stay/priority；无动作指令 |
| B2 | 飞行模式（**巡航/悬停/拍照**）+ 任务优先级 | 仅 ACRO/STAB；无巡航/悬停/拍照 |
| B3 | 航点序列**预览模拟动画** | 无 |
| B4 | 冲突检测（航点**距离过近/超出航程**） | 无，仅总距离统计 |
| B5 | 飞行日志**按日期/型号筛选** | 无，按路径排序 |
| B6 | **数据采样间隔设置（1–10 秒）** | 波形固定 250ms，无用户配置 |
| B7 | **低电量红色闪烁预警** | 仅禁解锁 Toast（armingDisabled 透传） |

## 四、D 类：部分/替代实现（以现有更强实现为准）

| # | 任务书条款 | 现有承担 | 判定 |
|---|---|---|---|
| D1 | **Drone 实体类**（型号/状态/传感器） | `model/VehicleSnapshot` + `model/LiveVehicle` | 替代满足 |
| D2 | **Waypoint 航点类**（坐标+动作+优先级+距离计算） | `ui/pages/PlanPage` 内 `Map<Integer,double[]>` + `haversineM()`；有拖拽/增删/保存加载 | 替代满足（缺高级属性见 B1） |
| D3 | **PathOptimizer 贪婪算法** | 无 | 硬缺失，见 A 补充 |
| D4 | 状态面板（位置/高度/速度/电量/信号） | `FlyPage` HUD 全量（位置/速度依赖 GNSS 输入，地速/爬升率现固定"—"） | 满足（更强） |
| D5 | 离线瓦片渲染实时位置+航点路径 | `MapView` canvas 瓦片引擎（vec/sat、拖拽缩放、航点标记+连线+拖拽、GNSS 航迹） | 满足（更强） |
| D6 | 任务文件手动编辑（增/删/改航点） | PlanPage 全支持 | 满足 |
| D7 | 日志备份 CSV/恢复 | `FlightRecorder`(.skylog) + `exportCsv()`(UTF-8 BOM+GPS) + `load()` + DataPage 回放 | 满足 |
| D8 | 异常处理（连接失败/格式错误/越界） | 连接失败 Toast；加载/保存失败 Toast；越界校验 | 前两项满足，越界缺（见 B 补充） |

## 五、E 类：已满足（含超出任务书）

1. 一、数据管理模块：任务文件导入/导出（JSON，非 XML）— 基本满足
2. 实时监控模块：无人机状态面板、地图瓦片渲染、数据图表（自定义波形）— 满足
3. 任务规划模块：手动添加/拖拽航点、保存/加载 — 满足
4. 系统设置模块：串口配置（ConnectDialog：端口/波特率/传输方式）、地图缩放 — 满足（传输频率/采样间隔不全，见 B6）
5. 三、JavaFX UI：主题切换（白天/夜间 CSS）**已实现**；低电量预警部分（仅 Toast）
6. 四、OO 技术要求：封装/单一职责原则（East 上体现）；信号强度显示（SignalCard/WifiLinkIcon）
7. 额外超出：MAVLink/WiFi UDP 双链路、GNSS 融合、航程预测、参数调参、CLI 控制台等（任务书未要求）

---

## 六、命名映射表（任务书点名 → 现有实现）

| 任务书点名类 | 现有实现（映射） | 映射程度 |
|---|---|---|
| Drone | `model/VehicleSnapshot`（不可变遥测快照） + `model/LiveVehicle`（FX 镜像，含型号/状态/传感器数据） | 职责完整映射 |
| Waypoint | `ui/pages/PlanPage` 的 `Map<Integer,double[]>`（含 haversine 距离计算 `haversineM()`） | 部分映射（缺高度/停留/优先级/动作） |
| PathOptimizer | **无实现** | 缺口 |
| CommunicationService | `core/ConnectionService`（单例连接服务） + `core/CliLink`（CLI 轮询/解析） + `core/MavLinkLink` + `serial/SerialTransport`（jSerialComm 封装） | 职责分散映射 |
| （XMLEncoder/XMLDecoder） | JSON（`PlanPage` 保存/加载） | 替代 |
| （LineChart） | 自定义 Canvas 折线（`DataPage`） | 替代 |
| （LinkedList 存航点） | `LinkedHashMap<Integer,double[]>` | 替代 |
| （HashMap 缓存无人机参数） | `LiveVehicle.params` + `ui/setup/ParamDict` | 替代 |
| （Timer 定时采样） | CliLink 轮询调度（周期式） | 替代 |
| （BufferedReader 日志读取） | `Files.readAllLines` + `FlightRecorder.load` | 替代 |
| （Canvas 地图绘制） | `ui/map/MapView`（自定义 Canvas 瓦片引擎） | 满足 |

---

## 七、只读核对项（建议但非必做）

以下为"若要完全贴合任务书可补充"的最小选项，供后续排期参考（本轮不实施）：

- **可选 1**：若需满足"CommunicationService 类名对应"，可在 `serial` 包建薄门面委托现有 ConnectionService/CliLink/SerialTransport（不改现有逻辑）。
- **可选 2**：若需满足"XML 任务文件"，可在 `core` 加 `MissionIo` 用 XMLEncoder/XMLDecoder（与 JSON 并存）。
- **可选 3**：若需满足"温度/湿度/气压 LineChart"，可在 DataPage 加三轴 LineChart 映射现有遥测。
- 其余 B 类（B1–B7）为需求超出项，需投入 UI 面板改造（航点表加列、模式/优先级下拉、预览动画、冲突检测、日志筛选、采样间隔、低电量闪烁）。

---

## 八、已知偏离说明（任务书 vs 项目，均属合理）

1. **UI 布局**：任务书"顶部菜单栏+左侧面板+中央地图+底部状态栏"；项目为 **TopNav 顶部页签 + 中央 StackPane 五页切换**，无独立左侧面板/底部状态栏。已完成且更现代，保持现状。
2. **FXML/SceneBuilder**：任务书要求 FXML 布局；项目**纯 Java 代码构建 UI（无 .fxml）**。黑色代码虽含 View3DSnapshot 等，无 FXML。
3. **电池模型**：任务书要求"线性回归（距离/负载/风速→续航）"；项目 `RangeEstimator` 为**标称模型**（`NOMINAL_FLIGHT_MINUTES=8min×电量%`，非回归）。按你的决策**保留标称模型**。
4. **传感器**：任务书需求温度/湿度/气压曲线；实机（BMP388/VL53L1X/QMC5883x/PMW3901）均未检测到，且电池仅 1S 无人机通常无这些传感器，故项目以电机/姿态/电压/RC通道波形替代。
5. **航点数据源**：任务书要求经纬度/高度/停留时间；项目航点现仅经纬度（高度/停留未建模）。

---

## 附：判定依据文件

- 任务书：`_面向对象课程设计任务）.doc`（附件）
- 现状来源：`.agent/plan.json`、`.agent/acceptance-s12.md`、`.agent/HANDOVER.md`、`README.md`、`src/main/java/com/cherglow/gcs/**`（完整类镜像清单）、`src/main/resources/**`