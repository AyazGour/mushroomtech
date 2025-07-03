#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

// WiFi credentials
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

// Raspberry Pi server details
const char* raspberryPiIP = "192.168.1.100"; // Replace with your Raspberry Pi IP
const int raspberryPiPort = 8080;

// Pin definitions
const int MIN_SENSOR_PIN = 34;  // Analog pin for minimum water level sensor (bottom)
const int MAX_SENSOR_PIN = 35;  // Analog pin for maximum water level sensor (top)
const int LED_PIN = 2;          // Built-in LED for status indication

// Sensor thresholds (adjust based on your sensors)
const int DRY_THRESHOLD = 2000;    // Higher value means dry (no water)
const int WET_THRESHOLD = 1000;    // Lower value means wet (water present)

// System states
enum WaterState {
  WATER_LOW,
  WATER_FILLING,
  WATER_FULL,
  WATER_ERROR
};

WaterState currentState = WATER_LOW;
WaterState previousState = WATER_ERROR;

// Timing variables
unsigned long lastSensorRead = 0;
unsigned long lastStatusSend = 0;
const unsigned long SENSOR_INTERVAL = 1000;    // Read sensors every 1 second
const unsigned long STATUS_INTERVAL = 5000;    // Send status every 5 seconds
const unsigned long DEBOUNCE_DELAY = 3000;     // 3 second debounce for state changes

// Debounce variables
unsigned long stateChangeTime = 0;
bool stateChangeDetected = false;

void setup() {
  Serial.begin(115200);
  
  // Initialize pins
  pinMode(LED_PIN, OUTPUT);
  pinMode(MIN_SENSOR_PIN, INPUT);
  pinMode(MAX_SENSOR_PIN, INPUT);
  
  // Connect to WiFi
  WiFi.begin(ssid, password);
  Serial.print("Connecting to WiFi");
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
    digitalWrite(LED_PIN, !digitalRead(LED_PIN)); // Blink LED while connecting
  }
  
  Serial.println();
  Serial.println("WiFi connected!");
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());
  
  digitalWrite(LED_PIN, HIGH); // Turn on LED when connected
  
  Serial.println("ESP32 Water Level Monitor Started");
  Serial.println("System ready for monitoring...");
}

void loop() {
  unsigned long currentTime = millis();
  
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
  
  delay(100); // Small delay to prevent excessive CPU usage
}

