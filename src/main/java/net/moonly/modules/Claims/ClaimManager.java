package net.moonly.modules.Claims;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import net.moonly.Box;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag; // Import StateFlag
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldedit.math.BlockVector3;


import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ClaimManager {

    private final Box plugin;
    private FileConfiguration config;
    private File configFile;

    private boolean enabled;
    private Material selectionToolMaterial;
    private boolean allowClearSelection; // NEW: Boolean for allow-clear-selection setting

    public static final Map<UUID, Map<String, Location>> playerSelections = new HashMap<>();


    public ClaimManager(Box plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public Box getPlugin() {
        return plugin;
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "modules/claims/claims.yml");
        if (!configFile.exists()) {
            plugin.saveResource("modules/claims/claims.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        this.enabled = config.getBoolean("settings.enabled", true);
        this.selectionToolMaterial = Material.valueOf(config.getString("settings.selection-tool", "GOLDEN_HOE").toUpperCase());
        this.allowClearSelection = config.getBoolean("settings.allow-clear-selection", true); // NEW: Load the setting

        plugin.getLogger().info("Loaded claims.yml configuration. Enabled: " + enabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    // NEW: Getter for allowClearSelection
    public boolean isAllowClearSelection() {
        return allowClearSelection;
    }

    public String getMessage(String path) {
        String message = config.getString("messages." + path, "&cMessage not found for path: " + path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Sends a title and subtitle to the player based on message paths.
     * @param player The player to send the title to.
     * @param titlePath The path to the title message in claims.yml.
     * @param subtitlePath The path to the subtitle message in claims.yml.
     * @param fadeIn The fade-in time in ticks.
     * @param stay The stay time in ticks.
     * @param fadeOut The fade-out time in ticks.
     * @param placeholders Optional map of placeholders to replace in title/subtitle messages (e.g., "{name}" -> "region1").
     */
    private void sendTitle(Player player, String titlePath, String subtitlePath, int fadeIn, int stay, int fadeOut, Map<String, String> placeholders) {
        String title = getMessage(titlePath);
        String subtitle = getMessage(subtitlePath);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                title = title.replace(entry.getKey(), entry.getValue());
                subtitle = subtitle.replace(entry.getKey(), entry.getValue());
            }
        }
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Gives the player the selection tool.
     * @param player The player to give the tool to.
     */
    public void giveSelectionTool(Player player) {
        ItemStack tool = new ItemStack(selectionToolMaterial);
        ItemMeta meta = tool.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Claim Selection Tool");
            meta.setLore(java.util.Arrays.asList(
                    ChatColor.GRAY + "Left-click: Set point 1",
                    ChatColor.GRAY + "Right-click: Set point 2"
            ));
            tool.setItemMeta(meta);
        }
        player.getInventory().addItem(tool);
        player.sendMessage(getMessage("selection-tool-given").replace("{tool}", selectionToolMaterial.name()));
    }

    /**
     * Stores a selection point for a player.
     * @param player The player.
     * @param pointType "point1" or "point2".
     * @param location The location to store.
     */
    public void setPlayerSelectionPoint(Player player, String pointType, Location location) {
        playerSelections.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(pointType, location);
        player.sendMessage(getMessage("selection-point-set").replace("{point}", pointType).replace("{location}", formatLocation(location)));

        if (pointType.equalsIgnoreCase("point1")) {
            sendTitle(player, "selection-point-1-title", "selection-point-1-subtitle", 10, 70, 20, null);
        } else if (pointType.equalsIgnoreCase("point2")) {
            sendTitle(player, "selection-point-2-title", "selection-point-2-subtitle", 10, 70, 20, null);
        }
    }

    /**
     * Checks if a player has a complete selection (both points set).
     * @param player The player.
     * @return True if both points are set, false otherwise.
     */
    public boolean hasCompleteSelection(Player player) {
        Map<String, Location> selection = playerSelections.get(player.getUniqueId());
        return selection != null && selection.containsKey("point1") && selection.containsKey("point2");
    }

    /**
     * Gets the stored selection points for a player.
     * @param player The player.
     * @return A Map containing "point1" and "point2" Locations, or null if incomplete.
     */
    public Map<String, Location> getPlayerSelection(Player player) {
        if (hasCompleteSelection(player)) {
            return playerSelections.get(player.getUniqueId());
        }
        return null;
    }

    /**
     * Clears a player's selection.
     * @param player The player.
     * @return True if selection was cleared, false if not allowed or no selection.
     */
    public boolean clearPlayerSelection(Player player) { // Changed return type to boolean
        if (!allowClearSelection) { // NEW: Check if clearing is allowed
            player.sendMessage(getMessage("selection-clear-disabled")); // NEW: Message for disabled
            return false;
        }
        if (!playerSelections.containsKey(player.getUniqueId())) {
            player.sendMessage(getMessage("no-selection-to-clear")); // NEW: Message if no selection exists
            return false;
        }

        playerSelections.remove(player.getUniqueId());
        player.sendMessage(getMessage("selection-cleared"));
        sendTitle(player, "selection-cleared-title", "selection-cleared-subtitle", 10, 70, 20, null);
        return true; // Indicate success
    }

    /**
     * Creates a new WorldGuard region based on the player's stored selection.
     *
     * @param player The player creating the claim.
     * @param regionName The name of the new region.
     * @return True if the region was created successfully, false otherwise.
     */
    public boolean createClaim(Player player, String regionName) {
        plugin.getLogger().info("DEBUG: Attempting to create claim '" + regionName + "' for player " + player.getName());

        Plugin worldGuardPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (!(worldGuardPlugin instanceof com.sk89q.worldguard.bukkit.WorldGuardPlugin)) {
            player.sendMessage(getMessage("worldguard-not-found"));
            plugin.getLogger().log(Level.WARNING, "DEBUG: WorldGuard plugin not found or not enabled. Aborting claim creation.");
            return false;
        }

        Map<String, Location> selectionPoints = getPlayerSelection(player);
        if (selectionPoints == null) {
            player.sendMessage(getMessage("selection-incomplete"));
            plugin.getLogger().log(Level.INFO, "DEBUG: Player " + player.getName() + " has no complete selection. Aborting claim creation.");
            return false;
        }

        Location point1 = selectionPoints.get("point1");
        Location point2 = selectionPoints.get("point2");

        if (!point1.getWorld().equals(point2.getWorld())) {
            player.sendMessage(getMessage("selection-different-worlds"));
            plugin.getLogger().log(Level.INFO, "DEBUG: Player " + player.getName() + " selection points are in different worlds. Aborting.");
            return false;
        }

        World bukkitWorld = point1.getWorld();
        plugin.getLogger().info("DEBUG: Claim world: " + bukkitWorld.getName());

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(bukkitWorld));

        if (regionManager == null) {
            player.sendMessage(getMessage("region-manager-error"));
            plugin.getLogger().log(Level.SEVERE, "DEBUG: RegionManager is null for world " + bukkitWorld.getName() + ". Aborting claim creation.");
            return false;
        }
        plugin.getLogger().info("DEBUG: RegionManager obtained for world " + bukkitWorld.getName());

        if (regionManager.getRegion(regionName) != null) {
            player.sendMessage(getMessage("claim-name-exists").replace("{name}", regionName));
            plugin.getLogger().log(Level.INFO, "DEBUG: Claim name '" + regionName + "' already exists. Aborting.");
            return false;
        }
        plugin.getLogger().info("DEBUG: Claim name '" + regionName + "' is unique.");

        ProtectedRegion region = new ProtectedCuboidRegion(
                regionName,
                BlockVector3.at(point1.getX(), point1.getY(), point1.getZ()),
                BlockVector3.at(point2.getX(), point2.getY(), point2.getZ())
        );
        plugin.getLogger().info("DEBUG: ProtectedRegion object created from points.");

        region.getOwners().addPlayer(player.getUniqueId());
        plugin.getLogger().info("DEBUG: Player " + player.getName() + " set as owner.");

        regionManager.addRegion(region);
        plugin.getLogger().info("DEBUG: Region added to RegionManager.");

        try {
            regionManager.save();
            player.sendMessage(getMessage("claim-created-success").replace("{name}", regionName));

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{name}", regionName);
            sendTitle(player, "claim-created-title", "claim-created-subtitle", 10, 70, 20, placeholders);

            plugin.getLogger().info("DEBUG: Claim '" + regionName + "' created and saved successfully by " + player.getName() + " in world " + bukkitWorld.getName());
            clearPlayerSelection(player); // Call the updated clearPlayerSelection
            return true;
        } catch (StorageException e) {
            plugin.getLogger().log(Level.SEVERE, "DEBUG: Error saving WorldGuard regions after creating '" + regionName + "': " + e.getMessage());
            player.sendMessage(getMessage("claim-created-error").replace("{name}", regionName));
            throw new RuntimeException(e);
        } finally {
            plugin.getLogger().info("DEBUG: Claim creation process for '" + regionName + "' finished.");
        }
    }

    /**
     * Toggles a specific WorldGuard flag for a given region based on flagType.
     *
     * @param player The player executing the command (for messages).
     * @param regionName The name of the new region.
     * @param flagType The type of flag to toggle (e.g., "mine" for BLOCK_BREAK).
     * @return True if the flag was successfully toggled and saved, false otherwise.
     */
    public boolean toggleRegionFlag(Player player, String regionName, String flagType) {
        plugin.getLogger().info("DEBUG: Attempting to toggle flag '" + flagType + "' for region '" + regionName + "' by " + player.getName());

        World bukkitWorld = player.getWorld();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(bukkitWorld));

        if (regionManager == null) {
            player.sendMessage(getMessage("region-manager-error"));
            plugin.getLogger().log(Level.SEVERE, "DEBUG: RegionManager is null for world " + bukkitWorld.getName() + ". Cannot toggle flag.");
            return false;
        }

        ProtectedRegion region = regionManager.getRegion(regionName);
        if (region == null) {
            player.sendMessage(getMessage("region-not-found").replace("{name}", regionName));
            plugin.getLogger().log(Level.INFO, "DEBUG: Region '" + regionName + "' not found for toggling flag.");
            return false;
        }

        StateFlag.State newState;
        String messagePath;
        String flagDisplayName = flagType.toUpperCase();

        switch (flagType.toLowerCase()) {
            case "mine":
                StateFlag.State currentBlockBreakState = region.getFlag(Flags.BLOCK_BREAK);
                if (currentBlockBreakState == StateFlag.State.ALLOW) {
                    newState = StateFlag.State.DENY;
                    messagePath = "mine-region-deactivated";
                    plugin.getLogger().info("DEBUG: Setting BLOCK_BREAK to DENY for region '" + regionName + "'.");
                } else {
                    newState = StateFlag.State.ALLOW;
                    messagePath = "mine-region-activated";
                    plugin.getLogger().info("DEBUG: Setting BLOCK_BREAK to ALLOW for region '" + regionName + "'.");
                }
                region.setFlag(Flags.BLOCK_BREAK, newState);
                flagDisplayName = "BLOCK_BREAK";
                break;
            default:
                player.sendMessage(getMessage("flag-type-invalid"));
                plugin.getLogger().log(Level.INFO, "DEBUG: Invalid flag type '" + flagType + "' for region '" + regionName + "'.");
                return false;
        }

        try {
            regionManager.save();
            player.sendMessage(getMessage("flag-set-success")
                    .replace("{flag_type}", flagDisplayName)
                    .replace("{name}", regionName)
                    .replace("{state}", newState.name()));

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{flag_type}", flagDisplayName);
            placeholders.put("{name}", regionName);
            placeholders.put("{state}", newState.name());
            sendTitle(player, "flag-set-title", "flag-set-subtitle", 10, 70, 20, placeholders);

            plugin.getLogger().info("DEBUG: WorldGuard regions saved after flag '" + flagDisplayName + "' change for '" + regionName + "'.");
            return true;
        } catch (StorageException e) {
            player.sendMessage(getMessage("flag-set-error").replace("{flag_type}", flagDisplayName).replace("{name}", regionName));
            plugin.getLogger().log(Level.SEVERE, "DEBUG: Error saving WorldGuard regions after flag '" + flagDisplayName + "' change for '" + regionName + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a WorldGuard region.
     * @param player The player requesting deletion.
     * @param regionName The name of the region to delete.
     * @return True if deleted successfully, false otherwise.
     */
    public boolean deleteRegion(Player player, String regionName) {
        plugin.getLogger().info("DEBUG: Attempting to delete region '" + regionName + "' for player " + player.getName()); //

        World bukkitWorld = player.getWorld(); //
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer(); //
        RegionManager regionManager = container.get(BukkitAdapter.adapt(bukkitWorld)); //

        if (regionManager == null) {
            player.sendMessage(getMessage("region-manager-error")); // // Keep this specific error, as it's a core failure before actual deletion logic
            plugin.getLogger().log(Level.SEVERE, "DEBUG: RegionManager is null for world " + bukkitWorld.getName() + ". Cannot delete region."); //
            return false; //
        }

        ProtectedRegion region = regionManager.getRegion(regionName); //
        if (region == null) {
            player.sendMessage(getMessage("region-not-found").replace("{name}", regionName)); // // Keep this specific error
            plugin.getLogger().log(Level.INFO, "DEBUG: Region '" + regionName + "' not found for deletion."); //
            return false; //
        }

        if (!region.isOwner(String.valueOf(player.getUniqueId())) && !player.hasPermission("boxcore.claim.delete.others")) { //
            player.sendMessage(getMessage("no-permission-delete-others")); // // Keep this specific error
            plugin.getLogger().log(Level.INFO, "DEBUG: Player " + player.getName() + " tried to delete region '" + regionName + "' without permission."); //
            return false; //
        }

        regionManager.removeRegion(regionName); //

        try {
            regionManager.save(); //
            // REMOVE THESE LINES:
            // player.sendMessage(getMessage("claim-deleted-success").replace("{name}", regionName));
            // Map<String, String> placeholders = new HashMap<>();
            // placeholders.put("{name}", regionName);
            // sendTitle(player, "claim-deleted-title", "claim-deleted-subtitle", 10, 70, 20, placeholders);

            plugin.getLogger().info("DEBUG: Region '" + regionName + "' deleted successfully by " + player.getName() + "."); //
            return true; //
        } catch (StorageException e) {
            // REMOVE THIS LINE:
            // player.sendMessage(getMessage("claim-deleted-error").replace("{name}", regionName));
            plugin.getLogger().log(Level.SEVERE, "DEBUG: Error saving WorldGuard regions after deleting '" + regionName + "': " + e.getMessage()); //
            e.printStackTrace(); //
            return false; //
        }
    }

    /**
     * Lists all regions in the player's current world with their flags.
     * @param player The player requesting the list.
     */
    public void listRegions(Player player) {
        plugin.getLogger().info("DEBUG: Player " + player.getName() + " requested region list for world " + player.getWorld().getName());

        World bukkitWorld = player.getWorld();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(bukkitWorld));

        if (regionManager == null) {
            player.sendMessage(getMessage("region-manager-error"));
            plugin.getLogger().log(Level.SEVERE, "DEBUG: RegionManager is null for world " + bukkitWorld.getName() + ". Cannot list regions.");
            return;
        }

        Map<String, ProtectedRegion> regions = regionManager.getRegions();

        if (regions.isEmpty()) {
            player.sendMessage(getMessage("no-regions-found"));
            return;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6--- Regions in " + bukkitWorld.getName() + " ---"));
        regions.values().stream()
                .sorted(Comparator.comparing(ProtectedRegion::getId))
                .forEach(region -> {
                    String ownerStatus = "";
                    if (region.getOwners().contains(player.getUniqueId())) {
                        ownerStatus = ChatColor.GREEN + " (Owner)";
                    } else if (region.getMembers().contains(player.getUniqueId())) {
                        ownerStatus = ChatColor.YELLOW + " (Member)";
                    }

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "§b- " + region.getId() + ownerStatus + " §7[" +
                                    "§f" + region.getMinimumPoint().getBlockX() + ", " + region.getMinimumPoint().getBlockY() + ", " + region.getMinimumPoint().getBlockZ() +
                                    " -> " +
                                    "§f" + region.getMaximumPoint().getBlockX() + ", " + region.getMaximumPoint().getBlockY() + ", " + region.getMaximumPoint().getBlockZ() +
                                    "§7]"
                    ));

                    if (!region.getFlags().isEmpty()) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  §8Flags: §7" +
                                region.getFlags().entrySet().stream()
                                        .map(entry -> {
                                            Flag<?> flag = entry.getKey();
                                            Object value = entry.getValue();
                                            String flagValue = "";
                                            if (flag instanceof StateFlag) {
                                                flagValue = ((StateFlag.State) value).name();
                                            } else {
                                                flagValue = value.toString();
                                            }
                                            return ChatColor.LIGHT_PURPLE + flag.getName() + ": " + ChatColor.AQUA + flagValue;
                                        })
                                        .collect(Collectors.joining(ChatColor.GRAY + ", "))
                        ));
                    }
                });
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6--------------------"));
    }


    // Helper to format location for messages
    private String formatLocation(Location loc) {
        return loc.getWorld().getName() + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    public Material getSelectionToolMaterial() {
        return selectionToolMaterial;
    }
}