# -*- coding: utf-8 -*-
"""
无人机地面站系统 思维导图生成器 v4
真正的树状思维导图：根节点+分支+贝塞尔曲线连接
图1: 双向思维导图(中心放射) - 需求分析过程
图2: 左→右逻辑结构图 - 功能完备性
图3: 上→下组织架构图 - 通讯架构
"""
import os, html, subprocess
from PIL import ImageFont
import PIL.Image as PILImage
PILImage.MAX_IMAGE_PIXELS = None

FR = "C:/Windows/Fonts/msyh.ttc"
FB = "C:/Windows/Fonts/msyhbd.ttc"
def F(path, sz): return ImageFont.truetype(path, sz)

class Node:
    def __init__(self, title, src="", desc="", color="#4a5a8a", children=None):
        self.title = title
        self.src = src
        self.desc = desc
        self.color = color
        self.children = children or []
        self.x = self.y = self.w = self.h = 0

def wrap(text, font, max_w):
    lines = []
    for para in text.split("\n"):
        if not para.strip(): lines.append(""); continue
        cur = ""
        for ch in para:
            if font.getlength(cur + ch) > max_w and cur:
                lines.append(cur); cur = ch
            else: cur += ch
        if cur: lines.append(cur)
    return lines

def node_h(node, w, ft, fd, fs):
    iw = w - 28
    h = 12
    h += len(wrap(node.title, ft, iw)) * (ft.size + 4)
    if node.desc:
        h += 4 + len(wrap(node.desc, fd, iw)) * (fd.size + 3)
    if node.src:
        h += 4 + len(wrap(node.src, fs, iw)) * (fs.size + 2)
    return max(h, 34) + 8

# ===== 左→右布局 =====
def layout_lr(n, x, ys, xg, yg, w, ft, fd, fs):
    n.x = x; n.w = w; n.h = node_h(n, w, ft, fd, fs)
    if not n.children:
        n.y = ys; return n.h + yg
    th = 0
    for c in n.children:
        th += layout_lr(c, x + w + xg, ys + th, xg, yg, w, ft, fd, fs)
    fy = n.children[0].y; ly = n.children[-1].y + n.children[-1].h
    n.y = (fy + ly) / 2 - n.h / 2
    return max(th, n.h + yg)

# ===== 右→左布局(镜像) =====
def layout_rl(n, xr, ys, xg, yg, w, ft, fd, fs):
    n.w = w; n.x = xr - w; n.h = node_h(n, w, ft, fd, fs)
    if not n.children:
        n.y = ys; return n.h + yg
    th = 0
    for c in n.children:
        th += layout_rl(c, n.x, ys + th, xg, yg, w, ft, fd, fs)
    fy = n.children[0].y; ly = n.children[-1].y + n.children[-1].h
    n.y = (fy + ly) / 2 - n.h / 2
    return max(th, n.h + yg)

# ===== 上→下布局 =====
def layout_tb(n, xs, y, xg, yg, w, ft, fd, fs):
    n.w = w; n.h = node_h(n, w, ft, fd, fs); n.y = y
    if not n.children:
        n.x = xs; return w + xg
    tw = 0
    for c in n.children:
        tw += layout_tb(c, xs + tw, y + n.h + yg, xg, yg, w, ft, fd, fs)
    fx = n.children[0].x; lx = n.children[-1].x + n.children[-1].w
    n.x = (fx + lx) / 2 - n.w / 2
    return max(tw, w + xg)

# ===== SVG绘制 =====
def esc(t): return html.escape(t, quote=True)