void readSensorsAndUpdateState() {
  // Read sensor values
  int minSensorValue = analogRead(MIN_SENSOR_PIN);
  int maxSensorValue = analogRead(MAX_SENSOR_PIN);
  
  // Determine water presence at each level
  bool waterAtMin = (minSensorValue < WET_THRESHOLD);
  bool waterAtMax = (maxSensorValue < WET_THRESHOLD);
  
  // Print sensor readings for debugging
  Serial.print("Min Sensor: ");
  Serial.print(minSensorValue);
  Serial.print(" (");
  Serial.print(waterAtMin ? "WET" : "DRY");
  Serial.print(") | Max Sensor: ");
  Serial.print(maxSensorValue);
  Serial.print(" (");
  Serial.print(waterAtMax ? "WET" : "DRY");
  Serial.println(")");
  
  // Determine new state based on sensor readings
  WaterState newState = determineWaterState(waterAtMin, waterAtMax);
  
  // Check for state change
  if (newState != currentState) {
    if (!stateChangeDetected) {
      stateChangeDetected = true;
      stateChangeTime = millis();
      Serial.print("State change detected: ");
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
    return WATER_LOW;           // No water at either sensor
  } else if (waterAtMin && !waterAtMax) {
    return WATER_FILLING;       // Water at minimum but not at maximum
  } else if (waterAtMin && waterAtMax) {
    return WATER_FULL;          // Water at both sensors
  } else {
    return WATER_ERROR;         // Water at max but not at min (error condition)
  }
}

void handleStateChanges(unsigned long currentTime) {
  if (stateChangeDetected && (currentTime - stateChangeTime >= DEBOUNCE_DELAY)) {
    // State change confirmed after debounce period
    previousState = currentState;
    
    // Determine new state
    int minSensorValue = analogRead(MIN_SENSOR_PIN);
    int maxSensorValue = analogRead(MAX_SENSOR_PIN);
    bool waterAtMin = (minSensorValue < WET_THRESHOLD);
    bool waterAtMax = (maxSensorValue < WET_THRESHOLD);
    
    currentState = determineWaterState(waterAtMin, waterAtMax);
    
    Serial.print("State changed from ");
    Serial.print(stateToString(previousState));
    Serial.print(" to ");
    Serial.println(stateToString(currentState));
    
    // Send immediate notification for critical state changes
    if (currentState == WATER_LOW || currentState == WATER_FULL) {
      sendPumpCommand();
    }
    
    stateChangeDetected = false;
  }
}

void sendPumpCommand() {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    String url = "http://" + String(raspberryPiIP) + ":" + String(raspberryPiPort) + "/pump";
    
    http.begin(url);
    http.addHeader("Content-Type", "application/json");
    
    // Create JSON payload
    StaticJsonDocument<200> doc;
    doc["device"] = "ESP32_Water_Monitor";
    doc["timestamp"] = millis();
    doc["water_state"] = stateToString(currentState);
    
    if (currentState == WATER_LOW) {
      doc["pump_command"] = "ON";
      doc["message"] = "Water level is low - starting pump";
    } else if (currentState == WATER_FULL) {
      doc["pump_command"] = "OFF";
      doc["message"] = "Water level is full - stopping pump";
    } else {
      doc["pump_command"] = "NONE";
      doc["message"] = "No pump action required";
    }
    
    String jsonString;
    serializeJson(doc, jsonString);
    
    int httpResponseCode = http.POST(jsonString);
    
    if (httpResponseCode > 0) {
      String response = http.getString();
      Serial.print("Pump command sent successfully. Response: ");
      Serial.println(response);
      
      // Blink LED to indicate successful communication
      for (int i = 0; i < 3; i++) {
        digitalWrite(LED_PIN, LOW);
        delay(100);
        digitalWrite(LED_PIN, HIGH);
        delay(100);
      }
    } else {
      Serial.print("Error sending pump command. HTTP error code: ");
      Serial.println(httpResponseCode);
    }
    
    http.end();
  } else {
    Serial.println("WiFi not connected - cannot send pump command");
  }
}

void sendStatusToRaspberryPi() {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    String url = "http://" + String(raspberryPiIP) + ":" + String(raspberryPiPort) + "/status";
    
    http.begin(url);
    http.addHeader("Content-Type", "application/json");
    
    // Create JSON payload with current status
    StaticJsonDocument<300> doc;
    doc["device"] = "ESP32_Water_Monitor";
    doc["timestamp"] = millis();
    doc["water_state"] = stateToString(currentState);
    doc["min_sensor_value"] = analogRead(MIN_SENSOR_PIN);
    doc["max_sensor_value"] = analogRead(MAX_SENSOR_PIN);
    doc["wifi_rssi"] = WiFi.RSSI();
    doc["uptime"] = millis() / 1000;
    
    String jsonString;
    serializeJson(doc, jsonString);
    
    int httpResponseCode = http.POST(jsonString);
    
    if (httpResponseCode > 0) {
      Serial.print("Status sent. Response code: ");
      Serial.println(httpResponseCode);
    } else {
      Serial.print("Error sending status. HTTP error code: ");
      Serial.println(httpResponseCode);
    }
    
    http.end();
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

void printSystemInfo() {
  Serial.println("\n=== ESP32 Water Level Monitor ===");
  Serial.print("Current State: ");
  Serial.println(stateToString(currentState));
  Serial.print("WiFi Status: ");
  Serial.println(WiFi.status() == WL_CONNECTED ? "Connected" : "Disconnected");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());
  Serial.print("Uptime: ");
  Serial.print(millis() / 1000);
  Serial.println(" seconds");
  Serial.println("================================\n");
} 