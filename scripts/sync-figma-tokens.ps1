param(
    [string]$TokenPath = "",
    [switch]$Quiet
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($TokenPath)) {
    $TokenPath = Join-Path $projectRoot "design\figma-tokens.json"
}

$colorFilePath = Join-Path $projectRoot "app\src\main\java\com\example\wordbookapp\ui\theme\Color.kt"

if (-not (Test-Path $TokenPath)) {
    if (-not $Quiet.IsPresent) {
        Write-Host "Figma token file not found. Skipping design sync: $TokenPath"
    }
    return
}

if (-not (Test-Path $colorFilePath)) {
    throw "Color.kt not found: $colorFilePath"
}

$tokens = Get-Content $TokenPath -Raw | ConvertFrom-Json

function Format-DoubleLiteral([double]$Value) {
    $formatted = [string]::Format([Globalization.CultureInfo]::InvariantCulture, "{0:0.###}", $Value)
    if ($formatted -notmatch "\.") {
        return "$formatted.0"
    }
    return $formatted
}

$requiredOrder = @(
    "Paper",
    "PaperElevated",
    "SurfaceSoft",
    "SurfaceTint",
    "SurfaceContainer",
    "SurfaceContainerHigh",
    "Ink",
    "InkSoft",
    "InkMuted",
    "PrimaryBlue",
    "PrimaryBlueSoft",
    "PrimaryBlueContainer",
    "SecondaryCoral",
    "SecondaryCoralSoft",
    "SecondaryCoralContainer",
    "DividerSoft",
    "CardBorderStrong"
)

foreach ($name in $requiredOrder) {
    if (-not $tokens.PSObject.Properties.Name.Contains($name)) {
        throw "Missing token '$name' in $TokenPath"
    }
}

$lines = @(
    "package com.example.wordbookapp.ui.theme",
    "",
    "// Generated from design/figma-tokens.json by scripts/sync-figma-tokens.ps1",
    "// Update tokens from Figma export, then run the build script.",
    ""
)

foreach ($name in $requiredOrder) {
    $token = $tokens.$name
    $lightness = Format-DoubleLiteral([double]$token.lightness)
    $chroma = Format-DoubleLiteral([double]$token.chroma)
    $hue = Format-DoubleLiteral([double]$token.hue)
    $lines += "val $name = oklch($lightness, $chroma, $hue)"
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($colorFilePath, ($lines -join [Environment]::NewLine), $utf8NoBom)

if (-not $Quiet.IsPresent) {
    Write-Host "Applied Figma tokens from $TokenPath"
}