def draw_node(n, ft, fd, fs):
    x, y, w, h, c = n.x, n.y, n.w, n.h, n.color
    s = [f'<rect x="{x:.0f}" y="{y:.0f}" width="{w:.0f}" height="{h:.0f}" rx="8" fill="white" stroke="#c0ccd8" stroke-width="1"/>']
    s.append(f'<rect x="{x:.0f}" y="{y:.0f}" width="5" height="{h:.0f}" rx="2" fill="{c}"/>')
    iw = w - 28; ty = y + 12; ix = x + 14
    for ln in wrap(n.title, ft, iw):
        s.append(f'<text x="{ix:.0f}" y="{ty+ft.size:.0f}" font-size="{ft.size}" font-weight="bold" fill="#1c3764" font-family="Microsoft YaHei">{esc(ln)}</text>')
        ty += ft.size + 4
    if n.desc:
        ty += 4
        for ln in wrap(n.desc, fd, iw):
            s.append(f'<text x="{ix:.0f}" y="{ty+fd.size:.0f}" font-size="{fd.size}" fill="#3a4560" font-family="Microsoft YaHei">{esc(ln)}</text>')
            ty += fd.size + 3
    if n.src:
        ty += 4
        for ln in wrap(n.src, fs, iw):
            s.append(f'<text x="{ix:.0f}" y="{ty+fs.size:.0f}" font-size="{fs.size}" fill="#8892a8" font-family="Microsoft YaHei">{esc(ln)}</text>')
            ty += fs.size + 2
    return "\n".join(s)

def conn_lr(p, c, xg):
    x1=p.x+p.w; y1=p.y+p.h/2; x2=c.x; y2=c.y+c.h/2; cx=x1+xg/2
    return f'<path d="M {x1:.0f} {y1:.0f} C {cx:.0f} {y1:.0f}, {cx:.0f} {y2:.0f}, {x2:.0f} {y2:.0f}" stroke="#b8c4d8" stroke-width="2" fill="none"/>'

def conn_rl(p, c, xg):
    x1=p.x; y1=p.y+p.h/2; x2=c.x+c.w; y2=c.y+c.h/2; cx=x1-xg/2
    return f'<path d="M {x1:.0f} {y1:.0f} C {cx:.0f} {y1:.0f}, {cx:.0f} {y2:.0f}, {x2:.0f} {y2:.0f}" stroke="#b8c4d8" stroke-width="2" fill="none"/>'

def conn_tb(p, c, yg):
    x1=p.x+p.w/2; y1=p.y+p.h; x2=c.x+c.w/2; y2=c.y; cy=y1+yg/2
    return f'<path d="M {x1:.0f} {y1:.0f} C {x1:.0f} {cy:.0f}, {x2:.0f} {cy:.0f}, {x2:.0f} {y2:.0f}" stroke="#b8c4d8" stroke-width="2" fill="none"/>'

def draw_conns(n, conn_fn, xg):
    parts = []
    for c in n.children:
        parts.append(conn_fn(n, c, xg))
        parts.extend(draw_conns(c, conn_fn, xg))
    return parts

def draw_all(n, ft, fd, fs):
    parts = [draw_node(n, ft, fd, fs)]
    for c in n.children:
        parts.append(draw_node(c, ft, fd, fs))
        parts.append(draw_all(c, ft, fd, fs))
    return parts

def bounds(n):
    xs = [n.x, n.x+n.w]; ys = [n.y, n.y+n.h]
    for c in n.children:
        cx0,cy0,cx1,cy1 = bounds(c)
        xs += [cx0, cx1]; ys += [cy0, cy1]
    return min(xs), min(ys), max(xs), max(ys)

def gen_svg(root, layout_fn, conn_fn, xg, yg, w, ft, fd, fs, x_init, y_init):
    if layout_fn == layout_lr:
        layout_lr(root, x_init, y_init, xg, yg, w, ft, fd, fs)
    elif layout_fn == layout_tb:
        layout_tb(root, x_init, y_init, xg, yg, w, ft, fd, fs)
    x0,y0,x1,y1 = bounds(root)
    pad = 40
    cw = x1-x0+pad*2; ch = y1-y0+pad*2
    dx = pad - x0; dy = pad - y0
    def shift(n):
        n.x += dx; n.y += dy
        for c in n.children: shift(c)
    shift(root)
    parts = [f'<svg width="{cw:.0f}" height="{ch:.0f}" xmlns="http://www.w3.org/2000/svg">']
    parts.append(f'<rect width="100%" height="100%" fill="#f8f9fb"/>')
    parts.extend(draw_conns(root, conn_fn, xg))
    parts.append(draw_node(root, ft, fd, fs))
    def draw_subs(n):
        for c in n.children:
            parts.append(draw_node(c, ft, fd, fs))
            draw_subs(c)
    draw_subs(root)
    parts.append('</svg>')
    return "\n".join(parts), cw, ch

