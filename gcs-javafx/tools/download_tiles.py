#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SkyLink GCS - 离线地图瓦片下载工具
支持按经纬度范围和缩放级别批量下载 OpenStreetMap 瓦片，
保存为标准 Slippy Map 目录结构：tiles/{z}/{x}/{y}.png

用法:
    python download_tiles.py --lat-min 32.05 --lat-max 32.07 --lon-min 118.78 --lon-max 118.81 --zoom 15 16 17 --output tiles/
    python download_tiles.py --city nanjing --zoom 14-17

注意: 使用 OSM 瓦片请遵守其使用政策 (https://operations.osmfoundation.org/policies/tiles/)
      仅供个人/教育用途，不要高频请求。
"""

import argparse
import os
import time
import math
import urllib.request
import urllib.error
import sys


def deg2num(lat_deg, lon_deg, zoom):
    """经纬度转瓦片坐标 (Slippy Map TMS 标准)"""
    lat_rad = math.radians(lat_deg)
    n = 2.0 ** zoom
    xtile = int((lon_deg + 180.0) / 360.0 * n)
    ytile = int((1.0 - math.asinh(math.tan(lat_rad)) / math.pi) / 2.0 * n)
    return xtile, ytile


def num2deg(xtile, ytile, zoom):
    """瓦片坐标转经纬度（返回瓦片左上角）"""
    n = 2.0 ** zoom
    lon_deg = xtile / n * 360.0 - 180.0
    lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * ytile / n)))
    lat_deg = math.degrees(lat_rad)
    return lat_deg, lon_deg


def download_tile(url, filepath, retries=3, timeout=10):
    """下载单个瓦片，失败重试"""
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={
                'User-Agent': 'SkyLink-GCS/2.1 (educational project)'
            })
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                data = resp.read()
                if len(data) < 100:
                    raise ValueError("瓦片数据过小，可能无效")
                os.makedirs(os.path.dirname(filepath), exist_ok=True)
                with open(filepath, 'wb') as f:
                    f.write(data)
                return True, len(data)
        except (urllib.error.URLError, urllib.error.HTTPError, ValueError, TimeoutError) as e:
            if attempt < retries - 1:
                time.sleep(0.5 * (attempt + 1))
            else:
                return False, str(e)
    return False, "max retries exceeded"


def main():
    parser = argparse.ArgumentParser(
        description='SkyLink GCS 离线地图瓦片下载工具',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='示例:\n'
               '  python download_tiles.py --lat-min 32.05 --lat-max 32.07 '
               '--lon-min 118.78 --lon-max 118.81 --zoom 15 16 17\n'
               '  python download_tiles.py --city nanjing --zoom 14-17 --output my_tiles/'
    )
    parser.add_argument('--lat-min', type=float, help='最小纬度')
    parser.add_argument('--lat-max', type=float, help='最大纬度')
    parser.add_argument('--lon-min', type=float, help='最小经度')
    parser.add_argument('--lon-max', type=float, help='最大经度')
    parser.add_argument('--zoom', type=str, default='15-17',
                        help='缩放级别，如 "15" 或 "14-17" 或 "15 16 17" (默认: 15-17)')
    parser.add_argument('--output', type=str, default='tiles/',
                        help='瓦片输出目录 (默认: tiles/)')
    parser.add_argument('--server', type=str,
                        default='https://tile.openstreetmap.org',
                        help='瓦片服务器地址 (默认: OpenStreetMap)')
    parser.add_argument('--delay', type=float, default=0.1,
                        help='每次请求间隔秒数 (默认: 0.1s)')
    parser.add_argument('--dry-run', action='store_true',
                        help='仅统计瓦片数量，不实际下载')
    parser.add_argument('--city', type=str, default=None,
                        help='使用预设城市范围 (nanjing, beijing, shanghai)')

    args = parser.parse_args()

    # 预设城市范围
    city_presets = {
        'nanjing': (32.0, 32.15, 118.7, 118.9),
        'beijing': (39.8, 40.1, 116.2, 116.6),
        'shanghai': (31.1, 31.35, 121.35, 121.6),
        'xianlin': (32.045, 32.075, 118.78, 118.815),
    }

    if args.city:
        city = args.city.lower()
        if city not in city_presets:
            print(f"错误: 未知城市 '{city}'，可选: {', '.join(city_presets.keys())}")
            sys.exit(1)
        lat_min, lat_max, lon_min, lon_max = city_presets[city]
        print(f"使用预设区域: {city} (纬度 {lat_min}~{lat_max}, 经度 {lon_min}~{lon_max})")
    else:
        if not all([args.lat_min, args.lat_max, args.lon_min, args.lon_max]):
            print("错误: 请指定经纬度范围 (--lat-min/--lat-max/--lon-min/--lon-max) 或使用 --city")
            sys.exit(1)
        lat_min = args.lat_min
        lat_max = args.lat_max
        lon_min = args.lon_min
        lon_max = args.lon_max

    # 解析缩放级别
    if '-' in args.zoom:
        z_start, z_end = map(int, args.zoom.split('-'))
        zoom_levels = list(range(z_start, z_end + 1))
    else:
        zoom_levels = [int(z) for z in args.zoom.replace(',', ' ').split()]

    print(f"缩放级别: {zoom_levels}")
    print(f"瓦片服务器: {args.server}")
    print(f"输出目录: {args.output}")

    # 计算总瓦片数
    total = 0
    for z in zoom_levels:
        x_min, y_min = deg2num(lat_max, lon_min, z)  # 左上角
        x_max, y_max = deg2num(lat_min, lon_max, z)  # 右下角
        tiles = (x_max - x_min + 1) * (y_max - y_min + 1)
        total += tiles
        print(f"  Z={z}: X[{x_min}..{x_max}] Y[{y_min}..{y_max}] = {tiles} 张瓦片")

    print(f"总计: {total} 张瓦片")

    if args.dry_run:
        print("(dry-run 模式，未实际下载)")
        return

    if total > 5000:
        confirm = input(f"瓦片数量较多 ({total} 张)，继续下载? [y/N] ").strip().lower()
        if confirm != 'y':
            print("已取消")
            return

    # 开始下载
    downloaded = 0
    failed = 0
    skipped = 0
    total_bytes = 0
    start_time = time.time()

    for z in zoom_levels:
        x_min, y_min = deg2num(lat_max, lon_min, z)
        x_max, y_max = deg2num(lat_min, lon_max, z)
        print(f"\n下载 Z={z} ({(x_max-x_min+1)*(y_max-y_min+1)} 张)...")

        for x in range(x_min, x_max + 1):
            for y in range(y_min, y_max + 1):
                filepath = os.path.join(args.output, str(z), str(x), f"{y}.png")
                if os.path.exists(filepath) and os.path.getsize(filepath) > 100:
                    skipped += 1
                    continue

                url = f"{args.server}/{z}/{x}/{y}.png"
                success, info = download_tile(url, filepath)

                if success:
                    downloaded += 1
                    total_bytes += info
                else:
                    failed += 1
                    if failed <= 5:
                        print(f"  失败: {url} - {info}")

                downloaded_so_far = downloaded + failed + skipped
                if downloaded_so_far % 50 == 0 and downloaded_so_far > 0:
                    pct = downloaded_so_far / total * 100
                    print(f"  进度: {downloaded_so_far}/{total} ({pct:.1f}%) "
                          f"成功:{downloaded} 跳过:{skipped} 失败:{failed}")

                time.sleep(args.delay)

    elapsed = time.time() - start_time
    print(f"\n{'='*50}")
    print(f"下载完成! 耗时 {elapsed:.1f}s")
    print(f"  成功: {downloaded} 张 ({total_bytes/1024:.1f} KB)")
    print(f"  跳过: {skipped} 张 (已存在)")
    print(f"  失败: {failed} 张")
    print(f"  总瓦片: {total} 张")
    print(f"{'='*50}")


if __name__ == '__main__':
    main()
