package dev.eren.oneplayersleep.api;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SleepSnapshot {
    private final World world;
    private final List<Player> eligiblePlayers;
    private final List<Player> sleepingPlayers;
    private final int requiredPlayers;
    private final String configuredThreshold;
    private final boolean thresholdClamped;

    public SleepSnapshot(World world, List<Player> eligiblePlayers, List<Player> sleepingPlayers,
                         int requiredPlayers, String configuredThreshold, boolean thresholdClamped) {
        this.world = world;
        this.eligiblePlayers = Collections.unmodifiableList(new ArrayList<Player>(eligiblePlayers));
        this.sleepingPlayers = Collections.unmodifiableList(new ArrayList<Player>(sleepingPlayers));
        this.requiredPlayers = requiredPlayers;
        this.configuredThreshold = configuredThreshold;
        this.thresholdClamped = thresholdClamped;
    }

    public World getWorld() { return world; }
    public List<Player> getEligiblePlayers() { return eligiblePlayers; }
    public List<Player> getSleepingPlayers() { return sleepingPlayers; }
    public int getEligibleCount() { return eligiblePlayers.size(); }
    public int getSleepingCount() { return sleepingPlayers.size(); }
    public int getRequiredPlayers() { return requiredPlayers; }
    public int getRemainingPlayers() { return Math.max(0, requiredPlayers - sleepingPlayers.size()); }
    public String getConfiguredThreshold() { return configuredThreshold; }
    public boolean isThresholdClamped() { return thresholdClamped; }
    public boolean isThresholdReached() { return requiredPlayers > 0 && sleepingPlayers.size() >= requiredPlayers; }
}
