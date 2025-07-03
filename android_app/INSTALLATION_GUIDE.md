# 🍄 MushroomTech App Installation Guide

## 📱 **Option 1: Easy Installation via Android Studio (Recommended)**

### **Step 1: Install Android Studio**
1. Download Android Studio from: https://developer.android.com/studio
2. Install with default settings (includes Java SDK)
3. Launch Android Studio and complete the setup wizard

### **Step 2: Open the Project**
1. Open Android Studio
2. Click "Open an existing project"
3. Navigate to the `android_app` folder and select it
4. Wait for Gradle sync to complete

### **Step 3: Connect Your Phone**
1. **Enable Developer Options on your Android phone:**
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times until you see "You are now a developer"
   - Go back to Settings → Developer Options
   - Enable "USB Debugging"

2. **Connect your phone:**
   - Connect via USB cable
   - Allow USB debugging when prompted on your phone

### **Step 4: Build and Install**
1. In Android Studio, click the green "Run" button (▶️)
2. Select your connected device
3. The app will build and install automatically

---

## 🔧 **Option 2: Manual APK Build (Advanced)**

### **Prerequisites:**
- Java Development Kit (JDK) 11 or higher
- Android SDK
- Command line tools

### **Step 1: Install Java**
1. Download JDK from: https://adoptium.net/
2. Install and set JAVA_HOME environment variable
3. Verify: `java -version` in command prompt

### **Step 2: Install Android SDK**
1. Download command line tools from: https://developer.android.com/studio#command-tools
2. Extract and set ANDROID_HOME environment variable
3. Install required packages:
   ```bash
   sdkmanager "platforms;android-34"
   sdkmanager "build-tools;34.0.0"
   ```

### **Step 3: Build APK**
1. Open command prompt in the `android_app` folder
2. Run: `gradlew.bat assembleDebug`
3. APK will be generated in: `app/build/outputs/apk/debug/`

### **Step 4: Install APK**
1. Transfer the APK file to your Android phone
2. Enable "Install from unknown sources" in phone settings
3. Tap the APK file to install

---

## 📋 **App Features**

### **🏠 Main Dashboard**
- **Real-time Environmental Monitoring:**
  - Temperature gauge (25-28°C optimal)
  - Humidity gauge (60-80% optimal)
  - CO2 level monitoring (400-500 ppm optimal)

- **Manual Controls:**
  - Water pump control
  - Fan/ventilation control
  - Heating/lighting control

- **System Status:**
  - Live connection to Raspberry Pi
  - ESP32 water monitor status
  - Active device indicators

### **🤖 AI Analysis**
- **Mushroom Photo Analysis:**
  - Growth stage assessment
  - Health condition evaluation
  - Environmental recommendations
  - Problem identification
  - Care suggestions

- **Image Sources:**
  - Camera capture
  - Gallery selection
  - Real-time processing

### **☁️ Firebase Integration**
- **Cloud Connectivity:**
  - Real-time data synchronization
  - Remote monitoring capabilities
  - Command execution
  - Data logging

---

## 🔑 **Configuration Required**

### **Firebase Setup:**
1. Your Firebase project: `mushroomtech-b0164`
2. Google services file: Already configured
3. Real-time database: Ready for use

### **Gemini AI:**
- API key: Already configured
- Image analysis: Ready for use
- Mushroom expertise: Enabled

### **Hardware Integration:**
- **Raspberry Pi:** Must run `raspberry_pi_firebase_controller.py`
- **ESP32:** Must run `esp32_firebase_water_monitor.ino`
- **Firebase:** Both devices must connect to your Firebase project

---

## 🚀 **Quick Start**

1. **Install the app** using Option 1 (Android Studio)
2. **Launch the app** - you'll see the splash screen with greeting
3. **Check connectivity** - app will connect to Firebase automatically
4. **Monitor your system** - view real-time environmental data
5. **Control devices** - use manual controls as needed
6. **Analyze photos** - tap the AI button to analyze mushroom photos

---

## 📞 **Troubleshooting**

### **Connection Issues:**
- Check internet connection
- Verify Firebase project configuration
- Ensure Raspberry Pi is running and connected

### **Permission Issues:**
- Grant camera permission for AI analysis
- Grant storage permission for photo access
- Enable location if required

### **Build Issues:**
- Ensure Java is properly installed
- Check Android SDK configuration
- Clear Gradle cache: `gradlew clean`

---

## 🎯 **System Requirements**

### **Android Device:**
- Android 7.0+ (API level 24+)
- 2GB RAM minimum
- Camera for AI analysis
- Internet connection for Firebase

### **Development Environment:**
- Windows 10/11
- Java JDK 11+
- Android Studio 2022.3.1+
- 8GB RAM recommended

---

## 📊 **Expected Performance**

- **Real-time updates:** Every 5-10 seconds
- **AI analysis:** 10-30 seconds per image
- **Firebase sync:** Near-instantaneous
- **Battery usage:** Optimized for continuous monitoring

Your MushroomTech app is ready to help you monitor and optimize your mushroom growing environment! 🍄✨ 