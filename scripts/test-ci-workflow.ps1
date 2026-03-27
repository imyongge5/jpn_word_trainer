param(
    [ValidateSet("BuildTag", "DistributeFirebase")]
    [string]$WorkflowMode = "BuildTag",
    [string]$EnvFilePath = "",
    [string]$ReleaseTag = "",
    [string]$TesterEmail = "",
    [string]$Groups = "",
    [string]$ServiceAccountPath = "",
    [string]$WorkflowPushToken = "",
    [switch]$SkipPublishRelease,
    [switch]$SkipFirebaseDistribution,
    [switch]$KeepTempFiles
)

$ErrorActionPreference = "Stop"

function Write-Step([string]$message) {
    Write-Host ""
    Write-Host "== $message =="
}

function Run-Command {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Description,
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [string[]]$Arguments = @()
    )

    Write-Host $Description
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $Description"
    }
}

function Resolve-FirstExistingPath([string[]]$paths) {
    foreach ($path in $paths) {
        if ([string]::IsNullOrWhiteSpace($path)) {
            continue
        }
        if (Test-Path $path) {
            return (Resolve-Path $path).Path
        }
    }
    return $null
}

function Read-KeyValueFile([string]$path) {
    $map = @{}
    if (-not (Test-Path $path)) {
        return $map
    }
    foreach ($line in Get-Content $path) {
        $normalizedLine = $line.TrimStart([char]0xFEFF)
        if ([string]::IsNullOrWhiteSpace($normalizedLine) -or $normalizedLine.StartsWith("#") -or $normalizedLine -notmatch "=") {
            continue
        }
        $parts = $normalizedLine -split "=", 2
        $map[$parts[0]] = $parts[1]
    }
    return $map
}

function Import-GithubEnv([string]$path) {
    foreach ($entry in (Read-KeyValueFile $path).GetEnumerator()) {
        Set-Item -Path "Env:$($entry.Key)" -Value $entry.Value
    }
    if (Test-Path $path) {
        Clear-Content $path
    }
}

function Read-GithubOutputs([string]$path) {
    return Read-KeyValueFile $path
}

function Import-EnvFile([string]$path) {
    if (-not (Test-Path $path)) {
        return
    }
    foreach ($entry in (Read-KeyValueFile $path).GetEnumerator()) {
        if ([string]::IsNullOrWhiteSpace($entry.Key)) {
            continue
        }
        Set-Item -Path "Env:$($entry.Key)" -Value $entry.Value
    }
}

