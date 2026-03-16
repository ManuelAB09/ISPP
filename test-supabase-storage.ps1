param(
    [string]$PayloadText = 'supabase smoke test',
    [string]$ObjectPrefix = 'smoke-tests',
    [switch]$KeepObject
)

$ErrorActionPreference = 'Stop'

function Load-EnvFile([string]$Path) {
    $vars = @{}
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
            $parts = $line.Split('=', 2)
            $vars[$parts[0]] = $parts[1]
        }
    }
    return $vars
}

$envPath = Join-Path $PSScriptRoot 'backend/src/main/resources/.env'
if (-not (Test-Path $envPath)) {
    throw ".env no encontrado en $envPath"
}

$envVars = Load-EnvFile $envPath
$supabaseUrl = $envVars['ZOOM_RECORDINGS_SUPABASE_URL']
$supabaseKey = $envVars['ZOOM_RECORDINGS_SUPABASE_SERVICE_ROLE_KEY']
$supabaseBucket = $envVars['ZOOM_RECORDINGS_SUPABASE_BUCKET']

if (-not $supabaseUrl -or -not $supabaseKey -or -not $supabaseBucket) {
    throw 'Falta configurar ZOOM_RECORDINGS_SUPABASE_URL, ZOOM_RECORDINGS_SUPABASE_SERVICE_ROLE_KEY o ZOOM_RECORDINGS_SUPABASE_BUCKET en .env'
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$objectName = "$ObjectPrefix/test-$timestamp.txt"
$uploadUrl = $supabaseUrl.TrimEnd('/') + '/storage/v1/object/' + $supabaseBucket + '/' + $objectName
$downloadUrl = $supabaseUrl.TrimEnd('/') + '/storage/v1/object/authenticated/' + $supabaseBucket + '/' + $objectName
$deleteUrl = $supabaseUrl.TrimEnd('/') + '/storage/v1/object/' + $supabaseBucket + '/' + $objectName

$payload = "${PayloadText} | created-at=$([DateTime]::UtcNow.ToString('o'))"
$payloadBytes = [System.Text.Encoding]::UTF8.GetBytes($payload)

$uploadHeaders = @{
    apikey        = $supabaseKey
    Authorization = "Bearer $supabaseKey"
    'x-upsert'    = 'true'
    'Content-Type'= 'text/plain; charset=utf-8'
}

$authHeaders = @{
    apikey        = $supabaseKey
    Authorization = "Bearer $supabaseKey"
}

Write-Host '== 1) Upload simple object to Supabase ==' -ForegroundColor Cyan
Invoke-RestMethod -Method Post -Uri $uploadUrl -Headers $uploadHeaders -Body $payloadBytes | Out-Null
Write-Host "OK upload -> $objectName" -ForegroundColor Green

Write-Host '== 2) Download object from Supabase ==' -ForegroundColor Cyan
$downloadedBytes = Invoke-RestMethod -Method Get -Uri $downloadUrl -Headers $authHeaders
if ($downloadedBytes -is [byte[]]) {
    $downloadedText = [System.Text.Encoding]::UTF8.GetString($downloadedBytes)
} else {
    $downloadedText = [string]$downloadedBytes
}

if ($downloadedText -notlike "$PayloadText*") {
    throw "Contenido inesperado descargado: $downloadedText"
}
Write-Host 'OK download and content validation' -ForegroundColor Green
Write-Host $downloadedText

if ($KeepObject) {
    Write-Host '== 3) Cleanup skipped by -KeepObject ==' -ForegroundColor Yellow
    Write-Host "Objeto conservado: $objectName"
    exit 0
}

Write-Host '== 3) Delete object from Supabase ==' -ForegroundColor Cyan
Invoke-RestMethod -Method Delete -Uri $deleteUrl -Headers $authHeaders | Out-Null
Write-Host 'OK delete' -ForegroundColor Green
