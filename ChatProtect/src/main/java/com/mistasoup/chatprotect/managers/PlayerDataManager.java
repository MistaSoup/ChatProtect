package com.mistasoup.chatprotect.managers;

import com.mistasoup.chatprotect.ChatProtect;
import com.mistasoup.chatprotect.data.PlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    
    private final ChatProtect plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    
    public PlayerDataManager(ChatProtect plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
    }
    
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, k -> 
            new PlayerData(plugin.getConfigManager().getMessageHistorySize())
        );
    }
    
    public void removePlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
    }
    
    public void clearAll() {
        playerDataMap.clear();
    }
}