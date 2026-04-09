package com.mrsuffix.singleplayersleep.modules;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.MessageUtil;
import com.mrsuffix.singleplayersleep.util.TickUtil;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

public class CountdownModule {
    
    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    private final EffectsModule effectsModule;
    private final com.mrsuffix.singleplayersleep.managers.MessageUtil messageUtil;
    
    public CountdownModule(SinglePlayerSleep plugin, ConfigManager configManager, EffectsModule effectsModule, com.mrsuffix.singleplayersleep.managers.MessageUtil messageUtil) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.effectsModule = effectsModule;
        this.messageUtil = messageUtil;
    }
    
    public BukkitTask start(World world, int durationSeconds, Runnable onFinish) {
        if (world == null || onFinish == null) {
            return null;
        }
        
        return new BukkitRunnable() {
            private int remaining = durationSeconds;
            
            @Override
            public void run() {
                if (remaining <= 0) {
                    cancel();
                    onFinish.run();
                    return;
                }
                
                String text = messageUtil.formatRaw("countdown",
                        Map.of("seconds", String.valueOf(remaining)));
                
                if (configManager.isCountdownShowActionBar()) {
                    for (Player player : world.getPlayers()) {
                        messageUtil.sendActionBar(player, text);
                    }
                }
                
                if (configManager.isCountdownShowChat()) {
                    messageUtil.broadcastWorldRaw(world, text);
                }
                
                if (configManager.isCountdownSoundOnTick() && effectsModule != null) {
                    effectsModule.playCountdownTick(world);
                }
                
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, TickUtil.TICKS_PER_SECOND);
    }
}
