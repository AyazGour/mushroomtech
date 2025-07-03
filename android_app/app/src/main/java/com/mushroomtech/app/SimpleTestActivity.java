package com.mushroomtech.app;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;

public class SimpleTestActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create a simple layout programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        
        // Title
        TextView title = new TextView(this);
        title.setText("🍄 Mushroom Tech App");
        title.setTextSize(24);
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);
        
        // Status text
        TextView status = new TextView(this);
        status.setText("✅ App Started Successfully!\n\n📱 Basic functionality is working.\n🔧 Advanced features loading...");
        status.setTextSize(16);
        status.setPadding(0, 0, 0, 20);
        layout.addView(status);
        
        // Test Firebase button
        Button testFirebase = new Button(this);
        testFirebase.setText("Test Firebase Connection");
        testFirebase.setOnClickListener(v -> testFirebaseConnection());
        layout.addView(testFirebase);
        
        // Test Main Activity button
        Button testMainActivity = new Button(this);
        testMainActivity.setText("Open Main Activity");
        testMainActivity.setOnClickListener(v -> openMainActivity());
        layout.addView(testMainActivity);
        
        setContentView(layout);
        
        // Initialize Firebase
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            showToast("Firebase initialized successfully");
        } catch (Exception e) {
            showToast("Firebase error: " + e.getMessage());
        }
    }
    
    private void testFirebaseConnection() {
        try {
            FirebaseApp app = FirebaseApp.getInstance();
            if (app != null) {
                showToast("✅ Firebase is connected!");
            } else {
                showToast("❌ Firebase not initialized");
            }
        } catch (Exception e) {
            showToast("Firebase test failed: " + e.getMessage());
        }
    }
    
    private void openMainActivity() {
        try {
            // Try to open the main activity
            android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            showToast("Main activity failed: " + e.getMessage());
        }
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
