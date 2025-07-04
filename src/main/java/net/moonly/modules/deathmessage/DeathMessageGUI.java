package net.moonly.modules.deathmessage; // Correct package for GUI classes

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
// REMOVER: import org.bukkit.event.Listener; // This line should already be removed as DeathMessageGUI does not implement Listener directly
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;

// NO IMPLEMENTA LISTENER directamente, la gestión de eventos la hace GUIManager
public class DeathMessageGUI {

    private final DeathMessageManager manager;
    private final Player player;
    private final Inventory inventory;
    private final GUIManager guiManager; // La instancia del manejador central de GUIs

    // CONSTRUCTOR: Ahora recibe la instancia de GUIManager
    public DeathMessageGUI(DeathMessageManager manager, Player player, GUIManager guiManager) {
        this.manager = manager;
        this.player = player;
        this.guiManager = guiManager; // Asigna la instancia de GUIManager
        this.inventory = Bukkit.createInventory(null, 36, ChatColor.BLACK + "Death Message Customization");

        initializeItems();
        // REMOVER: Bukkit.getPluginManager().registerEvents(this, manager.getPlugin()); // Esta línea ya no va aquí
    }

    private void initializeItems() {
        // Player's current message display (Skull)
        ItemStack playerSkull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) playerSkull.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName(ChatColor.YELLOW + "Your Current Death Message");
        String currentMessage = manager.getPlayerCustomDeathMessage(player.getUniqueId());
        List<String> lore = Arrays.asList(
                ChatColor.GRAY + "Current:",
                (currentMessage != null ? ChatColor.translateAlternateColorCodes('&', currentMessage) : ChatColor.WHITE + "Using Default"),
                "",
                ChatColor.AQUA + "Click to clear your custom message."
        );
        skullMeta.setLore(lore);
        playerSkull.setItemMeta(skullMeta);
        inventory.setItem(4, playerSkull);

        // Predefined Messages
        int slot = 9;
        for (String message : manager.getPredefinedDeathMessages()) {
            if (slot >= 45) break;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Predefined Message");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Message:",
                    ChatColor.translateAlternateColorCodes('&', message),
                    "",
                    ChatColor.AQUA + "Click to set this message."
            ));
            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                ItemStack grayGlassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta glassMeta = grayGlassPane.getItemMeta();
                glassMeta.setDisplayName(" ");
                grayGlassPane.setItemMeta(glassMeta);
                inventory.setItem(i, grayGlassPane);
            }
        }
    }

    public void openInventory() {
        player.openInventory(inventory);
        // REGISTRAR LA GUI CON EL MANEJADOR CENTRAL CUANDO SE ABRE
        // Esto permite que GUIManager capture los clics para esta GUI
        guiManager.registerDeathMessageGUI(player, this);
    }

    public Inventory getInventory() {
        return inventory;
    }

    // MÉTODO PARA MANEJAR CLICS, LLAMADO EXTERNAMENTE POR GUIManager
    public void handleClick(InventoryClickEvent event) {
        // El evento ya fue cancelado por GUIManager. No se necesita cancelar aquí.

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getItemMeta() == null) {
            return;
        }

        // Lógica del clic en la calavera para borrar el mensaje
        if (clickedItem.getType() == Material.PLAYER_HEAD && event.getSlot() == 4) {
            if (player.hasPermission(manager.getClearPermission())) {
                manager.clearPlayerCustomDeathMessage(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Your custom death message has been cleared.");
                player.closeInventory();
                guiManager.unregisterDeathMessageGUI(player); // Desregistrar la GUI al cerrar
            } else {
                player.sendMessage(ChatColor.RED + "You don't have permission to clear your death message.");
            }
            return;
        }

        // Lógica del clic en mensajes predefinidos (papel)
        if (clickedItem.getType() == Material.PAPER) {
            if (player.hasPermission(manager.getCustomMessagePermission())) {
                List<String> lore = clickedItem.getItemMeta().getLore();
                if (lore != null && lore.size() > 1) {
                    String messageLine = ChatColor.stripColor(lore.get(1));
                    manager.setPlayerCustomDeathMessage(player.getUniqueId(), messageLine);
                    player.sendMessage(ChatColor.GREEN + "Your death message has been set to: " + ChatColor.translateAlternateColorCodes('&', messageLine.replace("%player%", "You")));
                    player.closeInventory();
                    guiManager.unregisterDeathMessageGUI(player); // Desregistrar la GUI al cerrar
                }
            } else {
                player.sendMessage(ChatColor.RED + "You don't have permission to set a custom death message.");
            }
            return;
        }

    }
}