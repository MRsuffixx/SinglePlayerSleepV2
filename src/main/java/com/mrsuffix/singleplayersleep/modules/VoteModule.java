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
    private final ConfigManager configManager;
    
    public VoteModule(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    public boolean addVote(Player player) {
        if (player == null) {
            return false;
        }
        String world = player.getWorld().getName();
        return votes.computeIfAbsent(world, k -> ConcurrentHashMap.newKeySet())
                .add(player.getUniqueId());
    }
    
    public void removeVote(Player player) {
        if (player == null) {
            return;
        }
        Set<UUID> worldVotes = votes.get(player.getWorld().getName());
        if (worldVotes != null) {
            worldVotes.remove(player.getUniqueId());
        }
    }
    
    public boolean hasVoted(Player player) {
        if (player == null) {
            return false;
        }
        Set<UUID> worldVotes = votes.get(player.getWorld().getName());
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
        votes.remove(worldName);
    }

    public void clearAll() {
        votes.clear();
    }
    
    public boolean isVoteMode() {
        return configManager.getSleepMode() != null && configManager.getSleepMode().isPercentage();
    }
}
