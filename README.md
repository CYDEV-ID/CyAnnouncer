<p align="center">
  	<a href=""><img src="https://i.postimg.cc/t4SqcBq8/Cream-and-Green-Illustrative-Coming-Soon-Email-Header-600-x-100-px-600-x-150-px-5.png" width="1719" alt="whoimai" /></a>
</p>

<p align="center">
    A simple yet powerful, multi-platform announcement plugin designed for Bukkit, BungeeCord, and Velocity.<br>Keep your players informed with automated, scheduled messages tailored to specific servers or worlds.<br />
</p>

## Supported Platforms
- Bukkit/Spigot (and any forks like Paper, Purpur)
- Bungeecord (and any forks like Waterfall)
- Velocity

## Features
- Multi-Platform: A single codebase provides native JARs for Bukkit, BungeeCord, and Velocity.
- Customizable Messages: Configure unlimited announcements, each with its own multi-line text.
- Configurable Interval: Set the global time (in seconds) between each announcement.
- Custom Prefix: Add a global prefix to all messages (supports color codes).
- Context-Aware Targeting:
  - On Bukkit, show messages in specific worlds or globally (all).
  - On BungeeCord/Velocity, show messages on specific servers or globally (all).
- Reload Command: Reload the configuration in-game without a server/proxies restart.

## Installation
Download the correct JAR file for your server type from the <a href="https://github.com/CYDEV-ID/CyAnnouncer/releases/tag/build">Release Page</a>. Do not install all three JARs.

### For Bukkit/Spigot
1. Download `CyAnnouncerBukkit-1.0.0.jar` (Use the Latest Version).
2. Place the JAR file in your Bukkit/Spigot server's `plugins/` folder.
3. Restart your server.
4. Change the configuration as desired in `plugins/CyAnnouncerBukkit/config.yml`.

### For BungeeCord
1. Download `CyAnnouncerBungee-1.0.0.jar` (Use the Latest Version).
2. Place the JAR file in your Bungee proxy's `plugins/` folder.
3. Restart your proxy.
4. Change the configuration as desired in `plugins/CyAnnouncerBungee/config.yml`.

### For Velocity
1. Download `CyAnnouncerVelocity-1.0.0.jar` (Use the Latest Version).
2. Place the JAR file in your Velocity proxy's `plugins/` folder.
3. Restart your proxy.
4. Change the configuration as desired in `plugins/CyAnnouncerVelocity/config.yml`.

## Commands and Permissions
Commands are different and permissions remain the same

### Bukkit/Spigot
| Commands                             | Descriptions                                          |           Permissions           |
| ------------------------------------ | ----------------------------------------------------- | ------------------------------- |
| `/announcer`                         | Display a list of commands                            | `-`                             |
| `/announcer reload` or `/an reload`  | Reload config.yml                                     | `announcer.reload`              |
| `/announcer broadcast`               | Send messages to all worlds or to specific worlds.    | `announcer.broadcast`           |

### BungeeCord
| Commands                  | Descriptions                                          |           Permissions           |
| --------------------------| ----------------------------------------------------- | ------------------------------- |
| `/bcreload`               | Reload config.yml                                     | `announcer.reload`              |
| `/bcbroadcast all/server` | Send messages to all servers or to specific servers.  | `announcer.broadcast`           |

### Velocity
| Commands                 | Descriptions                                          |           Permissions           |
| ------------------------ | ----------------------------------------------------- | ------------------------------- |
| `/announcer reload`      | Reload config.yml                                     | `announcer.reload`              |
| `/vbroadcast all/server` | Send messages to all servers or to specific servers.  | `announcer.broadcast`           |

## Configuration
The configuration of each platform is different

### Bukkit/Spigot 
`/plugins/CyAnnouncerBukkit/config.yml`

```yaml
# ===================================================================
#                 CyAnnouncer-Bukkit Configuration
# ===================================================================
#
# Plugin Author: cydev-id
# Plugin Version: 1.0.0
#
# HOW IT WORKS:
# This plugin will display announcements with an independent cycle for each world.
#
# 1. A world with specific messages (e.g., 'world_the_end') will have a cycle of:
#    (Global Message -> Specific End Msg 1 -> Global Message -> ...)
#
# 2. A world without specific messages will ONLY cycle through global messages.
#
# ===================================================================

# Interval in SECONDS between each announcement for a world.
interval: 60

# Prefix appearing before each announcement line.
# Set to "" to disable.
prefix: "&e[&l!&r&e] &r"

# --- ANNOUNCEMENTS SECTION ---
announcements:
  # --- GLOBAL ANNOUNCEMENTS ---
  # These messages will be part of the cycle in ALL worlds.
  # Use "all" in the 'worlds' list.
  - worlds:
      - "all"
    lines:
      - "&eDon't forget to vote for the server every day!"
      - "&fUse the command &b/vote&f to get rewards."

  # --- WORLD-SPECIFIC ANNOUNCEMENTS ---
  # These messages will ONLY appear in the specified worlds.
  - worlds:
      - "world_the_end"
    lines:
      - "&5Be careful! The Ender Dragon is powerful in this world."

  - worlds:
      - "world_nether"
    lines:
      - "&cIt's hot in here! Make sure to bring fire resistance potions."
      - "&fDon't hit a Piglin without preparation!"
```

### BungeeCord & Velocity
`/plugins/CyAnnouncerBungee/config.yml`<br>`/plugins/CyAnnouncerVelocity/config.yml`<br />

```yaml
# ===================================================================+
#             CyAnnouncer-Bungee & Velocity Configuration
# ===================================================================+
#
# Plugin Author: cydev-id
# Plugin Version: 1.0.0
#
# HOW IT WORKS:
# This plugin will display announcements with an independent cycle for each server.
#
# 1. A server with specific messages (e.g., 'survival') will have a cycle of:
#    (Global Message -> Specific Survival Msg 1 -> Global Message -> ...)
#
# 2. A server WITHOUT specific messages will ONLY cycle through the global messages.
#
# ===================================================================

# Default interval in SECONDS between each announcement for a server.
interval: 60

# The prefix that appears before every line of an announcement.
# Set to "" (empty quotes) to disable the prefix entirely.
prefix: "&e[&l!&r&e] &r"

# --- ANNOUNCEMENTS SECTION ---
announcements:
  # --- GLOBAL ANNOUNCEMENTS ---
  # To make a message global, add "all" to its 'servers' list.
  - servers:
      - "all"
    lines:
      - "&eDon't forget to vote for the server every day!"
      - "&fUse the command &b/vote&f to get rewards."

  # --- SERVER-SPECIFIC ANNOUNCEMENTS ---
  # These messages will ONLY be shown to players on the specified servers.
  - servers:
      - "survival"
    lines:
      - "&cWatch out! This is a PvP-enabled server."
```

## License
This project is licensed under the [MIT](LICENSE)
