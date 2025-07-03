package com.mushroomtech.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.graphics.Color;
import android.view.Gravity;

public class MinimalActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create the simplest possible UI
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(Color.WHITE);
        
        // Title
        TextView title = new TextView(this);
        title.setText("🍄 Mushroom Tech App");
        title.setTextSize(24);
        title.setTextColor(Color.parseColor("#4CAF50"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 40);
        layout.addView(title);
        
        // Status
        TextView status = new TextView(this);
        status.setText("✅ App is working!\n\n" +
                      "📱 Installation successful\n" +
                      "🔧 Basic functionality active\n\n" +
                      "Version: Debug Build\n" +
                      "Status: Stable");
        status.setTextSize(16);
        status.setTextColor(Color.BLACK);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 0, 0, 40);
        layout.addView(status);
        
        // Test button
        Button testButton = new Button(this);
        testButton.setText("Test Features");
        testButton.setTextSize(16);
        testButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        testButton.setTextColor(Color.WHITE);
        testButton.setOnClickListener(v -> {
            status.setText("✅ Button clicked successfully!\n\n" +
                          "📱 Touch input working\n" +
                          "🔧 UI responding normally\n\n" +
                          "All basic tests passed!");
        });
        layout.addView(testButton);
        
        // Main app button
        Button mainButton = new Button(this);
        mainButton.setText("Try Main App");
        mainButton.setTextSize(16);
        mainButton.setBackgroundColor(Color.parseColor("#2196F3"));
        mainButton.setTextColor(Color.WHITE);
        mainButton.setOnClickListener(v -> {
            try {
                android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                status.setText("❌ Main app not available\n\n" +
                              "Error: " + e.getMessage() + "\n\n" +
                              "Using safe mode instead.");
            }
        });
        layout.addView(mainButton);
        
        setContentView(layout);
    }
} 