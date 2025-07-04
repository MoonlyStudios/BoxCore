// src/main/java/net/moonly/modules/Claims/gui/ClaimEditorMainGUI.java
package net.moonly.modules.Claims.editor;

import net.moonly.modules.Claims.ClaimManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ClaimEditorMainGUI {

    private final ClaimManager claimManager;

    public ClaimEditorMainGUI(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Claim Editor - Main Menu");

        // Manage Claims button (List existing claims)
        ItemStack manageClaimsItem = new ItemStack(Material.CHEST);
        ItemMeta manageClaimsMeta = manageClaimsItem.getItemMeta();
        if (manageClaimsMeta != null) {
            manageClaimsMeta.setDisplayName(ChatColor.GOLD + "Manage Your Claims");
            manageClaimsMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "List, edit, and delete claims",
                    ChatColor.GRAY + "you have created."
            ));
            manageClaimsItem.setItemMeta(manageClaimsMeta);
        }
        gui.setItem(11, manageClaimsItem);

        // Claim Creation Tools button (give tool, create claim from selection)
        ItemStack createClaimItem = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta createClaimMeta = createClaimItem.getItemMeta();
        if (createClaimMeta != null) {
            createClaimMeta.setDisplayName(ChatColor.GOLD + "Claim Creation Tools");
            createClaimMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Get the selection tool,",
                    ChatColor.GRAY + "clear selection, or create a claim."
            ));
            createClaimItem.setItemMeta(createClaimMeta);
        }
        gui.setItem(15, createClaimItem);

        player.openInventory(gui);
    }
}