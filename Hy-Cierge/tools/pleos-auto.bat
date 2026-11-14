@echo off
rem ==========================================================
rem  Run 버튼 누를 때마다 Gradle이 자동으로 실행하는 시연 설정
rem  (에뮬이 안 켜져 있으면 아무것도 안 하고 바로 끝남)
rem ==========================================================
chcp 65001 >nul
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
if not exist "%ADB%" exit /b 0

for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices') do (
  if "%%b"=="device" call :setup %%a
)
exit /b 0

:setup
set S=%1
rem Pleos 에뮬인지 확인 (car_service 가 있는 기기만)
"%ADB%" -s %S% shell service check car_service | findstr /c:"found" | findstr /v /c:"not found" >nul || exit /b 0
echo [Pleos auto] %S%
rem 1) 주행 중에도 앱 사용 (시연용)
"%ADB%" -s %S% shell cmd car_service enable-uxr false >nul 2>nul
"%ADB%" -s %S% shell dumpsys car_service enable-uxr false >nul 2>nul
rem 2) 한국 시간 (지금 시간대가 +0900 이 아닐 때만 바꿈)
"%ADB%" -s %S% shell date +%%z | findstr /c:"+0900" >nul
if errorlevel 1 (
  echo [Pleos auto] 시간대를 Asia/Seoul 로 변경
  "%ADB%" -s %S% shell settings put global auto_time_zone 0 >nul 2>nul
  "%ADB%" -s %S% shell cmd time_zone_detector set_auto_detection_enabled false >nul 2>nul
  "%ADB%" -s %S% shell cmd time_zone_detector suggest_manual_time_zone --zone_id Asia/Seoul >nul 2>nul
  "%ADB%" -s %S% shell service call alarm 3 s16 Asia/Seoul >nul 2>nul
)
"%ADB%" -s %S% shell date +%%z | findstr /c:"+0900" >nul
if errorlevel 1 (
  "%ADB%" -s %S% root >nul 2>nul
  ping -n 4 127.0.0.1 >nul
  "%ADB%" -s %S% wait-for-device
  "%ADB%" -s %S% shell setprop persist.sys.timezone Asia/Seoul
  "%ADB%" -s %S% shell service call alarm 3 s16 Asia/Seoul >nul 2>nul
  "%ADB%" -s %S% shell cmd car_service enable-uxr false >nul 2>nul
  "%ADB%" -s %S% shell dumpsys car_service enable-uxr false >nul 2>nul
)
rem 3) 예전 택시앱 정리 (에뮬에서만 삭제, 프로젝트 폴더는 그대로)
"%ADB%" -s %S% uninstall com.example.pleostaxiguide >nul 2>nul
rem 4) Virtual Console 포트
"%ADB%" -s %S% forward tcp:8088 tcp:8088 >nul 2>nul
exit /b 0
