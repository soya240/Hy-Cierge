@echo off
chcp 65001 >nul
cd /d "%~dp0"
rem 제출용 release APK 만들기 → 바탕화면에 Hy-Cierge_v1.0.apk 로 저장
if "%JAVA_HOME%"=="" if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
echo APK 만드는 중... (1~3분 걸림)
call gradlew.bat assembleRelease
if errorlevel 1 (
  echo.
  echo 빌드 실패. 이 창을 캡처해서 보내줘.
  pause
  exit /b 1
)
copy /Y "app\build\outputs\apk\release\app-release.apk" "%USERPROFILE%\Desktop\Hy-Cierge_v1.0.apk" >nul
echo.
echo 완료! 바탕화면에 Hy-Cierge_v1.0.apk 저장됨
pause
