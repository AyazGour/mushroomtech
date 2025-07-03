package com.mushroomtech.app;

public class ChatMessage {
    private String sender;
    private String message;
    private boolean isFromUser;
    private long timestamp;
    
    public ChatMessage(String sender, String message, boolean isFromUser, long timestamp) {
        this.sender = sender;
        this.message = message;
        this.isFromUser = isFromUser;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getSender() {
        return sender;
    }
    
    public String getMessage() {
        return message;
    }
    
    public boolean isFromUser() {
        return isFromUser;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    // Setters
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public void setFromUser(boolean fromUser) {
        isFromUser = fromUser;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 