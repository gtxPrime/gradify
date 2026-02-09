package com.gxdevs.gradify.models;

public class ChatMessage {
    private String message;
    private boolean isUserMessage;
    private long timestamp;

    public ChatMessage(String message, boolean isUserMessage, long timestamp) {
        this.message = message;
        this.isUserMessage = isUserMessage;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUserMessage() {
        return isUserMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }
} 