def wrap_html(svg, title, sub):
    return f'''<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8">
<style>*{{margin:0;padding:0;box-sizing:border-box}}
body{{font-family:"Microsoft YaHei";background:#f0f2f5;padding:24px}}
.h{{background:linear-gradient(135deg,#1c3764,#2a5290);color:#fff;padding:22px 32px;border-radius:10px;margin-bottom:16px}}
.h h1{{font-size:26px;margin-bottom:4px}}.h p{{font-size:14px;color:#b8c8de}}
.leg{{margin-top:16px;background:#e8edf5;border-radius:8px;padding:16px 22px}}
.leg h3{{font-size:16px;color:#16285a;margin-bottom:8px}}
.leg p{{font-size:14px;color:#4a5570;line-height:1.7}}
</style></head><body>
<div class="h"><h1>{esc(title)}</h1><p>{esc(sub)}</p></div>
{svg}
<div class="leg"><h3>图例·来源标注体系</h3>
<p>【源：文档名·章节】任务书·R1.1 / 02-需求分析.md·UC4 / 03-系统设计.md·第四节</p>
<p>【源：网址】github.com/songge8/CF-Drone / mavlink.io / ardupilot.org / gc.cherglowtech.com</p>
<p>【源：系统模块】CF-Drone固件·mavlink.ino / CGC前端·mavlink_pretty.js</p>
<p>【源：法规标准】GB 42590-2023 / ASTM F3411 ｜【源：对话分析·轮次】对话[24]语义二分结论</p>
<p>R# = 任务书第33题需求编号：R1数据管理/R2实时监控/R3任务规划/R4系统设置/R5 AI算法/R6 UI/R7 OO技术</p>
</div></body></html>'''

