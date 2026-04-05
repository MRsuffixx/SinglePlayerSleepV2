package com.mrsuffix.singleplayersleep.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;

public class MessageUtil {
    
    private static ConfigManager configManager;
    
    private MessageUtil() {
    }
    
    public static void init(ConfigManager configManager) {
        MessageUtil.configManager = configManager;
    }
    
    public static String format(String key, Map<String, String> replacements) {
        if (configManager == null) {
            return "[SPS] MessageUtil not initialized";
        }
        
        String message = configManager.getMessages().get(key);
        if (message == null) {
            return "[SPS] Missing message: " + key;
        }
        
        String prefix = configManager.getMessages().get("prefix");
        if (prefix != null) {
            message = prefix + message;
        }
        
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static String formatRaw(String key, Map<String, String> replacements) {
        if (configManager == null) {
            return "[SPS] MessageUtil not initialized";
        }
        
        String message = configManager.getMessages().get(key);
        if (message == null) {
            return "[SPS] Missing message: " + key;
        }
        
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static void send(Player player, String key, Map<String, String> replacements) {
        if (player == null) {
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        player.sendMessage(format(key, replacements));
    }
    
    public static void sendActionBar(Player player, String text) {
        if (player == null) {
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        
        try {
            String coloredText = ChatColor.translateAlternateColorCodes('&', text);
            player.sendActionBar(Component.text(coloredText));
        } catch (Exception e) {
            player.sendMessage(text);
        }
    }
    
    public static void broadcastWorld(World world, String key, Map<String, String> replacements) {
        if (world == null) {
            return;
        }
        
        String message = format(key, replacements);
        for (Player player : world.getPlayers()) {
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
    
    public static void broadcastWorldRaw(World world, String formattedText) {
        if (world == null) {
            return;
        }
        
        for (Player player : world.getPlayers()) {
            if (player != null && player.isOnline()) {
                player.sendMessage(formattedText);
            }
        }
    }
}
