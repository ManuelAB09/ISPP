# Wrapper script to ensure PostgreSQL is running before starting the Spring Boot backend
# Usage: run this script instead of calling ./mvnw spring-boot:run directly.

# path to Postgres data directory (adjust version if needed)
$pgData = 'C:\Program Files\PostgreSQL\18\data'
$pgCtl = 'C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe'

Write-Host "Checking PostgreSQL service..."
$pgStatus = & $pgCtl status -D "$pgData" 2>&1
if ($pgStatus -match 'no server running') {
    Write-Host "PostgreSQL not running; starting it now..."
    & $pgCtl start -D "$pgData" -l "$(Resolve-Path .)\pg.log"
    Write-Host "PostgreSQL started."
} else {
    Write-Host "PostgreSQL already running."
}

# finally start backend
Write-Host "Starting Spring Boot application..."
Push-Location backend
# invoke wrapper using call operator to avoid parsing issues
& ".\mvnw" spring-boot:run
Pop-Location
