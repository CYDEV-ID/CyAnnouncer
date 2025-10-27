package cydev.id.cyannouncer.bungee;

import java.util.List;

/**
 * @param servers
 * @param lines
 * @param type
 * @param sound
 */

public record Announcement(List<String> servers, List<String> lines, String type, String sound) {
}