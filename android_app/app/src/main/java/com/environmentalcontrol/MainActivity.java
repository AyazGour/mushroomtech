package com.mushroomtech.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements FirebaseApiService.EnvironmentalDataListener {
    
    private CircularMeterView temperatureMeter;
    private CircularMeterView humidityMeter;
    private CircularMeterView co2Meter;
    private TextView temperatureValue;
    private TextView humidityValue;
    private TextView co2Value;
    private TextView systemStatus;
    private Button waterPumpButton;
    private Button fanButton;
    private Button lightButton;
    private Button dataAnalysisButton;
    private FloatingActionButton aiChatFab;
    private CardView statusCard;
    
    private FirebaseApiService firebaseApiService;
    private Handler mainHandler;
    private ExecutorService executorService;
    private boolean isRealTimeListening = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize Firebase
        initializeFirebase();
        
        initializeViews();
        setupServices();
        setupClickListeners();
        startRealTimeUpdates();
    }
    
    private void initializeFirebase() {
        // Initialize Firebase if not already done
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
        
        // Enable offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
    
    private void initializeViews() {
        temperatureMeter = findViewById(R.id.temperatureMeter);
        humidityMeter = findViewById(R.id.humidityMeter);
        co2Meter = findViewById(R.id.co2Meter);
        temperatureValue = findViewById(R.id.temperatureValue);
        humidityValue = findViewById(R.id.humidityValue);
        co2Value = findViewById(R.id.co2Value);
        systemStatus = findViewById(R.id.systemStatus);
        waterPumpButton = findViewById(R.id.waterPumpButton);
        fanButton = findViewById(R.id.fanButton);
        lightButton = findViewById(R.id.lightButton);
        dataAnalysisButton = findViewById(R.id.dataAnalysisButton);
        aiChatFab = findViewById(R.id.aiChatFab);
        statusCard = findViewById(R.id.statusCard);
        
        // Setup meters
        setupMeters();
    }
    
    private void setupMeters() {
        // Temperature meter (0-50°C)
        temperatureMeter.setRange(0, 50);
        temperatureMeter.setOptimalRange(25, 28);
        temperatureMeter.setMeterColor(getResources().getColor(R.color.dark_green));
        temperatureMeter.setProgressColor(getResources().getColor(R.color.bright_green));
        temperatureMeter.setUnit("°C");
        
        // Humidity meter (0-100%)
        humidityMeter.setRange(0, 100);
        humidityMeter.setOptimalRange(60, 80);
        humidityMeter.setMeterColor(getResources().getColor(R.color.dark_green));
        humidityMeter.setProgressColor(getResources().getColor(R.color.bright_green));
        humidityMeter.setUnit("%");
        
        // CO2 meter (300-1000 ppm)
        co2Meter.setRange(300, 1000);
        co2Meter.setOptimalRange(400, 500);
        co2Meter.setMeterColor(getResources().getColor(R.color.dark_green));
        co2Meter.setProgressColor(getResources().getColor(R.color.bright_green));
        co2Meter.setUnit("ppm");
    }
    
    private void setupServices() {
        firebaseApiService = new FirebaseApiService();
        mainHandler = new Handler(getMainLooper());
        executorService = Executors.newFixedThreadPool(3);
    }
    
    private void setupClickListeners() {
        waterPumpButton.setOnClickListener(v -> toggleWaterPump());
        fanButton.setOnClickListener(v -> toggleFan());
        lightButton.setOnClickListener(v -> toggleLight());
        dataAnalysisButton.setOnClickListener(v -> openDataAnalysis());
        aiChatFab.setOnClickListener(v -> openAIChat());
    }
    
    private void startRealTimeUpdates() {
        if (!isRealTimeListening) {
            firebaseApiService.listenForRealTimeUpdates(this);
            isRealTimeListening = true;
            showToast("Connected to Firebase - Real-time updates enabled");
        }
    }
    
    @Override
    public void onDataUpdate(EnvironmentalData data) {
        mainHandler.post(() -> {
            if (data != null) {
                updateUI(data);
            } else {
                showConnectionError();
            }
        });
    }
    
    @Override
    public void onError(Exception error) {
        mainHandler.post(() -> {
            showConnectionError();
            showToast("Firebase error: " + error.getMessage());
        });
    }
    
    private void updateUI(EnvironmentalData data) {
        try {
            // Update temperature
            if (data.temperature != 0) {
                temperatureMeter.setValue((float) data.temperature);
                temperatureValue.setText(String.format("%.1f°C", data.temperature));
            }
            
            // Update humidity
            if (data.humidity != 0) {
                humidityMeter.setValue((float) data.humidity);
                humidityValue.setText(String.format("%.1f%%", data.humidity));
            }
            
            // Update CO2 (simulated for now)
            float co2Level = 400 + (float)(Math.random() * 200);
            co2Meter.setValue(co2Level);
            co2Value.setText(String.format("%.0f ppm", co2Level));
            
            // Update system status
            updateSystemStatus(data);
            
            // Update button states
            updateButtonStates(data);
            
        } catch (Exception e) {
            showToast("Error updating UI: " + e.getMessage());
        }
    }
    
    private void updateSystemStatus(EnvironmentalData data) {
        StringBuilder status = new StringBuilder();
        status.append("🔄 System Status: ");
        
        if (data.heaterStatus || data.humidifierStatus || data.aquariumPumpStatus) {
            status.append("Active\n");
        } else {
            status.append("Standby\n");
        }
        
        if (data.heaterStatus) {
            status.append("🔥 Heater: ON\n");
        }
        if (data.humidifierStatus) {
            status.append("💨 Humidifier: ON\n");
        }
        if (data.aquariumPumpStatus) {
            status.append("🚰 Water Pump: ON\n");
        }
        
        // ESP32 connection status
        if (data.esp32Connected) {
            status.append("📡 ESP32: Connected\n");
        } else {
            status.append("📡 ESP32: Disconnected\n");
        }
        
        // Firebase connection status
        status.append("☁️ Firebase: Connected");
        
        systemStatus.setText(status.toString());
    }
    
    private void updateButtonStates(EnvironmentalData data) {
        // Update water pump button
        if (data.waterPumpStatus) {
            waterPumpButton.setText("🚰 WATER PUMP: ON");
            waterPumpButton.setBackgroundColor(getResources().getColor(R.color.bright_green));
        } else {
            waterPumpButton.setText("🚰 WATER PUMP: OFF");
            waterPumpButton.setBackgroundColor(getResources().getColor(R.color.dark_green));
        }
        
        // Fan and light buttons (future implementation)
        fanButton.setText("🌀 FAN: OFF (Future)");
        lightButton.setText("💡 LIGHTS: OFF (Future)");
    }
    
    private void showConnectionError() {
        systemStatus.setText("❌ Connection Error\nUnable to connect to Firebase or Raspberry Pi is offline");
        temperatureValue.setText("--°C");
        humidityValue.setText("--%");
        co2Value.setText("-- ppm");
    }
    
    private void toggleWaterPump() {
        waterPumpButton.setEnabled(false);
        
        executorService.execute(() -> {
            try {
                // Get current status first via Firebase
                EnvironmentalData currentData = firebaseApiService.getEnvironmentalStatus().get();
                boolean newState = !currentData.waterPumpStatus;
                
                boolean success = firebaseApiService.controlRelay("WATER_PUMP", newState).get();
                
                mainHandler.post(() -> {
                    waterPumpButton.setEnabled(true);
                    if (success) {
                        showToast("Water pump " + (newState ? "turned ON" : "turned OFF"));
                    } else {
                        showToast("Failed to control water pump");
                    }
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    waterPumpButton.setEnabled(true);
                    showToast("Error: " + e.getMessage());
                });
            }
        });
    }
    
    private void toggleFan() {
        // Future implementation - placeholder for now
        showToast("Fan control will be implemented when hardware is added");
    }
    
    private void toggleLight() {
        // Future implementation - placeholder for now
        showToast("Light control will be implemented when hardware is added");
    }
    
    private void openDataAnalysis() {
        Intent intent = new Intent(MainActivity.this, DataAnalysisActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
    
    private void openAIChat() {
        Intent intent = new Intent(MainActivity.this, AIAnalysisActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
    }
    
    private void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (!isRealTimeListening) {
            startRealTimeUpdates();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Keep listening in background for real-time updates
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRealTimeListening = false;
        if (firebaseApiService != null) {
            firebaseApiService.stopListening();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
} 