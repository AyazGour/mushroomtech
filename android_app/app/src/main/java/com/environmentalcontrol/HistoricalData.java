package com.environmentalcontrol;

import org.json.JSONArray;

public class HistoricalData {
    public boolean success;
    public String errorMessage;
    public JSONArray dataPoints;
    
    public HistoricalData() {
        this.success = false;
        this.errorMessage = "";
        this.dataPoints = new JSONArray();
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public JSONArray getDataPoints() {
        return dataPoints;
    }
    
    public int getDataPointCount() {
        return dataPoints != null ? dataPoints.length() : 0;
    }
} 