#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <FirebaseESP32.h>

// WiFi credentials - Update these with your hotspot details
const char* ssid = "YOUR_HOTSPOT_SSID";
const char* password = "YOUR_HOTSPOT_PASSWORD";

// Firebase configuration
#define FIREBASE_HOST "your-project-id-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH "your-firebase-database-secret-or-token"

// Firebase objects
FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;

// Pin definitions
const int MIN_SENSOR_PIN = 34;  // Bottom sensor (minimum water level)
const int MAX_SENSOR_PIN = 35;  // Top sensor (maximum water level)
const int LED_PIN = 2;          // Built-in LED for status

// Sensor thresholds - Adjust based on your specific sensors
const int DRY_THRESHOLD = 2000;    // Value when sensor is dry (no water)
const int WET_THRESHOLD = 1000;    // Value when sensor detects water

// Device ID for this ESP32
String DEVICE_ID = "esp32_" + String(ESP.getEfuseMac(), HEX);

// System states
enum WaterState {
  WATER_LOW,      // Need to start pump
  WATER_FILLING,  // Pump is running
  WATER_FULL,     // Need to stop pump
  WATER_ERROR     // Error condition
};

WaterState currentState = WATER_LOW;
WaterState previousState = WATER_ERROR;

// Timing variables
unsigned long lastSensorRead = 0;
unsigned long lastFirebaseUpdate = 0;
unsigned long lastHeartbeat = 0;
const unsigned long SENSOR_INTERVAL = 1000;        // Read sensors every 1 second
const unsigned long FIREBASE_UPDATE_INTERVAL = 5000;   // Update Firebase every 5 seconds
const unsigned long HEARTBEAT_INTERVAL = 30000;    // Heartbeat every 30 seconds
const unsigned long DEBOUNCE_DELAY = 3000;         // 3 second debounce

// Debounce variables
unsigned long stateChangeTime = 0;
bool stateChangeDetected = false;

// Firebase paths
String deviceStatusPath = "/device_status/" + DEVICE_ID;
String esp32CommandsPath = "/esp32_commands/" + DEVICE_ID;
String esp32ResponsesPath = "/esp32_responses/" + DEVICE_ID;
String devicesPath = "/devices/" + DEVICE_ID;

void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("ESP32 Firebase Water Level Monitor Starting...");
  
  // Initialize pins
  pinMode(LED_PIN, OUTPUT);
  pinMode(MIN_SENSOR_PIN, INPUT);
  pinMode(MAX_SENSOR_PIN, INPUT);
  
  // Connect to WiFi
  connectToWiFi();
  
  // Initialize Firebase
  initializeFirebase();
  
  // Register device
  registerDevice();
  
  Serial.println("System ready for monitoring!");
  Serial.println("Sensor Configuration:");
  Serial.println("- Pin 34: Minimum water level (bottom sensor)");
  Serial.println("- Pin 35: Maximum water level (top sensor)");
  Serial.println("- Wet threshold: < " + String(WET_THRESHOLD));
  Serial.println("- Dry threshold: > " + String(DRY_THRESHOLD));
  Serial.println("- Device ID: " + DEVICE_ID);
}

void loop() {
  unsigned long currentTime = millis();
  
  // Check WiFi connection
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi disconnected. Reconnecting...");
    connectToWiFi();
  }
  
  // Check Firebase connection
  if (!Firebase.ready()) {
    Serial.println("Firebase not ready. Reinitializing...");
    initializeFirebase();
  }
  
  // Read sensors at regular intervals
  if (currentTime - lastSensorRead >= SENSOR_INTERVAL) {
    readSensorsAndUpdateState();
    lastSensorRead = currentTime;
  }
  
  // Update Firebase at regular intervals
  if (currentTime - lastFirebaseUpdate >= FIREBASE_UPDATE_INTERVAL) {
    updateFirebaseStatus();
    lastFirebaseUpdate = currentTime;
  }
  
  // Send heartbeat
  if (currentTime - lastHeartbeat >= HEARTBEAT_INTERVAL) {
    sendHeartbeat();
    lastHeartbeat = currentTime;
  }
  
  // Handle state changes with debouncing
  handleStateChanges(currentTime);
  
  delay(100);
}

