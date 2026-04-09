package com.mrsuffix.singleplayersleep;

import com.mrsuffix.singleplayersleep.core.CooldownManager;
import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.MessageUtil;
import com.mrsuffix.singleplayersleep.managers.StatsManager;
import com.mrsuffix.singleplayersleep.managers.WorldManager;
import com.mrsuffix.singleplayersleep.commands.SleepCommand;
import com.mrsuffix.singleplayersleep.commands.SpsCommand;
import com.mrsuffix.singleplayersleep.hooks.PlaceholderHook;
import com.mrsuffix.singleplayersleep.listeners.AfkListener;
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
    private StatsManager statsManager;
    private SleepManager sleepManager;
    private UpdateModule updateModule;
    private MessageUtil messageUtil;
    private TaskScheduler taskScheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        configManager = new ConfigManager(this);
        configManager.loadCache();
        
        // Create MessageUtil as injectable instance service
        messageUtil = new MessageUtil(configManager);
        
        getLogger().info("SinglePlayerSleep v" + getDescription().getVersion() + " by mrsuffix — enabling...");
        
        worldManager = new WorldManager(this, configManager);
        cooldownManager = new CooldownManager(configManager);
        
        effectsModule = new EffectsModule(this, configManager);
        phantomModule = new PhantomModule(configManager, messageUtil);
        afkModule = new AfkModule(configManager);
        voteModule = new VoteModule(configManager);
        countdownModule = new CountdownModule(this, configManager, effectsModule, messageUtil);
        statsManager = new StatsManager(this, configManager);
        statsManager.load();
        
        sleepManager = new SleepManager(this, configManager, cooldownManager,
                afkModule, effectsModule, phantomModule, countdownModule,
                statsManager, worldManager, voteModule, messageUtil);
        
        updateModule = new UpdateModule(this, configManager, messageUtil);

        HandlerList.unregisterAll(this);
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new SleepListener(sleepManager, afkModule), this);
        pluginManager.registerEvents(new AfkListener(afkModule, updateModule, sleepManager, configManager, this), this);
        pluginManager.registerEvents(new WorldListener(sleepManager, cooldownManager, voteModule), this);

        configureCommands();

        // Centralized task scheduling
        taskScheduler = new TaskScheduler(this, configManager, afkModule, updateModule, statsManager);
        taskScheduler.startAll();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this, configManager, sleepManager, cooldownManager,
                    afkModule, statsManager).register();
            getLogger().info("PlaceholderAPI hook registered.");
        } else {
            getLogger().info("PlaceholderAPI not found — hook not registered.");
        }

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
        if (statsManager != null) {
            statsManager.save();
        }

        getLogger().info("SinglePlayerSleep disabled. Goodbye!");
    }
    
    public void configureCommands() {
        // Sleep command
        PluginCommand sleepCmd = getCommand(configManager.getSleepCommandName());
        if (sleepCmd != null) {
            SleepCommand sleepExecutor = new SleepCommand(this, configManager, sleepManager, voteModule, worldManager, cooldownManager, messageUtil);
            sleepCmd.setExecutor(sleepExecutor);
            // Apply aliases from config
            List<String> aliases = configManager.getSleepAliases();
            if (aliases != null && !aliases.isEmpty()) {
                sleepCmd.setAliases(aliases);
            }
        } else {
            getLogger().warning("Could not register /sleep command. Check command name in config matches plugin.yml declaration.");
        }

        // SPS admin command
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
}
