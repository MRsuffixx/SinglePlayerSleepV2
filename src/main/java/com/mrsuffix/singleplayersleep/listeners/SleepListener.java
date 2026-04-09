package com.mrsuffix.singleplayersleep.listeners;

import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.modules.AfkModule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SleepListener implements Listener {
    
    private final SleepManager sleepManager;
    private final AfkModule afkModule;
    
    public SleepListener(SleepManager sleepManager, AfkModule afkModule) {
        this.sleepManager = sleepManager;
        this.afkModule = afkModule;
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.getPlayer() == null) {
            return;
        }
        sleepManager.onPlayerSleep(event);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBedLeave(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        World world = player.getWorld();
        if (world == null) {
            return;
        }
        sleepManager.getSessionIfExists(world)
                .ifPresent(session -> session.onPlayerWake(player));
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        afkModule.onPlayerLeave(player.getUniqueId());
        // Always notify session of player leave - player may be tracked in sleepingPlayers
        // even if isSleeping() returns false (e.g., kicked before sleep animation completed)
        World world = player.getWorld();
        if (world != null) {
            sleepManager.getSessionIfExists(world)
                    .ifPresent(session -> session.onPlayerLeave(player));
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        afkModule.onPlayerLeave(player.getUniqueId());
        // Always notify session of player leave - player may be tracked in sleepingPlayers
        World world = player.getWorld();
        if (world != null) {
            sleepManager.getSessionIfExists(world)
                    .ifPresent(session -> session.onPlayerLeave(player));
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player == null) {
            return;
        }
        // Always notify session of player leave
        World world = player.getWorld();
        if (world != null) {
            sleepManager.getSessionIfExists(world)
                    .ifPresent(session -> session.onPlayerLeave(player));
        }
        afkModule.onPlayerLeave(player.getUniqueId());
    }
}
