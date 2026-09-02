const fs = require("fs");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, LevelFormat, HeadingLevel,
  BorderStyle, WidthType, ShadingType, PageNumber, PageBreak, VerticalAlign
} = require("docx");

const F = { ascii: "Arial", hAnsi: "Arial", eastAsia: "Microsoft YaHei" };
const border = { style: BorderStyle.SINGLE, size: 1, color: "BBBBBB" };
const borders = { top: border, bottom: border, left: border, right: border };
const noBorder = { style: BorderStyle.NONE, size: 0, color: "FFFFFF" };
const noBorders = { top: noBorder, bottom: noBorder, left: noBorder, right: noBorder };

const h1 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text: t, bold: true, font: F, size: 32 })] });
const h2 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun({ text: t, bold: true, font: F, size: 26 })] });
const h3 = (t) => new Paragraph({ spacing: { before: 160, after: 80 }, children: [new TextRun({ text: t, bold: true, font: F, size: 24 })] });
const p = (t, opts = {}) => new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: t, font: F, size: 22, ...opts })] });
const pb = (t) => new Paragraph({ spacing: { after: 80 }, children: [new TextRun({ text: t, font: F, size: 22, bold: true })] });
const bullet = (t) => new Paragraph({ numbering: { reference: "bul", level: 0 }, spacing: { after: 40 }, children: [new TextRun({ text: t, font: F, size: 22 })] });

const wf = (lines) => lines.map(line => new Paragraph({
  spacing: { after: 0, line: 260 },
  shading: { fill: "F4F6F8", type: ShadingType.CLEAR },
  children: [new TextRun({ text: line, font: { ascii: "Consolas", hAnsi: "Consolas", eastAsia: "Microsoft YaHei" }, size: 16 })]
}));

const cell = (text, opts = {}) => new TableCell({
  borders, width: { size: opts.w || 2340, type: WidthType.DXA },
  shading: opts.fill ? { fill: opts.fill, type: ShadingType.CLEAR } : undefined,
  margins: { top: 60, bottom: 60, left: 100, right: 100 },
  verticalAlign: VerticalAlign.CENTER,
  children: [new Paragraph({ children: [new TextRun({ text, font: F, size: 20, bold: !!opts.bold })] })]
});

const t2 = (rows, w1 = 3000, w2 = 6360) => new Table({
  width: { size: 100, type: WidthType.PERCENTAGE }, columnWidths: [w1, w2],
  rows: rows.map(r => new TableRow({ cantSplit: true, children: [cell(r[0], { w: w1, bold: true, fill: "EDF2F7" }), cell(r[1], { w: w2 })] }))
});

const t3 = (rows, w1, w2, w3) => new Table({
  width: { size: 100, type: WidthType.PERCENTAGE }, columnWidths: [w1, w2, w3],
  rows: rows.map((r, i) => new TableRow({ cantSplit: true, children: [
    cell(r[0], { w: w1, bold: i === 0, fill: i === 0 ? "EDF2F7" : undefined }),
    cell(r[1], { w: w2, bold: i === 0, fill: i === 0 ? "EDF2F7" : undefined }),
    cell(r[2], { w: w3, bold: i === 0, fill: i === 0 ? "EDF2F7" : undefined })
  ] }))
});

const blank = () => new Paragraph({ spacing: { after: 80 }, children: [new TextRun("")] });

const children = [];

