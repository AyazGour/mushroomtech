# 🍄 MushroomTech Complete System Setup Guide

## 📋 **System Overview**

The MushroomTech system is a complete IoT solution for mushroom cultivation monitoring and control, consisting of:

- **ESP32 Water Monitor** - Automated water level sensing and pump control
- **Raspberry Pi Controller** - Environmental monitoring and relay control  
- **Android Mobile App** - Remote monitoring and AI analysis
- **Firebase Cloud** - Real-time data synchronization and remote access

---

## 🏗️ **System Architecture**

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   ESP32 + DHT   │    │  Raspberry Pi 4  │    │  Android Phone  │
│   Water Monitor │◄──►│  + 4-Relay Board │◄──►│   Mobile App    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                        │                        │
         │                        │                        │
         └────────────────────────┼────────────────────────┘
                                  │
                        ┌─────────▼─────────┐
                        │  Firebase Cloud   │
                        │  Realtime Database│
                        └───────────────────┘
```

---

## 🛠️ **Hardware Setup**

### **ESP32 Water Monitor**
- **Components:**
  - ESP32 DevKit
  - 2x Soil moisture sensors (water level detection)
  - Jumper wires
  - Breadboard

- **Connections:**
  - Sensor 1 (Bottom): GPIO 34
  - Sensor 2 (Top): GPIO 35
  - Power: 3.3V and GND

### **Raspberry Pi Environmental Controller**
- **Components:**
  - Raspberry Pi 4
  - 4-Channel Relay Module
  - DHT22 Temperature/Humidity Sensor
  - 240V AC appliances (heater, humidifier, pumps)

- **Connections:**
  - DHT22: GPIO 4
  - Relay 1 (Heater): GPIO 18
  - Relay 2 (Water Pump): GPIO 19  
  - Relay 3 (Aquarium Pump): GPIO 20
  - Relay 4 (Humidifier): GPIO 21

---

## 📱 **Android App Installation**

### **Quick Install (Recommended)**

1. **Install Android Studio:**
   ```
   Download: https://developer.android.com/studio
   ```

2. **Open Project:**
   - Launch Android Studio
   - Open `android_app` folder
   - Wait for Gradle sync

3. **Connect Phone & Install:**
   - Enable USB Debugging on phone
   - Connect via USB
   - Click Run button (▶️)

### **Alternative: Manual Build**

```powershell
# Navigate to android_app folder
cd android_app

# Run installation script
.\Install-MushroomTechApp.ps1
```

---

## 🔥 **Firebase Configuration**

### **Project Details:**
- **Project ID:** `mushroomtech-b0164`
- **Database URL:** `https://mushroomtech-b0164-default-rtdb.firebaseio.com/`
- **Package Name:** `com.mushroomtech.app`

### **Setup Steps:**

1. **Firebase Console:**
   - Project already created: `mushroomtech-b0164`
   - Realtime Database enabled
   - Authentication configured

2. **Configuration Files:**
   - ✅ `google-services.json` - Already placed in `android_app/app/`
   - ✅ Service account key - Required for Raspberry Pi

3. **Database Structure:**
   ```json
   {
     "devices": {
       "device_id": {
         "device_type": "raspberry_pi",
         "location": "mushroom_farm",
         "last_seen": "timestamp"
       }
     },
     "device_status": {
       "device_id": {
         "sensor_data": {
           "temperature": 26.5,
           "humidity": 75.2
         },
         "relay_states": {
           "heater": false,
           "humidifier": true,
           "aquarium_pump": false
         }
       }
     }
   }
   ```

---

## 🔧 **Software Installation**

### **ESP32 Setup**

1. **Arduino IDE Configuration:**
   ```
   - Install ESP32 board package
   - Install Firebase ESP32 Client library
   - Install DHT sensor library
   ```

2. **Upload Code:**
   ```cpp
   // File: esp32_firebase_water_monitor.ino
   // Configure WiFi credentials
   // Configure Firebase settings
   // Upload to ESP32
   ```

### **Raspberry Pi Setup**

1. **Install Dependencies:**
   ```bash
   # Run setup script
   chmod +x setup_raspberry_pi.sh
   ./setup_raspberry_pi.sh
   
   # Or install manually
   pip install -r requirements_firebase.txt
   ```

2. **Run Controller:**
   ```bash
   python raspberry_pi_firebase_controller.py
   ```

