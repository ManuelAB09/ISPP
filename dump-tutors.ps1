$login=@{email='admin@meerkat.es';password='Admin1234!'}|ConvertTo-Json
$response=Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/auth/login' -Method Post -Body $login -ContentType 'application/json'
$token=$response.accessToken
Write-Host "token len" $token.Length
$resp=Invoke-WebRequest -Uri 'http://localhost:8080/api/v1/tutors' -Headers @{Authorization="Bearer $token"} -UseBasicParsing
Write-Host "status" $resp.StatusCode
Write-Host "body start"
$resp.Content | Out-String | Write-Host
Write-Host "body end"