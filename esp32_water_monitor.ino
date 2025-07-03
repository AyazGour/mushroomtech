#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

// WiFi credentials - Update these with your network details
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

// Raspberry Pi server details - Update with your Pi's IP
const char* raspberryPiIP = "192.168.1.100";
const int raspberryPiPort = 8080;

// Pin definitions
const int MIN_SENSOR_PIN = 34;  // Bottom sensor (minimum water level)
const int MAX_SENSOR_PIN = 35;  // Top sensor (maximum water level)
const int LED_PIN = 2;          // Built-in LED for status

// Sensor thresholds - Adjust based on your specific sensors
const int DRY_THRESHOLD = 2000;    // Value when sensor is dry (no water)
const int WET_THRESHOLD = 1000;    // Value when sensor detects water

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
unsigned long lastStatusSend = 0;
const unsigned long SENSOR_INTERVAL = 1000;    // Read sensors every 1 second
const unsigned long STATUS_INTERVAL = 10000;   // Send status every 10 seconds
const unsigned long DEBOUNCE_DELAY = 3000;     // 3 second debounce

// Debounce variables
unsigned long stateChangeTime = 0;
bool stateChangeDetected = false;

void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("ESP32 Water Level Monitor Starting...");
  
  // Initialize pins
  pinMode(LED_PIN, OUTPUT);
  pinMode(MIN_SENSOR_PIN, INPUT);
  pinMode(MAX_SENSOR_PIN, INPUT);
  
  // Connect to WiFi
  connectToWiFi();
  
  Serial.println("System ready for monitoring!");
  Serial.println("Sensor Configuration:");
  Serial.println("- Pin 34: Minimum water level (bottom sensor)");
  Serial.println("- Pin 35: Maximum water level (top sensor)");
  Serial.println("- Wet threshold: < " + String(WET_THRESHOLD));
  Serial.println("- Dry threshold: > " + String(DRY_THRESHOLD));
}

void loop() {
  unsigned long currentTime = millis();
  
  // Check WiFi connection
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi disconnected. Reconnecting...");
    connectToWiFi();
  }
  
  // Read sensors at regular intervals
  if (currentTime - lastSensorRead >= SENSOR_INTERVAL) {
    readSensorsAndUpdateState();
    lastSensorRead = currentTime;
  }
  
  // Send status updates at regular intervals
  if (currentTime - lastStatusSend >= STATUS_INTERVAL) {
    sendStatusToRaspberryPi();
    lastStatusSend = currentTime;
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
      sendPumpCommand();
    }
    
    // Visual indication of state change
    blinkLED(5);
    
    stateChangeDetected = false;
  }
}

void sendPumpCommand() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("Cannot send pump command - WiFi not connected");
    return;
  }
  
  HTTPClient http;
  String url = "http://" + String(raspberryPiIP) + ":" + String(raspberryPiPort) + "/pump";
  
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.setTimeout(5000); // 5 second timeout
  
  // Create JSON payload
  StaticJsonDocument<300> doc;
  doc["device"] = "ESP32_Water_Monitor";
  doc["timestamp"] = millis();
  doc["water_state"] = stateToString(currentState);
  doc["min_sensor"] = analogRead(MIN_SENSOR_PIN);
  doc["max_sensor"] = analogRead(MAX_SENSOR_PIN);
  
  if (currentState == WATER_LOW) {
    doc["pump_command"] = "ON";
    doc["message"] = "Water level LOW - START PUMP";
  } else if (currentState == WATER_FULL) {
    doc["pump_command"] = "OFF";
    doc["message"] = "Water level FULL - STOP PUMP";
  } else {
    doc["pump_command"] = "NONE";
    doc["message"] = "No pump action required";
  }
  
  String jsonString;
  serializeJson(doc, jsonString);
  
  Serial.println("Sending pump command:");
  Serial.println(jsonString);
  
  int httpResponseCode = http.POST(jsonString);
  
  if (httpResponseCode > 0) {
    String response = http.getString();
    Serial.print("SUCCESS! Response code: ");
    Serial.print(httpResponseCode);
    Serial.print(" | Response: ");
    Serial.println(response);
    
    // Success blink pattern
    blinkLED(3);
  } else {
    Serial.print("ERROR! HTTP response code: ");
    Serial.println(httpResponseCode);
    
    // Error blink pattern
    for (int i = 0; i < 10; i++) {
      digitalWrite(LED_PIN, LOW);
      delay(50);
      digitalWrite(LED_PIN, HIGH);
      delay(50);
    }
  }
  
  http.end();
}

void sendStatusToRaspberryPi() {
  if (WiFi.status() != WL_CONNECTED) {
    return;
  }
  
  HTTPClient http;
  String url = "http://" + String(raspberryPiIP) + ":" + String(raspberryPiPort) + "/status";
  
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.setTimeout(3000);
  
  // Create status JSON
  StaticJsonDocument<400> doc;
  doc["device"] = "ESP32_Water_Monitor";
  doc["timestamp"] = millis();
  doc["uptime"] = millis() / 1000;
  doc["water_state"] = stateToString(currentState);
  doc["min_sensor_value"] = analogRead(MIN_SENSOR_PIN);
  doc["max_sensor_value"] = analogRead(MAX_SENSOR_PIN);
  doc["wifi_rssi"] = WiFi.RSSI();
  doc["free_heap"] = ESP.getFreeHeap();
  
  String jsonString;
  serializeJson(doc, jsonString);
  
  int httpResponseCode = http.POST(jsonString);
  
  if (httpResponseCode > 0) {
    Serial.print("Status sent - Response: ");
    Serial.println(httpResponseCode);
  } else {
    Serial.print("Status send failed - Error: ");
    Serial.println(httpResponseCode);
  }
  
  http.end();
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

void blinkLED(int times) {
  for (int i = 0; i < times; i++) {
    digitalWrite(LED_PIN, LOW);
    delay(150);
    digitalWrite(LED_PIN, HIGH);
    delay(150);
  }
}