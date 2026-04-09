package com.mrsuffix.singleplayersleep.commands;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.core.CooldownManager;
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
    private final CooldownManager cooldownManager;
    private final MessageUtil messageUtil;
    
    public SleepCommand(SinglePlayerSleep plugin, ConfigManager configManager,
                        SleepManager sleepManager, VoteModule voteModule,
                        WorldManager worldManager, CooldownManager cooldownManager,
                        MessageUtil messageUtil) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.sleepManager = sleepManager;
        this.voteModule = voteModule;
        this.worldManager = worldManager;
        this.cooldownManager = cooldownManager;
        this.messageUtil = messageUtil;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        if (!player.hasPermission("singleplayersleep.sleep")) {
            messageUtil.send(player, "no-permission", Map.of());
            return true;
        }
        
        World world = player.getWorld();
        if (world == null) {
            return true;
        }
        
        if (world.getEnvironment() != World.Environment.NORMAL) {
            messageUtil.send(player, "wrong-world", Map.of());
            return true;
        }
        
        if (!worldManager.isEnabled(world)) {
            messageUtil.send(player, "world-disabled", Map.of());
            return true;
        }
        
        // Check cooldown before allowing vote (unless player has bypass permission)
        if (!player.hasPermission("singleplayersleep.bypasscooldown") && cooldownManager.isOnCooldown(world)) {
            long remaining = cooldownManager.getRemainingSeconds(world);
            messageUtil.send(player, "cooldown-active", Map.of("seconds", String.valueOf(remaining)));
            return true;
        }
        
        if (configManager.getSleepMode() != null && configManager.getSleepMode().isSingle()) {
            messageUtil.send(player, "command-sleep-help", Map.of());
            int current = sleepManager.getSessionIfExists(world)
                    .map(session -> session.getEffectiveSleepingCount())
                    .orElse(0);
            messageUtil.send(player, "vote-needed",
                    Map.of("current", String.valueOf(current), "required", "1"));
            return true;
        }
        
        if (configManager.getSleepMode() != null && configManager.getSleepMode().isPercentage()) {
            if (voteModule.hasVoted(player)) {
                voteModule.removeVote(player);
                // Also remove from sleeping session if present
                SleepSession session = sleepManager.getSessionIfExists(world).orElse(null);
                if (session != null) {
                    session.onPlayerWake(player);
                }
                messageUtil.broadcastWorld(world, "player-woke-up",
                        Map.of("player", player.getName()));
                return true;
            }
            
            boolean isNew = voteModule.addVote(player);
            if (isNew) {
                int current = voteModule.getVoteCount(world.getName());
                SleepSession session = sleepManager.getSession(world);
                int required = session == null ? 1 : session.calculateRequired();
                messageUtil.broadcastWorld(world, "player-sleeping",
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
