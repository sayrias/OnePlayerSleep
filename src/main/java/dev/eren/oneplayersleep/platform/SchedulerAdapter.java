package dev.eren.oneplayersleep.platform;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface SchedulerAdapter {
    TaskHandle runLater(Runnable task, long delayTicks);

    void runForPlayer(Player player, Runnable task);

    void runAt(Location location, Runnable task);

    boolean isFolia();
}
