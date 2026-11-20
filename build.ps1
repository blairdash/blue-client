<#
 Build Blue Client for one or all Minecraft versions.
   .\build.ps1                 # all versions
   .\build.ps1 -Version 26.3   # one version
 Jars land in .\dist\
#>
param([string]$Version, [switch]$Online)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$all = (Get-ChildItem versions -Directory).Name
$targets = if ($Version) { @($Version) } else { $all }
# 1.8.9 is a Forge (not Fabric) build in legacy\1.8.9 -> dist\legacy, never part of blueclient-all.jar
$legacy = if ($Version) { @($Version | Where-Object { Test-Path "legacy\$_" }) } else { (Get-ChildItem legacy -Directory -ErrorAction SilentlyContinue).Name }
$targets = @($targets | Where-Object { $legacy -notcontains $_ })
New-Item -ItemType Directory -Force dist | Out-Null
foreach ($v in $targets) {
    Write-Host "=== $v ===" -ForegroundColor Cyan
    $gradleArgs = @("-Pmc=$v", 'build', '-x', 'test')
    if (-not $Online) { $gradleArgs += '--offline' }
    & .\gradlew.bat @gradleArgs
    if ($LASTEXITCODE -ne 0) { throw "build failed for $v" }
    Get-ChildItem "build\$v\libs\*.jar" | Where-Object { $_.Name -notmatch 'sources' } | Copy-Item -Destination dist -Force
}
Write-Host "Done -> $PSScriptRoot\dist" -ForegroundColor Green

foreach ($v in $legacy) {
    Write-Host "=== $v (Forge, legacy) ===" -ForegroundColor Cyan
    New-Item -ItemType Directory -Force dist\legacy | Out-Null
    Push-Location "legacy\$v"
    try {
        & .\build8.bat build --offline --no-daemon
        if ($LASTEXITCODE -ne 0) { throw "build failed for $v" }
    } finally { Pop-Location }
    # The Forge build must ship the shaded jar (it bundles Mixin); publish it under the plain name.
    Get-ChildItem "legacy\$v\build\libs\*-dep.jar" | ForEach-Object { Copy-Item $_.FullName (Join-Path 'dist\legacy' ($_.Name -replace '-dep\.jar$', '.jar')) -Force }
}
