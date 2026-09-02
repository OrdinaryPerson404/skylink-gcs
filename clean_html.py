import re

with open(r'c:\Users\wjh22\Desktop\无人机地面站-课设文稿\交互界面原型.html', 'r', encoding='utf-8') as f:
    html = f.read()

# 1. Remove AMap dynamic style injected at head
html = re.sub(r'<style type="text/css" id="AMap_Dynamic_style">.*?</style>', '', html, flags=re.DOTALL)

# 2. Fix head structure - ensure meta charset is right after <head>
html = re.sub(r'(<html lang="zh-CN"><head>)\s*<meta charset="UTF-8">', r'\1\n<meta charset="UTF-8">', html)

# 3. Clean flyMap container - remove all rendered AMap DOM, keep only the control bar
flymap_pattern = r'(<div id="flyMap").*?(</div>\s*<div class="fly-banner info-banner hidden" id="flyBanner">)'
def clean_flymap(m):
    return '<div id="flyMap" style="position:relative;flex:1;min-height:0;overflow:hidden;background:#0d1117">\n        <div class="map-ctrl-bar">\n          <button class="map-ctrl-btn active" id="btnFollow" onclick="toggleFollow()">跟随</button>\n          <button class="map-ctrl-btn active" id="btnTrajectory" onclick="toggleTrajectory()">轨迹</button>\n        </div>\n      </div>\n      <div class="fly-banner info-banner hidden" id="flyBanner">'
html = re.sub(flymap_pattern, clean_flymap, html, flags=re.DOTALL)

# 4. Clean planMap container
planmap_pattern = r'(<div id="planMap").*?(</div>\s*<div class="map-toolbar">)'
def clean_planmap(m):
    return '<div id="planMap" style="flex:1;min-height:0;overflow:hidden;background:#0d1117"></div>\n      <div class="map-toolbar">'
html = re.sub(planmap_pattern, clean_planmap, html, flags=re.DOTALL)

# 5. Clean flightMap container
flightmap_pattern = r'(<div id="flightMap").*?(</div>\s*<div class="flight-detail-stats" id="flightStats")'
def clean_flightmap(m):
    return '<div id="flightMap" style="flex:1;width:100%;overflow:hidden;background:#0d1117"></div>\n          <div class="flight-detail-stats" id="flightStats"'
html = re.sub(flightmap_pattern, clean_flightmap, html, flags=re.DOTALL)

# 6. Remove active class from page-fly (should not have it by default)
html = html.replace('id="page-fly" class="active"', 'id="page-fly"')
html = html.replace('id="page-plan" class="active"', 'id="page-plan"')
html = html.replace('id="page-data" class="active"', 'id="page-data"')

# 7. Remove any remaining amap-container classes on map divs
html = re.sub(r'<div id="(flyMap|planMap|flightMap)"[^>]*class="[^"]*amap-container[^"]*"[^>]*>', r'<div id="\1" style="flex:1;min-height:0;overflow:hidden;background:#0d1117">', html)

# 8. Remove any remaining AMap rendered elements inside body that are not in map containers
# (This is a safety net)

print('Cleaned HTML length:', len(html))

with open(r'c:\Users\wjh22\Desktop\无人机地面站-课设文稿\交互界面原型.html', 'w', encoding='utf-8') as f:
    f.write(html)

print('File saved.')
