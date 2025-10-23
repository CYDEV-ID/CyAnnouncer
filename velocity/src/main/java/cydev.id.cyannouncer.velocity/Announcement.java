package cydev.id.cyannouncer.velocity;

import java.util.List;

/**
 * A simple data container (record) to hold the target servers
 * and the message lines for a single announcement block.
 */
public record Announcement(List<String> servers, List<String> lines) {
}