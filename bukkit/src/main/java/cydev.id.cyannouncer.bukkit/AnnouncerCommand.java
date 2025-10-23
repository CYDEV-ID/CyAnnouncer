package cydev.id.cyannouncer.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AnnouncerCommand implements CommandExecutor, TabCompleter {

    private final CyAnnouncer plugin;

    public AnnouncerCommand(CyAnnouncer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "broadcast":
                return handleBroadcast(sender, Arrays.copyOfRange(args, 1, args.length));
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("announcer.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        plugin.loadConfig();
        plugin.startAnnouncements();
        sender.sendMessage(ChatColor.GREEN + "CyAnnouncer configuration has been reloaded.");
        return true;
    }

    private boolean handleBroadcast(CommandSender sender, String[] args) {
        if (!sender.hasPermission("announcer.broadcast")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /announcer broadcast <world1,world2,...|all> <message>");
            return true;
        }

        String targets = args[0];
        String message = ChatColor.translateAlternateColorCodes('&', plugin.getPrefix() + String.join(" ", Arrays.copyOfRange(args, 1, args.length)));

        if (targets.equalsIgnoreCase("all")) {
            Bukkit.broadcastMessage(message);
        } else {
            List<String> targetWorlds = Arrays.asList(targets.split(","));
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (targetWorlds.contains(player.getWorld().getName())) {
                    player.sendMessage(message);
                }
            }
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- CyAnnouncer Help ---");
        sender.sendMessage(ChatColor.YELLOW + "/announcer reload" + ChatColor.WHITE + " - Reloads the config file.");
        sender.sendMessage(ChatColor.YELLOW + "/announcer broadcast <worlds|all> <msg>" + ChatColor.WHITE + " - Broadcasts a message.");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("reload", "broadcast"));
            return subCommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("broadcast")) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("all");
            suggestions.addAll(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()));
            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}