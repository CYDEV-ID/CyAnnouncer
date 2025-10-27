package cydev.id.cyannouncer.bukkit;

import java.util.List;

/**
 * @param worlds
 * @param lines
 * @param type
 * @param sound
 */

public record Announcement(List<String> worlds, List<String> lines, String type, String sound) {
}