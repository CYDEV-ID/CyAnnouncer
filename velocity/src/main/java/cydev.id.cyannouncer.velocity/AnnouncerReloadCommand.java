package cydev.id.cyannouncer.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class AnnouncerReloadCommand implements SimpleCommand {

    private final VelocityAnnouncer plugin;

    public AnnouncerReloadCommand(VelocityAnnouncer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(final Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.loadConfig();
            plugin.startAnnouncements();

            invocation.source().sendMessage(
                    Component.text("CyAnnouncerVelocity configuration has been reloaded.", NamedTextColor.GREEN)
            );
        } else {
            sendUsage(invocation);
        }
    }

    private void sendUsage(final Invocation invocation) {
        invocation.source().sendMessage(
                Component.text("CyAnnouncer-Velocity", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text(" - Available Commands:", NamedTextColor.GRAY))
        );
        invocation.source().sendMessage(
                Component.text(" ● /announcer reload", NamedTextColor.YELLOW)
                        .append(Component.text(" - Reloads the configuration file.", NamedTextColor.WHITE))
        );
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("announcer.reload");
    }
}