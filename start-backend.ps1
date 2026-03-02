# Wrapper script to ensure PostgreSQL is running before starting the Spring Boot backend
# Usage: run this script instead of calling ./mvnw spring-boot:run directly.

# path to Postgres data directory (adjust version if needed)
$pgData = 'C:\Program Files\PostgreSQL\18\data'
$pgCtl = 'C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe'

# first try to detect a Windows service (PostgreSQL installer registers one)
$serviceName = 'postgresql-x64-18'  # adjust if your installer used a different name
$service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if ($service) {
    if ($service.Status -ne 'Running') {
        Write-Host "Service $serviceName is stopped; starting it now..."
        Start-Service -Name $serviceName
        Write-Host "Service started."
    } else {
        Write-Host "Service $serviceName already running."
    }
} else {
    # fallback to pg_ctl when there is no service or we prefer manual control
    Write-Host "Checking PostgreSQL cluster via pg_ctl..."
    $pgStatus = & $pgCtl status -D "$pgData" 2>&1
    if ($pgStatus -match 'no server running') {
        Write-Host "PostgreSQL not running; starting it now using pg_ctl..."
        & $pgCtl start -D "$pgData" -l "$(Resolve-Path .)\pg.log"
        Write-Host "PostgreSQL started via pg_ctl."
    } else {
        Write-Host "PostgreSQL already running according to pg_ctl."
    }
}

# finally start backend
Write-Host "Starting Spring Boot application..."
Push-Location backend
# invoke wrapper using call operator to avoid parsing issues
& ".\mvnw" spring-boot:run
Pop-Location
