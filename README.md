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
| ------------------------- | ----------------------------------------------------- | ------------------------------- |
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
<details>
<summary>Default Configuration</summary>

```yaml
# ===================================================================
#                  CyAnnouncer Configuration
# ===================================================================
#
# Plugin Author: cydev-id
# Plugin Version: 1.0.2
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

# DO NOT CHANGE THIS.
config-version: 2

# Interval in SECONDS between each announcement for a world.
interval: 60

# Prefix appearing before each announcement line.
# Set to "" to disable.
prefix: "&e[&l!&r&e] &r"

# (NEW) --- SETTINGS SECTION (Feature #3) ---
settings:
  # Set to true to pick a random message from all available messages (global + specific) each cycle.
  # Set to false to use the default sequential cycle (default).
  random: false

# --- ANNOUNCEMENTS SECTION ---
announcements:
  # --- GLOBAL ANNOUNCEMENTS ---
  # These messages will be part of the cycle in ALL worlds.
  # Use "all" in the 'worlds' list.

  # Example 1: Standard CHAT message (default type)
  - worlds:
      - "all"
    lines:
      - "&eDon't forget to vote for the server every day!"
      - "&fUse the command &b/vote&f to get rewards."

  # Example 2: Global ACTIONBAR message with a SOUND (Features #2 & #5)
  - worlds:
      - "all"
    type: "ACTIONBAR" # (NEW) Type: CHAT, ACTIONBAR, or TITLE
    sound: "ENTITY_PLAYER_LEVELUP" # (NEW) Sound from Bukkit's Sound Enum
    lines:
      - "&aWelcome! Check out our store at &b/buy"
      # Note: ACTIONBAR only uses the first line.

  # --- WORLD-SPECIFIC ANNOUNCEMENTS ---
  # These messages will ONLY appear in the specified worlds.

  # Example 3: TITLE message for a specific world (Feature #2)
  - worlds:
      - "world_the_end"
    type: "TITLE"
    lines:
      - "&5Welcome to The End!" # Line 1 is the main TITLE
      - "&7Be careful of the Dragon!" # Line 2 is the SUBTITLE

  - worlds:
      - "world_nether"
    lines:
      - "&cIt's hot in here! Make sure to bring fire resistance potions."
      - "&fDon't hit a Piglin without preparation!"
```
</details>

### BungeeCord
`/plugins/CyAnnouncerBungee/config.yml`
<details>
<summary>Default Configuration</summary>

```yaml
# ===================================================================
#                 CyAnnouncerBungee Configuration
# ===================================================================
#
# Plugin Author: cydev-id
# Plugin Version: 1.0.2
#
# HOW IT WORKS:
# This plugin will display announcements with an independent cycle for each server.
#
# ===================================================================

# DO NOT CHANGE THIS.
config-version: 2

# Default interval in SECONDS between each announcement for a server.
interval: 60

# The prefix that appears before every line of an announcement.
# Set to "" (empty quotes) to disable the prefix entirely.
prefix: "&e[&l!&r&e] &r"

# (NEW) --- SETTINGS SECTION (Feature #3) ---
settings:
  # Set to true to pick a random message from all available messages (global + specific) each cycle.
  # Set to false to use the default sequential cycle (default).
  random: false

# --- ANNOUNCEMENTS SECTION ---
announcements:
  # --- GLOBAL ANNOUNCEMENTS ---
  # To make a message global, add "all" to its 'servers' list.

  # Example 1: Standard CHAT message (default type)
  - servers:
      - "all"
    lines:
      - "&eDon't forget to vote for the server every day!"
      - "&fUse the command &b/vote&f to get rewards."

  # Example 2: Global ACTIONBAR message (Feature #2)
  - servers:
      - "all"
    type: "ACTIONBAR" # (NEW) Type: CHAT, ACTIONBAR, or TITLE
    # sound: "..." # (Note: Sound is NOT supported on BungeeCord)
    lines:
      - "&aWelcome! You are on server &e%server_name%&a!"
      # Note: ACTIONBAR only uses the first line.

  # --- SERVER-SPECIFIC ANNOUNCEMENTS ---
  # These messages will ONLY be shown to players on the specified servers.

  # Example 3: TITLE message for a specific server (Feature #2)
  - servers:
      - "lobby"
    type: "TITLE"
    lines:
      - "&bWelcome to the Lobby!" # Line 1 is the main TITLE
      - "&7Check out the new gadgets!" # Line 2 is the SUBTITLE

  # Example 4: Original Survival message
  - servers:
      - "survival"
    lines:
      - "&cWatch out! This is a PvP-enabled server."
```
</details>

