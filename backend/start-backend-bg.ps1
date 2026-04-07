param(
    [string]$BindHost = "127.0.0.1",
    [int]$Port = 8000,
    [switch]$Reload
)

$ErrorActionPreference = "Stop"

$scriptPath = Join-Path $PSScriptRoot "run-backend.ps1"
$arguments = @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", $scriptPath,
    "-BindHost", $BindHost,
    "-Port", $Port.ToString()
)

if ($Reload) {
    $arguments += "-Reload"
}

$process = Start-Process -FilePath "powershell.exe" -ArgumentList $arguments -WorkingDirectory $PSScriptRoot -PassThru

Write-Output "backend 백그라운드 실행 시작"
Write-Output " pid       : $($process.Id)"
Write-Output " host:port : $BindHost`:$Port"
