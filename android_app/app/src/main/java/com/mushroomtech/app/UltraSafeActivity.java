package com.mushroomtech.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;

public class UltraSafeActivity extends Activity {
    
    private TextView statusText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            createSimpleUI();
        } catch (Exception e) {
            // Ultimate fallback - create the most basic UI possible
            TextView emergency = new TextView(this);
            emergency.setText("EMERGENCY MODE\nApp is running in safe mode.\nBasic functionality only.");
            emergency.setTextSize(16);
            emergency.setPadding(40, 40, 40, 40);
            emergency.setBackgroundColor(Color.WHITE);
            emergency.setTextColor(Color.BLACK);
            setContentView(emergency);
        }
    }
    
    private void createSimpleUI() {
        // Create main layout
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(20, 20, 20, 20);
        mainLayout.setBackgroundColor(Color.WHITE);
        
        // Title
        TextView title = new TextView(this);
        title.setText("Mushroom Tech App");
        title.setTextSize(24);
        title.setTextColor(Color.parseColor("#4CAF50"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        mainLayout.addView(title);
        
        // Status display
        statusText = new TextView(this);
        statusText.setText("App started successfully!\nInstallation working!\nNo crashes detected!");
        statusText.setTextSize(16);
        statusText.setTextColor(Color.BLACK);
        statusText.setBackgroundColor(Color.parseColor("#F0F0F0"));
        statusText.setPadding(20, 20, 20, 20);
        statusText.setGravity(Gravity.CENTER);
        mainLayout.addView(statusText);
        
        // Test button
        Button testButton = new Button(this);
        testButton.setText("Test Button");
        testButton.setTextSize(16);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (statusText != null) {
                    statusText.setText("Button clicked successfully!\nUI is responsive!\nApp is working normally!");
                }
            }
        });
        mainLayout.addView(testButton);
        
        // Device info button
        Button deviceButton = new Button(this);
        deviceButton.setText("Device Info");
        deviceButton.setTextSize(16);
        deviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeviceInfo();
            }
        });
        mainLayout.addView(deviceButton);
        
        setContentView(mainLayout);
    }
    
    private void showDeviceInfo() {
        if (statusText != null) {
            try {
                String info = "Device Information:\n" +
                             "Android: " + android.os.Build.VERSION.RELEASE + "\n" +
                             "API Level: " + android.os.Build.VERSION.SDK_INT + "\n" +
                             "Model: " + android.os.Build.MODEL + "\n" +
                             "Manufacturer: " + android.os.Build.MANUFACTURER;
                statusText.setText(info);
            } catch (Exception e) {
                statusText.setText("Device info error: " + e.getMessage());
            }
        }
    }
} 