package com.mrsuffix.singleplayersleep.listeners;

import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PhantomListener implements Listener {
    
    private final ConfigManager configManager;
    private final boolean isPaperServer;
    
    public PhantomListener(ConfigManager configManager) {
        this.configManager = configManager;
        boolean paperDetected;
        try {
            Class.forName("com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent");
            paperDetected = true;
        } catch (ClassNotFoundException e) {
            paperDetected = false;
        }
        this.isPaperServer = paperDetected;
    }
    
    @EventHandler
    public void onPhantomSpawn(PhantomPreSpawnEvent event) {
        if (!isPaperServer || event == null) {
            return;
        }
    }
}
