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
        WorldSettings settings = configManager.getWorldSettings(world);
        if (settings == null) {
            return false;
        }
        return settings.enabled();
    }
    
    public void reload() {
    }
}
