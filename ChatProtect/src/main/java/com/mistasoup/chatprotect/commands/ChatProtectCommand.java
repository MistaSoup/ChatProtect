package com.mistasoup.chatprotect.commands;

import com.mistasoup.chatprotect.ChatProtect;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class ChatProtectCommand implements CommandExecutor, TabCompleter {
    
    private final ChatProtect plugin;
    
    public ChatProtectCommand(ChatProtect plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chatprotect.admin")) {
            String message = plugin.getConfigManager().getNoPermissionMessage();
            if (!message.isEmpty()) {
                sender.sendMessage(message);
            }
            return true;
        }
        
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("§eUsage: /" + label + " reload");
            return true;
        }
        
        plugin.reload();
        
        String message = plugin.getConfigManager().getReloadSuccessMessage();
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1 && sender.hasPermission("chatprotect.admin")) {
            completions.add("reload");
        }
        
        return completions;
    }
}