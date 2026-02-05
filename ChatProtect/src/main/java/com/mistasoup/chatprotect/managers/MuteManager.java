package com.mistasoup.chatprotect.managers;

import com.mistasoup.chatprotect.ChatProtect;
import com.mistasoup.chatprotect.data.MuteData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MuteManager {
    
    private final ChatProtect plugin;
    private final Map<UUID, MuteData> activeMutes;
    private final Map<UUID, List<Long>> recentKicks; // Track spam kicks
    private final File muteFile;
    
    public MuteManager(ChatProtect plugin) {
        this.plugin = plugin;
        this.activeMutes = new HashMap<>();
        this.recentKicks = new HashMap<>();
        this.muteFile = new File(plugin.getDataFolder(), "mutes.yml");
        loadMutes();
    }
    
    /**
     * Check if a player is currently muted
     */
    public boolean isMuted(UUID uuid) {
        MuteData mute = activeMutes.get(uuid);
        if (mute == null) {
            return false;
        }
        
        // Check if mute expired
        if (mute.isExpired()) {
            unmute(uuid);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get remaining mute time in seconds
     */
    public int getMuteSecondsRemaining(UUID uuid) {
        MuteData mute = activeMutes.get(uuid);
        if (mute == null) {
            return 0;
        }
        return mute.getSecondsRemaining();
    }
    
    /**
     * Mute a player for a duration
     */
    public void mute(UUID uuid, int durationSeconds) {
        long expiryTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        MuteData muteData = new MuteData(expiryTime, durationSeconds);
        activeMutes.put(uuid, muteData);
        saveMutes();
        
        logVerbose("Muted player " + uuid + " for " + durationSeconds + " seconds");
    }
    
    /**
     * Unmute a player
     */
    public void unmute(UUID uuid) {
        activeMutes.remove(uuid);
        saveMutes();
        logVerbose("Unmuted player " + uuid);
    }
    
    /**
     * Record a spam kick and check if player should be muted
     */
    public void recordSpamKick(UUID uuid) {
        if (!plugin.getConfigManager().isAutoMuteEnabled()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long windowMillis = plugin.getConfigManager().getAutoMuteKickWindow() * 60 * 1000L;
        long cutoffTime = currentTime - windowMillis;
        
        // Get or create kick list
        List<Long> kicks = recentKicks.computeIfAbsent(uuid, k -> new ArrayList<>());
        
        // Remove old kicks outside the window
        kicks.removeIf(kickTime -> kickTime < cutoffTime);
        
        // Add current kick
        kicks.add(currentTime);
        
        int threshold = plugin.getConfigManager().getAutoMuteKickThreshold();
        
        logVerbose("Player " + uuid + " kick count: " + kicks.size() + "/" + threshold);
        
        // Check if threshold exceeded
        if (kicks.size() >= threshold) {
            int muteDuration = plugin.getConfigManager().getAutoMuteDuration();
            mute(uuid, muteDuration);
            kicks.clear(); // Reset kick count after muting
            
            plugin.getLogger().info("Auto-muted player " + uuid + " for " + muteDuration + " seconds (spam kicks)");
        }
    }
    
    /**
     * Clean up expired mutes and old kick records
     */
    public void cleanup() {
        // Remove expired mutes
        activeMutes.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // Clean old kick records
        long currentTime = System.currentTimeMillis();
        long windowMillis = plugin.getConfigManager().getAutoMuteKickWindow() * 60 * 1000L;
        long cutoffTime = currentTime - windowMillis;
        
        recentKicks.values().forEach(kicks -> kicks.removeIf(kickTime -> kickTime < cutoffTime));
        recentKicks.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    
    /**
     * Save mutes to file (persistent storage)
     */
    private void saveMutes() {
        try {
            FileConfiguration config = new YamlConfiguration();
            
            for (Map.Entry<UUID, MuteData> entry : activeMutes.entrySet()) {
                String uuidStr = entry.getKey().toString();
                MuteData mute = entry.getValue();
                
                config.set("mutes." + uuidStr + ".expiry", mute.getMuteExpiry());
                config.set("mutes." + uuidStr + ".duration", mute.getMuteDuration());
            }
            
            config.save(muteFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save mutes: " + e.getMessage());
        }
    }
    
    /**
     * Load mutes from file
     */
    private void loadMutes() {
        if (!muteFile.exists()) {
            return;
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(muteFile);
            
            if (config.getConfigurationSection("mutes") == null) {
                return;
            }
            
            for (String uuidStr : config.getConfigurationSection("mutes").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    long expiry = config.getLong("mutes." + uuidStr + ".expiry");
                    int duration = config.getInt("mutes." + uuidStr + ".duration");
                    
                    MuteData mute = new MuteData(expiry, duration);
                    
                    // Only load if not expired
                    if (!mute.isExpired()) {
                        activeMutes.put(uuid, mute);
                        logVerbose("Loaded mute for " + uuid + " (" + mute.getSecondsRemaining() + "s remaining)");
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in mutes file: " + uuidStr);
                }
            }
            
            plugin.getLogger().info("Loaded " + activeMutes.size() + " active mutes");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load mutes: " + e.getMessage());
        }
    }
    
    /**
     * Clear all mutes and kicks (for reload)
     */
    public void clearAll() {
        activeMutes.clear();
        recentKicks.clear();
    }
    
    private void logVerbose(String message) {
        if (plugin.getConfigManager().isVerboseEnabled()) {
            plugin.getLogger().info("[VERBOSE] [MUTE] " + message);
        }
    }
}