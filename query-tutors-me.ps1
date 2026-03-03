$login=@{email='admin@meerkat.es';password='Admin1234!'}|ConvertTo-Json
$response=Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/auth/login' -Method Post -Body $login -ContentType 'application/json'
$token=$response.accessToken
Write-Host "token len:" $token.Length
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/tutors/me' -Headers @{Authorization="Bearer $token"} -Verbose
