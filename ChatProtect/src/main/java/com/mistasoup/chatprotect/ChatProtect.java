package com.mistasoup.chatprotect;

import com.mistasoup.chatprotect.commands.ChatProtectCommand;
import com.mistasoup.chatprotect.listeners.ChatListener;
import com.mistasoup.chatprotect.managers.ConfigManager;
import com.mistasoup.chatprotect.managers.MuteManager;
import com.mistasoup.chatprotect.managers.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatProtect extends JavaPlugin {
    
    private static ChatProtect instance;
    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private MuteManager muteManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.muteManager = new MuteManager(this);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new com.mistasoup.chatprotect.listeners.PrivateMessageListener(this), this);
        
        // Register commands
        getCommand("chatprotect").setExecutor(new ChatProtectCommand(this));
        
        // Start cleanup task (runs every 5 minutes)
        getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            muteManager.cleanup();
        }, 20L * 60 * 5, 20L * 60 * 5); // 5 minutes
        
        getLogger().info("ChatProtect has been enabled!");
        getLogger().info("Folia-compatible anti-spam protection active.");
    }
    
    @Override
    public void onDisable() {
        // Clear player data on shutdown
        if (playerDataManager != null) {
            playerDataManager.clearAll();
        }
        
        getLogger().info("ChatProtect has been disabled!");
    }
    
    public static ChatProtect getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public MuteManager getMuteManager() {
        return muteManager;
    }
    
    public void reload() {
        configManager.reload();
        playerDataManager.clearAll();
        muteManager.clearAll();
    }
}