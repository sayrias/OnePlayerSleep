package dev.eren.oneplayersleep.sleep;

import dev.eren.oneplayersleep.api.OneSleepPlusApi;
import dev.eren.oneplayersleep.api.SleepSnapshot;
import dev.eren.oneplayersleep.api.event.NightSkipEvent;
import dev.eren.oneplayersleep.api.event.NightSkippedEvent;
import dev.eren.oneplayersleep.api.event.SleepProgressEvent;
import dev.eren.oneplayersleep.api.event.SleepThresholdReachedEvent;
import dev.eren.oneplayersleep.config.PluginSettings;
import dev.eren.oneplayersleep.furnace.FurnaceCatchUpService;
import dev.eren.oneplayersleep.message.MessageService;
import dev.eren.oneplayersleep.platform.SchedulerAdapter;
import dev.eren.oneplayersleep.platform.TaskHandle;
import dev.eren.oneplayersleep.threshold.SleepThreshold;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SleepManager implements OneSleepPlusApi {
    private static final long NIGHT_START = 12542L;
    private static final long NIGHT_END = 23460L;

    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final SchedulerAdapter scheduler;
    private final MessageService messages;
    private final FurnaceCatchUpService furnaceCatchUp;
    private final Map<UUID, CountdownState> countdowns = new ConcurrentHashMap<UUID, CountdownState>();
    private volatile boolean shuttingDown;

    public SleepManager(JavaPlugin plugin, PluginSettings settings, SchedulerAdapter scheduler,
                        MessageService messages, FurnaceCatchUpService furnaceCatchUp) {
        this.plugin = plugin;
        this.settings = settings;
        this.scheduler = scheduler;
        this.messages = messages;
        this.furnaceCatchUp = furnaceCatchUp;
    }

    public void onSleepStarted(final Player player) {
        final World world = player.getWorld();
        scheduler.runLater(new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.getWorld() != world || !player.isSleeping()) {
                    recalculateNow(world);
                    return;
                }
                SleepSnapshot snapshot = snapshot(world);
                messages.broadcast(world, "sleep-started", snapshot, player.getName(), null);
                evaluate(snapshot);
            }
        }, 1L);
    }

    @Override
    public void recalculate(final World world) {
        if (world == null || shuttingDown) return;
        scheduler.runLater(new Runnable() {
            @Override
            public void run() {
                recalculateNow(world);
            }
        }, 1L);
    }

    public void recalculateNow(World world) {
        if (world == null || shuttingDown) return;
        SleepSnapshot snapshot = snapshot(world);
        Bukkit.getPluginManager().callEvent(new SleepProgressEvent(snapshot));
        evaluate(snapshot);
    }

    private void evaluate(SleepSnapshot snapshot) {
        World world = snapshot.getWorld();
        if (!settings.isWorldEnabled(world) || !canSleepNow(world) || !snapshot.isThresholdReached()) {
            cancelCountdown(world, countdowns.containsKey(world.getUID()));
            return;
        }
        if (!countdowns.containsKey(world.getUID())) {
            beginCountdown(snapshot);
        }
    }

    private void beginCountdown(SleepSnapshot snapshot) {
        SleepThresholdReachedEvent event = new SleepThresholdReachedEvent(snapshot);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        World world = snapshot.getWorld();
        CountdownState state = new CountdownState(settings.getCountdownSeconds());
        CountdownState previous = countdowns.putIfAbsent(world.getUID(), state);
        if (previous != null) return;

        if (state.secondsRemaining <= 0) {
            messages.broadcast(world, "threshold-reached", snapshot, null, 0);
            completeNightSkip(world, state);
            return;
        }
        tickCountdown(world, state);
    }

    private void tickCountdown(final World world, final CountdownState state) {
        if (countdowns.get(world.getUID()) != state || shuttingDown) return;
        SleepSnapshot snapshot = snapshot(world);
        if (!settings.isWorldEnabled(world) || !canSleepNow(world) || !snapshot.isThresholdReached()) {
            cancelCountdown(world, true);
            return;
        }
        if (state.secondsRemaining <= 0) {
            completeNightSkip(world, state);
            return;
        }

        messages.broadcast(world, "countdown", snapshot, null, state.secondsRemaining);
        state.secondsRemaining--;
        state.task = scheduler.runLater(new Runnable() {
            @Override
            public void run() {
                tickCountdown(world, state);
            }
        }, 20L);
    }

    private void completeNightSkip(World world, CountdownState state) {
        if (!countdowns.remove(world.getUID(), state)) return;
        SleepSnapshot snapshot = snapshot(world);
        if (!snapshot.isThresholdReached() || !canSleepNow(world)) return;

        long skippedTicks = isNight(world)
                ? NightTimeMath.ticksUntil(world.getTime(), settings.getMorningTime())
                : 0L;
        NightSkipEvent event = new NightSkipEvent(snapshot, skippedTicks);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        skippedTicks = event.getSkippedTicks();

        if (skippedTicks > 0L) {
            world.setFullTime(world.getFullTime() + skippedTicks);
        }
        if (settings.isResetWeather()) {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(0);
            world.setThunderDuration(0);
        }

        for (final Player sleeper : snapshot.getSleepingPlayers()) {
            scheduler.runForPlayer(sleeper, new Runnable() {
                @Override
                public void run() {
                    if (sleeper.isOnline() && sleeper.isSleeping()) {
                        try {
                            sleeper.wakeup(false);
                        } catch (IllegalStateException ignored) {
                            // The server may already have woken the player due to the time change.
                        }
                    }
                }
            });
        }

        int processedFurnaces = furnaceCatchUp.catchUp(world, skippedTicks);
        messages.broadcast(world, "morning", snapshot, null, 0);
        Bukkit.getPluginManager().callEvent(new NightSkippedEvent(snapshot, skippedTicks, processedFurnaces));
    }

    private void cancelCountdown(World world, boolean announce) {
        CountdownState state = countdowns.remove(world.getUID());
        if (state == null) return;
        if (state.task != null) state.task.cancel();
        if (announce && settings.isWorldEnabled(world)) {
            messages.broadcast(world, "countdown-cancelled", snapshot(world), null, null);
        }
    }

    public void reload() {
        for (World world : Bukkit.getWorlds()) {
            cancelCountdown(world, false);
            recalculate(world);
        }
    }

    public void shutdown() {
        shuttingDown = true;
        for (CountdownState state : countdowns.values()) {
            if (state.task != null) state.task.cancel();
        }
        countdowns.clear();
    }

    @Override
    public SleepSnapshot getSnapshot(World world) {
        return snapshot(world);
    }

    private SleepSnapshot snapshot(World world) {
        List<Player> eligible = new ArrayList<Player>();
        List<Player> sleeping = new ArrayList<Player>();
        for (Player player : world.getPlayers()) {
            if (!isEligible(player)) continue;
            eligible.add(player);
            if (player.isSleeping()) sleeping.add(player);
        }
        SleepThreshold threshold = settings.threshold(world);
        int required = threshold.requiredPlayers(eligible.size());
        return new SleepSnapshot(world, eligible, sleeping, required, threshold.asConfigValue(),
                threshold.isClamped(eligible.size()));
    }

    private boolean isEligible(Player player) {
        if (!player.isOnline() || player.isDead() || player.getGameMode() == GameMode.SPECTATOR
                || player.hasPermission("onesleepplus.ignore")) {
            return false;
        }
        return settings.isIncludeCreative() || player.getGameMode() != GameMode.CREATIVE;
    }

    public boolean canSleepNow(World world) {
        if (!settings.isRequireNightOrThunder()) return true;
        return isNight(world) || world.isThundering();
    }

    private static boolean isNight(World world) {
        long time = world.getTime();
        return time >= NIGHT_START && time < NIGHT_END;
    }

    @Override
    public int getRequiredPlayers(World world) {
        return snapshot(world).getRequiredPlayers();
    }

    @Override
    public boolean isCountdownActive(World world) {
        return world != null && countdowns.containsKey(world.getUID());
    }

    public int getCountdownSeconds(World world) {
        CountdownState state = world == null ? null : countdowns.get(world.getUID());
        return state == null ? 0 : Math.max(0, state.secondsRemaining);
    }

    public void forgetWorld(World world) {
        if (world != null) cancelCountdown(world, false);
    }

    private static final class CountdownState {
        private volatile int secondsRemaining;
        private volatile TaskHandle task;

        private CountdownState(int secondsRemaining) {
            this.secondsRemaining = secondsRemaining;
        }
    }
}
