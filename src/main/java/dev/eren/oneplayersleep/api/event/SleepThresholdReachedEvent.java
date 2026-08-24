package dev.eren.oneplayersleep.api.event;

import dev.eren.oneplayersleep.api.SleepSnapshot;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class SleepThresholdReachedEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final SleepSnapshot snapshot;
    private boolean cancelled;

    public SleepThresholdReachedEvent(SleepSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public SleepSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
