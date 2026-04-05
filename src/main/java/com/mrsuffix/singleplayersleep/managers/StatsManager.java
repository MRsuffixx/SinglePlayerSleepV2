package com.mrsuffix.singleplayersleep.managers;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.util.SafeRunner;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StatsManager {

    public record LeaderboardEntry(UUID uuid, String name, int value) {
    }
    
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
    private volatile List<LeaderboardEntry> topSleepersCache = List.of();
    private volatile List<LeaderboardEntry> topContributorsCache = List.of();
    private volatile long lastLeaderboardRefresh = 0L;
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

    public List<LeaderboardEntry> getTopSleepers(int limit) {
        return limitList(topSleepersCache, limit);
    }

    public List<LeaderboardEntry> getTopContributors(int limit) {
        return limitList(topContributorsCache, limit);
    }

    public long getLastLeaderboardRefresh() {
        return lastLeaderboardRefresh;
    }

    public void scheduleLeaderboardRefresh() {
        if (!configManager.isStatsEnabled() || !configManager.isTrackPerPlayer()) {
            return;
        }
        long intervalTicks = Math.max(20L, configManager.getLeaderboardRefreshSeconds() * 20L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::refreshLeaderboards, intervalTicks, intervalTicks);
        refreshLeaderboards();
    }

    public void refreshLeaderboards() {
        if (!configManager.isStatsEnabled() || !configManager.isTrackPerPlayer()) {
            topSleepersCache = List.of();
            topContributorsCache = List.of();
            return;
        }
        SafeRunner.runSync(plugin, () -> {
            List<LeaderboardEntry> sleepersSnapshot = new ArrayList<>(playerData.size());
            List<LeaderboardEntry> contributorsSnapshot = new ArrayList<>(playerData.size());
            for (Map.Entry<UUID, PlayerStats> entry : playerData.entrySet()) {
                PlayerStats stats = entry.getValue();
                String name = stats == null || stats.name == null ? entry.getKey().toString() : stats.name;
                int timesSlept = stats == null ? 0 : stats.timesSlept;
                int contributed = stats == null ? 0 : stats.nightsContributedTo;
                sleepersSnapshot.add(new LeaderboardEntry(entry.getKey(), name, timesSlept));
                contributorsSnapshot.add(new LeaderboardEntry(entry.getKey(), name, contributed));
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                Comparator<LeaderboardEntry> comparator = Comparator
                        .comparingInt(LeaderboardEntry::value)
                        .reversed()
                        .thenComparing(LeaderboardEntry::name, String.CASE_INSENSITIVE_ORDER);
                sleepersSnapshot.sort(comparator);
                contributorsSnapshot.sort(comparator);
                topSleepersCache = List.copyOf(sleepersSnapshot);
                topContributorsCache = List.copyOf(contributorsSnapshot);
                lastLeaderboardRefresh = System.currentTimeMillis();
            });
        });
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
        refreshLeaderboards();
    }
    
    private void saveConfig() throws IOException {
        statsConfig.save(statsFile);
    }
    
    public void reload() {
        playerData.clear();
        globalStats.totalNightsSkipped = 0;
        globalStats.totalSleepEvents = 0;
        globalStats.lastSkipTimestamp = 0;
        topSleepersCache = List.of();
        topContributorsCache = List.of();
        lastLeaderboardRefresh = 0L;
        load();
    }

    private List<LeaderboardEntry> limitList(List<LeaderboardEntry> source, int limit) {
        if (source == null || source.isEmpty() || limit <= 0) {
            return List.of();
        }
        int size = Math.min(limit, source.size());
        return List.copyOf(source.subList(0, size));
    }
}
