
package com.example.escapeGame;

import com.google.gson.JsonObject;
import java.time.LocalDateTime;

public class ChatMessage {
    private String sender;
    private String receiver;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    
    public enum MessageType {
        PUBLIC, PRIVATE, SYSTEM
    }
    
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ChatMessage(String sender, String receiver, String content, MessageType type) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    // Convert to JSON for network transmission
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("sender", sender);
        json.addProperty("receiver", receiver);
        json.addProperty("content", content);
        json.addProperty("type", type.toString());
        json.addProperty("timestamp", timestamp.toString());
        return json;
    }
    
    // Create from JSON
    public static ChatMessage fromJson(JsonObject json) {
        ChatMessage message = new ChatMessage();
        message.setSender(json.has("sender") ? json.get("sender").getAsString() : "");
        message.setReceiver(json.has("receiver") ? json.get("receiver").getAsString() : "");
        message.setContent(json.has("content") ? json.get("content").getAsString() : "");
        
        String typeStr = json.has("type") ? json.get("type").getAsString() : "PUBLIC";
        message.setType(MessageType.valueOf(typeStr));
        
        if (json.has("timestamp")) {
            message.setTimestamp(LocalDateTime.parse(json.get("timestamp").getAsString()));
        }
        
        return message;
    }
    
    @Override
    public String toString() {
        String timeStr = timestamp.toString().substring(11, 16); // HH:MM format
        String prefix = type == MessageType.PRIVATE ? "[PM]" : "";
        return String.format("[%s] %s%s: %s", timeStr, prefix, sender, content);
    }
}