package com.mrsuffix.singleplayersleep.modules;

import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AfkModule {
    
    private final ConfigManager configManager;
    private final ConcurrentHashMap<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    
    public AfkModule(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    public void recordActivity(UUID uuid) {
        if (uuid == null) {
            return;
        }
        lastActivity.put(uuid, System.currentTimeMillis());
    }
    
    public boolean isAfk(Player player) {
        if (player == null) {
            return false;
        }
        if (!configManager.isAfkEnabled()) {
            return false;
        }
        if (player.hasPermission("singleplayersleep.bypassafk")) {
            return false;
        }
        Long last = lastActivity.get(player.getUniqueId());
        if (last == null) {
            return false;
        }
        return (System.currentTimeMillis() - last) > configManager.getAfkTimeoutMs();
    }
    
    public void onPlayerJoin(UUID uuid) {
        recordActivity(uuid);
    }
    
    public void onPlayerLeave(UUID uuid) {
        if (uuid == null) {
            return;
        }
        lastActivity.remove(uuid);
    }
    
    public void cleanup() {
        lastActivity.entrySet().removeIf(e -> Bukkit.getPlayer(e.getKey()) == null);
    }
    
    public void scheduledCheck(SleepManager sleepManager) {
        if (sleepManager == null) {
            return;
        }
        cleanup();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null) {
                continue;
            }
            if (isAfk(player)) {
                sleepManager.getSessionIfExists(player.getWorld())
                        .ifPresent(session -> session.onPlayerLeave(player));
            }
        }
    }
}
