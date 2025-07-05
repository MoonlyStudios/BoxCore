package net.moonly.commands.player;

import net.moonly.modules.Economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PayCommand implements CommandExecutor {

    private final EconomyManager economyManager;

    public PayCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!economyManager.getPlugin().getConfig().getBoolean("modules.economy.enabled", true)) {
            sender.sendMessage(economyManager.getMessage("prefix") + "&cEl módulo de Economía está deshabilitado.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(economyManager.getMessage("prefix") + "&cEste comando solo puede ser ejecutado por un jugador.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("core.economy.use")) {
            player.sendMessage(economyManager.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(economyManager.getMessage("usage-pay"));
            return true;
        }

        String targetName = args[0];
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(economyManager.getMessage("invalid-amount"));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(economyManager.getMessage("invalid-amount"));
            return true;
        }

        if (player.getName().equalsIgnoreCase(targetName)) {
            player.sendMessage(economyManager.getMessage("prefix") + "&cNo puedes pagarte a ti mismo.");
            return true;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                // Obtener UUID del jugador objetivo de forma asíncrona
                economyManager.getDatabase().getPlayerUUID(targetName).thenAccept(targetUUID -> {
                    if (targetUUID == null) {
                        player.sendMessage(economyManager.getMessage("player-not-found"));
                        return;
                    }
                    // Obtener el nombre real del target si está online o el último conocido
                    OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetUUID);
                    String actualTargetName = offlineTarget.getName();
                    if (actualTargetName == null) actualTargetName = targetName; // Fallback por si acaso

                    economyManager.transferMoney(player.getUniqueId(), player.getName(), targetUUID, actualTargetName, amount, player);
                }).exceptionally(e -> {
                    economyManager.getPlugin().getLogger().severe("Error resolving target player UUID for pay command: " + e.getMessage());
                    player.sendMessage(economyManager.getMessage("prefix") + "&cOcurrió un error al procesar tu pago.");
                    return null;
                });
            }
        }.runTaskAsynchronously(economyManager.getPlugin());

        return true;
    }
}