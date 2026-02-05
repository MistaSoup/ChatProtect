package com.mistasoup.chatprotect.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerData {
    
    private final List<String> messageHistory;
    private final Map<String, Long> messageCooldowns;
    private final Map<String, Integer> messageRepeatCount;
    private final List<Long> messageTimestamps;
    private final int maxHistorySize;
    
    public PlayerData(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
        this.messageHistory = new ArrayList<>();
        this.messageCooldowns = new HashMap<>();
        this.messageRepeatCount = new HashMap<>();
        this.messageTimestamps = new ArrayList<>();
    }
    
    public void addMessage(String message) {
        messageHistory.add(message);
        if (messageHistory.size() > maxHistorySize) {
            messageHistory.remove(0);
        }
    }
    
    public List<String> getMessageHistory() {
        return new ArrayList<>(messageHistory);
    }
    
    public void setCooldown(String normalizedMessage, long expiryTime) {
        messageCooldowns.put(normalizedMessage, expiryTime);
    }
    
    public boolean isOnCooldown(String normalizedMessage) {
        Long expiryTime = messageCooldowns.get(normalizedMessage);
        if (expiryTime == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= expiryTime) {
            messageCooldowns.remove(normalizedMessage);
            return false;
        }
        
        return true;
    }
    
    public void incrementRepeatCount(String normalizedMessage) {
        messageRepeatCount.put(normalizedMessage, 
            messageRepeatCount.getOrDefault(normalizedMessage, 0) + 1);
    }
    
    public void resetRepeatCount(String normalizedMessage) {
        messageRepeatCount.remove(normalizedMessage);
    }
    
    public int getRepeatCount(String normalizedMessage) {
        return messageRepeatCount.getOrDefault(normalizedMessage, 0);
    }
    
    public void addTimestamp(long timestamp) {
        messageTimestamps.add(timestamp);
    }
    
    public void cleanOldTimestamps(long cutoffTime) {
        messageTimestamps.removeIf(timestamp -> timestamp < cutoffTime);
    }
    
    public int getRecentMessageCount() {
        return messageTimestamps.size();
    }
    
    public void clear() {
        messageHistory.clear();
        messageCooldowns.clear();
        messageRepeatCount.clear();
        messageTimestamps.clear();
    }
}