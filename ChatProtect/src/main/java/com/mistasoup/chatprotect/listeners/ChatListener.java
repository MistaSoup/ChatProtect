package com.mistasoup.chatprotect.listeners;

import com.mistasoup.chatprotect.ChatProtect;
import com.mistasoup.chatprotect.handlers.ChatChannel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    
    private final ChatProtect plugin;
    private final ChatChannel chatChannel;
    
    public ChatListener(ChatProtect plugin) {
        this.plugin = plugin;
        this.chatChannel = new ChatChannel(plugin);
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        // Immediately cancel the default chat event
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Process through our custom chat channel
        chatChannel.processMessage(player, message);
    }
}