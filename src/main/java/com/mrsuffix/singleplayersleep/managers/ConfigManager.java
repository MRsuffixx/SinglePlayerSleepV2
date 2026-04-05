package com.mrsuffix.singleplayersleep.managers;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class ConfigManager {
    
    private final SinglePlayerSleep plugin;
    
    private String sleepMode;
    private double sleepPercentage;
    private int delayTicks;
    private int cooldownSeconds;
    private boolean clearWeather;
    private boolean autoSave;
    
    private boolean afkEnabled;
    private long afkTimeoutMs;
    private boolean excludeAfkFromCount;
    private int afkCheckIntervalTicks;
    
    private boolean countdownEnabled;
    private int countdownDurationSeconds;
    private boolean countdownShowActionBar;
    private boolean countdownShowChat;
    private boolean countdownSoundOnTick;
    
    private boolean particlesEnabled;
    private Particle particleType;
    private boolean smartScale;
    
    private boolean soundsEnabled;
    private Sound sleepStartSound;
    private Sound nightSkipSound;
    private Sound countdownTickSound;
    
    private Set<String> enabledWorlds;
    private String worldsMode;
    
    private boolean phantomResetOnSkip;
    
    private boolean updateCheckerEnabled;
    private String githubUser;
    private String githubRepo;
    private boolean notifyOnJoin;
    private int updateCheckIntervalHours;
    
    private String sleepCommandName;
    private List<String> sleepAliases;
    
    private boolean statsEnabled;
    private boolean statsPersist;
    private boolean trackPerPlayer;
    
    private boolean debugEnabled;
    
    private Map<String, String> messages;
    
    public ConfigManager(SinglePlayerSleep plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
    }
    
    public void loadCache() {
        sleepMode = plugin.getConfig().getString("sleep.mode", "single");
        sleepPercentage = plugin.getConfig().getDouble("sleep.percentage", 50.0);
        delayTicks = plugin.getConfig().getInt("sleep.delay-ticks", 100);
        cooldownSeconds = plugin.getConfig().getInt("sleep.cooldown-seconds", 60);
        clearWeather = plugin.getConfig().getBoolean("sleep.clear-weather", true);
        autoSave = plugin.getConfig().getBoolean("sleep.auto-save", true);
        
        afkEnabled = plugin.getConfig().getBoolean("afk.enabled", true);
        int timeoutSeconds = plugin.getConfig().getInt("afk.timeout-seconds", 300);
        afkTimeoutMs = timeoutSeconds * 1000L;
        excludeAfkFromCount = plugin.getConfig().getBoolean("afk.exclude-from-count", true);
        afkCheckIntervalTicks = plugin.getConfig().getInt("afk.check-interval-ticks", 200);
        
        countdownEnabled = plugin.getConfig().getBoolean("countdown.enabled", true);
        countdownDurationSeconds = plugin.getConfig().getInt("countdown.duration-seconds", 5);
        countdownShowActionBar = plugin.getConfig().getBoolean("countdown.show-actionbar", true);
        countdownShowChat = plugin.getConfig().getBoolean("countdown.show-chat", false);
        countdownSoundOnTick = plugin.getConfig().getBoolean("countdown.sound-on-each-tick", true);
        
        particlesEnabled = plugin.getConfig().getBoolean("effects.particles.enabled", true);
        String particleTypeName = plugin.getConfig().getString("effects.particles.type", "CLOUD");
        particleType = parseParticle(particleTypeName);
        smartScale = plugin.getConfig().getBoolean("effects.particles.smart-scale", true);
        
        soundsEnabled = plugin.getConfig().getBoolean("effects.sounds.enabled", true);
        String sleepStartSoundName = plugin.getConfig().getString("effects.sounds.sleep-start", "ENTITY_PLAYER_SLEEP");
        sleepStartSound = parseSound(sleepStartSoundName);
        String nightSkipSoundName = plugin.getConfig().getString("effects.sounds.night-skip", "UI_TOAST_CHALLENGE_COMPLETE");
        nightSkipSound = parseSound(nightSkipSoundName);
        String countdownTickSoundName = plugin.getConfig().getString("effects.sounds.countdown-tick", "BLOCK_NOTE_BLOCK_HAT");
        countdownTickSound = parseSound(countdownTickSoundName);
        
        enabledWorlds = new HashSet<>(plugin.getConfig().getStringList("worlds.enabled"));
        worldsMode = plugin.getConfig().getString("worlds.mode", "whitelist");
        
        phantomResetOnSkip = plugin.getConfig().getBoolean("phantom.reset-on-skip", true);
        
        updateCheckerEnabled = plugin.getConfig().getBoolean("update-checker.enabled", true);
        githubUser = plugin.getConfig().getString("update-checker.github-user", "mrsuffix");
        githubRepo = plugin.getConfig().getString("update-checker.github-repo", "SinglePlayerSleep");
        notifyOnJoin = plugin.getConfig().getBoolean("update-checker.notify-on-join", true);
        updateCheckIntervalHours = plugin.getConfig().getInt("update-checker.check-interval-hours", 24);
        
        sleepCommandName = plugin.getConfig().getString("command.sleep-command-name", "sleep");
        sleepAliases = plugin.getConfig().getStringList("command.sleep-aliases");
        
        statsEnabled = plugin.getConfig().getBoolean("stats.enabled", true);
        statsPersist = plugin.getConfig().getBoolean("stats.persist", true);
        trackPerPlayer = plugin.getConfig().getBoolean("stats.track-per-player", true);
        
        debugEnabled = plugin.getConfig().getBoolean("debug.enabled", false);
        
        loadMessages();
    }
    
    private void loadMessages() {
        messages.clear();
        ConfigurationSection messagesSection = plugin.getConfig().getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                String value = messagesSection.getString(key);
                if (value != null) {
                    messages.put(key, value);
                }
            }
        }
    }
    
    private Particle parseParticle(String name) {
        if (name == null) {
            plugin.getLogger().warning("Particle type is null, using default: CLOUD");
            return Particle.CLOUD;
        }
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type '" + name + "', using default: CLOUD");
            return Particle.CLOUD;
        }
    }
    
    private Sound parseSound(String name) {
        if (name == null) {
            plugin.getLogger().warning("Sound name is null, using default sound");
            return Sound.ENTITY_PLAYER_SLEEP;
        }
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name '" + name + "', using default sound");
            return Sound.ENTITY_PLAYER_SLEEP;
        }
    }
    
    public String getSleepMode() {
        return sleepMode;
    }
    
    public double getSleepPercentage() {
        return sleepPercentage;
    }
    
    public int getDelayTicks() {
        return delayTicks;
    }
    
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }
    
    public boolean isClearWeather() {
        return clearWeather;
    }
    
    public boolean isAutoSave() {
        return autoSave;
    }
    
    public boolean isAfkEnabled() {
        return afkEnabled;
    }
    
    public long getAfkTimeoutMs() {
        return afkTimeoutMs;
    }
    
    public boolean isExcludeAfkFromCount() {
        return excludeAfkFromCount;
    }
    
    public int getAfkCheckIntervalTicks() {
        return afkCheckIntervalTicks;
    }
    
    public boolean isCountdownEnabled() {
        return countdownEnabled;
    }
    
    public int getCountdownDurationSeconds() {
        return countdownDurationSeconds;
    }
    
    public boolean isCountdownShowActionBar() {
        return countdownShowActionBar;
    }
    
    public boolean isCountdownShowChat() {
        return countdownShowChat;
    }
    
    public boolean isCountdownSoundOnTick() {
        return countdownSoundOnTick;
    }
    
    public boolean isParticlesEnabled() {
        return particlesEnabled;
    }
    
    public Particle getParticleType() {
        return particleType;
    }
    
    public boolean isSmartScale() {
        return smartScale;
    }
    
    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }
    
    public Sound getSleepStartSound() {
        return sleepStartSound;
    }
    
    public Sound getNightSkipSound() {
        return nightSkipSound;
    }
    
    public Sound getCountdownTickSound() {
        return countdownTickSound;
    }
    
    public Set<String> getEnabledWorlds() {
        return enabledWorlds;
    }
    
    public String getWorldsMode() {
        return worldsMode;
    }
    
    public boolean isPhantomResetOnSkip() {
        return phantomResetOnSkip;
    }
    
    public boolean isUpdateCheckerEnabled() {
        return updateCheckerEnabled;
    }
    
    public String getGithubUser() {
        return githubUser;
    }
    
    public String getGithubRepo() {
        return githubRepo;
    }
    
    public boolean isNotifyOnJoin() {
        return notifyOnJoin;
    }
    
    public int getUpdateCheckIntervalHours() {
        return updateCheckIntervalHours;
    }
    
    public String getSleepCommandName() {
        return sleepCommandName;
    }
    
    public List<String> getSleepAliases() {
        return sleepAliases;
    }
    
    public boolean isStatsEnabled() {
        return statsEnabled;
    }
    
    public boolean isStatsPersist() {
        return statsPersist;
    }
    
    public boolean isTrackPerPlayer() {
        return trackPerPlayer;
    }
    
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugOverride(boolean enabled) {
        this.debugEnabled = enabled;
    }
    
    public Map<String, String> getMessages() {
        return messages;
    }
}
