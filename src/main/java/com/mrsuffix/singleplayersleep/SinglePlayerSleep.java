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
import com.mrsuffix.singleplayersleep.listeners.PhantomListener;
import com.mrsuffix.singleplayersleep.listeners.SleepListener;
import com.mrsuffix.singleplayersleep.modules.*;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        configManager = new ConfigManager(this);
        configManager.loadCache();
        
        MessageUtil.init(configManager);
        getLogger().info("SinglePlayerSleep v" + getDescription().getVersion() + " by mrsuffix — enabling...");
        
        worldManager = new WorldManager(this, configManager);
        cooldownManager = new CooldownManager(configManager);
        
        effectsModule = new EffectsModule(this, configManager);
        phantomModule = new PhantomModule(configManager);
        afkModule = new AfkModule(configManager);
        voteModule = new VoteModule(configManager);
        countdownModule = new CountdownModule(this, configManager, effectsModule);
        statsManager = new StatsManager(this, configManager);
        statsManager.load();
        
        sleepManager = new SleepManager(this, configManager, cooldownManager,
                afkModule, effectsModule, phantomModule, countdownModule,
                statsManager, worldManager);
        
        updateModule = new UpdateModule(this, configManager);

        HandlerList.unregisterAll(this);
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new SleepListener(sleepManager, afkModule), this);
        pluginManager.registerEvents(new AfkListener(afkModule, updateModule), this);
        try {
            Class.forName("com.destroystokyo.paper.event.entity.PhantomSpawnEvent");
            pluginManager.registerEvents(new PhantomListener(configManager), this);
        } catch (ClassNotFoundException e) {
            getLogger().info("Paper not detected — PhantomListener not registered.");
        }

        PluginCommand sleepCmd = getCommand(configManager.getSleepCommandName());
        if (sleepCmd != null) {
            SleepCommand sleepExecutor = new SleepCommand(this, configManager, sleepManager, voteModule, worldManager);
            sleepCmd.setExecutor(sleepExecutor);
        } else {
            getLogger().warning("Could not register /sleep command. Check command name in config matches plugin.yml declaration.");
        }

        PluginCommand spsCmd = getCommand("sps");
        if (spsCmd != null) {
            SpsCommand spsExecutor = new SpsCommand(this, configManager, sleepManager, cooldownManager,
                    voteModule, worldManager, statsManager, updateModule, afkModule);
            spsCmd.setExecutor(spsExecutor);
            spsCmd.setTabCompleter(spsExecutor);
        }

        Bukkit.getScheduler().runTaskTimer(this,
                () -> afkModule.scheduledCheck(sleepManager),
                configManager.getAfkCheckIntervalTicks(),
                configManager.getAfkCheckIntervalTicks());

        updateModule.scheduleUpdateCheck();

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
        if (sleepManager != null) {
            sleepManager.resetAll();
        }
        if (statsManager != null) {
            statsManager.save();
        }

        getLogger().info("SinglePlayerSleep disabled. Goodbye!");
    }
}
