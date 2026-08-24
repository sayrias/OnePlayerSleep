package dev.eren.oneplayersleep.api.event;

import dev.eren.oneplayersleep.api.SleepSnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class NightSkippedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final SleepSnapshot snapshot;
    private final long skippedTicks;
    private final int processedFurnaces;

    public NightSkippedEvent(SleepSnapshot snapshot, long skippedTicks, int processedFurnaces) {
        this.snapshot = snapshot;
        this.skippedTicks = skippedTicks;
        this.processedFurnaces = processedFurnaces;
    }

    public SleepSnapshot getSnapshot() { return snapshot; }
    public long getSkippedTicks() { return skippedTicks; }
    public int getProcessedFurnaces() { return processedFurnaces; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