function Get-GitHubRepositoryFromOrigin([string]$repoRoot) {
    $originUrl = (git -C $repoRoot remote get-url origin).Trim()
    if ([string]::IsNullOrWhiteSpace($originUrl)) {
        throw "origin remote is not configured"
    }
    if ($originUrl -match "github\.com[:/](.+?)(?:\.git)?$") {
        return $matches[1]
    }
    throw "Could not derive GITHUB_REPOSITORY from origin URL: $originUrl"
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$venvPython = Join-Path $projectRoot "venv\Scripts\python.exe"
$resolvedEnvFilePath = if ([string]::IsNullOrWhiteSpace($EnvFilePath)) {
    Resolve-FirstExistingPath @((Join-Path $projectRoot "scripts\local-ci.env"))
} else {
    Resolve-FirstExistingPath @($EnvFilePath)
}

if ($resolvedEnvFilePath) {
    Import-EnvFile $resolvedEnvFilePath
}

$javaHome = Resolve-FirstExistingPath @(
    $env:JAVA_HOME,
    (Join-Path ${env:ProgramFiles} "Android\jbr"),
    (Join-Path ${env:ProgramFiles} "Android Studio\jbr")
)
$sdkRoot = Resolve-FirstExistingPath @(
    $env:ANDROID_SDK_ROOT,
    $env:ANDROID_HOME,
    (Join-Path ${env:LOCALAPPDATA} "Android\Sdk"),
    (Join-Path ${env:ProgramFiles} "Android SDK")
)
$resolvedServiceAccountPath = if ([string]::IsNullOrWhiteSpace($ServiceAccountPath)) {
    $null
} else {
    Resolve-FirstExistingPath @($ServiceAccountPath)
}

if (-not (Test-Path (Join-Path $projectRoot "gradlew.bat"))) {
    throw "gradlew.bat not found in $projectRoot"
}
if (-not $javaHome) {
    throw "JAVA_HOME not found. Set JAVA_HOME or install Android Studio with a bundled JDK."
}
if (-not $sdkRoot) {
    throw "ANDROID_SDK_ROOT not found. Set ANDROID_SDK_ROOT or install the Android SDK."
}

if (-not (Test-Path $venvPython)) {
    Write-Step "Create Python virtual environment"
    Push-Location $projectRoot
    try {
        Run-Command -Description "python -m venv venv" -FilePath "python" -Arguments @("-m", "venv", "venv")
    }
    finally {
        Pop-Location
    }
}

$env:JAVA_HOME = $javaHome
$env:ANDROID_SDK_ROOT = $sdkRoot
$env:ANDROID_HOME = $sdkRoot
$env:Path = @(
    (Join-Path $javaHome "bin"),
    (Join-Path $sdkRoot "platform-tools"),
    (Join-Path $sdkRoot "emulator"),
    (Join-Path $sdkRoot "cmdline-tools\latest\bin"),
    [Environment]::GetEnvironmentVariable("Path", "Machine"),
    [Environment]::GetEnvironmentVariable("Path", "User")
) -join ";"

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("wordbook-ci-local-" + [System.Guid]::NewGuid().ToString("N"))
$githubOutput = Join-Path $tempRoot "github-output.txt"
$githubEnv = Join-Path $tempRoot "github-env.txt"
New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
New-Item -ItemType File -Path $githubOutput -Force | Out-Null
New-Item -ItemType File -Path $githubEnv -Force | Out-Null

$env:GITHUB_OUTPUT = $githubOutput
$env:GITHUB_ENV = $githubEnv
$env:RUNNER_TEMP = $tempRoot
$env:GITHUB_REF_NAME = if ($WorkflowMode -eq "BuildTag") { "build" } else { $ReleaseTag }
$env:GITHUB_SHA = (git -C $projectRoot rev-parse HEAD).Trim()
$env:GITHUB_EVENT_NAME = "local-script"
$env:GITHUB_REPOSITORY = Get-GitHubRepositoryFromOrigin $projectRoot

if ([string]::IsNullOrWhiteSpace($env:FIREBASE_SERVICE_ACCOUNT)) {
    $envFileDir = if ($resolvedEnvFilePath) { Split-Path -Parent $resolvedEnvFilePath } else { $null }
    $resolvedServiceAccountFileFromEnv = Resolve-FirstExistingPath @(
        $env:FIREBASE_SERVICE_ACCOUNT_FILE,
        $(if ($envFileDir -and -not [string]::IsNullOrWhiteSpace($env:FIREBASE_SERVICE_ACCOUNT_FILE)) { Join-Path $envFileDir $env:FIREBASE_SERVICE_ACCOUNT_FILE } else { $null }),
        $(if (-not [string]::IsNullOrWhiteSpace($env:FIREBASE_SERVICE_ACCOUNT_FILE)) { Join-Path $projectRoot $env:FIREBASE_SERVICE_ACCOUNT_FILE } else { $null })
    )
    if ($resolvedServiceAccountFileFromEnv) {
        $env:FIREBASE_SERVICE_ACCOUNT = Get-Content $resolvedServiceAccountFileFromEnv -Raw
    }
}

if ([string]::IsNullOrWhiteSpace($env:FIREBASE_SERVICE_ACCOUNT)) {
    if (-not $resolvedServiceAccountPath) {
        throw "FIREBASE_SERVICE_ACCOUNT is not set. Put it in the current shell or env file, or set FIREBASE_SERVICE_ACCOUNT_FILE / -ServiceAccountPath."
    }
    $env:FIREBASE_SERVICE_ACCOUNT = Get-Content $resolvedServiceAccountPath -Raw
}

if (-not [string]::IsNullOrWhiteSpace($TesterEmail)) {
    $env:FIREBASE_APP_DISTRIBUTION_TESTERS = $TesterEmail
}
if (-not [string]::IsNullOrWhiteSpace($Groups)) {
    $env:FIREBASE_APP_DISTRIBUTION_GROUPS = $Groups
}
if (-not [string]::IsNullOrWhiteSpace($WorkflowPushToken)) {
    $env:WORKFLOW_PUSH_TOKEN = $WorkflowPushToken
}

Push-Location $projectRoot
try {
    Write-Host "Workflow mode: $WorkflowMode"

    Write-Step "Install CI Python dependencies"
    Run-Command -Description "$venvPython -m pip install --upgrade pip" -FilePath $venvPython -Arguments @("-m", "pip", "install", "--upgrade", "pip")
    Run-Command -Description "$venvPython -m pip install -r scripts/requirements-ci.txt" -FilePath $venvPython -Arguments @("-m", "pip", "install", "-r", "scripts/requirements-ci.txt")

    if ($WorkflowMode -eq "BuildTag") {
        Write-Step "Compute version"
        Run-Command -Description "$venvPython scripts/ci_compute_version.py --output-name version_tag" -FilePath $venvPython -Arguments @("scripts/ci_compute_version.py", "--output-name", "version_tag")
        Import-GithubEnv $githubEnv
        $outputs = Read-GithubOutputs $githubOutput
        $versionTag = $outputs["version_tag"]
        if ([string]::IsNullOrWhiteSpace($versionTag)) {
            throw "version_tag output was not produced"
        }

        Write-Step "Build release APK"
        Run-Command -Description "$venvPython scripts/ci_build_release.py --version-tag $versionTag" -FilePath $venvPython -Arguments @("scripts/ci_build_release.py", "--version-tag", $versionTag)

        if ($SkipPublishRelease.IsPresent) {
            Write-Host "Skipping GitHub Release publish step."
        } else {
            Write-Step "Publish version tag and GitHub Release"
            Run-Command -Description "$venvPython scripts/ci_publish_release.py --version-tag $versionTag" -FilePath $venvPython -Arguments @("scripts/ci_publish_release.py", "--version-tag", $versionTag, "--apk-path", "app/build/outputs/apk/release/app-release.apk")
        }

        if ($SkipFirebaseDistribution.IsPresent) {
            Write-Host "Skipping Firebase distribution step."
        } else {
            Write-Step "Distribute to Firebase App Distribution"
            Run-Command -Description "$venvPython scripts/ci_distribute_firebase.py --version-tag $versionTag" -FilePath $venvPython -Arguments @("scripts/ci_distribute_firebase.py", "--version-tag", $versionTag, "--note-line", "Build tag: build", "--note-line", "Version: $versionTag", "--note-line", "Commit: $env:GITHUB_SHA", "--artifact-path", "app/build/outputs/apk/release/app-release.apk")
        }
    } else {
        if ([string]::IsNullOrWhiteSpace($ReleaseTag)) {
            throw "DistributeFirebase mode requires -ReleaseTag"
        }

        Write-Step "Download release APK"
        Run-Command -Description "$venvPython scripts/ci_download_release_apk.py --release-tag $ReleaseTag" -FilePath $venvPython -Arguments @("scripts/ci_download_release_apk.py", "--release-tag", $ReleaseTag, "--output-path", "app/build/outputs/apk/release/app-release.apk")

        if ($SkipFirebaseDistribution.IsPresent) {
            Write-Host "Skipping Firebase distribution step."
        } else {
            Write-Step "Distribute to Firebase App Distribution"
            Run-Command -Description "$venvPython scripts/ci_distribute_firebase.py --version-tag $ReleaseTag" -FilePath $venvPython -Arguments @("scripts/ci_distribute_firebase.py", "--version-tag", $ReleaseTag, "--note-line", "Release tag: $ReleaseTag", "--note-line", "Triggered by: local-script", "--note-line", "Repository: $env:GITHUB_REPOSITORY", "--artifact-path", "app/build/outputs/apk/release/app-release.apk")
        }
    }

    Write-Host ""
    Write-Host "Local CI workflow test completed."
}
finally {
    Pop-Location
    if ($KeepTempFiles.IsPresent) {
        Write-Host "Keeping temp GitHub output files: $tempRoot"
    } else {
        try {
            if (Test-Path $tempRoot) {
                Remove-Item -Recurse -Force $tempRoot
            }
        }
        catch {
            Write-Host "Could not remove temp files: $tempRoot"
        }
    }
}
