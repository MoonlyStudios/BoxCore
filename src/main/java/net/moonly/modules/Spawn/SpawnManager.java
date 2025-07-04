package net.moonly.modules.Spawn;

import net.moonly.Box;
import net.moonly.utils.LocationSerializer; // Asegúrate de que esta clase exista y funcione correctamente
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SpawnManager {

    private final Box plugin;
    private FileConfiguration config;
    private File configFile;

    private Location spawnLocation;

    private int teleportCooldownSeconds;
    private boolean teleportCrossWorld;

    private boolean enableChatMessages;
    private boolean enableTitle;
    private boolean enableSubtitle;
    private boolean enableActionbar;

    private boolean enableTitleCountdown;
    private boolean enableSubtitleCountdown;

    private boolean enableTitleNoCooldown;
    private boolean enableSubtitleNoCooldown;

    private String teleportTitleNoCooldownMessage;
    private String teleportSubtitleNoCooldownMessage;

    private final Map<UUID, Integer> teleportingPlayers;
    private final Map<UUID, Location> teleportStartingLocations;

    public SpawnManager(Box plugin) {
        this.plugin = plugin;
        this.teleportingPlayers = new ConcurrentHashMap<>();
        this.teleportStartingLocations = new ConcurrentHashMap<>();

        loadConfig();
        loadSpawnLocation();
    }

    public Box getPlugin() {
        return plugin;
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "modules/spawn/spawn.yml");
        if (!configFile.exists()) {
            plugin.saveResource("modules/spawn/spawn.yml", false);
            plugin.getLogger().log(Level.INFO, "Created default spawn.yml.");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().log(Level.INFO, "Loaded spawn.yml from path: " + configFile.getAbsolutePath());

        this.teleportCooldownSeconds = config.getInt("settings.teleport-cooldown-seconds", 3);
        this.teleportCrossWorld = config.getBoolean("settings.teleport-cross-world", true);

        this.enableChatMessages = config.getBoolean("messages.enable-chat-messages", true);
        this.enableTitle = config.getBoolean("messages.enable-title", true);
        this.enableSubtitle = config.getBoolean("messages.enable-subtitle", true);
        this.enableActionbar = config.getBoolean("messages.enable-actionbar", true);

        this.enableTitleCountdown = config.getBoolean("messages.enable-title-countdown", false);
        this.enableSubtitleCountdown = config.getBoolean("messages.enable-subtitle-countdown", false);

        this.enableTitleNoCooldown = config.getBoolean("messages.enable-title-no-cooldown", true);
        this.enableSubtitleNoCooldown = config.getBoolean("messages.enable-subtitle-no-cooldown", true);

        this.teleportTitleNoCooldownMessage = config.getString("messages.teleport-title-no-cooldown", "&aSpawn!");
        this.teleportSubtitleNoCooldownMessage = config.getString("messages.teleport-subtitle-no-cooldown", "&fYou arrived instantly.");

        plugin.getLogger().info("Loaded spawn.yml configuration settings.");
    }

    public String getMessage(String path) {
        String message = config.getString("messages." + path);
        if (message == null) {
            plugin.getLogger().log(Level.WARNING, "Message path 'messages." + path + "' not found in spawn.yml. Using fallback.");
            return ChatColor.RED + "Message not found: " + path;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getTeleportTitleNoCooldownMessage() {
        return ChatColor.translateAlternateColorCodes('&', teleportTitleNoCooldownMessage);
    }

    public String getTeleportSubtitleNoCooldownMessage() {
        return ChatColor.translateAlternateColorCodes('&', teleportSubtitleNoCooldownMessage);
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;
        saveSpawnLocation();
        plugin.getLogger().log(Level.INFO, "Spawn location set to: " + location.toString());
    }

    private void loadSpawnLocation() {
        ConfigurationSection spawnSection = config.getConfigurationSection("spawn-location");
        if (spawnSection != null) {
            // Pasar un mapa directo para deserializar
            this.spawnLocation = LocationSerializer.deserializeLocation(spawnSection.getValues(true));
        } else {
            this.spawnLocation = null;
        }

        if (this.spawnLocation == null) {
            plugin.getLogger().warning("Spawn location not found or invalid in spawn.yml (section 'spawn-location' missing or deserialization failed). Please set one using /setspawn.");
        } else {
            plugin.getLogger().info("Spawn location loaded successfully: " + spawnLocation.getWorld().getName() + " " + (int)spawnLocation.getX() + "," + (int)spawnLocation.getY() + "," + (int)spawnLocation.getZ());
        }
    }

    private void saveSpawnLocation() {
        if (spawnLocation != null) {
            config.set("spawn-location", LocationSerializer.serializeLocation(spawnLocation));
        } else {
            config.set("spawn-location", null);
        }
        try {
            config.save(configFile);
            plugin.getLogger().info("Spawn location saved to spawn.yml.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving spawn location to spawn.yml:", e);
        }
    }

    public void teleportPlayerToSpawn(Player player) {
        plugin.getLogger().log(Level.INFO, "Attempting teleportPlayerToSpawn for " + player.getName());
        plugin.getLogger().log(Level.INFO, "Current spawnLocation in manager: " + (spawnLocation != null ? spawnLocation.toString() : "NULL"));

        if (spawnLocation == null) {
            plugin.getLogger().log(Level.WARNING, "teleportPlayerToSpawn: spawnLocation is NULL for " + player.getName() + ". Cannot teleport.");
            player.sendMessage(getMessage("spawn-not-set"));
            return;
        }

        if (teleportingPlayers.containsKey(player.getUniqueId())) {
            player.sendMessage(getMessage("teleport-already-in-progress"));
            return;
        }

        if (!teleportCrossWorld && !player.getWorld().equals(spawnLocation.getWorld())) {
            player.sendMessage(getMessage("teleport-cross-world-disabled"));
            return;
        }

        if (teleportCooldownSeconds > 0) {
            if (enableChatMessages) {
                player.sendMessage(getMessage("teleport-countdown-start").replace("{time}", String.valueOf(teleportCooldownSeconds)));
            }
            teleportStartingLocations.put(player.getUniqueId(), player.getLocation());

            int taskId = new BukkitRunnable() {
                int timeLeft = teleportCooldownSeconds;
                final UUID playerId = player.getUniqueId();

                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancelTeleport(playerId);
                        cancel();
                        return;
                    }
                    if (teleportStartingLocations.containsKey(playerId)) {
                        Location currentLoc = player.getLocation();
                        Location startLoc = teleportStartingLocations.get(playerId);
                        if (currentLoc.distanceSquared(startLoc) > 0.1 * 0.1) {
                            if (enableChatMessages) {
                                player.sendMessage(getMessage("teleport-cancelled-moved"));
                            }
                            if (enableActionbar) {
                                player.sendActionBar(ChatColor.RED + getMessage("actionbar-teleport-cancelled"));
                            }
                            cancelTeleport(playerId);
                            player.sendTitle("", "", 0, 0, 0);
                            cancel();
                            return;
                        }
                    }

                    if (timeLeft > 0) {
                        String titleMsg = enableTitleCountdown ? getMessage("title-teleporting").replace("{time}", String.valueOf(timeLeft)) : "";
                        String subtitleMsg = enableSubtitleCountdown ? getMessage("subtitle-teleporting").replace("{time}", String.valueOf(timeLeft)) : "";

                        if (enableTitleCountdown || enableSubtitleCountdown) {
                            player.sendTitle(titleMsg, subtitleMsg, 0, 25, 0);
                        }

                        if (enableActionbar) {
                            player.sendActionBar(getMessage("actionbar-teleporting").replace("{time}", String.valueOf(timeLeft)));
                        }
                        timeLeft--;
                    } else {
                        player.sendTitle("", "", 0, 0, 0);

                        plugin.getLogger().log(Level.INFO, "Teleporting " + player.getName() + " after cooldown to: " + (spawnLocation != null ? spawnLocation.toString() : "NULL_SPAWN_AFTER_COOLDOWN_CHECK"));
                        player.teleport(spawnLocation);

                        if (enableTitle) {
                            player.sendTitle(getMessage("teleport-title"), "", 10, 70, 20);
                        }
                        if (enableSubtitle) {
                            player.sendTitle("", getMessage("teleport-subtitle"), 0, 70, 20);
                        }
                        if (enableChatMessages) {
                            player.sendMessage(getMessage("teleport-success"));
                        }
                        if (enableActionbar) {
                            player.sendActionBar(ChatColor.GREEN + getMessage("actionbar-teleport-success"));
                        }
                        cancelTeleport(playerId);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L).getTaskId();
            teleportingPlayers.put(player.getUniqueId(), taskId);
        } else {
            // No cooldown active - instant teleport
            final Player targetPlayer = player;
            final Location finalSpawnLocation = spawnLocation;

            plugin.getLogger().log(Level.INFO, "Teleporting " + targetPlayer.getName() + " instantly to: " + (finalSpawnLocation != null ? finalSpawnLocation.toString() : "NULL_SPAWN_INSTANT_CHECK"));

            targetPlayer.teleport(finalSpawnLocation);

            if (enableChatMessages) {
                targetPlayer.sendMessage(getMessage("teleport-success"));
            }
            if (enableActionbar) {
                targetPlayer.sendActionBar(ChatColor.GREEN + getMessage("actionbar-teleport-success"));
            }

            if (enableTitleNoCooldown || enableSubtitleNoCooldown) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!targetPlayer.isOnline()) return;

                        String titleMsg = enableTitleNoCooldown ? getTeleportTitleNoCooldownMessage() : "";
                        String subtitleMsg = enableSubtitleNoCooldown ? getTeleportSubtitleNoCooldownMessage() : "";

                        targetPlayer.sendTitle(titleMsg, subtitleMsg, 10, 70, 20);

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (targetPlayer.isOnline()) {
                                    targetPlayer.sendTitle(titleMsg, subtitleMsg, 10, 70, 20);
                                }
                            }
                        }.runTaskLater(plugin, 5L);
                    }
                }.runTaskLater(plugin, 2L);
            }
        }
    }

    public void cancelTeleport(UUID playerUUID) {
        if (teleportingPlayers.containsKey(playerUUID)) {
            plugin.getServer().getScheduler().cancelTask(teleportingPlayers.remove(playerUUID));
        }
        teleportStartingLocations.remove(playerUUID);
    }

    public void shutdown() {
        for (Integer taskId : teleportingPlayers.values()) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
        teleportingPlayers.clear();
        teleportStartingLocations.clear();
        plugin.getLogger().info("Spawn teleportation tasks cancelled on shutdown.");
    }
}