# ===== 图1: 双向思维导图 =====
def gen_image1(out_dir):
    ft,fd,fs = F(FB,15),F(FR,13),F(FR,11)
    w, xg, yg = 210, 45, 10
    root = Node("需求分析", "", "带着任务书硬约束\n遇问题→查资料→做决策", "#1c3764")
    left = [
        Node("① 课程任务书", "【源：任务书.doc·第33题】", "硬约束·起点", "#2d6eb4", [
            Node("R1-R7 七类需求条款", "【源：任务书·功能需求1-4】", "数据/监控/规划/设置/AI/UI/OO"),
            Node("硬卡点：要串口但没真机", "【源：任务书·R4.1串口/波特率】"),
            Node("确认：只验收软件不要求真机", "【源：对话[5]分析结论】"),
        ]),
        Node("② 业内开源调研", "【源：github.com/mavlink/qgroundcontrol】", "对标·找参考", "#288c5a", [
            Node("QGC/MP/MAVProxy/Tower", "【源：github.com 各仓库】", "全C++/C#/Python，无JavaFX"),
            Node("决策：抄架构不抄源码", "【源：对话[3]调研结论】", "QGC三视图+MP布局"),
            Node("SITL输出标准MAVLink", "【源：ardupilot.org·SITL文档】"),
        ]),
        Node("③ 实机与固件", "【源：github.com/songge8/CF-Drone】", "用户要实物·顺藤摸瓜", "#c03c3c", [
            Node("ESP32四轴套件+MPU6500", "【源：对话[28]商品清单】"),
            Node("固件原生说MAVLink UDP 14550", "【源：CF-Drone·wifi.ino】", "与SITL零改动切换"),
        ]),
        Node("④ 仿真与行业标准", "【源：ardupilot.org/mavlink.io】", "无真机也有真遥测", "#28828c", [
            Node("SITL=真飞控固件非假数据", "【源：ardupilot.org·SITL】"),
            Node("RemoteID/ADS-B可收不可控", "【源：GB 42590-2023】", "定位为彩蛋"),
        ]),
        Node("⑤ 官方网页GCS对标", "【源：gc.cherglowtech.com】", "第二证人", "#8246aa", [
            Node("Vue+MapLibre五页结构", "【源：CGC前端·mavlink_pretty.js】"),
            Node("Haversine与自研GeoUtil一致", "【源：CGC提取README·第4点】"),
        ]),
    ]
    right = [
        Node("决策① 自研路线", "【源：对话[4]改造方案】", "语言栈不通→只抄架构", "#233248"),
        Node("决策② 双数据源", "【源：03-系统设计.md·E1对接】", "SITL供位置/E1供姿态电池", "#2d6eb4"),
        Node("决策③ 语义二分", "【源：对话[24]语义二分结论】", "收遥测=广播/发指令=握手", "#288c5a"),
        Node("决策④ 控制边界", "【源：对话[12]控制边界】", "任务级不做摇杆级", "#c03c3c"),
        Node("决策⑤ 三闭环保底", "【源：04-进度计划.md·裁剪规则】", "保遥测/规划/日志", "#d28228"),
    ]

    # 1. 根节点尺寸
    root.w = w; root.h = node_h(root, w, ft, fd, fs)

    # 2. 布局右侧子树（从x=0临时开始）
    rh = 0
    for c in right:
        rh += layout_lr(c, 0, rh, xg, yg, w, ft, fd, fs)
    # 3. 布局左侧子树（从x=w临时开始，向左展开）
    lh = 0
    for c in left:
        lh += layout_rl(c, w, lh, xg, yg, w, ft, fd, fs)

    # 4. 总高度
    total_h = max(rh, lh, root.h)
    # 根节点居中
    root.x = 0; root.y = total_h / 2 - root.h / 2

    # 5. 移动右侧子树到根右侧
    rx_target = root.x + root.w + xg
    ry_offset = (total_h - rh) / 2
    for c in right:
        def shift(n, dx, dy):
            n.x += dx; n.y += dy
            for ch in n.children: shift(ch, dx, dy)
        shift(c, rx_target, ry_offset)

    # 6. 移动左侧子树到根左侧
    lx_target = root.x - xg  # 左侧子树右边缘
    ly_offset = (total_h - lh) / 2
    for c in left:
        def shift2(n, dx, dy):
            n.x += dx; n.y += dy
            for ch in n.children: shift2(ch, dx, dy)
        # layout_rl设置c.x = xr - w，xr=w，所以c.x = 0，需要移到 lx_target - w
        shift2(c, lx_target - c.x - c.w, ly_offset)

    # 7. 计算bounds
    def collect(n, lst):
        lst.append(n)
        for c in n.children: collect(c, lst)
    nodes = [root]
    for c in left + right: collect(c, nodes)
    xs = [n.x for n in nodes] + [n.x + n.w for n in nodes]
    ys = [n.y for n in nodes] + [n.y + n.h for n in nodes]
    x0, y0, x1, y1 = min(xs), min(ys), max(xs), max(ys)
    pad = 40
    cw = x1 - x0 + pad * 2; ch = y1 - y0 + pad * 2
    dx = pad - x0; dy = pad - y0
    for n in nodes: n.x += dx; n.y += dy

    # 8. 生成SVG
    parts = [f'<svg width="{cw:.0f}" height="{ch:.0f}" xmlns="http://www.w3.org/2000/svg">']
    parts.append(f'<rect width="100%" height="100%" fill="#f8f9fb"/>')
    for c in left:
        parts.append(conn_rl(root, c, xg))
        parts.extend(draw_conns(c, conn_rl, xg))
    for c in right:
        parts.append(conn_lr(root, c, xg))
        parts.extend(draw_conns(c, conn_lr, xg))
    parts.append(draw_node(root, ft, fd, fs))
    def draw_sub(n):
        parts.append(draw_node(n, ft, fd, fs))
        for ch in n.children: draw_sub(ch)
    for c in left + right: draw_sub(c)
    parts.append('</svg>')
    svg = "\n".join(parts)
    h = wrap_html(svg, "图1·需求来源全景与分析推导链", "中心放射思维导图：左侧5路输入→中心需求分析→右侧5条决策")
    hp = os.path.join(out_dir, "图1-需求分析过程与思路.html")
    with open(hp, "w", encoding="utf-8") as f: f.write(h)
    return hp

