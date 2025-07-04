package net.moonly.modules.Spawn;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent; // Import this
// Importamos Level para los logs
import java.util.logging.Level;

public class SpawnListener implements Listener {

    private final SpawnManager spawnManager;

    public SpawnListener(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Logging para depuración
        spawnManager.getPlugin().getLogger().log(Level.INFO, "SpawnListener: PlayerJoinEvent triggered for " + event.getPlayer().getName());

        // Teletransportar al jugador al spawn al unirse si es su primera vez
        // o si prefieres que todos los jugadores vayan al spawn al unirse.
        // Solo si el SpawnManager tiene un spawnLocation válido.
        if (!event.getPlayer().hasPlayedBefore() && spawnManager.getSpawnLocation() != null) {
            event.getPlayer().teleport(spawnManager.getSpawnLocation());
            spawnManager.getPlugin().getLogger().log(Level.INFO, "SpawnListener: Teleported new player " + event.getPlayer().getName() + " to spawn on join.");
        } else if (spawnManager.getSpawnLocation() == null) {
            spawnManager.getPlugin().getLogger().log(Level.WARNING, "SpawnListener: No spawn location set. Player " + event.getPlayer().getName() + " will not be teleported on join.");
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        spawnManager.getPlugin().getLogger().log(Level.INFO, "SpawnListener: PlayerRespawnEvent triggered for " + event.getPlayer().getName());
        if (spawnManager.getSpawnLocation() != null) {
            event.setRespawnLocation(spawnManager.getSpawnLocation());
            spawnManager.getPlugin().getLogger().log(Level.INFO, "SpawnListener: Set respawn location for " + event.getPlayer().getName() + " to default spawn.");
        } else {
            spawnManager.getPlugin().getLogger().log(Level.WARNING, "SpawnListener: No spawn location set. Player " + event.getPlayer().getName() + " will respawn at world default.");
            // Optionally, you could send a message to the player here if the spawn isn't set.
            // event.getPlayer().sendMessage(spawnManager.getMessage("spawn-not-set"));
        }
    }
}