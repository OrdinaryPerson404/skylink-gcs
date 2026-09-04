# Checklist

## 测试基线

- [x] 全量单元测试基线运行通过（现有 113+ 测试全绿），基线数量已记录（122 个）

## MavlinkParser 电池有效性

- [x] BATTERY\_STATUS 哨兵值（remaining=-1、cell 全 0）解析结果 `batteryValid=false`（testBatteryStatusSentinelInvalid）

- [x] BATTERY\_STATUS 有效值（remaining ∈ \[0,100]）解析结果 `batteryValid=true`（testBatteryStatus / testV2BatteryStatusTruncated）

- [x] SYS\_STATUS 全 0（voltage=0、batteryPct=0）解析结果 `hasBattery=false`（testSysStatusZeroBatteryNotHasBattery）

- [x] SYS\_STATUS 有效电压（>0）解析结果 `hasBattery=true`（testSysStatus / testMultipleFrames）

- [x] MavlinkParserTest 新增三类场景断言（哨兵 / 全0 SYS\_STATUS / 有效 BATTERY\_STATUS）并通过

## Drone 模型

- [x] 无效 BATTERY\_STATUS 到达后：电池详情字段不更新、`batteryStatusAvailable` 保持 false（testInvalidBatteryStatusNotStored / testSentinelDoesNotOverwriteValid）

- [x] 有效 BATTERY\_STATUS 到达后：主网格电量/电压回填（电压=电芯和），`batteryAvailable=true`（testValidBatteryStatusBackfillsMainGrid）

- [x] `isBatteryDataValid()` 返回语义正确（仅有效电池遥测时为 true）

- [x] `markDisconnected()` 全部可用性标识清零、状态置 DISCONNECTED（testMarkDisconnected）

- [x] DroneTest 新增并通过（10 用例：遥测翻译 / 可用性 / 无效电池 / 回填 / 断开清理）

## 实时同步（≤1s）

- [x] 链路静默 >5s 时 `flushUiUpdates` 调用 `drone.markDisconnected()`（flushUiUpdates 静默超时分支，行 1242，代码审计确认）

- [x] UI 刷新 Timeline 仍为 100ms（10fps），遥测→显示最坏延迟 ≈100ms+EDT，满足 ≤1s（审计确认）

- [x] 链路恢复后遥测重新刷新（不残留陈旧状态；markDisconnected 幂等 + updateFromTelemetry 自然重置）

## 缺失数据诚实标识

- [x] E1 区"油门"未连接时显示 "—"（不再显示 "0%"；updateE1StatusDetail 行 1398）

- [x] 电池详情（电流/温度/电芯）在电池数据无效时显示 "—"（Drone 层 batteryStatusAvailable 仅有效时置位 + bsOk 门控）

- [x] 状态网格缺失数据全部显示 "—"（SYS\_STATUS 全 0 不再误导显示 0%/0.0V）

- [x] 充电工作区在飞控无电池遥测时显示"飞控链路已连接，但固件未上报电池遥测"说明卡片（BatteryChargingMonitor 哨兵分支）

## 电池充电工作区

- [x] BatteryChargingMonitor 无人机电池卡片门控使用 `isBatteryDataValid()`（buildSnapshot 行 225）

- [x] 电芯数未知时充电验证器跳过固定阈值电压校验（maxVoltage=0 → 无 over\_voltage/失配误报；testMavlinkCardUsesCellDerivedMaxVoltage）

- [x] BatteryChargingMonitorTest 新增并通过（5 用例：无 WMI 降级 / MAVLink 有效 / 无效 note / 电压阈值 / 快照稳定）

- [x] WMI 系统电池快照在非 Windows 平台优雅降级（返回空而非崩溃；testNonWindowsFallbackNoCrash）

## 安全约束（实机测试不变式）

- [x] 全量源码中未引入任何新的 PARAM\_SET / 解锁 / 模式切换调用（grep 审计：仅只读 request\* 调用，setParam/armDisarm/setMode 无 UI 调用点）

- [x] 新增测试仅使用只读请求与合成帧构造，不触碰飞控配置（合成 Telemetry / 反射注入时间戳，无真实串口/WMI/网络依赖）

## 回归

- [x] `mvn test` 全量通过（既有 + 新增测试全绿：140 个，0 失败 0 错误，13 个测试类）

- [x] 无编译错误 / 无新增弃用警告影响构建（mvn test exit code 0）

