// src/main/java/net/moonly/modules/Claims/gui/ClaimListGUI.java
package net.moonly.modules.Claims.editor;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ClaimListGUI {

    private final ClaimManager claimManager;
    private final Player player;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public ClaimListGUI(ClaimManager claimManager, Player player) {
        this.claimManager = claimManager;
        this.player = player;
    }

    public void open() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(player.getWorld()));

        List<ProtectedRegion> playerClaims = new ArrayList<>();
        if (regionManager != null) {
            // Filter regions: only show regions owned by the player
            playerClaims = regionManager.getRegions().values().stream()
                    .filter(region -> region.getOwners().contains(player.getUniqueId()))
                    .sorted(Comparator.comparing(ProtectedRegion::getId)) // Sort by name
                    .collect(Collectors.toList());
        }

        int numClaims = playerClaims.size();
        int rows = (int) Math.ceil((double) numClaims / 9.0);
        if (rows == 0) rows = 1; // At least one row, even if no claims
        if (rows > 6) rows = 6; // Max 6 rows (54 slots)

        Inventory gui = Bukkit.createInventory(null, rows * 9, ChatColor.DARK_AQUA + "Your Claims");

        for (ProtectedRegion claim : playerClaims) {
            ItemStack claimIcon = new ItemStack(Material.GRASS_BLOCK); // Default icon for a claim
            ItemMeta meta = claimIcon.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + claim.getId());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "World: " + ChatColor.AQUA + player.getWorld().getName());
                lore.add(ChatColor.GRAY + "Min: " + ChatColor.AQUA + claim.getMinimumPoint().getBlockX() + ", " + claim.getMinimumPoint().getBlockY() + ", " + claim.getMinimumPoint().getBlockZ());
                lore.add(ChatColor.GRAY + "Max: " + ChatColor.AQUA + claim.getMaximumPoint().getBlockX() + ", " + claim.getMaximumPoint().getBlockY() + ", " + claim.getMaximumPoint().getBlockZ());
                lore.add("");
                lore.add(ChatColor.YELLOW + "Left-click to manage flags");
                lore.add(ChatColor.RED + "Right-click to delete");
                meta.setLore(lore);
                claimIcon.setItemMeta(meta);
            }
            gui.addItem(claimIcon);
        }

        // Add a "Back" button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "Back");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(gui.getSize() - 9, backButton); // Place in bottom left corner

        player.openInventory(gui);
    }
}