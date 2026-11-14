@echo off
chcp 65001 >nul
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
echo ===== 지금 상태 =====
"%ADB%" devices
"%ADB%" shell date
"%ADB%" shell getprop persist.sys.timezone
"%ADB%" shell settings get global auto_time_zone
echo.
echo ===== 한국 시간으로 바꾸기 =====
"%ADB%" root
timeout /t 3 >nul
"%ADB%" wait-for-device
"%ADB%" shell settings put global auto_time_zone 0
"%ADB%" shell cmd time_zone_detector set_auto_detection_enabled false
"%ADB%" shell cmd time_zone_detector suggest_manual_time_zone --zone_id Asia/Seoul
"%ADB%" shell service call alarm 3 s16 Asia/Seoul
"%ADB%" shell setprop persist.sys.timezone Asia/Seoul
echo.
echo ===== 바꾼 뒤 =====
"%ADB%" shell date
"%ADB%" shell getprop persist.sys.timezone
echo.
echo 이 창을 스샷 찍어 보내주세요.
pause
