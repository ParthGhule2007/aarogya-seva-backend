@echo off
setlocal enabledelayedexpansion

title Aarogya Seva - Start All Services
color 0A

echo ===============================================================================
echo                AAROGYA SEVA - RURAL HEALTHCARE PLATFORM
echo                     Sequential Service Launcher
echo ===============================================================================
echo.
echo Starting all microservices one by one with staggered delay...
echo Each service will run in its own dedicated command window.
echo.

set ROOT_DIR=%~dp0
cd /d "%ROOT_DIR%"

if exist "%ROOT_DIR%\.env" (
    for /f "usebackq tokens=1* delims==" %%A in ("%ROOT_DIR%\.env") do (
        set "line=%%A"
        if not "!line:~0,1!"=="#" if not "%%B"=="" set "%%A=%%B"
    )
)

echo [1/8] Starting Core Service on Port 8081...
start "Core Service [8081]" cmd /k "title Core Service [8081] && cd /d "%ROOT_DIR%" && mvn spring-boot:run"
timeout /t 6 /nobreak >nul

echo [2/8] Starting SOS Emergency Service on Port 8082...
start "SOS Service [8082]" cmd /k "title SOS Service [8082] && cd /d "%ROOT_DIR%\sos-service" && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [3/8] Starting Consultation Service on Port 8083...
start "Consultation Service [8083]" cmd /k "title Consultation Service [8083] && cd /d "%ROOT_DIR%\consultation-service" && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [4/8] Starting Communication Service on Port 8084...
start "Communication Service [8084]" cmd /k "title Communication Service [8084] && cd /d "%ROOT_DIR%\communication-service" && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [5/8] Starting Chatbot Service on Port 8085...
start "Chatbot Service [8085]" cmd /k "title Chatbot Service [8085] && cd /d "%ROOT_DIR%\chatbot-service" && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [6/8] Starting Doctor Service on Port 8086...
start "Doctor Service [8086]" cmd /k "title Doctor Service [8086] && cd /d "%ROOT_DIR%\doctor-service" && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [7/9] Starting Medicine Reminder Service on Port 8088...
start "Medicine Service [8088]" cmd /k "title Medicine Service [8088] && cd /d "%ROOT_DIR%\medicine-service" && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [8/9] Starting Calling Service (Aarogya Mitra) on Port 8089...
start "Calling Service [8089]" cmd /k "title Calling Service [8089] && cd /d "%ROOT_DIR%\calling-service" && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [9/9] Starting Frontend Web App on Port 5173...
start "Frontend UI [5173]" cmd /k "title Frontend UI [5173] && cd /d "%ROOT_DIR%\frontend" && npm run dev"

echo.
echo ===============================================================================
echo ALL SERVICES INITIATED SUCCESSFULLY!
echo ===============================================================================
echo.
echo Service Endpoints:
echo   - Core Service:         http://localhost:8081 (Swagger: /swagger-ui.html)
echo   - SOS Service:          http://localhost:8082
echo   - Consultation Service: http://localhost:8083
echo   - Communication Service:http://localhost:8084
echo   - Chatbot Service:      http://localhost:8085
echo   - Doctor Service:       http://localhost:8086
echo   - Medicine Service:     http://localhost:8088
echo   - Calling Service:      http://localhost:8089 (Swagger: /swagger-ui.html)
echo   - Frontend UI:          http://localhost:5173
echo.
echo To stop all services at any time, run: stop-all-services.bat
echo ===============================================================================
pause
