package net.moonly.modules.Claims;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ClaimSelectionListener implements Listener {

    private final ClaimManager claimManager;

    public ClaimSelectionListener(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!claimManager.isEnabled()) return; // Module disabled

        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItem(); // Get the item the player is interacting with

        // Check if player is holding the selection tool material
        if (itemInHand != null && itemInHand.getType() == claimManager.getSelectionToolMaterial()) {
            // We are relying only on the material being the configured selection tool.
            // If you need a more specific tool (e.g., only YOUR golden hoe, not any golden hoe),
            // you would need to use PersistentDataContainer or custom NBT tags when giving the tool.

            // Prevent block interaction with the tool (e.g., placing blocks with the hoe)
            // Only cancel if it's a block interaction, not an air interaction
            if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
            }

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (event.getClickedBlock() != null) {
                    claimManager.setPlayerSelectionPoint(player, "point1", event.getClickedBlock().getLocation());
                }
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // Important: Right-click can also place blocks. Ensure it's cancelled.
                if (event.getClickedBlock() != null) {
                    claimManager.setPlayerSelectionPoint(player, "point2", event.getClickedBlock().getLocation());
                }
            }
        }
    }
}