// src/main/java/net/moonly/modules/Claims/gui/ClaimSettingsGUI.java
package net.moonly.modules.Claims.editor;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.moonly.modules.Claims.ClaimManager;
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
import java.util.logging.Level;

public class ClaimSettingsGUI {

    private final ClaimManager claimManager;
    private final String regionId; // The ID of the region being edited
    private final UUID playerUUID; // The UUID of the player who opened the GUI (owner)

    public ClaimSettingsGUI(ClaimManager claimManager, String regionId, UUID playerUUID) {
        this.claimManager = claimManager;
        this.regionId = regionId;
        this.playerUUID = playerUUID;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Claim: " + regionId + " - Settings");

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(player.getWorld()));
        ProtectedRegion region = null;
        if (regionManager != null) {
            region = regionManager.getRegion(regionId);
        }

        if (region == null) {
            player.sendMessage(claimManager.getMessage("region-not-found").replace("{name}", regionId));
            player.closeInventory();
            return;
        }

        // --- Toggle Mining (BLOCK_BREAK) Flag ---
        ItemStack mineFlagItem = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta mineFlagMeta = mineFlagItem.getItemMeta();
        if (mineFlagMeta != null) {
            StateFlag.State currentMineState = region.getFlag(Flags.BLOCK_BREAK);
            String status = (currentMineState == StateFlag.State.ALLOW) ? ChatColor.GREEN + "ALLOW" : ChatColor.RED + "DENY";
            mineFlagMeta.setDisplayName(ChatColor.GOLD + "Toggle Mining (BLOCK_BREAK)");
            mineFlagMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Current: " + status,
                    ChatColor.YELLOW + "Click to toggle."
            ));
            mineFlagItem.setItemMeta(mineFlagMeta);
        }
        gui.setItem(11, mineFlagItem); // Slot for Mining Flag

        // --- Delete Claim Button ---
        ItemStack deleteClaimItem = new ItemStack(Material.LAVA_BUCKET); // Or RED_STAINED_GLASS_PANE
        ItemMeta deleteClaimMeta = deleteClaimItem.getItemMeta();
        if (deleteClaimMeta != null) {
            deleteClaimMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Delete Claim");
            deleteClaimMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Permanently delete this claim."
            ));
            deleteClaimItem.setItemMeta(deleteClaimMeta);
        }
        gui.setItem(15, deleteClaimItem); // Slot for Delete Claim

        // --- Back Button ---
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "Back");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(22, backButton); // Bottom center

        player.openInventory(gui);
    }
}