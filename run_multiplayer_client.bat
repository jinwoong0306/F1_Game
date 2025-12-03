@echo off
echo ================================================
echo F1 Racing Game - Multiplayer Client
echo ================================================
echo.
echo Server IP: 203.234.62.51
echo Ports: TCP 54555, UDP 54777
echo.

set F1_SERVER_HOST=203.234.62.51

echo Starting game...
echo.

gradlew lwjgl3:run

pause
