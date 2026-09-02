import re
with open(r'c:\Users\wjh22\Desktop\无人机地面站-课设文稿\交互界面原型.html','r',encoding='utf-8') as f:
    html=f.read()
print('Has maplibregl:', 'maplibregl' in html.lower())
print('Has AMap.Map:', 'new AMap.Map' in html)
print('Has try-catch in initFlyMap:', 'try{' in html and 'catch(e)' in html)
print('Has AMap undefined check:', 'typeof AMap' in html)
m=re.search(r'plugin=([^&"]+)',html)
print('Plugins:', m.group(1) if m else 'none')
# Check for removed showDir
print('Has showDir:', 'showDir' in html)
print('Has ControlBar in code:', 'AMap.ControlBar' in html)
# Count try blocks
print('Try-catch blocks:', html.count('try{'))