# ===== 图2: 左→右逻辑结构图 =====
def gen_image2(out_dir):
    ft,fd,fs = F(FB,15),F(FR,13),F(FR,11)
    w, xg, yg = 215, 45, 10
    root = Node("无人机地面站系统", "", "任务书第33题·功能完备性全景", "#1c3764", [
        Node("R1 数据管理", "【源：任务书·功能需求1】", "XML/航点/集合类", "#2d6eb4", [
            Node("R1.1 导入导出XML任务文件", "【源：任务书·R1.1】【源：03-系统设计.md·第六节】", "XMLEncoder读写+字段校验"),
            Node("R1.2 航点手动编辑", "【源：任务书·R1.1】【源：02-需求分析.md·UC3】", "列表+地图拖拽双向联动"),
            Node("R1.3 集合类管理数据", "【源：任务书·R7.1】", "LinkedList航点/HashMap参数"),
        ]),
        Node("R2 实时监控", "【源：任务书·功能需求2】", "状态/地图/图表", "#288c5a", [
            Node("R2.1 状态面板", "【源：任务书·R2.1】【源：03-系统设计.md·第三节】", "位置/高度/速度/电池/信号"),
            Node("R2.2 地图显示区", "【源：任务书·R2.2】【源：03-系统设计.md·第五节】", "OSM瓦片+Web Mercator投影"),
            Node("R2.3 传感器数据图表", "【源：任务书·R2.3】【源：任务书·R7.1】", "Canvas+LineChart动态曲线"),
        ]),
        Node("R3 任务规划", "【源：任务书·功能需求3】", "航点+冲突检测", "#c03c3c", [
            Node("R3.1 航点添加与飞行模式", "【源：任务书·R3.1】【源：02-需求分析.md·数据字典】", "CRUISE/LOITER/TAKEPHOTO"),
            Node("R3.2 航点预览与冲突检测", "【源：任务书·R3.2】【源：03-系统设计.md·ConflictChecker】", "<50m标红+超航程标红"),
        ]),
        Node("R4 系统设置", "【源：任务书·功能需求4】", "通信参数+日志", "#d28228", [
            Node("R4.1 通信参数配置", "【源：任务书·R4.1】【源：03-系统设计.md·CommunicationService】", "串口/波特率/采样间隔"),
            Node("R4.2 日志备份与恢复", "【源：任务书·R4.2】【源：03-系统设计.md·CsvLogStore】", "CSV备份+按日期/型号筛选"),
        ]),
        Node("R5 AI算法", "【源：任务书·AI功能·禁第三方库】", "贪婪+线性回归", "#8246aa", [
            Node("R5.1 贪婪最短路径", "【源：任务书·AI功能1】【源：03-系统设计.md·PathOptimizer】", "O(n²)最近邻+2-opt对比"),
            Node("R5.2 电池电量预测", "【源：任务书·AI功能2】【源：03-系统设计.md·BatteryPredictor】", "线性回归·手写3×3矩阵求逆"),
        ]),
        Node("R6 JavaFX UI", "【源：任务书·UI要求】", "FXML+CSS主题", "#4a5a8a", [
            Node("R6.1 SceneBuilder+FXML", "【源：任务书·UI要求1】【源：03-系统设计.md·第一节】", "四区布局+Property绑定"),
            Node("R6.2 主题切换与预警", "【源：任务书·UI要求2】【源：03-系统设计.md·ThemeManager】", "白天/夜间CSS+低电量闪烁"),
        ]),
        Node("R7 面向对象", "【源：任务书·OO要求】", "系统类+自定义类+异常", "#3a6a5a", [
            Node("R7.1 系统类使用", "【源：任务书·OO要求1】【源：03-系统设计.md·第二节】", "File/XML/BufferedReader/Timer/Canvas/LineChart"),
            Node("R7.2 四个自定义类", "【源：任务书·OO要求2】【源：03-系统设计.md·第三节】", "Drone/Waypoint/PathOptimizer/CommService"),
            Node("R7.3 设计原则与异常处理", "【源：任务书·OO要求3】【源：03-系统设计.md·第八节】", "封装/单一职责+连接失败/格式错误/坐标越界"),
        ]),
        Node("用例(11个·含异常流)", "【源：02-需求分析.md·第二节】", "UC1-UC11", "#8a5a30", [
            Node("UC1-UC3 文件与编辑", "【源：02-需求分析.md·UC1-3】", "导入→格式错误弹窗/导出→不可写报错/编辑→越界标红"),
            Node("UC4-UC8 监控与规划", "【源：02-需求分析.md·UC4-8】", "断线→重连3次退手动/冲突→标红/预测→参数校验"),
            Node("UC9-UC11 日志与设置", "【源：02-需求分析.md·UC9-11】", "CSV坏行→跳过计数/设置越界→恢复默认/主题切换"),
        ]),
        Node("非功能需求(5条)", "【源：02-需求分析.md·第三节】", "性能/可靠/维护/合规/演示", "#5a5070", [
            Node("性能·UI不卡顿", "【源：02-需求分析.md·非功能1】", "遥测与FX线程隔离"),
            Node("可靠性·自动重连", "【源：02-需求分析.md·非功能2】", "指数退避3次转手动"),
            Node("合规·零第三方依赖", "【源：02-需求分析.md·非功能4】", "开源仅借架构代码全自写"),
            Node("演示稳健性·CSV回放", "【源：02-需求分析.md·非功能5】", "不依赖SITL存活"),
        ]),
        Node("数据字典", "【源：02-需求分析.md·第四节】", "关键字段约束", "#5a4060", [
            Node("航点约束", "【源：02-需求分析.md·数据字典】", "|lat|≤90 |lon|≤180 alt>0 hold≥0"),
            Node("动作/优先级枚举", "【源：02-需求分析.md·数据字典】", "CRUISE/LOITER/TAKEPHOTO priority 1-5"),
        ]),
    ])
    svg, cw, ch = gen_svg(root, layout_lr, conn_lr, xg, yg, w, ft, fd, fs, 40, 40)
    h = wrap_html(svg, "图2·系统功能完备性全景", "左→右逻辑结构图：根→R1-R7+用例+非功能+数据字典→子需求（含来源标注）")
    hp = os.path.join(out_dir, "图2-系统功能完备性全景.html")
    with open(hp, "w", encoding="utf-8") as f: f.write(h)
    return hp

