package com.mrsuffix.singleplayersleep.listeners;

import com.mrsuffix.singleplayersleep.core.CooldownManager;
import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.modules.VoteModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldListener implements Listener {
    
    private final SleepManager sleepManager;
    private final CooldownManager cooldownManager;
    private final VoteModule voteModule;
    
    public WorldListener(SleepManager sleepManager, CooldownManager cooldownManager, VoteModule voteModule) {
        this.sleepManager = sleepManager;
        this.cooldownManager = cooldownManager;
        this.voteModule = voteModule;
    }
    
    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        if (event == null || event.getWorld() == null) {
            return;
        }
        String worldName = event.getWorld().getName();
        // Clean up session for this world
        sleepManager.clearSession(event.getWorld());
        // Clear votes for this world
        if (voteModule != null) {
            voteModule.clearVotes(worldName);
        }
    }
}
