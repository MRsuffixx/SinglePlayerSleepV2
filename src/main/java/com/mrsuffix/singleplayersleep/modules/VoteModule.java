package com.mrsuffix.singleplayersleep.modules;

import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoteModule {
    
    private final Map<String, Set<UUID>> votes = new ConcurrentHashMap<>();
    private final Map<UUID, String> voteWorlds = new ConcurrentHashMap<>();
    private final Map<UUID, Long> voteTimestamps = new ConcurrentHashMap<>();
    private final ConfigManager configManager;
    
    public VoteModule(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    public boolean addVote(Player player) {
        if (player == null) {
            return false;
        }
        String world = player.getWorld().getName();
        boolean added = votes.computeIfAbsent(world, k -> ConcurrentHashMap.newKeySet())
                .add(player.getUniqueId());
        if (added) {
            voteWorlds.put(player.getUniqueId(), world);
            voteTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
        }
        return added;
    }
    
    public void removeVote(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        // Remove from the world the vote was originally cast in
        String originalWorld = voteWorlds.remove(uuid);
        voteTimestamps.remove(uuid);
        String worldName = originalWorld != null ? originalWorld : player.getWorld().getName();
        Set<UUID> worldVotes = votes.get(worldName);
        if (worldVotes != null) {
            worldVotes.remove(uuid);
        }
    }
    
    public boolean hasVoted(Player player) {
        if (player == null) {
            return false;
        }
        // Check if player voted in their current world
        String currentWorld = player.getWorld().getName();
        String voteWorld = voteWorlds.get(player.getUniqueId());
        if (voteWorld != null && !voteWorld.equals(currentWorld)) {
            // Player changed world since voting — invalidate their vote
            removeVote(player);
            return false;
        }
        Set<UUID> worldVotes = votes.get(currentWorld);
        return worldVotes != null && worldVotes.contains(player.getUniqueId());
    }
    
    public Set<UUID> getVotes(String worldName) {
        Set<UUID> worldVotes = votes.get(worldName);
        return worldVotes == null ? Set.of() : Collections.unmodifiableSet(worldVotes);
    }
    
    public int getVoteCount(String worldName) {
        Set<UUID> worldVotes = votes.get(worldName);
        return worldVotes == null ? 0 : worldVotes.size();
    }
    
    public void clearVotes(String worldName) {
        Set<UUID> removed = votes.remove(worldName);
        if (removed != null) {
            for (UUID uuid : removed) {
                voteWorlds.remove(uuid);
                voteTimestamps.remove(uuid);
            }
        }
    }

    public void clearAll() {
        votes.clear();
        voteWorlds.clear();
        voteTimestamps.clear();
    }

    /**
     * Removes expired votes based on the configured timeout.
     * Should be called periodically from the scheduler.
     */
    public void cleanupExpiredVotes() {
        int timeoutSeconds = configManager.getVoteTimeoutSeconds();
        if (timeoutSeconds <= 0) {
            return;
        }
        long cutoff = System.currentTimeMillis() - (timeoutSeconds * 1000L);
        voteTimestamps.entrySet().removeIf(entry -> {
            if (entry.getValue() < cutoff) {
                UUID uuid = entry.getKey();
                String worldName = voteWorlds.remove(uuid);
                if (worldName != null) {
                    Set<UUID> worldVotes = votes.get(worldName);
                    if (worldVotes != null) {
                        worldVotes.remove(uuid);
                    }
                }
                return true;
            }
            return false;
        });
    }
    
    public boolean isVoteMode() {
        return configManager.getSleepMode() != null && configManager.getSleepMode().isPercentage();
    }
}
