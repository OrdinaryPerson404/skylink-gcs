# 无人机地面站实机电池与状态显示 实时性/准确性 测试完善 Spec

## Why

对实机（CF-Drone / 琛光 E1）连接下的 JavaFX 地面站进行系统化测试与完善：参照 `交互界面原型-电池功能增加.html` 的设计规范，保证界面显示的所有无人机状态参数与实机状态实时同步（更新延迟 ≤ 1 秒），凡飞控未上报的数据一律诚实标识（"—" / "数据暂不可用"）而不显示误导性默认值，重点核对电池功能模块。全程不修改无人机任何配置参数与固件。

## 测试依据（实机证据）

来自过往实机诊断日志（`diag-hb.txt` / `diag-now.txt` / `diag-lenient.txt`，串口 COM5@115200，CF-Drone 固件）：

| 实机行为 | 日志证据 | 当前 GCS 表现 | 结论 |
|---|---|---|---|
| BATTERY_STATUS(147) 回传哨兵值（无电池传感器）：id=255、remaining=-1%、cell0=0mV、current≈0、temp≈0、consumed 溢出值 | `BATTERY_STATUS [CRC FAIL]: id=255 current=-0.01A temp=-0.0°C consumed=2147483647mAh remaining=-1% cell0=0mV` | `hasBatteryStatus=true` → 电池详情显示 "0.00 A / 0.0 °C" | **不一致**：应显示 "—"（数据无效） |
| SYS_STATUS(1) 回传电池字段全 0（voltage=0.00V/current=0.00A/batteryPct=0%） | `SYS_STATUS: voltage=0.00V current=0.00A batteryPct=0%` | `hasBattery=true` → 主网格显示 "0%"、"0.0V" | **不一致**：0% 是误导性默认值，应显示 "—" |
| 链路静默超时（>5s 无数据） | —（代码审计） | `drone.markDisconnected()` 仅显式断开时调用；超时后旧数据持续显示 | **不一致**：陈旧值残留 > 1s，违反实时同步 |
| E1 油门行：未连接时 | 代码审计 | `connected ? "—" : "0%"` | **不一致**：未连接显示 "0%" |

## What Changes

- **MavlinkParser**：为 BATTERY_STATUS(147) 增加数据有效性判定（`batteryValid`），哨兵值（remaining=-1 / 电芯全 0 / consumed 溢出）不再作为有效数据上报；SYS_STATUS(1) 仅当电压或电量存在有效读数时才置 `hasBattery=true`（全 0/未知视为无电池）。
- **Drone**：
  - `updateFromTelemetry` 只在电池数据有效时更新电池字段并置 `batteryStatusAvailable`；无效哨兵值到达时保持可用性为 false、字段不落库。
  - 有效 BATTERY_STATUS（remaining ∈ [0,100]）除更新详情外，同步回填主网格（`batteryAvailable=true`、电压取电芯和），解决 CF-Drone 无 SYS_STATUS 时主网格电池永远 "—" 的状态不一致。
  - `markDisconnected()` 增加对新增可用性标识的兼容；新增链路超时入口（复用现有方法即可）。
  - 新增 `isBatteryDataValid()` 便捷判定（供 BatteryChargingMonitor 复用）。
- **MainController**：
  - `flushUiUpdates()`：链路静默超时（`!comm.isLinkActive() && lastReceivedMs>0`）时调用 `drone.markDisconnected()`，状态网格/详情/E1 区及时回 "—"。
  - `updateE1StatusDetail()`：`e1ThrottleVal` 未连接时由 "0%" 改为 "—"。
  - 电池详情（电流/温度/电芯）：改用有效性判定而非仅 `batteryStatusAvailable`，无效时显示 "—"。
- **BatteryChargingMonitor**：无人机（MAVLink）电池卡片门控改用 `drone.isBatteryDataValid()`；无有效电池遥测时卡片显示说明文本（"飞控未上报电池遥测"），不展示 0 值卡片；充电验证器的固定 3S 电压阈值在电芯数未知时跳过电压类校验，避免误报。
- **测试**：新增 `DroneTest`（updateFromTelemetry 映射 / 可用性标识 / 无效电池处理 / markDisconnected）、`BatteryChargingMonitorTest`（无 WMI 平台、MAVLink 有效/无效电池），扩充 `MavlinkParserTest`（BATTERY_STATUS 哨兵值、SYS_STATUS 全 0 场景）。
- **不修改**：飞控固件/参数（无 PARAM_SET / 无解锁 / 无模式切换）；不重引入原型中的"模拟电池库 / 模拟充电曲线"（已按项目约束永久移除）；不改动既有常量与 UI 主风格。

## Impact

- 受影响能力：电池状态显示（主网格、电池详情、充电工作区卡片）、状态网格诚实标识、链路超时状态机、遥测→UI 节流链路（行为不变，仍 100ms/10fps，延迟上限 100ms << 1s）。
- 受影响代码：
  - `gcs-javafx/src/main/java/cn/edu/nuaa/gcs/comm/MavlinkParser.java`
  - `gcs-javafx/src/main/java/cn/edu/nuaa/gcs/model/Drone.java`
  - `gcs-javafx/src/main/java/cn/edu/nuaa/gcs/ui/MainController.java`
  - `gcs-javafx/src/main/java/cn/edu/nuaa/gcs/comm/BatteryChargingMonitor.java`
  - 新增测试：`DroneTest`、`BatteryChargingMonitorTest`；扩充 `MavlinkParserTest`
- 兼容性：现有 113+ 单元测试必须保持绿色（MavlinkParser/TelemetryRecorder/BatteryPredictor 等）。

## ADDED Requirements

