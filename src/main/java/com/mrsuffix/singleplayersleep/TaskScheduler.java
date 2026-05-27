package com.mrsuffix.singleplayersleep;

import com.mrsuffix.singleplayersleep.hooks.PlaceholderHook;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.StatsManager;
import com.mrsuffix.singleplayersleep.modules.AfkModule;
import com.mrsuffix.singleplayersleep.modules.UpdateModule;
import com.mrsuffix.singleplayersleep.modules.VoteModule;
import com.mrsuffix.singleplayersleep.util.TickUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

import com.mrsuffix.singleplayersleep.core.SleepManager;

public class TaskScheduler {

    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    private final AfkModule afkModule;
    private final UpdateModule updateModule;
    private final StatsManager statsManager;
    private final SleepManager sleepManager;
    private final VoteModule voteModule;
    private final List<BukkitTask> tasks = new ArrayList<>();
    private PlaceholderHook placeholderHook;
    
    public TaskScheduler(SinglePlayerSleep plugin, ConfigManager configManager,
                         AfkModule afkModule, UpdateModule updateModule,
                         StatsManager statsManager, SleepManager sleepManager,
                         VoteModule voteModule) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.afkModule = afkModule;
        this.updateModule = updateModule;
        this.statsManager = statsManager;
        this.sleepManager = sleepManager;
        this.voteModule = voteModule;
    }

    public void setPlaceholderHook(PlaceholderHook placeholderHook) {
        this.placeholderHook = placeholderHook;
    }
    
    public void startAll() {
        stopAll();
        
        // AFK check task
        int afkInterval = Math.max(20, configManager.getAfkCheckIntervalTicks());
        BukkitTask afkTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> afkModule.scheduledCheck(sleepManager),
                afkInterval, afkInterval);
        tasks.add(afkTask);

        // PlaceholderAPI cache refresh task (every 10 seconds)
        if (placeholderHook != null) {
            BukkitTask papiTask = Bukkit.getScheduler().runTaskTimer(plugin,
                    placeholderHook::refreshCache,
                    TickUtil.TICKS_PER_SECOND * 10, TickUtil.TICKS_PER_SECOND * 10);
            tasks.add(papiTask);
        }
        
        // Update checker task
        if (configManager.isUpdateCheckerEnabled() && updateModule != null) {
            long updateInterval = Math.max(TickUtil.TICKS_PER_HOUR,
                    configManager.getUpdateCheckIntervalHours() * TickUtil.TICKS_PER_HOUR);
            BukkitTask updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                    () -> updateModule.checkForUpdate(),
                    TickUtil.TICKS_PER_SECOND * 2, updateInterval);
            tasks.add(updateTask);
        }
        
        // Leaderboard refresh task
        if (configManager.isStatsEnabled() && configManager.isTrackPerPlayer()) {
            long leaderboardInterval = Math.max(20L,
                    configManager.getLeaderboardRefreshSeconds() * 20L);
            BukkitTask leaderboardTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                    () -> statsManager.refreshLeaderboards(),
                    leaderboardInterval, leaderboardInterval);
            tasks.add(leaderboardTask);
        }

        // Periodic stats save task (every 5 minutes)
        if (configManager.isStatsEnabled() && configManager.isStatsPersist()) {
            long saveInterval = TickUtil.TICKS_PER_5_MINUTES;
            BukkitTask statsSaveTask = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    () -> statsManager.save(),
                    saveInterval,
                    saveInterval);
            tasks.add(statsSaveTask);
        }

        // Vote timeout cleanup task (every 10 seconds)
        if (voteModule != null && configManager.getVoteTimeoutSeconds() > 0) {
            BukkitTask voteCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin,
                    voteModule::cleanupExpiredVotes,
                    TickUtil.TICKS_PER_SECOND * 10, TickUtil.TICKS_PER_SECOND * 10);
            tasks.add(voteCleanupTask);
        }

        // Empty session cleanup task (every 5 minutes)
        if (sleepManager != null) {
            long cleanupInterval = TickUtil.TICKS_PER_5_MINUTES;
            BukkitTask cleanupTask = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    () -> sleepManager.cleanupEmptySessions(),
                    cleanupInterval,
                    cleanupInterval);
            tasks.add(cleanupTask);
        }
    }
    
    public void stopAll() {
        for (BukkitTask task : tasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        tasks.clear();
    }
    
    public void restart() {
        stopAll();
        startAll();
    }
}
