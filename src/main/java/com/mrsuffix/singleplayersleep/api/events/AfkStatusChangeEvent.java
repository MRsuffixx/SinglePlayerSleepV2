package com.mrsuffix.singleplayersleep.api.events;

import com.mrsuffix.singleplayersleep.modules.AfkState;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AfkStatusChangeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final World world;
    private final AfkState previousState;
    private final AfkState newState;
    private final long timestamp;

    public AfkStatusChangeEvent(Player player, World world, AfkState previousState, AfkState newState) {
        this.player = player;
        this.world = world;
        this.previousState = previousState;
        this.newState = newState;
        this.timestamp = System.currentTimeMillis();
    }

    public Player getPlayer() {
        return player;
    }

    public World getWorld() {
        return world;
    }

    public AfkState getPreviousState() {
        return previousState;
    }

    public AfkState getNewState() {
        return newState;
    }

    public boolean isNowAfk() {
        return newState == AfkState.FULL_AFK || newState == AfkState.FARMING_DETECTED;
    }

    public boolean isNowSemiAfk() {
        return newState == AfkState.SEMI_AFK;
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