package net.moonly.commands.staff;

import net.moonly.Box;
import net.moonly.modules.Spawn.SpawnManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final SpawnManager spawnManager;
    private final Box plugin;

    public SetSpawnCommand(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
        this.plugin = spawnManager.getPlugin();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getConfig().getBoolean("modules.spawn.enabled", true)) {
            sender.sendMessage(ChatColor.RED + "The Spawn module is disabled.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("core.spawn.setspawn")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to set the spawn.");
            return true;
        }

        spawnManager.setSpawnLocation(player.getLocation());
        player.sendMessage(spawnManager.getMessage("setspawn-success"));
        player.sendTitle(spawnManager.getMessage("setspawn-title"), spawnManager.getMessage("setspawn-subtitle"), 10, 70, 20);

        return true;
    }
}