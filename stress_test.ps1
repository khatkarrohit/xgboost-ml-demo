# stress_test.ps1
param (
    [int]$count = 1000,
    [switch]$leak = $false
)

$url = "http://localhost:8080/api/test/stress?count=$count&leak=$($leak.IsPresent)"
Write-Host "Triggering stress test: $url"
Invoke-RestMethod -Method Post -Uri $url
