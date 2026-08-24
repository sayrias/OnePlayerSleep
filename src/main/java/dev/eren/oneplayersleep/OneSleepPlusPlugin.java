package dev.eren.oneplayersleep;

import dev.eren.oneplayersleep.api.OnePlayerSleepApi;
import dev.eren.oneplayersleep.api.OneSleepPlusApi;
import dev.eren.oneplayersleep.command.OneSleepCommand;
import dev.eren.oneplayersleep.config.PluginSettings;
import dev.eren.oneplayersleep.furnace.FurnaceCatchUpService;
import dev.eren.oneplayersleep.integration.BedrockDetector;
import dev.eren.oneplayersleep.integration.PlaceholderRegistrar;
import dev.eren.oneplayersleep.message.MessageService;
import dev.eren.oneplayersleep.platform.Platform;
import dev.eren.oneplayersleep.platform.SchedulerAdapter;
import dev.eren.oneplayersleep.sleep.SleepListener;
import dev.eren.oneplayersleep.sleep.SleepManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class OneSleepPlusPlugin extends JavaPlugin {
    private PluginSettings settings;
    private SchedulerAdapter scheduler;
    private BedrockDetector bedrockDetector;
    private MessageService messages;
    private FurnaceCatchUpService furnaceCatchUp;
    private SleepManager sleepManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureLanguageSetting();
        settings = new PluginSettings(this);
        settings.reload();
        scheduler = Platform.scheduler(this);
        bedrockDetector = new BedrockDetector(this, settings);
        messages = new MessageService(this, settings, scheduler, bedrockDetector);
        furnaceCatchUp = new FurnaceCatchUpService(this, settings, scheduler);
        sleepManager = new SleepManager(this, settings, scheduler, messages, furnaceCatchUp);

        getServer().getPluginManager().registerEvents(new SleepListener(sleepManager, settings), this);
        getServer().getPluginManager().registerEvents(furnaceCatchUp, this);
        getServer().getServicesManager().register(OneSleepPlusApi.class, sleepManager, this, ServicePriority.Normal);
        getServer().getServicesManager().register(OnePlayerSleepApi.class, sleepManager, this, ServicePriority.Normal);

        PluginCommand command = getCommand("onesleepplus");
        if (command == null) {
            throw new IllegalStateException("The onesleepplus command is missing from plugin.yml");
        }
        OneSleepCommand commandHandler = new OneSleepCommand(this, settings, sleepManager, messages, new Runnable() {
            @Override
            public void run() {
                reloadPlugin();
            }
        });
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        registerPlaceholderApi();
        getLogger().info("OneSleepPlus enabled on " + getServer().getName() + ' ' + getServer().getBukkitVersion()
                + (scheduler.isFolia() ? " (Folia mode)" : ""));
    }

    private void ensureLanguageSetting() {
        if (!getConfig().contains("language", true)) {
            getConfig().set("language", "en");
            saveConfig();
        }
    }

    private void reloadPlugin() {
        settings.reload();
        bedrockDetector.discoverHooks();
        messages.reloadMessages();
        messages.discoverPlaceholderApi();
        furnaceCatchUp.reloadRecipes();
        sleepManager.reload();
    }

    private void registerPlaceholderApi() {
        if (!settings.isPlaceholderApi() || getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            if (PlaceholderRegistrar.register(this, sleepManager, settings)) {
                getLogger().info("PlaceholderAPI expansions registered.");
            }
        } catch (Throwable exception) {
            getLogger().warning("PlaceholderAPI integration could not be enabled: " + exception.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (sleepManager != null) sleepManager.shutdown();
        getServer().getServicesManager().unregisterAll(this);
    }
}
