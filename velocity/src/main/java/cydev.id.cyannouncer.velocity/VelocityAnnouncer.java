package cydev.id.cyannouncer.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Plugin(id = "cyannouncervelocity", name = "CyAnnouncerVelocity", version = "1.0.1",
        description = "An advanced, server-specific announcer plugin for Velocity.", authors = {"cydev-id"})
public class VelocityAnnouncer {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    private ScheduledTask announcementTask;
    private List<Announcement> allMessages;
    private Map<String, List<Announcement>> serverSpecificMessages;

    private final Map<String, AtomicInteger> specificCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> allCounters = new ConcurrentHashMap<>();

    private String prefix;
    private int interval;

    @Inject
    public VelocityAnnouncer(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @com.velocitypowered.api.event.Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        int pluginId = 27593;
        metricsFactory.make(this, pluginId);

        loadConfig();

        server.getCommandManager().register("vbroadcast", new BroadcastCommand(this));
        server.getCommandManager().register("announcer", new AnnouncerReloadCommand(this));
        logger.info("Successfully registered commands.");

        startAnnouncements();
    }

    public void loadConfig() {
        File configFile = new File(dataDirectory.toFile(), "config.yml");
        if (!configFile.exists()) {
            try {
                Files.createDirectories(dataDirectory);
                try (InputStream defaultConfig = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (defaultConfig != null) { Files.copy(defaultConfig, configFile.toPath()); }
                }
            } catch (Exception e) {
                logger.error("Failed to create the default configuration file!", e);
                return;
            }
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

                if (!servers.isEmpty() && !lines.isEmpty()) {
                    Announcement announcement = new Announcement(servers, lines);
                    if (servers.contains("all")) {
                        allMessages.add(announcement);
                    } else {
                        for (String serverName : servers) {
                            serverSpecificMessages.computeIfAbsent(serverName, k -> new ArrayList<>()).add(announcement);
                        }
                    }
                } else {
                    logger.warn("Skipping an announcement entry because 'servers' or 'lines' is empty.");
                }
            }

            logger.info("Configuration loaded. Found " + allMessages.size() + " global announcements and messages for " + serverSpecificMessages.size() + " specific servers.");
            specificCounters.clear();
            allCounters.clear();
        } catch (Exception e) {
            logger.error("Failed to load the configuration!", e);
        }
    }

    public void startAnnouncements() {
        if (announcementTask != null) announcementTask.cancel();

        if (allMessages.isEmpty() && serverSpecificMessages.isEmpty() || interval <= 0) {
            logger.warn("Announcements are disabled (no messages found or invalid interval).");
            return;
        }

        announcementTask = server.getScheduler().buildTask(this, () -> {
            if (server.getPlayerCount() == 0) return;

            Map<String, List<Player>> playersByServer = server.getAllPlayers().stream()
                    .filter(p -> p.getCurrentServer().isPresent())
                    .collect(Collectors.groupingBy(p -> p.getCurrentServer().get().getServerInfo().getName()));

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
                    List<Player> targetPlayers = playersByServer.get(serverName);

                    for (Player player : targetPlayers) {
                        for (String line : announcementToSend.lines()) {
                            String rawLine = this.prefix + line;

                            String parsedLine = replacePlaceholders(rawLine, player);

                            Component finalLine = LegacyComponentSerializer.legacyAmpersand().deserialize(parsedLine);
                            player.sendMessage(finalLine);
                        }
                    }
                }
            }
        }).repeat(Duration.ofSeconds(this.interval)).schedule();

        logger.info("Advanced announcements scheduler started, running every " + this.interval + " seconds.");
    }
    /**
     * @param text
     * @param player
     * @return
     */
    private String replacePlaceholders(String text, Player player) {
        String serverName = "unknown";
        int serverOnline = 0;

        if (player.getCurrentServer().isPresent()) {
            serverName = player.getCurrentServer().get().getServerInfo().getName();
            serverOnline = player.getCurrentServer().get().getServer().getPlayersConnected().size();
        }

        return text
                .replace("%player_name%", player.getUsername())
                .replace("%server_name%", serverName)
                .replace("%proxy_online%", String.valueOf(server.getPlayerCount()))
                .replace("%server_online%", String.valueOf(serverOnline))
                .replace("%ping%", String.valueOf(player.getPing()));
    }


    public ProxyServer getServer() { return server; }
    public String getPrefix() { return prefix; }
    public Logger getLogger() { return logger; }
}