package com.mrsuffix.singleplayersleep;

import com.mrsuffix.singleplayersleep.api.SleepApiManager;
import com.mrsuffix.singleplayersleep.core.CooldownManager;
import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.MessageUtil;
import com.mrsuffix.singleplayersleep.managers.SleepAuditLog;
import com.mrsuffix.singleplayersleep.managers.StatsManager;
import com.mrsuffix.singleplayersleep.managers.WorldManager;
import com.mrsuffix.singleplayersleep.commands.SleepCommand;
import com.mrsuffix.singleplayersleep.commands.SpsCommand;
import com.mrsuffix.singleplayersleep.hooks.PlaceholderHook;
import com.mrsuffix.singleplayersleep.listeners.AfkListener;
import com.mrsuffix.singleplayersleep.listeners.PhantomListener;
import com.mrsuffix.singleplayersleep.listeners.SleepListener;
import com.mrsuffix.singleplayersleep.listeners.WorldListener;
import com.mrsuffix.singleplayersleep.modules.*;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class SinglePlayerSleep extends JavaPlugin {

    private ConfigManager configManager;
    private WorldManager worldManager;
    private CooldownManager cooldownManager;
    private AfkModule afkModule;
    private EffectsModule effectsModule;
    private PhantomModule phantomModule;
    private VoteModule voteModule;
    private CountdownModule countdownModule;
    private BossBarModule bossBarModule;
    private StatsManager statsManager;
    private SleepManager sleepManager;
    private UpdateModule updateModule;
    private MessageUtil messageUtil;
    private TaskScheduler taskScheduler;
    private SleepAuditLog auditLog;
    private SleepApiManager apiManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        configManager.loadCache();

        messageUtil = new MessageUtil(configManager);

        getLogger().info("SinglePlayerSleep v" + getDescription().getVersion() + " by mrsuffix — enabling...");

        worldManager = new WorldManager(this, configManager);
        cooldownManager = new CooldownManager(configManager);

        effectsModule = new EffectsModule(this, configManager);
        phantomModule = new PhantomModule(configManager, messageUtil);
        afkModule = new AfkModule(this, configManager);
        voteModule = new VoteModule(configManager);
        bossBarModule = new BossBarModule(configManager, messageUtil);
        countdownModule = new CountdownModule(this, configManager, effectsModule, messageUtil);
        countdownModule.setBossBarModule(bossBarModule);
        statsManager = new StatsManager(this, configManager);
        statsManager.load();

        auditLog = new SleepAuditLog(this, configManager);
        auditLog.load();

        apiManager = new SleepApiManager(this);

        sleepManager = new SleepManager(this, configManager, cooldownManager,
                afkModule, effectsModule, phantomModule, countdownModule,
                statsManager, worldManager, voteModule, messageUtil, auditLog, apiManager);
        sleepManager.setBossBarModule(bossBarModule);

        updateModule = new UpdateModule(this, configManager, messageUtil);

        HandlerList.unregisterAll(this);
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new SleepListener(sleepManager, afkModule, auditLog), this);
        pluginManager.registerEvents(new AfkListener(afkModule, updateModule, sleepManager, configManager, this, auditLog), this);
        pluginManager.registerEvents(new WorldListener(sleepManager, cooldownManager, voteModule), this);
        if (configManager.isPhantomResetOnSkip()) {
            pluginManager.registerEvents(new PhantomListener(configManager), this);
        }

        configureCommands();

        taskScheduler = new TaskScheduler(this, configManager, afkModule, updateModule, statsManager, sleepManager, voteModule, auditLog);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PlaceholderHook placeholderHook = new PlaceholderHook(this, configManager, sleepManager, cooldownManager,
                    afkModule, statsManager);
            placeholderHook.register();
            taskScheduler.setPlaceholderHook(placeholderHook);
            getLogger().info("PlaceholderAPI hook registered.");
        } else {
            getLogger().info("PlaceholderAPI not found — hook not registered.");
        }

        taskScheduler.startAll();

        getLogger().info("SinglePlayerSleep enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (taskScheduler != null) {
            taskScheduler.stopAll();
        }
        if (sleepManager != null) {
            sleepManager.resetAll();
        }
        if (bossBarModule != null) {
            bossBarModule.hideAll();
        }
        if (statsManager != null) {
            statsManager.save();
        }
        if (auditLog != null) {
            auditLog.save();
        }
        if (apiManager != null) {
            apiManager.unregisterAll();
        }

        getLogger().info("SinglePlayerSleep disabled. Goodbye!");
    }

    public void configureCommands() {
        PluginCommand sleepCmd = getCommand(configManager.getSleepCommandName());
        if (sleepCmd != null) {
            SleepCommand sleepExecutor = new SleepCommand(this, configManager, sleepManager, voteModule, worldManager, cooldownManager, messageUtil, auditLog);
            sleepCmd.setExecutor(sleepExecutor);
            List<String> aliases = configManager.getSleepAliases();
            if (aliases != null && !aliases.isEmpty()) {
                sleepCmd.setAliases(aliases);
            }
        } else {
            getLogger().warning("Could not register /sleep command. Check command name in config matches plugin.yml declaration.");
        }

        PluginCommand spsCmd = getCommand("sps");
        if (spsCmd != null) {
            SpsCommand spsExecutor = new SpsCommand(this, configManager, sleepManager, cooldownManager,
                    voteModule, worldManager, statsManager, updateModule, afkModule, messageUtil, taskScheduler);
            spsCmd.setExecutor(spsExecutor);
            spsCmd.setTabCompleter(spsExecutor);
        }
    }

    public TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public SleepAuditLog getAuditLog() {
        return auditLog;
    }

    public SleepApiManager getApiManager() {
        return apiManager;
    }
}