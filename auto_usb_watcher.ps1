# Khabar Auto USB Plug & Launch Watcher
$ErrorActionPreference = "Continue"

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "     KHABAR - AUTO USB PLUG & LAUNCH WATCHER" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "[*] Watcher is actively monitoring USB ports in background." -ForegroundColor Yellow
Write-Host "[*] Whenever you plug your phone into this PC via USB," -ForegroundColor Cyan
Write-Host "    it will AUTOMATICALLY install & launch Khabar on your phone!" -ForegroundColor Green
Write-Host "    (Press Ctrl+C anytime to stop)`n" -ForegroundColor Gray

# 1. Locate ADB
$adb = "adb"
if (-not (Get-Command "adb" -ErrorAction SilentlyContinue)) {
    $standardAdb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $standardAdb) {
        $adb = $standardAdb
    } else {
        Write-Host "[!] ADB was not found on your system." -ForegroundColor Red
        Read-Host "Press Enter to exit..."
        exit 1
    }
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$apk = Join-Path $scriptDir "app\build\outputs\apk\debug\app-debug.apk"

while ($true) {
    # Block until phone is plugged in
    & $adb wait-for-device
    $state = (& $adb get-state 2>$null).Trim()
    
    if ($state -eq "device") {
        $model = (& $adb shell getprop ro.product.model 2>$null).Trim()
        if (-not $model) { $model = "Android Device" }
        $timeStr = (Get-Date).ToString("HH:mm:ss")
        
        Write-Host "`n[$timeStr] PHONE DETECTED: $model" -ForegroundColor Green
        
        if (Test-Path $apk) {
            Write-Host "[*] Auto-installing Khabar APK..." -ForegroundColor Yellow
            $res = & $adb install -r -d $apk 2>&1
            Write-Host $res -ForegroundColor DarkGray
            
            Write-Host "[*] Auto-launching Khabar app..." -ForegroundColor Green
            & $adb shell am start -n "com.khabar.app/.ui.MainActivity" | Out-Null
            
            [console]::beep(800, 250)
            Write-Host "[+] Khabar successfully opened on your phone!" -ForegroundColor Green
        } else {
            Write-Host "[!] APK not found at $apk. Please compile it first." -ForegroundColor Red
        }
        
        Write-Host "[*] Phone is connected. Waiting until you disconnect it before re-arming..." -ForegroundColor Gray
        while ($true) {
            Start-Sleep -Seconds 3
            $checkState = (& $adb get-state 2>$null).Trim()
            if ($checkState -ne "device") {
                Write-Host "[*] Phone disconnected. Re-armed! Ready for next plug-in...`n" -ForegroundColor Cyan
                break
            }
        }
    }
    Start-Sleep -Seconds 2
}
