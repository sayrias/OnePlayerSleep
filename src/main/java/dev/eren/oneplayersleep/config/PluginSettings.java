package dev.eren.oneplayersleep.config;

import dev.eren.oneplayersleep.threshold.PercentageRounding;
import dev.eren.oneplayersleep.threshold.SleepThreshold;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class PluginSettings {
    private final JavaPlugin plugin;
    private String language;
    private SleepThreshold defaultThreshold;
    private PercentageRounding rounding;
    private int countdownSeconds;
    private long morningTime;
    private boolean resetWeather;
    private boolean includeCreative;
    private boolean requireNightOrThunder;
    private boolean defaultWorldEnabled;
    private Set<String> disabledWorlds = Collections.emptySet();
    private Map<String, WorldOverride> worldOverrides = Collections.emptyMap();

    private boolean actionBar;
    private boolean chat;
    private boolean sound;
    private String soundName;
    private float soundVolume;
    private float soundPitch;

    private boolean furnaceCatchUp;
    private int maxFurnacesPerSkip;
    private long maxSkippedTicks;
    private boolean catchUpFurnace;
    private boolean catchUpSmoker;
    private boolean catchUpBlastFurnace;
    private Map<String, Integer> customFuelBurnTimes = Collections.emptyMap();

    private boolean placeholderApi;
    private boolean geyser;
    private boolean floodgate;
    private boolean bedrockSimpleMessages;

    public PluginSettings(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        String configuredLanguage = config.getString("language", "en");
        language = configuredLanguage == null ? "en" : configuredLanguage.trim().toLowerCase(Locale.ROOT);
        if (!language.matches("[a-z0-9_-]{2,32}")) {
            plugin.getLogger().warning("Invalid language code '" + configuredLanguage + "'; using en.");
            language = "en";
        }

        rounding = PercentageRounding.parse(config.getString("sleep.percentage-rounding", "CEIL"));
        String configuredRounding = config.getString("sleep.percentage-rounding", "CEIL");
        if (!rounding.name().equalsIgnoreCase(configuredRounding)) {
            plugin.getLogger().warning("Unknown percentage rounding '" + configuredRounding + "'; using CEIL.");
        }
        defaultThreshold = parseThreshold(config.get("sleep.threshold"), "sleep.threshold", SleepThreshold.parse("1", rounding));
        countdownSeconds = clamp(config.getInt("sleep.countdown-seconds", 3), 0, 60);
        morningTime = Math.floorMod(config.getLong("sleep.morning-time", 0L), 24000L);
        resetWeather = config.getBoolean("sleep.reset-weather", true);
        includeCreative = config.getBoolean("sleep.include-creative", true);
        requireNightOrThunder = config.getBoolean("sleep.require-night-or-thunder", true);

        defaultWorldEnabled = config.getBoolean("worlds.default-enabled", true);
        Set<String> disabled = new HashSet<String>();
        for (String world : config.getStringList("worlds.disabled")) {
            disabled.add(world.toLowerCase(Locale.ROOT));
        }
        disabledWorlds = Collections.unmodifiableSet(disabled);

        Map<String, WorldOverride> overrides = new HashMap<String, WorldOverride>();
        ConfigurationSection section = config.getConfigurationSection("worlds.overrides");
        if (section != null) {
            for (String worldName : section.getKeys(false)) {
                String base = "worlds.overrides." + worldName;
                boolean enabled = config.getBoolean(base + ".enabled", defaultWorldEnabled);
                SleepThreshold threshold = defaultThreshold;
                if (config.contains(base + ".threshold")) {
                    threshold = parseThreshold(config.get(base + ".threshold"), base + ".threshold", defaultThreshold);
                }
                overrides.put(worldName.toLowerCase(Locale.ROOT), new WorldOverride(enabled, threshold));
            }
        }
        worldOverrides = Collections.unmodifiableMap(overrides);

        actionBar = config.getBoolean("notifications.action-bar", true);
        chat = config.getBoolean("notifications.chat", false);
        sound = config.getBoolean("notifications.sound", true);
        soundName = config.getString("notifications.sound-name", "BLOCK_NOTE_BLOCK_CHIME");
        soundVolume = (float) Math.max(0.0D, config.getDouble("notifications.sound-volume", 0.8D));
        soundPitch = (float) Math.max(0.0D, config.getDouble("notifications.sound-pitch", 1.2D));

        furnaceCatchUp = config.getBoolean("furnace-catch-up.enabled", true);
        maxFurnacesPerSkip = clamp(config.getInt("furnace-catch-up.max-furnaces-per-skip", 500), 0, 10000);
        maxSkippedTicks = clamp(config.getLong("furnace-catch-up.max-skipped-ticks", 12000L), 0L, 24000L);
        catchUpFurnace = config.getBoolean("furnace-catch-up.furnace", true);
        catchUpSmoker = config.getBoolean("furnace-catch-up.smoker", true);
        catchUpBlastFurnace = config.getBoolean("furnace-catch-up.blast-furnace", true);
        Map<String, Integer> customFuels = new HashMap<String, Integer>();
        ConfigurationSection customFuelSection = config.getConfigurationSection("furnace-catch-up.custom-fuel-burn-times");
        if (customFuelSection != null) {
            for (String material : customFuelSection.getKeys(false)) {
                int burnTicks = customFuelSection.getInt(material, 0);
                if (burnTicks > 0) {
                    customFuels.put(material.toUpperCase(Locale.ROOT), burnTicks);
                } else {
                    plugin.getLogger().warning("Ignoring non-positive custom fuel time for " + material + '.');
                }
            }
        }
        customFuelBurnTimes = Collections.unmodifiableMap(customFuels);

        placeholderApi = config.getBoolean("integrations.placeholder-api", true);
        geyser = config.getBoolean("integrations.geyser", true);
        floodgate = config.getBoolean("integrations.floodgate", true);
        bedrockSimpleMessages = config.getBoolean("integrations.bedrock-simple-messages", true);
    }

    private SleepThreshold parseThreshold(Object raw, String path, SleepThreshold fallback) {
        try {
            return SleepThreshold.parse(raw, rounding);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().log(Level.WARNING, "Invalid threshold at " + path + " ('" + raw + "'): "
                    + exception.getMessage() + ". Using " + fallback.asConfigValue() + '.');
            return fallback;
        }
    }

    public SleepThreshold threshold(World world) {
        WorldOverride override = worldOverrides.get(world.getName().toLowerCase(Locale.ROOT));
        return override == null ? defaultThreshold : override.threshold;
    }

    public boolean isWorldEnabled(World world) {
        String key = world.getName().toLowerCase(Locale.ROOT);
        WorldOverride override = worldOverrides.get(key);
        if (override != null) {
            return override.enabled;
        }
        return defaultWorldEnabled && !disabledWorlds.contains(key);
    }

    public String getLanguage() { return language; }
    public int getCountdownSeconds() { return countdownSeconds; }
    public PercentageRounding getRounding() { return rounding; }
    public long getMorningTime() { return morningTime; }
    public boolean isResetWeather() { return resetWeather; }
    public boolean isIncludeCreative() { return includeCreative; }
    public boolean isRequireNightOrThunder() { return requireNightOrThunder; }
    public boolean isActionBar() { return actionBar; }
    public boolean isChat() { return chat; }
    public boolean isSound() { return sound; }
    public String getSoundName() { return soundName; }
    public float getSoundVolume() { return soundVolume; }
    public float getSoundPitch() { return soundPitch; }
    public boolean isFurnaceCatchUp() { return furnaceCatchUp; }
    public int getMaxFurnacesPerSkip() { return maxFurnacesPerSkip; }
    public long getMaxSkippedTicks() { return maxSkippedTicks; }
    public boolean isCatchUpFurnace() { return catchUpFurnace; }
    public boolean isCatchUpSmoker() { return catchUpSmoker; }
    public boolean isCatchUpBlastFurnace() { return catchUpBlastFurnace; }
    public Map<String, Integer> getCustomFuelBurnTimes() { return customFuelBurnTimes; }
    public boolean isPlaceholderApi() { return placeholderApi; }
    public boolean isGeyser() { return geyser; }
    public boolean isFloodgate() { return floodgate; }
    public boolean isBedrockSimpleMessages() { return bedrockSimpleMessages; }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class WorldOverride {
        private final boolean enabled;
        private final SleepThreshold threshold;

        private WorldOverride(boolean enabled, SleepThreshold threshold) {
            this.enabled = enabled;
            this.threshold = threshold;
        }
    }
}
