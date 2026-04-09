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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.mrsuffix.singleplayersleep.util.TimeUtil;

import java.util.concurrent.ConcurrentHashMap;

public class SleepSession {
    
    // Time constants now imported from TimeUtil
    
    private final SinglePlayerSleep plugin;
    private final World world;
    private final ConfigManager configManager;
    private final CooldownManager cooldownManager;
    private final AfkModule afkModule;
    private final EffectsModule effectsModule;
    private final PhantomModule phantomModule;
    private final CountdownModule countdownModule;
    private final StatsManager statsManager;
    
    private final VoteModule voteModule;
    private final MessageUtil messageUtil;
    
    private final Set<UUID> sleepingPlayers = ConcurrentHashMap.newKeySet();
    private BukkitTask skipTask = null;
    private BukkitTask delayTask = null;
    private boolean isProcessing = false;
    private boolean afkMessageSentThisSession = false;
    
    public SleepSession(SinglePlayerSleep plugin, World world, ConfigManager configManager,
                        CooldownManager cooldownManager, AfkModule afkModule,
                        EffectsModule effectsModule, PhantomModule phantomModule,
                        CountdownModule countdownModule, StatsManager statsManager,
                        VoteModule voteModule, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.world = world;
        this.configManager = configManager;
        this.cooldownManager = cooldownManager;
        this.afkModule = afkModule;
        this.effectsModule = effectsModule;
        this.phantomModule = phantomModule;
        this.countdownModule = countdownModule;
        this.statsManager = statsManager;
        this.voteModule = voteModule;
        this.messageUtil = messageUtil;
    }
    
    public void onPlayerSleep(Player player) {
        if (player == null) {
            return;
        }
        
        if (isProcessing) {
            return;
        }
        
        // Check cooldown unless player has bypass permission
        if (!player.hasPermission("singleplayersleep.bypasscooldown") && cooldownManager.isOnCooldown(world)) {
            long remaining = cooldownManager.getRemainingSeconds(world);
            Map<String, String> replacements = new HashMap<>();
            replacements.put("seconds", String.valueOf(remaining));
            messageUtil.send(player, "cooldown-active", replacements);
            return;
        }
        
        sleepingPlayers.add(player.getUniqueId());
        
        // Also add as a vote in percentage mode to unify counting
        if (configManager.getSleepMode() != null && configManager.getSleepMode().isPercentage() && voteModule != null) {
            voteModule.addVote(player);
        }
        
        if (effectsModule != null) {
            effectsModule.playSleepStart(player);
        }
        if (statsManager != null) {
            statsManager.recordSleepEvent(player);
        }
        
        int required = calculateRequired();
        int current = getEffectiveSleepingCount();
        Map<String, String> replacements = new HashMap<>();
        replacements.put("player", player.getName());
        replacements.put("current", String.valueOf(current));
        replacements.put("required", String.valueOf(required));
        messageUtil.broadcastWorld(world, "player-sleeping", replacements);
        
        if (configManager.isAfkEnabled() && configManager.isExcludeAfkFromCount() && !afkMessageSentThisSession) {
            if (world != null && afkModule != null
                    && world.getPlayers().stream().anyMatch(p -> p != null && afkModule.isAfk(p))) {
                messageUtil.broadcastWorld(world, "afk-excluded", new HashMap<>());
                afkMessageSentThisSession = true;
            }
        }
        
        checkSleepCondition();
    }
    
