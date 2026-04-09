package com.mrsuffix.singleplayersleep;

import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.StatsManager;
import com.mrsuffix.singleplayersleep.modules.AfkModule;
import com.mrsuffix.singleplayersleep.modules.UpdateModule;
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
    private final List<BukkitTask> tasks = new ArrayList<>();
    
    public TaskScheduler(SinglePlayerSleep plugin, ConfigManager configManager,
                         AfkModule afkModule, UpdateModule updateModule,
                         StatsManager statsManager, SleepManager sleepManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.afkModule = afkModule;
        this.updateModule = updateModule;
        this.statsManager = statsManager;
        this.sleepManager = sleepManager;
    }
    
    public void startAll() {
        stopAll();
        
        // AFK check task
        int afkInterval = Math.max(20, configManager.getAfkCheckIntervalTicks());
        BukkitTask afkTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> afkModule.scheduledCheck(null),
                afkInterval, afkInterval);
        tasks.add(afkTask);
        
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
            long saveInterval = 20L * 60L * 5L; // 5 minutes in ticks
            BukkitTask statsSaveTask = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    () -> statsManager.save(),
                    saveInterval,
                    saveInterval);
            tasks.add(statsSaveTask);
        }

        // Empty session cleanup task (every 5 minutes)
        if (sleepManager != null) {
            long cleanupInterval = 20L * 60L * 5L; // 5 minutes in ticks
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
