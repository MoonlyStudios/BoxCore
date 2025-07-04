package net.moonly.commands.staff;

import net.moonly.Box;
import net.moonly.modules.Claims.ClaimManager;
import net.moonly.modules.Claims.editor.ClaimEditorMainGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaimCommand implements CommandExecutor {

    private final Box plugin;
    private final ClaimManager claimManager;

    public ClaimCommand(Box plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (claimManager.getPlugin().getClaimManager() == null || !claimManager.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "The Claims module is disabled.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "select":
                if (!player.hasPermission("boxcore.claim.select")) {
                    player.sendMessage(claimManager.getMessage("no-permission-command"));
                    return true;
                }
                claimManager.giveSelectionTool(player);
                return true;

            case "create":
                if (!player.hasPermission("boxcore.claim.create")) {
                    player.sendMessage(claimManager.getMessage("no-permission-command"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /claim create <name>");
                    return true;
                }
                String regionName = args[1];

                if (regionName.length() < 3 || regionName.length() > 32 || !regionName.matches("^[a-zA-Z0-9_]+$")) {
                    player.sendMessage(claimManager.getMessage("claim-name-invalid"));
                    return true;
                }

                claimManager.createClaim(player, regionName);
                return true;

            case "clear":
                if (!player.hasPermission("boxcore.claim.clear")) {
                    player.sendMessage(claimManager.getMessage("no-permission-command"));
                    return true;
                }
                claimManager.clearPlayerSelection(player);
                return true;

            case "set":
                if (!player.hasPermission("boxcore.claim.setflag")) {
                    player.sendMessage(claimManager.getMessage("no-permission-command"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /claim set <region_name> <flag_type>");
                    player.sendMessage(ChatColor.RED + "Available flag types: mine");
                    return true;
                }
                String targetRegionName = args[1];
                String flagType = args[2].toLowerCase();

                claimManager.toggleRegionFlag(player, targetRegionName, flagType);
                return true;

            case "delete":
                if (!player.hasPermission("boxcore.claim.delete")) {
                    player.sendMessage(claimManager.getMessage("no-permission-command"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /claim delete <name>");
                    return true;
                }
                String regionToDelete = args[1];
                claimManager.deleteRegion(player, regionToDelete);
                return true;

            case "list":
                if (!player.hasPermission("boxcore.claim.list")) {
                    player.sendMessage(claimManager.getMessage("no-permission-command"));
                    return true;
                }
                claimManager.listRegions(player);
                return true;

            case "editor": // NEW SUBCOMMAND
                if (!player.hasPermission("boxcore.claim.editor")) { // NEW PERMISSION
                    player.sendMessage(claimManager.getMessage("no-permission-command"));
                    return true;
                }
                new ClaimEditorMainGUI(claimManager).open(player);
                return true;

            default:
                sendUsage(player);
                return true;
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lClaims &7| &f<> = Required [] = Optional"));
        player.sendMessage("§r");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "§c§lUsage:"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/claim select §8- §7Get the selection tool to define your claim area."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/claim create <name> §8- §7Create a new claim from your selection."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/claim clear §8- §7Clear your current claim selection."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/claim set <region_name> <flag_type> §8- §7Toggle a region's flag (e.g., mine)."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/claim delete <name> §8- §7Delete a claim."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/claim list §8- §7List all regions in your current world."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7• §f/claim editor §8- §7Open the graphical claim editor.")); // NEW USAGE
        player.sendMessage("§r");
    }
}