package com.mistasoup.chatprotect.managers;

import com.mistasoup.chatprotect.ChatProtect;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {
    
    private final ChatProtect plugin;
    private FileConfiguration config;
    
    public ConfigManager(ChatProtect plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }
    
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    // Settings
    public int getMessageHistorySize() {
        return config.getInt("settings.message-history-size", 10);
    }
    
    public double getSimilarityThreshold() {
        return config.getDouble("settings.similarity-threshold", 75.0);
    }
    
    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug", false);
    }
    
    public boolean isVerboseEnabled() {
        return config.getBoolean("settings.verbose", false);
    }
    
    // Duplicate Messages
    public int getMaxRepeats() {
        return config.getInt("duplicate-messages.max-repeats", 2);
    }
    
    public int getCooldownSeconds() {
        return config.getInt("duplicate-messages.cooldown-seconds", 30);
    }
    
    // Blocked Words
    public boolean isBlockedWordsEnabled() {
        return config.getBoolean("blocked-words.enabled", true);
    }
    
    public List<String> getBlockedWords() {
        return config.getStringList("blocked-words.word-list");
    }
    
    // Anti-Spam Kick
    public boolean isAntiSpamKickEnabled() {
        return config.getBoolean("anti-spam-kick.enabled", true);
    }
    
    public int getKickMessageThreshold() {
        return config.getInt("anti-spam-kick.message-threshold", 7);
    }
    
    public int getKickTimeWindow() {
        return config.getInt("anti-spam-kick.time-window-seconds", 5);
    }
    
    public String getKickMessage() {
        return ChatColor.translateAlternateColorCodes('&', 
            config.getString("anti-spam-kick.kick-message", "&cYou have been kicked for spamming!"));
    }
    
    // Auto-Mute
    public boolean isAutoMuteEnabled() {
        return config.getBoolean("auto-mute.enabled", true);
    }
    
    public int getAutoMuteKickThreshold() {
        return config.getInt("auto-mute.kick-threshold", 3);
    }
    
    public int getAutoMuteKickWindow() {
        return config.getInt("auto-mute.kick-window-minutes", 10);
    }
    
    public int getAutoMuteDuration() {
        return config.getInt("auto-mute.mute-duration-seconds", 300);
    }
    
    public String getMuteMessage() {
        return ChatColor.translateAlternateColorCodes('&', 
            config.getString("auto-mute.mute-message", "&cYou are muted for spamming. Time remaining: &e{time} &cseconds."));
    }
    
    public boolean canMutedPlayersReceivePM() {
        return config.getBoolean("auto-mute.allow-receive-pm", true);
    }
    
    // Messages
    public String getReloadSuccessMessage() {
        String message = config.getString("messages.reload-success", "");
        return message.isEmpty() ? "" : ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String getNoPermissionMessage() {
        String message = config.getString("messages.no-permission", "");
        return message.isEmpty() ? "" : ChatColor.translateAlternateColorCodes('&', message);
    }
    
    // Private Messaging
    public boolean isPrivateMessagingEnabled() {
        return config.getBoolean("private-messaging.enabled", true);
    }
    
    public String getPrivateMessageColor() {
        return ChatColor.translateAlternateColorCodes('&', 
            config.getString("private-messaging.message-color", "&d"));
    }
    
    public List<String> getPrivateMessageCommands() {
        return config.getStringList("private-messaging.commands");
    }
    
    public String getPrivateMessageSentFormat() {
        return ChatColor.translateAlternateColorCodes('&', 
            config.getString("private-messaging.sent-format", "&7[&dYou &7-> &d{receiver}&7] &r{message}"));
    }
    
    public String getPrivateMessageReceivedFormat() {
        return ChatColor.translateAlternateColorCodes('&', 
            config.getString("private-messaging.received-format", "&7[&d{sender} &7-> &dYou&7] &r{message}"));
    }
    
    // Chat Colors
    public boolean isChatColorsEnabled() {
        return config.getBoolean("chat-colors.enabled", true);
    }
    
    public String getColorPrefix() {
        return config.getString("chat-colors.color-prefix", ">");
    }
    
    public String getPrefixColor() {
        return ChatColor.translateAlternateColorCodes('&', 
            config.getString("chat-colors.prefix-color", "&a"));
    }
}