    public void onPlayerWake(Player player) {
        if (player == null) {
            return;
        }
        
        sleepingPlayers.remove(player.getUniqueId());
        if (voteModule != null) {
            voteModule.removeVote(player);
        }
        
        if (isProcessing) {
            cancelSkipTask();
            cancelDelayTask();
            isProcessing = false;
        }
        
        if (getEffectiveSleepingCount() == 0 && configManager.getSleepMode() != null && configManager.getSleepMode().isPercentage()) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", player.getName());
            messageUtil.broadcastWorld(world, "player-woke-up", replacements);
        }
    }

    public void refreshRequirement() {
        int required = calculateRequired();
        if (required <= 0) {
            return;
        }
        int effectiveCount = getEffectiveSleepingCount();
        if (isProcessing) {
            if (effectiveCount < required) {
                cancelSkipTask();
                isProcessing = false;
            }
            return;
        }
        if (effectiveCount >= required) {
            startCountdown();
        }
    }
    
    public void onPlayerLeave(Player player) {
        if (player == null) {
            return;
        }
        
        sleepingPlayers.remove(player.getUniqueId());
        if (voteModule != null) {
            voteModule.removeVote(player);
        }
        
        if (isProcessing) {
            int required = calculateRequired();
            if (getEffectiveSleepingCount() < required) {
                cancelSkipTask();
                cancelDelayTask();
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
        
        int current = getEffectiveSleepingCount();
        if (current >= required) {
            startDelayOrCountdown();
        } else if (configManager.getSleepMode() != null && configManager.getSleepMode().isPercentage()) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("current", String.valueOf(current));
            replacements.put("required", String.valueOf(required));
            messageUtil.broadcastWorld(world, "vote-needed", replacements);
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
        
        if (configManager.getSleepMode() != null && configManager.getSleepMode().isSingle()) {
            return 1;
        } else {
            double percentage = resolvePercentage(eligibleCount);
            return (int) Math.max(1, Math.ceil(eligibleCount * percentage / 100.0));
        }
    }
    
    private void startDelayOrCountdown() {
        isProcessing = true;
        int delayTicks = Math.max(0, configManager.getDelayTicks());
        if (delayTicks > 0) {
            cancelDelayTask();
            delayTask = Bukkit.getScheduler().runTaskLater(plugin, this::startCountdown, delayTicks);
        } else {
            startCountdown();
        }
    }
    
    private void startCountdown() {
        if (!isProcessing) {
            return;
        }
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
        // TimeUtil.SUNSET_TICKS (12541) = first tick players can sleep (inclusive)
        // TimeUtil.SUNRISE_TICKS (23458) = last tick of night before dawn (inclusive)
        if (time < TimeUtil.SUNSET_TICKS || time > TimeUtil.SUNRISE_TICKS) {
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
            if (p != null && p.getWorld().equals(world)) {
                if (effectsModule != null) {
                    effectsModule.spawnSleepParticles(p);
                }
            }
        }
        
        messageUtil.broadcastWorld(world, "night-skipped", new HashMap<>());
        
        // Defer world save to reduce main-thread lag
        if (configManager.isAutoSave()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (world != null) {
                    try {
                        world.save();
                    } catch (Exception e) {
                        plugin.getLogger().warning("World save failed after night skip: " + e.getMessage());
                    }
                }
            }, 5L);
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
        cancelDelayTask();
        sleepingPlayers.clear();
        if (voteModule != null && world != null) {
            voteModule.clearVotes(world.getName());
        }
        isProcessing = false;
        afkMessageSentThisSession = false;
    }
    
    private void cancelDelayTask() {
        if (delayTask != null) {
            delayTask.cancel();
            delayTask = null;
        }
    }
    
    private void cancelSkipTask() {
        if (skipTask != null) {
            skipTask.cancel();
            skipTask = null;
        }
    }
    
    public Set<UUID> getSleepingPlayers() {
        return Collections.unmodifiableSet(sleepingPlayers);
    }
    
    public int getEffectiveSleepingCount() {
        int count = sleepingPlayers.size();
        if (voteModule == null || world == null) {
            return count;
        }
        World sessionWorld = this.world;
        for (UUID uuid : voteModule.getVotes(world.getName())) {
            if (!sleepingPlayers.contains(uuid)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline() && p.getWorld().equals(sessionWorld)) {
                    count++;
                }
            }
        }
        return count;
    }

    private double resolvePercentage(long eligibleCount) {
        WorldSettings settings = configManager.getWorldSettings(world);
        double percentage = settings != null ? settings.sleepPercentage() : configManager.getSleepPercentage();
        if (configManager.getSleepMode() == null || configManager.getSleepMode().isSingle()) {
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
