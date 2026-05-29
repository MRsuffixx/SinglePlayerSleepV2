package com.mrsuffix.singleplayersleep.api;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.api.events.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.RegisteredListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class SleepApiManager {

    private final SinglePlayerSleep plugin;
    private final List<SleepApi> registeredApis = new CopyOnWriteArrayList<>();
    private boolean isEnabled = true;

    public SleepApiManager(SinglePlayerSleep plugin) {
        this.plugin = plugin;
    }

    public void registerApi(SleepApi api) {
        if (api != null && !registeredApis.contains(api)) {
            registeredApis.add(api);
            plugin.getLogger().info("Registered SleepApi: " + api.getClass().getName());
        }
    }

    public void unregisterApi(SleepApi api) {
        if (api != null) {
            registeredApis.remove(api);
            plugin.getLogger().info("Unregistered SleepApi: " + api.getClass().getName());
        }
    }

    public void unregisterAll() {
        registeredApis.clear();
    }

    public List<SleepApi> getRegisteredApis() {
        return List.copyOf(registeredApis);
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public void callNightSkipEvent(World world, Set<UUID> sleepingPlayers, int sleepingCount, int totalCount) {
        if (!isEnabled || registeredApis.isEmpty()) {
            return;
        }
        NightSkipEvent event = new NightSkipEvent(world, sleepingPlayers, sleepingCount, totalCount);
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (SleepApi api : registeredApis) {
                try {
                    api.onNightSkip(event);
                } catch (Exception e) {
                    plugin.getLogger().warning("SleepApi error in onNightSkip: " + e.getMessage());
                }
            }
        });
    }

    public void callPlayerSleepEvent(Player player, World world) {
        if (!isEnabled || registeredApis.isEmpty()) {
            return;
        }
        PlayerSleepEvent event = new PlayerSleepEvent(player, world);
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (SleepApi api : registeredApis) {
                try {
                    api.onPlayerSleep(event);
                } catch (Exception e) {
                    plugin.getLogger().warning("SleepApi error in onPlayerSleep: " + e.getMessage());
                }
            }
        });
    }

    public void callPlayerWakeEvent(Player player, World world) {
        if (!isEnabled || registeredApis.isEmpty()) {
            return;
        }
        PlayerWakeEvent event = new PlayerWakeEvent(player, world);
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (SleepApi api : registeredApis) {
                try {
                    api.onPlayerWake(event);
                } catch (Exception e) {
                    plugin.getLogger().warning("SleepApi error in onPlayerWake: " + e.getMessage());
                }
            }
        });
    }

    public void callVoteCastEvent(Player player, World world, int currentVotes, int requiredVotes) {
        if (!isEnabled || registeredApis.isEmpty()) {
            return;
        }
        VoteCastEvent event = new VoteCastEvent(player, world, currentVotes, requiredVotes);
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (SleepApi api : registeredApis) {
                try {
                    api.onVoteCast(event);
                } catch (Exception e) {
                    plugin.getLogger().warning("SleepApi error in onVoteCast: " + e.getMessage());
                }
            }
        });
    }

    public void callAfkStatusChangeEvent(Player player, World world,
                                          com.mrsuffix.singleplayersleep.modules.AfkState previousState,
                                          com.mrsuffix.singleplayersleep.modules.AfkState newState) {
        if (!isEnabled || registeredApis.isEmpty()) {
            return;
        }
        AfkStatusChangeEvent event = new AfkStatusChangeEvent(player, world, previousState, newState);
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (SleepApi api : registeredApis) {
                try {
                    api.onAfkStatusChange(event);
                } catch (Exception e) {
                    plugin.getLogger().warning("SleepApi error in onAfkStatusChange: " + e.getMessage());
                }
            }
        });
    }
}