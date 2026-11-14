@echo off
chcp 65001 >nul
rem Figma P01·P03A·P08 에 쓰인 이미지 4장을 앱 폴더로 받아옴 (배경, 책상 일러스트, 오브 텍스처, P08 지도)
rem 링크는 Figma가 만든 임시 링크라 일주일쯤 지나면 만료됨 → 그 전에 한 번만 실행하면 됨
set D=%~dp0app\src\main\res\drawable-nodpi
if not exist "%D%" mkdir "%D%"
echo [1/4] 배경
powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://www.figma.com/api/mcp/asset/0962dbca-8a64-4a5d-87cb-f33f623ae1d5/5d335.png' -OutFile '%D%\fig_bg.png'"
echo [2/4] 책상 일러스트
powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://www.figma.com/api/mcp/asset/0962dbca-8a64-4a5d-87cb-f33f623ae1d5/dcc88.png' -OutFile '%D%\fig_desk.png'"
echo [3/4] 오브 텍스처
powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://www.figma.com/api/mcp/asset/0962dbca-8a64-4a5d-87cb-f33f623ae1d5/9dfbc.png' -OutFile '%D%\fig_orb.png'"
echo [4/4] P08 지도 (홀로그램 지도 바탕)
powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://www.figma.com/api/mcp/asset/6d86d8ba-cd4b-47ae-b684-52c3ce8ca881/f6654.png' -OutFile '%D%\fig_map.png'"
echo.
dir "%D%"
echo.
echo 끝! 안드 스튜디오에서 Run 하면 적용됩니다.
pause
