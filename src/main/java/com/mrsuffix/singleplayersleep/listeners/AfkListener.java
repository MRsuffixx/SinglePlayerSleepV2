package com.mrsuffix.singleplayersleep.listeners;

import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.modules.AfkModule;
import com.mrsuffix.singleplayersleep.modules.UpdateModule;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class AfkListener implements Listener {
    
    private final AfkModule afkModule;
    private final UpdateModule updateModule;
    private final SleepManager sleepManager;
    
    public AfkListener(AfkModule afkModule, UpdateModule updateModule, SleepManager sleepManager) {
        this.afkModule = afkModule;
        this.updateModule = updateModule;
        this.sleepManager = sleepManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event == null) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        afkModule.recordActivity(player, AfkModule.ActivityType.MOVE);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event == null) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        afkModule.recordActivity(player, AfkModule.ActivityType.INTERACT);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        afkModule.recordActivity(event.getPlayer(), AfkModule.ActivityType.CHAT);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        afkModule.recordActivity(event.getPlayer(), AfkModule.ActivityType.COMMAND);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        if (event.getPlayer() instanceof Player player) {
            afkModule.recordActivity(player, AfkModule.ActivityType.INVENTORY);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event == null || event.getWhoClicked() == null) {
            return;
        }
        if (event.getWhoClicked() instanceof Player player) {
            afkModule.recordActivity(player, AfkModule.ActivityType.INVENTORY);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (event == null) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        afkModule.onPlayerJoin(player);
        if (sleepManager != null && player.getWorld() != null) {
            sleepManager.getSessionIfExists(player.getWorld())
                    .ifPresent(session -> session.refreshRequirement());
        }
        if (updateModule != null && updateModule.isUpdateAvailable()) {
            updateModule.notifyPlayer(player);
        }
    }
}
