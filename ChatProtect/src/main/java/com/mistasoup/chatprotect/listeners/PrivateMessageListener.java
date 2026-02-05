package com.mistasoup.chatprotect.listeners;

import com.mistasoup.chatprotect.ChatProtect;
import com.mistasoup.chatprotect.data.PlayerData;
import com.mistasoup.chatprotect.managers.ConfigManager;
import com.mistasoup.chatprotect.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PrivateMessageListener implements Listener {
    
    private final ChatProtect plugin;
    private final Map<UUID, UUID> replyTargets; // Track who each player last messaged
    
    public PrivateMessageListener(ChatProtect plugin) {
        this.plugin = plugin;
        this.replyTargets = new HashMap<>();
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfigManager().isPrivateMessagingEnabled()) {
            return;
        }
        
        String message = event.getMessage();
        if (!message.startsWith("/")) {
            return;
        }
        
        // Parse command
        String[] parts = message.substring(1).split(" ", 3);
        if (parts.length < 1) {
            return;
        }
        
        String command = parts[0].toLowerCase();
        List<String> pmCommands = plugin.getConfigManager().getPrivateMessageCommands();
        
        // Check if this is a PM command
        if (!pmCommands.contains(command)) {
            return;
        }
        
        // This is a private message - cancel the event and handle it ourselves
        event.setCancelled(true);
        
        Player sender = event.getPlayer();
        Player receiver;
        String pmMessage;
        
        // Handle /r and /reply differently (they don't have a target player name)
        if (command.equals("r") || command.equals("reply")) {
            if (parts.length < 2) {
                sender.sendMessage(org.bukkit.ChatColor.RED + "Usage: /" + command + " <message>");
                return;
            }
            
            // Get reply target
            UUID replyTargetUUID = replyTargets.get(sender.getUniqueId());
            if (replyTargetUUID == null) {
                sender.sendMessage(org.bukkit.ChatColor.RED + "No one to reply to!");
                return;
            }
            
            receiver = Bukkit.getPlayer(replyTargetUUID);
            if (receiver == null || !receiver.isOnline()) {
                sender.sendMessage(org.bukkit.ChatColor.RED + "That player is no longer online.");
                replyTargets.remove(sender.getUniqueId()); // Clear stale reply target
                return;
            }
            
            pmMessage = message.substring(command.length() + 2); // Skip "/r " or "/reply "
        } else {
            // Regular PM commands like /msg, /w, etc.
            if (parts.length < 3) {
                sender.sendMessage(org.bukkit.ChatColor.RED + "Usage: /" + command + " <player> <message>");
                return;
            }
            
            String receiverName = parts[1];
            pmMessage = parts[2];
            
            receiver = Bukkit.getPlayer(receiverName);
            if (receiver == null || !receiver.isOnline()) {
                sender.sendMessage(org.bukkit.ChatColor.RED + "Player not found or not online.");
                return;
            }
        }
        
        logVerbose("=== PRIVATE MESSAGE INTERCEPTED ===");
        logVerbose("From: " + sender.getName());
        logVerbose("To: " + receiver.getName());
        logVerbose("Message: " + pmMessage);
        
        // Check if sender has bypass
        if (sender.hasPermission("chatprotect.bypass")) {
            sendPrivateMessage(sender, receiver, pmMessage);
            return;
        }
        
        // Check if muted
        UUID uuid = sender.getUniqueId();
        if (plugin.getMuteManager().isMuted(uuid)) {
            int secondsRemaining = plugin.getMuteManager().getMuteSecondsRemaining(uuid);
            String muteMsg = plugin.getConfigManager().getMuteMessage()
                .replace("{time}", String.valueOf(secondsRemaining));
            sender.sendMessage(muteMsg);
            logDebug("BLOCKED PM (muted) - " + sender.getName() + " to " + receiver.getName());
            return;
        }
        
        // Run anti-spam checks
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(uuid);
        ConfigManager config = plugin.getConfigManager();
        
        // Check for spam kick
        if (config.isAntiSpamKickEnabled()) {
            if (checkSpamKick(sender, playerData, config)) {
                logDebug("BLOCKED PM (spam kick) - " + sender.getName() + " to " + receiver.getName());
                return;
            }
        }
        
        // Check for blocked words
        if (config.isBlockedWordsEnabled()) {
            if (MessageUtils.containsBlockedWord(pmMessage, config.getBlockedWords())) {
                logDebug("BLOCKED PM (blocked word) - " + sender.getName() + " to " + receiver.getName());
                return;
            }
        }
        
        // Check for duplicates
        if (isDuplicateMessage(pmMessage, playerData, config)) {
            logDebug("BLOCKED PM (duplicate) - " + sender.getName() + " to " + receiver.getName());
            return;
        }
        
        // Message passed all checks
        playerData.addMessage(pmMessage);
        logDebug("ALLOWED PM - " + sender.getName() + " to " + receiver.getName());
        
        sendPrivateMessage(sender, receiver, pmMessage);
    }
    
    private void sendPrivateMessage(Player sender, Player receiver, String message) {
        ConfigManager config = plugin.getConfigManager();
        
        // Track reply targets for both players
        replyTargets.put(sender.getUniqueId(), receiver.getUniqueId());
        replyTargets.put(receiver.getUniqueId(), sender.getUniqueId());
        
        // Apply message color
        String coloredMessage = config.getPrivateMessageColor() + message;
        
        // Format messages
        String sentMsg = config.getPrivateMessageSentFormat()
            .replace("{sender}", sender.getName())
            .replace("{receiver}", receiver.getName())
            .replace("{message}", coloredMessage);
            
        String receivedMsg = config.getPrivateMessageReceivedFormat()
            .replace("{sender}", sender.getName())
            .replace("{receiver}", receiver.getName())
            .replace("{message}", coloredMessage);
        
        // Always send to sender
        sender.sendMessage(sentMsg);
        
        // Check if receiver is muted and if they're allowed to receive PMs
        boolean receiverIsMuted = plugin.getMuteManager().isMuted(receiver.getUniqueId());
        boolean canReceive = config.canMutedPlayersReceivePM();
        
        if (!receiverIsMuted || canReceive) {
            // Receiver is not muted, OR they are muted but can still receive
            receiver.sendMessage(receivedMsg);
            logVerbose("PM sent successfully");
        } else {
            // Receiver is muted and cannot receive PMs
            logVerbose("PM not delivered to " + receiver.getName() + " (muted and config blocks receiving)");
        }
    }
    
    private boolean checkSpamKick(Player player, PlayerData playerData, ConfigManager config) {
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - (config.getKickTimeWindow() * 1000L);
        
        playerData.cleanOldTimestamps(cutoffTime);
        playerData.addTimestamp(currentTime);
        
        if (playerData.getRecentMessageCount() > config.getKickMessageThreshold()) {
            // Record the kick for auto-mute
            plugin.getMuteManager().recordSpamKick(player.getUniqueId());
            
            // Kick player
            player.getScheduler().run(plugin, task -> {
                player.kick(net.kyori.adventure.text.Component.text(config.getKickMessage()));
            }, null);
            return true;
        }
        
        return false;
    }
    
    private boolean isDuplicateMessage(String message, PlayerData playerData, ConfigManager config) {
        // Allow very short messages (1-2 chars) to avoid false positives
        if (message.trim().length() <= 2) {
            return false;
        }
        
        String normalized = MessageUtils.normalizeMessage(message);
        
        // If normalized message is too short or empty, allow it
        if (normalized.length() <= 1) {
            return false;
        }
        
        if (playerData.isOnCooldown(normalized)) {
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
            
            if (similarity >= config.getSimilarityThreshold()) {
                playerData.incrementRepeatCount(normalized);
                
                if (playerData.getRepeatCount(normalized) >= config.getMaxRepeats()) {
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
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up reply targets when player leaves
        UUID uuid = event.getPlayer().getUniqueId();
        replyTargets.remove(uuid);
    }
}