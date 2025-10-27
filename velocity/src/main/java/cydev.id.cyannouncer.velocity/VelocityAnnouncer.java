package cydev.id.cyannouncer.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
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

@Plugin(id = "cyannouncervelocity", name = "CyAnnouncerVelocity", version = "1.0.2",
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
    private boolean isRandom = false;

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
        int targetConfigVersion = 2;

        File configFile = new File(dataDirectory.toFile(), "config.yml");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(configFile).build();
        CommentedConfigurationNode config;

        if (!configFile.exists()) {
            logger.info("No config.yml found, creating a new one...");
            saveDefaultConfigHelper();
        }

        try {
            config = loader.load();

            int currentVersion = config.node("config-version").getInt(0);

            if (currentVersion < targetConfigVersion) {
                logger.warn("!!! DETECTED OUTDATED CONFIG (Version " + currentVersion + ") !!!");
                logger.warn("Backing up your old config to 'config.yml.old'...");

                File oldConfigFile = new File(dataDirectory.toFile(), "config.yml.old");
                if (oldConfigFile.exists()) {
                    oldConfigFile.delete();
                }

                loader = null;
                if (!configFile.renameTo(oldConfigFile)) {
                    logger.error("!!! FAILED TO BACK UP OLD CONFIG. Please do it manually! Aborting load.");
                    return;
                }

                saveDefaultConfigHelper();

                loader = YamlConfigurationLoader.builder().file(configFile).build();
                config = loader.load();

                logger.info("Successfully loaded new config.yml (Version " + targetConfigVersion + ").");
                logger.info("Please transfer your old announcement lines from 'config.yml.old'.");
            }

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

                if (!servers.isEmpty() && !lines.isEmpty()) {
                    Announcement announcement = new Announcement(servers, lines, type, sound);

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

            logger.info("Configuration loaded. Random Mode: " + this.isRandom);
            logger.info("Found " + allMessages.size() + " global announcements and messages for " + serverSpecificMessages.size() + " specific servers.");
            specificCounters.clear();
            allCounters.clear();

        } catch (Exception e) {
            logger.error("Failed to load the configuration!", e);
        }
    }

    private void saveDefaultConfigHelper() {
        File configFile = new File(dataDirectory.toFile(), "config.yml");
        try {
            Files.createDirectories(dataDirectory);
            if (!configFile.exists()) {
                try (InputStream defaultConfig = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (defaultConfig != null) {
                        Files.copy(defaultConfig, configFile.toPath());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to create the default configuration file!", e);
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
                List<Player> targetPlayers = playersByServer.get(serverName);
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
        }).repeat(Duration.ofSeconds(this.interval)).schedule();

        logger.info("Advanced announcements scheduler started, running every " + this.interval + " seconds.");
    }

    private void sendAnnouncement(List<Player> players, Announcement announcement) {
        String type = announcement.type();
        String soundName = announcement.sound();
        List<String> lines = announcement.lines();

        String title = lines.isEmpty() ? "" : lines.get(0);
        String subtitle = lines.size() > 1 ? lines.get(1) : "";

        Title.Times times = Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofMillis(3500),
                Duration.ofMillis(1000)
        );

        for (Player player : players) {
            if (soundName != null && !soundName.isEmpty()) {
                try {
                    String minecraftKey = soundName.toLowerCase(Locale.ROOT).replace('_', '.');

                    Sound sound = Sound.sound(
                            Key.key("minecraft", minecraftKey),
                            Sound.Source.MASTER, 1f, 1f
                    );
                    player.playSound(sound);
                } catch (Exception e) {
                    logger.warn("Invalid sound name or format in config.yml: " + soundName);
                }
            }

            Component parsedTitle = deserialize(replacePlaceholders(title, player));
            Component parsedSubtitle = deserialize(replacePlaceholders(subtitle, player));

            switch (type) {
                case "TITLE":
                    Title titleObj = Title.title(parsedTitle, parsedSubtitle, times);
                    player.showTitle(titleObj);
                    break;

                case "ACTIONBAR":
                    player.sendActionBar(parsedTitle);
                    break;

                case "CHAT":
                default:
                    for (String line : lines) {
                        Component parsedLine = deserialize(replacePlaceholders(this.prefix + line, player));
                        player.sendMessage(parsedLine);
                    }
                    break;
            }
        }
    }

    private String replacePlaceholders(String text, Player player) {
        if (text == null || text.isEmpty()) return "";

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

    private Component deserialize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public ProxyServer getServer() { return server; }
    public String getPrefix() { return prefix; }
    public Logger getLogger() { return logger; }
}