package com.mrsuffix.singleplayersleep.managers;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.util.SafeRunner;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StatsManager {

    public record LeaderboardEntry(UUID uuid, String name, int value) {
    }

    private record LeaderboardSnapshot(List<LeaderboardEntry> sleepers, List<LeaderboardEntry> contributors, long timestamp) {
    }
    
    public static class PlayerStats {
        public String name;
        public int timesSlept;
        public int nightsContributedTo;
        public long lastSeen;

        public PlayerStats(String name) {
            this.name = name;
            this.lastSeen = System.currentTimeMillis();
        }

        public PlayerStats(String name, int timesSlept, int nightsContributedTo, long lastSeen) {
            this.name = name;
            this.timesSlept = timesSlept;
            this.nightsContributedTo = nightsContributedTo;
            this.lastSeen = lastSeen;
        }
    }
    
    public static class GlobalStats {
        public final AtomicInteger totalNightsSkipped = new AtomicInteger(0);
        public final AtomicInteger totalSleepEvents = new AtomicInteger(0);
        public final AtomicLong lastSkipTimestamp = new AtomicLong(0L);

        public GlobalStats() {}

        public GlobalStats(int nights, int events, long timestamp) {
            totalNightsSkipped.set(nights);
            totalSleepEvents.set(events);
            lastSkipTimestamp.set(timestamp);
        }
    }
    
    private static final PlayerStats UNKNOWN = new PlayerStats("Unknown");

    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    private final GlobalStats globalStats = new GlobalStats();
    private final Map<UUID, PlayerStats> playerData = new ConcurrentHashMap<>();
    private volatile LeaderboardSnapshot leaderboardSnapshot = new LeaderboardSnapshot(List.of(), List.of(), 0L);
    private volatile boolean dirty = false;
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
        globalStats.totalSleepEvents.incrementAndGet();
        if (configManager.isTrackPerPlayer()) {
            PlayerStats ps = playerData.computeIfAbsent(player.getUniqueId(),
                    k -> new PlayerStats(player.getName()));
            ps.name = player.getName();
            ps.timesSlept++;
            dirty = true;
        }
    }
    
    public void recordNightSkip(World world, Set<UUID> sleepers) {
        if (!configManager.isStatsEnabled()) {
            return;
        }
        if (world == null) {
            return;
        }
        globalStats.totalNightsSkipped.incrementAndGet();
        globalStats.lastSkipTimestamp.set(System.currentTimeMillis());
        if (!configManager.isTrackPerPlayer() || sleepers == null) {
            return;
        }
        for (UUID uuid : sleepers) {
            if (uuid == null) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            String name;
            if (player != null) {
                name = player.getName();
            } else {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
                name = offline.getName() != null ? offline.getName() : uuid.toString();
            }
            PlayerStats ps = playerData.computeIfAbsent(uuid,
                    k -> new PlayerStats(name));
            ps.nightsContributedTo++;
        }
        dirty = true;
    }
    
    public PlayerStats getPlayerStats(UUID uuid) {
        PlayerStats original = playerData.get(uuid);
        if (original == null) {
            return UNKNOWN;
        }
        return new PlayerStats(original.name, original.timesSlept, original.nightsContributedTo, original.lastSeen);
    }

    public GlobalStats getGlobalStats() {
        return new GlobalStats(
            globalStats.totalNightsSkipped.get(),
            globalStats.totalSleepEvents.get(),
            globalStats.lastSkipTimestamp.get()
        );
    }

    public List<LeaderboardEntry> getTopSleepers(int limit) {
        return limitList(leaderboardSnapshot.sleepers(), limit);
    }

    public List<LeaderboardEntry> getTopContributors(int limit) {
        return limitList(leaderboardSnapshot.contributors(), limit);
    }

    public long getLastLeaderboardRefresh() {
        return leaderboardSnapshot.timestamp();
    }

    public void scheduleLeaderboardRefresh() {
        // Deprecated: scheduling now handled by TaskScheduler
        refreshLeaderboards();
    }

    public void refreshLeaderboards() {
        if (!configManager.isStatsEnabled() || !configManager.isTrackPerPlayer()) {
            leaderboardSnapshot = new LeaderboardSnapshot(List.of(), List.of(), 0L);
            return;
        }
        if (!dirty && leaderboardSnapshot.timestamp() > 0) {
            return;
        }
        dirty = false;
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
                // Atomic update of all three fields via single snapshot object
                leaderboardSnapshot = new LeaderboardSnapshot(
                        List.copyOf(sleepersSnapshot),
                        List.copyOf(contributorsSnapshot),
                        System.currentTimeMillis()
                );
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
        statsConfig.set("global.total-nights-skipped", globalStats.totalNightsSkipped.get());
        statsConfig.set("global.total-sleep-events", globalStats.totalSleepEvents.get());
        statsConfig.set("global.last-skip-timestamp", globalStats.lastSkipTimestamp.get());
        for (Map.Entry<UUID, PlayerStats> entry : playerData.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerStats ps = entry.getValue();
            statsConfig.set(path + ".name", ps.name);
            statsConfig.set(path + ".times-slept", ps.timesSlept);
            statsConfig.set(path + ".nights-contributed", ps.nightsContributedTo);
            statsConfig.set(path + ".last-seen", ps.lastSeen);
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
            if (statsConfig == null) {
                statsConfig = new YamlConfiguration();
            }
            globalStats.totalNightsSkipped.set(statsConfig.getInt("global.total-nights-skipped", 0));
            globalStats.totalSleepEvents.set(statsConfig.getInt("global.total-sleep-events", 0));
            globalStats.lastSkipTimestamp.set(statsConfig.getLong("global.last-skip-timestamp", 0L));
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
                        ps.lastSeen = statsConfig.getLong(path + ".last-seen", 0L);
                        playerData.put(uuid, ps);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in stats.yml: " + uuidStr);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load stats.yml: " + e.getMessage());
        }
        cleanupOldEntries(configManager.getStatsCleanupDays());
        refreshLeaderboards();
    }
    
    private void saveConfig() throws IOException {
        statsConfig.save(statsFile);
    }

    private void cleanupOldEntries(int maxDays) {
        if (maxDays <= 0) return;
        long cutoff = System.currentTimeMillis() - (maxDays * 86400_000L);
        playerData.entrySet().removeIf(e -> e.getValue().lastSeen < cutoff);
    }
    
    public void reload() {
        playerData.clear();
        globalStats.totalNightsSkipped.set(0);
        globalStats.totalSleepEvents.set(0);
        globalStats.lastSkipTimestamp.set(0L);
        leaderboardSnapshot = new LeaderboardSnapshot(List.of(), List.of(), 0L);
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
