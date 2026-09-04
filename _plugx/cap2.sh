#!/system/bin/sh
# 1) 清理残留 nc 听众；2) 主动向飞控发送一次 HEARTBEAT(只读)；3) 抓取 8 秒飞控回传
for p in $(ps -A | grep ' toybox' | awk '{print $2}'); do kill "$p" 2>/dev/null; done
sleep 1
cat /data/local/tmp/hb.bin | toybox nc -u -w 2 192.168.4.1 14550 &
sleep 2
timeout 8 toybox nc -u -l -p 14550 -o- 2>/dev/null | od -Ax -tx1 > /data/local/tmp/cap2.txt
echo CAP2_DONE > /data/local/tmp/cap2status.txt