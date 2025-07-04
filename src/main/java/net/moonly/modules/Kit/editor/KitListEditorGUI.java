package net.moonly.modules.Kit.editor;


import net.moonly.modules.Kit.Kit;
import net.moonly.modules.Kit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class KitListEditorGUI {

    private final KitManager kitManager;

    public KitListEditorGUI(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    public void open(Player player) {
        int size = ((kitManager.getKits().size() / 9) + 1) * 9;
        if (size == 0) size = 9;
        if (size > 54) size = 54;

        Inventory gui = Bukkit.createInventory(null, size, ChatColor.DARK_AQUA + "Kit Editor - Kits");

        ItemStack[] guiItems = new ItemStack[size];

        for (Kit kit : kitManager.getKits().values()) {
            ItemStack kitIcon = kit.getGuiIcon();
            if (kitIcon == null) {
                kitIcon = new ItemStack(Material.CHEST);
            }

            ItemMeta meta = kitIcon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + kit.getName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Slot: " + (kit.getGuiSlot() == -1 ? "Auto" : kit.getGuiSlot()));
                lore.add(ChatColor.GRAY + "Cooldown: " + TimeUnit.MILLISECONDS.toSeconds(kit.getCooldown()) + "s");
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click izquierdo para editar");
                lore.add(ChatColor.RED + "Click derecho para eliminar");
                meta.setLore(lore);
                kitIcon.setItemMeta(meta);
            }

            if (kit.getGuiSlot() != -1 && kit.getGuiSlot() < size && guiItems[kit.getGuiSlot()] == null) {
                guiItems[kit.getGuiSlot()] = kitIcon;
            } else {
                for (int i = 0; i < size; i++) {
                    if (guiItems[i] == null) {
                        guiItems[i] = kitIcon;
                        if (kit.getGuiSlot() == -1 || kit.getGuiSlot() >= size) {
                            kit.setGuiSlot(i);
                            kitManager.saveKit(kit);
                        }
                        break;
                    }
                }
            }
        }
        gui.setContents(guiItems);
        player.openInventory(gui);
    }
}