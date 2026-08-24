package dev.eren.oneplayersleep.api.event;

import dev.eren.oneplayersleep.api.SleepSnapshot;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class NightSkipEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final SleepSnapshot snapshot;
    private long skippedTicks;
    private boolean cancelled;

    public NightSkipEvent(SleepSnapshot snapshot, long skippedTicks) {
        this.snapshot = snapshot;
        this.skippedTicks = skippedTicks;
    }

    public SleepSnapshot getSnapshot() { return snapshot; }
    public long getSkippedTicks() { return skippedTicks; }

    public void setSkippedTicks(long skippedTicks) {
        this.skippedTicks = Math.max(0L, skippedTicks);
    }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
