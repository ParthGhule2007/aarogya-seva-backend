@echo off
setlocal enabledelayedexpansion

title Aarogya Seva - Stop All Services
color 0C

echo ===============================================================================
echo                AAROGYA SEVA - RURAL HEALTHCARE PLATFORM
echo                     Stopping All Running Services
echo ===============================================================================
echo.

set PORTS=8081 8082 8083 8084 8085 8086 8088 8089 5173

for %%p in (%PORTS%) do (
    echo Checking port %%p...
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%%p " ^| findstr "LISTENING"') do (
        echo   [KILLING] Port %%p used by PID %%a
        taskkill /F /PID %%a >nul 2>&1
    )
)

echo.
echo ===============================================================================
echo All Aarogya Seva services on ports 8081-8089 and 5173 stopped!
echo ===============================================================================
pause
