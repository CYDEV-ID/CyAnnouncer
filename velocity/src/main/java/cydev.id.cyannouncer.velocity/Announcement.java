package cydev.id.cyannouncer.velocity;

import java.util.List;

/**
 * @param servers
 * @param lines
 * @param type
 * @param sound
 */

public record Announcement(List<String> servers, List<String> lines, String type, String sound) {
}