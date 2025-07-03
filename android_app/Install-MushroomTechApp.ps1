# MushroomTech App Installation Script
Write-Host ""
Write-Host "================================================================" -ForegroundColor Green
Write-Host "            MushroomTech App Installation Helper" -ForegroundColor Green  
Write-Host "================================================================" -ForegroundColor Green
Write-Host ""

Write-Host "Checking system requirements..." -ForegroundColor Yellow
Write-Host ""

# Check Java installation
try {
    $javaVersion = java -version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Java is installed" -ForegroundColor Green
        Write-Host "   Version: $($javaVersion[0])" -ForegroundColor Gray
    } else {
        throw "Java not found"
    }
} catch {
    Write-Host "Java is not installed or not in PATH" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install Java JDK 11+ from: https://adoptium.net/" -ForegroundColor Yellow
    Write-Host "After installation, restart this script." -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

# Check Android SDK
$androidHome = $env:ANDROID_HOME
if (-not $androidHome -or -not (Test-Path "$androidHome\platform-tools\adb.exe")) {
    Write-Host "Android SDK not found" -ForegroundColor Red
    Write-Host ""
    Write-Host "RECOMMENDED: Install Android Studio from:" -ForegroundColor Yellow
    Write-Host "https://developer.android.com/studio" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "This will automatically install Java, Android SDK, and provide" -ForegroundColor Yellow
    Write-Host "an easy way to build and install your app." -ForegroundColor Yellow
    Write-Host ""
    
    $choice = Read-Host "Do you want to continue anyway? (y/N)"
    if ($choice -ne "y" -and $choice -ne "Y") {
        exit 1
    }
} else {
    Write-Host "Android SDK found" -ForegroundColor Green
    Write-Host "   Location: $androidHome" -ForegroundColor Gray
}

Write-Host ""
Write-Host "================================================================" -ForegroundColor Green
Write-Host "                    Building APK..." -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
Write-Host ""

# Build the APK
try {
    & .\gradlew.bat assembleDebug
    
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed"
    }
    
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Green
    Write-Host "                  Build Successful!" -ForegroundColor Green
    Write-Host "================================================================" -ForegroundColor Green
    Write-Host ""
    
    Write-Host "APK created successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Location: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "To install on your phone:" -ForegroundColor Yellow
    Write-Host "   1. Transfer the APK file to your Android device" -ForegroundColor White
    Write-Host "   2. Enable Install from unknown sources in Settings" -ForegroundColor White
    Write-Host "   3. Tap the APK file to install" -ForegroundColor White
    Write-Host ""
    Write-Host "OR connect your phone via USB and run:" -ForegroundColor Yellow
    Write-Host "   adb install app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
    Write-Host ""
    
} catch {
    Write-Host ""
    Write-Host "Build failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please check the error messages above." -ForegroundColor Yellow
    Write-Host "Consider using Android Studio for easier setup." -ForegroundColor Yellow
    Write-Host ""
}

Read-Host "Press Enter to exit" 