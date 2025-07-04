package net.moonly.modules.Kit.editor;

import net.moonly.modules.Kit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KitConfigEditorGUI {

    private final KitManager kitManager;

    public static final Map<UUID, Boolean> playersWaitingForTitleInput = new HashMap<>();


    public KitConfigEditorGUI(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Configuración del Menú Principal");

        ItemStack rowsButton = new ItemStack(Material.NETHER_STAR);
        ItemMeta rowsMeta = rowsButton.getItemMeta();
        if (rowsMeta != null) {
            rowsMeta.setDisplayName(ChatColor.GOLD + "Cambiar Filas del Menú Principal");
            rowsMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click para alternar filas (1-6).",
                    ChatColor.GRAY + "(Actual: " + kitManager.getMainGuiRows() + " filas)"
            ));
            rowsButton.setItemMeta(rowsMeta);
        }
        gui.setItem(11, rowsButton);

        ItemStack titleButton = new ItemStack(Material.PAPER);
        ItemMeta titleMeta = titleButton.getItemMeta();
        if (titleMeta != null) {
            titleMeta.setDisplayName(ChatColor.GOLD + "Cambiar Título del Menú Principal");
            titleMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click para cambiar el título en el chat.",
                    ChatColor.GRAY + "(Actual: " + ChatColor.stripColor(kitManager.getGuiTitle()) + ")"
            ));
            titleButton.setItemMeta(titleMeta);
        }
        gui.setItem(15, titleButton);

        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "Volver");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(22, backButton);

        player.openInventory(gui);
    }
}