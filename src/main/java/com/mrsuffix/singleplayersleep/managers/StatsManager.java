package com.mrsuffix.singleplayersleep.managers;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StatsManager {
    
    public static class PlayerStats {
        public String name;
        public int timesSlept;
        public int nightsContributedTo;
        
        public PlayerStats(String name) {
            this.name = name;
        }
    }
    
    public static class GlobalStats {
        public int totalNightsSkipped;
        public int totalSleepEvents;
        public long lastSkipTimestamp;
    }
    
    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    private final GlobalStats globalStats = new GlobalStats();
    private final Map<UUID, PlayerStats> playerData = new HashMap<>();
    private File statsFile;
    private FileConfiguration statsConfig;
    
    public StatsManager(SinglePlayerSleep plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    public void recordSleepEvent(Player player) {
        if (!configManager.isStatsEnabled()) {
            return;
        }
        if (player == null) {
            return;
        }
        globalStats.totalSleepEvents++;
        if (configManager.isTrackPerPlayer()) {
            PlayerStats ps = playerData.computeIfAbsent(player.getUniqueId(),
                    k -> new PlayerStats(player.getName()));
            ps.name = player.getName();
            ps.timesSlept++;
        }
    }
    
    public void recordNightSkip(World world, Set<UUID> sleepers) {
        if (!configManager.isStatsEnabled()) {
            return;
        }
        if (world == null) {
            return;
        }
        globalStats.totalNightsSkipped++;
        globalStats.lastSkipTimestamp = System.currentTimeMillis();
        if (!configManager.isTrackPerPlayer() || sleepers == null) {
            return;
        }
        for (UUID uuid : sleepers) {
            if (uuid == null) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            PlayerStats ps = playerData.computeIfAbsent(uuid,
                    k -> new PlayerStats(player != null ? player.getName() : uuid.toString()));
            ps.nightsContributedTo++;
        }
    }
    
    public PlayerStats getPlayerStats(UUID uuid) {
        return playerData.getOrDefault(uuid, new PlayerStats("Unknown"));
    }
    
    public GlobalStats getGlobalStats() {
        return globalStats;
    }
    
    public void save() {
        if (!configManager.isStatsPersist()) {
            return;
        }
        if (statsConfig == null || statsFile == null) {
            load();
        }
        if (statsConfig == null || statsFile == null) {
            return;
        }
        statsConfig.set("global.total-nights-skipped", globalStats.totalNightsSkipped);
        statsConfig.set("global.total-sleep-events", globalStats.totalSleepEvents);
        statsConfig.set("global.last-skip-timestamp", globalStats.lastSkipTimestamp);
        for (Map.Entry<UUID, PlayerStats> entry : playerData.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerStats ps = entry.getValue();
            statsConfig.set(path + ".name", ps.name);
            statsConfig.set(path + ".times-slept", ps.timesSlept);
            statsConfig.set(path + ".nights-contributed", ps.nightsContributedTo);
        }
        try {
            saveConfig();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save stats: " + e.getMessage());
        }
    }
    
    public void load() {
        if (!configManager.isStatsPersist()) {
            return;
        }
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try {
                if (!statsFile.getParentFile().exists()) {
                    statsFile.getParentFile().mkdirs();
                }
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create stats.yml: " + e.getMessage());
            }
        }
        try {
            statsConfig = YamlConfiguration.loadConfiguration(statsFile);
            globalStats.totalNightsSkipped = statsConfig.getInt("global.total-nights-skipped", 0);
            globalStats.totalSleepEvents = statsConfig.getInt("global.total-sleep-events", 0);
            globalStats.lastSkipTimestamp = statsConfig.getLong("global.last-skip-timestamp", 0L);
            ConfigurationSection players = statsConfig.getConfigurationSection("players");
            if (players != null) {
                for (String uuidStr : players.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        String path = "players." + uuidStr;
                        PlayerStats ps = new PlayerStats(
                                statsConfig.getString(path + ".name", "Unknown"));
                        ps.timesSlept = statsConfig.getInt(path + ".times-slept", 0);
                        ps.nightsContributedTo = statsConfig.getInt(path + ".nights-contributed", 0);
                        playerData.put(uuid, ps);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in stats.yml: " + uuidStr);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load stats.yml: " + e.getMessage());
        }
    }
    
    private void saveConfig() throws IOException {
        statsConfig.save(statsFile);
    }
    
    public void reload() {
        playerData.clear();
        globalStats.totalNightsSkipped = 0;
        globalStats.totalSleepEvents = 0;
        globalStats.lastSkipTimestamp = 0;
        load();
    }
}