void connectToWiFi() {
  WiFi.begin(ssid, password);
  Serial.print("Connecting to WiFi");
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    digitalWrite(LED_PIN, !digitalRead(LED_PIN)); // Blink LED
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println();
    Serial.println("WiFi connected!");
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());
    digitalWrite(LED_PIN, HIGH); // Solid LED when connected
  } else {
    Serial.println();
    Serial.println("WiFi connection failed!");
    digitalWrite(LED_PIN, LOW);
  }
}

void initializeFirebase() {
  // Configure Firebase
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  
  // Initialize Firebase
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  
  // Set timeout
  firebaseData.setResponseSize(4096);
  
  if (Firebase.ready()) {
    Serial.println("Firebase connected successfully!");
    blinkLED(3); // Success indication
  } else {
    Serial.println("Firebase connection failed!");
    blinkLED(10); // Error indication
  }
}

void registerDevice() {
  if (!Firebase.ready()) return;
  
  // Create device registration data
  FirebaseJson deviceInfo;
  deviceInfo.set("device_id", DEVICE_ID);
  deviceInfo.set("device_type", "esp32_water_monitor");
  deviceInfo.set("status", "online");
  deviceInfo.set("last_seen", getTimestamp());
  
  FirebaseJson capabilities;
  capabilities.set("water_level_monitoring", true);
  capabilities.set("dual_sensor_setup", true);
  capabilities.set("pump_control", true);
  deviceInfo.set("capabilities", capabilities);
  
  FirebaseJson sensorConfig;
  sensorConfig.set("min_sensor_pin", MIN_SENSOR_PIN);
  sensorConfig.set("max_sensor_pin", MAX_SENSOR_PIN);
  sensorConfig.set("wet_threshold", WET_THRESHOLD);
  sensorConfig.set("dry_threshold", DRY_THRESHOLD);
  deviceInfo.set("sensor_config", sensorConfig);
  
  // Register device
  if (Firebase.setJSON(firebaseData, devicesPath, deviceInfo)) {
    Serial.println("Device registered successfully in Firebase");
  } else {
    Serial.println("Failed to register device: " + firebaseData.errorReason());
  }
}

void readSensorsAndUpdateState() {
  // Read sensor values
  int minSensorValue = analogRead(MIN_SENSOR_PIN);
  int maxSensorValue = analogRead(MAX_SENSOR_PIN);
  
  // Determine water presence at each level
  bool waterAtMin = (minSensorValue < WET_THRESHOLD);
  bool waterAtMax = (maxSensorValue < WET_THRESHOLD);
  
  // Debug output
  Serial.print("Sensors - Min: ");
  Serial.print(minSensorValue);
  Serial.print(waterAtMin ? " (WET)" : " (DRY)");
  Serial.print(" | Max: ");
  Serial.print(maxSensorValue);
  Serial.print(waterAtMax ? " (WET)" : " (DRY)");
  Serial.print(" | State: ");
  Serial.println(stateToString(currentState));
  
  // Determine new state
  WaterState newState = determineWaterState(waterAtMin, waterAtMax);
  
  // Check for state change
  if (newState != currentState) {
    if (!stateChangeDetected) {
      stateChangeDetected = true;
      stateChangeTime = millis();
      Serial.print(">>> State change detected: ");
      Serial.print(stateToString(currentState));
      Serial.print(" -> ");
      Serial.println(stateToString(newState));
    }
  } else {
    stateChangeDetected = false;
  }
}

WaterState determineWaterState(bool waterAtMin, bool waterAtMax) {
  if (!waterAtMin && !waterAtMax) {
    return WATER_LOW;           // No water detected - need to fill
  } else if (waterAtMin && !waterAtMax) {
    return WATER_FILLING;       // Water at minimum but not maximum - filling
  } else if (waterAtMin && waterAtMax) {
    return WATER_FULL;          // Water at both levels - tank full
  } else {
    return WATER_ERROR;         // Water at max but not min - sensor error
  }
}

