package dev.eren.oneplayersleep.message;

import dev.eren.oneplayersleep.api.SleepSnapshot;
import dev.eren.oneplayersleep.config.PluginSettings;
import dev.eren.oneplayersleep.integration.BedrockDetector;
import dev.eren.oneplayersleep.platform.SchedulerAdapter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageService {
    private static final Pattern HEX = Pattern.compile("&?#([A-Fa-f0-9]{6})");
    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final SchedulerAdapter scheduler;
    private final BedrockDetector bedrockDetector;
    private YamlConfiguration englishMessages;
    private YamlConfiguration activeMessages;
    private String loadedLanguage = "en";
    private Method papiSetPlaceholders;

    public MessageService(JavaPlugin plugin, PluginSettings settings, SchedulerAdapter scheduler,
                          BedrockDetector bedrockDetector) {
        this.plugin = plugin;
        this.settings = settings;
        this.scheduler = scheduler;
        this.bedrockDetector = bedrockDetector;
        ensureBundledLanguageFiles();
        reloadMessages();
        discoverPlaceholderApi();
    }

    public void reloadMessages() {
        ensureBundledLanguageFiles();
        File languageFolder = new File(plugin.getDataFolder(), "lang");
        englishMessages = loadMessages(new File(languageFolder, "messages_en.yml"));

        String requestedLanguage = settings.getLanguage();
        File requestedFile = new File(languageFolder, "messages_" + requestedLanguage + ".yml");
        if (!requestedFile.isFile()) {
            plugin.getLogger().warning("Language file " + requestedFile.getName()
                    + " was not found; using messages_en.yml.");
            activeMessages = englishMessages;
            loadedLanguage = "en";
            return;
        }

        if (requestedLanguage.equals("en")) {
            activeMessages = englishMessages;
        } else {
            YamlConfiguration selected = loadMessages(requestedFile);
            activeMessages = selected.getKeys(true).isEmpty() ? englishMessages : selected;
        }
        loadedLanguage = activeMessages == englishMessages ? "en" : requestedLanguage;
        plugin.getLogger().info("Language loaded: " + loadedLanguage + '.');
    }

    private void ensureBundledLanguageFiles() {
        saveBundledLanguage("lang/messages_en.yml");
        saveBundledLanguage("lang/messages_tr.yml");
    }

    private void saveBundledLanguage(String resourcePath) {
        File target = new File(plugin.getDataFolder(), resourcePath);
        if (!target.isFile()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private YamlConfiguration loadMessages(File file) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not read " + file.getName() + ": " + exception.getMessage());
        } catch (InvalidConfigurationException exception) {
            plugin.getLogger().warning("Invalid YAML in " + file.getName() + ": " + exception.getMessage());
        }
        return configuration;
    }

    public void discoverPlaceholderApi() {
        papiSetPlaceholders = null;
        if (!settings.isPlaceholderApi()) {
            return;
        }
        try {
            Class<?> placeholderApi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            papiSetPlaceholders = placeholderApi.getMethod("setPlaceholders", Player.class, String.class);
        } catch (ReflectiveOperationException ignored) {
            // PlaceholderAPI is optional.
        }
    }

    public void broadcast(final World world, final String messageKey, final SleepSnapshot snapshot,
                          String actorName, Integer seconds) {
        final Map<String, String> replacements = snapshotReplacements(snapshot);
        replacements.put("player", actorName == null ? "-" : actorName);
        replacements.put("seconds", seconds == null ? "0" : String.valueOf(seconds));
        for (final Player player : world.getPlayers()) {
            scheduler.runForPlayer(player, new Runnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        sendNotification(player, messageKey, replacements);
                    }
                }
            });
        }
    }

    public void sendNotification(Player player, String messageKey, Map<String, String> replacements) {
        String resolvedKey = bedrockMessageKey(player, messageKey);
        String raw = getMessage(resolvedKey, null);
        if (raw == null || raw.isEmpty()) {
            return;
        }
        String message = render(player, raw, replacements);

        if (settings.isActionBar()) {
            try {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            } catch (Throwable unsupported) {
                player.sendMessage(message);
            }
        }
        if (settings.isChat()) {
            player.sendMessage(message);
        }
        if (settings.isSound()) {
            playSound(player);
        }
    }

    public void sendChat(CommandSender sender, String messageKey) {
        sendChat(sender, messageKey, Collections.<String, String>emptyMap());
    }

    public void sendChat(CommandSender sender, String messageKey, Map<String, String> replacements) {
        sendChat(sender, messageKey, replacements, true);
    }

    public void sendUnprefixedChat(CommandSender sender, String messageKey, Map<String, String> replacements) {
        sendChat(sender, messageKey, replacements, false);
    }

    private void sendChat(CommandSender sender, String messageKey, Map<String, String> replacements,
                          boolean includePrefix) {
        String raw = getMessage(messageKey, messageKey);
        String prefix = includePrefix ? getMessage("prefix", "") : "";
        Player player = sender instanceof Player ? (Player) sender : null;
        sender.sendMessage(render(player, prefix + raw, replacements));
    }

    public void sendActionBar(Player player, String messageKey, Map<String, String> replacements) {
        String raw = getMessage(bedrockMessageKey(player, messageKey), messageKey);
        String message = render(player, raw, replacements);
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        } catch (Throwable unsupported) {
            player.sendMessage(message);
        }
    }

    public Map<String, String> snapshotReplacements(SleepSnapshot snapshot) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("world", snapshot.getWorld().getName());
        values.put("sleeping", String.valueOf(snapshot.getSleepingCount()));
        values.put("required", String.valueOf(snapshot.getRequiredPlayers()));
        values.put("remaining", String.valueOf(snapshot.getRemainingPlayers()));
        values.put("eligible", String.valueOf(snapshot.getEligibleCount()));
        values.put("threshold", snapshot.getConfiguredThreshold());
        return values;
    }

    public String getMessage(String messageKey, String defaultValue) {
        String value = activeMessages == null ? null : activeMessages.getString(messageKey);
        if (value == null && englishMessages != null && activeMessages != englishMessages) {
            value = englishMessages.getString(messageKey);
        }
        return value == null ? defaultValue : value;
    }

    private String bedrockMessageKey(Player player, String messageKey) {
        if (!settings.isBedrockSimpleMessages() || !bedrockDetector.isBedrockPlayer(player)) {
            return messageKey;
        }
        String candidate = "bedrock-" + messageKey;
        return hasMessage(candidate) ? candidate : messageKey;
    }

    private boolean hasMessage(String messageKey) {
        return (activeMessages != null && activeMessages.contains(messageKey))
                || (englishMessages != null && englishMessages.contains(messageKey));
    }

    private void playSound(Player player) {
        try {
            Sound sound = Sound.valueOf(settings.getSoundName().toUpperCase(java.util.Locale.ROOT));
            player.playSound(player.getLocation(), sound, settings.getSoundVolume(), settings.getSoundPitch());
        } catch (IllegalArgumentException ignored) {
            // A sound may have been renamed between Minecraft versions.
        }
    }

    private String render(Player player, String input, Map<String, String> replacements) {
        String output = input;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            output = output.replace('{' + entry.getKey() + '}', entry.getValue());
        }
        if (player != null && papiSetPlaceholders != null) {
            try {
                output = String.valueOf(papiSetPlaceholders.invoke(null, player, output));
            } catch (ReflectiveOperationException ignored) {
                // Keep the original message if another expansion fails.
            }
        }
        return colorize(output);
    }

    static String colorize(String input) {
        Matcher matcher = HEX.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char character : hex.toCharArray()) {
                replacement.append('§').append(character);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
