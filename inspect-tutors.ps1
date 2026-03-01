$login=@{email='admin@meerkat.es';password='Admin1234!'}|ConvertTo-Json
$response=Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/auth/login' -Method Post -Body $login -ContentType 'application/json'
$token=$response.accessToken
$resp=Invoke-WebRequest -Uri 'http://localhost:8080/api/v1/tutors' -Headers @{Authorization="Bearer $token"} -UseBasicParsing
Write-Host "status" $resp.StatusCode
Write-Host "length" $($resp.Content.Length)
if($resp.Content.Length -gt 400){
    Write-Host "start:" ; Write-Host $resp.Content.Substring(0,200)
    Write-Host "end:" ; Write-Host $resp.Content.Substring($resp.Content.Length-200)
}
