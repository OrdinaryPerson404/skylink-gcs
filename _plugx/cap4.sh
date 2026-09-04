#!/system/bin/sh
# 二分测试：手机自身UDP可达性 vs 飞控发送
for p in $(ps -A | grep ' toybox' | awk '{print $2}'); do kill "$p" 2>/dev/null; done
sleep 1
timeout 6 toybox nc -u -l -p 14550 -o- > /data/local/tmp/cap4.txt 2>/dev/null &
LP=$!
sleep 1
echo -n "SELFTEST-LOOPBACK" | toybox nc -u -w 1 127.0.0.1 14550 2>/dev/null
echo -n "SELFTEST-WLAN0" | toybox nc -u -w 1 192.168.4.2 14550 2>/dev/null
wait $LP
echo CAP4_DONE > /data/local/tmp/cap4status.txt