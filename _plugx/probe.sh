#!/system/bin/sh
# Web控制台只读探测：主循环活性验证（time 命令两次，比较 Time 是否推进）
B=192.168.4.1
req() { printf "%s\r\n" "$@"; }
post() { printf "POST $1 HTTP/1.0\r\nHost: $B\r\nContent-Length: %d\r\n\r\n%s" "${#2}" "$2" | toybox nc -w 3 $B 80 2>/dev/null | head -1; }
get()  { printf "GET $1 HTTP/1.0\r\nHost: $B\r\n\r\n" | toybox nc -w 3 $B 80 2>/dev/null | tail -3; }

post /console/enable ""
post /console/cmd "time"
sleep 2
echo "--- T1 ---"; get "/console?limit=6"
post /console/cmd "time"
sleep 2
echo "--- T2 ---"; get "/console?limit=6"