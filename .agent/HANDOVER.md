# SkyLink GCS 交接文档

> 更新时间：2026-09-06 21:20 · 由上一个会话交接（上下文窗口将满，重开窗口续作）
> **新会话第一步：通读本文件 + `.agent/plan.json`，然后按「六、新窗口开工指引」执行。**

---

## 一、项目概况

**目标**：JavaFX 无人机地面站（SkyLink GCS），复刻 CHERGLOW GCS（https://gc.cherglowtech.com/）界面与功能，直连真机调试。

**真机**：琛光 E1 系自研机（songge8/CF-Drone 固件，ESP32），USB 串口 **COM5 @ 115200 8N1**，CLI 文本协议。**无 GPS / 磁力计 / 气压计 / 测距 / 光流**（真机探测全部缺失）；1S 电池 3.31V 低电禁解锁。

**铁律（用户多次强调）**：
1. **禁用假数据/模拟器**——所有显示数据必须来自真机。
2. 但**「遥控器模拟」面板必须保留**：其杆位由电机实际输出**反向推演**，不是假数据（曾被误解删除，用户纠正过）。
3. 地图**离线**使用（高德瓦片已预下载）。
4. 界面全中文；沟通与产物全中文；计划落盘 `.agent/plan.json`。
5. 用户按截图编号逐条反馈问题（"图一/图二"=附件顺序），修正需逐条对应并说明根因。

**技术栈**：
- Java 17（Temurin）+ JavaFX 17.0.2（win classifier）+ jSerialComm 2.11.0 + junit-jupiter 5.10.2（均本地 ~/.m2 缓存）
- Maven 3.9.6 **不在 PATH**，完整路径：
  `C:/Users/wjh22/.m2/wrapper/dists/apache-maven-3.9.6-bin/3311e1d4/apache-maven-3.9.6/bin/mvn.cmd`
- 项目根：`C:\Users\wjh22\Desktop\skylink-gcs-main\skylink-gcs-main`（注意目录嵌套两层）

**常用命令**（在项目根执行）：
```
编译+测试   <mvn路径> -q test
启动应用    <mvn路径> javafx:run
一键启动    桌面「SkyLink GCS.lnk」（launch-gcs.vbs → launch-gcs.bat，javaw + target/classes）
```

---

## 二、进度总览（plan.json 已同步）

| Story | 内容 | 状态 |
|---|---|---|
| S1 | 工程骨架+主外壳（顶栏/侧栏/主题） | ✅ done |
| S2 | 真机串口链路（CLI 协议+连接状态机） | ✅ done |
| S4 | 连接对话框+顶栏接入 | ✅ done |
| S5 | Canvas 瓦片地图（替代 WebView）+ 高德离线三层 | ✅ done |
| S6 | 仪表盘页（3D/ADI/传感器/遥控/电机） | ✅ done（3D 挂账，见下） |
| S7 | 飞行页（HUD+模式+指令链路，chip 行已删） | ✅ done |
| S8 | 规划页（任务面板+航点+距离） | ✅ done |
| S9 | 调参页（参数字典/机架/电机/CH/PID/参数列表三态） | ✅ done |
| S10 | 数据页·波形监视器（250ms 采样+环形缓冲+16 通道） | ✅ done |
| **S11** | **数据页·CLI 控制台+飞行日志** | ✅ done（代码完成+构建全绿；**真机验收清单未跑**，见六-2） |
| S12 | 真机联调验收+端到端清单+README+打包 | ⬜ 未开工（**下一个**） |
| S13 | 预留：MAVLink/WiFi UDP（Drone_WiFi@192.168.4.1:14550） | ⬜ 未开工 |

**S11 实现偏差（已记入 plan.json notes）**：飞行日志未用 SQLite，改为 `~/.skylink/logs/session_<ts>.skylog` 文件（首行元数据 JSON + CSV 采样行，空字段=NaN）；「板上日志导入（log dump 解析）」未实现，留 S12 评估。

**挂账（用户指示"先放一步"）**：3D 模型显示异常。现版 `Drone3DView` 为 Canvas 软投影实现（按三视图重做过、固定视角、随姿态变化），用户仍反馈异常。回修时基于现版继续，勿推倒重来。

---

## 三、架构与关键文件地图

