package net.moonly.modules.Economy;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class EconomyListener implements Listener {

    private final EconomyManager economyManager;

    public EconomyListener(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Ejecutar de forma asíncrona para no bloquear el hilo principal
        new BukkitRunnable() {
            @Override
            public void run() {
                economyManager.getDatabase().getBalance(player.getUniqueId()).thenAccept(balance -> {
                    // Si el balance es 0.0 y el jugador nunca ha jugado antes (primera vez en el servidor)
                    // También se comprueba si el nombre en la DB está desactualizado
                    boolean isNewPlayer = !player.hasPlayedBefore();
                    boolean hasZeroBalance = (balance == 0.0);

                    // Si es nuevo jugador O tiene balance cero Y su nombre no está en la DB o está desactualizado
                    if (isNewPlayer || hasZeroBalance) {
                        economyManager.getDatabase().getPlayerName(player.getUniqueId()).thenAccept(storedName -> {
                            boolean needsUpdate = storedName == null || !storedName.equals(player.getName());

                            if (isNewPlayer && hasZeroBalance) { // Es un jugador nuevo y no tiene dinero
                                economyManager.getDatabase().setBalance(player.getUniqueId(), player.getName(), economyManager.getInitialBalance()).thenRun(() -> {
                                    // Este mensaje se envía al jugador en el hilo principal
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            player.sendMessage(economyManager.getMessage("initial-balance-given")
                                                    .replace("{amount}", economyManager.formatBalance(economyManager.getInitialBalance()))
                                                    .replace("{symbol}", economyManager.getCurrencySymbol()));
                                        }
                                    }.runTask(economyManager.getPlugin());
                                    economyManager.getPlugin().getLogger().log(Level.INFO, "Player " + player.getName() + " received initial balance of " + economyManager.getInitialBalance());
                                });
                            } else if (needsUpdate) { // No es nuevo jugador, pero su nombre está desactualizado
                                economyManager.getDatabase().setBalance(player.getUniqueId(), player.getName(), balance); // Actualiza el nombre, balance no cambia
                                economyManager.getPlugin().getLogger().log(Level.INFO, "Updated player name in economy database for " + player.getName());
                            }
                        });
                    }
                }).exceptionally(e -> {
                    economyManager.getPlugin().getLogger().log(Level.SEVERE, "Error checking/setting initial balance for " + player.getName(), e);
                    return null;
                });
            }
        }.runTaskLaterAsynchronously(economyManager.getPlugin(), 20L); // Esperar 1 segundo para asegurar que el jugador esté cargado
    }
}