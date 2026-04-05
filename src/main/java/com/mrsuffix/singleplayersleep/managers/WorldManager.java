package com.mrsuffix.singleplayersleep.managers;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import org.bukkit.World;

public class WorldManager {
    
    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    
    public WorldManager(SinglePlayerSleep plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    public boolean isEnabled(World world) {
        if (world == null) {
            return false;
        }
        
        String worldName = world.getName();
        String mode = configManager.getWorldsMode();
        boolean inList = configManager.getEnabledWorlds().contains(worldName);
        
        if ("whitelist".equalsIgnoreCase(mode)) {
            return inList;
        } else if ("blacklist".equalsIgnoreCase(mode)) {
            return !inList;
        }

        plugin.getLogger().warning("Unknown worlds.mode '" + mode + "' - disabling world: " + worldName);
        return false;
    }
    
    public void reload() {
    }
}
