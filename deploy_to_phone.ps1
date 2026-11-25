# Khabar 1-Click Phone Deployer
$ErrorActionPreference = "Continue"

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "     KHABAR - 1-CLICK PHONE DEPLOYER" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Cyan

# 1. Locate ADB
$adb = "adb"
if (-not (Get-Command "adb" -ErrorAction SilentlyContinue)) {
    $standardAdb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $standardAdb) {
        $adb = $standardAdb
    } else {
        Write-Host "[!] ADB was not found on your system." -ForegroundColor Red
        Write-Host "Please ensure Android SDK platform-tools are installed."
        Read-Host "Press Enter to exit..."
        exit 1
    }
}

# 2. Check APK
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$apk = Join-Path $scriptDir "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $apk)) {
    Write-Host "[*] APK not found. Building fresh APK with Gradle..." -ForegroundColor Yellow
    Set-Location $scriptDir
    & ".\gradlew.bat" assembleDebug
    if (-not (Test-Path $apk)) {
        Write-Host "[X] Build failed! Please check Gradle errors." -ForegroundColor Red
        Read-Host "Press Enter to exit..."
        exit 1
    }
}

# 3. Detect Device
Write-Host "`n[*] Checking for connected Android phone..." -ForegroundColor Yellow
$state = & $adb get-state 2>$null
if ($LASTEXITCODE -ne 0 -or $state -notmatch "device") {
    Write-Host "[!] Phone not connected yet." -ForegroundColor DarkYellow
    Write-Host "[*] Connect your phone via USB (or turn on Wireless ADB)..." -ForegroundColor Cyan
    Write-Host "[*] Waiting for phone..." -ForegroundColor Gray
    & $adb wait-for-device
}

$model = (& $adb shell getprop ro.product.model 2>$null).Trim()
if (-not $model) { $model = "Android Device" }

Write-Host "`n[+] Device Connected: $model" -ForegroundColor Green
Write-Host "[*] Installing Khabar APK ($apk)..." -ForegroundColor Yellow

$installResult = & $adb install -r -d $apk 2>&1
Write-Host $installResult -ForegroundColor DarkGray

if ($LASTEXITCODE -ne 0) {
    Write-Host "[*] Retrying with grant permissions flag..." -ForegroundColor Yellow
    & $adb install -r -d -g $apk
}

Write-Host "`n[*] Launching Khabar on your phone..." -ForegroundColor Green
& $adb shell am start -n "com.khabar.app/.ui.MainActivity"

Write-Host "`n===================================================" -ForegroundColor Cyan
Write-Host "  SUCCESS: Khabar is now open on your phone! Enjoy!" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Cyan
Start-Sleep -Seconds 3
