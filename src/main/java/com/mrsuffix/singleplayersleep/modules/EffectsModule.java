package com.mrsuffix.singleplayersleep.modules;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class EffectsModule {
    
    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    
    public EffectsModule(SinglePlayerSleep plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    public void playSleepStart(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!configManager.isSoundsEnabled()) {
            return;
        }
        player.playSound(player.getLocation(), configManager.getSleepStartSound(), 1.0f, 1.0f);
    }
    
    public void playNightSkip(World world) {
        if (world == null) {
            return;
        }
        if (!configManager.isSoundsEnabled()) {
            return;
        }
        for (Player player : world.getPlayers()) {
            if (player != null) {
                Location loc = player.getLocation();
                world.playSound(loc, configManager.getNightSkipSound(), 1.0f, 1.0f);
            }
        }
    }
    
    public void spawnSleepParticles(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!configManager.isParticlesEnabled()) {
            return;
        }
        World world = player.getWorld();
        if (world == null) {
            return;
        }
        int count = calculateParticleCount(world);
        world.spawnParticle(
                configManager.getParticleType(),
                player.getLocation().add(0, 2, 0),
                count, 0.3, 0.2, 0.3, 0
        );
    }
    
    private int calculateParticleCount(World world) {
        if (!configManager.isSmartScale()) {
            return 10;
        }
        int online = world == null ? 1 : world.getPlayers().size();
        if (online <= 1) {
            return 10;
        }
        if (online <= 4) {
            return 7;
        }
        if (online <= 9) {
            return 5;
        }
        if (online <= 19) {
            return 3;
        }
        return 1;
    }
    
    public void playCountdownTick(World world) {
        if (world == null) {
            return;
        }
        if (!configManager.isSoundsEnabled()) {
            return;
        }
        for (Player player : world.getPlayers()) {
            if (player != null) {
                Location loc = player.getLocation();
                world.playSound(loc, configManager.getCountdownTickSound(), 0.5f, 1.2f);
            }
        }
    }
}
