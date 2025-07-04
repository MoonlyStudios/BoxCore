package net.moonly.handlers; // Asegúrate de que este sea el paquete correcto

import net.moonly.Box; // Importa tu clase principal Box
import net.moonly.modules.Spawn.SpawnManager; // Importa tu SpawnManager

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent; // <-- Cambiado a PlayerDeathEvent

import java.util.logging.Level;

public class AutoRespawnListener implements Listener {

    private final Box plugin;
    private final SpawnManager spawnManager;
    private boolean enableAutoRespawn;
    private int respawnDelayTicks; // Nuevo campo para el retraso en ticks

    public AutoRespawnListener(Box plugin, SpawnManager spawnManager, boolean enableAutoRespawn) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
        this.enableAutoRespawn = enableAutoRespawn;
        this.respawnDelayTicks = respawnDelayTicks; // Inicializa el retraso
    }

    public void setEnableAutoRespawn(boolean enableAutoRespawn) {
        this.enableAutoRespawn = enableAutoRespawn;
        plugin.getLogger().log(Level.INFO, "AutoRespawn module enabled status set to: " + enableAutoRespawn);
    }

    // Puedes agregar un setter para el delay si quieres recargarlo
    public void setRespawnDelayTicks(int respawnDelayTicks) {
        this.respawnDelayTicks = respawnDelayTicks;
        plugin.getLogger().log(Level.INFO, "AutoRespawn delay set to: " + respawnDelayTicks + " ticks.");
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) { // <-- Cambiado a PlayerDeathEvent
        plugin.getLogger().log(Level.INFO, "AutoRespawnListener: PlayerDeathEvent triggered for " + event.getEntity().getName());

        if (!enableAutoRespawn) {
            plugin.getLogger().log(Level.INFO, "AutoRespawn module is disabled. Skipping auto-respawn for " + event.getEntity().getName());
            return;
        }

        Player player = event.getEntity();
        Location spawnLocation = spawnManager.getSpawnLocation();

        if (spawnLocation != null) {
            // No usamos event.setRespawnLocation(spawnLocation) aquí porque el PlayerDeathEvent
            // es demasiado temprano en el proceso de reaparición para eso.
            // La ubicación se establecerá cuando el jugador haga spigot().respawn() o teleport.

            plugin.getLogger().log(Level.INFO, "AutoRespawnListener: Scheduling respawn for " + player.getName() + " in " + respawnDelayTicks + " ticks.");

            // Programar la reaparición después del retraso configurado
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) { // Asegurarse de que el jugador sigue online

                    // Intentar el método directo de Spigot primero
                    if (player.spigot() != null) {
                        try {
                            player.spigot().respawn();
                            plugin.getLogger().log(Level.INFO, "AutoRespawnListener: Forced respawn via Spigot API for " + player.getName() + " after delay.");
                        } catch (NoSuchMethodError | Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Spigot API respawn failed for " + player.getName() + ". Attempting teleport as fallback. Error: " + e.getMessage());
                            player.teleport(spawnLocation); // Fallback a teletransporte
                        }
                    } else {
                        // Si la API de Spigot no está disponible, solo teletransportar
                        player.teleport(spawnLocation);
                        plugin.getLogger().log(Level.INFO, "AutoRespawnListener: Teleported " + player.getName() + " to spawn (Spigot API not available) after delay.");
                    }
                    // Opcional: enviar un mensaje al jugador
                    // player.sendMessage(ChatColor.GREEN + "¡Has reaparecido después de un retraso!");
                } else {
                    plugin.getLogger().log(Level.INFO, "AutoRespawnListener: Player " + player.getName() + " disconnected before delayed respawn could occur.");
                }
            }, respawnDelayTicks); // Usamos el retraso configurado
        } else {
            plugin.getLogger().log(Level.WARNING, "No spawn location set for auto-respawn. Player " + player.getName() + " will respawn at world default after delay.");
            // No podemos enviar un mensaje aquí porque el jugador aún está en la pantalla de muerte
            // y no puede recibir mensajes de chat hasta que reaparezca.
        }
    }
}