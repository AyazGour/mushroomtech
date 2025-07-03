package com.mushroomtech.app;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class GeminiAIService {
    
    private static final String TAG = "GeminiAIService";
    private static final String API_KEY = "AIzaSyD5sByoAvhs5RUjuFV-LrMg8lig0poDUTM"; // Gemini API key
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private static final int TIMEOUT_SECONDS = 30;
    
    public interface AnalysisCallback {
        void onSuccess(String analysis);
        void onError(String error);
    }
    
    public void analyzeImage(String base64Image, String prompt, AnalysisCallback callback) {
        if (API_KEY.equals("YOUR_GEMINI_API_KEY_HERE")) {
            // Return demo response when API key is not configured
            String demoResponse = "⚠️ Gemini API key not configured.\n\n" +
                   "Please add your Gemini API key to the GeminiAIService.java file to enable AI analysis.\n\n" +
                   "For now, here's a sample analysis:\n\n" +
                   "🍄 **Mushroom Analysis (Demo)**\n\n" +
                   "**Growth Stage:** Mature fruiting bodies visible\n" +
                   "**Health Status:** Appears healthy with good coloration\n" +
                   "**Environmental Conditions:** Suitable moisture levels detected\n" +
                   "**Recommendations:**\n" +
                   "• Maintain current humidity levels (60-80%)\n" +
                   "• Ensure adequate ventilation\n" +
                   "• Monitor for any signs of contamination\n" +
                   "• Harvest when caps begin to flatten\n\n" +
                   "**Note:** This is a demo response. Configure your Gemini API key for actual AI analysis.";
            callback.onSuccess(demoResponse);
            return;
        }
        
        try {
            String response = makeGeminiRequest(prompt, base64Image);
            String analysis = parseGeminiResponse(response);
            callback.onSuccess(analysis);
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing image with Gemini AI", e);
            callback.onError("AI analysis failed: " + e.getMessage());
        }
    }
    
    private String makeGeminiRequest(String prompt, String base64Image) throws Exception {
        URL url = new URL(BASE_URL + "?key=" + API_KEY);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            // Create request body
            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            
            // Add text prompt
            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);
            parts.put(textPart);
            
            // Add image
            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mime_type", "image/jpeg");
            inlineData.put("data", base64Image);
            imagePart.put("inline_data", inlineData);
            parts.put(imagePart);
            
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);
            
            // Add generation config
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 1024);
            requestBody.put("generationConfig", generationConfig);
            
            // Add safety settings
            JSONArray safetySettings = new JSONArray();
            String[] categories = {
                "HARM_CATEGORY_HARASSMENT",
                "HARM_CATEGORY_HATE_SPEECH",
                "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                "HARM_CATEGORY_DANGEROUS_CONTENT"
            };
            
            for (String category : categories) {
                JSONObject setting = new JSONObject();
                setting.put("category", category);
                setting.put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
                safetySettings.put(setting);
            }
            requestBody.put("safetySettings", safetySettings);
            
            // Write request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Read response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readResponse(connection);
            } else {
                String errorResponse = readErrorResponse(connection);
                throw new IOException("HTTP error " + responseCode + ": " + errorResponse);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    private String readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
    
    private String readErrorResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
    
    private String parseGeminiResponse(String jsonResponse) throws Exception {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray candidates = json.getJSONArray("candidates");
            
            if (candidates.length() > 0) {
                JSONObject candidate = candidates.getJSONObject(0);
                JSONObject content = candidate.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                
                if (parts.length() > 0) {
                    JSONObject part = parts.getJSONObject(0);
                    return part.getString("text");
                }
            }
            
            throw new Exception("No valid response from Gemini AI");
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Gemini response: " + jsonResponse, e);
            throw new Exception("Failed to parse AI response: " + e.getMessage());
        }
    }
    
    // Method to validate API key
    public boolean isApiKeyConfigured() {
        return !API_KEY.equals("YOUR_GEMINI_API_KEY_HERE") && !API_KEY.isEmpty();
    }
    
    // Method to test API connection
    public boolean testConnection() {
        try {
            // Simple test without making actual API call
            return isApiKeyConfigured();
        } catch (Exception e) {
            Log.e(TAG, "Connection test failed", e);
            return false;
        }
    }
} 