# ===== 图3: 上→下组织架构图 =====
def gen_image3(out_dir):
    ft,fd,fs = F(FB,15),F(FR,13),F(FR,11)
    w, xg, yg = 210, 25, 50
    root = Node("通讯架构", "", "物理→协议→语义→数据源→验证", "#1c3764", [
        Node("物理链路层", "【源：ardupilot.org·SITL】【源：CF-Drone·wifi.ino】", "换介质不换协议", "#2d6eb4", [
            Node("SITL模拟(主线)", "【源：ardupilot.org·SITL文档】", "本机UDP 127.0.0.1:14550"),
            Node("E1实机WiFi AP", "【源：CF-Drone·wifi.ino】【源：对话[35]】", "UDP 14550同端口零改动"),
            Node("串口电台(野外)", "【源：对话[20]无线链路】【源：任务书·R4.1】", "433/915MHz→USB→COM口"),
        ]),
        Node("MAVLink协议层", "【源：mavlink.io·协议规范】", "帧结构+报文清单", "#288c5a", [
            Node("帧结构", "【源：mavlink.io】【源：03-系统设计.md·第四节】", "0xFE|LEN|SEQ|SYSID|COMPID|MSGID|PAYLOAD|CRC"),
            Node("上行报文(广播)", "【源：mavlink.io·消息定义】", "HEARTBEAT/SYS_STATUS/POSITION_INT/VFR_HUD"),
            Node("下行报文(握手)", "【源：mavlink.io·消息定义】", "COMMAND_LONG/MISSION_REQUEST/ACK"),
            Node("E1实机报文差异", "【源：CF-Drone·mavlink.ino】", "ATTITUDE 10Hz/无GPS/位置由SITL补"),
        ]),
        Node("语义二分(核心设计)", "【源：对话[24]语义二分结论】", "一套协议两种语义", "#8246aa", [
            Node("遥测=广播式", "【源：对话[24]】【源：03-系统设计.md·第四节】", "飞控只管推谁连谁看"),
            Node("指令=握手式", "【源：对话[24]】【源：mavlink.io·MISSION协议】", "发COMMAND_LONG必须等ACK"),
        ]),
        Node("数据源生态", "【源：对话[23]数据源总谱】", "四类各司其职", "#d28228", [
            Node("SITL模拟", "【源：ardupilot.org·SITL】", "¥0·必做·主线"),
            Node("E1实机", "【源：github.com/songge8/CF-Drone】", "¥100-200·强烈建议"),
            Node("ADS-B航班", "【源：dump1090·1090MHz】", "¥100-200·可选彩蛋"),
            Node("RemoteID", "【源：GB 42590-2023】", "¥0·答辩话术"),
        ]),
        Node("四证人验证链", "【源：对话[14]证据链四步法】", "不自己给自己作证", "#c03c3c", [
            Node("①SITL回执", "【源：对话[14]】【源：ardupilot.org】", "发指令→ACK→航迹变化"),
            Node("②MP同屏对账", "【源：对话[14]】【源：github.com/ArduPilot/MissionPlanner】", "官方独立实现同屏看"),
            Node("③Wireshark抓包", "【源：对话[18]验收级铁证】", "0xFE原始字节流肉眼可辨"),
            Node("④CGC同屏比对", "【源：gc.cherglowtech.com】【源：对话[35]】", "官方网页GCS第二证人"),
        ]),
    ])
    svg, cw, ch = gen_svg(root, layout_tb, conn_tb, xg, yg, w, ft, fd, fs, 40, 40)
    h = wrap_html(svg, "图3·通讯架构·数据流·验证证据链", "上→下组织架构图：物理链路→MAVLink协议→语义二分→数据源→四证人验证")
    hp = os.path.join(out_dir, "图3-通讯架构与数据流.html")
    with open(hp, "w", encoding="utf-8") as f: f.write(h)
    return hp

