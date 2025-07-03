package com.environmentalcontrol;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class ApiService {
    
    private static final String TAG = "ApiService";
    private static final String BASE_URL = "http://192.168.1.100:8080"; // Replace with your Raspberry Pi IP
    private static final int TIMEOUT_SECONDS = 10;
    
    public EnvironmentalData getEnvironmentalStatus() throws Exception {
        String response = makeGetRequest("/status");
        return parseEnvironmentalData(response);
    }
    
    public boolean controlRelay(String relayName, boolean state) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("relay", relayName);
        requestBody.put("state", state);
        
        String response = makePostRequest("/control", requestBody.toString());
        JSONObject jsonResponse = new JSONObject(response);
        
        return "success".equals(jsonResponse.optString("status"));
    }
    
    public HistoricalData getHistoricalData(int hours) throws Exception {
        String response = makeGetRequest("/history?hours=" + hours);
        return parseHistoricalData(response);
    }
    
    public boolean updateThresholds(double tempMin, double tempMax, double humidityMin, double humidityMax) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("temp_min", tempMin);
        requestBody.put("temp_max", tempMax);
        requestBody.put("humidity_min", humidityMin);
        requestBody.put("humidity_max", humidityMax);
        
        String response = makePostRequest("/thresholds", requestBody.toString());
        JSONObject jsonResponse = new JSONObject(response);
        
        return "success".equals(jsonResponse.optString("status"));
    }
    
    private String makeGetRequest(String endpoint) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            connection.setRequestProperty("Accept", "application/json");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readResponse(connection);
            } else {
                throw new IOException("HTTP error code: " + responseCode);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    private String makePostRequest(String endpoint, String requestBody) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            
            // Write request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readResponse(connection);
            } else {
                throw new IOException("HTTP error code: " + responseCode);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    private String readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
    
    private EnvironmentalData parseEnvironmentalData(String jsonResponse) throws JSONException {
        JSONObject json = new JSONObject(jsonResponse);
        JSONObject sensorData = json.getJSONObject("sensor_data");
        JSONObject relayStatus = json.getJSONObject("relay_status");
        JSONObject thresholds = json.getJSONObject("thresholds");
        JSONObject esp32Status = json.getJSONObject("esp32_status");
        
        EnvironmentalData data = new EnvironmentalData();
        
        // Sensor data
        data.temperature = sensorData.getDouble("temperature");
        data.humidity = sensorData.getDouble("humidity");
        data.lastReading = sensorData.getString("last_reading");
        
        // Relay status
        data.heaterStatus = relayStatus.getBoolean("heater");
        data.humidifierStatus = relayStatus.getBoolean("humidifier");
        data.waterPumpStatus = relayStatus.getBoolean("water_pump");
        data.aquariumPumpStatus = relayStatus.getBoolean("aquarium_pump");
        
        // Thresholds
        data.tempMin = thresholds.getDouble("temperature_min");
        data.tempMax = thresholds.getDouble("temperature_max");
        data.humidityMin = thresholds.getDouble("humidity_min");
        data.humidityMax = thresholds.getDouble("humidity_max");
        
        // ESP32 status
        data.esp32Connected = esp32Status.getBoolean("connected");
        data.esp32LastContact = esp32Status.optString("last_contact", null);
        
        return data;
    }
    
    private HistoricalData parseHistoricalData(String jsonResponse) throws JSONException {
        JSONObject json = new JSONObject(jsonResponse);
        HistoricalData historicalData = new HistoricalData();
        
        if ("success".equals(json.optString("status"))) {
            // Parse historical data points
            // This would contain arrays of temperature, humidity, and timestamp data
            // Implementation depends on the specific data structure returned by the Pi
            historicalData.success = true;
            historicalData.dataPoints = json.getJSONArray("data");
        } else {
            historicalData.success = false;
            historicalData.errorMessage = json.optString("message", "Unknown error");
        }
        
        return historicalData;
    }
    
    // Method to test connection
    public boolean testConnection() {
        try {
            makeGetRequest("/status");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Connection test failed", e);
            return false;
        }
    }
    
    // Method to set custom base URL (for different Pi IP addresses)
    public void setBaseUrl(String baseUrl) {
        // This would require making BASE_URL non-final and adding proper validation
        // For now, users need to modify the BASE_URL constant directly
        Log.i(TAG, "To change base URL, modify BASE_URL constant in ApiService.java");
    }
} 