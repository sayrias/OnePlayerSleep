package dev.eren.oneplayersleep.platform;

import org.bukkit.plugin.java.JavaPlugin;

public final class Platform {
    private Platform() {
    }

    public static SchedulerAdapter scheduler(JavaPlugin plugin) {
        if (classExists("io.papermc.paper.threadedregions.RegionizedServer")) {
            try {
                plugin.getLogger().info("Folia detected; region-aware scheduler enabled.");
                return new FoliaSchedulerAdapter(plugin);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Folia was detected but its scheduler could not be initialized", exception);
            }
        }
        return new BukkitSchedulerAdapter(plugin);
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, Platform.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
