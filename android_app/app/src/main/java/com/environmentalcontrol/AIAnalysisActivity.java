package com.environmentalcontrol;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;

public class AIAnalysisActivity extends AppCompatActivity {
    
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int REQUEST_IMAGE_GALLERY = 103;
    
    private ImageView selectedImageView;
    private Button captureButton;
    private Button galleryButton;
    private Button analyzeButton;
    private TextView analysisResult;
    private ProgressBar progressBar;
    private RecyclerView chatRecyclerView;
    private FloatingActionButton backFab;
    
    private Uri photoUri;
    private String currentPhotoPath;
    private Bitmap selectedBitmap;
    private GeminiAIService geminiService;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_analysis);
        
        initializeViews();
        setupServices();
        setupRecyclerView();
        setupClickListeners();
        checkPermissions();
        
        // Add welcome message
        addWelcomeMessage();
    }
    
    private void initializeViews() {
        selectedImageView = findViewById(R.id.selectedImageView);
        captureButton = findViewById(R.id.captureButton);
        galleryButton = findViewById(R.id.galleryButton);
        analyzeButton = findViewById(R.id.analyzeButton);
        analysisResult = findViewById(R.id.analysisResult);
        progressBar = findViewById(R.id.progressBar);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        backFab = findViewById(R.id.backFab);
        
        analyzeButton.setEnabled(false);
    }
    
    private void setupServices() {
        geminiService = new GeminiAIService();
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(getMainLooper());
    }
    
    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
    }
    
    private void setupClickListeners() {
        captureButton.setOnClickListener(v -> showImageSourceDialog());
        galleryButton.setOnClickListener(v -> openGallery());
        analyzeButton.setOnClickListener(v -> analyzeImage());
        backFab.setOnClickListener(v -> finish());
    }
    
    private void addWelcomeMessage() {
        ChatMessage welcomeMessage = new ChatMessage(
            "🤖 AI Assistant", 
            "Welcome to Mushroom Analysis! 🍄\n\n" +
            "I can help you analyze your mushroom growing conditions by examining photos. " +
            "Simply capture a photo or select one from your gallery, and I'll provide detailed analysis including:\n\n" +
            "• Growth stage assessment\n" +
            "• Health condition evaluation\n" +
            "• Environmental recommendations\n" +
            "• Potential issues identification\n" +
            "• Care suggestions\n\n" +
            "Let's get started! 📸", 
            false, 
            System.currentTimeMillis()
        );
        
        chatMessages.add(welcomeMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }
    
    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                REQUEST_CAMERA_PERMISSION);
        }
    }
    
    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(new String[]{"📷 Camera", "🖼️ Gallery"}, (dialog, which) -> {
            if (which == 0) {
                openCamera();
            } else {
                openGallery();
            }
        });
        builder.show();
    }
    
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                    "com.environmentalcontrol.fileprovider",
                    photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }
    
    private void openGallery() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, REQUEST_IMAGE_GALLERY);
    }
    
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "MUSHROOM_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            try {
                Bitmap bitmap = null;
                
                if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    // Image captured from camera
                    bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                } else if (requestCode == REQUEST_IMAGE_GALLERY) {
                    // Image selected from gallery
                    Uri selectedImageUri = data.getData();
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                }
                
                if (bitmap != null) {
                    // Scale bitmap to reasonable size
                    selectedBitmap = scaleBitmap(bitmap, 800, 600);
                    selectedImageView.setImageBitmap(selectedBitmap);
                    selectedImageView.setVisibility(View.VISIBLE);
                    analyzeButton.setEnabled(true);
                    
                    // Add user message
                    ChatMessage userMessage = new ChatMessage(
                        "You", 
                        "📸 Image uploaded for analysis", 
                        true, 
                        System.currentTimeMillis()
                    );
                    chatMessages.add(userMessage);
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                }
                
            } catch (Exception e) {
                Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private Bitmap scaleBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);
        
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    
    private void analyzeImage() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        analyzeButton.setEnabled(false);
        
        // Add analysis start message
        ChatMessage analysisMessage = new ChatMessage(
            "🤖 AI Assistant", 
            "🔍 Analyzing your mushroom image...\nPlease wait while I examine the conditions.", 
            false, 
            System.currentTimeMillis()
        );
        chatMessages.add(analysisMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        
        executorService.execute(() -> {
            try {
                String base64Image = bitmapToBase64(selectedBitmap);
                String analysis = geminiService.analyzeMushroomImage(base64Image);
                
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    analyzeButton.setEnabled(true);
                    
                    // Add AI response
                    ChatMessage aiResponse = new ChatMessage(
                        "🤖 AI Assistant", 
                        analysis, 
                        false, 
                        System.currentTimeMillis()
                    );
                    chatMessages.add(aiResponse);
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    analyzeButton.setEnabled(true);
                    
                    ChatMessage errorMessage = new ChatMessage(
                        "🤖 AI Assistant", 
                        "❌ Sorry, I encountered an error analyzing the image: " + e.getMessage() + 
                        "\n\nPlease try again or check your internet connection.", 
                        false, 
                        System.currentTimeMillis()
                    );
                    chatMessages.add(errorMessage);
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                });
            }
        });
    }
    
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            
            if (!allPermissionsGranted) {
                Toast.makeText(this, "Camera and storage permissions are required for image analysis", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
} 