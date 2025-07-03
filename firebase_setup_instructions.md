# Firebase Setup Instructions for Environmental Control System

This guide will help you set up Firebase for remote monitoring and control of your mushroom growing system.

## 🔥 Firebase Project Setup

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter project name: `mushroom-environmental-control`
4. Enable Google Analytics (optional)
5. Click "Create project"

### Step 2: Enable Required Services

In your Firebase project, enable these services:

#### Realtime Database
1. Go to "Realtime Database" in the left sidebar
2. Click "Create Database"
3. Choose your location (closest to your devices)
4. Start in **test mode** for development
5. Note the database URL: `https://your-project-id-default-rtdb.firebaseio.com/`

#### Authentication (Optional)
1. Go to "Authentication" → "Sign-in method"
2. Enable "Anonymous" for now
3. Later you can add email/password or Google sign-in

#### Storage (For image uploads)
1. Go to "Storage" 
2. Click "Get started"
3. Start in test mode

## 📱 Android App Configuration

### Step 1: Add Android App to Firebase

1. In Firebase Console, click "Add app" → Android
2. Enter package name: `com.environmentalcontrol`
3. Download `google-services.json`
4. Place it in `android_app/app/` directory

### Step 2: Update Build Files

Add to `android_app/build.gradle` (project level):
```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.0'
    }
}
```

The app-level `build.gradle` is already configured with Firebase dependencies.

### Step 3: Initialize Firebase in App

The app is already configured to initialize Firebase. Just ensure `google-services.json` is in place.

## 🥧 Raspberry Pi Configuration

### Step 1: Install Firebase Admin SDK

```bash
pip install firebase-admin
```

### Step 2: Create Service Account

1. In Firebase Console, go to "Project settings" → "Service accounts"
2. Click "Generate new private key"
3. Download the JSON file
4. Rename it to `firebase-service-account.json`
5. Place it in your Raspberry Pi project directory

### Step 3: Update Raspberry Pi Code

Replace the Firebase configuration in `raspberry_pi_firebase_controller.py`:

```python
# Load service account key
import json
with open('firebase-service-account.json', 'r') as f:
    FIREBASE_CONFIG = json.load(f)

DATABASE_URL = "https://your-project-id-default-rtdb.firebaseio.com/"
```

## 📟 ESP32 Configuration

### Step 1: Install Firebase ESP32 Library

In Arduino IDE:
1. Go to "Tools" → "Manage Libraries"
2. Search for "Firebase ESP32 Client"
3. Install the library by Mobizt

### Step 2: Get Database Secret (Legacy Token)

1. In Firebase Console, go to "Project settings" → "Service accounts"
2. Go to "Database secrets" tab
3. Click "Add secret" if none exists
4. Copy the secret token

### Step 3: Update ESP32 Code

In `esp32_firebase_water_monitor.ino`, update:

```cpp
#define FIREBASE_HOST "your-project-id-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH "your-database-secret-token"

// WiFi credentials for your hotspot
const char* ssid = "YOUR_HOTSPOT_SSID";
const char* password = "YOUR_HOTSPOT_PASSWORD";
```

## 🔒 Firebase Security Rules

### Realtime Database Rules

Go to "Realtime Database" → "Rules" and update:

```json
{
  "rules": {
    "devices": {
      ".read": true,
      ".write": true
    },
    "device_status": {
      ".read": true,
      ".write": true
    },
    "commands": {
      ".read": true,
      ".write": true
    },
    "command_responses": {
      ".read": true,
      ".write": true
    },
    "esp32_commands": {
      ".read": true,
      ".write": true
    },
    "esp32_responses": {
      ".read": true,
      ".write": true
    },
    "historical_data": {
      ".read": true,
      ".write": true
    }
  }
}
```

**Note**: These are permissive rules for development. In production, implement proper authentication and authorization.

### Storage Rules

