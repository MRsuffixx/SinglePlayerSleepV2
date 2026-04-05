package com.mrsuffix.singleplayersleep.core;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.StatsManager;
import com.mrsuffix.singleplayersleep.managers.WorldManager;
import com.mrsuffix.singleplayersleep.modules.*;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerBedEnterEvent;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SleepManager {
    
    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    private final CooldownManager cooldownManager;
    private final AfkModule afkModule;
    private final EffectsModule effectsModule;
    private final PhantomModule phantomModule;
    private final CountdownModule countdownModule;
    private final StatsManager statsManager;
    private final WorldManager worldManager;
    
    private final ConcurrentHashMap<String, SleepSession> sessions = new ConcurrentHashMap<>();
    
    public SleepManager(SinglePlayerSleep plugin, ConfigManager configManager,
                        CooldownManager cooldownManager, AfkModule afkModule,
                        EffectsModule effectsModule, PhantomModule phantomModule,
                        CountdownModule countdownModule, StatsManager statsManager,
                        WorldManager worldManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cooldownManager = cooldownManager;
        this.afkModule = afkModule;
        this.effectsModule = effectsModule;
        this.phantomModule = phantomModule;
        this.countdownModule = countdownModule;
        this.statsManager = statsManager;
        this.worldManager = worldManager;
    }
    
    public SleepSession getSession(World world) {
        if (world == null) {
            return null;
        }
        
        return sessions.computeIfAbsent(world.getName(), k -> 
            new SleepSession(plugin, world, configManager, cooldownManager,
                           afkModule, effectsModule, phantomModule,
                           countdownModule, statsManager)
        );
    }
    
    public Optional<SleepSession> getSessionIfExists(World world) {
        if (world == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(world.getName()));
    }
    
    public boolean isWorldEnabled(World world) {
        return worldManager.isEnabled(world);
    }
    
    public void resetAll() {
        sessions.values().forEach(SleepSession::reset);
        sessions.clear();
    }
    
    public void onPlayerSleep(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        
        World world = player.getWorld();
        if (world == null) {
            return;
        }
        
        if (!isWorldEnabled(world)) {
            return;
        }
        
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        
        if (world.getTime() < 12541) {
            return;
        }
        
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        
        SleepSession session = getSession(world);
        if (session != null) {
            session.onPlayerSleep(player);
        }
    }
}
