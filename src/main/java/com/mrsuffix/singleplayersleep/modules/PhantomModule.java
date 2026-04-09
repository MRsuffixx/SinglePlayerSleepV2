package com.mrsuffix.singleplayersleep.modules;

import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.MessageUtil;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;

public class PhantomModule {
    
    private final ConfigManager configManager;
    private final com.mrsuffix.singleplayersleep.managers.MessageUtil messageUtil;
    
    public PhantomModule(ConfigManager configManager, com.mrsuffix.singleplayersleep.managers.MessageUtil messageUtil) {
        this.configManager = configManager;
        this.messageUtil = messageUtil;
    }
    
    public void resetPhantomTimers(World world) {
        if (world == null) {
            return;
        }
        if (!configManager.isPhantomResetOnSkip()) {
            return;
        }
        for (Player player : world.getPlayers()) {
            if (player == null) {
                continue;
            }
            try {
                player.setStatistic(Statistic.TIME_SINCE_REST, 0);
            } catch (Exception e) {
                player.getServer().getLogger().warning("Could not reset phantom timer for " + player.getName());
            }
        }
        messageUtil.broadcastWorld(world, "phantom-reset", Map.of());
    }
}
