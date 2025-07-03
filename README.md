# Mushroom Tech - Complete IoT Environmental Control System

🍄 **Professional mushroom growing system with ESP32, Raspberry Pi, and Android app**

## 🚀 Features

### 📱 Android Mobile App
- **Firebase Cloud Integration** - Monitor and control from anywhere
- **Gemini AI Analysis** - Upload mushroom photos for AI identification and health analysis
- **Real-time Dashboard** - Circular meters showing temperature, humidity, and CO2 levels
- **Manual Controls** - Direct control of water pump, fans, lights, and heater
- **Dark Green Theme** - Professional UI with mushroom-themed design
- **Time-based Greetings** - Personalized messages based on time of day

### 🔧 Hardware Integration
- **ESP32 Water Monitor** - Dual soil moisture sensors with automatic pump control
- **Raspberry Pi Controller** - 4-channel relay control for 240V appliances
- **DHT22 Environmental Sensor** - Temperature and humidity monitoring
- **Automatic Climate Control** - Maintains optimal growing conditions (25-28°C, 60-80% humidity)

### ☁️ Cloud Architecture
- **Firebase Realtime Database** - Instant synchronization across all devices
- **Remote Monitoring** - Works with mobile data and WiFi hotspots
- **Data Logging** - Historical environmental data storage
- **Device Registration** - Automatic device discovery and management

## 📊 System Components

### 🔌 Relay Control (240V AC)
- **Relay 1:** Humidifier (automatic humidity control)
- **Relay 2:** 1HP Water Pump (ESP32 controlled)
- **Relay 3:** Aquarium Pump (circulation)
- **Relay 4:** Heater (automatic temperature control)

### 📡 Communication Flow
```
ESP32 → Firebase → Android App
  ↓       ↑         ↓
Raspberry Pi ← Firebase ← Manual Controls
```

## 🛠️ Installation

### 📱 Android App
1. Download APK from GitHub Releases
2. Enable "Unknown Sources" in Android settings
3. Install and configure Firebase connection

### 🔧 Hardware Setup
1. **ESP32:** Upload `esp32_firebase_water_monitor.ino`
2. **Raspberry Pi:** Run `raspberry_pi_firebase_controller.py`
3. **Firebase:** Configure using `firebase_setup_instructions.md`

## 🔑 Configuration

### Firebase Setup
- Project ID: `mushroomtech-b0164`
- Configure authentication and database rules
- Add device registration for remote access

### API Keys
- Gemini AI integration for mushroom photo analysis
- Firebase configuration for cloud connectivity

## 📈 Monitoring & Control

### Automatic Features
- **Temperature Control:** Heater activates below 25°C
- **Humidity Control:** Humidifier activates below 60%
- **Water Level Management:** Automatic pump control via ESP32
- **Data Logging:** Continuous environmental monitoring

### Manual Controls
- Water pump on/off
- Fan speed control
- Light scheduling
- Heater override

## 🎯 Perfect for Mushroom Cultivation

This system maintains optimal growing conditions for various mushroom species:
- **Oyster Mushrooms:** 25-28°C, 85-95% humidity
- **Shiitake:** 20-25°C, 80-90% humidity  
- **Button Mushrooms:** 15-20°C, 80-85% humidity

## 🔧 Technical Specifications

- **ESP32:** WiFi-enabled microcontroller with dual moisture sensors
- **Raspberry Pi:** Multi-threaded Python controller with Flask API
- **Android:** Firebase-integrated mobile app with AI capabilities
- **Database:** SQLite local storage + Firebase cloud sync
- **Communication:** HTTP/HTTPS with Firebase Realtime Database

## 📞 Support

For technical support or customization:
- Check GitHub Issues for common problems
- Review Firebase setup instructions
- Verify hardware connections and power supply

---

**Built with ❤️ for professional mushroom cultivation**

All your hardware integration, AI analysis, and cloud connectivity is ready to go!

<!-- APK Build Trigger - Updated to ensure GitHub Actions builds the Android APK -->
<!-- Build timestamp: 2025-01-03 -->