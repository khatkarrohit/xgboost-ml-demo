# monitor_memory.ps1
# This script monitors the memory usage of the Spring Boot application.

$processName = "java"
Write-Host "Monitoring memory for '$processName' process..."
Write-Host "Press Ctrl+C to stop."
Write-Host "Time                | Private Memory (MB) | Working Set (MB)"
Write-Host "-----------------------------------------------------------"

while($true) {
    # Using CIM to get command line for filtering
    $procInfo = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object { $_.CommandLine -like "*xgboost-ml-demo*" }
    if ($procInfo) {
        $proc = Get-Process -Id $procInfo.ProcessId
        $memPrivate = [math]::Round($proc.PrivateMemorySize64 / 1MB, 2)
        $memWorking = [math]::Round($proc.WorkingSet64 / 1MB, 2)
        $timestamp = Get-Date -Format "HH:mm:ss"
        Write-Host "$timestamp            | $memPrivate              | $memWorking"
    } else {
        Write-Host "Application process not found."
    }
    Start-Sleep -Seconds 2
}
