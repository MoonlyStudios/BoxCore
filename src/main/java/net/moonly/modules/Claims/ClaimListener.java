package net.moonly.modules.Claims;

import net.moonly.modules.Claims.editor.ClaimEditorMainGUI;
import net.moonly.modules.Claims.editor.ClaimListGUI;
import net.moonly.modules.Claims.editor.ClaimSettingsGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class ClaimListener implements Listener {

    private final ClaimManager claimManager;

    public static final Map<UUID, String> playersEditingClaimSettings = new HashMap<>(); // Player UUID -> Region ID
    public static final Map<UUID, String> playersPendingDeletion = new HashMap<>(); // Player UUID -> Region ID

    public ClaimListener(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!claimManager.isEnabled()) return; // Module disabled

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        String title = event.getView().getTitle();

        // --- Main Editor Menu (/claim editor) ---
        if (title.equals(ChatColor.DARK_AQUA + "Claim Editor - Main Menu")) {
            event.setCancelled(true); // Cancel clicks ONLY within this specific GUI
            if (!player.hasPermission("boxcore.claim.editor")) {
                player.sendMessage(claimManager.getMessage("no-permission-command"));
                player.closeInventory();
                return;
            }

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = ChatColor.stripColor(meta.getDisplayName());
                if (displayName.equals("Manage Your Claims")) {
                    new ClaimListGUI(claimManager, player).open();
                } else if (displayName.equals("Claim Creation Tools")) {
                    player.closeInventory();
                    claimManager.giveSelectionTool(player);
                    player.sendMessage(claimManager.getMessage("selection-tool-given").replace("{tool}", claimManager.getSelectionToolMaterial().name()));
                    player.sendMessage(ChatColor.YELLOW + "Use Left-click to set Point 1, Right-click to set Point 2.");
                    player.sendMessage(ChatColor.YELLOW + "Then use /claim create <name> to create the claim.");
                    if(claimManager.isAllowClearSelection()) {
                        player.sendMessage(ChatColor.YELLOW + "Use /claim clear to reset your selection.");
                    }
                }
            }
            return; // Return after handling this GUI to prevent further processing
        }

        // --- Claim List GUI (Manage Claims) ---
        if (title.equals(ChatColor.DARK_AQUA + "Your Claims")) {
            event.setCancelled(true); // Cancel clicks ONLY within this specific GUI
            if (!player.hasPermission("boxcore.claim.editor")) { // Use editor perm for list access
                player.sendMessage(claimManager.getMessage("no-permission-command"));
                player.closeInventory();
                return;
            }

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = ChatColor.stripColor(meta.getDisplayName());

                if (displayName.equals("Back")) { // Back button
                    player.closeInventory();
                    new ClaimEditorMainGUI(claimManager).open(player);
                    return;
                }

                // If it's a claim item
                String regionId = displayName; // The claim name is the display name

                if (event.isLeftClick()) { // Left-click to open settings for this claim
                    playersEditingClaimSettings.put(player.getUniqueId(), regionId); // Store selected region
                    new ClaimSettingsGUI(claimManager, regionId, player.getUniqueId()).open(player);
                } else if (event.isRightClick()) { // Right-click to delete claim
                    if (player.hasPermission("boxcore.claim.delete")) {
                        player.closeInventory(); // Close GUI for confirmation
                        player.sendMessage(ChatColor.YELLOW + "Are you sure you want to delete claim '" + regionId + "'? Type 'confirm' in chat to proceed, or 'cancel' to abort.");
                        playersPendingDeletion.put(player.getUniqueId(), regionId); // Store pending deletion
                        playersEditingClaimSettings.remove(player.getUniqueId()); // Clear editing state
                        claimManager.getPlugin().getLogger().log(Level.INFO, "DEBUG: Player " + player.getName() + " initiated delete for claim '" + regionId + "'. Awaiting confirmation.");
                    } else {
                        player.sendMessage(claimManager.getMessage("no-permission-command"));
                    }
                }
            }
            return; // Return after handling this GUI
        }

        // --- Claim Settings GUI ---
        if (title.startsWith(ChatColor.DARK_AQUA + "Claim: ")) {
            event.setCancelled(true); // Cancel clicks ONLY within this specific GUI
            if (!player.hasPermission("boxcore.claim.editor")) { // Use editor perm for settings access
                player.sendMessage(claimManager.getMessage("no-permission-command"));
                player.closeInventory();
                return;
            }

            String regionId = ChatColor.stripColor(title.replace(ChatColor.DARK_AQUA + "Claim: ", "").replace(" - Settings", ""));

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = ChatColor.stripColor(meta.getDisplayName());

                if (displayName.equals("Toggle Mining (BLOCK_BREAK)")) {
                    if (player.hasPermission("boxcore.claim.setflag")) {
                        claimManager.toggleRegionFlag(player, regionId, "mine");
                        // Refresh GUI after toggle
                        Bukkit.getScheduler().runTaskLater(claimManager.getPlugin(), () -> {
                            if (player.isOnline()) {
                                new ClaimSettingsGUI(claimManager, regionId, player.getUniqueId()).open(player);
                            }
                        }, 1L);
                    } else {
                        player.sendMessage(claimManager.getMessage("no-permission-command"));
                    }
                } else if (displayName.equals("Delete Claim")) {
                    if (player.hasPermission("boxcore.claim.delete")) {
                        player.closeInventory();
                        player.sendMessage(ChatColor.YELLOW + "Are you sure you want to delete claim '" + regionId + "'? Type 'confirm' in chat to proceed, or 'cancel' to abort.");
                        playersPendingDeletion.put(player.getUniqueId(), regionId); // Store pending deletion
                        playersEditingClaimSettings.remove(player.getUniqueId()); // Clear editing state
                        claimManager.getPlugin().getLogger().log(Level.INFO, "DEBUG: Player " + player.getName() + " initiated delete for claim '" + regionId + "'. Awaiting confirmation.");
                    } else {
                        player.sendMessage(claimManager.getMessage("no-permission-command"));
                    }
                } else if (displayName.equals("Back")) {
                    player.closeInventory();
                    playersEditingClaimSettings.remove(player.getUniqueId()); // Ensure editing state is cleared when going back
                    new ClaimListGUI(claimManager, player).open(); // Go back to claim list
                }
            }
            return; // Return after handling this GUI
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Check for pending deletion only
        if (playersPendingDeletion.containsKey(player.getUniqueId())) {
            String regionId = playersPendingDeletion.get(player.getUniqueId()); // Get the pending region
            if (regionId != null && event.getMessage().equalsIgnoreCase("confirm")) {
                event.setCancelled(true); // Consume the message
                // FIX: Remove player from pending deletion IMMEDIATELY to prevent double processing
                playersPendingDeletion.remove(player.getUniqueId());

                // Run deletion on main thread
                Bukkit.getScheduler().runTask(claimManager.getPlugin(), () -> {
                    if (claimManager.deleteRegion(player, regionId)) {
                        player.sendMessage(claimManager.getMessage("claim-deleted-success").replace("{name}", regionId));
                        // Go back to the claim list after successful deletion
                        Bukkit.getScheduler().runTaskLater(claimManager.getPlugin(), () -> {
                            if (player.isOnline()) {
                                new ClaimListGUI(claimManager, player).open();
                            }
                        }, 1L);
                    } else {
                        player.sendMessage(claimManager.getMessage("claim-deleted-error").replace("{name}", regionId));
                        // If deletion failed, reopen settings GUI
                        Bukkit.getScheduler().runTaskLater(claimManager.getPlugin(), () -> {
                            if (player.isOnline()) {
                                new ClaimSettingsGUI(claimManager, regionId, player.getUniqueId()).open(player);
                            }
                        }, 1L);
                    }
                    // OLD: playersPendingDeletion.remove(player.getUniqueId()); // This line was moved up
                });
            } else if (regionId != null && event.getMessage().equalsIgnoreCase("cancel")) {
                event.setCancelled(true); // Consume the message
                player.sendMessage(ChatColor.YELLOW + "Claim deletion cancelled.");
                playersPendingDeletion.remove(player.getUniqueId()); // Keep this here
                // Reopen the settings GUI
                Bukkit.getScheduler().runTaskLater(claimManager.getPlugin(), () -> {
                    if (player.isOnline()) {
                        new ClaimSettingsGUI(claimManager, regionId, player.getUniqueId()).open(player);
                    }
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        // Check if the player currently has one of your GUIs open
        String title = event.getPlayer().getOpenInventory().getTitle();

        if (title.equals(ChatColor.DARK_AQUA + "Claim Editor - Main Menu") ||
                title.equals(ChatColor.DARK_AQUA + "Your Claims") ||
                title.startsWith(ChatColor.DARK_AQUA + "Claim: ")) {

            // If the player is in GM 1 (creative)
            if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                // Cancel the event to prevent the item from being dropped from the inventory.
                // In creative, the item isn't removed from the inventory anyway,
                // so cancelling this prevents the "duplicate" from appearing on the ground.
                event.setCancelled(true);
            }
        }
    }
}