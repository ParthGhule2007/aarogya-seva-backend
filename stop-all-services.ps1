# Aarogya Seva - Stop All Services (PowerShell)

Write-Host "===============================================================================" -ForegroundColor Red
Write-Host "               AAROGYA SEVA - RURAL HEALTHCARE PLATFORM" -ForegroundColor Yellow
Write-Host "                     Stopping All Running Services" -ForegroundColor Red
Write-Host "===============================================================================" -ForegroundColor Red
Write-Host ""

$ports = @(8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8089, 5173)

foreach ($port in $ports) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($conns) {
        foreach ($conn in $conns) {
            $pidToKill = $conn.OwningProcess
            try {
                $proc = Get-Process -Id $pidToKill -ErrorAction SilentlyContinue
                Write-Host "  [STOPPING] Port $port (PID $pidToKill - $($proc.ProcessName))" -ForegroundColor Yellow
                Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
            } catch {
                Write-Host "  [ERROR] Could not stop PID ${pidToKill}: $_" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "  [CLEAN] Port $port is already free" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "===============================================================================" -ForegroundColor Green
Write-Host "All Aarogya Seva services on ports 8081-8089 and 5173 have been stopped!" -ForegroundColor Green
Write-Host "===============================================================================" -ForegroundColor Green
