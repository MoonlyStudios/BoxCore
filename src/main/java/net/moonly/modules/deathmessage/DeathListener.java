package net.moonly.modules.deathmessage;

import org.bukkit.Bukkit; // Importar Bukkit para el broadcast
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import java.util.logging.Level;

public class DeathListener implements Listener {

    private final DeathMessageManager manager;

    public DeathListener(DeathMessageManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST) // Mantenemos la prioridad más alta
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!manager.isEnableCustomDeathMessages()) {
            return;
        }

        Player killedPlayer = event.getEntity();
        LivingEntity killer = killedPlayer.getKiller();

        String originalDeathMessage = event.getDeathMessage();
        String finalDeathMessage;

        if (killer instanceof Player) {
            Player killingPlayer = (Player) killer;
            finalDeathMessage = manager.getDeathMessageForKiller(killingPlayer, killedPlayer, originalDeathMessage);
        } else {
            finalDeathMessage = manager.getDeathMessage(killedPlayer, originalDeathMessage);
        }

        // Línea de log crucial para depuración, verifica que los símbolos '§' estén presentes
        manager.getPlugin().getLogger().log(Level.INFO, "Intentando emitir mensaje de muerte para " + killedPlayer.getName() + ": " + finalDeathMessage);

        // --- CAMBIO CLAVE: CANCELAR MENSAJE ORIGINAL Y EMITIR MANUALMENTE ---
        event.setDeathMessage(null); // Esto cancela el mensaje de muerte por defecto de Bukkit
        Bukkit.broadcastMessage(finalDeathMessage); // Esto envía tu mensaje personalizado directamente al chat
        // --- FIN DEL CAMBIO ---
    }
}