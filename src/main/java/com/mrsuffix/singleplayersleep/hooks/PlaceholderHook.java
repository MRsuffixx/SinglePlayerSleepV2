package com.mrsuffix.singleplayersleep.hooks;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.core.CooldownManager;
import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.StatsManager;
import com.mrsuffix.singleplayersleep.modules.AfkModule;
import com.mrsuffix.singleplayersleep.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class PlaceholderHook extends PlaceholderExpansion {
    
    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    private final SleepManager sleepManager;
    private final CooldownManager cooldownManager;
    private final AfkModule afkModule;
    private final StatsManager statsManager;
    
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
        
        switch (key) {
            case "sleeping": {
                if (!canViewSensitive) return "";
                return runSync(() -> {
                    World world = player.getWorld();
                    if (world == null) return "0";
                    return sleepManager.getSessionIfExists(world)
                            .map(session -> String.valueOf(session.getEffectiveSleepingCount()))
                            .orElse("0");
                });
            }
            case "required": {
                if (!canViewSensitive) return "";
                return runSync(() -> {
                    World world = player.getWorld();
                    if (world == null) return "0";
                    return sleepManager.getSessionIfExists(world)
                            .map(session -> String.valueOf(session.calculateRequired()))
                            .orElse("0");
                });
            }
            case "is_night": {
                return runSync(() -> {
                    World world = player.getWorld();
                    if (world == null) return "";
                    return String.valueOf(TimeUtil.isNight(world));
                });
            }
            case "is_processing": {
                if (!canViewSensitive) return "";
                return runSync(() -> {
                    World world = player.getWorld();
                    if (world == null) return "false";
                    return sleepManager.getSessionIfExists(world)
                            .map(session -> String.valueOf(session.isProcessing()))
                            .orElse("false");
                });
            }
            case "cooldown": {
                if (!canViewSensitive) return "0";
                return runSync(() -> {
                    World world = player.getWorld();
                    if (world == null) return "0";
                    return String.valueOf(cooldownManager.getRemainingSeconds(world));
                });
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
    
    private String runSync(Callable<String> task) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return task.call();
            } catch (Exception e) {
                return "";
            }
        }
        FutureTask<String> future = new FutureTask<>(task);
        Bukkit.getScheduler().runTask(plugin, future);
        try {
            return future.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
    }
}
