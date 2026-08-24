package dev.eren.oneplayersleep.sleep;

import dev.eren.oneplayersleep.config.PluginSettings;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public final class SleepListener implements Listener {
    private final SleepManager sleepManager;
    private final PluginSettings settings;

    public SleepListener(SleepManager sleepManager, PluginSettings settings) {
        this.sleepManager = sleepManager;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        sleepManager.onSleepStarted(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedLeave(PlayerBedLeaveEvent event) {
        sleepManager.recalculate(event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVanillaNightSkip(TimeSkipEvent event) {
        if (TimeSkipReasonCompat.isNightSkip(event) && settings.isWorldEnabled(event.getWorld())) {
            event.setCancelled(true);
            sleepManager.recalculate(event.getWorld());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        sleepManager.recalculate(event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        sleepManager.recalculate(event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        sleepManager.recalculate(event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        sleepManager.recalculate(event.getEntity().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        sleepManager.recalculate(event.getFrom());
        sleepManager.recalculate(event.getPlayer().getWorld());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        sleepManager.forgetWorld(event.getWorld());
    }
}
