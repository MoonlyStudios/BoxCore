package net.moonly.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ConfigFile {

    private final JavaPlugin plugin;
    private final String fileName; // This is the full path, e.g., "modules/scoreboard/scoreboard.yml"
    private File configFile;
    private FileConfiguration fileConfiguration;

    public ConfigFile(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName; // Store the full path as fileName

        // Correctly initialize configFile with the full path inside the plugin's data folder
        this.configFile = new File(plugin.getDataFolder(), fileName);
        saveDefaultConfig();
    }

    public void reloadConfig() {
        if (configFile == null) {
            // Should already be initialized in constructor
            configFile = new File(plugin.getDataFolder(), fileName);
        }
        fileConfiguration = YamlConfiguration.loadConfiguration(configFile);

        try (InputStream defConfigStream = plugin.getResource(fileName)) { // Use fileName (full path) to get resource
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defConfigStream));
                fileConfiguration.setDefaults(defConfig);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load default config for " + fileName + ": " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        if (fileConfiguration == null) {
            reloadConfig();
        }
        return fileConfiguration;
    }

    public void saveConfig() {
        if (fileConfiguration == null || configFile == null) {
            return;
        }
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save config to " + configFile + ": " + ex.getMessage());
        }
    }

    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            // Ensure parent directories exist based on the full file path
            configFile.getParentFile().mkdirs();
            try (InputStream defaultStream = plugin.getResource(fileName)) { // Use fileName (full path) to get resource
                if (defaultStream != null) {
                    Files.copy(defaultStream, configFile.toPath());
                } else {
                    plugin.getLogger().warning("Default config file '" + fileName + "' not found in plugin JAR resources. Cannot save default.");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save default config for " + fileName + ": " + e.getMessage());
            }
        }
    }
}