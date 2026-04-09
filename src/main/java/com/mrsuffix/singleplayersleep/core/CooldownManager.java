package com.mrsuffix.singleplayersleep.core;

import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import org.bukkit.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    
    private final ConfigManager configManager;
    private final Map<String, Long> lastSkipTime = new ConcurrentHashMap<>();
    
    public CooldownManager(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    public boolean isOnCooldown(World world) {
        if (world == null) {
            return false;
        }
        
        Long last = lastSkipTime.get(world.getName());
        if (last == null) {
            return false;
        }
        
        long cooldownMs = configManager.getCooldownSeconds() * 1000L;
        return System.currentTimeMillis() < last + cooldownMs;
    }
    
    public void setCooldown(World world) {
        if (world == null) {
            return;
        }
        lastSkipTime.put(world.getName(), System.currentTimeMillis());
    }
    
    public long getRemainingSeconds(World world) {
        if (world == null) {
            return 0;
        }
        
        Long last = lastSkipTime.get(world.getName());
        if (last == null) {
            return 0;
        }
        
        long cooldownMs = configManager.getCooldownSeconds() * 1000L;
        long remaining = (last + cooldownMs - System.currentTimeMillis()) / 1000L;
        return Math.max(0, remaining);
    }
    
    public void clear() {
        lastSkipTime.clear();
    }
}
