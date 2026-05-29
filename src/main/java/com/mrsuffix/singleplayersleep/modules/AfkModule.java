package com.mrsuffix.singleplayersleep.modules;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class AfkModule {

    public enum ActivityType {
        MOVE,
        CHAT,
        INTERACT,
        COMMAND,
        INVENTORY,
        COMBAT,
        BLOCK_INTERACT,
        MANUAL
    }

    public static class ActivityMetrics {
        private long lastActivity;
        private long lastMove;
        private long lastChat;
        private long lastInteract;
        private long lastCommand;
        private long lastInventory;
        private long lastCombat;
        private long lastBlockInteract;
        private boolean forcedAfk;
        private String listNameBeforeAfk;

        private final Deque<Long> activityTimestamps = new ConcurrentLinkedDeque<>();
        private final Deque<Long> movementTimestamps = new ConcurrentLinkedDeque<>();
        private final Deque<Long> blockInteractTimestamps = new ConcurrentLinkedDeque<>();

        private static final int PATTERN_WINDOW_SECONDS = 60;
        private static final int MIN_ACTIVITIES_FOR_PATTERN = 10;
        private static final double FARMING_MOVEMENT_THRESHOLD = 0.15;

        private ActivityMetrics(long now) {
            this.lastActivity = now;
        }

        private void update(ActivityType type, long now) {
            lastActivity = now;
            activityTimestamps.addLast(now);
            cleanOldTimestamps(activityTimestamps, now);

            switch (type) {
                case MOVE -> {
                    lastMove = now;
                    movementTimestamps.addLast(now);
                    cleanOldTimestamps(movementTimestamps, now);
                }
                case CHAT -> lastChat = now;
                case INTERACT -> lastInteract = now;
                case COMMAND -> lastCommand = now;
                case INVENTORY -> lastInventory = now;
                case COMBAT -> lastCombat = now;
                case BLOCK_INTERACT -> {
                    lastBlockInteract = now;
                    blockInteractTimestamps.addLast(now);
                    cleanOldTimestamps(blockInteractTimestamps, now);
                }
                case MANUAL -> {}
            }
        }

        private void cleanOldTimestamps(Deque<Long> timestamps, long now) {
            long cutoff = now - (PATTERN_WINDOW_SECONDS * 1000L);
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }
        }

        public AfkState calculateState(long timeoutMs, long semiAfkMs, boolean farmingPreventionEnabled) {
            long now = System.currentTimeMillis();

            if (forcedAfk) {
                return AfkState.FULL_AFK;
            }

            long inactiveTime = now - lastActivity;

            if (farmingPreventionEnabled && detectFarmingBehavior(now)) {
                return AfkState.FARMING_DETECTED;
            }

            if (inactiveTime > timeoutMs) {
                return AfkState.FULL_AFK;
            }

            if (inactiveTime > semiAfkMs) {
                return AfkState.SEMI_AFK;
            }

            return AfkState.ACTIVE;
        }

        private boolean detectFarmingBehavior(long now) {
            if (movementTimestamps.size() < MIN_ACTIVITIES_FOR_PATTERN) {
                return false;
            }

            long windowStart = now - (PATTERN_WINDOW_SECONDS * 1000L);
            List<Long> recentMovements = new ArrayList<>();
            for (Long ts : movementTimestamps) {
                if (ts >= windowStart) {
                    recentMovements.add(ts);
                }
            }

            if (recentMovements.size() < MIN_ACTIVITIES_FOR_PATTERN) {
                return false;
            }

            double movementFrequency = (double) recentMovements.size() / PATTERN_WINDOW_SECONDS;

            if (movementFrequency < FARMING_MOVEMENT_THRESHOLD) {
                return false;
            }

            int blockInteractions = 0;
            for (Long ts : blockInteractTimestamps) {
                if (ts >= windowStart) {
                    blockInteractions++;
                }
            }

            double blockInteractionFrequency = (double) blockInteractions / PATTERN_WINDOW_SECONDS;

            return blockInteractionFrequency > 0.1 && movementFrequency > 0.5;
        }

        public boolean isPatternSuspicious() {
            if (activityTimestamps.size() < MIN_ACTIVITIES_FOR_PATTERN) {
                return false;
            }

            long now = System.currentTimeMillis();
            long windowStart = now - (PATTERN_WINDOW_SECONDS * 1000L);

            List<Long> recentActivity = new ArrayList<>();
            for (Long ts : activityTimestamps) {
                if (ts >= windowStart) {
                    recentActivity.add(ts);
                }
            }

            if (recentActivity.size() < MIN_ACTIVITIES_FOR_PATTERN) {
                return false;
            }

            double avgInterval = 0;
            for (int i = 1; i < recentActivity.size(); i++) {
                avgInterval += recentActivity.get(i) - recentActivity.get(i - 1);
            }
            avgInterval /= (recentActivity.size() - 1);

            double variance = 0;
            for (int i = 1; i < recentActivity.size(); i++) {
                double diff = (recentActivity.get(i) - recentActivity.get(i - 1)) - avgInterval;
                variance += diff * diff;
            }
            variance /= (recentActivity.size() - 1);

            double stdDev = Math.sqrt(variance);
            return stdDev < 500 && avgInterval < 2000;
        }

        public int getActivityCountInWindow(int windowSeconds) {
            long now = System.currentTimeMillis();
            long cutoff = now - (windowSeconds * 1000L);
            int count = 0;
            for (Long ts : activityTimestamps) {
                if (ts >= cutoff) {
                    count++;
                }
            }
            return count;
        }
    }

    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    private final Map<UUID, ActivityMetrics> activityState = new ConcurrentHashMap<>();
    private final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> semiAfkPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> farmingPlayers = ConcurrentHashMap.newKeySet();

    public AfkModule(SinglePlayerSleep plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void recordActivity(Player player, ActivityType type) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        ActivityMetrics metrics = getMetrics(uuid, now);
        metrics.forcedAfk = false;
        metrics.update(type, now);

        if (afkPlayers.remove(uuid)) {
            applyIndicator(player, metrics, false, AfkState.ACTIVE);
        }
        semiAfkPlayers.remove(uuid);
        farmingPlayers.remove(uuid);
    }

    public void recordActivity(UUID uuid) {
        if (uuid == null) {
            return;
        }
        long now = System.currentTimeMillis();
        ActivityMetrics metrics = getMetrics(uuid, now);
        metrics.forcedAfk = false;
        metrics.update(ActivityType.MANUAL, now);
    }

    public void recordCombat(Player player) {
        recordActivity(player, ActivityType.COMBAT);
    }

    public void recordBlockInteract(Player player) {
        recordActivity(player, ActivityType.BLOCK_INTERACT);
    }

    public boolean isAfk(Player player) {
        return getAfkState(player) == AfkState.FULL_AFK;
    }

    public boolean isSemiAfk(Player player) {
        AfkState state = getAfkState(player);
        return state == AfkState.SEMI_AFK || state == AfkState.FULL_AFK;
    }

    public AfkState getAfkState(Player player) {
        if (player == null) {
            return AfkState.ACTIVE;
        }
        if (!configManager.isAfkEnabled()) {
            return AfkState.ACTIVE;
        }
        if (player.hasPermission("singleplayersleep.bypassafk")) {
            return AfkState.ACTIVE;
        }
        ActivityMetrics metrics = activityState.get(player.getUniqueId());
        if (metrics == null) {
            return AfkState.ACTIVE;
        }
        if (metrics.forcedAfk) {
            return AfkState.FULL_AFK;
        }
        long timeoutMs = configManager.getAfkTimeoutMs();
        long semiAfkMs = configManager.getAfkSemiAfkTimeoutMs();
        boolean farmingPrevention = configManager.isAfkFarmingPreventionEnabled();
        return metrics.calculateState(timeoutMs, semiAfkMs, farmingPrevention);
    }

    public boolean isFarmingDetected(Player player) {
        return getAfkState(player) == AfkState.FARMING_DETECTED;
    }

    public void onPlayerJoin(Player player) {
        if (player == null) {
            return;
        }
        recordActivity(player, ActivityType.MANUAL);
        afkPlayers.remove(player.getUniqueId());
        semiAfkPlayers.remove(player.getUniqueId());
        farmingPlayers.remove(player.getUniqueId());
        ActivityMetrics metrics = activityState.get(player.getUniqueId());
        if (metrics != null) {
            applyIndicator(player, metrics, false, AfkState.ACTIVE);
        }
    }

    public void onPlayerLeave(UUID uuid) {
        if (uuid == null) {
            return;
        }
        activityState.remove(uuid);
        afkPlayers.remove(uuid);
        semiAfkPlayers.remove(uuid);
        farmingPlayers.remove(uuid);
    }

    public void cleanup() {
        Iterator<UUID> iterator = afkPlayers.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            if (Bukkit.getPlayer(uuid) == null) {
                iterator.remove();
            }
        }
        Iterator<UUID> semiIterator = semiAfkPlayers.iterator();
        while (semiIterator.hasNext()) {
            UUID uuid = semiIterator.next();
            if (Bukkit.getPlayer(uuid) == null) {
                semiIterator.remove();
            }
        }
        Iterator<UUID> farmingIterator = farmingPlayers.iterator();
        while (farmingIterator.hasNext()) {
            UUID uuid = farmingIterator.next();
            if (Bukkit.getPlayer(uuid) == null) {
                farmingIterator.remove();
            }
        }
        activityState.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
    }

    public void scheduledCheck(SleepManager sleepManager) {
        if (!configManager.isAfkEnabled()) {
            clearAllAfkIndicators();
            return;
        }
        cleanup();
        long now = System.currentTimeMillis();
        long timeoutMs = configManager.getAfkTimeoutMs();
        long semiAfkMs = configManager.getAfkSemiAfkTimeoutMs();
        boolean farmingPrevention = configManager.isAfkFarmingPreventionEnabled();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null) {
                continue;
            }
            ActivityMetrics metrics = getMetrics(player.getUniqueId(), now);
            AfkState newState = metrics.calculateState(timeoutMs, semiAfkMs, farmingPrevention);
            updateAfkState(player, metrics, newState, sleepManager);
        }
    }

    private void updateAfkState(Player player, ActivityMetrics metrics, AfkState newState, SleepManager sleepManager) {
        UUID uuid = player.getUniqueId();
        AfkState previousState = AfkState.ACTIVE;

        if (afkPlayers.contains(uuid)) {
            previousState = AfkState.FULL_AFK;
        } else if (semiAfkPlayers.contains(uuid)) {
            previousState = AfkState.SEMI_AFK;
        } else if (farmingPlayers.contains(uuid)) {
            previousState = AfkState.FARMING_DETECTED;
        }

        if (newState == previousState) {
            return;
        }

        afkPlayers.remove(uuid);
        semiAfkPlayers.remove(uuid);
        farmingPlayers.remove(uuid);

        switch (newState) {
            case FULL_AFK -> afkPlayers.add(uuid);
            case SEMI_AFK -> semiAfkPlayers.add(uuid);
            case FARMING_DETECTED -> farmingPlayers.add(uuid);
            case ACTIVE -> {}
        }

        applyIndicator(player, metrics, newState != AfkState.ACTIVE, newState);

        if (sleepManager != null) {
            sleepManager.getSessionIfExists(player.getWorld()).ifPresent(session -> {
                if (newState == AfkState.FULL_AFK || newState == AfkState.FARMING_DETECTED) {
                    session.onPlayerLeave(player);
                }
                session.refreshRequirement();
            });
        }
    }

    public boolean toggleManualAfk(Player player) {
        if (player == null) {
            return false;
        }
        ActivityMetrics metrics = getMetrics(player.getUniqueId(), System.currentTimeMillis());
        boolean next = !metrics.forcedAfk;
        setManualAfk(player, next);
        return next;
    }

    public void setManualAfk(Player player, boolean afk) {
        if (player == null) {
            return;
        }
        ActivityMetrics metrics = getMetrics(player.getUniqueId(), System.currentTimeMillis());
        metrics.forcedAfk = afk;
        if (afk) {
            updateAfkState(player, metrics, AfkState.FULL_AFK, null);
        } else {
            recordActivity(player, ActivityType.MANUAL);
        }
    }

    private ActivityMetrics getMetrics(UUID uuid, long now) {
        return activityState.computeIfAbsent(uuid, ignored -> new ActivityMetrics(now));
    }

    private void applyIndicator(Player player, ActivityMetrics metrics, boolean isAfk, AfkState state) {
        if (!configManager.isAfkIndicatorEnabled()) {
            return;
        }

        String prefix;
        String suffix;

        switch (state) {
            case FULL_AFK -> {
                prefix = ChatColor.translateAlternateColorCodes('&', configManager.getAfkFullAfkPrefix());
                suffix = ChatColor.translateAlternateColorCodes('&', configManager.getAfkFullAfkSuffix());
            }
            case SEMI_AFK -> {
                prefix = ChatColor.translateAlternateColorCodes('&', configManager.getAfkSemiAfkPrefix());
                suffix = ChatColor.translateAlternateColorCodes('&', configManager.getAfkSemiAfkSuffix());
            }
            case FARMING_DETECTED -> {
                prefix = ChatColor.translateAlternateColorCodes('&', configManager.getAfkFarmingPrefix());
                suffix = ChatColor.translateAlternateColorCodes('&', configManager.getAfkFarmingSuffix());
            }
            default -> {
                if (metrics.listNameBeforeAfk != null) {
                    player.setPlayerListName(metrics.listNameBeforeAfk);
                    metrics.listNameBeforeAfk = null;
                }
                return;
            }
        }

        if (metrics.listNameBeforeAfk == null) {
            metrics.listNameBeforeAfk = player.getPlayerListName();
        }

        String baseName = metrics.listNameBeforeAfk == null || metrics.listNameBeforeAfk.isBlank()
                ? player.getName()
                : metrics.listNameBeforeAfk;
        player.setPlayerListName(prefix + baseName + suffix);
    }

    private void clearAllAfkIndicators() {
        for (UUID uuid : afkPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            ActivityMetrics metrics = activityState.get(uuid);
            if (player != null && metrics != null) {
                applyIndicator(player, metrics, false, AfkState.ACTIVE);
            }
        }
        for (UUID uuid : semiAfkPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            ActivityMetrics metrics = activityState.get(uuid);
            if (player != null && metrics != null) {
                applyIndicator(player, metrics, false, AfkState.ACTIVE);
            }
        }
        for (UUID uuid : farmingPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            ActivityMetrics metrics = activityState.get(uuid);
            if (player != null && metrics != null) {
                applyIndicator(player, metrics, false, AfkState.ACTIVE);
            }
        }
        afkPlayers.clear();
        semiAfkPlayers.clear();
        farmingPlayers.clear();
    }

    public Set<UUID> getAfkPlayers() {
        return Set.copyOf(afkPlayers);
    }

    public Set<UUID> getSemiAfkPlayers() {
        return Set.copyOf(semiAfkPlayers);
    }

    public Set<UUID> getFarmingPlayers() {
        return Set.copyOf(farmingPlayers);
    }

    public ActivityMetrics getPlayerMetrics(Player player) {
        if (player == null) {
            return null;
        }
        return activityState.get(player.getUniqueId());
    }
}