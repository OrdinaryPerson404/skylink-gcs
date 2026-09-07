# SkyLink GCS · CHERGLOW 地面站复刻

JavaFX 桌面无人机地面站（GCS），复刻 CHERGLOW GCS 的界面与功能，通过 USB 串口直连琛光 E1 系自研机（songge8/CF-Drone 固件，ESP32）的 CLI 文本协议，实现连接、遥测、仪表、飞行指令、任务规划、调参、波形监视、CLI 控制台与飞行日志全链路。

> 全部显示数据来自真机，**不使用任何模拟器/假数据**；「遥控器模拟」面板的杆位由电机实际输出反向推演。

## 功能一览

| 页面 | 内容 |
|---|---|
| 仪表盘 | 3D 四旋翼模型（分色地平线+罗盘环+地面网格+三轴箭头参考系）、ADI 姿态仪、传感器在线状态、遥控输入、遥控器模拟（电机反推）、电机输出 |
| 飞行 | 全幅离线地图 + HUD（航向/横滚/俯仰等）、迷你 ADI 浮窗、系统消息条、指令条（解锁/上锁二次确认 + ACRO/STAB/ALTHOLD/POSHOLD 直切）、任务横幅 |
| 规划 | 任务面板（航点增删/距离统计/保存加载 JSON）、地图点击加航点；机载任务上传当前固件不支持（置灰提示） |
| 调参 | 参数字典驱动的机架/电机/遥控器/端口/PID/安全/传感器分组，参数读写回读校验，校准向导（ca/cr/cm） |
| 数据 | 波形监视器（16 通道/时间窗/十字光标）、CLI 控制台（危险指令二次确认/过滤/暂停/自动滚动）、飞行日志（解锁自动记录/回放） |

## 环境与构建

- JDK 17（Temurin）+ JavaFX 17.0.2 + jSerialComm 2.11.0（Maven 自动解析）
- Maven ≥ 3.8（本机使用缓存发行版 `~/.m2/wrapper/dists/apache-maven-3.9.6-bin/.../bin/mvn.cmd`）

```bash
# 编译 + 单元测试（CLI 协议解析黄金样本 + 连接状态机矩阵）
mvn test

# 启动应用
mvn javafx:run

# 一键启动（Windows，使用已编译产物）
launch-gcs.vbs   # 或桌面「SkyLink GCS」快捷方式
```

打包为 Windows 安装包（需 JDK 17 自带 jpackage）：

```powershell
powershell -ExecutionPolicy Bypass -File package-jpackage.ps1
# 产物：target/installer/SkyLinkGCS-<版本>.exe 及目录版 target/installer/SkyLinkGCS/
```

## 真机连接

| 项 | 值 |
|---|---|
| 端口 | USB 虚拟串口（本机为 **COM5**），**115200 8N1** |
| 铁律 | 打开串口前 **DTR/RTS 必须为 false**，否则 ESP32 复位重启（驱动已内置处理） |
| 供电 | USB 供电时 `status` 读数约 3.31V，**低于解锁阈值属正常**（未插电池）；插上 1S 锂电（HY 802540 3.7V 600mAh）后才能解锁 |
| 传感器 | 实机无 GPS/磁力计/气压计/激光测距/光流，对应卡片显示空态「—」为正确表现 |

连接入口：顶栏「连接」→ 选通道 → 连接；连接失败（如端口被占用）保持错误态并自动退避重试。

- **串口 / USB**（主通道）：COM5 @ 115200，CLI 文本协议轮询。
- **UDP / WiFi**（S13）：电脑加入无人机 AP「Drone_WiFi」（默认密码 12345678）后，填 192.168.4.1:14550 直连 MAVLink——高频遥测 + 解锁/模式/调参全功能（COMMAND_LONG / PARAM 协议 / SERIAL_CONTROL 桥接 CLI）。本机占用 UDP 14550 端口。

### CLI 协议速查（只读）

`status`（状态+电池+安全）/ `ps`（roll/pitch/yaw）/ `psq`（四元数）/ `rc`（通道+控制量）/ `mot`（电机 0~1）/ `imu`（陀螺/加计）/ `p`（全量参数约 100 项）/ `p NAME`（单参数）/ `p NAME value`（写入）/ `sys` / `wifi`

模式：`acro` / `stab` / `althold` / `poshold`；危险命令（UI 强制二次确认）：`arm` `mfr` `mfl` `mrr` `mrl` `preset` `reboot`。

### 姿态口径（与固件 CF-Drone 对齐）

机体系 **FLU：X 机头、Y 左、Z 上**；`ps` 输出为 ZYX 欧拉角（度）：**+roll=右倾、+pitch=低头、+yaw=左转（逆时针）**。无磁力计 → yaw 为上电起陀螺积分的**相对航向**（`reset` 清零），界面罗盘环的「N」代表上电时机头方向，非地理北。

## 离线地图

- 瓦片缓存 `~/.skylink/tiles/`（约 316MB：全国 z3-8 + 北京 20km z11-15 + 南昌 25km z10-15，高德矢量/卫星双底图，矢量瓦片运行时像素反相做暗色）。
- 运行时在线补块由内置 `TileServer`（127.0.0.1:8135）代理落盘；批量扩区用 `tools.TileDownloader#downloadRegion`。
- 地图为纯 Canvas 自绘引擎，**勿**回退 WebView 方案（WebKit 合成慢、跨域限制多）。

## 数据文件

| 路径 | 内容 |
|---|---|
| `~/.skylink/logs/session_<时间戳>.skylog` | 飞行记录：解锁自动开始、上锁/断开结束；首行 JSON 元数据 + CSV 采样行（tRelMs,roll,pitch,yaw,volt,m1..m4，空字段=NaN） |
| `~/.skylink/missions/*.json` | 规划页任务文件 |
| `~/.skylink/tiles/` | 地图瓦片缓存 |

## 开发辅助工具（tools/，java 直跑，不进主程序）

| 工具 | 用途 |
|---|---|
| `tools.S12E2eCheck` | 真机端到端自检：枚举→连接→遥测流断言→参数拉取→模式切换回读→断开清理（无 arm/无电机/无参数写） |
| `tools.SerialSmokeTest` | 串口链路 5s 遥测流冒烟 |
| `tools.View3DSnapshot` | 3D 模型固定姿态快照导出 PNG（9 帧） |
| `tools.AdiSnapshot` | ADI 姿态仪快照导出 PNG（4 帧） |
| `tools.RawSerialProbe` | 串口协议探针（抓原始流判协议） |

运行示例：

```bash
mvn -q compile
java -cp target/classes;<jSerialComm.jar> com.cherglow.gcs.tools.S12E2eCheck COM5
```

## 安全须知

1. **解锁前务必卸桨**；UI 对 `arm` 类指令强制二次确认。
2. 电量低/未插电池时固件禁止解锁，UI 置灰解锁按钮并在点击时 Toast 透传原因。
3. 界面中所有姿态方向口径以固件为准（见上），修改显示代码前先读固件 `quaternion.h`/`imu.ino`。

## 路线图

- S13（预留）：WiFi AP（Drone_WiFi@192.168.4.1）+ MAVLink UDP 14550 高频遥测通道，开工前需确认。
