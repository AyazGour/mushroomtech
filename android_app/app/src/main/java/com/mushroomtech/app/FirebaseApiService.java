package com.mushroomtech.app;

import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class FirebaseApiService {
    
    private static final String TAG = "FirebaseApiService";
    private static final int TIMEOUT_SECONDS = 10;
    
    private DatabaseReference databaseRef;
    private String raspberryPiDeviceId;
    
    public FirebaseApiService() {
        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseRef = database.getReference();
        
        // Find Raspberry Pi device ID
        findRaspberryPiDevice();
    }
    
    private void findRaspberryPiDevice() {
        databaseRef.child("devices").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot deviceSnapshot : dataSnapshot.getChildren()) {
                    String deviceType = deviceSnapshot.child("device_type").getValue(String.class);
                    if ("raspberry_pi".equals(deviceType)) {
                        raspberryPiDeviceId = deviceSnapshot.getKey();
                        Log.i(TAG, "Found Raspberry Pi device: " + raspberryPiDeviceId);
                        break;
                    }
                }
                
                if (raspberryPiDeviceId == null) {
                    Log.w(TAG, "No Raspberry Pi device found in Firebase");
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to find Raspberry Pi device", databaseError.toException());
            }
        });
    }
    
    public CompletableFuture<EnvironmentalData> getEnvironmentalStatus() {
        CompletableFuture<EnvironmentalData> future = new CompletableFuture<>();
        
        if (raspberryPiDeviceId == null) {
            future.completeExceptionally(new Exception("Raspberry Pi device not found"));
            return future;
        }
        
        DatabaseReference statusRef = databaseRef.child("device_status").child(raspberryPiDeviceId);
        
        statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    EnvironmentalData data = parseEnvironmentalData(dataSnapshot);
                    future.complete(data);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        
        // Set timeout
        CompletableFuture.delayedExecutor(TIMEOUT_SECONDS, TimeUnit.SECONDS).execute(() -> {
            if (!future.isDone()) {
                future.completeExceptionally(new Exception("Request timeout"));
            }
        });
        
        return future;
    }
    
    public CompletableFuture<Boolean> controlRelay(String relayName, boolean state) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        if (raspberryPiDeviceId == null) {
            future.completeExceptionally(new Exception("Raspberry Pi device not found"));
            return future;
        }
        
        String commandId = UUID.randomUUID().toString();
        
        Map<String, Object> command = new HashMap<>();
        command.put("relay", relayName);
        command.put("state", state);
        command.put("command_id", commandId);
        command.put("timestamp", System.currentTimeMillis());
        
        // Send command
        DatabaseReference commandRef = databaseRef.child("commands").child(raspberryPiDeviceId).child("relay_control");
        commandRef.setValue(command).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Listen for response
                listenForCommandResponse(commandId, future);
            } else {
                future.completeExceptionally(task.getException());
            }
        });
        
        return future;
    }
    
    public CompletableFuture<Boolean> updateThresholds(double tempMin, double tempMax, double humidityMin, double humidityMax) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        if (raspberryPiDeviceId == null) {
            future.completeExceptionally(new Exception("Raspberry Pi device not found"));
            return future;
        }
        
        Map<String, Object> thresholds = new HashMap<>();
        thresholds.put("temp_min", tempMin);
        thresholds.put("temp_max", tempMax);
        thresholds.put("humidity_min", humidityMin);
        thresholds.put("humidity_max", humidityMax);
        thresholds.put("timestamp", System.currentTimeMillis());
        
        DatabaseReference thresholdRef = databaseRef.child("commands").child(raspberryPiDeviceId).child("thresholds");
        thresholdRef.setValue(thresholds).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                future.complete(true);
            } else {
                future.completeExceptionally(task.getException());
            }
        });
        
        return future;
    }
    
    public CompletableFuture<HistoricalData> getHistoricalData(int hours) {
        CompletableFuture<HistoricalData> future = new CompletableFuture<>();
        
        // For historical data, we'll read from a historical_data node
        // This would be populated by the Raspberry Pi periodically
        DatabaseReference historyRef = databaseRef.child("historical_data").child(raspberryPiDeviceId);
        
        historyRef.limitToLast(hours * 6).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    HistoricalData historicalData = new HistoricalData();
                    historicalData.success = true;
                    
                    // Parse historical data
                    // Implementation depends on how data is structured in Firebase
                    
                    future.complete(historicalData);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });
        
        return future;
    }
    
    private void listenForCommandResponse(String commandId, CompletableFuture<Boolean> future) {
        DatabaseReference responseRef = databaseRef.child("command_responses").child(commandId);
        
        ValueEventListener responseListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Boolean success = dataSnapshot.child("success").getValue(Boolean.class);
                    future.complete(success != null && success);
                    
                    // Clean up the response
                    responseRef.removeValue();
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        };
        
        responseRef.addValueEventListener(responseListener);
        
        // Set timeout for response
        CompletableFuture.delayedExecutor(TIMEOUT_SECONDS, TimeUnit.SECONDS).execute(() -> {
            if (!future.isDone()) {
                responseRef.removeEventListener(responseListener);
                future.completeExceptionally(new Exception("Command response timeout"));
            }
        });
    }
    
    private EnvironmentalData parseEnvironmentalData(DataSnapshot dataSnapshot) {
        EnvironmentalData data = new EnvironmentalData();
        
        // Parse sensor data
        DataSnapshot sensorData = dataSnapshot.child("sensor_data");
        if (sensorData.exists()) {
            Double temp = sensorData.child("temperature").getValue(Double.class);
            Double humidity = sensorData.child("humidity").getValue(Double.class);
            
            data.temperature = temp != null ? temp : 0.0;
            data.humidity = humidity != null ? humidity : 0.0;
        }
        
        // Parse relay states
        DataSnapshot relayStates = dataSnapshot.child("relay_states");
        if (relayStates.exists()) {
            Boolean heater = relayStates.child("heater").getValue(Boolean.class);
            Boolean humidifier = relayStates.child("humidifier").getValue(Boolean.class);
            Boolean aquarium = relayStates.child("aquarium_pump").getValue(Boolean.class);
            
            data.heaterStatus = heater != null && heater;
            data.humidifierStatus = humidifier != null && humidifier;
            data.aquariumPumpStatus = aquarium != null && aquarium;
        }
        
        // Parse ESP32 connection status
        DataSnapshot esp32Status = dataSnapshot.child("esp32_status");
        if (esp32Status.exists()) {
            Boolean connected = esp32Status.child("connected").getValue(Boolean.class);
            data.esp32Connected = connected != null && connected;
        }
        
        data.timestamp = System.currentTimeMillis();
        
        return data;
    }
    
    public void listenForRealTimeUpdates(EnvironmentalDataListener listener) {
        if (raspberryPiDeviceId == null) {
            listener.onError(new Exception("Raspberry Pi device not found"));
            return;
        }
        
        DatabaseReference statusRef = databaseRef.child("device_status").child(raspberryPiDeviceId);
        
        statusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    EnvironmentalData data = parseEnvironmentalData(dataSnapshot);
                    listener.onDataUpdate(data);
                } catch (Exception e) {
                    listener.onError(e);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onError(databaseError.toException());
            }
        });
    }
    
    public void stopListening() {
        // Remove all listeners when app is paused/stopped
        databaseRef.removeEventListener(null);
    }
    
    public boolean testConnection() {
        try {
            // Simple test to check if Firebase is accessible
            FirebaseDatabase.getInstance().getReference(".info/connected");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Firebase connection test failed", e);
            return false;
        }
    }
    
    public interface EnvironmentalDataListener {
        void onDataUpdate(EnvironmentalData data);
        void onError(Exception error);
    }
} 