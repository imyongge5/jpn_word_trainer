param(
    [string]$Remote = "origin"
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$versionFile = Join-Path $projectRoot "version.properties"

function Get-VersionMap {
    param([string]$Path)

    $map = @{}
    foreach ($line in Get-Content $Path) {
        if (-not $line -or $line.Trim().StartsWith("#")) {
            continue
        }
        $parts = $line.Split("=", 2)
        if ($parts.Count -eq 2) {
            $map[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
    return $map
}

function Write-VersionMap {
    param(
        [string]$Path,
        [hashtable]$VersionMap
    )

    @(
        "VERSION_A=$($VersionMap.VERSION_A)"
        "VERSION_B=$($VersionMap.VERSION_B)"
        "VERSION_CCC=$($VersionMap.VERSION_CCC)"
    ) | Set-Content -Path $Path -Encoding UTF8
}

$currentBranch = (git branch --show-current).Trim()
if ($currentBranch -ne "beta") {
    throw "이 스크립트는 beta 브랜치에서만 실행할 수 있습니다. 현재 브랜치: $currentBranch"
}

$versionMap = Get-VersionMap -Path $versionFile
$versionA = [int]$versionMap.VERSION_A
$versionB = [int]$versionMap.VERSION_B
$versionCcc = [int]$versionMap.VERSION_CCC + 1

$versionMap.VERSION_CCC = $versionCcc
Write-VersionMap -Path $versionFile -VersionMap $versionMap

$versionName = "v$versionA.$versionB.$versionCcc"

git add $versionFile
git commit -m "버전을 $versionName 로 갱신"
git push $Remote beta

Write-Host "beta 푸시 완료: $versionName"
