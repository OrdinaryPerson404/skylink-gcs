# SkyLink GCS jpackage packaging script (Windows)
# Usage: powershell -ExecutionPolicy Bypass -File package-jpackage.ps1
# Output: target/installer/ (tries exe installer first, requires WiX; falls back to app-image dir)
$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$ver = "1.0.0"
$jdkCandidates = @(
    "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot",
    "$env:JAVA_HOME"
)
$jpackage = $null
$jar = $null
foreach ($jdk in $jdkCandidates) {
    if ($jdk -and (Test-Path (Join-Path $jdk "bin\jpackage.exe"))) {
        $jpackage = Join-Path $jdk "bin\jpackage.exe"
        $jar = Join-Path $jdk "bin\jar.exe"
        break
    }
}
if (-not $jpackage) { throw "jpackage.exe not found, check JDK 17 install path" }

$m2 = "$env:USERPROFILE\.m2\repository"
$javafx = "$m2\org\openjfx"
$depJars = @(
    "$m2\com\fazecast\jSerialComm\2.11.0\jSerialComm-2.11.0.jar",
    "$javafx\javafx-base\17.0.2\javafx-base-17.0.2-win.jar",
    "$javafx\javafx-graphics\17.0.2\javafx-graphics-17.0.2-win.jar",
    "$javafx\javafx-controls\17.0.2\javafx-controls-17.0.2-win.jar",
    "$javafx\javafx-web\17.0.2\javafx-web-17.0.2-win.jar",
    "$javafx\javafx-media\17.0.2\javafx-media-17.0.2-win.jar"
)
foreach ($j in $depJars) {
    if (-not (Test-Path $j)) { throw "missing dependency: $j" }
}

# 1) prepare input dir (app jar + dependency jars; jpackage puts all jars on app classpath)
$inputDir = Join-Path $project "target\jpackage-input"
if (Test-Path $inputDir) { Remove-Item $inputDir -Recurse -Force }
New-Item -ItemType Directory -Path $inputDir | Out-Null
& $jar cf (Join-Path $inputDir "SkyLinkGCS.jar") -C (Join-Path $project "target\classes") .
foreach ($j in $depJars) { Copy-Item $j $inputDir }

# 2) package: try exe first (needs WiX), fall back to app-image dir
$dest = Join-Path $project "target\installer"
if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
New-Item -ItemType Directory -Path $dest | Out-Null

function Invoke-JPackage($type) {
    & $jpackage `
        --type $type `
        --name SkyLinkGCS `
        --app-version $ver `
        --vendor "SkyLink" `
        --description "SkyLink GCS ground station" `
        --input $inputDir `
        --main-jar SkyLinkGCS.jar `
        --main-class com.cherglow.gcs.Launcher `
        --dest $dest
    $script:LASTRC = $LASTEXITCODE
}

$exeOk = $false
try {
    Invoke-JPackage "exe"
    if ($script:LASTRC -eq 0) { $exeOk = $true }
} catch { Write-Output "exe packaging failed (WiX missing?): $_" }

if (-not $exeOk) {
    Write-Output "falling back to app-image..."
    Invoke-JPackage "app-image"
    if ($script:LASTRC -ne 0) { throw "app-image packaging failed (rc=$script:LASTRC)" }
}

Write-Output "done. artifacts:"
Get-ChildItem $dest | ForEach-Object { Write-Output ("  " + $_.FullName) }
