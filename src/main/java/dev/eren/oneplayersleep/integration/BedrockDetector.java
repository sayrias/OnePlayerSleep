package dev.eren.oneplayersleep.integration;

import dev.eren.oneplayersleep.config.PluginSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class BedrockDetector {
    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private Method floodgateGetInstance;
    private Method floodgateIsPlayer;
    private Method geyserApi;
    private Method geyserConnection;

    public BedrockDetector(JavaPlugin plugin, PluginSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        discoverHooks();
    }

    public void discoverHooks() {
        floodgateGetInstance = null;
        floodgateIsPlayer = null;
        geyserApi = null;
        geyserConnection = null;

        if (settings.isFloodgate()) {
            try {
                Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                floodgateGetInstance = api.getMethod("getInstance");
                floodgateIsPlayer = api.getMethod("isFloodgatePlayer", UUID.class);
                plugin.getLogger().info("Floodgate integration enabled.");
            } catch (ReflectiveOperationException ignored) {
                // Floodgate is optional.
            }
        }

        if (settings.isGeyser()) {
            try {
                Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi");
                geyserApi = api.getMethod("api");
                geyserConnection = api.getMethod("connectionByUuid", UUID.class);
                plugin.getLogger().info("Geyser integration enabled.");
            } catch (ReflectiveOperationException ignored) {
                // Geyser may be installed on a proxy instead of this backend.
            }
        }
    }

    public boolean isBedrockPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (floodgateGetInstance != null && floodgateIsPlayer != null) {
            try {
                Object api = floodgateGetInstance.invoke(null);
                if (Boolean.TRUE.equals(floodgateIsPlayer.invoke(api, uuid))) {
                    return true;
                }
            } catch (ReflectiveOperationException ignored) {
                // Fall through to Geyser detection.
            }
        }

        if (geyserApi != null && geyserConnection != null) {
            try {
                Object api = geyserApi.invoke(null);
                return api != null && geyserConnection.invoke(api, uuid) != null;
            } catch (ReflectiveOperationException ignored) {
                return false;
            }
        }
        return false;
    }
}
