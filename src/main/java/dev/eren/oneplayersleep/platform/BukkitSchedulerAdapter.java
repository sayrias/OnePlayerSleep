package dev.eren.oneplayersleep.platform;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class BukkitSchedulerAdapter implements SchedulerAdapter {
    private final JavaPlugin plugin;

    public BukkitSchedulerAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public TaskHandle runLater(Runnable task, long delayTicks) {
        final BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskLater(plugin, task, Math.max(1L, delayTicks));
        return new TaskHandle() {
            @Override
            public void cancel() {
                bukkitTask.cancel();
            }
        };
    }

    @Override
    public void runForPlayer(Player player, Runnable task) {
        task.run();
    }

    @Override
    public void runAt(Location location, Runnable task) {
        task.run();
    }

    @Override
    public boolean isFolia() {
        return false;
    }
}
