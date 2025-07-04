package net.moonly.modules.deathmessage; // Asegúrate de que este paquete coincida exactamente con la estructura de tu proyecto.

import net.moonly.Box;
import net.moonly.utils.ConfigFile;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeathMessageManager {

    private final Box plugin;
    private final ConfigFile config;
    private Map<UUID, String> playerCustomDeathMessages;
    private List<String> predefinedDeathMessages;
    private String defaultDeathMessage;
    private boolean enableCustomDeathMessages;
    private int maxMessageLength;
    private String customMessagePermission;
    private String defaultPermission;
    private String clearPermission;
    private String guiPermission;
    private String writePermission;


    public DeathMessageManager(Box plugin) {
        this.plugin = plugin;
        this.config = new ConfigFile(plugin, "modules/deathmessages/deathmessages.yml");
        this.playerCustomDeathMessages = new HashMap<>();
        loadConfig();
        loadCustomMessages();
    }

    public Box getPlugin() {
        return plugin;
    }

    private void loadConfig() {
        config.reloadConfig();

        ConfigurationSection settings = config.getConfig().getConfigurationSection("settings");
        if (settings != null) {
            this.enableCustomDeathMessages = settings.getBoolean("enable-custom-death-messages", true);
            this.maxMessageLength = settings.getInt("max-message-length", 64);
            // Asegúrate de que el valor por defecto también se traduce si no se encuentra en la config.
            this.defaultDeathMessage = settings.getString("default-death-message", "%player% died."); // Keep this raw, translate later.
        } else {
            this.enableCustomDeathMessages = true;
            this.maxMessageLength = 64;
            this.defaultDeathMessage = "%player% died."; // Keep this raw, translate later.
        }

        this.predefinedDeathMessages = new ArrayList<>();
        List<String> predefinedSection = config.getConfig().getStringList("predefined-messages");
        if (predefinedSection != null) {
            for (String msg : predefinedSection) {
                // MODIFICACIÓN CLAVE: NO traducir los colores aquí.
                // Guardamos el mensaje con los códigos '&' sin traducir.
                predefinedDeathMessages.add(msg);
            }
        }

        ConfigurationSection permissions = config.getConfig().getConfigurationSection("permissions");
        if (permissions != null) {
            this.customMessagePermission = permissions.getString("set-custom-message", "sakuracore.deathmessage.set");
            this.defaultPermission = permissions.getString("use-default-message", "sakuracore.deathmessage.use");
            this.clearPermission = permissions.getString("clear-custom-message", "sakuracore.deathmessage.clear");
            this.guiPermission = permissions.getString("open-gui", "sakuracore.deathmessage.gui");
            this.writePermission = permissions.getString("write-in-gui", "sakuracore.deathmessage.write");
        } else {
            this.customMessagePermission = "sakuracore.deathmessage.set";
            this.defaultPermission = "sakuracore.deathmessage.use";
            this.clearPermission = "sakuracore.deathmessage.clear";
            this.guiPermission = "sakuracore.deathmessage.gui";
            this.writePermission = "sakuracore.deathmessage.write";
        }

        plugin.getLogger().info("Death messages configuration loaded.");
    }

    private void loadCustomMessages() {
        ConfigurationSection customMessagesSection = config.getConfig().getConfigurationSection("player-custom-messages");
        if (customMessagesSection != null) {
            for (String uuidStr : customMessagesSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String message = customMessagesSection.getString(uuidStr);
                    if (message != null) {
                        playerCustomDeathMessages.put(uuid, message);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID found in deathmessages.yml: " + uuidStr);
                }
            }
        }
        plugin.getLogger().info("Loaded " + playerCustomDeathMessages.size() + " custom death messages.");
    }

    public void saveCustomMessages() {
        ConfigurationSection customMessagesSection = config.getConfig().createSection("player-custom-messages");
        for (Map.Entry<UUID, String> entry : playerCustomDeathMessages.entrySet()) {
            customMessagesSection.set(entry.getKey().toString(), entry.getValue());
        }
        config.saveConfig();
        plugin.getLogger().info("Saved " + playerCustomDeathMessages.size() + " custom death messages.");
    }

    /**
     * Obtiene el mensaje de muerte para el JUGADOR QUE MUERE,
     * usando su mensaje personalizado o el default global.
     * Solo se usa si el asesino NO es un jugador.
     * @param killedPlayer El jugador que murió.
     * @param originalDeathMessage El mensaje de muerte original de Bukkit.
     * @return El mensaje de muerte final.
     */
    public String getDeathMessage(Player killedPlayer, String originalDeathMessage) {
        String customMessage = playerCustomDeathMessages.get(killedPlayer.getUniqueId());
        String messageToUse;

        if (customMessage != null && killedPlayer.hasPermission(customMessagePermission)) {
            messageToUse = customMessage;
        } else {
            messageToUse = defaultDeathMessage;
        }

        return processDeathMessagePlaceholders(killedPlayer, null, originalDeathMessage, messageToUse);
    }

    /**
     * Obtiene el mensaje de muerte cuando un JUGADOR MATA a otro.
     * El mensaje se basa en la configuración del JUGADOR QUE MATA.
     * @param killingPlayer El jugador que mató.
     * @param killedPlayer El jugador que murió.
     * @param originalDeathMessage El mensaje de muerte original de Bukkit.
     * @return El mensaje de muerte final.
     */
    public String getDeathMessageForKiller(Player killingPlayer, Player killedPlayer, String originalDeathMessage) {
        String customMessage = playerCustomDeathMessages.get(killingPlayer.getUniqueId());
        String messageToUse;

        if (customMessage != null && killingPlayer.hasPermission(customMessagePermission)) {
            messageToUse = customMessage;
        } else {
            messageToUse = defaultDeathMessage;
        }

        return processDeathMessagePlaceholders(killedPlayer, killingPlayer, originalDeathMessage, messageToUse);
    }

    /**
     * Método auxiliar para procesar todos los placeholders comunes en los mensajes de muerte.
     * @param killedPlayer El jugador que ha muerto.
     * @param killingPlayer El jugador que ha matado (puede ser null si el asesino no es un jugador).
     * @param originalDeathMessage El mensaje de muerte original de Bukkit.
     * @param messageTemplate La plantilla de mensaje a procesar.
     * @return El mensaje final con todos los placeholders reemplazados y colores.
     */
    private String processDeathMessagePlaceholders(Player killedPlayer, Player killingPlayer, String originalDeathMessage, String messageTemplate) {
        String messageToProcess = messageTemplate;

        String killerName = (killingPlayer != null) ? killingPlayer.getName() : "";
        String deathCause = originalDeathMessage;
        LivingEntity directKillerEntity = killedPlayer.getKiller();

        if (directKillerEntity != null) {
            deathCause = originalDeathMessage.replace(killedPlayer.getName(), "").replace(directKillerEntity.getName(), "").trim();
            deathCause = deathCause.replaceAll("^(was slain by|was shot by|was pricked to death by|was blown up by|died by|was killed by|fue asesinado por)", "").trim();
            if (deathCause.isEmpty()) {
                deathCause = "died";
            }
        } else {
            deathCause = originalDeathMessage.replace(killedPlayer.getName(), "").trim();
            deathCause = deathCause.replaceAll("^(died|was killed|fell|drowned|burned|starved|froze|suffocated|was struck by lightning|was obliterated|was impaled|was squashed)", "").trim();
            if (deathCause.isEmpty()) {
                deathCause = "died";
            }
        }

        // Aplicar PlaceholderAPI primero
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            messageToProcess = PlaceholderAPI.setPlaceholders(killedPlayer, messageToProcess);
            if (killingPlayer != null) {
                messageToProcess = PlaceholderAPI.setPlaceholders(killingPlayer, messageToProcess);
            }
        }

        // Reemplazar placeholders personalizados de tu plugin
        messageToProcess = messageToProcess.replace("%player%", killedPlayer.getName());
        messageToProcess = messageToProcess.replace("%killer%", killerName.isEmpty() ? "something" : killerName);
        messageToProcess = messageToProcess.replace("%cause%", deathCause);
        messageToProcess = messageToProcess.replace("%original_death_message%", originalDeathMessage);

        // Traducción de colores solo una vez al final del procesamiento.
        return ChatColor.translateAlternateColorCodes('&', messageToProcess);
    }

    public void setPlayerCustomDeathMessage(UUID playerUUID, String message) {
        playerCustomDeathMessages.put(playerUUID, message);
        saveCustomMessages();
    }

    public void clearPlayerCustomDeathMessage(UUID playerUUID) {
        playerCustomDeathMessages.remove(playerUUID);
        saveCustomMessages();
    }

    public String getPlayerCustomDeathMessage(UUID playerUUID) {
        return playerCustomDeathMessages.get(playerUUID);
    }

    public List<String> getPredefinedDeathMessages() {
        return predefinedDeathMessages;
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }

    public String getCustomMessagePermission() {
        return customMessagePermission;
    }

    public String getDefaultPermission() {
        return defaultPermission;
    }

    public String getClearPermission() {
        return clearPermission;
    }

    public String getGuiPermission() {
        return guiPermission;
    }
    public String getWritePermission() {
        return writePermission;
    }

    public boolean isEnableCustomDeathMessages() {
        return enableCustomDeathMessages;
    }

    public void reload() {
        playerCustomDeathMessages.clear();
        loadConfig();
        loadCustomMessages();
    }
}