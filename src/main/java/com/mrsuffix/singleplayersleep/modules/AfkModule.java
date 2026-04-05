package com.mrsuffix.singleplayersleep.modules;

import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AfkModule {

    public enum ActivityType {
        MOVE,
        CHAT,
        INTERACT,
        COMMAND,
        INVENTORY,
        MANUAL
    }

    private static final class ActivityState {
        private long lastActivity;
        private long lastMove;
        private long lastChat;
        private long lastInteract;
        private long lastCommand;
        private long lastInventory;
        private boolean forcedAfk;
        private String listNameBeforeAfk;

        private ActivityState(long now) {
            this.lastActivity = now;
        }

        private void update(ActivityType type, long now) {
            lastActivity = now;
            switch (type) {
                case MOVE -> lastMove = now;
                case CHAT -> lastChat = now;
                case INTERACT -> lastInteract = now;
                case COMMAND -> lastCommand = now;
                case INVENTORY -> lastInventory = now;
                case MANUAL -> {
                }
            }
        }
    }
    
    private final ConfigManager configManager;
    private final Map<UUID, ActivityState> activityState = new ConcurrentHashMap<>();
    private final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();
    
    public AfkModule(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    public void recordActivity(Player player, ActivityType type) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        ActivityState state = getState(uuid, now);
        state.forcedAfk = false;
        state.update(type, now);
        if (afkPlayers.remove(uuid)) {
            applyIndicator(player, state, false);
        }
    }

    public void recordActivity(UUID uuid) {
        if (uuid == null) {
            return;
        }
        long now = System.currentTimeMillis();
        ActivityState state = getState(uuid, now);
        state.forcedAfk = false;
        state.update(ActivityType.MANUAL, now);
    }
    
    public boolean isAfk(Player player) {
        if (player == null) {
            return false;
        }
        if (!configManager.isAfkEnabled()) {
            return false;
        }
        if (player.hasPermission("singleplayersleep.bypassafk")) {
            return false;
        }
        ActivityState state = activityState.get(player.getUniqueId());
        if (state == null) {
            return false;
        }
        if (state.forcedAfk) {
            return true;
        }
        return (System.currentTimeMillis() - state.lastActivity) > configManager.getAfkTimeoutMs();
    }
    
    public void onPlayerJoin(Player player) {
        if (player == null) {
            return;
        }
        recordActivity(player, ActivityType.MANUAL);
        afkPlayers.remove(player.getUniqueId());
        ActivityState state = activityState.get(player.getUniqueId());
        if (state != null) {
            applyIndicator(player, state, false);
        }
    }
    
    public void onPlayerLeave(UUID uuid) {
        if (uuid == null) {
            return;
        }
        activityState.remove(uuid);
        afkPlayers.remove(uuid);
    }
    
    public void cleanup() {
        activityState.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        afkPlayers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }
    
    public void scheduledCheck(SleepManager sleepManager) {
        if (sleepManager == null) {
            return;
        }
        if (!configManager.isAfkEnabled()) {
            clearAfkIndicators();
            return;
        }
        cleanup();
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null) {
                continue;
            }
            ActivityState state = getState(player.getUniqueId(), now);
            boolean afkNow = state.forcedAfk
                    || (now - state.lastActivity) > configManager.getAfkTimeoutMs();
            updateAfkState(player, state, afkNow, sleepManager);
        }
    }

    public boolean toggleManualAfk(Player player) {
        if (player == null) {
            return false;
        }
        ActivityState state = getState(player.getUniqueId(), System.currentTimeMillis());
        boolean next = !state.forcedAfk;
        setManualAfk(player, next);
        return next;
    }

    public void setManualAfk(Player player, boolean afk) {
        if (player == null) {
            return;
        }
        ActivityState state = getState(player.getUniqueId(), System.currentTimeMillis());
        state.forcedAfk = afk;
        if (afk) {
            updateAfkState(player, state, true, null);
        } else {
            recordActivity(player, ActivityType.MANUAL);
        }
    }

    private ActivityState getState(UUID uuid, long now) {
        return activityState.computeIfAbsent(uuid, ignored -> new ActivityState(now));
    }

    private void updateAfkState(Player player,
                                ActivityState state,
                                boolean isAfkNow,
                                SleepManager sleepManager) {
        UUID uuid = player.getUniqueId();
        boolean wasAfk = afkPlayers.contains(uuid);
        if (isAfkNow == wasAfk) {
            return;
        }
        if (isAfkNow) {
            afkPlayers.add(uuid);
        } else {
            afkPlayers.remove(uuid);
        }
        applyIndicator(player, state, isAfkNow);
        if (sleepManager != null) {
            sleepManager.getSessionIfExists(player.getWorld()).ifPresent(session -> {
                if (isAfkNow) {
                    session.onPlayerLeave(player);
                }
                session.refreshRequirement();
            });
        }
    }

    private void applyIndicator(Player player, ActivityState state, boolean isAfkNow) {
        if (!configManager.isAfkIndicatorEnabled()) {
            return;
        }
        if (isAfkNow) {
            if (state.listNameBeforeAfk == null) {
                state.listNameBeforeAfk = player.getPlayerListName();
            }
            String prefix = ChatColor.translateAlternateColorCodes('&', configManager.getAfkIndicatorPrefix());
            String suffix = ChatColor.translateAlternateColorCodes('&', configManager.getAfkIndicatorSuffix());
            String baseName = state.listNameBeforeAfk == null || state.listNameBeforeAfk.isBlank()
                    ? player.getName()
                    : state.listNameBeforeAfk;
            player.setPlayerListName(prefix + baseName + suffix);
        } else if (state.listNameBeforeAfk != null) {
            player.setPlayerListName(state.listNameBeforeAfk);
            state.listNameBeforeAfk = null;
        }
    }

    private void clearAfkIndicators() {
        for (UUID uuid : afkPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            ActivityState state = activityState.get(uuid);
            if (player != null && state != null) {
                applyIndicator(player, state, false);
            }
        }
        afkPlayers.clear();
    }
}
