#!/system/bin/sh
# 重启后验证：抓取飞控 UDP 广播 10s
for p in $(ps -A | grep ' toybox' | awk '{print $2}'); do kill "$p" 2>/dev/null; done
sleep 1
timeout 10 toybox nc -u -l -p 14550 -o- 2>/dev/null | head -c 400 | od -Ax -tx1 > /data/local/tmp/cap7.txt
echo CAP7_DONE > /data/local/tmp/cap7status.txt