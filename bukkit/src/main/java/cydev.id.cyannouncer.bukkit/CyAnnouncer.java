package cydev.id.cyannouncer.bukkit;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.*;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class CyAnnouncer extends JavaPlugin {

    private Runnable cancelTask = () -> {};
    private List<Announcement> allMessages;
    private Map<String, List<Announcement>> worldSpecificMessages;
    private final Map<String, AtomicInteger> specificCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> allCounters = new ConcurrentHashMap<>();
    private String prefix;
    private int interval;
    private boolean placeholderApiEnabled = false;
    private boolean isRandom = false;
    private boolean isFolia = false;

    @Override
    public void onEnable() {
        int pluginId = 27594;
        new Metrics(this, pluginId);

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            this.isFolia = true;
        } catch (ClassNotFoundException e) {
            this.isFolia = false;
        }

        loadConfig();

        AnnouncerCommand announcerExecutor = new AnnouncerCommand(this);
        getCommand("announcer").setExecutor(announcerExecutor);
        getCommand("announcer").setTabCompleter(announcerExecutor);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.placeholderApiEnabled = true;
            getLogger().info("Successfully hooked into PlaceholderAPI.");
        } else {
            getLogger().info("PlaceholderAPI not found. Placeholders will not be parsed.");
        }

        getLogger().info("CyAnnouncer has been enabled.");
        startAnnouncements();
    }

    @Override
    public void onDisable() {
        this.cancelTask.run();
        getLogger().info("CyAnnouncer has been disabled.");
    }

    public void loadConfig() {
        int targetConfigVersion = 2;
        File configFile = new File(getDataFolder(), "config.yml");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(configFile).build();
        CommentedConfigurationNode config;

        if (!configFile.exists()) {
            getLogger().info("No config.yml found, creating a new one...");
            saveDefaultConfig();
        }
        try {
            config = loader.load();
            int currentVersion = config.node("config-version").getInt(0);
            if (currentVersion < targetConfigVersion) {
                getLogger().warning("!!! DETECTED OUTDATED CONFIG (Version " + currentVersion + ") !!!");
                getLogger().warning("Backing up your old config to 'config.yml.old'...");
                File oldConfigFile = new File(getDataFolder(), "config.yml.old");
                if (oldConfigFile.exists()) oldConfigFile.delete();
                loader = null;
                if (!configFile.renameTo(oldConfigFile)) {
                    getLogger().severe("!!! FAILED TO BACK UP OLD CONFIG. Please do it manually! Aborting load.");
                    return;
                }
                saveDefaultConfig();
                loader = YamlConfigurationLoader.builder().file(configFile).build();
                config = loader.load();
                getLogger().info("Successfully loaded new config.yml (Version " + targetConfigVersion + ").");
                getLogger().info("Please transfer your old announcement lines from 'config.yml.old'.");
            }
            this.interval = config.node("interval").getInt(60);
            this.prefix = config.node("prefix").getString("&e[&l!&r&e] &r");
            this.isRandom = config.node("settings", "random").getBoolean(false);
            this.allMessages = new ArrayList<>();
            this.worldSpecificMessages = new HashMap<>();
            List<? extends CommentedConfigurationNode> announcementNodes = config.node("announcements").childrenList();
            for (CommentedConfigurationNode node : announcementNodes) {
                List<String> worlds = node.node("worlds").getList(String.class, Collections.emptyList());
                List<String> lines = node.node("lines").getList(String.class, Collections.emptyList());
                String type = node.node("type").getString("CHAT").toUpperCase();
                String sound = node.node("sound").getString("");
                if (worlds.isEmpty() || lines.isEmpty()) continue;
                Announcement announcement = new Announcement(worlds, lines, type, sound);
                if (worlds.contains("all")) {
                    allMessages.add(announcement);
                } else {
                    for (String worldName : worlds) {
                        worldSpecificMessages.computeIfAbsent(worldName, k -> new ArrayList<>()).add(announcement);
                    }
                }
            }
            getLogger().info("Configuration loaded. Random Mode: " + this.isRandom);
            getLogger().info("Found " + allMessages.size() + " global announcements and messages for " + worldSpecificMessages.size() + " specific worlds.");
            specificCounters.clear();
            allCounters.clear();
        } catch (Exception e) {
            getLogger().severe("Failed to load the configuration!");
            e.printStackTrace();
        }
    }

    public void startAnnouncements() {
        this.cancelTask.run();

        if (allMessages.isEmpty() && worldSpecificMessages.isEmpty() || interval <= 0) {
            getLogger().warning("Announcements are disabled (no messages found or invalid interval).");
            return;
        }

        Runnable announcementRunnable = () -> {
            if (getServer().getOnlinePlayers().isEmpty()) return;
            Map<String, List<Player>> playersByWorld = getServer().getOnlinePlayers().stream()
                    .collect(Collectors.groupingBy(player -> player.getWorld().getName()));
            for (String worldName : playersByWorld.keySet()) {
                List<Announcement> specificMessages = worldSpecificMessages.getOrDefault(worldName, Collections.emptyList());
                List<Player> targetPlayers = playersByWorld.get(worldName);
                Announcement announcementToSend = null;
                if (this.isRandom) {
                    List<Announcement> availableMessages = new ArrayList<>(allMessages);
                    availableMessages.addAll(specificMessages);
                    if (!availableMessages.isEmpty()) {
                        announcementToSend = availableMessages.get(new Random().nextInt(availableMessages.size()));
                    }
                } else {
                    AtomicInteger specificCounter = specificCounters.computeIfAbsent(worldName, k -> new AtomicInteger(0));
                    AtomicInteger allCounter = allCounters.computeIfAbsent(worldName, k -> new AtomicInteger(0));
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
        };

        if (this.isFolia) {
            ScheduledTask foliaTask = getServer().getGlobalRegionScheduler().runAtFixedRate(
                    this,
                    (task) -> announcementRunnable.run(),
                    20L * interval,
                    20L * interval
            );
            this.cancelTask = () -> foliaTask.cancel();
        } else {
            BukkitTask bukkitTask = getServer().getScheduler().runTaskTimer(this,
                    announcementRunnable,
                    20L * interval, 20L * interval);
            this.cancelTask = () -> bukkitTask.cancel();
        }

        getLogger().info("Announcements scheduler started, running every " + this.interval + " seconds.");
    }

    private void sendAnnouncement(List<Player> players, Announcement announcement) {
        String type = announcement.type();
        String soundName = announcement.sound();
        List<String> lines = announcement.lines();
        String title = lines.isEmpty() ? "" : lines.get(0);
        String subtitle = lines.size() > 1 ? lines.get(1) : "";
        for (Player player : players) {
            if (soundName != null && !soundName.isEmpty()) {
                try {
                    Sound soundEnum = Sound.valueOf(soundName.toUpperCase());
                    player.playSound(player.getLocation(), soundEnum, 1.0f, 1.0f);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid sound name in config.yml: " + soundName);
                }
            }
            String parsedTitle = parsePlaceholders(player, title);
            String parsedSubtitle = parsePlaceholders(player, subtitle);
            switch (type) {
                case "TITLE":
                    player.sendTitle(parsedTitle, parsedSubtitle, 10, 70, 20);
                    break;
                case "ACTIONBAR":
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(parsedTitle));
                    break;
                case "CHAT":
                default:
                    for (String line : lines) {
                        String parsedLine = parsePlaceholders(player, this.prefix + line);
                        player.sendMessage(parsedLine);
                    }
                    break;
            }
        }
    }

    private String parsePlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) return "";
        String parsed = this.placeholderApiEnabled
                ? PlaceholderAPI.setPlaceholders(player, text)
                : text;
        return ChatColor.translateAlternateColorCodes('&', parsed);
    }

    public String getPrefix() { return prefix; }
}