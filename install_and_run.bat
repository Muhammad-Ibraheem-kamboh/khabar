@echo off
setlocal enabledelayedexpansion

title Khabar - 1-Click Mobile Deploy & Launch

echo =====================================================================
echo                  KHABAR APP - 1-CLICK LAUNCHER
echo =====================================================================
echo.

:: 1. Locate ADB
set "ADB_EXE=adb"
where adb >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set "ADB_EXE=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    ) else if exist "C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
        set "ADB_EXE=C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    ) else (
        echo [ERROR] ADB not found! Please check your Android SDK installation.
        pause
        exit /b 1
    )
)

:: 2. Check if device is attached
echo [*] Checking for connected Android device...
for /f "tokens=1,2" %%i in ('"%ADB_EXE%" devices ^| findstr /v "List of devices attached" ^| findstr /r "[a-zA-Z0-9]"') do (
    if "%%j"=="device" (
        set "DEVICE_FOUND=%%i"
    )
)

if not defined DEVICE_FOUND (
    echo [!] No device detected yet.
    echo [!] Connect your Android phone with USB Debugging enabled, or enable Wireless ADB.
    echo [*] Waiting for device... (Connect your phone now)
    "%ADB_EXE%" wait-for-device
    echo [+] Phone connected!
) else (
    echo [+] Detected device: %DEVICE_FOUND%
)

:: 3. Check for APK
set "APK_PATH=app\build\outputs\apk\debug\app-debug.apk"
if not exist "%APK_PATH%" (
    echo [*] APK not found. Building fresh debug APK...
    call gradlew.bat assembleDebug
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Build failed! Check errors above.
        pause
        exit /b 1
    )
)

:: 4. Install APK onto connected device
echo [*] Installing latest Khabar APK on your phone...
"%ADB_EXE%" install -r -d "%APK_PATH%"
if %ERRORLEVEL% NEQ 0 (
    echo [!] Reinstalling with fallback flags...
    "%ADB_EXE%" install -r "%APK_PATH%"
)

:: 5. Launch Khabar MainActivity
echo [*] Launching Khabar on your phone...
"%ADB_EXE%" shell input keyevent 224 >nul 2>&1
"%ADB_EXE%" shell am start -n com.khabar.app/.ui.MainActivity

echo.
echo =====================================================================
echo   [SUCCESS] Khabar App is now installed and launched on your phone!
echo =====================================================================
echo.
pause
