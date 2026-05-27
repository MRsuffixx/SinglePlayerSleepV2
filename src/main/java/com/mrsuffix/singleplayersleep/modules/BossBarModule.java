package com.mrsuffix.singleplayersleep.modules;

import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarModule {

    private final ConfigManager configManager;
    private final MessageUtil messageUtil;
    private final Map<String, BossBar> activeBars = new ConcurrentHashMap<>();

    public BossBarModule(ConfigManager configManager, MessageUtil messageUtil) {
        this.configManager = configManager;
        this.messageUtil = messageUtil;
    }

    public void showCountdown(World world, int remaining, int total) {
        if (world == null || !configManager.isBossBarEnabled()) {
            return;
        }
        String worldName = world.getName();

        BossBar bar = activeBars.computeIfAbsent(worldName, k -> {
            BossBar newBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
            newBar.setVisible(true);
            return newBar;
        });

        if (bar.getPlayers().isEmpty()) {
            for (Player player : world.getPlayers()) {
                if (player != null) {
                    bar.addPlayer(player);
                }
            }
        } else {
            bar.getPlayers().removeIf(p -> !p.getWorld().equals(world));
            for (Player player : world.getPlayers()) {
                if (player != null && !bar.getPlayers().contains(player)) {
                    bar.addPlayer(player);
                }
            }
        }

        String title = messageUtil.formatRaw("countdown",
                Map.of("seconds", String.valueOf(remaining)));
        bar.setTitle(title);

        double progress = total > 0 ? (double) remaining / total : 0.0;
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

        if (progress <= 0.33) {
            bar.setColor(BarColor.RED);
        } else if (progress <= 0.66) {
            bar.setColor(BarColor.YELLOW);
        } else {
            bar.setColor(BarColor.BLUE);
        }
    }

    public void hideCountdown(World world) {
        if (world == null) {
            return;
        }
        BossBar bar = activeBars.remove(world.getName());
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
    }

    public void hideAll() {
        for (BossBar bar : activeBars.values()) {
            bar.removeAll();
            bar.setVisible(false);
        }
        activeBars.clear();
    }
}
