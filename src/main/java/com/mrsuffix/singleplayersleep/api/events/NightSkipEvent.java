package com.mrsuffix.singleplayersleep.api.events;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Set;
import java.util.UUID;

public class NightSkipEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final World world;
    private final Set<UUID> sleepingPlayers;
    private final int sleepingPlayerCount;
    private final int totalPlayerCount;
    private final long timestamp;

    public NightSkipEvent(World world, Set<UUID> sleepingPlayers, int sleepingPlayerCount, int totalPlayerCount) {
        this.world = world;
        this.sleepingPlayers = sleepingPlayers;
        this.sleepingPlayerCount = sleepingPlayerCount;
        this.totalPlayerCount = totalPlayerCount;
        this.timestamp = System.currentTimeMillis();
    }

    public World getWorld() {
        return world;
    }

    public Set<UUID> getSleepingPlayers() {
        return sleepingPlayers;
    }

    public int getSleepingPlayerCount() {
        return sleepingPlayerCount;
    }

    public int getTotalPlayerCount() {
        return totalPlayerCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}