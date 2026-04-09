package com.mrsuffix.singleplayersleep.modules;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.managers.ConfigManager;
import com.mrsuffix.singleplayersleep.managers.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import com.mrsuffix.singleplayersleep.util.TickUtil;
import java.util.logging.Logger;

public class UpdateModule {
    
    private final SinglePlayerSleep plugin;
    private final ConfigManager configManager;
    private final com.mrsuffix.singleplayersleep.managers.MessageUtil messageUtil;
    private final Logger logger;
    private final String currentVersion;
    
    private String latestVersion = null;
    private boolean updateAvailable = false;
    
    public UpdateModule(SinglePlayerSleep plugin, ConfigManager configManager, com.mrsuffix.singleplayersleep.managers.MessageUtil messageUtil) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageUtil = messageUtil;
        this.logger = plugin.getLogger();
        this.currentVersion = plugin.getDescription().getVersion();
    }
    
    public void checkForUpdate() {
        try {
            String url = "https://api.github.com/repos/" + configManager.getGithubUser() + "/"
                    + configManager.getGithubRepo() + "/releases/latest";
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "SinglePlayerSleep-Updater");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            
            if (connection.getResponseCode() != 200) {
                return;
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            String json = response.toString();
            String tagKey = "\"tag_name\":\"";
            int startIndex = json.indexOf(tagKey);
            if (startIndex == -1) {
                return;
            }
            int tagStart = startIndex + tagKey.length();
            int tagEnd = json.indexOf("\"", tagStart);
            if (tagEnd == -1) {
                return;
            }
            
            String tagName = json.substring(tagStart, tagEnd);
            String remoteVersion = normalizeVersion(tagName);
            String localVersion = normalizeVersion(currentVersion);
            
            if (isNewer(remoteVersion, localVersion)) {
                latestVersion = tagName;
                updateAvailable = true;
                logger.info("Update available: " + latestVersion + " — Download at: github.com/"
                        + configManager.getGithubUser() + "/" + configManager.getGithubRepo() + "/releases");
            }
        } catch (Exception e) {
            if (configManager.isDebugEnabled()) {
                logger.warning("Update check failed: " + e.getMessage());
            }
        }
    }
    
    private String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }
        return version.startsWith("v") ? version.substring(1) : version;
    }
    
    private boolean isNewer(String remote, String current) {
        if (remote == null || current == null) {
            return false;
        }
        String[] remoteParts = remote.split("\\.");
        String[] currentParts = current.split("\\.");
        int length = Math.max(remoteParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int remoteVal = i < remoteParts.length ? parseInt(remoteParts[i]) : 0;
            int currentVal = i < currentParts.length ? parseInt(currentParts[i]) : 0;
            if (remoteVal > currentVal) {
                return true;
            }
            if (remoteVal < currentVal) {
                return false;
            }
        }
        return false;
    }
    
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public String getLatestVersion() {
        return latestVersion;
    }
    
    public void notifyPlayer(Player player) {
        if (!updateAvailable || latestVersion == null) {
            return;
        }
        if (player == null || !player.hasPermission("singleplayersleep.admin")) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                messageUtil.send(player, "update-available", Map.of("version", latestVersion)),
                TickUtil.TICKS_PER_SECOND);
    }
}
