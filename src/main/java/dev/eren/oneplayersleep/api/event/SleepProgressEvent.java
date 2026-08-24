package dev.eren.oneplayersleep.api.event;

import dev.eren.oneplayersleep.api.SleepSnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class SleepProgressEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final SleepSnapshot snapshot;

    public SleepProgressEvent(SleepSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public SleepSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
