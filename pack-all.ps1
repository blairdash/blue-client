<#
 Packs every jar in .\dist into ONE jar (dist\blueclient-all.jar) using Fabric jar-in-jar.
 Fabric picks the nested Blue Client build whose Minecraft/Java requirements match the running game.
 Run .\build.ps1 first.
#>
param([string]$OutVersion = '1.4.3')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jars = Get-ChildItem dist -Filter 'blueclient-*.jar' | Where-Object { $_.Name -notmatch 'sources|^blueclient-all' }
if (-not $jars) { throw 'no jars in dist - run build.ps1 first' }
$out = Join-Path $PSScriptRoot 'dist\blueclient-all.jar'
if (Test-Path $out) { Remove-Item $out }
$entries = ($jars | ForEach-Object { '    { "file": "META-INF/jars/' + $_.Name + '" }' }) -join ",`n"
$json = @"
{
  "schemaVersion": 1,
  "id": "blueclient-bundle",
  "version": "$OutVersion",
  "name": "Blue Client",
  "description": "Blue Client for every supported Minecraft version in a single jar.",
  "environment": "client",
  "icon": "icon.png",
  "jars": [
$entries
  ]
}
"@
$zip = [IO.Compression.ZipFile]::Open($out, 'Create')
try {
    $e = $zip.CreateEntry('fabric.mod.json'); $w = New-Object IO.StreamWriter($e.Open(), (New-Object Text.UTF8Encoding($false))); $w.Write($json); $w.Close()
    $icon = Get-ChildItem 'groups\yarn\src\main\resources\assets\clientmod\textures\gui\logo.png' -ErrorAction SilentlyContinue
    if ($icon) { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $icon.FullName, 'icon.png') | Out-Null }
    foreach ($j in $jars) { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $j.FullName, "META-INF/jars/$($j.Name)", 'NoCompression') | Out-Null }
} finally { $zip.Dispose() }
Write-Host ("Packed {0} builds -> {1} ({2:N1} MB)" -f $jars.Count, $out, ((Get-Item $out).Length / 1MB)) -ForegroundColor Green