```
src/main/java/com/cherglow/gcs/
├── App.java                 入口；ThemeManager.setMainScene/apply；ConnectDialog 接线
├── Launcher.java            launch-gcs.bat 用的 main（避开 JavaFX 模块封装）
├── core/
│   ├── AppState.java        connStatus{DISCONNECTED/CONNECTING/CONNECTED/ERROR}+connDetail+missionDirty/Count
│   ├── ConnectionService.java 单例；桥接 CliLink→AppState/LiveVehicle；sendCommand(未连接 Toast 拦截)/requestOnce/requestParams
│   ├── CliLink.java         轮询计划 POLL_PLAN={ps,mot,psq,rc,ps,mot,psq,imu,status}；PendingCmd 队列；"p NAME" 单参数 800ms 预算
│   └── FlightRecorder.java  S11 新增。单例；armed true→start("解锁")、false→stopAndSave("上锁")、断开也 stop；
│                            250ms Timeline 采样 double[9]={tRelMs,roll,pitch,yaw,volt,m1..m4}(百分比)；
│                            会话文件 ~/.skylink/logs/session_<ts>.skylog；sessions()/load()/Session record
├── serial/SerialTransport.java jSerialComm 封装；open 前 clearDTR()/clearRTS()（防 ESP32 复位）；
│                            SEMI_BLOCKING，SerialPortTimeoutException 必须 catch-continue（read 超时抛异常不是返回-1）
├── protocol/cli/CliProtocol.java 解析器（黄金样本=workspace/com5_dump.txt 真机抓包）；P_ARMING_DISABLED 用 [ \t]*
├── ui/
│   ├── map/MapView.java     纯 Canvas 瓦片引擎：LRU512+磁盘直读+高德异步补块落盘+上级瓦片补位≤3级+锚点缩放；
│   │                        invert() 像素反相暗色化；getViewportBBox()；setCenter(lat,lng,zoom)；
│   │                        ⚠ 勿调 getCenter()（BorderPane 的 final，不是地图中心）
│   ├── pages/FlyPage.java   飞行页；ADI 浮窗 setMaxSize(USE_PREF_SIZE) 定尺寸 160×150；模式徽章中文映射；Ctrl+A 解锁
│   ├── pages/DashPage/...   仪表盘（含 Drone3DView 3D 模型、遥控器模拟=电机反推杆位、电池 HY 802540 3.7V 600mAh 2.22Wh 卡）
│   ├── pages/SetupPage.java S9 调参页（ParamDict 字典 ~90 项中文/范围；paramListSection 三态）
│   ├── pages/PlanPage.java  任务面板；haversine 距离；JSON 存 ~/.skylink/missions/；脏标记同步 AppState
│   ├── pages/DataPage.java  三 tab：波形监视器 / CLI 控制台 / 飞行日志
│   │                        consolePane(): consoleLines(上限800)+FilteredList+危险命令 ConfirmDialog
│   │                        （DANGER_CMDS={arm,mfr,mfl,mrr,mrl,preset,reboot}）+过滤/暂停/自动滚动/清空
│   │                        logPane(): 会话列表+drawLog() 8曲线+logSlider 标记+recorder.addListener
│   └── setup/ParamDict.java 参数中文名/范围字典 + fmt()
├── server/TileServer.java   com.sun.net.httpserver 127.0.0.1:8135 服务 ~/.skylink/tiles +CORS+代理回退渐进落盘
└── tools/                   TileDownloader（downloadRegion(bbox,progress) 可复用）/SerialSmokeTest/RawSerialProbe
```

**离线地图现状**：`~/.skylink/tiles` 共 316MB——全国 z3-8（3283 块）+ 北京 20km z11-15（5070）+ 南昌 25km z10-15（6002）。高德端点：矢量 `webrd0{1-4}.is.autonavi.com` style=8；卫星 `webst0{1-4}` style=6（免 key，vec 瓦片运行时像素反相做暗色）。

**主题**：`app.css` looked-up colors（-c-*）+ `theme-light.css` 只覆盖 -c-* + ThemeManager。深色异常白底类问题先查 `.viewport` 透明与 -c-* 覆盖。

---

## 四、真机 CLI 协议速查

- 打开串口后 **DTR/RTS 必须为 false**，否则 ESP32 复位重启。
- 命令（CR/LF 结尾）：`status` `ps` `psq` `rc` `mot` `imu` `p`（全部参数，"NAME = value" 行约100项）`p NAME`（单参数）`p NAME value`（设置）`sys` `wifi`
- 模式映射：RAW0 / ACRO1 / STAB2 / ALTHOLD3 / POSHOLD4
- 危险命令（UI 必须二次确认）：`arm` `mfr` `mfl` `mrr` `mrl` `preset` `reboot`
- 真机抓包黄金样本：`workspace/com5_dump.txt`（上个会话工作区，若丢失可重新抓）
- COM5 错误码 5 = 端口被其它程序占用（曾发生，等用户释放；不是本应用的问题）

