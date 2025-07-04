package net.moonly.commands.player;

import net.moonly.modules.deathmessage.DeathMessageGUI;
import net.moonly.modules.deathmessage.DeathMessageManager;
import net.moonly.modules.deathmessage.GUIManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DeathMessageCommand implements CommandExecutor {

    private final DeathMessageManager manager;
    private final GUIManager guiManager;

    public DeathMessageCommand(DeathMessageManager manager, GUIManager guiManager) {
        this.manager = manager;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        if (args.length == 0) {
            if (player.hasPermission(manager.getGuiPermission())) {
                new DeathMessageGUI(manager, player, guiManager).openInventory();
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /deathmessage <set <message> | clear | gui | reload>");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("set")) {
            if (!player.hasPermission(manager.getCustomMessagePermission())) {
                player.sendMessage(ChatColor.RED + "You don't have permission to set a custom death message.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /deathmessage set <message>");
                return true;
            }

            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                messageBuilder.append(args[i]).append(" ");
            }
            String customMessage = messageBuilder.toString().trim();

            if (customMessage.length() > manager.getMaxMessageLength()) {
                player.sendMessage(ChatColor.RED + "Your message is too long! Max length: " + manager.getMaxMessageLength());
                return true;
            }

            manager.setPlayerCustomDeathMessage(playerUUID, customMessage);
            player.sendMessage(ChatColor.GREEN + "Your death message has been set to: " + ChatColor.translateAlternateColorCodes('&', customMessage.replace("%player%", "You")));
            player.sendMessage(ChatColor.GRAY + "(Use %player% for your name, %original_death_message% for the default message, %killer% for killer's name, %cause% for death cause)");

        } else if (subCommand.equals("clear")) {
            if (!player.hasPermission(manager.getClearPermission())) {
                player.sendMessage(ChatColor.RED + "You don't have permission to clear your death message.");
                return true;
            }
            manager.clearPlayerCustomDeathMessage(playerUUID);
            player.sendMessage(ChatColor.GREEN + "Your custom death message has been cleared. You will now use the default.");

        } else if (subCommand.equals("gui")) {
            if (!player.hasPermission(manager.getGuiPermission())) {
                player.sendMessage(ChatColor.RED + "You don't have permission to open the death message GUI.");
                return true;
            }
            new DeathMessageGUI(manager, player, guiManager).openInventory();

        } else if (subCommand.equals("reload")) {
            if (!player.hasPermission("boxcore.deathmessage.reload")) { // Permiso específico para reload
                player.sendMessage(ChatColor.RED + "You don't have permission to reload the death messages module.");
                return true;
            }
            manager.reload();
            player.sendMessage(ChatColor.GREEN + "Death messages module reloaded.");
        } else {
            player.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /deathmessage <set <message> | clear | gui | reload>");
        }

        return true;
    }
}