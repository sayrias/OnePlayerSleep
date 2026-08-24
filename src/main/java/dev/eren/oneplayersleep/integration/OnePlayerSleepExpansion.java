package dev.eren.oneplayersleep.integration;

import dev.eren.oneplayersleep.api.SleepSnapshot;
import dev.eren.oneplayersleep.config.PluginSettings;
import dev.eren.oneplayersleep.sleep.SleepManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class OnePlayerSleepExpansion extends PlaceholderExpansion {
    private final JavaPlugin plugin;
    private final SleepManager sleepManager;
    private final PluginSettings settings;
    private final String identifier;

    public OnePlayerSleepExpansion(JavaPlugin plugin, SleepManager sleepManager, PluginSettings settings,
                                   String identifier) {
        this.plugin = plugin;
        this.sleepManager = sleepManager;
        this.settings = settings;
        this.identifier = identifier;
    }

    @Override
    public @NotNull String getIdentifier() {
        return identifier;
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String parameter) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) return "";
        Player player = offlinePlayer.getPlayer();
        if (player == null) return "";
        World world = player.getWorld();
        SleepSnapshot snapshot = sleepManager.getSnapshot(world);

        if (parameter.equalsIgnoreCase("sleeping")) return String.valueOf(snapshot.getSleepingCount());
        if (parameter.equalsIgnoreCase("required")) return String.valueOf(snapshot.getRequiredPlayers());
        if (parameter.equalsIgnoreCase("remaining")) return String.valueOf(snapshot.getRemainingPlayers());
        if (parameter.equalsIgnoreCase("eligible")) return String.valueOf(snapshot.getEligibleCount());
        if (parameter.equalsIgnoreCase("threshold")) return snapshot.getConfiguredThreshold();
        if (parameter.equalsIgnoreCase("countdown")) return String.valueOf(sleepManager.getCountdownSeconds(world));
        if (parameter.equalsIgnoreCase("world_enabled")) return String.valueOf(settings.isWorldEnabled(world));
        if (parameter.equalsIgnoreCase("progress")) {
            if (snapshot.getRequiredPlayers() <= 0) return "0";
            return String.valueOf(Math.min(100, snapshot.getSleepingCount() * 100 / snapshot.getRequiredPlayers()));
        }
        return null;
    }
}
