#!/system/bin/sh
# 精准清理 toybox nc 听众，并 8 秒抓取飞控 UDP 14550 到达手机的字节
for p in $(ps -A | grep ' toybox' | awk '{print $2}'); do kill "$p" 2>/dev/null; done
sleep 1
timeout 8 toybox nc -u -l -p 14550 -o- 2>/dev/null | od -Ax -tx1 > /data/local/tmp/cap.txt
echo CAPTURE_DONE > /data/local/tmp/capstatus.txt