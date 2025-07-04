package net.moonly.modules.Kit.editor;

import net.moonly.modules.Kit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitEditorMainGUI {

    private final KitManager kitManager;

    public KitEditorMainGUI(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Kit Editor - Main Menu");

        ItemStack kitsIcon = new ItemStack(Material.CHEST);
        ItemMeta kitsMeta = kitsIcon.getItemMeta();
        if (kitsMeta != null) {
            kitsMeta.setDisplayName(ChatColor.GOLD + "Gestionar Kits");
            kitsMeta.setLore(java.util.Arrays.asList(ChatColor.GRAY + "Edita la posición, nombre, ítems", ChatColor.GRAY + "e icono de cada kit."));
            kitsIcon.setItemMeta(kitsMeta);
        }
        gui.setItem(11, kitsIcon);

        ItemStack configIcon = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta configMeta = configIcon.getItemMeta();
        if (configMeta != null) {
            configMeta.setDisplayName(ChatColor.GOLD + "Configuración del Menú Principal");
            configMeta.setLore(java.util.Arrays.asList(ChatColor.GRAY + "Cambia el título y las filas", ChatColor.GRAY + "del menú principal de kits."));
            configIcon.setItemMeta(configMeta);
        }
        gui.setItem(15, configIcon);

        player.openInventory(gui);
    }
}