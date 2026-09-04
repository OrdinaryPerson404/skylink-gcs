#!/system/bin/sh
# 修正版二分：全新监听器，wlan0 自测包先发（避免 nc connect 过滤）
for p in $(ps -A | grep ' toybox' | awk '{print $2}'); do kill "$p" 2>/dev/null; done
sleep 1
timeout 5 toybox nc -u -l -p 14550 -o- > /data/local/tmp/cap5.txt 2>/dev/null &
sleep 1
echo -n "WLAN0-FIRST" | toybox nc -u -w 1 192.168.4.2 14550 2>/dev/null
wait
echo CAP5_DONE > /data/local/tmp/cap5status.txt
# 第二段：再测飞控（5s 心跳持续 + 5s 抓包）
( i=0; while [ $i -lt 5 ]; do cat /data/local/tmp/hb.bin | toybox nc -u -w 1 192.168.4.1 14550 2>/dev/null; sleep 1; i=$((i+1)); done ) &
timeout 7 toybox nc -u -l -p 14550 -o- > /data/local/tmp/cap6.txt 2>/dev/null
wait
echo CAP6_DONE > /data/local/tmp/cap6status.txt