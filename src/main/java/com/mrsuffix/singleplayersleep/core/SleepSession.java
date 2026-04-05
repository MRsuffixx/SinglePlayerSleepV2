package com.mrsuffix.singleplayersleep.core;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.MessageUtil;
import com.mrsuffix.singleplayersleep.managers.StatsManager;
import com.mrsuffix.singleplayersleep.managers.WorldSettings;
import com.mrsuffix.singleplayersleep.modules.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class SleepSession {
    
    private final SinglePlayerSleep plugin;
    private final World world;
    private final ConfigManager configManager;
    private final CooldownManager cooldownManager;
    private final AfkModule afkModule;
    private final EffectsModule effectsModule;
    private final PhantomModule phantomModule;
    private final CountdownModule countdownModule;
    private final StatsManager statsManager;
    
    private final Set<UUID> sleepingPlayers = new HashSet<>();
    private BukkitTask skipTask = null;
    private boolean isProcessing = false;
    
    public SleepSession(SinglePlayerSleep plugin, World world, ConfigManager configManager,
                        CooldownManager cooldownManager, AfkModule afkModule,
                        EffectsModule effectsModule, PhantomModule phantomModule,
                        CountdownModule countdownModule, StatsManager statsManager) {
        this.plugin = plugin;
        this.world = world;
        this.configManager = configManager;
        this.cooldownManager = cooldownManager;
        this.afkModule = afkModule;
        this.effectsModule = effectsModule;
        this.phantomModule = phantomModule;
        this.countdownModule = countdownModule;
        this.statsManager = statsManager;
    }
    
    public void onPlayerSleep(Player player) {
        if (player == null) {
            return;
        }
        
        if (isProcessing) {
            return;
        }
        
        if (cooldownManager.isOnCooldown(world)) {
            long remaining = cooldownManager.getRemainingSeconds(world);
            Map<String, String> replacements = new HashMap<>();
            replacements.put("seconds", String.valueOf(remaining));
            MessageUtil.send(player, "cooldown-active", replacements);
            return;
        }
        
        sleepingPlayers.add(player.getUniqueId());
        
        if (effectsModule != null) {
            effectsModule.playSleepStart(player);
        }
        if (statsManager != null) {
            statsManager.recordSleepEvent(player);
        }
        
        int required = calculateRequired();
        Map<String, String> replacements = new HashMap<>();
        replacements.put("player", player.getName());
        replacements.put("current", String.valueOf(sleepingPlayers.size()));
        replacements.put("required", String.valueOf(required));
        MessageUtil.broadcastWorld(world, "player-sleeping", replacements);
        
        if (configManager.isAfkEnabled() && configManager.isExcludeAfkFromCount()) {
            if (world != null && afkModule != null
                    && world.getPlayers().stream().anyMatch(p -> p != null && afkModule.isAfk(p))) {
                MessageUtil.broadcastWorld(world, "afk-excluded", new HashMap<>());
            }
        }
        
        checkSleepCondition();
    }
    
    public void onPlayerWake(Player player) {
        if (player == null) {
            return;
        }
        
        sleepingPlayers.remove(player.getUniqueId());
        
        if (isProcessing) {
            cancelSkipTask();
            isProcessing = false;
        }
        
        if (sleepingPlayers.isEmpty() && configManager.getSleepMode().equals("percentage")) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", player.getName());
            MessageUtil.broadcastWorld(world, "player-woke-up", replacements);
        }
    }

    public void refreshRequirement() {
        int required = calculateRequired();
        if (required <= 0) {
            return;
        }
        if (isProcessing) {
            if (sleepingPlayers.size() < required) {
                cancelSkipTask();
                isProcessing = false;
            }
            return;
        }
        if (sleepingPlayers.size() >= required) {
            startCountdown();
        }
    }
    
    public void onPlayerLeave(Player player) {
        if (player == null) {
            return;
        }
        
        sleepingPlayers.remove(player.getUniqueId());
        
        if (isProcessing) {
            int required = calculateRequired();
            if (sleepingPlayers.size() < required) {
                cancelSkipTask();
                isProcessing = false;
            }
        }
    }
    
    public void checkSleepCondition() {
        if (isProcessing) {
            return;
        }
        
        int required = calculateRequired();
        if (required <= 0) {
            return;
        }
        
        if (sleepingPlayers.size() >= required) {
            startCountdown();
        } else if (configManager.getSleepMode().equals("percentage")) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("current", String.valueOf(sleepingPlayers.size()));
            replacements.put("required", String.valueOf(required));
            MessageUtil.broadcastWorld(world, "vote-needed", replacements);
        }
    }
    
    public int calculateRequired() {
        if (world == null) {
            return 1;
        }
        
        List<Player> worldPlayers = world.getPlayers();
        if (worldPlayers == null) {
            return 1;
        }
        
        long eligibleCount = worldPlayers.stream()
                .filter(p -> p != null && p.isOnline())
                .filter(p -> !(configManager.isAfkEnabled() 
                        && configManager.isExcludeAfkFromCount() 
                        && afkModule != null
                        && afkModule.isAfk(p)))
                .count();
        
        if (configManager.getSleepMode().equals("single")) {
            return 1;
        } else {
            double percentage = resolvePercentage(eligibleCount);
            return (int) Math.max(1, Math.ceil(eligibleCount * percentage / 100.0));
        }
    }
    
    public void startCountdown() {
        isProcessing = true;
        
        if (!configManager.isCountdownEnabled()) {
            executeSkip();
            return;
        }
        if (countdownModule == null) {
            executeSkip();
            return;
        }
        cancelSkipTask();
        skipTask = countdownModule.start(world, configManager.getCountdownDurationSeconds(), this::executeSkip);
        if (skipTask == null) {
            isProcessing = false;
        }
    }
    
    public void executeSkip() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::executeSkip);
            return;
        }
        
        if (world == null) {
            reset();
            return;
        }
        
        long time = world.getTime();
        if (time < 12541 || time > 23458) {
            reset();
            return;
        }
        
        world.setTime(0);
        
        WorldSettings settings = configManager.getWorldSettings(world);
        WorldSettings.WeatherSettings weatherSettings = settings == null ? null : settings.weatherSettings();
        if (weatherSettings == null) {
            if (configManager.isClearWeather()) {
                world.setStorm(false);
                world.setThundering(false);
            }
        } else if (weatherSettings.changeWeather()) {
            if (weatherSettings.clearRain()) {
                world.setStorm(false);
            }
            if (weatherSettings.clearThunder()) {
                world.setThundering(false);
            }
        }
        
        if (effectsModule != null) {
            effectsModule.playNightSkip(world);
        }
        
        for (UUID uuid : sleepingPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                if (effectsModule != null) {
                    effectsModule.spawnSleepParticles(p);
                }
            }
        }
        
        MessageUtil.broadcastWorld(world, "night-skipped", new HashMap<>());
        
        if (configManager.isAutoSave()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (world != null) {
                    world.save();
                }
            }, 1L);
        }
        
        if (configManager.isPhantomResetOnSkip() && phantomModule != null) {
            phantomModule.resetPhantomTimers(world);
        }
        
        if (statsManager != null) {
            statsManager.recordNightSkip(world, new HashSet<>(sleepingPlayers));
        }
        
        cooldownManager.setCooldown(world);
        
        reset();
    }
    
    public void reset() {
        cancelSkipTask();
        sleepingPlayers.clear();
        isProcessing = false;
    }
    
    private void cancelSkipTask() {
        if (skipTask != null) {
            skipTask.cancel();
            skipTask = null;
        }
    }
    
    public Set<UUID> getSleepingPlayers() {
        return Collections.unmodifiableSet(new HashSet<>(sleepingPlayers));
    }

    private double resolvePercentage(long eligibleCount) {
        WorldSettings settings = configManager.getWorldSettings(world);
        double percentage = settings != null ? settings.sleepPercentage() : configManager.getSleepPercentage();
        if (!configManager.getSleepMode().equalsIgnoreCase("percentage")) {
            return percentage;
        }
        List<SleepRule> rules = settings != null ? settings.dynamicRules() : configManager.getDynamicRules();
        if (rules != null) {
            for (SleepRule rule : rules) {
                if (rule != null && rule.matches(eligibleCount)) {
                    return rule.percentage();
                }
            }
        }
        return percentage;
    }
    
    public boolean isProcessing() {
        return isProcessing;
    }
    
    public World getWorld() {
        return world;
    }
}