def render_png(html_path, png_path, width=3200):
    import re
    with open(html_path, "r", encoding="utf-8") as f:
        content = f.read()
    m = re.search(r'<svg width="([\d.]+)" height="([\d.]+)"', content)
    if not m:
        print(f"ERROR: no SVG found in {html_path}"); return
    svg_w, svg_h = float(m.group(1)), float(m.group(2))
    # 标题约80 + 图例约200 + padding 48 = 328
    total_h = int(svg_h + 340)
    total_w = max(int(svg_w) + 60, 1200)
    h = html_path.replace("\\","/")
    subprocess.run([
        "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
        "--headless", "--disable-gpu", "--force-device-scale-factor=2",
        f"--window-size={total_w},{total_h}",
        f"--screenshot={png_path}",
        f"file:///{h}"
    ], capture_output=True, timeout=30)
    from PIL import Image, ImageChops
    img = Image.open(png_path)
    bg = Image.new("RGB", img.size, (240,242,245))
    diff = ImageChops.difference(img.convert("RGB"), bg)
    bbox = diff.getbbox()
    if bbox:
        p = 20
        bbox = (max(0,bbox[0]-p),max(0,bbox[1]-p),min(img.size[0],bbox[2]+p),min(img.size[1],bbox[3]+p))
        img.crop(bbox).save(png_path, "PNG", optimize=False)
        print(f"saved {png_path} {img.crop(bbox).size}")
    else:
        print(f"saved {png_path} {img.size}")

if __name__ == "__main__":
    out = r"c:\Users\wjh22\Desktop\无人机地面站-课设文稿\思维导图"
    h1 = gen_image1(out)
    render_png(h1, os.path.join(out, "01-需求分析过程与思路.png"), 3200)
    h2 = gen_image2(out)
    render_png(h2, os.path.join(out, "02-系统功能完备性全景.png"), 3200)
    h3 = gen_image3(out)
    render_png(h3, os.path.join(out, "03-通讯架构与数据流.png"), 3200)
    print("all done")
