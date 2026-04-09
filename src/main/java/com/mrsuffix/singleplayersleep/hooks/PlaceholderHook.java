package com.mrsuffix.singleplayersleep.hooks;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.core.CooldownManager;
import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.StatsManager;
import com.mrsuffix.singleplayersleep.modules.AfkModule;
import com.mrsuffix.singleplayersleep.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceholderHook extends PlaceholderExpansion {
    
    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    private final SleepManager sleepManager;
    private final CooldownManager cooldownManager;
    private final AfkModule afkModule;
    private final StatsManager statsManager;

    // Cached values updated periodically on the main thread to avoid thread-hopping
    private final Map<String, Integer> cachedSleeping = new ConcurrentHashMap<>();
    private final Map<String, Integer> cachedRequired = new ConcurrentHashMap<>();
    private final Map<String, Boolean> cachedIsNight = new ConcurrentHashMap<>();
    private final Map<String, Boolean> cachedIsProcessing = new ConcurrentHashMap<>();
    private final Map<String, Long> cachedCooldown = new ConcurrentHashMap<>();
    
    public PlaceholderHook(SinglePlayerSleep plugin, ConfigManager configManager,
                           SleepManager sleepManager, CooldownManager cooldownManager,
                           AfkModule afkModule, StatsManager statsManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.sleepManager = sleepManager;
        this.cooldownManager = cooldownManager;
        this.afkModule = afkModule;
        this.statsManager = statsManager;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "sps";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "mrsuffix";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }

    /**
     * Called periodically from the main thread (via TaskScheduler) to refresh
     * cached placeholder values without blocking async threads.
     */
    public void refreshCache() {
        for (World world : plugin.getServer().getWorlds()) {
            if (world == null) continue;
            String name = world.getName();
            cachedIsNight.put(name, TimeUtil.isNight(world));
            cachedCooldown.put(name, cooldownManager.getRemainingSeconds(world));
            sleepManager.getSessionIfExists(world).ifPresentOrElse(session -> {
                cachedSleeping.put(name, session.getEffectiveSleepingCount());
                cachedRequired.put(name, session.calculateRequired());
                cachedIsProcessing.put(name, session.isProcessing());
            }, () -> {
                cachedSleeping.put(name, 0);
                cachedRequired.put(name, 0);
                cachedIsProcessing.put(name, false);
            });
        }
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (identifier == null) {
            return "";
        }
        String key = identifier.toLowerCase();
        
        // Permission gate for sensitive placeholders
        boolean canViewSensitive = player == null || player.hasPermission("singleplayersleep.admin");
        
        if (player == null) {
            switch (key) {
                case "nights_skipped":
                    return String.valueOf(statsManager.getGlobalStats().totalNightsSkipped);
                case "mode":
                    return configManager.getSleepModeName();
                case "percentage":
                    return String.valueOf((int) configManager.getSleepPercentage());
                default:
                    return "";
            }
        }
        
        String worldName = player.getWorld() != null ? player.getWorld().getName() : null;

        switch (key) {
            case "sleeping": {
                if (!canViewSensitive) return "";
                return String.valueOf(cachedSleeping.getOrDefault(worldName, 0));
            }
            case "required": {
                if (!canViewSensitive) return "";
                return String.valueOf(cachedRequired.getOrDefault(worldName, 0));
            }
            case "is_night": {
                return String.valueOf(cachedIsNight.getOrDefault(worldName, false));
            }
            case "is_processing": {
                if (!canViewSensitive) return "";
                return String.valueOf(cachedIsProcessing.getOrDefault(worldName, false));
            }
            case "cooldown": {
                if (!canViewSensitive) return "0";
                return String.valueOf(cachedCooldown.getOrDefault(worldName, 0L));
            }
            case "is_afk": {
                if (!canViewSensitive) return "";
                return String.valueOf(afkModule.isAfk(player));
            }
            case "nights_skipped":
                return String.valueOf(statsManager.getGlobalStats().totalNightsSkipped);
            case "player_times_slept": {
                return String.valueOf(statsManager.getPlayerStats(player.getUniqueId()).timesSlept);
            }
            case "mode":
                return configManager.getSleepModeName();
            case "percentage":
                return String.valueOf((int) configManager.getSleepPercentage());
            default:
                return "";
        }
    }
}
