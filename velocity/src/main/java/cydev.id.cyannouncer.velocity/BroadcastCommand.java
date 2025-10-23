package cydev.id.cyannouncer.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BroadcastCommand implements SimpleCommand {

    private final VelocityAnnouncer plugin;

    public BroadcastCommand(VelocityAnnouncer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(final Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 2) {
            invocation.source().sendMessage(Component.text("Usage: /vbroadcast <server1,server2,...|all> <message>", NamedTextColor.RED));
            return;
        }

        String targets = args[0];
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Component componentMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getPrefix() + message);

        if (targets.equalsIgnoreCase("all")) {
            plugin.getServer().sendMessage(componentMessage);
            invocation.source().sendMessage(Component.text("Broadcast sent to all servers.", NamedTextColor.GREEN));
        } else {
            List<String> targetServers = Arrays.asList(targets.split(","));
            List<String> allServerNames = plugin.getServer().getAllServers().stream()
                    .map(rs -> rs.getServerInfo().getName())
                    .collect(Collectors.toList());

            for (String targetServer : targetServers) {
                if (!allServerNames.contains(targetServer)) {
                    invocation.source().sendMessage(Component.text("Error: Server '" + targetServer + "' not found.", NamedTextColor.RED));
                    invocation.source().sendMessage(Component.text("Available servers: " + String.join(", ", allServerNames), NamedTextColor.GRAY));
                    return;
                }
            }

            for (Player player : plugin.getServer().getAllPlayers()) {
                player.getCurrentServer().ifPresent(serverConnection -> {
                    String serverName = serverConnection.getServerInfo().getName();
                    if (targetServers.contains(serverName)) {
                        player.sendMessage(componentMessage);
                    }
                });
            }
            invocation.source().sendMessage(Component.text("Broadcast sent to servers: " + String.join(", ", targetServers), NamedTextColor.GREEN));
        }
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("announcer.broadcast");
    }
}