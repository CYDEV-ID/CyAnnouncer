package cydev.id.cyannouncer.bukkit;

import me.clip.placeholderapi.PlaceholderAPI; // Ditambahkan
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
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

public class CyAnnouncer extends JavaPlugin {

    private BukkitTask announcementTask;
    private List<Announcement> allMessages;
    private Map<String, List<Announcement>> worldSpecificMessages;
    private final Map<String, AtomicInteger> specificCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> allCounters = new ConcurrentHashMap<>();
    private String prefix;
    private int interval;

    // Variabel untuk menyimpan status PAPI
    private boolean placeholderApiEnabled = false;

    @Override
    public void onEnable() {
        int pluginId = 27594;
        new Metrics(this, pluginId);

        loadConfig();

        // Pastikan class AnnouncerCommand Anda ada
        AnnouncerCommand announcerExecutor = new AnnouncerCommand(this);
        getCommand("announcer").setExecutor(announcerExecutor);
        getCommand("announcer").setTabCompleter(announcerExecutor);

        // Cek apakah PlaceholderAPI ada di server
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.placeholderApiEnabled = true;
            getLogger().info("Successfully hooked into PlaceholderAPI.");
        } else {
            getLogger().info("PlaceholderAPI not found. Placeholders will not be parsed.");
        }

        getLogger().info("CyAnnouncer has been enabled.");
        startAnnouncements();
    }

    public void loadConfig() {
        saveDefaultConfig();
        File configFile = new File(getDataFolder(), "config.yml");

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(configFile).build();
        try {
            CommentedConfigurationNode config = loader.load();
            this.interval = config.node("interval").getInt(60);
            this.prefix = config.node("prefix").getString("&e[&l!&r&e] &r");

            this.allMessages = new ArrayList<>();
            this.worldSpecificMessages = new HashMap<>();

            List<? extends CommentedConfigurationNode> announcementNodes = config.node("announcements").childrenList();
            for (CommentedConfigurationNode node : announcementNodes) {
                List<String> worlds = node.node("worlds").getList(String.class, Collections.emptyList());
                List<String> lines = node.node("lines").getList(String.class, Collections.emptyList());

                if (worlds.isEmpty() || lines.isEmpty()) continue;

                // Pastikan class Announcement Anda ada
                Announcement announcement = new Announcement(worlds, lines);
                if (worlds.contains("all")) {
                    allMessages.add(announcement);
                } else {
                    for (String worldName : worlds) {
                        worldSpecificMessages.computeIfAbsent(worldName, k -> new ArrayList<>()).add(announcement);
                    }
                }
            }
            getLogger().info("Configuration loaded. Found " + allMessages.size() + " global announcements and messages for " + worldSpecificMessages.size() + " specific worlds.");
            specificCounters.clear();
            allCounters.clear();
        } catch (Exception e) {
            getLogger().severe("Failed to load the configuration!");
            e.printStackTrace();
        }
    }

    public void startAnnouncements() {
        if (announcementTask != null) announcementTask.cancel();

        if (allMessages.isEmpty() && worldSpecificMessages.isEmpty() || interval <= 0) {
            getLogger().warning("Announcements are disabled (no messages found or invalid interval).");
            return;
        }

        announcementTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (getServer().getOnlinePlayers().isEmpty()) return;

            Map<String, List<Player>> playersByWorld = getServer().getOnlinePlayers().stream()
                    .collect(Collectors.groupingBy(player -> player.getWorld().getName()));

            for (String worldName : playersByWorld.keySet()) {
                List<Announcement> specificMessages = worldSpecificMessages.getOrDefault(worldName, Collections.emptyList());

                AtomicInteger specificCounter = specificCounters.computeIfAbsent(worldName, k -> new AtomicInteger(0));
                AtomicInteger allCounter = allCounters.computeIfAbsent(worldName, k -> new AtomicInteger(0));

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
                    List<Player> targetPlayers = playersByWorld.get(worldName);
                    for (Player player : targetPlayers) {
                        for (String line : announcementToSend.lines()) {
                            String rawLine = this.prefix + line;
                            String parsedLine = this.placeholderApiEnabled
                                    ? PlaceholderAPI.setPlaceholders(player, rawLine)
                                    : rawLine;
                            String finalLine = ChatColor.translateAlternateColorCodes('&', parsedLine);
                            player.sendMessage(finalLine);
                        }
                    }
                }
            }
        }, 20L * interval, 20L * interval);

        getLogger().info("Announcements scheduler started, running every " + this.interval + " seconds.");
    }

    public String getPrefix() { return prefix; }
}