---

## 五、踩坑清单（务必避免重蹈）

1. **jSerialComm**：SEMI_BLOCKING read 超时抛 `SerialPortTimeoutException`（不是返回 -1）→ catch 后 continue；`setDTR(false)` 不存在 → 用 `clearDTR()/clearRTS()`；逐字节 `(char)c` 会把中文 Latin-1 化 → 用 ByteArrayOutputStream 按 UTF-8 解码整行。
2. **JavaFX 17 限制**：无 `BooleanProperty.map`；无 DirectionalLight（用 PointLight）；`TextAlignment`/`StrokeLineCap` 在 `javafx.scene.text/shape`；Canvas 不是 Region；`BorderPane.getCenter` 是 final；BorderStroke 构造器是 10 参（别多传 strokes）。
3. **连接状态机语义**：失败保持 `ERROR` + retrying 标记，只有成功才转 `CONNECTED`（复刻自项目已修坑，别改回去）。
4. **Maven 中央仓库被墙** → 阿里云镜像 `maven.aliyun.com/repository/public`；javafx-web/media 17.0.2 曾手动下载放 ~/.m2（win+plain 两个 media jar 都要 + 删 .lastUpdated）。
5. **脚本/工具坑**：bash 双引号会吞 PowerShell `$_` → 复杂 PS 写 .ps1 文件执行；python str.replace 匹配失败是静默 no-op → **每次 replace 后必须 grep 回读验证**；引号密集内容用 Write 工具写 .py 文件执行（heredoc 嵌套炸过 3 次）。
6. **地图**：CARTO 匿名瓦片要 API key（返回水印图）；WebView 版地图因 WebKit 合成慢+黑屏已弃用，勿回头。
7. 参数列表 25Hz 无效刷新 → hashCode 内容变化检测已修。

---

## 六、新窗口开工指引

### 1. 恢复上下文
读本文件 → 读 `.agent/plan.json`（statuses + notes）→ `mvn -q test` 确认全绿 → `mvn javafx:run` 启动。

### 2. S11 真机验收清单（首个执行项，需真机 COM5 在位）
- [ ] 连接 COM5 → 数据页「CLI 控制台」tab：轮询流（ps/mot/psq/rc/imu）持续滚动
- [ ] 输入 `status` 回车 → 回显正常；输入 `p pid_rp_p` → 单参数读取返回
- [ ] 输入 `arm` → 弹二次确认；取消则不发送（**电机在架子上，确认弹窗后务必先取消或空载再试**）
- [ ] 过滤关键字 / 暂停 / 清空 / 自动滚动开关均生效；行数上限 800 不膨胀
- [ ] 解锁→电机转→观察「飞行日志」出现「● 记录中…」；上锁后生成 session_*.skylog
- [ ] 会话列表点选 → 右侧 8 曲线回放 + 滑块标记 + 读数
- [ ] 未连接时输入命令 → Toast 拦截提示

### 3. S12（真机联调验收与交付）
- 端到端清单：连接→仪表盘→地图→规划→飞行指令→调参→波形→控制台→日志 全链路真机过一遍
- README（含构建/启动/离线地图/真机接线说明）
- jpackage 打包（Java 17 自带；注意 --add-modules javafx.controls,javafx.web + jSerialComm 依赖）
- 顺带评估：是否补 S11 遗留的「板上日志导入」
- S13（MAVLink UDP）为预留，用户未要求开工，勿主动做

### 4. 3D 模型挂账（回修时机由用户定）
现版 `Drone3DView`（Canvas 软投影、固定视角、随 roll/pitch/yaw 变化）用户仍反馈显示异常。已按三视图（附件）重做过一轮。回修时：先让用户描述/截图具体异常现象，基于现版修，勿推倒重来。电池规格 HY 802540 3.7V 600mAh 2.22Wh 已入卡。

### 5. 沟通规范
中文回复；用户给截图反馈时逐条编号对应修复并说明根因；改完必须构建验证；大改前先备份原文件到会话工作区（非 git 仓库，无版本控制兜底）。
