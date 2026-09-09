@echo off
setlocal
chcp 65001 >nul 2>&1
set "PROJECT=C:\Users\wjh22\Desktop\skylink-gcs-main\skylink-gcs-main"
set "JAVAW=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javaw.exe"
set "MVN=C:\Users\wjh22\.m2\wrapper\dists\apache-maven-3.9.6-bin\3311e1d4\apache-maven-3.9.6\bin\mvn.cmd"
set "MP=C:\Users\wjh22\.m2\repository\org\openjfx\javafx-base\17.0.2\javafx-base-17.0.2-win.jar;C:\Users\wjh22\.m2\repository\org\openjfx\javafx-graphics\17.0.2\javafx-graphics-17.0.2-win.jar;C:\Users\wjh22\.m2\repository\org\openjfx\javafx-controls\17.0.2\javafx-controls-17.0.2-win.jar;C:\Users\wjh22\.m2\repository\org\openjfx\javafx-web\17.0.2\javafx-web-17.0.2-win.jar;C:\Users\wjh22\.m2\repository\org\openjfx\javafx-media\17.0.2\javafx-media-17.0.2-win.jar"
set "CP=%PROJECT%\target\classes;C:\Users\wjh22\.m2\repository\com\fazecast\jSerialComm\2.11.0\jSerialComm-2.11.0.jar"

rem 每次启动前增量编译（离线模式）：源码有更新即重编，一键启动永远是最新开发版（无改动时约 3 秒）
call "%MVN%" -o -q -f "%PROJECT%\pom.xml" compile
if errorlevel 1 (
  echo [%date% %time%] 编译失败，本次不启动。请检查 src 源码，或手动运行 mvn compile>"%PROJECT%\launch-error.log"
  exit /b 1
)

start "" "%JAVAW%" --module-path "%MP%" --add-modules javafx.controls,javafx.web -cp "%CP%" com.cherglow.gcs.Launcher
endlocal
