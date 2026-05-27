package com.mrsuffix.singleplayersleep.commands;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.TaskScheduler;
import com.mrsuffix.singleplayersleep.core.CooldownManager;
import com.mrsuffix.singleplayersleep.core.SleepManager;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.MessageUtil;
import com.mrsuffix.singleplayersleep.managers.StatsManager;
import com.mrsuffix.singleplayersleep.managers.WorldManager;
import com.mrsuffix.singleplayersleep.modules.AfkModule;
import com.mrsuffix.singleplayersleep.modules.UpdateModule;
import com.mrsuffix.singleplayersleep.modules.VoteModule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class SpsCommand implements CommandExecutor, TabCompleter {
    
    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    private final SleepManager sleepManager;
    private final CooldownManager cooldownManager;
    private final VoteModule voteModule;
    private final WorldManager worldManager;
    private final StatsManager statsManager;
    private final UpdateModule updateModule;
    private final AfkModule afkModule;
    private final MessageUtil messageUtil;
    private final TaskScheduler taskScheduler;
    
    public SpsCommand(SinglePlayerSleep plugin, ConfigManager configManager,
                      SleepManager sleepManager, CooldownManager cooldownManager,
                      VoteModule voteModule, WorldManager worldManager,
                      StatsManager statsManager, UpdateModule updateModule,
                      AfkModule afkModule, MessageUtil messageUtil,
                      TaskScheduler taskScheduler) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.sleepManager = sleepManager;
        this.cooldownManager = cooldownManager;
        this.voteModule = voteModule;
        this.worldManager = worldManager;
        this.statsManager = statsManager;
        this.updateModule = updateModule;
        this.afkModule = afkModule;
        this.messageUtil = messageUtil;
        this.taskScheduler = taskScheduler;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("singleplayersleep.admin")) {
            sender.sendMessage(messageUtil.format("no-permission", Map.of()));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                sleepManager.resetAll();
                cooldownManager.clear();
                for (World world : Bukkit.getWorlds()) {
                    voteModule.clearVotes(world.getName());
                }
                plugin.reloadConfig();
                configManager.loadCache();
                // Reconfigure command aliases from config
                plugin.configureCommands();
                // Restart scheduled tasks with new config
                if (taskScheduler != null) {
                    taskScheduler.restart();
                }
                statsManager.reload();
                sender.sendMessage(messageUtil.format("reload-success", Map.of()));
                return true;
            case "stats":
                handleStats(sender, args);
                return true;
            case "afk":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                Player player = (Player) sender;
                boolean nowAfk = afkModule.toggleManualAfk(player);
                sender.sendMessage(messageUtil.format(nowAfk ? "afk-marked" : "afk-unmarked", Map.of()));
                return true;
            case "version":
                sender.sendMessage("§b=== SinglePlayerSleep v" + plugin.getDescription().getVersion() + " by mrsuffix ===");
                sender.sendMessage("§7Mode: §f" + configManager.getSleepModeName());
                sender.sendMessage("§7API version: §f" + plugin.getDescription().getAPIVersion());
                if (updateModule != null && updateModule.isUpdateAvailable()) {
                    sender.sendMessage("§eUpdate available: §f" + updateModule.getLatestVersion());
                } else {
                    sender.sendMessage("§aPlugin is up to date.");
                }
                return true;
            case "debug":
                boolean current = configManager.isDebugEnabled();
                configManager.setDebugOverride(!current);
                sender.sendMessage(messageUtil.format(current ? "debug-disabled" : "debug-enabled", Map.of()));
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private void handleStats(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found or offline.");
                return;
            }
            StatsManager.PlayerStats stats = statsManager.getPlayerStats(target.getUniqueId());
            if (stats == null) {
                stats = new StatsManager.PlayerStats(target.getName());
            }
            sender.sendMessage("§b=== SinglePlayerSleep Player Stats ===");
            sender.sendMessage("§7Player: §f" + stats.name);
            sender.sendMessage("§7Times slept: §f" + stats.timesSlept);
            sender.sendMessage("§7Nights contributed: §f" + stats.nightsContributedTo);
            return;
        }
        
        StatsManager.GlobalStats global = statsManager.getGlobalStats();
        sender.sendMessage("§b=== SinglePlayerSleep Stats ===");
        sender.sendMessage("§7Total nights skipped: §f" + global.totalNightsSkipped);
        sender.sendMessage("§7Total sleep events: §f" + global.totalSleepEvents);
        long lastSkip = global.lastSkipTimestamp;
        String lastSkipText = lastSkip <= 0 ? "Never" :
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastSkip));
        sender.sendMessage("§7Last skip: §f" + lastSkipText);
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§b/sps reload §7- Reload config");
        sender.sendMessage("§b/sps stats [player] §7- View stats");
        sender.sendMessage("§b/sps afk §7- Toggle AFK status");
        sender.sendMessage("§b/sps version §7- Plugin info");
        sender.sendMessage("§b/sps debug §7- Toggle debug mode");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("singleplayersleep.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            List<String> options = List.of("reload", "stats", "afk", "version", "debug");
            return options.stream()
                    .filter(opt -> opt.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT)
                            .startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
