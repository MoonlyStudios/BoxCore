package net.moonly.modules.TemporaryBlocks;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class TemporaryBlockListener implements Listener {

    private final TemporaryBlockManager manager;

    public TemporaryBlockListener(TemporaryBlockManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!manager.isEnabled()) return; // Module disabled

        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("core.temporaryblocks.bypass")) {
            return;
        }

        Material placedMaterial = event.getBlockPlaced().getType();

        if (manager.isApplyToAllBlocks()) {
            manager.addTemporaryBlock(event.getBlockPlaced().getLocation(), placedMaterial, player.getUniqueId());
        } else if (manager.getSpecificBlocks().contains(placedMaterial)) {
            manager.addTemporaryBlock(event.getBlockPlaced().getLocation(), placedMaterial, player.getUniqueId());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!manager.isEnabled()) return; // Module disabled

        if (manager.getTemporaryBlock(event.getBlock().getLocation()) != null) {
            manager.removeTemporaryBlock(event.getBlock().getLocation());
        }
    }
}