package cydev.id.cyannouncer.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class ReloadCommand extends Command {
    private final BungeeAnnouncer plugin;

    public ReloadCommand(BungeeAnnouncer plugin) {
        super("bcreload", "announcer.reload");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.loadConfig();
        plugin.startAnnouncements();
        sender.sendMessage(new TextComponent(ChatColor.GREEN + "CyAnnouncer-Bungee configuration has been reloaded."));
    }
}