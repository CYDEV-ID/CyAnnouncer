package cydev.id.cyannouncer.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.Title;
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
    private boolean isRandom = false;

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

            this.isRandom = config.node("settings", "random").getBoolean(false);

            this.allMessages = new ArrayList<>();
            this.serverSpecificMessages = new HashMap<>();

            List<? extends CommentedConfigurationNode> announcementNodes = config.node("announcements").childrenList();
            for (CommentedConfigurationNode node : announcementNodes) {
                List<String> servers = node.node("servers").getList(String.class, Collections.emptyList());
                List<String> lines = node.node("lines").getList(String.class, Collections.emptyList());

                String type = node.node("type").getString("CHAT").toUpperCase();
                String sound = node.node("sound").getString("");

                if (servers.isEmpty() || lines.isEmpty()) continue;

                Announcement announcement = new Announcement(servers, lines, type, sound);

                if (servers.contains("all")) {
                    allMessages.add(announcement);
                } else {
                    for (String serverName : servers) {
                        serverSpecificMessages.computeIfAbsent(serverName, k -> new ArrayList<>()).add(announcement);
                    }
                }
            }
            getLogger().info("Configuration loaded. Random Mode: " + this.isRandom);
            getLogger().info("Found " + allMessages.size() + " global announcements and messages for " + serverSpecificMessages.size() + " specific servers.");
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
                List<ProxiedPlayer> targetPlayers = playersByServer.get(serverName);
                List<Announcement> specificMessages = serverSpecificMessages.getOrDefault(serverName, Collections.emptyList());

                Announcement announcementToSend = null;

                if (this.isRandom) {
                    List<Announcement> availableMessages = new ArrayList<>(allMessages);
                    availableMessages.addAll(specificMessages);
                    if (!availableMessages.isEmpty()) {
                        announcementToSend = availableMessages.get(new Random().nextInt(availableMessages.size()));
                    }
                } else {
                    AtomicInteger specificCounter = specificCounters.computeIfAbsent(serverName, k -> new AtomicInteger(0));
                    AtomicInteger allCounter = allCounters.computeIfAbsent(serverName, k -> new AtomicInteger(0));

                    if (!specificMessages.isEmpty() && specificCounter.get() < specificMessages.size()) {
                        announcementToSend = specificMessages.get(specificCounter.getAndIncrement());
                    } else {
                        if (!allMessages.isEmpty()) {
                            announcementToSend = allMessages.get(allCounter.getAndIncrement());
                            if (allCounter.get() >= allMessages.size()) allCounter.set(0);
                        }
                        specificCounter.set(0);
                    }
                }

                if (announcementToSend != null) {
                    sendAnnouncement(targetPlayers, announcementToSend);
                }
            }
        }, interval, interval, TimeUnit.SECONDS);

        getLogger().info("Announcements scheduler started, running every " + this.interval + " seconds.");
    }

    private void sendAnnouncement(List<ProxiedPlayer> players, Announcement announcement) {
        String type = announcement.type();
        List<String> lines = announcement.lines();

        String title = lines.isEmpty() ? "" : lines.get(0);
        String subtitle = lines.size() > 1 ? lines.get(1) : "";

        Title titleObj = getProxy().createTitle()
                .fadeIn(10).stay(70).fadeOut(20);

        for (ProxiedPlayer player : players) {

            String parsedTitle = replacePlaceholders(title, player);
            String parsedSubtitle = replacePlaceholders(subtitle, player);

            switch (type) {
                case "TITLE":
                    titleObj.title(TextComponent.fromLegacyText(parsedTitle));
                    titleObj.subTitle(TextComponent.fromLegacyText(parsedSubtitle));
                    player.sendTitle(titleObj);
                    break;

                case "ACTIONBAR":
                    player.sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(parsedTitle));
                    break;

                case "CHAT":
                default:
                    for (String line : lines) {
                        String parsedLine = replacePlaceholders(this.prefix + line, player);
                        player.sendMessage(TextComponent.fromLegacyText(parsedLine));
                    }
                    break;
            }
        }
    }


    private String replacePlaceholders(String text, ProxiedPlayer player) {
        if (text == null || text.isEmpty()) return "";

        String serverName = "unknown";
        int serverOnline = 0;
        if (player.getServer() != null && player.getServer().getInfo() != null) {
            ServerInfo serverInfo = player.getServer().getInfo();
            serverName = serverInfo.getName();
            serverOnline = serverInfo.getPlayers().size();
        }

        String replacedText = text
                .replace("%player_name%", player.getName())
                .replace("%server_name%", serverName)
                .replace("%proxy_online%", String.valueOf(getProxy().getOnlineCount()))
                .replace("%server_online%", String.valueOf(serverOnline))
                .replace("%ping%", String.valueOf(player.getPing()));

        return ChatColor.translateAlternateColorCodes('&', replacedText);
    }

    public String getPrefix() { return prefix; }
    public Map<String, ServerInfo> getServers() { return getProxy().getServers(); }
}