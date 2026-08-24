package dev.eren.oneplayersleep.integration;

import dev.eren.oneplayersleep.config.PluginSettings;
import dev.eren.oneplayersleep.sleep.SleepManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlaceholderRegistrar {
    private PlaceholderRegistrar() {
    }

    public static boolean register(JavaPlugin plugin, SleepManager sleepManager, PluginSettings settings) {
        boolean branded = new OnePlayerSleepExpansion(plugin, sleepManager, settings, "onesleepplus").register();
        new OnePlayerSleepExpansion(plugin, sleepManager, settings, "oneplayersleep").register();
        return branded;
    }
}
