package net.moonly.modules.deathmessage;

import net.moonly.Box;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent; // Considerar este evento

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIManager implements Listener {

    private final Box plugin;
    private final Map<UUID, DeathMessageGUI> activeDeathMessageGUIs;

    public GUIManager(Box plugin) {
        this.plugin = plugin;
        this.activeDeathMessageGUIs = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin); // <--- SOLO AQUÍ SE REGISTRA EL LISTENER
    }

    public void registerDeathMessageGUI(Player player, DeathMessageGUI gui) {
        activeDeathMessageGUIs.put(player.getUniqueId(), gui);
    }

    public void unregisterDeathMessageGUI(Player player) {
        activeDeathMessageGUIs.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();

        if (activeDeathMessageGUIs.containsKey(playerUUID)) {
            DeathMessageGUI gui = activeDeathMessageGUIs.get(playerUUID);
            if (event.getInventory().equals(gui.getInventory())) {
                event.setCancelled(true); // <--- ESTA LÍNEA ES CLAVE PARA PREVENIR MOVER/SOLTAR ÍTEMS
                gui.handleClick(event); // Delegar el clic a la GUI específica
            }
        }
    }

    // Opcional: Desregistrar la GUI cuando el jugador la cierra.
    // Esto es importante para limpiar recursos.
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Solo desregistra si el inventario que se cierra es uno de nuestras GUIs de DeathMessage
        if (activeDeathMessageGUIs.containsKey(playerUUID) && event.getInventory().equals(activeDeathMessageGUIs.get(playerUUID).getInventory())) {
            unregisterDeathMessageGUI(player);
        }
    }
}