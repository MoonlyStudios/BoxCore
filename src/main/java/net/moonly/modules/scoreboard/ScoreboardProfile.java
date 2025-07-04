package net.moonly.modules.scoreboard; // THIS MUST EXACTLY MATCH THE FOLDER PATH

import fr.mrmicky.fastboard.FastBoard;
import net.moonly.utils.ConfigFile;

import java.util.UUID;

public class ScoreboardProfile {
  private final UUID playerUUID;
  private final FastBoard fastBoard;
  private final ConfigFile config; // Reference to the scoreboard config

  public ScoreboardProfile(UUID playerUUID, FastBoard fastBoard, ConfigFile config) {
    this.playerUUID = playerUUID;
    this.fastBoard = fastBoard;
    this.config = config;
  }

  public UUID getPlayerUUID() {
    return playerUUID;
  }

  public FastBoard getFastBoard() {
    return fastBoard;
  }

  public ConfigFile getConfig() {
    return config;
  }
}