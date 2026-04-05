package com.mrsuffix.singleplayersleep.commands;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.core.SleepSession;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.MessageUtil;
import com.mrsuffix.singleplayersleep.managers.WorldManager;
import com.mrsuffix.singleplayersleep.modules.VoteModule;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class SleepCommand implements CommandExecutor {
    
    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    private final SleepManager sleepManager;
    private final VoteModule voteModule;
    private final WorldManager worldManager;
    
    public SleepCommand(SinglePlayerSleep plugin, ConfigManager configManager,
                        SleepManager sleepManager, VoteModule voteModule,
                        WorldManager worldManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.sleepManager = sleepManager;
        this.voteModule = voteModule;
        this.worldManager = worldManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        if (!player.hasPermission("singleplayersleep.sleep")) {
            MessageUtil.send(player, "no-permission", Map.of());
            return true;
        }
        
        World world = player.getWorld();
        if (world == null) {
            return true;
        }
        
        if (world.getEnvironment() != World.Environment.NORMAL) {
            MessageUtil.send(player, "wrong-world", Map.of());
            return true;
        }
        
        if (!worldManager.isEnabled(world)) {
            MessageUtil.send(player, "no-permission", Map.of());
            return true;
        }
        
        if (configManager.getSleepMode().equals("single")) {
            MessageUtil.send(player, "command-sleep-help", Map.of());
            int current = sleepManager.getSessionIfExists(world)
                    .map(session -> session.getSleepingPlayers().size())
                    .orElse(0);
            MessageUtil.send(player, "vote-needed",
                    Map.of("current", String.valueOf(current), "required", "1"));
            return true;
        }
        
        if (configManager.getSleepMode().equals("percentage")) {
            if (voteModule.hasVoted(player)) {
                voteModule.removeVote(player);
                MessageUtil.broadcastWorld(world, "player-woke-up",
                        Map.of("player", player.getName()));
                return true;
            }
            
            boolean isNew = voteModule.addVote(player);
            if (isNew) {
                int current = voteModule.getVoteCount(world.getName());
                SleepSession session = sleepManager.getSession(world);
                int required = session == null ? 1 : session.calculateRequired();
                MessageUtil.broadcastWorld(world, "player-sleeping",
                        Map.of("player", player.getName(),
                               "current", String.valueOf(current),
                               "required", String.valueOf(required)));
                if (session != null) {
                    session.checkSleepCondition();
                }
            }
        }
        
        return true;
    }
}
