package com.mushroomtech.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
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
        
        systemStatus.setText(status.toString());
    }
    
    private void updateButtonStates(EnvironmentalData data) {
        // Update button text based on current states
        waterPumpButton.setText(data.aquariumPumpStatus ? "Water Pump: ON" : "Water Pump: OFF");
        fanButton.setText(data.humidifierStatus ? "Fan: ON" : "Fan: OFF");
        lightButton.setText(data.heaterStatus ? "Light: ON" : "Light: OFF");
        
        // Update button colors
        int activeColor = getResources().getColor(R.color.bright_green);
        int inactiveColor = getResources().getColor(R.color.dark_green);
        
        waterPumpButton.setBackgroundColor(data.aquariumPumpStatus ? activeColor : inactiveColor);
        fanButton.setBackgroundColor(data.humidifierStatus ? activeColor : inactiveColor);
        lightButton.setBackgroundColor(data.heaterStatus ? activeColor : inactiveColor);
    }
    
    private void showConnectionError() {
        systemStatus.setText("❌ Connection Error\nUnable to connect to Firebase.\nPlease check your internet connection.");
        statusCard.setCardBackgroundColor(getResources().getColor(R.color.error_red));
    }
    
    private void toggleWaterPump() {
        executorService.execute(() -> {
            try {
                firebaseApiService.controlRelay("aquarium_pump", true).thenAccept(success -> {
                    mainHandler.post(() -> {
                        if (success) {
                            showToast("Water pump command sent");
                        } else {
                            showToast("Failed to control water pump");
                        }
                    });
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showToast("Error controlling water pump: " + e.getMessage());
                });
            }
        });
    }
    
    private void toggleFan() {
        // Toggle humidifier (fan)
        showToast("Fan control not implemented yet");
    }
    
    private void toggleLight() {
        // Toggle heater (light)
        showToast("Light control not implemented yet");
    }
    
    private void openDataAnalysis() {
        // Open data analysis activity
        showToast("Data analysis feature coming soon");
    }
    
    private void openAIChat() {
        Intent intent = new Intent(this, AIAnalysisActivity.class);
        startActivity(intent);
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
        if (firebaseApiService != null) {
            firebaseApiService.stopListening();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        isRealTimeListening = false;
    }
} 