### Velocity
`/plugins/CyAnnouncerVelocity/config.yml`
<details>
<summary>Default Configuration</summary>

```yaml
# ===================================================================
#                 CyAnnouncerVelocity Configuration
# ===================================================================
#
# Plugin Author: cydev-id
# Plugin Version: 1.0.2
#
# HOW TO USE THIS FILE:
#
# - Use the '&' symbol for color codes (e.g., &a for green, &l for bold).
# - YAML is sensitive to spacing. Do not use tabs; use spaces.
#   Indentation is crucial for the structure.
#
# ===================================================================

# Default interval in SECONDS between each announcement for a server.
# For example, if a player is on the Survival server, they will see a
# new message from their announcement cycle every 60 seconds.
interval: 60

# The prefix that appears before every line of an announcement.
# Set to "" (empty quotes) to disable the prefix entirely.
prefix: "&e[&l!&r&e] &r"

# (NEW) --- SETTINGS SECTION (Feature #3) ---
settings:
  # Set to true to pick a random message from all available messages (global + specific) each cycle.
  # Set to false to use the default sequential cycle (default).
  random: false


# ===================================================================
#                     ANNOUNCEMENTS SECTION
# ===================================================================
#
# HOW IT WORKS:
# This plugin uses an intelligent, independent cycle system for each server.
#
# 1.  A server with its own specific messages (e.g., 'survival') will create a
#     unique cycle: (Global Message -> Specific Survival Msg 1 -> Specific Survival Msg 2 -> Global Message -> ...)
#
# 2.  A server WITHOUT specific messages (e.g., 'lobby') will only cycle
#     through the global messages: (Global Message 1 -> Global Message 2 -> Global Message 1 -> ...)
#
# Each server's cycle runs independently and does not affect the others.
#
announcements:
  # --- GLOBAL ANNOUNCEMENTS ---
  # These messages will be part of the cycle for EVERY server.
  # To make a message global, add "all" to its 'servers' list.

  # Example 1: Standard CHAT message (default)
  - servers:
      - "all"
    lines:
      - "&eDon't forget to vote for our server every day!"
      - "&fUse the command &b/vote&f to get rewards."

  # Example 2: Global ACTIONBAR message with a SOUND (Features #2 & #5)
  - servers:
      - "all"
    type: "ACTIONBAR" # (NEW) Type: CHAT, ACTIONBAR, or TITLE
    sound: "entity.player.levelup" # (NEW) Sound (use Minecraft key, e.g., 'block.note_block.pling')
    lines:
      - "&dCheck out our webstore for exclusive ranks and items!"
      # Note: ACTIONBAR only uses the first line.

  # --- SERVER-SPECIFIC ANNOUNCEMENTS ---
  # These messages will ONLY be shown to players on the specified servers.
  # They will be inserted into the cycle after a global message.

  # Example 3: TITLE message for Skyblock/Oneblock (Feature #2)
  - servers:
      - "oneblock"
      - "skyblock" # You can target multiple servers with the same message block.
    type: "TITLE"
    lines:
      - "&aWelcome to the Sky!" # Line 1 is the main TITLE
      - "&fType &b/is&f to start your adventure." # Line 2 is the SUBTITLE

  - servers:
      - "oneblock"
      - "skyblock"
    lines:
      - "&bLooking for a team? Use &a/team&b to cooperate with others!"

  # Example 4: Standard CHAT message for Survival
  - servers:
      - "survival"
    lines:
      - "&cWatch out! PvP is enabled in the wilderness."
      - "&fClaim your land using a Golden Shovel to stay safe."
```
</details>

## License
This project is licensed under the [MIT](LICENSE)
