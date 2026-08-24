package dev.eren.oneplayersleep.api;

import org.bukkit.World;

public interface OnePlayerSleepApi {
    SleepSnapshot getSnapshot(World world);

    int getRequiredPlayers(World world);

    boolean isCountdownActive(World world);

    void recalculate(World world);
}
