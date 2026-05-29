package com.mrsuffix.singleplayersleep.api.events;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class VoteCastEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final World world;
    private final int currentVotes;
    private final int requiredVotes;
    private final long timestamp;

    public VoteCastEvent(Player player, World world, int currentVotes, int requiredVotes) {
        this.player = player;
        this.world = world;
        this.currentVotes = currentVotes;
        this.requiredVotes = requiredVotes;
        this.timestamp = System.currentTimeMillis();
    }

    public Player getPlayer() {
        return player;
    }

    public World getWorld() {
        return world;
    }

    public int getCurrentVotes() {
        return currentVotes;
    }

    public int getRequiredVotes() {
        return requiredVotes;
    }

    public boolean isThresholdMet() {
        return currentVotes >= requiredVotes;
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