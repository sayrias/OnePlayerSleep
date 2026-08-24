package dev.eren.oneplayersleep.platform;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class FoliaSchedulerAdapter implements SchedulerAdapter {
    private final JavaPlugin plugin;
    private final Object globalScheduler;
    private final Object regionScheduler;

    public FoliaSchedulerAdapter(JavaPlugin plugin) throws ReflectiveOperationException {
        this.plugin = plugin;
        Server server = plugin.getServer();
        globalScheduler = server.getClass().getMethod("getGlobalRegionScheduler").invoke(server);
        regionScheduler = server.getClass().getMethod("getRegionScheduler").invoke(server);
    }

    @Override
    public TaskHandle runLater(final Runnable task, long delayTicks) {
        try {
            Method method = findMethod(globalScheduler.getClass(), "runDelayed", 3);
            Object scheduled = method.invoke(globalScheduler, plugin, new Consumer<Object>() {
                @Override
                public void accept(Object ignored) {
                    task.run();
                }
            }, Math.max(1L, delayTicks));
            return reflectiveHandle(scheduled);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to schedule a Folia global task", exception);
        }
    }

    @Override
    public void runForPlayer(Player player, final Runnable task) {
        try {
            Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
            Method run = findMethod(scheduler.getClass(), "run", 3);
            run.invoke(scheduler, plugin, new Consumer<Object>() {
                @Override
                public void accept(Object ignored) {
                    task.run();
                }
            }, null);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Unable to schedule a Folia player task: " + exception.getMessage());
        }
    }

    @Override
    public void runAt(Location location, Runnable task) {
        try {
            Method execute = findExecuteMethod(regionScheduler.getClass());
            execute.invoke(regionScheduler, plugin, location, task);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Unable to schedule a Folia region task: " + exception.getMessage());
        }
    }

    @Override
    public boolean isFolia() {
        return true;
    }

    private static TaskHandle reflectiveHandle(final Object scheduled) {
        return new TaskHandle() {
            @Override
            public void cancel() {
                try {
                    scheduled.getClass().getMethod("cancel").invoke(scheduled);
                } catch (ReflectiveOperationException ignored) {
                    // The task may already be complete.
                }
            }
        };
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == parameterCount) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + '#' + name);
    }

    private static Method findExecuteMethod(Class<?> type) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals("execute") && method.getParameterTypes().length == 3
                    && Plugin.class.isAssignableFrom(method.getParameterTypes()[0])) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "#execute");
    }
}
