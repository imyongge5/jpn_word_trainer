param(
    [string]$TesterEmail = "crossflex682@gmail.com",
    [string]$ServiceAccountPath = "",
    [switch]$KeepTempDir
)

$ErrorActionPreference = "Stop"

$script:LastCommand = ""
$script:LastStep = "initialization"

function Write-Step([string]$message) {
    Write-Host ""
    Write-Host "== $message =="
}

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Command,
        [Parameter(Mandatory = $true)]
        [scriptblock]$ScriptBlock
    )

    $script:LastCommand = $Command
    & $ScriptBlock
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $Command"
    }
}

function Get-RepoRoot([string]$scriptRoot) {
    return Split-Path -Parent $scriptRoot
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

function Get-VersionTag([string]$workdir) {
    $versionFile = Join-Path $workdir "version-series.properties"
    if (-not (Test-Path $versionFile)) {
        throw "version-series.properties not found in $workdir"
    }

    $properties = @{}
    foreach ($line in Get-Content $versionFile) {
        if ($line -match "^\s*#" -or $line -notmatch "=") {
            continue
        }
        $parts = $line -split "=", 2
        $properties[$parts[0].Trim()] = $parts[1].Trim()
    }

    $versionA = $properties["VERSION_A"]
    $versionB = $properties["VERSION_B"]
    if ([string]::IsNullOrWhiteSpace($versionA) -or [string]::IsNullOrWhiteSpace($versionB)) {
        throw "VERSION_A or VERSION_B is missing in version-series.properties"
    }

    $baseCommit = (git -C $workdir log -n 1 --format=%H -- version-series.properties).Trim()
    if ([string]::IsNullOrWhiteSpace($baseCommit)) {
        throw "Failed to resolve base commit from version-series.properties"
    }

    $commitsAfterBase = (git -C $workdir rev-list --count "$baseCommit..HEAD").Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($commitsAfterBase)) {
        throw "Failed to count commits after base commit"
    }

    $patch = [int]$commitsAfterBase + 1
    return "v$versionA.$versionB.$patch"
}

function Write-GitDiagnostics([string]$repoRoot) {
    Write-Host ""
    Write-Host "---- git diagnostics ----"
    Write-Host "branch: $(git -C $repoRoot branch --show-current)"
    Write-Host "local HEAD: $(git -C $repoRoot rev-parse HEAD)"
    $remoteHead = git -C $repoRoot rev-parse refs/remotes/origin/beta 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "origin/beta: $($remoteHead.Trim())"
    } else {
        Write-Host "origin/beta: <unavailable>"
    }
    Write-Host "status:"
    git -C $repoRoot status --short
    Write-Host "-------------------------"
}

$projectRoot = Get-RepoRoot $PSScriptRoot
$resolvedServiceAccountPath = if ([string]::IsNullOrWhiteSpace($ServiceAccountPath)) {
    Resolve-FirstExistingPath @(
        (Join-Path $projectRoot "jpn-word-trainer-firebase-adminsdk-fbsvc-b9f21aa44d.json")
        (Join-Path $projectRoot "firebase-adminsdk.json")
    )
} else {
    Resolve-FirstExistingPath @($ServiceAccountPath)
}

$javaHome = Resolve-FirstExistingPath @(
    $env:JAVA_HOME
    (Join-Path ${env:ProgramFiles} "Android\jbr")
    (Join-Path ${env:ProgramFiles} "Android Studio\jbr")
)

$sdkRoot = Resolve-FirstExistingPath @(
    $env:ANDROID_SDK_ROOT
    $env:ANDROID_HOME
    (Join-Path ${env:LOCALAPPDATA} "Android\Sdk")
    (Join-Path ${env:ProgramFiles} "Android SDK")
)

if (-not (Test-Path (Join-Path $projectRoot "gradlew.bat"))) {
    throw "gradlew.bat not found in $projectRoot"
}

if (-not $javaHome) {
    throw "JAVA_HOME not found. Set JAVA_HOME or install Android Studio with a bundled JDK."
}

if (-not $sdkRoot) {
    throw "ANDROID_SDK_ROOT not found. Set ANDROID_SDK_ROOT or install the Android SDK."
}

