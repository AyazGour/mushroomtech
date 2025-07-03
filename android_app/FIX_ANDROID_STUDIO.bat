@echo off
echo.
echo ================================================================
echo          🔧 Fixing Android Studio Gradle Issues
echo ================================================================
echo.

echo Creating Gradle directories with proper permissions...
mkdir "%USERPROFILE%\.gradle" 2>nul
mkdir "%USERPROFILE%\.gradle\wrapper" 2>nul
mkdir "%USERPROFILE%\.gradle\wrapper\dists" 2>nul
mkdir "%USERPROFILE%\.gradle\caches" 2>nul

echo Setting permissions...
icacls "%USERPROFILE%\.gradle" /grant "%USERNAME%:(OI)(CI)F" /T 2>nul

echo Cleaning local Gradle cache...
if exist ".gradle" rmdir /s /q ".gradle"
if exist "app\build" rmdir /s /q "app\build"

echo.
echo ✅ Gradle directories created and permissions set!
echo.
echo 🎯 Now try one of these options:
echo.
echo OPTION 1: Android Studio
echo   - Close Android Studio completely
echo   - Restart Android Studio as Administrator
echo   - Open your project
echo   - File → Invalidate Caches and Restart
echo   - Try building again
echo.
echo OPTION 2: Command Line Build
echo   - Run: gradlew.bat clean
echo   - Run: gradlew.bat assembleDebug
echo.
echo OPTION 3: GitHub Actions (Recommended)
echo   - Upload project to GitHub
echo   - Automatic build without local issues
echo   - Download APK in 10 minutes
echo.

pause 