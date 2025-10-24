package cydev.id.cyannouncer.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.bstats.bungeecord.Metrics;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.*;

public class BungeeAnnouncer extends Plugin {

    private ScheduledTask announcementTask;
    private List<Announcement> allMessages;
    private Map<String, List<Announcement>> serverSpecificMessages;
    private final Map<String, AtomicInteger> specificCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> allCounters = new ConcurrentHashMap<>();
    private String prefix;
    private int interval;

    @Override
    public void onEnable() {
        int pluginId = 27596;
        new Metrics(this, pluginId);

        loadConfig();
        getProxy().getPluginManager().registerCommand(this, new BroadcastCommand(this));
        getProxy().getPluginManager().registerCommand(this, new ReloadCommand(this));
        getLogger().info("Successfully registered commands.");
        startAnnouncements();
    }

    public void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                getDataFolder().mkdir();
                try (InputStream defaultConfig = getResourceAsStream("config.yml")) {
                    if (defaultConfig != null) {
                        Files.copy(defaultConfig, configFile.toPath());
                    }
                }
            } catch (Exception e) { getLogger().severe("Failed to create the default configuration file!"); return; }
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(configFile).build();
        try {
            CommentedConfigurationNode config = loader.load();
            this.interval = config.node("interval").getInt(60);
            this.prefix = config.node("prefix").getString("&e[&l!&r&e] &r");

            this.allMessages = new ArrayList<>();
            this.serverSpecificMessages = new HashMap<>();

            List<? extends CommentedConfigurationNode> announcementNodes = config.node("announcements").childrenList();
            for (CommentedConfigurationNode node : announcementNodes) {
                List<String> servers = node.node("servers").getList(String.class, Collections.emptyList());
                List<String> lines = node.node("lines").getList(String.class, Collections.emptyList());

                if (servers.isEmpty() || lines.isEmpty()) continue;

                Announcement announcement = new Announcement(servers, lines);
                if (servers.contains("all")) {
                    allMessages.add(announcement);
                } else {
                    for (String serverName : servers) {
                        serverSpecificMessages.computeIfAbsent(serverName, k -> new ArrayList<>()).add(announcement);
                    }
                }
            }
            getLogger().info("Configuration loaded. Found " + allMessages.size() + " global announcements and messages for " + serverSpecificMessages.size() + " specific servers.");
            specificCounters.clear();
            allCounters.clear();
        } catch (Exception e) { getLogger().severe("Failed to load the configuration!"); }
    }

    public void startAnnouncements() {
        if (announcementTask != null) announcementTask.cancel();

        if (allMessages.isEmpty() && serverSpecificMessages.isEmpty() || interval <= 0) {
            getLogger().warning("Announcements are disabled (no messages found or invalid interval).");
            return;
        }

        announcementTask = getProxy().getScheduler().schedule(this, () -> {
            if (getProxy().getPlayers().isEmpty()) return;

            Map<String, List<ProxiedPlayer>> playersByServer = getProxy().getPlayers().stream()
                    .filter(p -> p.getServer() != null)
                    .collect(Collectors.groupingBy(p -> p.getServer().getInfo().getName()));

            for (String serverName : playersByServer.keySet()) {
                List<Announcement> specificMessages = serverSpecificMessages.getOrDefault(serverName, Collections.emptyList());

                AtomicInteger specificCounter = specificCounters.computeIfAbsent(serverName, k -> new AtomicInteger(0));
                AtomicInteger allCounter = allCounters.computeIfAbsent(serverName, k -> new AtomicInteger(0));

                Announcement announcementToSend = null;

                if (!specificMessages.isEmpty() && specificCounter.get() < specificMessages.size()) {
                    announcementToSend = specificMessages.get(specificCounter.getAndIncrement());
                } else {
                    if (!allMessages.isEmpty()) {
                        announcementToSend = allMessages.get(allCounter.getAndIncrement());
                        if (allCounter.get() >= allMessages.size()) allCounter.set(0);
                    }
                    specificCounter.set(0);
                }

                if (announcementToSend != null) {
                    List<ProxiedPlayer> targetPlayers = playersByServer.get(serverName);

                    for (ProxiedPlayer player : targetPlayers) {
                        for (String line : announcementToSend.lines()) {
                            String rawLine = this.prefix + line;

                            String parsedLine = replacePlaceholders(rawLine, player);

                            TextComponent finalLine = new TextComponent(ChatColor.translateAlternateColorCodes('&', parsedLine));
                            player.sendMessage(finalLine);
                        }
                    }
                }
            }
        }, interval, interval, TimeUnit.SECONDS);

        getLogger().info("Announcements scheduler started, running every " + this.interval + " seconds.");
    }
    /**
     * @param text
     * @param player
     * @return
     */
    private String replacePlaceholders(String text, ProxiedPlayer player) {
        String serverName = "unknown";
        if (player.getServer() != null && player.getServer().getInfo() != null) {
            serverName = player.getServer().getInfo().getName();
        }

        return text
                .replace("%player_name%", player.getName())
                .replace("%server_name%", serverName)
                .replace("%proxy_online%", String.valueOf(getProxy().getOnlineCount()))
                .replace("%server_online%", String.valueOf(player.getServer().getInfo().getPlayers().size()))
                .replace("%ping%", String.valueOf(player.getPing()));
    }

    public String getPrefix() { return prefix; }
    public Map<String, ServerInfo> getServers() { return getProxy().getServers(); }
}