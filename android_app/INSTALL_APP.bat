@echo off
echo.
echo ================================================================
echo             🍄 MushroomTech App Installation Helper
echo ================================================================
echo.

echo Checking system requirements...
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java is not installed or not in PATH
    echo.
    echo Please install Java JDK 11+ from: https://adoptium.net/
    echo After installation, restart this script.
    echo.
    pause
    exit /b 1
) else (
    echo ✅ Java is installed
)

REM Check if Android SDK is available
if not exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    echo ❌ Android SDK not found
    echo.
    echo RECOMMENDED: Install Android Studio from:
    echo https://developer.android.com/studio
    echo.
    echo This will automatically install Java, Android SDK, and provide
    echo an easy way to build and install your app.
    echo.
    pause
    exit /b 1
) else (
    echo ✅ Android SDK found
)

echo.
echo ================================================================
echo                    Building APK...
echo ================================================================
echo.

REM Build the APK
call gradlew.bat assembleDebug

if %errorlevel% neq 0 (
    echo.
    echo ❌ Build failed! 
    echo.
    echo Please check the error messages above.
    echo Consider using Android Studio for easier setup.
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================================
echo                  Build Successful! 
echo ================================================================
echo.

echo ✅ APK created successfully!
echo.
echo 📁 Location: app\build\outputs\apk\debug\app-debug.apk
echo.
echo 📱 To install on your phone:
echo    1. Transfer the APK file to your Android device
echo    2. Enable "Install from unknown sources" in Settings
echo    3. Tap the APK file to install
echo.
echo OR connect your phone via USB and run:
echo    adb install app\build\outputs\apk\debug\app-debug.apk
echo.

pause 