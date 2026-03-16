param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Email = 'admin@meerkat.es',
    [string]$Password = 'Admin1234!',
    [Parameter(Mandatory = $true)]
    [long]$CommunityId,
    [string]$RecordingId = ''
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

Write-Host '== 1) Cargando .env y validando Supabase ==' -ForegroundColor Cyan
$envPath = Join-Path $PSScriptRoot 'backend/src/main/resources/.env'
if (-not (Test-Path $envPath)) {
    throw ".env no encontrado en $envPath"
}

$envVars = Load-EnvFile $envPath
$supabaseUrl = $envVars['ZOOM_RECORDINGS_SUPABASE_URL']
$supabaseKey = $envVars['ZOOM_RECORDINGS_SUPABASE_SERVICE_ROLE_KEY']
$supabaseBucket = $envVars['ZOOM_RECORDINGS_SUPABASE_BUCKET']
$storageMode = $envVars['ZOOM_RECORDINGS_STORAGE_MODE']

Write-Host "storage-mode: $storageMode"
if ($storageMode -ne 'supabase') {
    Write-Warning 'ZOOM_RECORDINGS_STORAGE_MODE no está en supabase.'
}
if (-not $supabaseUrl -or -not $supabaseKey -or -not $supabaseBucket) {
    throw 'Falta configurar ZOOM_RECORDINGS_SUPABASE_URL, ZOOM_RECORDINGS_SUPABASE_SERVICE_ROLE_KEY o ZOOM_RECORDINGS_SUPABASE_BUCKET en .env'
}

$headers = @{
    apikey        = $supabaseKey
    Authorization = "Bearer $supabaseKey"
}

$bucketUrl = $supabaseUrl.TrimEnd('/') + '/storage/v1/bucket/' + $supabaseBucket
try {
    $bucketResp = Invoke-RestMethod -Method Get -Uri $bucketUrl -Headers $headers
    Write-Host "OK Supabase bucket: $($bucketResp.name)" -ForegroundColor Green
}
catch {
    Write-Host 'ERROR validando bucket en Supabase' -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host $_.ErrorDetails.Message
    }
    throw
}

Write-Host '== 2) Login backend ==' -ForegroundColor Cyan
$loginBody = @{ email = $Email; password = $Password } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body $loginBody -ContentType 'application/json'
$token = $loginResp.accessToken
if (-not $token) {
    throw 'No se recibió accessToken en login'
}
Write-Host "OK login. token length=$($token.Length)" -ForegroundColor Green

$authHeaders = @{ Authorization = "Bearer $token" }

Write-Host '== 3) Probar endpoint de listado de grabaciones ==' -ForegroundColor Cyan
$listUrl = "$BaseUrl/api/v1/zoom/communities/$CommunityId/recordings"
$listResp = Invoke-RestMethod -Uri $listUrl -Method Get -Headers $authHeaders

if ($listResp -is [System.Array]) {
    Write-Host "OK listado. total grabaciones=$($listResp.Count)" -ForegroundColor Green
}
else {
    Write-Host 'OK listado respondido (objeto único o vacío serializado).' -ForegroundColor Green
}

if ($RecordingId) {
    Write-Host '== 4) Probar detalle de grabación ==' -ForegroundColor Cyan
    $detailUrl = "$BaseUrl/api/v1/zoom/communities/$CommunityId/recordings/$RecordingId"
    $detailResp = Invoke-RestMethod -Uri $detailUrl -Method Get -Headers $authHeaders
    Write-Host "OK detalle. status=$($detailResp.status), expiresAt=$($detailResp.expiresAt)" -ForegroundColor Green

    Write-Host '== 5) Probar descarga de grabación ==' -ForegroundColor Cyan
    $downloadUrl = "$BaseUrl/api/v1/zoom/communities/$CommunityId/recordings/$RecordingId/download"
    $outFile = Join-Path $PSScriptRoot "zoom-recording-$RecordingId.bin"

    Invoke-WebRequest -Uri $downloadUrl -Method Get -Headers $authHeaders -OutFile $outFile | Out-Null
    $size = (Get-Item $outFile).Length
    Write-Host "OK descarga. fichero: $outFile ($size bytes)" -ForegroundColor Green
}
else {
    Write-Host 'RecordingId no proporcionado: se omitió detalle/descarga.' -ForegroundColor Yellow
}

Write-Host '== Validación completada ==' -ForegroundColor Cyan
