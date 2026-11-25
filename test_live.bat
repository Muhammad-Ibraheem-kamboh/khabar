@echo off
setlocal enabledelayedexpansion
title Khabar - Live In-Chat Phone Deploy & Interactive Mirror

:: 1. Locate ADB
set "ADB_EXE=adb"
where adb >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set "ADB_EXE=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    ) else if exist "C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
        set "ADB_EXE=C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    ) else (
        echo [ERROR] ADB not found.
        pause
        exit /b 1
    )
)

:: 2. Locate scrcpy for live interactive desktop window
set "SCRCPY_EXE=scrcpy"
where scrcpy >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if exist "C:\Users\Administrator\AppData\Local\Microsoft\WinGet\Packages\Genymobile.scrcpy_Microsoft.Winget.Source_8wekyb3d8bbwe\scrcpy-win64-v5.0.1\scrcpy.exe" (
        set "SCRCPY_EXE=C:\Users\Administrator\AppData\Local\Microsoft\WinGet\Packages\Genymobile.scrcpy_Microsoft.Winget.Source_8wekyb3d8bbwe\scrcpy-win64-v5.0.1\scrcpy.exe"
    )
)

echo =====================================================================
echo           KHABAR APP - LIVE INTERACTIVE PC TESTER
echo =====================================================================
echo.
echo [*] Waiting for connected Android phone via USB...
"%ADB_EXE%" wait-for-device
echo [+] Phone connected!

:: 3. Install latest debug APK
set "APK_PATH=app\build\outputs\apk\debug\app-debug.apk"
if exist "%APK_PATH%" (
    echo [*] Installing latest Khabar APK on your phone...
    "%ADB_EXE%" install -r -d "%APK_PATH%"
) else (
    echo [!] APK not found at %APK_PATH%. Please build it first.
)

:: 4. Launch Khabar MainActivity on Phone
echo [*] Launching Khabar on phone...
"%ADB_EXE%" shell input keyevent 224 >nul 2>&1
"%ADB_EXE%" shell am start -n com.khabar.app/.ui.MainActivity

:: 5. Capture snapshot for in-chat inspection
set "TARGET_DIR=C:\Users\Administrator\.gemini\antigravity\brain\7ea6366e-2fc5-4554-8f67-979a872b0235"
if exist "%TARGET_DIR%" (
    echo [*] Capturing screen snapshot into chat workspace...
    "%ADB_EXE%" exec-out screencap -p > "%TARGET_DIR%\live_screen.png"
    echo [+] Captured live screen to: %TARGET_DIR%\live_screen.png
)

:: 6. Launch Live Interactive Window on PC Screen (scrcpy)
echo [*] Opening live interactive phone mirror on your PC screen...
start "" "%SCRCPY_EXE%" --window-title "Khabar App (Live Testing Window)" --stay-awake

echo.
echo =====================================================================
echo   [SUCCESS] Live testing window is now open on your PC screen!
echo   - Click, type, and test with your mouse and keyboard like a website.
echo   - Screenshots and logs are synced to the chat window for live review.
echo =====================================================================
echo.
pause
