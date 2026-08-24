package dev.eren.oneplayersleep.command;

import dev.eren.oneplayersleep.api.SleepSnapshot;
import dev.eren.oneplayersleep.config.PluginSettings;
import dev.eren.oneplayersleep.message.MessageService;
import dev.eren.oneplayersleep.sleep.SleepManager;
import dev.eren.oneplayersleep.threshold.SleepThreshold;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OneSleepCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final SleepManager sleepManager;
    private final MessageService messages;
    private final Runnable reloadAction;

    public OneSleepCommand(JavaPlugin plugin, PluginSettings settings, SleepManager sleepManager,
                           MessageService messages, Runnable reloadAction) {
        this.plugin = plugin;
        this.settings = settings;
        this.sleepManager = sleepManager;
        this.messages = messages;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("status")) return status(sender, args);
        if (subcommand.equals("reload")) return reload(sender);
        if (subcommand.equals("set")) return setThreshold(sender, args);
        if (subcommand.equals("test")) return test(sender);
        sendHelp(sender, label);
        return true;
    }

    private boolean status(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "onesleepplus.status")) return true;
        World world = args.length >= 2 ? Bukkit.getWorld(args[1]) : senderWorld(sender);
        if (world == null) {
            worldNotFound(sender, args.length >= 2 ? args[1] : "?");
            return true;
        }
        SleepSnapshot snapshot = sleepManager.getSnapshot(world);
        Map<String, String> values = messages.snapshotReplacements(snapshot);
        values.put("clamped", snapshot.isThresholdClamped()
                ? messages.getMessage("status-clamped", "") : "");
        messages.sendChat(sender, "status", values);
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!hasPermission(sender, "onesleepplus.reload")) return true;
        reloadAction.run();
        messages.sendChat(sender, "reload-success");
        return true;
    }

    private boolean setThreshold(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "onesleepplus.set")) return true;
        if (args.length < 2) {
            messages.sendChat(sender, "invalid-threshold");
            return true;
        }
        final SleepThreshold threshold;
        try {
            threshold = SleepThreshold.parse(args[1], settings.getRounding());
        } catch (IllegalArgumentException exception) {
            messages.sendChat(sender, "invalid-threshold");
            return true;
        }

        World world = args.length >= 3 ? Bukkit.getWorld(args[2]) : senderWorld(sender);
        if (world == null) {
            worldNotFound(sender, args.length >= 3 ? args[2] : "?");
            return true;
        }
        String base = "worlds.overrides." + world.getName();
        plugin.getConfig().set(base + ".enabled", true);
        plugin.getConfig().set(base + ".threshold", threshold.asConfigValue());
        plugin.saveConfig();
        reloadAction.run();

        Map<String, String> values = new HashMap<String, String>();
        values.put("world", world.getName());
        values.put("threshold", threshold.asConfigValue());
        messages.sendChat(sender, "threshold-set", values);
        return true;
    }

    private boolean test(CommandSender sender) {
        if (!hasPermission(sender, "onesleepplus.test")) return true;
        if (sender instanceof Player) {
            messages.sendActionBar((Player) sender, "test-message", Collections.<String, String>emptyMap());
        } else {
            messages.sendChat(sender, "test-message");
        }
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("onesleepplus.admin")) return true;
        messages.sendChat(sender, "no-permission");
        return false;
    }

    private static World senderWorld(CommandSender sender) {
        return sender instanceof Player ? ((Player) sender).getWorld() : null;
    }

    private void worldNotFound(CommandSender sender, String world) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("world", world);
        messages.sendChat(sender, "world-not-found", values);
    }

    private void sendHelp(CommandSender sender, String label) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("label", label);
        messages.sendUnprefixedChat(sender, "help-header", values);
        messages.sendUnprefixedChat(sender, "help-status", values);
        messages.sendUnprefixedChat(sender, "help-set", values);
        messages.sendUnprefixedChat(sender, "help-reload", values);
        messages.sendUnprefixedChat(sender, "help-test", values);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("status", "set", "reload", "test"), args[0]);
        }
        if ((args[0].equalsIgnoreCase("status") && args.length == 2)
                || (args[0].equalsIgnoreCase("set") && args.length == 3)) {
            List<String> worlds = new ArrayList<String>();
            for (World world : Bukkit.getWorlds()) worlds.add(world.getName());
            return filter(worlds, args[args.length - 1]);
        }
        if (args[0].equalsIgnoreCase("set") && args.length == 2) {
            return filter(Arrays.asList("1", "2", "50%", "%50"), args[1]);
        }
        return Collections.emptyList();
    }

    private static List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<String>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) matches.add(value);
        }
        return matches;
    }
}
