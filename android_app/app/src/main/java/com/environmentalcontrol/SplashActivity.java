package com.environmentalcontrol;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplashActivity extends AppCompatActivity {
    
    private TextView greetingText;
    private TextView statusText;
    private Button skipButton;
    private ImageView logoImage;
    private Handler mainHandler;
    private ExecutorService executorService;
    private FirebaseApiService firebaseApiService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Initialize Firebase first
        initializeFirebase();
        
        initializeViews();
        setupExecutor();
        displayGreeting();
        loadEnvironmentalStatus();
        
        // Auto-proceed to main activity after 8 seconds
        new Handler().postDelayed(() -> {
            if (!isFinishing()) {
                proceedToMainActivity();
            }
        }, 8000);
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
        greetingText = findViewById(R.id.greetingText);
        statusText = findViewById(R.id.statusText);
        skipButton = findViewById(R.id.skipButton);
        logoImage = findViewById(R.id.logoImage);
        
        skipButton.setOnClickListener(v -> proceedToMainActivity());
    }
    
    private void setupExecutor() {
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(getMainLooper());
        firebaseApiService = new FirebaseApiService();
    }
    
    private void displayGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        String greeting;
        if (hour < 12) {
            greeting = "🌅 Good Morning!";
        } else if (hour < 17) {
            greeting = "☀️ Good Afternoon!";
        } else {
            greeting = "🌙 Good Evening!";
        }
        
        greetingText.setText(greeting);
        
        // Animate greeting text
        greetingText.setAlpha(0f);
        greetingText.animate()
                .alpha(1f)
                .setDuration(1000)
                .start();
    }
    
    private void loadEnvironmentalStatus() {
        statusText.setText("🔄 Connecting to Firebase...\nLoading environmental data...");
        
        executorService.execute(() -> {
            try {
                // Test Firebase connection first
                boolean firebaseConnected = firebaseApiService.testConnection();
                
                if (!firebaseConnected) {
                    mainHandler.post(() -> {
                        statusText.setText("⚠️ Unable to connect to Firebase.\nPlease check your internet connection and try again.");
                    });
                    return;
                }
                
                // Get environmental data from Firebase
                firebaseApiService.getEnvironmentalStatus().thenAccept(data -> {
                    mainHandler.post(() -> {
                        if (data != null) {
                            displayEnvironmentalStatus(data);
                        } else {
                            statusText.setText("⚠️ No data available from environmental system.\nRaspberry Pi may be offline.");
                        }
                    });
                }).exceptionally(throwable -> {
                    mainHandler.post(() -> {
                        statusText.setText("❌ Error loading data: " + throwable.getMessage() + 
                                         "\n\nThis could mean:\n• Raspberry Pi is offline\n• Firebase configuration issue\n• Network connectivity problem");
                    });
                    return null;
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    statusText.setText("❌ Firebase connection failed: " + e.getMessage() + 
                                     "\n\nPlease check:\n• Internet connection\n• Firebase configuration\n• App permissions");
                });
            }
        });
    }
    
    private void displayEnvironmentalStatus(EnvironmentalData data) {
        StringBuilder status = new StringBuilder();
        
        status.append("☁️ Connected to Firebase!\n\n");
        
        // Temperature analysis
        status.append("🌡️ Temperature: ").append(String.format("%.1f°C", data.temperature));
        if (data.temperature < 25) {
            status.append(" (Low - Heater activated)");
        } else if (data.temperature > 28) {
            status.append(" (High - Heater deactivated)");
        } else {
            status.append(" (Optimal)");
        }
        status.append("\n\n");
        
        // Humidity analysis
        status.append("💧 Humidity: ").append(String.format("%.1f%%", data.humidity));
        if (data.humidity < 60) {
            status.append(" (Low - Humidifier activated)");
        } else if (data.humidity > 80) {
            status.append(" (High - Humidifier deactivated)");
        } else {
            status.append(" (Optimal)");
        }
        status.append("\n\n");
        
        // CO2 levels (simulated for now)
        double co2Level = 400 + (Math.random() * 200); // Simulate 400-600 ppm
        status.append("🌬️ CO2 Level: ").append(String.format("%.0f ppm", co2Level));
        if (co2Level > 500) {
            status.append(" (High - Consider ventilation)");
        } else {
            status.append(" (Normal)");
        }
        status.append("\n\n");
        
        // Water system status
        status.append("🚰 Water System: ");
        if (data.aquariumPumpStatus) {
            status.append("Filling");
        } else {
            status.append("Standby");
        }
        status.append("\n\n");
        
        // ESP32 connection status
        status.append("📡 ESP32 Monitor: ");
        if (data.esp32Connected) {
            status.append("Connected");
        } else {
            status.append("Disconnected");
        }
        status.append("\n\n");
        
        // System recommendations
        status.append("💡 AI Recommendations:\n");
        if (data.temperature < 25 || data.humidity < 60) {
            status.append("• Monitor heating and humidity levels\n");
        }
        if (co2Level > 500) {
            status.append("• Consider increasing ventilation\n");
        }
        if (!data.esp32Connected) {
            status.append("• Check ESP32 water monitor connection\n");
        }
        if (data.temperature >= 25 && data.temperature <= 28 && 
            data.humidity >= 60 && data.humidity <= 80 && data.esp32Connected) {
            status.append("• Environment is optimal for mushroom growth! 🍄\n");
        }
        
        status.append("\n🔄 Real-time monitoring active via Firebase");
        
        statusText.setText(status.toString());
        
        // Animate status text
        statusText.setAlpha(0f);
        statusText.animate()
                .alpha(1f)
                .setDuration(1500)
                .start();
    }
    
    private void proceedToMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
} 