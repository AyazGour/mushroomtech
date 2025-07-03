package com.environmentalcontrol;

public class EnvironmentalData {
    
    // Sensor readings
    public double temperature;
    public double humidity;
    public String lastReading;
    
    // Relay status
    public boolean heaterStatus;
    public boolean humidifierStatus;
    public boolean waterPumpStatus;
    public boolean aquariumPumpStatus;
    
    // Thresholds
    public double tempMin;
    public double tempMax;
    public double humidityMin;
    public double humidityMax;
    
    // ESP32 connection status
    public boolean esp32Connected;
    public String esp32LastContact;
    
    // Calculated properties
    public boolean isTemperatureOptimal() {
        return temperature >= tempMin && temperature <= tempMax;
    }
    
    public boolean isHumidityOptimal() {
        return humidity >= humidityMin && humidity <= humidityMax;
    }
    
    public String getTemperatureStatus() {
        if (temperature < tempMin) {
            return "LOW";
        } else if (temperature > tempMax) {
            return "HIGH";
        } else {
            return "OPTIMAL";
        }
    }
    
    public String getHumidityStatus() {
        if (humidity < humidityMin) {
            return "LOW";
        } else if (humidity > humidityMax) {
            return "HIGH";
        } else {
            return "OPTIMAL";
        }
    }
    
    public int getActiveRelayCount() {
        int count = 0;
        if (heaterStatus) count++;
        if (humidifierStatus) count++;
        if (waterPumpStatus) count++;
        if (aquariumPumpStatus) count++;
        return count;
    }
    
    public String getSystemStatus() {
        if (getActiveRelayCount() == 0) {
            return "STANDBY";
        } else {
            return "ACTIVE";
        }
    }
    
    @Override
    public String toString() {
        return "EnvironmentalData{" +
                "temperature=" + temperature +
                ", humidity=" + humidity +
                ", heaterStatus=" + heaterStatus +
                ", humidifierStatus=" + humidifierStatus +
                ", waterPumpStatus=" + waterPumpStatus +
                ", aquariumPumpStatus=" + aquariumPumpStatus +
                ", esp32Connected=" + esp32Connected +
                '}';
    }
} 