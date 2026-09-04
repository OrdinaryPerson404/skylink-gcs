#!/system/bin/sh
# 联合诊断：A) 飞控Web存活  B) 1Hz持续心跳 + 12s抓包  C) UDP收包统计
for p in $(ps -A | grep ' toybox' | awk '{print $2}'); do kill "$p" 2>/dev/null; done
sleep 1
echo "=== A) Web liveness ==="
echo "GET / HTTP/1.0" | toybox nc -w 3 192.168.4.1 80 2>/dev/null | head -3
echo "=== B) 持续心跳+抓包 ==="
( i=0; while [ $i -lt 10 ]; do cat /data/local/tmp/hb.bin | toybox nc -u -w 1 192.168.4.1 14550 2>/dev/null; sleep 1; i=$((i+1)); done ) &
HB_PID=$!
timeout 12 toybox nc -u -l -p 14550 2>/dev/null | wc -c > /data/local/tmp/cap3_bytes.txt
wait $HB_PID 2>/dev/null
echo "CAP3_DONE" > /data/local/tmp/cap3status.txt
echo "=== C) /proc/net/udp (14550=0x38C6) ==="
cat /proc/net/udp | grep -i "38C6" | head -5