package net.moonly.commands.staff;

import net.moonly.Box;


import net.moonly.modules.Kit.editor.KitEditorMainGUI;
import net.moonly.modules.Kit.editor.KitSettingsEditorGUI;
import net.moonly.modules.Kit.Kit;
import net.moonly.modules.Kit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class KitCommand implements CommandExecutor {

    private final Box plugin;
    private final KitManager kitManager;

    public static final HashMap<UUID, String> playerEditingKitSlot = new HashMap<>();


    public KitCommand(Box plugin, KitManager kitManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if Kits module is enabled
        if (plugin.getKitManager() == null) {
            sender.sendMessage(ChatColor.RED + "The Kits module is disabled.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;

        // Reset editing state if a command is run outside of GUI flow
        KitSettingsEditorGUI.playersEditingKitContents.remove(player.getUniqueId());
        playerEditingKitSlot.remove(player.getUniqueId());


        if (args.length == 0) {
            openKitGUI(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "editor":
                if (!player.hasPermission("core.kit.editor")) {
                    player.sendMessage(kitManager.getMessage("no-permission-command"));
                    return true;
                }
                new KitEditorMainGUI(kitManager).open(player);
                return true;

            case "create":
                if (!player.hasPermission("core.kit.create")) {
                    player.sendMessage(kitManager.getMessage("no-permission-command"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Correct usage: /kit create <name> <cooldown_seconds>");
                    return true;
                }
                String kitName = args[1];
                long cooldownSeconds;
                try {
                    cooldownSeconds = Long.parseLong(args[2]);
                    if (cooldownSeconds < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Cooldown must be a positive integer in seconds.");
                    return true;
                }

                ItemStack[] contents = player.getInventory().getContents();
                ItemStack[] filteredContents = Arrays.stream(contents)
                        .filter(item -> item != null && item.getType() != Material.AIR)
                        .toArray(ItemStack[]::new);

                if (kitManager.createKit(kitName, TimeUnit.SECONDS.toMillis(cooldownSeconds), filteredContents)) {
                    player.sendMessage(kitManager.getMessage("kit-saved").replace("{kit_name}", kitName));
                } else {
                    player.sendMessage(ChatColor.RED + "A kit with the name '" + kitName + "' already exists.");
                }
                return true;

            case "delete":
                if (!player.hasPermission("core.kit.delete")) {
                    player.sendMessage(kitManager.getMessage("no-permission-command"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Correct usage: /kit delete <name>");
                    return true;
                }
                String kitToDelete = args[1];
                if (kitManager.deleteKit(kitToDelete)) {
                    player.sendMessage(kitManager.getMessage("kit-deleted").replace("{kit_name}", kitToDelete));
                } else {
                    player.sendMessage(kitManager.getMessage("kit-does-not-exist").replace("{kit_name}", kitToDelete));
                }
                return true;

            case "list":
                if (!player.hasPermission("core.kit.list")) {
                    player.sendMessage(kitManager.getMessage("no-permission-command"));
                    return true;
                }
                if (kitManager.getKits().isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "No kits available.");
                    return true;
                }
                player.sendMessage(ChatColor.GOLD + "--- Available Kits ---");
                kitManager.getKits().values().forEach(kit -> {
                    long cooldownDisplaySeconds = TimeUnit.MILLISECONDS.toSeconds(kit.getCooldown());
                    player.sendMessage(ChatColor.AQUA + "- " + kit.getName() + ChatColor.GRAY + " (Cooldown: " + cooldownDisplaySeconds + "s)" +
                            ChatColor.DARK_GRAY + " [Slot: " + (kit.getGuiSlot() == -1 ? "Auto" : kit.getGuiSlot()) + "]");
                });
                return true;

            case "claim":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Correct usage: /kit claim <name>");
                    player.sendMessage(ChatColor.RED + "You can also use /kit to open the GUI.");
                    return true;
                }
                String kitToClaim = args[1];
                Kit kit = kitManager.getKit(kitToClaim);
                if (kit == null) {
                    player.sendMessage(kitManager.getMessage("kit-does-not-exist").replace("{kit_name}", kitToClaim));
                    return true;
                }
                claimKit(player, kit);
                return true;

            case "give":
                if (!player.hasPermission("core.kit.give")) {
                    player.sendMessage(kitManager.getMessage("no-permission-command"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Correct usage: /kit give <player> <name>");
                    return true;
                }
                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    player.sendMessage(kitManager.getMessage("player-not-found").replace("{player_name}", args[1]));
                    return true;
                }
                String kitToGive = args[2];
                Kit kitToGiveObj = kitManager.getKit(kitToGive);
                if (kitToGiveObj == null) {
                    player.sendMessage(kitManager.getMessage("kit-does-not-exist").replace("{kit_name}", kitToGive));
                    return true;
                }

                giveKitContents(targetPlayer, kitToGiveObj);
                player.sendMessage(kitManager.getMessage("kit-given")
                        .replace("{kit_name}", kitToGive)
                        .replace("{player_name}", targetPlayer.getName()));
                targetPlayer.sendMessage(kitManager.getMessage("kit-received").replace("{kit_name}", kitToGive));
                return true;

            case "rename":
                if (!player.hasPermission("core.kit.edit")) {
                    player.sendMessage(kitManager.getMessage("no-permission-command"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /kit rename <current_name> <new_name>");
                    return true;
                }
                String oldName = args[1];
                String newName = args[2];
                Kit kitToRename = kitManager.getKit(oldName);
                if (kitToRename == null) {
                    player.sendMessage(kitManager.getMessage("kit-does-not-exist").replace("{kit_name}", oldName));
                    return true;
                }
                if (kitManager.getKit(newName) != null) {
                    player.sendMessage(ChatColor.RED + "A kit with the name '" + newName + "' already exists.");
                    return true;
                }
                ItemStack[] oldContents = kitToRename.getContents();
                long oldCooldown = kitToRename.getCooldown();
                int oldSlot = kitToRename.getGuiSlot();
                ItemStack oldIcon = kitToRename.getGuiIcon();
                Map<UUID, Long> oldLastClaimed = kitToRename.getLastClaimedMap();

                kitManager.deleteKit(oldName);
                Kit renamedKit = new Kit(newName, oldCooldown, oldContents, oldSlot, oldIcon);
                renamedKit.setLastClaimedMap(oldLastClaimed);

                kitManager.getKits().put(newName.toLowerCase(), renamedKit);
                kitManager.saveKit(renamedKit);
                player.sendMessage(ChatColor.GREEN + "Kit '" + oldName + "' renamed to '" + newName + "' successfully.");
                return true;

            case "cooldown":
                if (!player.hasPermission("core.kit.edit")) {
                    player.sendMessage(kitManager.getMessage("no-permission-command"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /kit cooldown <kit_name> <seconds>");
                    return true;
                }
                String cooldownKitName = args[1];
                long newCooldownSeconds;
                try {
                    newCooldownSeconds = Long.parseLong(args[2]);
                    if (newCooldownSeconds < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Cooldown must be a positive integer in seconds.");
                    return true;
                }
                Kit targetKit = kitManager.getKit(cooldownKitName);
                if (targetKit == null) {
                    player.sendMessage(kitManager.getMessage("kit-does-not-exist").replace("{kit_name}", cooldownKitName));
                    return true;
                }
                targetKit.setCooldown(TimeUnit.SECONDS.toMillis(newCooldownSeconds));
                kitManager.saveKit(targetKit);
                player.sendMessage(ChatColor.GREEN + "Cooldown for kit '" + cooldownKitName + "' changed to " + newCooldownSeconds + " seconds.");
                return true;

            case "reset":
                if (!player.hasPermission("core.kit.reset")) {
                    player.sendMessage(kitManager.getMessage("no-permission-command"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /kit reset <player> <kit_name>");
                    return true;
                }
                String targetPlayerName = args[1];
                String kitToResetName = args[2];

                OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(targetPlayerName);
                if (!targetOfflinePlayer.hasPlayedBefore() && !targetOfflinePlayer.isOnline()) {
                    player.sendMessage(kitManager.getMessage("player-not-found").replace("{player_name}", targetPlayerName));
                    return true;
                }

                Kit kitToReset = kitManager.getKit(kitToResetName);
                if (kitToReset == null) {
                    player.sendMessage(kitManager.getMessage("kit-does-not-exist").replace("{kit_name}", kitToResetName));
                    return true;
                }

                UUID targetUUID = targetOfflinePlayer.getUniqueId();
                if (kitToReset.getLastClaimedMap().containsKey(targetUUID)) {
                    kitToReset.getLastClaimedMap().remove(targetUUID);
                    kitManager.saveKit(kitToReset);
                    player.sendMessage(ChatColor.GREEN + "Cooldown for kit '" + kitToResetName + "' reset for " + targetPlayerName + ".");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Player " + targetPlayerName + " does not have an active cooldown for kit '" + kitToResetName + "'.");
                }
                return true;

            default:
                // Custom Usage Message in English
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§b&lKits §7| §f<> = Required [] = Optional"));
                sender.sendMessage("§r"); // Empty line
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§c§lUsage:"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/kit §8- §7Open the kit menu."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/kit editor §8- §7Open the kit editor menu."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/kit create <name> <cooldown_seconds> §8- §7Create a new kit from your inventory items."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/kit delete <name> §8- §7Delete an existing kit."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/kit list §8- §7View a list of all available kits."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/kit claim <name> §8- §7Claim a kit by its name."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/kit give <player> <name> §8- §7Give a kit to another player."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/kit rename <current> <new> §8- §7Rename a kit."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/kit cooldown <name> <seconds> §8- §7Change a kit's cooldown."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/kit reset <player> <name> §8- §7Reset a player's kit cooldown."));
                sender.sendMessage("§r"); // Empty line
                return true;
        }
    }

    public void openKitGUI(Player player) {
        int size = kitManager.getMainGuiRows() * 9;
        if (size < 9) size = 9;
        if (size > 54) size = 54;

        Inventory gui = Bukkit.createInventory(null, size, kitManager.getGuiTitle());

        Set<Integer> usedSlots = new HashSet<>();
        for (Kit kit : kitManager.getKits().values()) {
            if (kit.getGuiSlot() != -1 && kit.getGuiSlot() < gui.getSize()) {
                ItemStack kitIcon = kit.getGuiIcon();
                if (kitIcon == null) {
                    kitIcon = new ItemStack(Material.CHEST);
                }
                ItemMeta meta = kitIcon.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GOLD + kit.getName());
                    List<String> lore = new ArrayList<>();
                    long timeLeft = kit.getTimeLeft(player.getUniqueId());

                    if (timeLeft > 0) {
                        lore.add(ChatColor.RED + "Time remaining: " + formatTime(timeLeft));
                        lore.add(ChatColor.GRAY + "Click to view contents.");
                    } else {
                        lore.add(ChatColor.GREEN + "Ready to claim!");
                        lore.add(ChatColor.GRAY + "Click to claim.");
                    }
                    meta.setLore(lore);
                    kitIcon.setItemMeta(meta);
                }
                gui.setItem(kit.getGuiSlot(), kitIcon);
                usedSlots.add(kit.getGuiSlot());
            }
        }

        for (Kit kit : kitManager.getKits().values()) {
            if (kit.getGuiSlot() == -1 || kit.getGuiSlot() >= gui.getSize() || !usedSlots.contains(kit.getGuiSlot())) {
                int nextFreeSlot = -1;
                for (int i = 0; i < gui.getSize(); i++) {
                    if (gui.getItem(i) == null || gui.getItem(i).getType() == Material.AIR) {
                        nextFreeSlot = i;
                        break;
                    }
                }
                if (nextFreeSlot != -1) {
                    ItemStack kitIcon = kit.getGuiIcon();
                    if (kitIcon == null) {
                        kitIcon = new ItemStack(Material.CHEST);
                    }
                    ItemMeta meta = kitIcon.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ChatColor.GOLD + kit.getName());
                        List<String> lore = new ArrayList<>();
                        long timeLeft = kit.getTimeLeft(player.getUniqueId());

                        if (timeLeft > 0) {
                            lore.add(ChatColor.RED + "Time remaining: " + formatTime(timeLeft));
                            lore.add(ChatColor.GRAY + "Click to view contents.");
                        } else {
                            lore.add(ChatColor.GREEN + "Ready to claim!");
                            lore.add(ChatColor.GRAY + "Click to claim.");
                        }
                        meta.setLore(lore);
                        kitIcon.setItemMeta(meta);
                    }
                    gui.setItem(nextFreeSlot, kitIcon);
                    if (kit.getGuiSlot() == -1 || kit.getGuiSlot() >= gui.getSize()) {
                        kit.setGuiSlot(nextFreeSlot);
                        kitManager.saveKit(kit);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Not enough slots in the menu to display all kits.");
                    break;
                }
            }
        }
        player.openInventory(gui);
    }

    private void claimKit(Player player, Kit kit) {
        if (!kit.canClaim(player.getUniqueId())) {
            player.sendMessage(kitManager.getMessage("kit-on-cooldown")
                    .replace("{kit_name}", kit.getName())
                    .replace("{time}", formatTime(kit.getTimeLeft(player.getUniqueId()))));
            return;
        }

        giveKitContents(player, kit);
        kit.recordClaim(player.getUniqueId());
        kitManager.saveKit(kit);
        player.sendMessage(kitManager.getMessage("kit-claimed").replace("{kit_name}", kit.getName()));
    }

    private void giveKitContents(Player player, Kit kit) {
        ItemStack[] validContents = Arrays.stream(kit.getContents())
                .filter(item -> item != null && item.getType() != Material.AIR)
                .toArray(ItemStack[]::new);

        if (player.getInventory().getContents().length + validContents.length > player.getInventory().getSize()) {
            player.sendMessage(kitManager.getMessage("inventory-full"));
        }

        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(validContents);
        if (!remaining.isEmpty()) {
            for (ItemStack item : remaining.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
    }

    private String formatTime(long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long days = TimeUnit.MILLISECONDS.toDays(millis);

        if (days > 0) {
            return String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds);
        } else {
            return String.format("%d seconds", seconds);
        }
    }
}