// ======================== 封面 ========================
children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 2400, after: 200 },
  children: [new TextRun({ text: "无人机地面站系统", font: F, size: 52, bold: true })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 600 },
  children: [new TextRun({ text: "交互界面设计说明书", font: F, size: 44, bold: true })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
  children: [new TextRun({ text: "课程：面向对象课程设计（25-26-01 学期）", font: F, size: 24 })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
  children: [new TextRun({ text: "选题：第 33 题《无人机地面站系统》", font: F, size: 24 })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
  children: [new TextRun({ text: "技术栈：HTML5 + CSS3 + JavaScript + MapLibre GL JS", font: F, size: 24 })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
  children: [new TextRun({ text: "参考实现：gc.cherglowtech.com（琛光 CGC 网页地面站）", font: F, size: 24 })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 600 },
  children: [new TextRun({ text: "配套交付物：交互界面原型.html（高保真可交互原型）", font: F, size: 22, color: "666666" })] }));
children.push(new Paragraph({ children: [new PageBreak()] }));

// ======================== 第一章 ========================
children.push(h1("第一章  设计概述"));

children.push(h2("1.1 设计目标"));
children.push(p("本说明书为无人机地面站系统（GCS）的交互界面设计文档。界面以琛光 CGC 网页地面站（gc.cherglowtech.com）为参考蓝本，采用 HTML5 + CSS3 + JavaScript 前端技术栈，集成 MapLibre GL JS 真实地图渲染，实现无人机实时监控、任务规划、参数调整与数据管理四大核心功能。界面要求清晰可读、美观、简单易懂、符合用户操作习惯。"));

children.push(h2("1.2 设计原则"));
children.push(bullet("精确对标 CGC：提取 CGC 网站前端代码中的设计 Token（颜色值、字体、间距、圆角、阴影），确保视觉一致性。"));
children.push(bullet("深色科技风：以 #0D1117 为基底背景，#F0A500 为主色，#22C55E/#EF4444/#F59E0B 为状态色，JetBrains Mono 等宽字体显示数值。"));
children.push(bullet("真实地图集成：使用 MapLibre GL JS 渲染 CARTO 暗色底图与 Esri 卫星影像，支持缩放、平移、比例尺、导航控件。"));
children.push(bullet("五页架构：仪表盘 / 飞行 / 规划 / 调参 / 数据 五个功能页，顶部 Tab 切换，各页独立布局。"));
children.push(bullet("实时遥测模拟：JavaScript 定时器驱动状态数据（姿态、高度、速度、电池、GPS 等）实时变化，模拟飞控连接。"));

children.push(h2("1.3 设计依据"));
children.push(t2([
  ["任务书 R1.1", "XML 任务文件导入导出（无人机参数/航点/日志）"],
  ["任务书 R1.2", "航点手动编辑（新增/修改/删除）"],
  ["任务书 R2.1-R2.3", "状态面板、地图显示区、传感器数据图表"],
  ["任务书 R3.1-R3.2", "地图点击加航点、飞行模式/优先级、路径预览、冲突检测"],
  ["任务书 R4.1-R4.2", "通信参数配置、采样间隔 1-10 秒、日志备份/恢复/筛选"],
  ["任务书 R5.1-R5.2", "贪婪路径优化（禁第三方库）、线性回归电池预测"],
  ["任务书 R6.1-R6.2", "界面布局规范、CSS 主题、低电量红色闪烁预警"],
  ["参考站 CGC", "仪表盘/飞行/规划/调参/数据 五页结构，深色科技风视觉，HUD 组件设计"]
], 2400, 6960));
children.push(blank());

// ======================== 第二章 ========================
children.push(h1("第二章  整体布局设计"));

children.push(h2("2.1 顶部导航栏"));
children.push(p("全屏顶部固定 48px 高导航栏，左侧为品牌标识「NUAA GCS」，中部为五个功能页 Tab（仪表盘/飞行/规划/调参/数据），右侧为连接状态指示灯与连接信息文本。Tab 采用下边框高亮（主色 #F0A500）表示当前页，点击切换页面内容。"));
children.push(blank());

children.push(h2("2.2 五页功能架构"));
children.push(p("放弃任务书四区布局，完全对标 CGC 网站的五页独立布局。每个功能页占据导航栏以下的全屏空间，各自定义独立的内部布局结构："));
children.push(t3([
  ["功能页", "对应 CGC 页面", "核心职责"],
  ["仪表盘 Overview", "OverviewPage", "系统总览：3D 模型姿态 + ADI 姿态指示仪 + 传感器矩阵 + RC 通道条 + 摇杆模拟 + 电机输出条"],
  ["飞行 Fly", "FlyPage", "实时飞行监控：MapLibre 全屏地图 + ADI 浮层 + 右侧 HUD + 底部告警面板 + HUD 条 + 指令栏"],
  ["规划 Plan", "PlanPage", "任务规划：MapLibre 地图 + 工具栏 + 右侧任务面板（航点列表/编辑/统计/优化/预测/文件管理）"],
  ["调参 Setup", "SetupPage", "系统设置：左菜单（设置/传感器/参数/固件）+ 右内容区（机架/电机/RC/端口/PID/校准/GPS/气压/光流/测距/OTA）"],
  ["数据 Data", "DataPage", "数据管理：三 Tab（波形监视器 Canvas / MAVLink 控制台 / 飞行日志 + 轨迹回放地图）"]
], 1800, 2200, 5360));
children.push(blank());

children.push(h2("2.3 整体框架线框图"));
children.push(...wf([
  "+============================================================================+",
  "| NUAA GCS    [仪表盘] [飞行] [规划] [调参] [数据]    ● 已连接·UDP 14550  |  <- 导航栏 48px",
  "+============================================================================+",
  "|                                                                            |",
  "|                                                                            |",
  "|              各功能页独立布局（占满导航栏以下全部空间）                     |",
  "|                                                                            |",
  "|              · 仪表盘：三行卡片网格                                         |",
  "|              · 飞行：全屏地图 + 浮层 + 底部条                               |",
  "|              · 规划：地图 + 右侧任务面板                                    |",
  "|              · 调参：左菜单 + 右内容                                        |",
  "|              · 数据：Tab 切换三视图                                         |",
  "|                                                                            |",
  "|                                                                            |",
  "+============================================================================+"
]));
children.push(blank());

// ======================== 第三章 ========================
children.push(h1("第三章  各功能页详细设计"));

// ---- 3.1 仪表盘 ----
children.push(h2("3.1 仪表盘页（Overview）"));
children.push(pb("布局说明"));
children.push(p("仪表盘为系统总览页，采用垂直滚动布局，内容区分为两行卡片网格：第一行三列（无人机模型 / 姿态指示仪 ADI / 传感器状态），第二行三列（遥控输入 CH1-12 / 遥控器摇杆模拟 Mode2 / 电机输出 M1-12）。每张卡片为圆角面板（#161B22 背景 + #21262D 边框 + 10px 圆角），含标题栏与内容区。"));
children.push(pb("组件清单"));
children.push(t2([
  ["无人机模型卡", "SVG 绘制四旋翼俯视图，四电机旋翼随解锁状态旋转，机身随横滚角倾斜；右侧显示横滚/俯仰/航向数值"],
  ["姿态指示仪 ADI", "SVG 人工地平仪，天地分界线随横滚/俯仰旋转平移，下方三格显示高度/地速/爬升率"],
  ["传感器状态矩阵", "2×3 网格，IMU/罗盘/气压计/测距仪/光流/GPS，左侧色条标识状态（绿正常/橙警告/红故障/灰离线）"],
  ["遥控输入条 CH1-12", "12 个垂直条形图，从底部向上填充（主色 #F0A500），顶部显示 PWM 值"],
  ["摇杆模拟 Mode2", "两个圆形摇杆区域，左摇杆 THR/YAW、右摇杆 ROLL/PITCH，圆点位置随 RC 通道值移动"],
  ["电机输出条 M1-12", "12 个垂直条形图，填充色随百分比变化（绿<65%/橙<85%/红>85%）"]
], 2400, 6960));
children.push(blank());
children.push(pb("线框图"));
children.push(...wf([
  "+-----------------------------------------------------------------------------+",
  "|  +---------------+  +---------------+  +---------------+                    |",
  "|  | 无人机模型     |  | 姿态指示仪 ADI |  | 传感器状态     |                   |",
  "|  |    [SVG]      |  |    [ADI球]    |  | IMU  罗盘     |                   |",
  "|  |  ROL 2.3°     |  | 86.4m 8.2 +0.6|  | 气压  测距!   |                   |",
  "|  |  PIT 1.5°     |  |  高度 地速 爬升|  | 光流  GPS     |                   |",
  "|  |  YAW 128°     |  |               |  |               |                   |",
  "|  +---------------+  +---------------+  +---------------+                    |",
  "|  +---------------+  +---------------+  +---------------+                    |",
  "|  | 遥控输入       |  | 遥控器模拟     |  | 电机输出       |                   |",
  "|  | CH1 ▓▓▓       |  |   ●         ●  |  | M1 ▓▓▓       |                   |",
  "|  | CH2 ▓▓▓       |  |  THR/YAW  R/P  |  | M2 ▓▓▓       |                   |",
  "|  | ...CH12       |  | 1500 1500     |  | ...M12       |                   |",
  "|  +---------------+  +---------------+  +---------------+                    |",
  "+-----------------------------------------------------------------------------+"
]));
children.push(blank());

// ---- 3.2 飞行 ----
children.push(h2("3.2 飞行页（Fly）"));
children.push(pb("布局说明"));
children.push(p("飞行页以 MapLibre 全屏地图为主视觉，地图上叠加多个浮层：顶部中央信息横幅、左下角 ADI 姿态指示仪（可切换显示/隐藏）、右侧 HUD 数据面板、底部中央可折叠告警面板。地图下方为底部 HUD 条（电池/电压/GPS/RC/经纬度）和指令栏（解锁/起飞/任务/降落/返航/悬停）。"));
children.push(pb("组件清单"));
children.push(t2([
  ["MapLibre 地图", "CARTO 暗色底图全屏渲染，支持缩放/平移，叠加无人机方向标记（蓝色菱形 SVG，随航向旋转）"],
  ["顶部横幅", "中央浮层，显示飞行模式提示或警告信息，可点击跳转"],
  ["ADI 浮层", "左下角 164px 圆形姿态指示仪，可隐藏/显示切换（◎ ADI 按钮）"],
  ["右侧 HUD 面板", "毛玻璃浮层，显示 HDG/ALT/GS/VS/ROL/PIT 六项数值 + 飞行模式标签"],
  ["告警面板", "底部中央可折叠，按时间倒序列示系统消息，告警计数徽章（橙色警告数）"],
  ["底部 HUD 条", "44px 高常驻条，电池/电压/GPS卫星数/RC信号/纬度/经度"],
  ["指令栏", "64px 高，六按钮：解锁(橙)/起飞(主色)/任务(绿)/降落/返航/悬停，均二次确认"],
  ["电池预警", "电量 <15% 数值红色，<30% 橙色，正常绿色"]
], 2400, 6960));
children.push(blank());
children.push(pb("线框图"));
children.push(...wf([
  "+-----------------------------------------------------------------------------+",
  "|  [横幅: 飞行模式提示]                        [HUD: HDG 128°             ]   |",
  "|                                              [     ALT 86.4m            ]   |",
  "|                                              [     GS  8.2 m/s          ]   |",
  "|  [ADI]                                       [     VS  +0.6 m/s         ]   |",
  "|  (姿态球)                                    [     ROL 2.3  PIT 1.5     ]   |",
  "|                                              [     [AUTO]               ]   |",
  "|             MapLibre 真实地图                                   [告警面板]  |",
  "|             CARTO 暗色底图                                  2 ⚠测距仪弱     |",
  "|                  ◎ ←无人机标记                               14:21 自动模式  |",
  "|                                                                       |",
  "+-----------------------------------------------------------------------------+",
  "| 电池72% | 电压12.4V | GPS14颗 | RC88% | LAT32.0612° | LON118.7930°  |  <- HUD条 |",
  "+-----------------------------------------------------------------------------+",
  "|        [解锁]  [起飞]  [任务]  [降落]  [返航]  [悬停]                    |  <- 指令栏 |",
  "+-----------------------------------------------------------------------------+"
]));
children.push(blank());
children.push(pb("交互说明"));
children.push(bullet("点击「解锁」→ 弹出确认框「解锁后电机将启动，请确保周围安全」→ 确认后发送解锁指令。"));
children.push(bullet("点击「起飞」→ 弹出确认框「起飞至默认高度 2 米」→ 确认后执行自动起飞。"));
children.push(bullet("点击「任务」→ 弹出确认框「将按任务列表依次飞行」→ 确认后按航点序列飞行。"));
children.push(bullet("点击「◎ ADI」按钮 → 切换左下角姿态指示仪的显示/隐藏。"));
children.push(bullet("告警面板点击标题栏可折叠/展开，徽章显示告警数量。"));
children.push(bullet("电量 <15% 时电池数值红色，<30% 橙色，正常绿色。"));
children.push(blank());

// ---- 3.3 规划 ----
children.push(h2("3.3 规划页（Plan）"));
children.push(pb("布局说明"));
children.push(p("规划页左侧为 MapLibre 地图区域（占主体面积），右侧为 280px 宽任务面板。地图上左上角浮动工具栏（适配视图/切换底图/清空航点），底部浮动 HUD（显示距离/高度/速度/爬升率/经纬度/航向）或操作提示。点击地图任意位置添加航点，航点以序号圆标标记，自动连线路径。右侧面板含任务名、航点列表、航点编辑器、任务统计、任务操作（上传/下载/保存/加载）、路径优化、电池预测。"));
children.push(pb("组件清单"));
children.push(t2([
  ["MapLibre 地图", "CARTO 暗色底图 / Esri 卫星影像可切换，点击添加航点，航点可拖拽调整位置"],
  ["地图工具栏", "左上角浮层：适配视图 / 切换底图(卫星/矢量) / 清空航点"],
  ["地图 HUD", "底部中央浮层：距离 D / 高度 H / 地速 GS / 爬升 VS / 经纬度 / 航向 HDG"],
  ["任务名称", "文本输入框，可编辑任务名称"],
  ["航点列表", "序号圆标(主色背景) + 经纬度坐标 + 删除按钮，选中项左侧主色边框"],
  ["航点编辑器", "高度(m) / 速度(m/s) / 悬停(s) / 优先级(1-5) / 动作(CRUISE/LOITER/TAKEPHOTO)"],
  ["任务统计", "航点数、预计距离(Haversine 求和 km)、冲突检测(<50m 提示)"],
  ["任务操作", "上传到飞控(进度条) / 从飞控读取 / 保存XML / 加载XML"],
  ["路径优化", "贪婪最近邻算法，一键优化，显示优化前后距离对比（R5.1）"],
  ["电池预测", "距离/负载/风速输入 → 线性回归预测续航时间与耗电百分比（R5.2）"],
  ["航点路径线", "GeoJSON LineString，主色 #F0A500 虚线，航点圆标 SVG（Home 绿色/其余主色）"]
], 2400, 6960));
children.push(blank());
children.push(pb("线框图"));
children.push(...wf([
  "+---------------------------------------------+-------------------+",
  "| [⊞适配] [🛰卫星] [🗑清空]                   | 任务规划  ●      |",
  "|                                             | 任务名:[南京仙林] |",
  "|     1 ─ ─ ─ 2 ─ ─ ─ 3                      |-------------------|",
  "|    (●)           (●)           (●)          | ① 32.06,118.79 ✕|",
  "|      \\            |            /            | ② 32.06,118.79 ✕|",
  "|       4 ─ ─ ─ 5 ─ ─ ─ 6                     |   高度:100 速度5 |",
  "|      (●)           (●)           (●)          |   悬停:0 优先3   |",
  "|                                             |   动作:巡航      |",
  "|          (MapLibre 真实地图)                |-------------------|",
  "|                                             | 航点数:6         |",
  "|                                             | 距离:2.43km      |",
  "|                                             | 冲突:无          |",
  "|                                             |-------------------|",
  "| [D:412m H:86.4 GS:8.2 VS:+0.6 LAT LON HDG]| [⚡一键优化路径]  |",
  "|                                             | 优化前 2.43km    |",
  "|  点击地图添加航点·拖动调整·右侧编辑参数     | 优化后 1.87km    |",
  "+---------------------------------------------+ 电池预测          |",
  "|                                               距离/负载/风速    |",
  "|                                               [预测续航]        |",
  "|                                               续航 14.2min      |",
  "|                                               -------------------|",
  "|                                               [⬆上传][⬇读取]   |",
  "|                                               [💾保存][📂加载]  |",
  "+---------------------------------------------+-------------------+"
]));
children.push(blank());
children.push(pb("交互说明"));
children.push(bullet("点击地图空白处 → 在该坐标新增航点，自动选中新航点并展开编辑器。"));
children.push(bullet("点击航点列表项 → 展开/收起该航点编辑器，修改后点「应用修改」生效。"));
children.push(bullet("拖拽地图上航点标记 → 实时更新坐标，列表与路径同步刷新。"));
children.push(bullet("航点间距 <50m → 统计区显示「航点过近 <50m」红色提示。"));
children.push(bullet("点「一键优化路径」→ 贪婪最近邻重排航点（Home 不动），显示优化前后总距离对比。"));
children.push(bullet("点「预测续航」→ 输入距离/负载/风速，线性回归模型输出预测续航时间与耗电百分比。"));
children.push(bullet("点「上传到飞控」→ 进度条逐个上传航点，完成后显示「任务已上传」并提示切换到飞行页。"));
children.push(bullet("点「切换底图」→ 在 CARTO 暗色底图与 Esri 卫星影像之间切换。"));
children.push(blank());

// ---- 3.4 调参 ----
children.push(h2("3.4 调参页（Setup）"));
children.push(pb("布局说明"));
children.push(p("调参页采用左右分栏布局：左侧 180px 宽菜单树，分为三组（设置 / 传感器 / 参数与固件），右侧为对应内容区。点击菜单项切换右侧内容，涵盖机架类型选择、电机输出监控、遥控通道标定、串口端口配置、PID 调参、安全设置、IMU 六面校准、罗盘 3D 校准、GPS 状态、气压计、光流传感器、激光测距仪、全参数列表、固件 OTA 升级。"));
children.push(pb("组件清单"));
children.push(t2([
  ["机架类型", "卡片网格：四旋翼X/四旋翼+/六旋翼/八旋翼/三旋翼，SVG 示意图 + CLASS/TYPE 参数"],
  ["电机设置", "12 个电机卡片，垂直条形图 + PWM 值 + 百分比，离线电机半透明"],
  ["遥控器", "12 通道水平条形图，CH3 油门从左填充其余从中心填充，显示 PWM 值与百分比偏移"],
  ["端口设置", "SERIAL1-6 表格，每行：串口号 / 协议下拉(MAVLink2/GPS/关闭) / 波特率下拉 / 写入按钮"],
  ["PID 调参", "四 Tab（姿态环/角速度环/高度控制/位置控制），参数表：参数名/当前值/范围/写入"],
  ["安全设置", "低电量阈值 / 失控保护动作 / 最大飞行高度 / 最大飞行距离"],
  ["IMU 校准", "六面加速度计校准，步骤卡片（水平/左侧/右侧/机头朝上/机头朝下/倒置），逐步确认"],
  ["罗盘校准", "3D 球面校准，进度条动画，开始校准按钮"],
  ["GPS 状态", "状态卡片：定位类型/可见卫星/HDOP/地速 + 当前坐标 + 定位说明"],
  ["气压计", "状态卡片：健康/气压hPa/温度°C + 自动校准说明"],
  ["光流传感器", "状态卡片：质量%/X速度/Y速度/地面距离"],
  ["激光测距仪", "状态卡片：健康/当前距离/最小量程/最大量程"],
  ["参数列表", "可搜索全参数表（ARMING_CHECK/BAT_LOW_VOLT/RTL_ALT 等），参数名/值/类型/写入"],
  ["固件 OTA", "升级警告 + 飞控IP输入 + 固件文件拖放区 + 开始升级按钮"]
], 2400, 6960));
children.push(blank());
children.push(pb("线框图"));
children.push(...wf([
  "+------------------+----------------------------------------------+",
  "| 调参             |  机架类型                                     |",
  "| 设置             |  选择机架后点击应用，重启飞控生效              |",
  "|  > 机架类型      |  +--------+ +--------+ +--------+            |",
  "|    电机设置      |  |四旋翼X | |四旋翼+ | |六旋翼X |            |",
  "|    遥控器        |  | [SVG]  | | [SVG]  | | [SVG]  |            |",
  "|    端口设置      |  |✓当前   | |        | |        |            |",
  "|    PID 调参      |  +--------+ +--------+ +--------+            |",
  "|    安全设置      |  +--------+ +--------+                        |",
  "| 传感器           |  |八旋翼X | |三旋翼Y |                        |",
  "|    IMU 校准      |  +--------+ +--------+                        |",
  "|    罗盘校准      |                                              |",
  "|    GPS           |  --- 点击其他菜单项切换内容 ---               |",
  "|    气压计        |  端口设置:                                   |",
  "|    光流传感器    |  SERIAL1 [MAVLink2▼] [115200▼] [写入]        |",
  "|    激光/测距仪   |  SERIAL2 [GPS▼]      [57600▼]  [写入]        |",
  "| 参数 / 固件      |  ...                                         |",
  "|    参数列表      |  PID调参:                                    |",
  "|    固件升级      |  ATC_RAT_RLL_P [0.135] 范围0-0.5 [写入]     |",
  "+------------------+----------------------------------------------+"
]));
children.push(blank());

// ---- 3.5 数据 ----
children.push(h2("3.5 数据页（Data）"));
children.push(pb("布局说明"));
children.push(p("数据页顶部为三个 Tab（波形监视器 / MAVLink 控制台 / 飞行日志），切换不同视图。波形监视器为 Canvas 实时曲线图 + 右侧通道选择器；MAVLink 控制台为消息列表 + 右侧频率统计面板；飞行日志为左侧日志列表 + 右侧轨迹回放地图。"));
children.push(pb("组件清单"));
children.push(t2([
  ["波形监视器", "Canvas 实时曲线，12 通道（姿态/飞行/电池/GPS），5s/10s/30s/60s 时间窗口，暂停/清空，右侧通道开关"],
  ["MAVLink 控制台", "消息流列表（时间/ID/消息名/SYS/长度/Payload），过滤/暂停/清空，右侧频率统计(Hz) + 数据流频率控制"],
  ["飞行日志", "左侧日志列表（编号/类型/时间/时长/最高高度/距离），右侧轨迹回放地图(MapLibre) + 统计栏（数据点/最高高度/最大速度/最低电压）"],
  ["通道选择器", "按分组（姿态/飞行/电池/GPS）列示通道，点击开关启用/禁用，色点标识通道颜色"],
  ["频率统计", "按消息 ID 统计出现次数，换算为 Hz，高频(>30Hz)主色标识"],
  ["数据流控制", "EXTRA1/EXTRA2/POSITION/RAW_SENSORS/RC_CHANNELS 等数据流频率下拉设置（0-50Hz）"]
], 2400, 6960));
children.push(blank());
children.push(pb("线框图"));
children.push(...wf([
  "+-----------------------------------------------------------------------------+",
  "| [📈 波形监视器] [📡 MAVLink 控制台] [📋 飞行日志]                           |",
  "+-----------------------------------------------------------------------------+",
  "| 波形监视器:                                                                 |",
  "| [5s][10s][30s][60s] | [⏸暂停][清空]  3通道·128样本     | 姿态              |",
  "|                                                     | ● 横滚 Roll  °   |",
  "|     ___                                             | ● 俯仰 Pitch °   |",
  "|    /   \\___       (Canvas 实时曲线)                | ○ 偏航 Yaw   °   |",
  "| __/       \\___                                    |------------------|",
  "|                    ___                            | 飞行             |",
  "|                ___/   \\___                        | ● 相对高度  m    |",
  "|                               [通道开关列表]      | ○ 爬升率   m/s   |",
  "|                                                     | ...              |",
  "+-----------------------------------------------------------------------------+",
  "| MAVLink 控制台:                                          | 频率统计        |",
  "| [过滤...] [⏸暂停][清空][频率统计]  128条              | HEARTBEAT  1.0Hz|",
  "| 时间        ID  消息名      SYS 长度 Payload          | ATTITUDE   8.3Hz|",
  "| 14:22:36.012 22  ATTITUDE   1   8   0a 1f ...        | GPS_RAW    1.0Hz|",
  "| 14:22:36.008  0   HEARTBEAT  1   8   01 02 ...        |------------------|",
  "| ...                                                    | 数据流频率控制  |",
  "|                                                        | EXTRA1  [50Hz▼] |",
  "+-----------------------------------------------------------------------------+",
  "| 飞行日志:                                                                  |",
  "| +-----------------+  +------------------------------------+               |",
  "| | 飞行记录  [刷新]|  | 飞行记录 #1                        |               |",
  "| | #1 SITL-Quad   ||  | 开始: 2026-08-31 14:19  时长:16分20|               |",
  "| | 14:19 16m 102m ||  |                                    |               |",
  "| | #2 SITL-Quad   ||  |     (MapLibre 轨迹回放地图)        |               |",
  "| | 10:15 13m 85m  ||  |                                    |               |",
  "| | #3 SITL-Quad   ||  |------------------------------------|               |",
  "| | 15:42 13m 77m  ||  | 490数据点 | 102.5m | 12.3m/s | 11.2V|             |",
  "| +-----------------+  +------------------------------------+               |",
  "+-----------------------------------------------------------------------------+"
]));
children.push(blank());

// ======================== 第四章 ========================
children.push(h1("第四章  交互流程设计"));

children.push(h2("4.1 任务规划与路径优化流程"));
children.push(...wf([
  "  开始                                                       ",
  "   │                                                         ",
  "   ▼                                                         ",
  "进入规划页 ──► 点击地图添加航点 ──► 点击航点展开编辑器        ",
  "   │                                 │                       ",
  "   │                                 ▼                       ",
  "   │                          设置高度/速度/悬停/动作/优先级  ",
  "   │                                 │                       ",
  "   ▼                                 ▼                       ",
  "冲突检测(自动) ──► 有冲突? ──是──► 统计区标红提示修改         ",
  "   │                  │                                      ",
  "   否                 └──────────┐                           ",
  "   ▼                             ▼                           ",
  "一键优化(贪婪最近邻) ──► 显示优化前后距离 ──► 确认重排        ",
  "   │                                                         ",
  "   ▼                                                         ",
  "电池预测(距离/负载/风速→续航) ──► 评估是否可行               ",
  "   │                                                         ",
  "   ▼                                                         ",
  "上传到飞控(进度条) / 保存XML ──► 提示切换飞行页执行           "
]));
children.push(blank());

children.push(h2("4.2 飞行控制流程"));
children.push(...wf([
  "  进入飞行页                                                 ",
  "   │                                                         ",
  "   ▼                                                         ",
  "确认已连接 ──否──► 提示检查通信参数                          ",
  "   │是                                                       ",
  "   ▼                                                         ",
  "解锁(二次确认) ──► 起飞(二次确认,默认2m)                     ",
  "   │                                                         ",
  "   ▼                                                         ",
  "开始任务(二次确认) ──► 按航点序列自动飞行                     ",
  "   │                                                         ",
  "   ├──► 实时监控:地图位置/HUD/ADI/告警面板                    ",
  "   │                                                         ",
  "   ├──► 电量<15%? ──是──► 数值红色,建议返航                   ",
  "   │                                                         ",
  "   ▼                                                         ",
  "任务完成 / 手动干预                                          ",
  "   ├──返航(RTL,二次确认)──► 返回起飞点                        ",
  "   ├──降落(二次确认)──► 原地降落                             ",
  "   └──悬停(二次确认)──► 当前位置悬停                         "
]));
children.push(blank());

children.push(h2("4.3 数据管理流程"));
children.push(p("数据页提供三种数据视图：波形监视器实时绘制遥测通道曲线（采样间隔 200ms，可暂停/清空/切换时间窗口）；MAVLink 控制台实时显示飞控消息流（含消息 ID、名称、Payload，支持过滤与频率统计）；飞行日志列表展示历史飞行记录，点击后在 MapLibre 地图上回放飞行轨迹。任务文件支持 XML 导入导出，格式错误弹窗提示并保留原状态。"));
children.push(blank());

children.push(h2("4.4 异常处理交互"));
children.push(t3([
  ["异常场景", "触发位置", "界面交互"],
  ["连接失败/中断", "飞控通信层", "导航栏连接指示灯变红 + 弹窗提示"],
  ["任务文件格式错误", "XML 读入校验", "弹窗报具体原因，内存中原任务不受影响"],
  ["航点坐标过近(<50m)", "冲突检测", "统计区显示红色「航点过近 <50m」提示"],
  ["航点数不足优化", "路径优化", "Toast 提示「至少需要 3 个航点」"],
  ["低电量", "遥测电池<阈值", "数值颜色变化（<15%红/<30%橙/正常绿）"]
], 2000, 2400, 4960));
children.push(blank());

// ======================== 第五章 ========================
children.push(h1("第五章  视觉规范"));

children.push(h2("5.1 色彩规范（CGC 设计 Token）"));
children.push(p("严格提取自 CGC 网站前端 CSS 变量，深色暗色主题为唯一主题："));
children.push(t3([
  ["语义", "变量名", "色值"],
  ["主色（强调/激活）", "--primary", "#F0A500"],
  ["辅助蓝", "--accent-blue / --blue-highlight", "#4B8BF5 / #3B82F6"],
  ["正常/绿", "--success", "#22C55E"],
  ["告警/橙", "--warning", "#F59E0B"],
  ["故障/红", "--danger", "#EF4444"],
  ["信息/灰", "--info", "#8B949E"],
  ["基底背景", "--bg-base", "#0D1117"],
  ["面板背景", "--bg-panel", "#161B22"],
  ["悬停背景", "--bg-hover", "#1C2128"],
  ["条栏背景", "--bg-bar", "#21262D"],
  ["边框默认", "--border-default", "#21262D"],
  ["边框悬停", "--border-hover", "#30363D"],
  ["正文", "--text-primary", "#E6EDF3"],
  ["次要文字", "--text-secondary", "#C9D1D9"],
  ["弱化文字", "--text-muted", "#484F58"]
], 2200, 2600, 4160));
children.push(blank());

children.push(h2("5.2 字体与排版"));
children.push(bullet("正文：无衬线（-apple-system / Segoe UI / Microsoft YaHei），基础字号 11px，行高 1.5。"));
children.push(bullet("数值：等宽字体 JetBrains Mono，font-variant-numeric: tabular-nums，保证跳动时不抖动。"));
children.push(bullet("导航栏 Tab：12px；卡片标题：12px 加粗；HUD 数值：15px 加粗；遥测数值：14px 加粗。"));
children.push(bullet("标签/单位：9-10px 辅助色(#8B949E)，与数值分离显示。"));
children.push(bullet("圆角：卡片 10px，浮层/按钮 6-8px，徽章/指示灯 50%。"));
children.push(bullet("阴影：地图浮层 box-shadow 0 4px 16px #00000073；毛玻璃 backdrop-filter: blur(10px)。"));
children.push(blank());

children.push(h2("5.3 状态色与预警机制"));
children.push(p("全系统统一状态语义：绿(#22C55E)= 正常/已连接/ARMED；橙(#F59E0B)= 警告/弱信号；红(#EF4444)= 故障/低电量/冲突；灰(#484F58)= 离线/未知。导航栏连接指示灯：绿色实心圆 + 发光阴影表示已连接，红色表示断开。飞行页电池电量 <15% 数值红色，<30% 橙色，正常绿色，颜色实时随遥测数据变化。"));
children.push(blank());

// ======================== 第六章 ========================
children.push(h1("第六章  需求追溯表"));
children.push(p("下表逐条验证界面设计对任务书需求与参考站功能的覆盖："));
children.push(t3([
  ["需求编号", "需求内容", "界面落点"],
  ["R1.1", "XML 任务文件导入导出", "规划页任务操作区（保存XML / 加载XML）"],
  ["R1.2", "航点手动编辑(新增/修改/删除)", "规划页地图点击新增 + 航点列表 + 编辑器"],
  ["R2.1", "状态面板(位置/高度/速度/电池/信号)", "仪表盘遥测卡片 + 飞行页 HUD/底部 HUD 条"],
  ["R2.2", "地图显示区(实时位置+航点路径)", "飞行页/规划页 MapLibre 真实地图 + 无人机标记 + 路径线"],
  ["R2.3", "传感器数据图表", "数据页波形监视器(Canvas 实时曲线) + 仪表盘传感器矩阵"],
  ["R3.1", "地图点击加航点、飞行模式、优先级", "规划页地图点击 + 航点编辑器(动作/优先级)"],
  ["R3.2", "航点序列预览、冲突检测", "规划页路径线 + 冲突检测(<50m 红色提示)"],
  ["R4.1", "通信参数/地图缩放/采样间隔", "调参页端口设置 + 地图缩放控件 + 波形监视器采样"],
  ["R4.2", "日志备份恢复/筛选", "数据页飞行日志列表 + MAVLink 控制台 + CSV 日志管理"],
  ["R5.1", "贪婪最短路径优化(禁第三方)", "规划页一键优化(贪婪最近邻) + 前后距离对比"],
  ["R5.2", "线性回归电池预测", "规划页电池预测(距离/负载/风速 → 续航/耗电)"],
  ["R6.1", "界面布局规范", "顶部导航栏 + 五页独立布局(对标 CGC)"],
  ["R6.2", "CSS 主题 + 低电量预警", "深色主题(CGC Token) + 电池颜色三级预警"],
  ["R7.3", "异常处理(连接/文件/越界)", "第四章 4.4 节异常处理交互矩阵"]
], 1400, 3800, 4160));
children.push(blank());
children.push(p("结论：本界面设计完整覆盖任务书 R1-R7 全部条款，完全对标琛光 CGC 地面站的五页功能结构与视觉设计规范，集成 MapLibre GL JS 真实地图渲染，满足清晰可读、美观、简单易懂、符合用户习惯的要求。配套高保真可交互原型见「交互界面原型.html」。"));

// ======================== 组装文档 ========================
const doc = new Document({
  styles: {
    default: { document: { run: { font: F, size: 22 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 32, bold: true, font: F }, paragraph: { spacing: { before: 360, after: 200 }, outlineLevel: 0, keepNext: false, keepLines: false } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 26, bold: true, font: F }, paragraph: { spacing: { before: 240, after: 120 }, outlineLevel: 1, keepNext: false, keepLines: false } },
    ]
  },
  numbering: { config: [
    { reference: "bul", levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT,
      style: { paragraph: { indent: { left: 600, hanging: 300 } } } }] }
  ] },
  sections: [{
    properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } } },
    headers: { default: new Header({ children: [new Paragraph({ alignment: AlignmentType.RIGHT,
      children: [new TextRun({ text: "无人机地面站系统 · 交互界面设计说明书", font: F, size: 18, color: "999999" })] })] }) },
    footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER,
      children: [new TextRun({ text: "第 ", font: F, size: 18, color: "999999" }), new TextRun({ children: [PageNumber.CURRENT], font: F, size: 18, color: "999999" }), new TextRun({ text: " 页", font: F, size: 18, color: "999999" })] })] }) },
    children
  }]
});

Packer.toBuffer(doc).then(buf => {
  fs.writeFileSync("c:\\Users\\wjh22\\Desktop\\无人机地面站-课设文稿\\交互界面设计说明书.docx", buf);
  console.log("docx generated OK");
});
