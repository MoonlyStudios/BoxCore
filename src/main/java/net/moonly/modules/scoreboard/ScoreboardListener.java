package net.moonly.modules.scoreboard;

import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ScoreboardListener implements Listener {

  private final ScoreboardService service;

  public ScoreboardListener(ScoreboardService service) {
    this.service = service;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    if (!service.getServices().getConfig().getBoolean("modules.scoreboard.enabled", true)) {
      return;
    }

    if (service.getScoreboards().containsKey(player.getUniqueId())) {
      service.getScoreboards().get(player.getUniqueId()).getFastBoard().delete(); // Clean up old one
      service.getScoreboards().remove(player.getUniqueId());
    }

    ScoreboardProfile scoreboardProfile = new ScoreboardProfile(player.getUniqueId(), new FastBoard(player), service.getConfig());
    service.getScoreboards().put(player.getUniqueId(), scoreboardProfile);
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (service.getScoreboards().containsKey(player.getUniqueId())) {
      service.getScoreboards().get(player.getUniqueId()).getFastBoard().delete();
      service.getScoreboards().remove(player.getUniqueId());
    }
  }
}