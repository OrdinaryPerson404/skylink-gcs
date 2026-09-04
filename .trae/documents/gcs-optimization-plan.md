# 无人机地面站全面优化实施计划

## Context

当前地面站存在以下经代码审计确认的缺口：
1. **电池数据缺失**：`BatteryPredictor` 用 8 组硬编码数据做线性回归，输入仅 distance/load/wind，不使用任何真实遥测；CF-Drone 固件不通过 SYS_STATUS 上报电池（代码注释 L151 已确认）。用户反馈电池"疑似可通过配置读取到"。
2. **WiFi 链路缺失**：仅 `SerialLink`+`UdpLink`，无 `TcpLink`；无人机 WiFi（192.168.4.1，固定端口可操作页面显示姿态）无法接入。
3. **遥测录制缺失**：`CsvLogStore` 仅管理历史记录，无实时遥测流落盘。
4. **姿态可视化弱**：无人工地平仪，仅有 LineChart 曲线。
5. **链路质量度量缺失**：`isLinkActive()` 仅判 5 秒内是否有数据，无丢包率/包率。
6. **消息解码不全**：`BATTERY_STATUS(147)`/`STATUSTEXT(253)`/`HIGHRES_IMU(105)` 未解码；`SYS_STATUS(1)` 偏移量非标准且未读 current_battery。

**硬约束**：不得修改无人机硬件/固件/配置。全部地面站侧实现，仅只读操作（PARAM_REQUEST_LIST 安全，禁用 PARAM_SET）。

## 实施阶段（依赖驱动）

| 阶段 | 范围 | 依赖 | 验证方式 |
|------|------|------|----------|
| P1 | Parser+Telemetry+Drone 模型 | 无 | `mvn test`（新解码测试） |
| P2 | BatteryPredictor 动态模型 | P1 | `mvn test`（动态预测测试） |
| P3 | TcpLink+NetworkMonitor | 无 | `mvn test`（TCP 回环） |
| P4 | CommunicationService 链路质量 | 无 | `mvn test`（计数器逻辑） |
| P5 | TelemetryRecorder | P1 | `mvn test`（CSV 格式） |
| P6 | AttitudeIndicator Canvas | 无 | 可视化冒烟测试 |
| P7 | MainController+FXML 集成 | P1-P6 | 手动冒烟测试 |
| P8 | 全量回归 | 全部 | 完整 `mvn test` |

## 新增文件（4 个）

### `comm/TcpLink.java`
Socket 实现 `Link` 接口。构造 `TcpLink(String host, int port)`。
- `open()`: `new Socket(host,port)`，`setSoTimeout(1000)` 使 `read()` 可周期返回 null（复刻 `UdpLink` 模式，`listenLoop()` 无需改动）
- `read()`: 从 InputStream 读 2048 字节缓冲，`SocketTimeoutException` 返回 null
- `send()`: OutputStream.write
- `close()/isOpen()`: 标准 Socket 管理

### `util/NetworkMonitor.java`
- `isOfflineMode()` / `checkAsync(Runnable)` / `getLastCheckMs()`
- 探测 `api.open-meteo.com` 可达性，3s 超时，60s 缓存
- `onFetchWindApi` 短路时显示 `[缓存]` 标签

### `data/TelemetryRecorder.java`
- `TelemetryRecorder(File dir, int intervalMs)` → `telemetry_<timestamp>.csv`
- `start(Drone, CommunicationService)` / `stop()`
- 列：`t_ms,roll,pitch,yaw,rollSpeed,...,voltage,batteryPct,batteryCurrent,...,rssi,landedState,armed,customMode`
- 不可用字段写 `—`（诚实标注约定）
- 1Hz 默认间隔，1 小时/10 万行文件轮转

### `ui/AttitudeIndicator.java`
继承 `Canvas`（~220×220）。
- `update(double rollRad, double pitchRad, double yawRad)`：限流 ≤20Hz
- 绘制：天/地平线、俯仰梯（每 10°）、坡度刻度（0/±10/±20/±30/±45/±60）、固定黄色指针
- 使用 `GraphicsContext.setLineDashes()`（JavaFX 21 正确方法名，见项目记忆）

## 修改文件（8 个）

### `comm/MavlinkParser.java`
**a) Telemetry 新增字段**：
- BATTERY_STATUS：`batteryId, batteryCurrent, batteryTemp, cellVoltages[10], capacityConsumed, batteryRemaining2, hasBatteryStatus`
- STATUSTEXT：`statusSeverity, statusText, hasStatusText`
- HIGHRES_IMU：`absPressure, pressureAlt, imuTemp2, airTemp, magX/Y/Z, hasHighresImu`

**b) decodeMessage() 新增 case**：
- case 147 (BATTERY_STATUS)：temperature@3(i16,centi-°C), voltages[10]@5-24(u16,mV), current_battery@25(i16,cA→/100A), current_consumed@27(i32,mAh), remaining@35(i8,%)
- case 253 (STATUSTEXT)：severity@0(u8), text@1(char[50])
- case 105 (HIGHRES_IMU)：abs_pressure@28(f32,hPa), pressure_alt@36(f32,m), temperature@40(f32,°C), xmag/ymag/zmag@20/22/24(i16,milli-T→/1000T)
- **修正 case 1 (SYS_STATUS)**：标准偏移 voltage@8(u16,mV→/1000), current_battery@10(i16,10mA→/100A), battery_remaining@12(i8,%)

**c) getCrcExtra() 新增**：`147→117, 253→83, 105→97`