### Requirement: 电池遥测无效数据识别（BATTERY_STATUS 哨兵值）
系统 SHALL 识别 CF-Drone 无电池传感器时 BATTERY_STATUS 回传的哨兵值（`battery_remaining=-1` 或 电芯电压全 0），并将该次数据视为无效，不用于任何 UI 显示或预测。

#### Scenario: 收到哨兵值 BATTERY_STATUS
- **WHEN** 飞控回传 BATTERY_STATUS 且 `batteryRemaining2=-1`、所有电芯电压为 0
- **THEN** `Telemetry.batteryValid=false`；`Drone` 不更新电池详情字段且 `batteryStatusAvailable` 保持 false；UI 电池详情显示 "—"

### Requirement: SYS_STATUS 零值电池不误报
系统 SHALL 仅当 SYS_STATUS 携带有效电压（>0）或有效电量（pctRaw != 0xFF）时才置 `hasBattery=true`；全 0 / 未知值视为"未上报电池"，主网格显示 "—"。

#### Scenario: 收到全 0 的 SYS_STATUS
- **WHEN** SYS_STATUS 载荷 voltage=0、batteryPct=0（实机 CF-Drone 行为）
- **THEN** `hasBattery=false`；主网格 "电量 BAT / 电压 V" 显示 "—" 而非 "0% / 0.0V"

### Requirement: 链路超时驱动状态清理
系统 SHALL 在链路静默（>5s 无遥测帧）时清理 Drone 模型可用性状态，使全部状态参数回显 "—"，确保显示值与实机状态实时同步（不留陈旧值 > 1s）。

#### Scenario: 无人机链路静默断开
- **WHEN** 已连接实机后停止遥测超过 5 秒
- **THEN** `flushUiUpdates`（100ms 节流）调用 `drone.markDisconnected()`，状态网格/E1 详情/状态栏（纬度经度 GPS HDOP 等）全部显示 "—"，连接横幅显示"链路超时/离线"

### Requirement: 有效 BATTERY_STATUS 回填主网格
系统 SHALL 在收到有效 BATTERY_STATUS（remaining ∈ [0,100]）时，除更新电池详情（电流/温度/电芯/剩余时间）外，同步更新主网格电量与电压（电压取电芯和），使 CF-Drone 等无 SYS_STATUS 飞控的电池主指标仍可实时刷新。

#### Scenario: 有效 BATTERY_STATUS 到达
- **WHEN** BATTERY_STATUS remaining=80、电芯 [3600,3600]mV
- **THEN** 主网格电量显示 "80%"、电压显示"7.2V"；放电电流/温度/电芯/剩余时间同步刷新

### Requirement: 缺失数据诚实标识（"数据暂不可用"）
系统 SHALL 对所有未上报/无效的状态参数显示 "—"（灰色 off 样式），且不得显示 0、0%、空字符串等误导性默认值；电池无数据场景在充电工作区提供"飞控未上报电池遥测"说明。

#### Scenario: E1 未连接时油门行
- **WHEN** 打开地面站且未连接飞控
- **THEN** E1 区"油门"显示 "—"，而非当前实现的 "0%"

### Requirement: 新增单元测试覆盖
系统 SHALL 提供单元测试覆盖：Drone 遥测翻译与可用性标识、无效电池哨兵值、电池充电监控快照（无 WMI / MAVLink 有效 / MAVLink 无效）。

#### Scenario: 运行完整测试套件
- **WHEN** 执行全量测试（`mvn test`）
- **THEN** 全部测试通过，且新增测试断言上述无效电池/零值电池/超时清理语义

### Requirement: 实机测试只读安全约束
**BREAKING（对测试流程）**：测试过程中系统 SHALL 只发送只读报文（HEARTBEAT、PARAM_REQUEST_LIST、REQUEST_MESSAGE、PARAM_REQUEST_READ、AUTOPILOT_VERSION_REQUEST）；严禁发送 PARAM_SET、解锁、模式切换等任何写操作；测试代码不得调用 `setParam` / `armDisarm` / `setMode`。

#### Scenario: 实机/回放测试
- **WHEN** 进行实机或日志回放测试
- **THEN** 上行仅含只读请求，遥测链路无任何配置变更

## MODIFIED Requirements

### Requirement: 电池充电工作区数据源准确性（原：充电监控卡片）
`BatteryChargingMonitor.buildSnapshot` 中无人机电池卡片的"有数据"门控由 `drone.isBatteryAvailable()` 改为同时校验 `drone.isBatteryDataValid()`；当 MAVLink 已连接但电池遥测无效时，卡片显示说明文本（同现有 `note` 机制），摘要中不把无效电池计入"充电中/已充满/总功率"。

#### Scenario: 飞控连接但无电池遥测
- **WHEN** MAVLink 链路已连接，BATTERY_STATUS 为哨兵值
- **THEN** 充电工作区显示无人机电池占位卡，文本"飞控链路已连接，但固件未上报电池遥测"；汇总卡不计入该电池

### Requirement: 电池详情显示门控（原：仅按 hasBatteryStatus）
电池详情（放电电流/电池温度/电芯电压）显示门控由 `batteryStatusAvailable` 改为"可用 + 数据有效"双重判定；电流/温度字段在剩 余量为无效哨兵值时显示 "—"。

#### Scenario: 哨兵值 BATTERY_STATUS 下的电池详情
- **WHEN** 收到 remaining=-1 的 BATTERY_STATUS
- **THEN** 放电电流/电池温度显示 "—"（当前实现会显示 0.00 A / 0.0 °C）

## REMOVED Requirements

（无需求删除。仅确认：不恢复原型中的模拟电池/模拟充电曲线——按项目既有约束，数据源一律真实，缺失以说明文本标识。）