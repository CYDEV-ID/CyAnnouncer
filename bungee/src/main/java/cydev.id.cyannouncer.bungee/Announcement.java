package cydev.id.cyannouncer.bungee;

import java.util.List;

public record Announcement(List<String> servers, List<String> lines) {
}