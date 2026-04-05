package com.mrsuffix.singleplayersleep.hooks;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.core.CooldownManager;
import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.StatsManager;
import com.mrsuffix.singleplayersleep.modules.AfkModule;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
        if (player == null) {
            switch (key) {
                case "nights_skipped":
                    return String.valueOf(statsManager.getGlobalStats().totalNightsSkipped);
                case "mode":
                    return configManager.getSleepMode();
                case "percentage":
                    return String.valueOf((int) configManager.getSleepPercentage());
                default:
                    return "";
            }
        }
        
        switch (key) {
            case "sleeping": {
                if (player == null || player.getWorld() == null) {
                    return "";
                }
                World world = player.getWorld();
                return sleepManager.getSessionIfExists(world)
                        .map(session -> String.valueOf(session.getSleepingPlayers().size()))
                        .orElse("0");
            }
            case "required": {
                if (player == null || player.getWorld() == null) {
                    return "";
                }
                World world = player.getWorld();
                return sleepManager.getSessionIfExists(world)
                        .map(session -> String.valueOf(session.calculateRequired()))
                        .orElse("0");
            }
            case "is_night": {
                if (player == null) {
                    return "";
                }
                World world = player.getWorld();
                if (world == null) {
                    return "";
                }
                return String.valueOf(world.getTime() >= 12541 && world.getTime() <= 23458);
            }
            case "is_processing": {
                if (player == null || player.getWorld() == null) {
                    return "";
                }
                World world = player.getWorld();
                return sleepManager.getSessionIfExists(world)
                        .map(session -> String.valueOf(session.isProcessing()))
                        .orElse("false");
            }
            case "cooldown": {
                if (player == null) {
                    return "0";
                }
                World world = player.getWorld();
                if (world == null) {
                    return "0";
                }
                return String.valueOf(cooldownManager.getRemainingSeconds(world));
            }
            case "is_afk": {
                if (player == null) {
                    return "";
                }
                return String.valueOf(afkModule.isAfk(player));
            }
            case "nights_skipped":
                return String.valueOf(statsManager.getGlobalStats().totalNightsSkipped);
            case "player_times_slept": {
                if (player == null) {
                    return "0";
                }
                return String.valueOf(statsManager.getPlayerStats(player.getUniqueId()).timesSlept);
            }
            case "mode":
                return configManager.getSleepMode();
            case "percentage":
                return String.valueOf((int) configManager.getSleepPercentage());
            default:
                return "";
        }
    }
}
