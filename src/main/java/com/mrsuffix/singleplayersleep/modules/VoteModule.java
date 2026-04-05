package com.mrsuffix.singleplayersleep.modules;

import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VoteModule {
    
    private final Map<String, Set<UUID>> votes = new HashMap<>();
    private final ConfigManager configManager;
    
    public VoteModule(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    public boolean addVote(Player player) {
        if (player == null || player.getWorld() == null) {
            return false;
        }
        String world = player.getWorld().getName();
        votes.computeIfAbsent(world, k -> new HashSet<>());
        return votes.get(world).add(player.getUniqueId());
    }
    
    public void removeVote(Player player) {
        if (player == null || player.getWorld() == null) {
            return;
        }
        Set<UUID> worldVotes = votes.get(player.getWorld().getName());
        if (worldVotes != null) {
            worldVotes.remove(player.getUniqueId());
        }
    }
    
    public boolean hasVoted(Player player) {
        if (player == null || player.getWorld() == null) {
            return false;
        }
        Set<UUID> worldVotes = votes.get(player.getWorld().getName());
        return worldVotes != null && worldVotes.contains(player.getUniqueId());
    }
    
    public int getVoteCount(String worldName) {
        Set<UUID> worldVotes = votes.get(worldName);
        return worldVotes == null ? 0 : worldVotes.size();
    }
    
    public void clearVotes(String worldName) {
        votes.remove(worldName);
    }
    
    public boolean isVoteMode() {
        return configManager.getSleepMode().equals("percentage");
    }
}
