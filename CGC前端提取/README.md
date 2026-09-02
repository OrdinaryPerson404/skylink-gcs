# CGC 官方网页地面站 — 前端代码提取报告

来源：https://gc.cherglowtech.com/ （琛光官方 GCS，"CHERGLOW GROUND CONTROL (CGC) — CF-Drone MAVLink 地面站，兼容 PX4 / ArduPilot"）
提取时间：2026-08-31　方式：无 sourcemap，产品级提取 + JS 美化（非源码还原）

## 文件清单

| 文件 | 说明 |
|---|---|
| *_pretty.js | 美化后的关键分包（可读，可作为协议/交互参考） |
| mavlink_pretty.js | **重点**：MAVLink 协议层 + 连接状态机 + 任务协议模块 |
| FlyPage_pretty.js / PlanPage_pretty.js | 飞行视图 / 规划视图 |
| AttitudeIndicator_pretty.js / Compass_pretty.js | 姿态仪 / 罗盘部件 |
| 其余原文件 | Vue+NaiveUI+MapLibre 打包产物（CgcMap 1MB≈maplibre，DataPage 1.1MB≈含图表库） |

## 从 mavlink_pretty.js 挖出的关键设计（Java 实现对照）

1. 连接方式：`ws://{飞控IP}:{端口}`，binaryType=arraybuffer，自定义 MAVLink 帧解析器（带 CRC 校验失败统计回调 pushCrcFail）
2. 状态机：connecting →(6s 超时)→ connected / error；GCS 心跳 1s 间隔；断线重连退避 1s/3s；连接成功 500ms 后配置数据流
3. 任务协议：独立 mission 模块（sysid=1），标准握手
4. 测距：Haversine（地球半径 6371000m）——与我们 GeoUtil 的公式一致，可直接对照验证
5. 页面结构：Overview / Fly / Plan / Setup / Data 五页 + 姿态仪 + 罗盘部件——与 QGC 三视图同构，JavaFX 端 FXML 可按此组织

## 端口结论（结合固件源码 songge8/CF-Drone）

- 固件实锤通道：HTTP:80（网页遥控）+ **MAVLink UDP 14550**（wifi.ino，与 SITL 同端口）
- 本仓库版 web_rc.ino 无 WebSocket 服务；官方 CGC 所需的 WS 为更新固件特性或经桥接
- **我们的 Java GCS 走 UDP 14550 即可对接 E1，无需 WebSocket**

## 使用边界

- 仅作协议格式、状态机设计、交互布局的参考
- 代码为闭源产品产物：不得复制进课设交付物；答辩引用时说明"参考官方站公开行为而非源码"
- 语言也不同（JS vs Java）：我们真正的实现参考是 QGC 架构 + 03-系统设计.md
