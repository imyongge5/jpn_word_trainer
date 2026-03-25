param(
    [string]$GradleTask = "assembleDebug",
    [switch]$SkipInstall,
    [string]$PackageName = "com.example.wordbookapp",
    [string]$LaunchActivity = ".MainActivity"
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

    $shouldInstall = -not $SkipInstall.IsPresent -and $GradleTask -eq "assembleDebug"
    if (-not $shouldInstall) {
        return
    }

    $apkPath = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apkPath)) {
        throw "APK not found at $apkPath"
    }

    $deviceLines = & adb devices
    $emulatorSerial = $deviceLines |
        Select-String "emulator-\d+\s+device" |
        ForEach-Object { ($_ -split "\s+")[0] } |
        Select-Object -First 1

    if (-not $emulatorSerial) {
        Write-Host "No running emulator found. Build finished, install skipped."
        return
    }

    Write-Host "Installing APK on $emulatorSerial"
    & adb -s $emulatorSerial install -r $apkPath
    if ($LASTEXITCODE -ne 0) {
        throw "APK install failed with exit code $LASTEXITCODE"
    }

    Write-Host "Launching $PackageName/$LaunchActivity"
    & adb -s $emulatorSerial shell am start -n "$PackageName/$LaunchActivity"
    if ($LASTEXITCODE -ne 0) {
        throw "App launch failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}
