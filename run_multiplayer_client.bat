@echo off
echo ================================================
echo F1 Racing Game - Multiplayer Client
echo ================================================
echo.
echo Server IP: 203.234.62.51
echo Ports: TCP 54555, UDP 54777
echo.

REM 현재 디렉토리를 배치 파일이 있는 위치로 변경
cd /d "%~dp0"

REM 환경변수 설정
set F1_SERVER_HOST=203.234.62.51

echo Starting game...
echo.

REM Windows에서 gradlew.bat 명시적 호출
if exist gradlew.bat (
    call gradlew.bat lwjgl3:run
) else (
    echo ERROR: gradlew.bat not found!
    echo Please make sure you are in the project root directory.
    pause
    exit /b 1
)

pause
