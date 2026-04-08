param(
    [string]$BindHost = "0.0.0.0",
    [int]$Port = 8000,
    [switch]$Reload
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$venvPython = Join-Path $repoRoot "venv\Scripts\python.exe"
$backendDir = $PSScriptRoot
$envFile = Join-Path $backendDir ".env"

if (-not (Test-Path $venvPython)) {
    throw "가상환경 Python을 찾지 못했어: $venvPython"
}

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }

        $key, $value = $line.Split("=", 2)
        [Environment]::SetEnvironmentVariable($key.Trim(), $value.Trim(), "Process")
    }
}

if (-not $env:SECRET_KEY) {
    $env:SECRET_KEY = "local-dev-secret-key"
}

if (-not $env:DATABASE_URL) {
    $env:DATABASE_URL = "sqlite:///./data/wordbook_server.db"
}

$uvicornArgs = @(
    "-m", "uvicorn",
    "main:app",
    "--host", $BindHost,
    "--port", $Port.ToString()
)

if ($Reload) {
    $uvicornArgs += "--reload"
}

Write-Output "backend 실행 준비 완료"
Write-Output " backendDir  : $backendDir"
Write-Output " python      : $venvPython"
Write-Output " host:port   : $BindHost`:$Port"
Write-Output " databaseUrl : $env:DATABASE_URL"

Push-Location $backendDir
try {
    & $venvPython @uvicornArgs
}
finally {
    Pop-Location
}
