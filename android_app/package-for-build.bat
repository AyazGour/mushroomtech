@echo off
echo.
echo ================================================================
echo          📦 Packaging MushroomTech for Online Build
echo ================================================================
echo.

echo Creating build package...

REM Create a clean directory for packaging
if exist "MushroomTech-BuildPackage" rmdir /s /q "MushroomTech-BuildPackage"
mkdir "MushroomTech-BuildPackage"

echo Copying essential files...

REM Copy all necessary files
xcopy "app" "MushroomTech-BuildPackage\app" /E /I /H /Y
copy "build.gradle" "MushroomTech-BuildPackage\"
copy "settings.gradle" "MushroomTech-BuildPackage\"
copy "gradle.properties" "MushroomTech-BuildPackage\"
copy "gradlew.bat" "MushroomTech-BuildPackage\"
xcopy ".github" "MushroomTech-BuildPackage\.github" /E /I /H /Y

REM Copy documentation
copy "BUILD_APK_ONLINE.md" "MushroomTech-BuildPackage\"
copy "INSTALLATION_GUIDE.md" "MushroomTech-BuildPackage\"

echo.
echo ✅ Package created successfully!
echo.
echo 📁 Location: MushroomTech-BuildPackage\
echo.
echo 🚀 Next steps:
echo    1. Zip the MushroomTech-BuildPackage folder
echo    2. Upload to GitHub or online build service
echo    3. Download your APK in 5-10 minutes!
echo.
echo 🌐 Recommended online build services:
echo    - GitHub Actions (upload to GitHub repo)
echo    - Appetize.io
echo    - BuildBot.com
echo.

pause 