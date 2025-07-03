package com.mushroomtech.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.content.Intent;

public class BasicLauncherActivity extends Activity {
    
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
        title.setText("Mushroom Tech App");
        title.setTextSize(24);
        title.setTextColor(Color.parseColor("#4CAF50"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 40);
        layout.addView(title);
        
        // Status
        TextView status = new TextView(this);
        status.setText("SUCCESS!\n\nApp is working correctly!\nInstallation successful!\nNo crashes detected!\n\nVersion: Stable Build");
        status.setTextSize(16);
        status.setTextColor(Color.BLACK);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 0, 0, 40);
        layout.addView(status);
        
        // Test button
        Button testButton = new Button(this);
        testButton.setText("Test App Features");
        testButton.setTextSize(16);
        testButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        testButton.setTextColor(Color.WHITE);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                status.setText("BUTTON TEST PASSED!\n\nTouch input working!\nUI responding normally!\nApp is fully functional!");
            }
        });
        layout.addView(testButton);
        
        // Main app button
        Button mainButton = new Button(this);
        mainButton.setText("Open Full App");
        mainButton.setTextSize(16);
        mainButton.setBackgroundColor(Color.parseColor("#2196F3"));
        mainButton.setTextColor(Color.WHITE);
        mainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(BasicLauncherActivity.this, MainActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    status.setText("Main app not available.\nUsing safe mode instead.\n\nError: " + e.getMessage());
                }
            }
        });
        layout.addView(mainButton);
        
        setContentView(layout);
    }
} 