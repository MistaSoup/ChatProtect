package com.mistasoup.chatprotect.data;

public class MuteData {
    
    private final long muteExpiry; // When the mute expires (timestamp)
    private final int muteDuration; // Total mute duration in seconds
    
    public MuteData(long muteExpiry, int muteDuration) {
        this.muteExpiry = muteExpiry;
        this.muteDuration = muteDuration;
    }
    
    public long getMuteExpiry() {
        return muteExpiry;
    }
    
    public int getMuteDuration() {
        return muteDuration;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() >= muteExpiry;
    }
    
    public int getSecondsRemaining() {
        long remaining = (muteExpiry - System.currentTimeMillis()) / 1000;
        return Math.max(0, (int) remaining);
    }
}