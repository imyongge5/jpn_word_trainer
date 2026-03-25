param(
    [string]$GradleTask = "assembleDebug"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$javaHome = "<ANDROID_JBR>"
$sdkRoot = "<ANDROID_SDK>"

if (-not (Test-Path (Join-Path $projectRoot "gradlew.bat"))) {
    throw "gradlew.bat not found in $projectRoot"
}

if (-not (Test-Path $javaHome)) {
    throw "JAVA_HOME path not found: $javaHome"
}

if (-not (Test-Path $sdkRoot)) {
    throw "ANDROID_SDK_ROOT path not found: $sdkRoot"
}

$env:JAVA_HOME = $javaHome
$env:ANDROID_SDK_ROOT = $sdkRoot
$env:ANDROID_HOME = $sdkRoot
$env:Path = @(
    (Join-Path $javaHome "bin")
    (Join-Path $sdkRoot "platform-tools")
    (Join-Path $sdkRoot "emulator")
    (Join-Path $sdkRoot "cmdline-tools\latest\bin")
    [Environment]::GetEnvironmentVariable("Path", "Machine")
    [Environment]::GetEnvironmentVariable("Path", "User")
) -join ";"

Push-Location $projectRoot
try {
    Write-Host "Running Gradle task: $GradleTask"
    & ".\gradlew.bat" $GradleTask
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle task failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}
