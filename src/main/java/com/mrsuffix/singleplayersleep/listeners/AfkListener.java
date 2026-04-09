package com.mrsuffix.singleplayersleep.listeners;

import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.modules.AfkModule;
import com.mrsuffix.singleplayersleep.modules.UpdateModule;
import org.bukkit.Bukkit;
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
    private final ConfigManager configManager;
    private final com.mrsuffix.singleplayersleep.SinglePlayerSleep plugin;
    
    public AfkListener(AfkModule afkModule, UpdateModule updateModule, SleepManager sleepManager,
                        ConfigManager configManager, com.mrsuffix.singleplayersleep.SinglePlayerSleep plugin) {
        this.afkModule = afkModule;
        this.updateModule = updateModule;
        this.sleepManager = sleepManager;
        this.configManager = configManager;
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
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
        Player player = event.getPlayer();
        afkModule.recordActivity(player, AfkModule.ActivityType.INTERACT);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        // AsyncPlayerChatEvent is always async - dispatch to main thread
        Bukkit.getScheduler().runTask(plugin,
            () -> afkModule.recordActivity(player, AfkModule.ActivityType.CHAT));
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        afkModule.recordActivity(player, AfkModule.ActivityType.COMMAND);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            afkModule.recordActivity(player, AfkModule.ActivityType.INVENTORY);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            afkModule.recordActivity(player, AfkModule.ActivityType.INVENTORY);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        afkModule.onPlayerJoin(player);
        if (sleepManager != null && player.getWorld() != null) {
            sleepManager.getSessionIfExists(player.getWorld())
                    .ifPresent(session -> session.refreshRequirement());
        }
        // Respect notify-on-join config
        if (updateModule != null && updateModule.isUpdateAvailable() && configManager.isNotifyOnJoin()) {
            updateModule.notifyPlayer(player);
        }
    }
}
