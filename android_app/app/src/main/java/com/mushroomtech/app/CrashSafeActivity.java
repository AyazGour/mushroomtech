package com.mushroomtech.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ScrollView;
import android.graphics.Color;
import android.view.Gravity;

public class CrashSafeActivity extends Activity {
    
    private TextView statusText;
    private StringBuilder logBuilder = new StringBuilder();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            createUI();
            log("✅ Crash-Safe App Started!");
            log("📱 Minimal UI loaded successfully");
            testBasicFunctionality();
        } catch (Exception e) {
            // Ultimate fallback
            TextView emergency = new TextView(this);
            emergency.setText("🚨 EMERGENCY MODE ACTIVE\n\n" +
                             "App is running in ultra-safe mode.\n\n" +
                             "Error: " + e.getMessage());
            emergency.setTextSize(16);
            emergency.setPadding(40, 40, 40, 40);
            emergency.setBackgroundColor(Color.parseColor("#FFEBEE"));
            setContentView(emergency);
        }
    }
    
    private void createUI() {
        // Create main scroll view
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.WHITE);
        
        // Create main layout
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 30, 30, 30);
        mainLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        
        // Title
        TextView title = new TextView(this);
        title.setText("🍄 Mushroom Tech - Safe Mode");
        title.setTextSize(24);
        title.setTextColor(Color.parseColor("#2E7D32"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);
        mainLayout.addView(title);
        
        // Status display
        statusText = new TextView(this);
        statusText.setText("Initializing safe mode...");
        statusText.setTextSize(14);
        statusText.setTextColor(Color.BLACK);
        statusText.setBackgroundColor(Color.parseColor("#F5F5F5"));
        statusText.setPadding(20, 20, 20, 20);
        statusText.setMinHeight(300);
        mainLayout.addView(statusText);
        
        // Test buttons
        addButton(mainLayout, "🔄 Refresh", this::refreshStatus);
        addButton(mainLayout, "📱 Device Info", this::showDeviceInfo);
        addButton(mainLayout, "🔥 Test Firebase", this::testFirebase);
        addButton(mainLayout, "📊 Open Main App", this::openMainApp);
        
        scrollView.addView(mainLayout);
        setContentView(scrollView);
    }
    
    private void addButton(LinearLayout parent, String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16);
        button.setPadding(20, 15, 20, 15);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 10, 0, 10);
        button.setLayoutParams(params);
        
        button.setOnClickListener(v -> {
            try {
                action.run();
            } catch (Exception e) {
                log("❌ Error: " + e.getMessage());
                updateStatus();
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        parent.addView(button);
    }
    
    private void testBasicFunctionality() {
        log("🔍 Testing basic functionality...");
        
        try {
            String packageName = getPackageName();
            log("📦 Package: " + packageName);
        } catch (Exception e) {
            log("❌ Package test failed: " + e.getMessage());
        }
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            log("📊 Memory: " + maxMemory + " MB");
        } catch (Exception e) {
            log("❌ Memory test failed: " + e.getMessage());
        }
        
        updateStatus();
    }
    
    private void refreshStatus() {
        log("🔄 Status refreshed at " + System.currentTimeMillis());
        updateStatus();
    }
    
    private void showDeviceInfo() {
        log("📱 Device Information:");
        try {
            log("• Android: " + android.os.Build.VERSION.RELEASE);
            log("• API: " + android.os.Build.VERSION.SDK_INT);
            log("• Device: " + android.os.Build.MODEL);
            log("• Brand: " + android.os.Build.MANUFACTURER);
        } catch (Exception e) {
            log("❌ Device info error: " + e.getMessage());
        }
        updateStatus();
    }
    
    private void testFirebase() {
        log("🔥 Testing Firebase...");
        try {
            Class.forName("com.google.firebase.FirebaseApp");
            log("✅ Firebase classes available");
        } catch (ClassNotFoundException e) {
            log("❌ Firebase not found: " + e.getMessage());
        } catch (Exception e) {
            log("❌ Firebase error: " + e.getMessage());
        }
        updateStatus();
    }
    
    private void openMainApp() {
        log("📊 Attempting to open main app...");
        try {
            android.content.Intent intent = new android.content.Intent(this, 
                com.mushroomtech.app.MainActivity.class);
            startActivity(intent);
            log("✅ Main app launched successfully");
        } catch (Exception e) {
            log("❌ Main app failed: " + e.getMessage());
            Toast.makeText(this, "Main app not available: " + e.getMessage(), 
                         Toast.LENGTH_LONG).show();
        }
        updateStatus();
    }
    
    private void log(String message) {
        logBuilder.append(message).append("\n");
    }
    
    private void updateStatus() {
        if (statusText != null) {
            statusText.setText(logBuilder.toString());
        }
    }
} 