Go to "Storage" → "Rules":

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if true;
    }
  }
}
```

## 🌐 Network Setup

### Hotspot Configuration

1. **Create a WiFi Hotspot** (using phone or dedicated router)
   - SSID: `MushroomFarm_Control`
   - Password: `SecurePassword123`
   - Ensure internet access

2. **Connect Devices to Hotspot**:
   - ESP32: Connects to hotspot
   - Raspberry Pi: Connects to hotspot
   - Android Phone: Uses mobile data or different WiFi

### Internet Requirements

- **ESP32**: Needs internet to reach Firebase
- **Raspberry Pi**: Needs internet to reach Firebase
- **Android Phone**: Needs internet to reach Firebase

## 📊 Firebase Database Structure

Your Firebase Realtime Database will have this structure:

```
mushroom-environmental-control/
├── devices/
│   ├── rpi_12345678/
│   │   ├── device_type: "raspberry_pi"
│   │   ├── status: "online"
│   │   ├── capabilities: {...}
│   │   └── last_seen: "2024-01-01T10:00:00Z"
│   └── esp32_abcdef12/
│       ├── device_type: "esp32_water_monitor"
│       ├── status: "online"
│       └── sensor_config: {...}
├── device_status/
│   └── rpi_12345678/
│       ├── sensor_data: {...}
│       ├── relay_status: {...}
│       └── timestamp: "2024-01-01T10:00:00Z"
├── commands/
│   └── rpi_12345678/
│       ├── relay_control: {...}
│       └── thresholds: {...}
├── esp32_commands/
│   └── esp32_abcdef12/
│       ├── pump_command: "ON"
│       └── water_state: "WATER_LOW"
└── historical_data/
    └── rpi_12345678/
        └── 2024-01-01T10:00:00Z: {...}
```

## 🚀 Testing the Setup

### 1. Test ESP32 Connection

1. Upload the Firebase code to ESP32
2. Open Serial Monitor
3. Look for "Firebase connected successfully!"
4. Check Firebase Console → Realtime Database for device registration

### 2. Test Raspberry Pi Connection

1. Run the Firebase controller: `python3 raspberry_pi_firebase_controller.py`
2. Check logs for "Firebase initialized successfully"
3. Verify device appears in Firebase Console

### 3. Test Android App

1. Install the app on your phone
2. Open the app
3. Check splash screen for "Connected to Firebase!"
4. Verify real-time data updates

## 🔧 Troubleshooting

### Common Issues

1. **Firebase Connection Failed**
   - Check internet connectivity
   - Verify Firebase project configuration
   - Ensure service account key is correct

2. **ESP32 Not Connecting**
   - Verify WiFi credentials
   - Check Firebase host and auth token
   - Ensure Firebase ESP32 library is installed

3. **Android App Crashes**
   - Ensure `google-services.json` is in correct location
   - Check Firebase dependencies in build.gradle
   - Verify package name matches Firebase configuration

4. **No Real-time Updates**
   - Check Firebase Database rules
   - Verify all devices are online
   - Check network connectivity

### Debug Commands

**Check Firebase connectivity:**
```bash
# On Raspberry Pi
ping firebase.google.com

# Check Python Firebase connection
python3 -c "import firebase_admin; print('Firebase Admin SDK installed')"
```

**Monitor Firebase Database:**
- Use Firebase Console → Realtime Database
- Watch for real-time updates
- Check device status timestamps

## 🔐 Security Recommendations

### For Production Use:

1. **Enable Authentication**
   - Implement user sign-in
   - Use Firebase Auth tokens

2. **Secure Database Rules**
   - Restrict read/write access
   - Implement user-based permissions

3. **Use Environment Variables**
   - Store API keys securely
   - Don't commit secrets to version control

4. **Network Security**
   - Use WPA3 for WiFi hotspot
   - Consider VPN for remote access

## 📞 Support

If you encounter issues:

1. Check Firebase Console for error messages
2. Review device logs (Serial Monitor, system logs)
3. Verify all configuration files are correct
4. Test internet connectivity on all devices

The system is now configured for remote monitoring and control through Firebase, allowing you to manage your mushroom growing environment from anywhere with internet access! 