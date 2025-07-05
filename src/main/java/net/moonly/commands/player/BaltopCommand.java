package net.moonly.commands.player;

import net.moonly.modules.Economy.EconomyManager;
import net.moonly.modules.Economy.gui.BaltopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BaltopCommand implements CommandExecutor {

    private final EconomyManager economyManager;

    public BaltopCommand(EconomyManager economyManager) {
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

        // Permiso para usar el comando /baltop
        if (!player.hasPermission("core.economy.use")) {
            player.sendMessage(economyManager.getMessage("no-permission"));
            return true;
        }

        // Si hay argumentos adicionales, ignóralos o muestra un mensaje de uso.
        if (args.length > 0) {
            player.sendMessage(economyManager.getMessage("prefix") + "&cUso: /baltop");
            return true;
        }

        // Obtener el top de balances de forma asíncrona y abrir la GUI
        new BukkitRunnable() {
            @Override
            public void run() {
                economyManager.getDatabase().getTopBalances(economyManager.getBaltopSize()).thenAccept(topBalances -> {
                    // Abrir la GUI en el hilo principal después de obtener los datos
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            BaltopGUI baltopGUI = new BaltopGUI(economyManager, topBalances);
                            player.openInventory(baltopGUI.getInventory());
                        }
                    }.runTask(economyManager.getPlugin());
                }).exceptionally(e -> {
                    economyManager.getPlugin().getLogger().severe("Error fetching baltop for GUI: " + e.getMessage());
                    player.sendMessage(economyManager.getMessage("prefix") + "&cOcurrió un error al generar el top de dinero.");
                    return null;
                });
            }
        }.runTaskAsynchronously(economyManager.getPlugin());

        return true;
    }
}