### `model/Drone.java`
- 新增属性：`batteryCurrent, batteryTemp, capacityConsumed, cellVoltages[10], statusSeverity, statusText, absPressure, pressureAlt, magX/Y/Z, airTemp`
- 新增可用性标识：`batteryStatusAvailable, statusTextAvailable, highresImuAvailable`
- `updateFromTelemetry()` 新增三个 `if (t.hasX)` 块（复刻现有模式）
- `markDisconnected()` 清零新增标识 + 重置 statusText
- 新增访问器（复刻 L221+ 的 getXxx/xxxProperty() 风格）

### `planner/BatteryPredictor.java`
保留静态 `predict()` 交叉验证。新增动态模型：
- `observe(double voltage, double current, double remainingPct, long tMs)` → 滑动窗口 300 样本
- `predictDynamicMinutes()` → <10 样本返回 -1；线性回归 remainingPct vs tMs，剩余时间 = -截距/斜率
- `isDynamicReady()` / `reset()`
- 复用现有 `normalEquation/invert`（2×2 系统）

### `comm/CommunicationService.java`
- 新增计数器：`totalPacketsReceived, heartbeatsSent, lastResetMs`
- `listenLoop()` 有效帧后 `totalPacketsReceived++`
- 心跳 TimerTask 成功 send 后 `heartbeatsSent++`
- 新增 `getLinkQualityPct()`：启发式（活跃且 ≥1/s=100，5s 无包=0）
- `stop()` 重置计数器

### `data/AppSettings.java`
- 复用 `e1Ip(192.168.4.1)/e1Port(14550)` 作为 WiFi 端点（无需新增字段）
- 新增 `boolean wifiPortProbe`（端口探测开关，load/save 持久化）

### `ui/MainController.java`（10 处集成）
a) 新增 @FXML 字段：`sbLink`(状态栏链路), `attitudeCanvas`(姿态画布)
b) `initSettings()` L716：serialPortCombo 增加 `"WiFi: "+e1Ip+":"+e1Port` 选项
c) `connectE1Serial()` L1813：新增 `port.startsWith("WiFi:")` 分支 → `TcpLink`
d) `initDroneStatusGrid()` L750：fields 数组新增电流/电池温度/已耗容量/电芯电压
e) `updateDroneStatus()` L1879：values/formats 数组同步扩展
f) 状态栏链路质量：`sbLink.setText(active?pct+"%":"—")`，e1ConnTimer 每秒更新
g) STATUSTEXT → toast 显示（按 severity 映射告警级别，500ms 去重）
h) BatteryPredictor 动态采样：comm callback 中 `observe()`；预测页显示 `"(实测 Xmin)"`
i) AttitudeIndicator：init() 实例化并替换父容器中的 attitudeCanvas；callback 中 `update(roll,pitch,yaw)`
j) NetworkMonitor：`onFetchWindApi()` 短路离线模式

### `resources/fxml/main.fxml`（3 处）
a) 状态栏 L365 新增：`<VBox styleClass="sb-item"><Label text="链路"/><Label fx:id="sbLink" text="--"/></VBox>`
b) waveSidebar L414 顶部新增：`<Canvas fx:id="attitudeCanvas" width="220" height="220"/>`
c) 设置页 E1 区可选新增 WiFi 探测开关

### 测试文件
- `MavlinkParserTest.java`：getCrcExtra 新增 3 项；修正 testSysStatus 偏移；新增 testBatteryStatus/testStatustext/testHighresImu（+1 个 v2 变体）
- `BatteryPredictorTest.java`：新增 testDynamicPredictionInsufficientSamples/AfterSamples/Reset
- 新增 `TelemetryRecorderTest.java`：CSV 表头+行格式验证

## CRC_EXTRA 验证值（对照 MAVLink common.xml）
- BATTERY_STATUS(147) = 117
- STATUSTEXT(253) = 83
- HIGHRES_IMU(105) = 97

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| CF-Drone 不发 BATTERY_STATUS(147) | 回退：`paramService.getParamsByPrefix("EST_")` 检查电池配置参数，显示 `[配置]` 标签 |
| SYS_STATUS 偏移修正破坏现有测试 | 同步更新 testSysStatus |
| WiFi 端口探测无响应 | 可选开关，后台线程，300ms/port 超时 |
| 姿态 50Hz 重绘卡顿 | 限流 ≤20Hz，GraphicsContext save/restore |
| 录制文件无限增长 | 1Hz 默认间隔，1h/10万行轮转，断开时 stop() |
| TcpLink read() 阻塞 | setSoTimeout(1000)，异常后 listenLoop 重连 |
| STATUSTEXT 高频刷屏 | 500ms 去重，连续相同文本合并 |

## 验证步骤

**分阶段自动化**：
- P1 后：`mvn test -Dtest=MavlinkParserTest`
- P2 后：`mvn test -Dtest=BatteryPredictorTest`
- P5 后：`mvn test -Dtest=TelemetryRecorderTest`

**全量回归（P8）**：
```
& "C:\Users\wjh22\apache-maven\apache-maven-3.9.6\bin\mvn.cmd" test -f "c:\Users\wjh22\Desktop\无人机地面站-课设文稿\gcs-javafx\pom.xml"
```
预期 ~80 测试通过（原 71 + 新增 ~9）。

**运行验证（P7 后）**：
```
& "C:\Users\wjh22\apache-maven\apache-maven-3.9.6\bin\mvn.cmd" javafx:run -f "c:\Users\wjh22\Desktop\无人机地面站-课设文稿\gcs-javafx\pom.xml"
```
视觉检查：姿态指示器随遥测旋转、状态栏链路质量更新、电池详情显示真实值、STATUSTEXT 弹出 toast、TelemetryRecorder 生成 CSV。

**实机测试**：
- WiFi 连接 Drone_WiFi，TcpLink 获取姿态遥测
- 检查 BATTERY_STATUS(147) 是否上报；若无，回退 EST_ 参数检查
- 断网降级验证：风速 [API]→[缓存]，离线地图瓦片正常
