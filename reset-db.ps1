# reset-db.ps1
# This script resets the 'meerkatters' database by dropping and recreating the public schema.
# This is useful during development when database models change.

$DbName = "meerkatters"
$DbHost = "localhost"
$DbPort = "5432"

# Default values
$DbUser = "meerkatters_user"
$DbPassword = "meerkatters_password"

# Load credentials from .env file or environment variables if present
$envPath = Join-Path -Path $PSScriptRoot -ChildPath ".env"
if (Test-Path $envPath) {
    Get-Content $envPath | Where-Object { $_ -match '^\s*([^#]+?)\s*=\s*(.*)$' } | ForEach-Object {
        $name = $matches[1].Trim()
        if ($name -eq "SPRING_DATASOURCE_USERNAME") { $DbUser = $matches[2].Trim() }
        if ($name -eq "SPRING_DATASOURCE_PASSWORD") { $DbPassword = $matches[2].Trim() }
    }
} else {
    Write-Host "No .env file found. Using default credentials." -ForegroundColor Yellow
}

$PsqlPath = "C:\Program Files\PostgreSQL\18\bin\psql.exe"

if (-Not (Test-Path $PsqlPath)) {
    Write-Host "Error: Cannot find psql.exe at '$PsqlPath'." -ForegroundColor Red
    Write-Host "Please verify your PostgreSQL installation path." -ForegroundColor Yellow
    exit 1
}

Write-Host "WARNING: This will delete ALL data and tables in the local '$DbName' database!" -ForegroundColor Yellow
$confirmation = Read-Host "Are you sure you want to proceed? (y/N)"

if ($confirmation -ne 'y' -and $confirmation -ne 'Y') {
    Write-Host "Operation cancelled." -ForegroundColor Cyan
    exit 0
}

Write-Host "Resetting database schema..." -ForegroundColor Cyan

# Set password environment variable so psql doesn't prompt for it
$env:PGPASSWORD = $DbPassword

# Command to drop and recreate the public schema
$SqlCommand = "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO public; GRANT ALL ON SCHEMA public TO $DbUser;"

# Execute the command
& $PsqlPath -h $DbHost -p $DbPort -U $DbUser -d $DbName -c $SqlCommand

if ($LASTEXITCODE -eq 0) {
    Write-Host "Database reset successfully!" -ForegroundColor Green
    Write-Host "The tables will be recreated automatically the next time you start the Spring Boot backend." -ForegroundColor Green
} else {
    Write-Host "An error occurred while resetting the database." -ForegroundColor Red
}

# Clear password environment variable
$env:PGPASSWORD = ""
