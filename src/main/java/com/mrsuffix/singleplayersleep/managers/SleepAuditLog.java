package com.mrsuffix.singleplayersleep.managers;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SleepAuditLog {

    public static class SleepAuditEntry {
        private final long timestamp;
        private final UUID playerUuid;
        private final String playerName;
        private final String worldName;
        private final EntryType type;
        private final Map<String, String> metadata;

        public enum EntryType {
            PLAYER_SLEPT,
            PLAYER_WOKE,
            NIGHT_SKIPPED,
            VOTE_CAST,
            VOTE_REMOVED,
            VOTES_CLEARED,
            COUNTDOWN_STARTED,
            COOLDOWN_TRIGGERED,
            AFK_STATUS_CHANGED
        }

        public SleepAuditEntry(UUID playerUuid, String playerName, String worldName,
                                EntryType type, Map<String, String> metadata) {
            this.timestamp = System.currentTimeMillis();
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.worldName = worldName;
            this.type = type;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        public long getTimestamp() {
            return timestamp;
        }

        public UUID getPlayerUuid() {
            return playerUuid;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getWorldName() {
            return worldName;
        }

        public EntryType getType() {
            return type;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public String toFormattedString() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(sdf.format(new Date(timestamp))).append("] ");
            sb.append("[").append(type.name()).append("] ");
            if (playerName != null) {
                sb.append(playerName).append(" (");
                if (playerUuid != null) {
                    sb.append(playerUuid);
                }
                sb.append(")");
            }
            if (worldName != null) {
                sb.append(" [World: ").append(worldName).append("]");
            }
            if (!metadata.isEmpty()) {
                sb.append(" {");
                metadata.forEach((k, v) -> sb.append(k).append("=").append(v).append(", "));
                sb.delete(sb.length() - 2, sb.length());
                sb.append("}");
            }
            return sb.toString();
        }
    }

    public static class WorldSkipHistory {
        private final List<SkipRecord> skips;
        private int totalSkips;

        public WorldSkipHistory() {
            this.skips = new CopyOnWriteArrayList<>();
            this.totalSkips = 0;
        }

        public void addSkip(long timestamp, int sleepingPlayers, int totalPlayers) {
            skips.add(new SkipRecord(timestamp, sleepingPlayers, totalPlayers));
            totalSkips++;
            long cutoff = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000);
            skips.removeIf(r -> r.timestamp < cutoff);
        }

        public int getTotalSkips() {
            return totalSkips;
        }

        public List<SkipRecord> getRecentSkips(int limit) {
            int size = Math.min(limit, skips.size());
            return new ArrayList<>(skips.subList(skips.size() - size, skips.size()));
        }

        public static class SkipRecord {
            public final long timestamp;
            public final int sleepingPlayers;
            public final int totalPlayers;

            public SkipRecord(long timestamp, int sleepingPlayers, int totalPlayers) {
                this.timestamp = timestamp;
                this.sleepingPlayers = sleepingPlayers;
                this.totalPlayers = totalPlayers;
            }
        }
    }

    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    private final List<SleepAuditEntry> inMemoryLog = new ArrayList<>();
    private final Map<String, WorldSkipHistory> worldHistory = new ConcurrentHashMap<>();
    private final Map<UUID, List<Long>> voteSpamTracker = new ConcurrentHashMap<>();
    private static final int MAX_VOTE_SPAM_ENTRIES = 20;
    private static final long VOTE_SPAM_WINDOW_MS = 60000;
    private File auditFile;
    private FileConfiguration auditConfig;
    private boolean dirty = false;

    public SleepAuditLog(SinglePlayerSleep plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void logSleepEvent(UUID playerUuid, String playerName, String worldName) {
        if (!configManager.isAuditLogEnabled()) {
            return;
        }
        SleepAuditEntry entry = new SleepAuditEntry(playerUuid, playerName, worldName,
                SleepAuditEntry.EntryType.PLAYER_SLEPT, null);
        addEntry(entry);
    }

    public void logWakeEvent(UUID playerUuid, String playerName, String worldName) {
        if (!configManager.isAuditLogEnabled()) {
            return;
        }
        SleepAuditEntry entry = new SleepAuditEntry(playerUuid, playerName, worldName,
                SleepAuditEntry.EntryType.PLAYER_WOKE, null);
        addEntry(entry);
    }

    public void logNightSkip(String worldName, int sleepingPlayers, int totalPlayers) {
        if (!configManager.isAuditLogEnabled()) {
            return;
        }
        Map<String, String> metadata = new HashMap<>();
        metadata.put("sleepingPlayers", String.valueOf(sleepingPlayers));
        metadata.put("totalPlayers", String.valueOf(totalPlayers));
        SleepAuditEntry entry = new SleepAuditEntry(null, null, worldName,
                SleepAuditEntry.EntryType.NIGHT_SKIPPED, metadata);
        addEntry(entry);

        WorldSkipHistory history = worldHistory.computeIfAbsent(worldName, k -> new WorldSkipHistory());
        history.addSkip(System.currentTimeMillis(), sleepingPlayers, totalPlayers);
        dirty = true;
    }

    public void logVoteCast(UUID playerUuid, String playerName, String worldName) {
        if (!configManager.isAuditLogEnabled()) {
            return;
        }

        if (isVoteSpamming(playerUuid)) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("reason", "vote_spam_detected");
            SleepAuditEntry entry = new SleepAuditEntry(playerUuid, playerName, worldName,
                    SleepAuditEntry.EntryType.VOTE_CAST, metadata);
            addEntry(entry);
            return;
        }

        recordVote(playerUuid);
        SleepAuditEntry entry = new SleepAuditEntry(playerUuid, playerName, worldName,
                SleepAuditEntry.EntryType.VOTE_CAST, null);
        addEntry(entry);
    }

    public void logVoteRemoved(UUID playerUuid, String playerName, String worldName) {
        if (!configManager.isAuditLogEnabled()) {
            return;
        }
        SleepAuditEntry entry = new SleepAuditEntry(playerUuid, playerName, worldName,
                SleepAuditEntry.EntryType.VOTE_REMOVED, null);
        addEntry(entry);
    }

    public void logVotesCleared(String worldName, int count) {
        if (!configManager.isAuditLogEnabled()) {
            return;
        }
        Map<String, String> metadata = new HashMap<>();
        metadata.put("clearedCount", String.valueOf(count));
        SleepAuditEntry entry = new SleepAuditEntry(null, null, worldName,
                SleepAuditEntry.EntryType.VOTES_CLEARED, metadata);
        addEntry(entry);
        dirty = true;
    }

    public void logAfkStatusChange(UUID playerUuid, String playerName, String worldName, boolean isAfk) {
        if (!configManager.isAuditLogEnabled()) {
            return;
        }
        Map<String, String> metadata = new HashMap<>();
        metadata.put("isAfk", String.valueOf(isAfk));
        SleepAuditEntry entry = new SleepAuditEntry(playerUuid, playerName, worldName,
                SleepAuditEntry.EntryType.AFK_STATUS_CHANGED, metadata);
        addEntry(entry);
    }

    private void addEntry(SleepAuditEntry entry) {
        inMemoryLog.add(entry);
        if (inMemoryLog.size() > configManager.getAuditLogMaxEntries()) {
            inMemoryLog.remove(0);
        }
        dirty = true;
    }

    private boolean isVoteSpamming(UUID uuid) {
        List<Long> votes = voteSpamTracker.get(uuid);
        if (votes == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long windowStart = now - VOTE_SPAM_WINDOW_MS;
        votes.removeIf(ts -> ts < windowStart);
        return votes.size() >= 3;
    }

    private void recordVote(UUID uuid) {
        List<Long> votes = voteSpamTracker.computeIfAbsent(uuid, k -> new ArrayList<>());
        votes.add(System.currentTimeMillis());
        if (votes.size() > MAX_VOTE_SPAM_ENTRIES) {
            votes.remove(0);
        }
    }

    public void save() {
        if (!configManager.isAuditLogEnabled()) {
            return;
        }
        loadFile();
        if (auditConfig == null || auditFile == null) {
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        List<Map<String, Object>> entries = new ArrayList<>();
        for (SleepAuditEntry entry : inMemoryLog) {
            Map<String, Object> map = new HashMap<>();
            map.put("timestamp", entry.getTimestamp());
            map.put("timestampFormatted", sdf.format(new Date(entry.getTimestamp())));
            if (entry.getPlayerUuid() != null) {
                map.put("playerUuid", entry.getPlayerUuid().toString());
            }
            map.put("playerName", entry.getPlayerName());
            map.put("worldName", entry.getWorldName());
            map.put("type", entry.getType().name());
            if (!entry.getMetadata().isEmpty()) {
                map.put("metadata", entry.getMetadata());
            }
            entries.add(map);
        }
        auditConfig.set("entries", entries);

        for (Map.Entry<String, WorldSkipHistory> worldEntry : worldHistory.entrySet()) {
            String path = "worldHistory." + worldEntry.getKey();
            WorldSkipHistory history = worldEntry.getValue();
            auditConfig.set(path + ".totalSkips", history.getTotalSkips());
            List<Map<String, Object>> skipRecords = new ArrayList<>();
            for (WorldSkipHistory.SkipRecord record : history.getRecentSkips(100)) {
                Map<String, Object> recordMap = new HashMap<>();
                recordMap.put("timestamp", record.timestamp);
                recordMap.put("sleepingPlayers", record.sleepingPlayers);
                recordMap.put("totalPlayers", record.totalPlayers);
                skipRecords.add(recordMap);
            }
            auditConfig.set(path + ".recentSkips", skipRecords);
        }

        try {
            auditConfig.save(auditFile);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save audit log: " + e.getMessage());
        }
    }

    public void load() {
        if (!configManager.isAuditLogEnabled()) {
            return;
        }
        loadFile();
        if (auditConfig == null || auditFile == null) {
            return;
        }

        List<?> entriesRaw = auditConfig.getList("entries");
        if (entriesRaw != null) {
            inMemoryLog.clear();
            for (Object raw : entriesRaw) {
                if (raw instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) raw;
                    try {
                        UUID uuid = map.get("playerUuid") != null ? UUID.fromString((String) map.get("playerUuid")) : null;
                        String playerName = (String) map.get("playerName");
                        String worldName = (String) map.get("worldName");
                        SleepAuditEntry.EntryType type = SleepAuditEntry.EntryType.valueOf((String) map.get("type"));
                        @SuppressWarnings("unchecked")
                        Map<String, String> metadata = map.get("metadata") != null
                                ? (Map<String, String>) map.get("metadata")
                                : new HashMap<>();
                        SleepAuditEntry entry = new SleepAuditEntry(uuid, playerName, worldName, type, metadata);
                        inMemoryLog.add(entry);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to parse audit entry: " + e.getMessage());
                    }
                }
            }
        }

        ConfigurationSection historySection = auditConfig.getConfigurationSection("worldHistory");
        if (historySection != null) {
            worldHistory.clear();
            for (String worldName : historySection.getKeys(false)) {
                ConfigurationSection worldSection = historySection.getConfigurationSection(worldName);
                if (worldSection != null) {
                    WorldSkipHistory history = new WorldSkipHistory();
                    List<?> skipRecordsRaw = worldSection.getList("recentSkips");
                    if (skipRecordsRaw != null) {
                        for (Object raw : skipRecordsRaw) {
                            if (raw instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> map = (Map<String, Object>) raw;
                                long timestamp = ((Number) map.get("timestamp")).longValue();
                                int sleeping = ((Number) map.get("sleepingPlayers")).intValue();
                                int total = ((Number) map.get("totalPlayers")).intValue();
                                history.addSkip(timestamp, sleeping, total);
                            }
                        }
                    }
                    worldHistory.put(worldName, history);
                }
            }
        }
    }

    private void loadFile() {
        if (auditFile == null) {
            auditFile = new File(plugin.getDataFolder(), "audit.yml");
        }
        if (!auditFile.exists()) {
            try {
                if (!auditFile.getParentFile().exists()) {
                    auditFile.getParentFile().mkdirs();
                }
                auditFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create audit.yml: " + e.getMessage());
                return;
            }
        }
        try {
            auditConfig = YamlConfiguration.loadConfiguration(auditFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load audit.yml: " + e.getMessage());
        }
    }

    public List<SleepAuditEntry> getRecentEntries(int limit) {
        int size = Math.min(limit, inMemoryLog.size());
        if (size == 0) {
            return List.of();
        }
        return new ArrayList<>(inMemoryLog.subList(inMemoryLog.size() - size, inMemoryLog.size()));
    }

    public List<SleepAuditEntry> getEntriesForPlayer(UUID uuid, int limit) {
        List<SleepAuditEntry> result = new ArrayList<>();
        for (int i = inMemoryLog.size() - 1; i >= 0 && result.size() < limit; i--) {
            SleepAuditEntry entry = inMemoryLog.get(i);
            if (uuid.equals(entry.getPlayerUuid())) {
                result.add(entry);
            }
        }
        return result;
    }

    public WorldSkipHistory getWorldHistory(String worldName) {
        return worldHistory.getOrDefault(worldName, new WorldSkipHistory());
    }

    public Map<String, WorldSkipHistory> getAllWorldHistory() {
        return Map.copyOf(worldHistory);
    }

    public void clearVoteSpamTracker(UUID uuid) {
        voteSpamTracker.remove(uuid);
    }

    public void clearAllData() {
        inMemoryLog.clear();
        worldHistory.clear();
        voteSpamTracker.clear();
    }
}