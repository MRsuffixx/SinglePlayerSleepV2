package com.mrsuffix.singleplayersleep.api.events;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerWakeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final World world;
    private final long timestamp;

    public PlayerWakeEvent(Player player, World world) {
        this.player = player;
        this.world = world;
        this.timestamp = System.currentTimeMillis();
    }

    public Player getPlayer() {
        return player;
    }

    public World getWorld() {
        return world;
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