# Tasks

- [x] Task 1: 建立测试基线：运行全量单元测试（`mvn test`），记录通过与失败数量，确认基线全绿。基线：122 个测试全部通过。
  - [x] SubTask 1.1: 定位 Maven（wrapper dists 中的 apache-maven-3.9.6）并执行 `mvn test`
  - [x] SubTask 1.2: 统计 surefire 报告，确认现有 113+ 测试全部通过，记录基线（实际 122 个）

- [x] Task 2: 修复 MavlinkParser 电池有效性识别（BATTERY_STATUS 哨兵值 / SYS_STATUS 零值）
  - [x] SubTask 2.1: `Telemetry` 增加 `batteryValid` 字段；BATTERY_STATUS 解码时依据 remaining / 电芯 / consumed 判定有效性并置位
  - [x] SubTask 2.2: SYS_STATUS 解码仅在 voltage>0 或 pctRaw!=0xFF 且 >=0 时置 `hasBattery=true`；全 0/未知不置位
  - [x] SubTask 2.3: 扩充 `MavlinkParserTest`：哨兵 BATTERY_STATUS（remaining=-1、cell 全 0）、全 0 SYS_STATUS、有效 BATTERY_STATUS 三类断言

- [x] Task 3: 修复 Drone 模型电池状态翻译（updateFromTelemetry / markDisconnected / 有效性回填）
  - [x] SubTask 3.1: `updateFromTelemetry` 的 hasBatteryStatus 分支仅在 `t.batteryValid` 时更新详情并置 `batteryStatusAvailable=true`；无效时保持 false
  - [x] SubTask 3.2: 有效 BATTERY_STATUS 回填主网格：`batteryAvailable=true`、电压取电芯和（>0 时）
  - [x] SubTask 3.3: 新增 `isBatteryDataValid()`（= batteryStatusAvailable 且电池详情字段有效）；`markDisconnected` 兼容
  - [x] SubTask 3.4: 新增 `DroneTest`：遥测翻译映射、可用性标识、无效电池不落库、有效电池回填、markDisconnected 语义

- [ ] Task 4: 修复 MainController 链路超时清理与标识一致性
  - [ ] SubTask 4.1: `flushUiUpdates()` 静默超时分支调用 `drone.markDisconnected()`（并在恢复后自然重渲染）
  - [ ] SubTask 4.2: `updateE1StatusDetail()` 油门行未连接时显示 "—"（删除 "0%"）
  - [ ] SubTask 4.3: 电池详情（电流/温度/电芯）门控增加数据有效判定，无效时 "—"
  - [ ] SubTask 4.4: 编译并确认 UI 路径无回归（MVN 编译通过；纯逻辑保持可在无 JavaFX 环境测试）

- [ ] Task 5: 修复 BatteryChargingMonitor 无人机电池卡片数据准确性
  - [ ] SubTask 5.1: `buildSnapshot` 中 MAVLink 无人机电池使用 `drone.isBatteryDataValid()` 门控，无效时输出说明卡片（note 机制）
  - [ ] SubTask 5.2: 充电验证器在电芯数未知（cell 全 0）时跳过 voltage/maxVoltage 类校验，避免 3S 固定阈值误报
  - [ ] SubTask 5.3: 新增 `BatteryChargingMonitorTest`：无 WMI（非 Windows）降级、MAVLink 有效电池快照、MAVLink 无效电池 note 快照

- [ ] Task 6: 全量回归验证
  - [ ] SubTask 6.1: 运行 `mvn test`，确认新增 + 既有全部测试通过（目标全绿）
  - [ ] SubTask 6.2: 静态核对 checklist.md 全部检查点，逐项勾选

# Task Dependencies

- [Task 2] 独立（解析层）
- [Task 3] 依赖 [Task 2]（Drone 消费 Telemetry.batteryValid）
- [Task 4] 依赖 [Task 3]（UI 消费 Drone 可用性）
- [Task 5] 依赖 [Task 3]（充电监控消费 Drone.isBatteryDataValid()）
- [Task 2] 与 [Task 4.2]（油门 "—"）可并行；[Task 6] 依赖所有前置任务

**并行建议**：Task 1（基线）先行；随后 Task 2 与 Task 4.2 可并行；Task 3 紧随 Task 2；Task 4 与 Task 5 在 Task 3 完成后可并行；最后 Task 6 汇总验证。