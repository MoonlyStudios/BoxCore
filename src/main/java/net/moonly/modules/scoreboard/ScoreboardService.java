package net.moonly.modules.scoreboard;

import fr.mrmicky.fastboard.FastBoard;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.moonly.Box; // Your main plugin class
import net.moonly.commands.staff.CustomTimerCommand;
import net.moonly.utils.ConfigFile; // Your ConfigFile utility class
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection; // NEW IMPORT
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler; // NEW IMPORT
import org.bukkit.event.Listener; // NEW IMPORT
import org.bukkit.event.player.PlayerJoinEvent; // NEW IMPORT
import org.bukkit.event.player.PlayerQuitEvent; // NEW IMPORT


public class ScoreboardService implements Listener { // Implements Listener for join/quit events
  private final Box services;

  public Box getServices() {
    return this.services;
  }

  private final Map<UUID, ScoreboardProfile> scoreboards = new ConcurrentHashMap<>();

  private ScoreboardTask scoreboardTask;

  private ConfigFile config;
  private FileConfiguration configFile;

  // NEW: Secciones de configuración para ambos scoreboards
  private ConfigurationSection defaultScoreboardConfig;
  private ConfigurationSection customTimerScoreboardConfig;

  private CustomTimerCommand customTimerCommand;

  public Map<UUID, ScoreboardProfile> getScoreboards() {
    return this.scoreboards;
  }

  public ScoreboardTask getScoreboardTask() {
    return this.scoreboardTask;
  }

  public ConfigFile getConfig() {
    return this.config;
  }

  public FileConfiguration getConfigFile() {
    return this.configFile;
  }

  public CustomTimerCommand getCustomTimerCommand() {
    return customTimerCommand;
  }

  public void setCustomTimerCommand(CustomTimerCommand customTimerCommand) {
    this.customTimerCommand = customTimerCommand;
    if (scoreboardTask != null) {
      scoreboardTask.setCustomTimerCommand(customTimerCommand);
    }
  }

  public ConfigurationSection getDefaultScoreboardConfig() {
    return defaultScoreboardConfig;
  }

  public ConfigurationSection getCustomTimerScoreboardConfig() {
    return customTimerScoreboardConfig;
  }

  public ScoreboardService(Box services) {
    this.services = services;
    this.config = new ConfigFile(services, "modules/scoreboard/scoreboard.yml");
    this.configFile = this.config.getConfig();

    this.defaultScoreboardConfig = configFile.getConfigurationSection("default-scoreboard");
    this.customTimerScoreboardConfig = configFile.getConfigurationSection("custom-timer-scoreboard");

    Bukkit.getPluginManager().registerEvents(this, services);

    this.scoreboardTask = new ScoreboardTask(this);
    if (this.customTimerCommand != null) { // Esto es poco probable que sea true aquí, pero por seguridad
      this.scoreboardTask.setCustomTimerCommand(this.customTimerCommand);
    }

    Bukkit.getOnlinePlayers().forEach(player -> {
      ScoreboardProfile scoreboardProfile = new ScoreboardProfile(player.getUniqueId(), new FastBoard(player), this.config);
      this.scoreboards.put(player.getUniqueId(), scoreboardProfile);
    });
  }

  public void unload() {
    if (this.scoreboardTask != null)
      this.scoreboardTask.cancel();
    this.scoreboards.forEach((uuid, profile) -> profile.getFastBoard().delete());
    this.scoreboards.clear();
  }

  public void reload() {
    this.config = new ConfigFile(services, "modules/scoreboard/scoreboard.yml");
    this.configFile = this.config.getConfig();

    this.defaultScoreboardConfig = configFile.getConfigurationSection("default-scoreboard");
    this.customTimerScoreboardConfig = configFile.getConfigurationSection("custom-timer-scoreboard");

    if (this.scoreboardTask != null)
      this.scoreboardTask.cancel();

    this.scoreboards.forEach((uuid, profile) -> profile.getFastBoard().delete());
    this.scoreboards.clear();

    this.scoreboardTask = new ScoreboardTask(this);
    if (this.customTimerCommand != null) { // Asegurarse de que se re-setee
      this.scoreboardTask.setCustomTimerCommand(this.customTimerCommand);
    }

    Bukkit.getOnlinePlayers().forEach(player -> {
      ScoreboardProfile scoreboardProfile = new ScoreboardProfile(player.getUniqueId(), new FastBoard(player), this.config);
      this.scoreboards.put(player.getUniqueId(), scoreboardProfile);
    });
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    if (!services.getConfig().getBoolean("modules.scoreboard.enabled", true)) {
      return;
    }
    if (!scoreboards.containsKey(player.getUniqueId())) {
      scoreboards.put(player.getUniqueId(), new ScoreboardProfile(player.getUniqueId(), new FastBoard(player), this.config));
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    ScoreboardProfile profile = scoreboards.remove(player.getUniqueId());
    if (profile != null) {
      profile.getFastBoard().delete();
    }
  }
}