### **Android App Setup**

1. **Using Android Studio:**
   - Open `android_app` project
   - Build and run on device

2. **Using Scripts:**
   ```powershell
   # Windows PowerShell
   .\Install-MushroomTechApp.ps1
   
   # Or batch file
   INSTALL_APP.bat
   ```

---

## 🚀 **Quick Start Guide**

### **Step 1: Hardware Setup**
1. Connect ESP32 with water sensors
2. Set up Raspberry Pi with relays and DHT22
3. Connect appliances to relay outputs

### **Step 2: Software Configuration**
1. Flash ESP32 with `esp32_firebase_water_monitor.ino`
2. Run `raspberry_pi_firebase_controller.py` on Pi
3. Install Android app on phone

### **Step 3: Firebase Connection**
1. Both ESP32 and Raspberry Pi connect to WiFi
2. Devices register with Firebase automatically
3. Android app connects and shows real-time data

### **Step 4: System Operation**
1. **Automatic Control:**
   - Temperature: 25-28°C (heater control)
   - Humidity: 60-80% (humidifier control)
   - Water: Auto-fill when low (pump control)

2. **Manual Control:**
   - Use Android app to override automatic settings
   - Real-time status monitoring
   - AI-powered mushroom analysis

---

## 🎯 **Key Features**

### **Environmental Monitoring**
- ✅ Real-time temperature and humidity tracking
- ✅ Automatic climate control
- ✅ Water level monitoring and auto-fill
- ✅ ESP32 connectivity status

### **Mobile App Features**
- ✅ Live dashboard with circular meters
- ✅ Manual device control
- ✅ AI mushroom photo analysis (Gemini)
- ✅ System status notifications
- ✅ Firebase cloud synchronization

### **Cloud Integration**
- ✅ Remote monitoring from anywhere
- ✅ Real-time data synchronization
- ✅ Command execution via mobile app
- ✅ Historical data logging

### **AI Analysis**
- ✅ Gemini AI integration
- ✅ Mushroom growth stage assessment
- ✅ Health condition evaluation
- ✅ Care recommendations
- ✅ Problem identification

---

## 📊 **System Specifications**

### **Environmental Ranges**
- **Temperature:** 25-28°C (optimal for mushroom growth)
- **Humidity:** 60-80% (optimal for mushroom growth)
- **Water Level:** Auto-maintained in 20L container
- **CO2:** Monitoring (400-500 ppm optimal)

### **Hardware Requirements**
- **ESP32:** DevKit with WiFi capability
- **Raspberry Pi:** Model 4 recommended (2GB+ RAM)
- **Relays:** 4-channel, 240V AC rated
- **Sensors:** DHT22, soil moisture sensors

### **Software Requirements**
- **Android:** 7.0+ (API level 24+)
- **Python:** 3.7+ for Raspberry Pi
- **Arduino IDE:** For ESP32 programming
- **Firebase:** Realtime Database

---

## 🔍 **Troubleshooting**

### **Connection Issues**
- Check WiFi credentials in ESP32/Pi code
- Verify Firebase project configuration
- Ensure internet connectivity

### **App Issues**
- Grant camera and storage permissions
- Check Firebase configuration
- Verify package name matches

### **Hardware Issues**
- Check sensor connections
- Verify relay wiring
- Test individual components

---

## 📞 **Support**

### **Configuration Files**
- `firebase_setup_instructions.md` - Detailed Firebase setup
- `INSTALLATION_GUIDE.md` - Android app installation
- `README.md` - Project overview

### **Code Files**
- `esp32_firebase_water_monitor.ino` - ESP32 firmware
- `raspberry_pi_firebase_controller.py` - Pi controller
- `android_app/` - Complete Android application

Your complete MushroomTech system is ready for deployment! 🍄✨

---

## 🎉 **Expected Results**

Once fully deployed, you'll have:
- 📱 **Remote monitoring** from anywhere with internet
- 🤖 **AI-powered analysis** of your mushroom photos  
- ⚡ **Automatic environmental control** for optimal growing conditions
- 📊 **Real-time data** on temperature, humidity, and water levels
- 🔧 **Manual override** capabilities via mobile app
- ☁️ **Cloud backup** of all environmental data

Perfect for professional mushroom cultivation or advanced hobbyist setups! 