package net.moonly.commands.staff;

import net.moonly.modules.Economy.EconomyManager;
// import net.moonly.modules.Economy.gui.BaltopGUI; // Ya no necesario aquí
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CompletableFuture;

public class EconomyAdminCommand implements CommandExecutor {

    private final EconomyManager economyManager;

    public EconomyAdminCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!economyManager.getPlugin().getConfig().getBoolean("modules.economy.enabled", true)) {
            sender.sendMessage(economyManager.getMessage("prefix") + "&cEl módulo de Economía está deshabilitado.");
            return true;
        }

        // Si no hay argumentos, y es un jugador, puede ver su balance (core.economy.use)
        if (args.length < 1) {
            if (sender instanceof Player) {
                if (!sender.hasPermission("core.economy.use")) {
                    sender.sendMessage(economyManager.getMessage("no-permission"));
                    return true;
                }
                economyManager.getPlayerBalance(((Player) sender).getUniqueId()).thenAccept(balance -> {
                    sender.sendMessage(economyManager.getMessage("balance-self")
                            .replace("{balance}", economyManager.formatBalance(balance))
                            .replace("{symbol}", economyManager.getCurrencySymbol()));
                });
            } else { // Consola sin argumentos
                sender.sendMessage(economyManager.getMessage("usage-eco-admin"));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Permisos para los subcomandos:
        // - 'bal', 'balance' requieren 'core.economy.use'
        // - Todos los demás (give, set, take, giveall, takeall, reset, reload) requieren 'core.economy.admin'
        if (subCommand.equals("bal") || subCommand.equals("balance")) { // 'baltop' ha sido removido de aquí
            if (!sender.hasPermission("core.economy.use")) {
                sender.sendMessage(economyManager.getMessage("no-permission"));
                return true;
            }
        } else { // Si es cualquier otro subcomando (admin)
            if (!sender.hasPermission("core.economy.admin")) {
                sender.sendMessage(economyManager.getMessage("no-permission"));
                return true;
            }
        }


        switch (subCommand) {
            case "give":
            case "take":
            case "set":
                if (args.length < 3) {
                    sender.sendMessage(economyManager.getMessage("usage-eco-admin"));
                    return true;
                }
                handlePlayerMoneyModification(sender, subCommand, args[1], args[2]);
                break;
            case "giveall":
            case "takeall":
                if (args.length < 2) {
                    sender.sendMessage(economyManager.getMessage("usage-eco-admin"));
                    return true;
                }
                handleGiveTakeAll(sender, subCommand, args[1]);
                break;
            case "reset":
                if (args.length < 2) {
                    sender.sendMessage(economyManager.getMessage("usage-eco-admin"));
                    return true;
                }
                handleReset(sender, args[1]);
                break;
            case "bal": // Para ver el balance de otro jugador
            case "balance":
                if (args.length < 2) {
                    sender.sendMessage(economyManager.getMessage("usage-eco-admin"));
                    return true;
                }
                handleBalanceCheck(sender, args[1]);
                break;
            // case "baltop": // <--- ESTA SECCIÓN HA SIDO REMOVIDA
            //     if (!(sender instanceof Player)) {
            //         sender.sendMessage(economyManager.getMessage("prefix") + "&cEste comando de GUI solo puede ser ejecutado por un jugador.");
            //         return true;
            //     }
            //     handleBaltopGUI((Player) sender);
            //     break;
            case "reload":
                economyManager.reloadConfig();
                sender.sendMessage(economyManager.getMessage("prefix") + "&aConfiguración de economía recargada.");
                break;
            default:
                sender.sendMessage(economyManager.getMessage("usage-eco-admin"));
                break;
        }
        return true;
    }

    private void handlePlayerMoneyModification(CommandSender sender, String action, String targetName, String amountString) {
        new BukkitRunnable() {
            @Override
            public void run() {
                double rawAmount;
                try {
                    rawAmount = Double.parseDouble(amountString);
                } catch (NumberFormatException e) {
                    sender.sendMessage(economyManager.getMessage("invalid-amount"));
                    return;
                }

                final double finalAmount;
                if (rawAmount < 0 && (action.equals("give"))) {
                    sender.sendMessage(economyManager.getMessage("invalid-amount"));
                    return;
                }
                if (rawAmount < 0 && (action.equals("take"))) {
                    finalAmount = Math.abs(rawAmount);
                } else {
                    finalAmount = rawAmount;
                }


                economyManager.getDatabase().getPlayerUUID(targetName).thenAccept(uuid -> {
                    if (uuid == null) {
                        sender.sendMessage(economyManager.getMessage("player-not-found"));
                        return;
                    }

                    OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(uuid);
                    String actualTargetName = offlineTarget.getName();
                    if (actualTargetName == null) actualTargetName = targetName;

                    Player playerSender = (sender instanceof Player) ? (Player) sender : null;

                    switch (action) {
                        case "give":
                            economyManager.addMoney(uuid, actualTargetName, finalAmount, playerSender);
                            break;
                        case "take":
                            economyManager.takeMoney(uuid, actualTargetName, finalAmount, playerSender);
                            break;
                        case "set":
                            economyManager.setMoney(uuid, actualTargetName, finalAmount, playerSender);
                            break;
                    }
                }).exceptionally(e -> {
                    economyManager.getPlugin().getLogger().severe("Error in handlePlayerMoneyModification: " + e.getMessage());
                    sender.sendMessage(economyManager.getMessage("prefix") + "&cOcurrió un error al procesar el comando.");
                    return null;
                });
            }
        }.runTaskAsynchronously(economyManager.getPlugin());
    }

    private void handleGiveTakeAll(CommandSender sender, String action, String amountString) {
        new BukkitRunnable() {
            @Override
            public void run() {
                double amount;
                try {
                    amount = Double.parseDouble(amountString);
                } catch (NumberFormatException e) {
                    sender.sendMessage(economyManager.getMessage("invalid-amount"));
                    return;
                }

                if (amount < 0) {
                    sender.sendMessage(economyManager.getMessage("invalid-amount"));
                    return;
                }

                if (action.equalsIgnoreCase("giveall")) {
                    economyManager.addMoneyToAll(amount);
                } else if (action.equalsIgnoreCase("takeall")) {
                    economyManager.takeMoneyFromAll(amount);
                }
            }
        }.runTaskAsynchronously(economyManager.getPlugin());
    }

    private void handleReset(CommandSender sender, String targetName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                economyManager.getDatabase().getPlayerUUID(targetName).thenAccept(uuid -> {
                    if (uuid == null) {
                        sender.sendMessage(economyManager.getMessage("player-not-found"));
                        return;
                    }
                    OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(uuid);
                    String actualTargetName = offlineTarget.getName();
                    if (actualTargetName == null) actualTargetName = targetName;

                    Player playerSender = (sender instanceof Player) ? (Player) sender : null;
                    economyManager.resetMoney(uuid, actualTargetName, playerSender);
                }).exceptionally(e -> {
                    economyManager.getPlugin().getLogger().severe("Error in handleReset: " + e.getMessage());
                    sender.sendMessage(economyManager.getMessage("prefix") + "&cOcurrió un error al resetear el balance.");
                    return null;
                });
            }
        }.runTaskAsynchronously(economyManager.getPlugin());
    }

    private void handleBalanceCheck(CommandSender sender, String targetName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                economyManager.getDatabase().getPlayerUUID(targetName).thenCompose(uuid -> {
                    if (uuid == null) {
                        sender.sendMessage(economyManager.getMessage("player-not-found"));
                        return CompletableFuture.completedFuture(null);
                    }
                    return economyManager.getPlayerBalance(uuid).thenApply(balance -> {
                        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(uuid);
                        String actualTargetName = offlineTarget.getName();
                        if (actualTargetName == null) actualTargetName = targetName;

                        sender.sendMessage(economyManager.getMessage("balance-other")
                                .replace("{player}", actualTargetName)
                                .replace("{balance}", economyManager.formatBalance(balance))
                                .replace("{symbol}", economyManager.getCurrencySymbol()));
                        return null;
                    });
                }).exceptionally(e -> {
                    economyManager.getPlugin().getLogger().severe("Error in handleBalanceCheck: " + e.getMessage());
                    sender.sendMessage(economyManager.getMessage("prefix") + "&cOcurrió un error al consultar el balance.");
                    return null;
                });
            }
        }.runTaskAsynchronously(economyManager.getPlugin());
    }

    // El método handleBaltopGUI ya no es necesario en esta clase.
    // private void handleBaltopGUI(Player player) { ... }
}