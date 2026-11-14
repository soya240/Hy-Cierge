@echo off
cd /d "%~dp0"
echo Downloading place photos (Wikipedia / Wikimedia Commons)...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\photos.ps1"
pause
