# Aarogya Seva - Start All Services (PowerShell)
$rootDir = $PSScriptRoot

# Automatically load local .env variables into process if present
if (Test-Path "$rootDir\.env") {
    Get-Content "$rootDir\.env" | ForEach-Object {
        $line = $_.Trim()
        if ($line -and !$line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $k = $parts[0].Trim()
            $v = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($k, $v, "Process")
        }
    }
}

if (Test-Path "C:\tools\apache-maven-3.10.0\bin") {
    $env:PATH = "C:\tools\apache-maven-3.10.0\bin;$env:PATH"
}

Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host "               AAROGYA SEVA - RURAL HEALTHCARE PLATFORM" -ForegroundColor Green
Write-Host "                    Sequential Service Launcher" -ForegroundColor Yellow
Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Starting all microservices one by one with staggered delay..." -ForegroundColor White
Write-Host "Each service will run in its own dedicated command window." -ForegroundColor Gray
Write-Host ""

$services = @(
    @{ Name = "Core Service"; Port = 8081; Dir = "$rootDir"; Command = "mvn spring-boot:run" },
    @{ Name = "SOS Service"; Port = 8082; Dir = "$rootDir\sos-service"; Command = "mvn spring-boot:run" },
    @{ Name = "Consultation Service"; Port = 8083; Dir = "$rootDir\consultation-service"; Command = "mvn spring-boot:run" },
    @{ Name = "Communication Service"; Port = 8084; Dir = "$rootDir\communication-service"; Command = "mvn spring-boot:run" },
    @{ Name = "Chatbot Service"; Port = 8085; Dir = "$rootDir\chatbot-service"; Command = "mvn spring-boot:run" },
    @{ Name = "Doctor Service"; Port = 8086; Dir = "$rootDir\doctor-service"; Command = "mvn spring-boot:run" },
    @{ Name = "OAuth Service"; Port = 8087; Dir = "$rootDir\oauth-service"; Command = "mvn spring-boot:run" },
    @{ Name = "Medicine Service"; Port = 8088; Dir = "$rootDir\medicine-service"; Command = "mvn spring-boot:run" },
    @{ Name = "Calling Service"; Port = 8089; Dir = "$rootDir\calling-service"; Command = "mvn spring-boot:run" },
    @{ Name = "Frontend UI"; Port = 5173; Dir = "$rootDir\frontend"; Command = "npm run dev" }
)

$index = 1
$total = $services.Count
foreach ($svc in $services) {
    $existing = Get-NetTCPConnection -LocalPort $svc.Port -State Listen -ErrorAction SilentlyContinue
    if ($existing) {
        Write-Host "[$index/$total] $($svc.Name) is ALREADY RUNNING on Port $($svc.Port) (PID: $($existing[0].OwningProcess)) - skipping." -ForegroundColor Green
        $index++
        continue
    }
    Write-Host "[$index/$total] Starting $($svc.Name) on Port $($svc.Port)..." -ForegroundColor Yellow
    $title = "$($svc.Name) [$($svc.Port)]"
    $cmdArgs = "/k title $title && cd /d `"$($svc.Dir)`" && $($svc.Command)"
    Start-Process -FilePath "cmd.exe" -ArgumentList $cmdArgs
    Start-Sleep -Seconds 5
    $index++
}

Write-Host ""
Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host "ALL SERVICES INITIATED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host "  - Core Service:          http://localhost:8081 (Swagger: /swagger-ui.html)"
Write-Host "  - SOS Service:           http://localhost:8082"
Write-Host "  - Consultation Service:  http://localhost:8083"
Write-Host "  - Communication Service: http://localhost:8084"
Write-Host "  - Chatbot Service:       http://localhost:8085"
Write-Host "  - Doctor Service:        http://localhost:8086"
Write-Host "  - OAuth Service:         http://localhost:8087 (Swagger: /swagger-ui.html)"
Write-Host "  - Medicine Service:      http://localhost:8088"
Write-Host "  - Calling Service:       http://localhost:8089 (Swagger: /swagger-ui.html)"
Write-Host "  - Frontend UI:           http://localhost:5173"
Write-Host ""
Write-Host "To stop all services, run: .\stop-all-services.ps1 or stop-all-services.bat" -ForegroundColor Magenta
Write-Host "===============================================================================" -ForegroundColor Cyan