if (-not $resolvedServiceAccountPath) {
    throw "Firebase service account JSON file not found. Pass -ServiceAccountPath or place the file in the repo root."
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

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("wordbook-beta-build-" + [System.Guid]::NewGuid().ToString("N"))
$archivePath = Join-Path $tempRoot "repo.tar"
$buildRoot = Join-Path $tempRoot "repo"
$notesFile = Join-Path $tempRoot "firebase-release-notes.txt"

New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null

Push-Location $projectRoot
try {
    $script:LastStep = "branch validation"
    Write-Step "beta 브랜치 확인"
    $currentBranch = (git branch --show-current).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to determine current branch"
    }
    if ($currentBranch -ne "beta") {
        throw "This script can only run from the beta branch. Current branch: $currentBranch"
    }

    $script:LastStep = "remote validation"
    Write-Step "origin remote 확인"
    $originUrl = (git remote get-url origin).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($originUrl)) {
        throw "origin remote is not configured"
    }
    Write-Host "origin: $originUrl"

    $script:LastStep = "sync check"
    Write-Step "origin/beta 최신 커밋 동기화 확인"
    Invoke-CheckedCommand "git fetch origin beta" { git fetch origin beta }

    $localHead = (git rev-parse HEAD).Trim()
    $remoteHead = (git rev-parse refs/remotes/origin/beta).Trim()
    Write-Host "local HEAD : $localHead"
    Write-Host "origin/beta: $remoteHead"

    if ($localHead -ne $remoteHead) {
        Write-Host "로컬 HEAD가 origin/beta와 다릅니다. push를 시도합니다."
        Invoke-CheckedCommand "git push origin HEAD:refs/heads/beta" { git push origin HEAD:refs/heads/beta }
        Invoke-CheckedCommand "git fetch origin beta" { git fetch origin beta }

        $localHeadAfterPush = (git rev-parse HEAD).Trim()
        $remoteHeadAfterPush = (git rev-parse refs/remotes/origin/beta).Trim()
        Write-Host "push 후 local HEAD : $localHeadAfterPush"
        Write-Host "push 후 origin/beta: $remoteHeadAfterPush"
        if ($localHeadAfterPush -ne $remoteHeadAfterPush) {
            Write-GitDiagnostics $projectRoot
            throw "HEAD and origin/beta still differ after push verification"
        }
    }

    $script:LastStep = "version calculation"
    Write-Step "버전 계산"
    $versionTag = Get-VersionTag $projectRoot
    Write-Host "이 커밋의 버전은 $versionTag 입니다."

    $script:LastStep = "archive export"
    Write-Step "HEAD 커밋을 임시 작업 디렉터리로 내보내기"
    Invoke-CheckedCommand "git archive --format=tar HEAD --output `"$archivePath`"" {
        git archive --format=tar HEAD --output $archivePath
    }
    Invoke-CheckedCommand "tar -xf `"$archivePath`" -C `"$buildRoot`"" {
        tar -xf $archivePath -C $buildRoot
    }

    $script:LastStep = "build"
    Write-Step "로컬 release 빌드 및 Firebase 배포"
    $env:GOOGLE_APPLICATION_CREDENTIALS = $resolvedServiceAccountPath
    $env:FIREBASE_APP_DISTRIBUTION_TESTERS = $TesterEmail
    $env:FIREBASE_DISTRIBUTION_ARTIFACT_PATH = "app/build/outputs/apk/release/app-release.apk"
    $env:APP_VERSION_TAG = $versionTag

    @(
        "Local beta latest commit build"
        "Version: $versionTag"
        "Commit: $localHead"
        "Built from archived HEAD"
        "Date: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")"
    ) | Set-Content -Path $notesFile -Encoding UTF8
    $env:FIREBASE_RELEASE_NOTES_FILE = $notesFile

    Push-Location $buildRoot
    try {
        Invoke-CheckedCommand ".\gradlew.bat assembleRelease appDistributionUploadRelease -PfirebaseServiceCredentialsFile=`"$resolvedServiceAccountPath`" -PfirebaseArtifactPath=`"$env:FIREBASE_DISTRIBUTION_ARTIFACT_PATH`"" {
            .\gradlew.bat assembleRelease appDistributionUploadRelease -PfirebaseServiceCredentialsFile="$resolvedServiceAccountPath" -PfirebaseArtifactPath="$env:FIREBASE_DISTRIBUTION_ARTIFACT_PATH"
        }
    }
    finally {
        Pop-Location
    }

    $script:LastStep = "artifact verification"
    $apkPath = Join-Path $buildRoot "app\build\outputs\apk\release\app-release.apk"
    if (-not (Test-Path $apkPath)) {
        throw "APK not found at $apkPath"
    }
    Write-Host "APK created: $apkPath"
    Write-Host "Firebase 배포가 완료되었습니다."
}
catch {
    Write-Host ""
    Write-Host "FAILED STEP: $script:LastStep" -ForegroundColor Red
    if ($script:LastCommand) {
        Write-Host "LAST COMMAND: $script:LastCommand" -ForegroundColor Red
    }
    Write-GitDiagnostics $projectRoot
    Write-Error $_
    exit 1
}
finally {
    Pop-Location
    if ($KeepTempDir.IsPresent) {
        Write-Host "임시 작업 디렉터리 유지: $tempRoot"
    } else {
        try {
            if (Test-Path $tempRoot) {
                Remove-Item $tempRoot -Recurse -Force
            }
        } catch {
            Write-Host "임시 작업 디렉터리를 지우지 못했습니다: $tempRoot"
        }
    }
}
