$login=@{ email='admin@meerkat.es'; password='Admin1234!' }|ConvertTo-Json
$response=Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/auth/login' -Method Post -Body $login -ContentType 'application/json'
Write-Host "login response:"; $response | Format-List *
$token=$response.accessToken
Write-Host "token length = $($token.Length)"
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/tutors?page=0&size=1' -Headers @{ Authorization = "Bearer $token" } -Verbose
