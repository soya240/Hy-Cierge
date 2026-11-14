@echo off
chcp 65001 >nul
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
echo [1/5] Waiting for Pleos emulator...
"%ADB%" wait-for-device
echo [2/5] Root (for timezone)
"%ADB%" root
timeout /t 3 >nul
"%ADB%" wait-for-device
echo [3/5] Timezone: Asia/Seoul
"%ADB%" shell setprop persist.sys.timezone Asia/Seoul
echo [4/5] Allow app while driving (demo only)
"%ADB%" shell dumpsys car_service enable-uxr false
"%ADB%" shell cmd car_service enable-uxr false
echo [5/5] Virtual Console port 8088
"%ADB%" forward tcp:8088 tcp:8088
start "" http://127.0.0.1:8088/
echo.
echo DONE. Virtual Console opened in browser.
pause
