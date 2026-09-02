import re

with open(r'c:\Users\wjh22\Desktop\无人机地面站-课设文稿\交互界面原型.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Clean flyMap container
fly_pattern = r'(<div id="flyMap").*?(</div>\s*<div class="fly-banner info-banner hidden" id="flyBanner">)'
def clean_fly(m):
    return '<div id="flyMap" style="position:relative;flex:1;min-height:0;overflow:hidden;background:#0d1117"></div>\n      <div class="fly-banner info-banner hidden" id="flyBanner">'
html = re.sub(fly_pattern, clean_fly, html, flags=re.DOTALL)

# Clean planMap container
plan_pattern = r'(<div id="planMap").*?(</div>\s*<div class="map-toolbar">)'
def clean_plan(m):
    return '<div id="planMap" style="flex:1;min-height:0;overflow:hidden;background:#0d1117"></div>\n      <div class="map-toolbar">'
html = re.sub(plan_pattern, clean_plan, html, flags=re.DOTALL)

# Clean flightMap container
flight_pattern = r'(<div id="flightMap").*?(</div>\s*<div class="flight-detail-stats" id="flightStats")'
def clean_flight(m):
    return '<div id="flightMap" style="flex:1;width:100%;overflow:hidden;background:#0d1117"></div>\n          <div class="flight-detail-stats" id="flightStats"'
html = re.sub(flight_pattern, clean_flight, html, flags=re.DOTALL)

# Remove any AMap dynamic style
html = re.sub(r'<style type="text/css" id="AMap_Dynamic_style">.*?</style>', '', html, flags=re.DOTALL)

# Fix nav-tab active states
html = html.replace('class="nav-tab active" data-page="fly"', 'class="nav-tab" data-page="fly"')
html = html.replace('class="nav-tab" data-page="overview"', 'class="nav-tab active" data-page="overview"')

# Fix page active states
html = html.replace('id="page-overview" class="page"', 'id="page-overview" class="page active"')
html = html.replace('id="page-fly" class="page active"', 'id="page-fly" class="page"')

print('Cleaned HTML length:', len(html))

with open(r'c:\Users\wjh22\Desktop\无人机地面站-课设文稿\交互界面原型.html', 'w', encoding='utf-8') as f:
    f.write(html)

print('File saved.')