void handleStateChanges(unsigned long currentTime) {
  if (stateChangeDetected && (currentTime - stateChangeTime >= DEBOUNCE_DELAY)) {
    // State change confirmed after debounce period
    previousState = currentState;
    
    // Re-read sensors to confirm state
    int minSensorValue = analogRead(MIN_SENSOR_PIN);
    int maxSensorValue = analogRead(MAX_SENSOR_PIN);
    bool waterAtMin = (minSensorValue < WET_THRESHOLD);
    bool waterAtMax = (maxSensorValue < WET_THRESHOLD);
    
    currentState = determineWaterState(waterAtMin, waterAtMax);
    
    Serial.println("*** STATE CHANGE CONFIRMED ***");
    Serial.print("Previous: ");
    Serial.print(stateToString(previousState));
    Serial.print(" -> Current: ");
    Serial.println(stateToString(currentState));
    
    // Send pump command for critical state changes
    if (currentState == WATER_LOW || currentState == WATER_FULL) {
      sendPumpCommandToFirebase();
    }
    
    // Visual indication of state change
    blinkLED(5);
    
    stateChangeDetected = false;
  }
}

void sendPumpCommandToFirebase() {
  if (!Firebase.ready()) {
    Serial.println("Cannot send pump command - Firebase not ready");
    return;
  }
  
  // Create command data
  FirebaseJson commandData;
  commandData.set("device_id", DEVICE_ID);
  commandData.set("timestamp", getTimestamp());
  commandData.set("water_state", stateToString(currentState));
  commandData.set("min_sensor", analogRead(MIN_SENSOR_PIN));
  commandData.set("max_sensor", analogRead(MAX_SENSOR_PIN));
  
  String pumpCommand = "NONE";
  String message = "No pump action required";
  
  if (currentState == WATER_LOW) {
    pumpCommand = "ON";
    message = "Water level LOW - START PUMP";
  } else if (currentState == WATER_FULL) {
    pumpCommand = "OFF";
    message = "Water level FULL - STOP PUMP";
  }
  
  commandData.set("pump_command", pumpCommand);
  commandData.set("message", message);
  
  Serial.println("Sending pump command to Firebase:");
  Serial.println("Command: " + pumpCommand);
  Serial.println("Message: " + message);
  
  // Send command to Firebase
  if (Firebase.setJSON(firebaseData, esp32CommandsPath, commandData)) {
    Serial.println("Pump command sent successfully to Firebase");
    blinkLED(3); // Success indication
  } else {
    Serial.println("Failed to send pump command: " + firebaseData.errorReason());
    blinkLED(10); // Error indication
  }
}

void updateFirebaseStatus() {
  if (!Firebase.ready()) return;
  
  // Create status data
  FirebaseJson statusData;
  statusData.set("device_id", DEVICE_ID);
  statusData.set("timestamp", getTimestamp());
  statusData.set("uptime", millis() / 1000);
  statusData.set("water_state", stateToString(currentState));
  statusData.set("min_sensor_value", analogRead(MIN_SENSOR_PIN));
  statusData.set("max_sensor_value", analogRead(MAX_SENSOR_PIN));
  statusData.set("wifi_rssi", WiFi.RSSI());
  statusData.set("free_heap", ESP.getFreeHeap());
  statusData.set("status", "online");
  
  // Update device status
  if (Firebase.setJSON(firebaseData, deviceStatusPath, statusData)) {
    Serial.println("Status updated in Firebase");
  } else {
    Serial.println("Failed to update status: " + firebaseData.errorReason());
  }
}

void sendHeartbeat() {
  if (!Firebase.ready()) return;
  
  // Update last seen timestamp
  if (Firebase.setString(firebaseData, devicesPath + "/last_seen", getTimestamp())) {
    Serial.println("Heartbeat sent");
  } else {
    Serial.println("Failed to send heartbeat: " + firebaseData.errorReason());
  }
}

String stateToString(WaterState state) {
  switch (state) {
    case WATER_LOW:
      return "WATER_LOW";
    case WATER_FILLING:
      return "WATER_FILLING";
    case WATER_FULL:
      return "WATER_FULL";
    case WATER_ERROR:
      return "WATER_ERROR";
    default:
      return "UNKNOWN";
  }
}

String getTimestamp() {
  // Get current time in ISO format
  // For simplicity, using millis(). In production, use NTP for real timestamps
  return String(millis());
}

void blinkLED(int times) {
  for (int i = 0; i < times; i++) {
    digitalWrite(LED_PIN, LOW);
    delay(150);
    digitalWrite(LED_PIN, HIGH);
    delay(150);
  }
} 