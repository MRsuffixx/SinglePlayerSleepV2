package com.mrsuffix.singleplayersleep.managers;

import com.mrsuffix.singleplayersleep.SinglePlayerSleep;
import com.mrsuffix.singleplayersleep.core.SleepRule;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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
    private List<SleepRule> dynamicRules;
    private boolean weatherChangeEnabled;
    private boolean clearRain;
    private boolean clearThunder;
    
    private boolean afkEnabled;
    private long afkTimeoutMs;
    private boolean excludeAfkFromCount;
    private int afkCheckIntervalTicks;
    private boolean afkIndicatorEnabled;
    private String afkIndicatorPrefix;
    private String afkIndicatorSuffix;
    
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
    private WorldSettings defaultWorldSettings;
    private final Map<String, WorldSettings> worldOverrides = new HashMap<>();
    private final Map<String, Boolean> worldEnableOverrides = new HashMap<>();
    
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
    private int leaderboardRefreshSeconds;
    
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
        dynamicRules = parseRules(plugin.getConfig().getStringList("sleep.dynamic-rules"));
        boolean legacyClearWeather = plugin.getConfig().getBoolean("sleep.clear-weather", true);
        ConfigurationSection sleepWeather = plugin.getConfig().getConfigurationSection("sleep.weather");
        if (sleepWeather != null) {
            weatherChangeEnabled = sleepWeather.getBoolean("change-weather", legacyClearWeather);
            clearRain = sleepWeather.getBoolean("clear-rain", true);
            clearThunder = sleepWeather.getBoolean("clear-thunder", true);
        } else {
            weatherChangeEnabled = legacyClearWeather;
            clearRain = legacyClearWeather;
            clearThunder = legacyClearWeather;
        }
        clearWeather = weatherChangeEnabled;
        autoSave = plugin.getConfig().getBoolean("sleep.auto-save", true);
        
        afkEnabled = plugin.getConfig().getBoolean("afk.enabled", true);
        int timeoutSeconds = plugin.getConfig().getInt("afk.timeout-seconds", 300);
        afkTimeoutMs = timeoutSeconds * 1000L;
        excludeAfkFromCount = plugin.getConfig().getBoolean("afk.exclude-from-count", true);
        afkCheckIntervalTicks = plugin.getConfig().getInt("afk.check-interval-ticks", 200);
        ConfigurationSection afkIndicator = plugin.getConfig().getConfigurationSection("afk.indicator");
        if (afkIndicator != null) {
            afkIndicatorEnabled = afkIndicator.getBoolean("enabled", false);
            afkIndicatorPrefix = afkIndicator.getString("list-prefix", "&7[AFK] ");
            afkIndicatorSuffix = afkIndicator.getString("list-suffix", "");
        } else {
            afkIndicatorEnabled = false;
            afkIndicatorPrefix = "&7[AFK] ";
            afkIndicatorSuffix = "";
        }
        
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
        String sleepStartSoundName = plugin.getConfig().getString("effects.sounds.sleep-start", "ENTITY_PLAYER_BREATH");
        sleepStartSound = parseSound(sleepStartSoundName);
        String nightSkipSoundName = plugin.getConfig().getString("effects.sounds.night-skip", "UI_TOAST_CHALLENGE_COMPLETE");
        nightSkipSound = parseSound(nightSkipSoundName);
        String countdownTickSoundName = plugin.getConfig().getString("effects.sounds.countdown-tick", "BLOCK_NOTE_BLOCK_HAT");
        countdownTickSound = parseSound(countdownTickSoundName);
        
        enabledWorlds = new HashSet<>(plugin.getConfig().getStringList("worlds.enabled"));
        worldsMode = plugin.getConfig().getString("worlds.mode", "whitelist");
        WorldSettings.WeatherSettings baseWeather = new WorldSettings.WeatherSettings(
                weatherChangeEnabled, clearRain, clearThunder);
        ConfigurationSection worldDefaultsSection = plugin.getConfig().getConfigurationSection("worlds.defaults");
        double defaultPercentage = readPercentage(worldDefaultsSection, sleepPercentage);
        List<SleepRule> defaultRules = readRules(worldDefaultsSection, dynamicRules);
        WorldSettings.WeatherSettings defaultWeather = readWeatherSettings(worldDefaultsSection, baseWeather);
        defaultWorldSettings = new WorldSettings(true, defaultPercentage, defaultRules, defaultWeather);

        worldOverrides.clear();
        worldEnableOverrides.clear();
        ConfigurationSection overridesSection = plugin.getConfig().getConfigurationSection("worlds.overrides");
        if (overridesSection != null) {
            for (String worldName : overridesSection.getKeys(false)) {
                ConfigurationSection overrideSection = overridesSection.getConfigurationSection(worldName);
                if (overrideSection == null) {
                    continue;
                }
                if (overrideSection.contains("enabled")) {
                    worldEnableOverrides.put(worldName, overrideSection.getBoolean("enabled"));
                }
                double overridePercentage = readPercentage(overrideSection, defaultWorldSettings.sleepPercentage());
                List<SleepRule> overrideRules = readRules(overrideSection, defaultWorldSettings.dynamicRules());
                WorldSettings.WeatherSettings overrideWeather = readWeatherSettings(overrideSection, defaultWorldSettings.weatherSettings());
                worldOverrides.put(worldName, new WorldSettings(true, overridePercentage, overrideRules, overrideWeather));
            }
        }
        
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
        leaderboardRefreshSeconds = plugin.getConfig().getInt("stats.leaderboard-refresh-seconds", 300);
        
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
            return Sound.ENTITY_PLAYER_BREATH;
        }
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name '" + name + "', using default sound");
            return Sound.ENTITY_PLAYER_BREATH;
        }
    }
    
    public String getSleepMode() {
        return sleepMode;
    }

    public List<SleepRule> getDynamicRules() {
        return dynamicRules == null ? List.of() : Collections.unmodifiableList(dynamicRules);
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

    public WorldSettings getWorldSettings(World world) {
        if (world == null) {
            return defaultWorldSettings;
        }
        return getWorldSettings(world.getName());
    }

    public WorldSettings getWorldSettings(String worldName) {
        if (worldName == null) {
            return defaultWorldSettings;
        }
        boolean baseEnabled = isWorldEnabledByMode(worldName);
        Boolean overrideEnabled = worldEnableOverrides.get(worldName);
        boolean enabled = overrideEnabled != null ? overrideEnabled : baseEnabled;
        WorldSettings override = worldOverrides.get(worldName);
        WorldSettings base = defaultWorldSettings == null
                ? new WorldSettings(true, sleepPercentage,
                dynamicRules == null ? List.of() : dynamicRules,
                new WorldSettings.WeatherSettings(weatherChangeEnabled, clearRain, clearThunder))
                : defaultWorldSettings;
        if (override == null) {
            return new WorldSettings(enabled, base.sleepPercentage(), base.dynamicRules(), base.weatherSettings());
        }
        return new WorldSettings(enabled, override.sleepPercentage(), override.dynamicRules(), override.weatherSettings());
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

    public boolean isAfkIndicatorEnabled() {
        return afkIndicatorEnabled;
    }

    public String getAfkIndicatorPrefix() {
        return afkIndicatorPrefix;
    }

    public String getAfkIndicatorSuffix() {
        return afkIndicatorSuffix;
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

    public int getLeaderboardRefreshSeconds() {
        return leaderboardRefreshSeconds;
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

    private List<SleepRule> parseRules(List<String> rawRules) {
        List<SleepRule> parsed = new ArrayList<>();
        if (rawRules == null) {
            return parsed;
        }
        for (String raw : rawRules) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            SleepRule.parse(raw).ifPresentOrElse(parsed::add, () ->
                    plugin.getLogger().warning("Invalid sleep rule: " + raw));
        }
        return parsed;
    }

    private double readPercentage(ConfigurationSection section, double fallback) {
        if (section == null) {
            return fallback;
        }
        if (section.contains("percentage")) {
            return section.getDouble("percentage", fallback);
        }
        if (section.contains("sleep-percentage")) {
            return section.getDouble("sleep-percentage", fallback);
        }
        return fallback;
    }

    private List<SleepRule> readRules(ConfigurationSection section, List<SleepRule> fallback) {
        if (section == null) {
            return fallback;
        }
        List<SleepRule> parsed = parseRules(section.getStringList("dynamic-rules"));
        return parsed.isEmpty() ? fallback : parsed;
    }

    private WorldSettings.WeatherSettings readWeatherSettings(ConfigurationSection section,
                                                             WorldSettings.WeatherSettings fallback) {
        if (section == null) {
            return fallback;
        }
        ConfigurationSection weather = section.getConfigurationSection("weather");
        if (weather == null) {
            return fallback;
        }
        boolean changeWeather = weather.getBoolean("change-weather", fallback.changeWeather());
        boolean nextClearRain = weather.getBoolean("clear-rain", fallback.clearRain());
        boolean nextClearThunder = weather.getBoolean("clear-thunder", fallback.clearThunder());
        return new WorldSettings.WeatherSettings(changeWeather, nextClearRain, nextClearThunder);
    }

    private boolean isWorldEnabledByMode(String worldName) {
        if (worldName == null) {
            return false;
        }
        boolean inList = enabledWorlds.contains(worldName);
        if ("whitelist".equalsIgnoreCase(worldsMode)) {
            return inList;
        } else if ("blacklist".equalsIgnoreCase(worldsMode)) {
            return !inList;
        }
        plugin.getLogger().warning("Unknown worlds.mode '" + worldsMode + "' - disabling world: " + worldName);
        return false;
    }
}
