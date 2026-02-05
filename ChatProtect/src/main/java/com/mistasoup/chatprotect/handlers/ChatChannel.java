package com.mistasoup.chatprotect.handlers;

import com.mistasoup.chatprotect.ChatProtect;
import com.mistasoup.chatprotect.data.PlayerData;
import com.mistasoup.chatprotect.managers.ConfigManager;
import com.mistasoup.chatprotect.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Custom chat channel that intercepts, processes, and delivers messages
 */
public class ChatChannel {
    
    private final ChatProtect plugin;
    
    public ChatChannel(ChatProtect plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Process a chat message and determine if it should be sent
     * Returns true if message was handled (sent or blocked)
     */
    public boolean processMessage(Player player, String message) {
        logVerbose("=== CHAT CHANNEL: Processing message from " + player.getName() + " ===");
        logVerbose("Raw message: '" + message + "'");
        
        UUID uuid = player.getUniqueId();
        
        // Bypass check
        if (player.hasPermission("chatprotect.bypass")) {
            logVerbose("Player has bypass - sending directly");
            sendMessage(player, message);
            return true;
        }
        
        // Check if muted
        if (plugin.getMuteManager().isMuted(uuid)) {
            int secondsRemaining = plugin.getMuteManager().getMuteSecondsRemaining(uuid);
            String muteMsg = plugin.getConfigManager().getMuteMessage()
                .replace("{time}", String.valueOf(secondsRemaining));
            player.sendMessage(muteMsg);
            logDebug("BLOCKED (muted) - " + player.getName() + ": " + message);
            return true;
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(uuid);
        ConfigManager config = plugin.getConfigManager();
        
        // Anti-spam kick check
        if (config.isAntiSpamKickEnabled()) {
            if (checkAndKickSpammer(player, playerData, config)) {
                logDebug("BLOCKED (spam kick) - " + player.getName() + ": " + message);
                return true; // Message blocked, player kicked
            }
        }
        
        // Check for blocked words
        if (config.isBlockedWordsEnabled()) {
            if (MessageUtils.containsBlockedWord(message, config.getBlockedWords())) {
                logDebug("BLOCKED (blocked word) - " + player.getName() + ": " + message);
                return true; // Message blocked
            }
        }
        
        // Duplicate detection
        if (isDuplicateMessage(message, playerData, config)) {
            logDebug("BLOCKED (duplicate) - " + player.getName() + ": " + message);
            return true; // Message blocked
        }
        
        // Message passed all checks - send it
        playerData.addMessage(message);
        logDebug("ALLOWED - " + player.getName() + ": " + message);
        
        sendMessage(player, message);
        return true;
    }
    
    /**
     * Send the message to all players
     */
    private void sendMessage(Player sender, String message) {
        logVerbose(">>> SENDING MESSAGE TO ALL PLAYERS <<<");
        
        ConfigManager config = plugin.getConfigManager();
        
        // Check if message starts with color prefix
        String displayMessage = message;
        if (config.isChatColorsEnabled()) {
            String prefix = config.getColorPrefix();
            if (message.startsWith(prefix)) {
                // Remove prefix and apply color
                displayMessage = message.substring(prefix.length()).trim();
                displayMessage = config.getPrefixColor() + displayMessage;
                logVerbose("Color prefix detected - applying color: " + config.getPrefixColor());
            }
        }
        
        // Build chat message in Minecraft format: <PlayerName> message
        Component chatComponent = Component.text()
            .append(Component.text("<", NamedTextColor.WHITE))
            .append(Component.text(sender.getName(), NamedTextColor.WHITE))
            .append(Component.text("> ", NamedTextColor.WHITE))
            .append(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(displayMessage))
            .build();
        
        int playerCount = Bukkit.getOnlinePlayers().size();
        logVerbose("Broadcasting to " + playerCount + " online players");
        
        // Send to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(chatComponent);
            logVerbose("  - Sent to: " + player.getName());
        }
        
        // Send to console
        Bukkit.getConsoleSender().sendMessage(chatComponent);
        logVerbose("  - Sent to console");
        logVerbose(">>> MESSAGE BROADCAST COMPLETE <<<");
    }
    
    private boolean checkAndKickSpammer(Player player, PlayerData playerData, ConfigManager config) {
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - (config.getKickTimeWindow() * 1000L);
        
        playerData.cleanOldTimestamps(cutoffTime);
        playerData.addTimestamp(currentTime);
        
        int count = playerData.getRecentMessageCount();
        int threshold = config.getKickMessageThreshold();
        
        logVerbose("Spam check: " + count + "/" + threshold);
        
        if (count > threshold) {
            // Record the kick for auto-mute
            plugin.getMuteManager().recordSpamKick(player.getUniqueId());
            
            // Kick player
            player.getScheduler().run(plugin, task -> {
                player.kick(Component.text(config.getKickMessage()));
            }, null);
            return true;
        }
        
        return false;
    }
    
    private boolean isDuplicateMessage(String message, PlayerData playerData, ConfigManager config) {
        // Allow very short messages (1-2 chars) to avoid false positives
        if (message.trim().length() <= 2) {
            logVerbose("Short message (≤2 chars) - skipping duplicate check");
            return false;
        }
        
        String normalized = MessageUtils.normalizeMessage(message);
        
        // If normalized message is too short or empty, allow it
        if (normalized.length() <= 1) {
            logVerbose("Normalized message too short - skipping duplicate check");
            return false;
        }
        
        double threshold = config.getSimilarityThreshold();
        
        logVerbose("Checking duplicates for: '" + normalized + "'");
        
        if (playerData.isOnCooldown(normalized)) {
            logVerbose("Message on cooldown");
            return true;
        }
        
        List<String> history = playerData.getMessageHistory();
        for (String past : history) {
            // Skip comparing with very short past messages
            if (past.trim().length() <= 2) {
                continue;
            }
            
            double similarity = MessageUtils.calculateSimilarity(
                MessageUtils.normalizeMessage(message),
                MessageUtils.normalizeMessage(past)
            );
            
            logVerbose("Similarity: " + String.format("%.1f", similarity) + "% with '" + past + "'");
            
            if (similarity >= threshold) {
                playerData.incrementRepeatCount(normalized);
                int repeatCount = playerData.getRepeatCount(normalized);
                
                logVerbose("Repeat count: " + repeatCount + "/" + config.getMaxRepeats());
                
                if (repeatCount >= config.getMaxRepeats()) {
                    long cooldownExpiry = System.currentTimeMillis() + (config.getCooldownSeconds() * 1000L);
                    playerData.setCooldown(normalized, cooldownExpiry);
                    playerData.resetRepeatCount(normalized);
                    return true;
                }
                
                return false;
            }
        }
        
        playerData.resetRepeatCount(normalized);
        return false;
    }
    
    private void logDebug(String message) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
    
    private void logVerbose(String message) {
        if (plugin.getConfigManager().isVerboseEnabled()) {
            plugin.getLogger().info("[VERBOSE] " + message);
        }
    }
}