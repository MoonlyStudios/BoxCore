package net.moonly.modules.Economy;

import net.moonly.Box;
import net.moonly.modules.Economy.data.EconomyDatabase;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class EconomyManager {

    private final Box plugin;
    private EconomyDatabase database;
    private FileConfiguration config; // Este ahora será el economy.yml
    private File configFile;

    private double initialBalance;
    private String currencySymbol;
    private String decimalFormatPattern;
    private boolean formatLargeNumbers;
    private int baltopSize;
    private String prefix;

    // --- Nuevas propiedades para la GUI de Baltop ---
    private String baltopGuiTitle;
    private String baltopGuiItemName;
    private java.util.List<String> baltopGuiItemLore;
    // ----------------------------------------------


    public EconomyManager(Box plugin) {
        this.plugin = plugin;
        this.database = new EconomyDatabase(plugin);
        loadConfig();
    }

    public Box getPlugin() {
        return plugin;
    }

    public EconomyDatabase getDatabase() {
        return database;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public double getInitialBalance() {
        return initialBalance;
    }

    private void loadConfig() {
        // La ruta ahora apunta al archivo dentro de la subcarpeta 'modules/economy'
        configFile = new File(plugin.getDataFolder(), "modules/economy/economy.yml");
        if (!configFile.exists()) {
            // Guarda el recurso predeterminado desde el JAR a la carpeta de datos del plugin
            plugin.saveResource("modules/economy/economy.yml", false);
            plugin.getLogger().log(Level.INFO, "Created default economy.yml.");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().log(Level.INFO, "Loaded economy.yml from path: " + configFile.getAbsolutePath());

        // Aquí no necesitamos el prefijo "settings." porque economy.yml es la configuración raíz de este módulo
        this.initialBalance = config.getDouble("initial-balance", 1000.0);
        this.currencySymbol = config.getString("currency-symbol", "$");
        this.decimalFormatPattern = config.getString("decimal-format", "#,##0.00");
        this.formatLargeNumbers = config.getBoolean("format-large-numbers", true);
        this.baltopSize = config.getInt("baltop-size", 10);
        this.prefix = ChatColor.translateAlternateColorCodes('&', config.getString("messages.prefix", "&b[&6Economia&b] &r"));

        // --- Cargar nuevas propiedades de la GUI de Baltop ---
        this.baltopGuiTitle = ChatColor.translateAlternateColorCodes('&', config.getString("gui.baltop.title", "&8Top Economia"));
        this.baltopGuiItemName = ChatColor.translateAlternateColorCodes('&', config.getString("gui.baltop.item-name", "&a{position}. &e{player}"));
        this.baltopGuiItemLore = config.getStringList("gui.baltop.item-lore");
        // Traducir colores en la lore
        if (this.baltopGuiItemLore != null) {
            for (int i = 0; i < this.baltopGuiItemLore.size(); i++) {
                this.baltopGuiItemLore.set(i, ChatColor.translateAlternateColorCodes('&', this.baltopGuiItemLore.get(i)));
            }
        }
        // ----------------------------------------------------

        plugin.getLogger().info("Loaded economy.yml configuration settings.");
    }

    public void reloadConfig() {
        loadConfig();
        plugin.getLogger().info("Economy module config reloaded.");
    }

    public String getMessage(String path) {
        String message = config.getString("messages." + path);
        if (message == null) {
            plugin.getLogger().log(Level.WARNING, "Message path 'messages." + path + "' not found in economy.yml. Using fallback.");
            return ChatColor.RED + "Message not found: " + path;
        }
        return prefix + ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Formatea un valor monetario según la configuración.
     * Soporta formatos como 10K, 1M, y decimales.
     */
    public String formatBalance(double amount) {
        DecimalFormat formatter = new DecimalFormat(decimalFormatPattern);
        formatter.setRoundingMode(RoundingMode.FLOOR); // Redondea hacia abajo para evitar centavos extra

        if (formatLargeNumbers) {
            if (amount >= 1_000_000_000_000.0) { // Trillions
                return formatter.format(amount / 1_000_000_000_000.0) + "T";
            } else if (amount >= 1_000_000_000.0) { // Billions
                return formatter.format(amount / 1_000_000_000.0) + "B";
            } else if (amount >= 1_000_000.0) { // Millions
                return formatter.format(amount / 1_000_000.0) + "M";
            } else if (amount >= 1_000.0) { // Thousands
                return formatter.format(amount / 1_000.0) + "K";
            }
        }
        return formatter.format(amount);
    }

    public CompletableFuture<Double> getPlayerBalance(UUID uuid) {
        return database.getBalance(uuid);
    }

    public CompletableFuture<Boolean> hasEnough(UUID uuid, double amount) {
        return getPlayerBalance(uuid).thenApply(balance -> balance >= amount);
    }

    public void addMoney(UUID uuid, String playerName, double amount, Player sender) {
        if (amount < 0) {
            if (sender != null) sender.sendMessage(getMessage("invalid-amount"));
            return;
        }
        database.addBalance(uuid, playerName, amount).thenRun(() -> {
            getPlayerBalance(uuid).thenAccept(newBalance -> {
                if (sender != null) { // Si el comando fue ejecutado por un jugador (admin)
                    sender.sendMessage(getMessage("eco-give-success")
                            .replace("{amount}", formatBalance(amount))
                            .replace("{symbol}", currencySymbol)
                            .replace("{target}", playerName)
                            .replace("{new_balance}", formatBalance(newBalance)));
                }
                // Si el jugador está online, actualiza su balance
                Player targetPlayer = plugin.getServer().getPlayer(uuid);
                if (targetPlayer != null && targetPlayer.isOnline() && (sender == null || !targetPlayer.equals(sender))) {
                    // Solo envía si no es el mismo que el sender, o si el sender es null (ej. giveall)
                    targetPlayer.sendMessage(getMessage("eco-give-received").replace("{amount}", formatBalance(amount)).replace("{symbol}", currencySymbol).replace("{sender}", (sender != null ? sender.getName() : "Consola/Servidor")));
                }
            });
        });
    }

    public void takeMoney(UUID uuid, String playerName, double amount, Player sender) {
        if (amount < 0) {
            if (sender != null) sender.sendMessage(getMessage("invalid-amount"));
            return;
        }
        database.takeBalance(uuid, playerName, amount).thenRun(() -> {
            getPlayerBalance(uuid).thenAccept(newBalance -> {
                if (sender != null) {
                    sender.sendMessage(getMessage("eco-take-success")
                            .replace("{amount}", formatBalance(amount))
                            .replace("{symbol}", currencySymbol)
                            .replace("{target}", playerName)
                            .replace("{new_balance}", formatBalance(newBalance)));
                }
                Player targetPlayer = plugin.getServer().getPlayer(uuid);
                if (targetPlayer != null && targetPlayer.isOnline() && (sender == null || !targetPlayer.equals(sender))) {
                    targetPlayer.sendMessage(getMessage("eco-take-removed").replace("{amount}", formatBalance(amount)).replace("{symbol}", currencySymbol).replace("{sender}", (sender != null ? sender.getName() : "Consola/Servidor")));
                }
            });
        });
    }

    public void setMoney(UUID uuid, String playerName, double amount, Player sender) {
        if (amount < 0) {
            if (sender != null) sender.sendMessage(getMessage("invalid-amount"));
            return;
        }
        database.setBalance(uuid, playerName, amount).thenRun(() -> {
            getPlayerBalance(uuid).thenAccept(newBalance -> {
                if (sender != null) {
                    sender.sendMessage(getMessage("eco-set-success")
                            .replace("{target}", playerName)
                            .replace("{new_balance}", formatBalance(newBalance))
                            .replace("{symbol}", currencySymbol));
                }
                Player targetPlayer = plugin.getServer().getPlayer(uuid);
                if (targetPlayer != null && targetPlayer.isOnline() && (sender == null || !targetPlayer.equals(sender))) {
                    targetPlayer.sendMessage(getMessage("eco-set-to-you").replace("{new_balance}", formatBalance(newBalance)).replace("{symbol}", currencySymbol).replace("{sender}", (sender != null ? sender.getName() : "Consola/Servidor")));
                }
            });
        });
    }

    public void resetMoney(UUID uuid, String playerName, Player sender) {
        setMoney(uuid, playerName, 0.0, sender);
    }

    public void transferMoney(UUID senderUuid, String senderName, UUID targetUuid, String targetName, double amount, Player senderPlayer) {
        if (amount <= 0) {
            senderPlayer.sendMessage(getMessage("invalid-amount"));
            return;
        }

        hasEnough(senderUuid, amount).thenAccept(hasMoney -> {
            if (!hasMoney) {
                senderPlayer.sendMessage(getMessage("not-enough-money"));
                return;
            }

            // Realizar las operaciones de forma asíncrona
            database.takeBalance(senderUuid, senderName, amount).thenCompose(v ->
                    database.addBalance(targetUuid, targetName, amount)
            ).thenRun(() -> {
                // Notificar al que envía
                senderPlayer.sendMessage(getMessage("pay-success-sender")
                        .replace("{amount}", formatBalance(amount))
                        .replace("{symbol}", currencySymbol)
                        .replace("{target}", targetName));

                // Notificar al que recibe si está online
                Player receiverPlayer = plugin.getServer().getPlayer(targetUuid);
                if (receiverPlayer != null && receiverPlayer.isOnline()) {
                    receiverPlayer.sendMessage(getMessage("pay-success-receiver")
                            .replace("{amount}", formatBalance(amount))
                            .replace("{symbol}", currencySymbol)
                            .replace("{sender}", senderName));
                }
            }).exceptionally(e -> {
                plugin.getLogger().log(Level.SEVERE, "Error during money transfer:", e);
                senderPlayer.sendMessage(ChatColor.RED + "Ocurrió un error al intentar transferir dinero.");
                return null;
            });
        });
    }

    public CompletableFuture<Void> addMoneyToAll(double amount) {
        if (amount < 0) return CompletableFuture.completedFuture(null); // No permitir añadir negativo
        return CompletableFuture.runAsync(() -> {
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                // Se usa el método directo de la base de datos para eficiencia masiva
                database.addBalance(player.getUniqueId(), player.getName(), amount);
            });
        }).thenRun(() -> {
            // Se envía un mensaje global en el hilo principal
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getServer().broadcastMessage(getMessage("eco-giveall-success")
                            .replace("{amount}", formatBalance(amount))
                            .replace("{symbol}", currencySymbol));
                }
            }.runTask(plugin);
        });
    }

    public CompletableFuture<Void> takeMoneyFromAll(double amount) {
        if (amount < 0) return CompletableFuture.completedFuture(null); // No permitir quitar negativo
        return CompletableFuture.runAsync(() -> {
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                // Se usa el método directo de la base de datos para eficiencia masiva
                database.takeBalance(player.getUniqueId(), player.getName(), amount);
            });
        }).thenRun(() -> {
            // Se envía un mensaje global en el hilo principal
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getServer().broadcastMessage(getMessage("eco-takeall-success")
                            .replace("{amount}", formatBalance(amount))
                            .replace("{symbol}", currencySymbol));
                }
            }.runTask(plugin);
        });
    }

    public void shutdown() {
        database.closeConnection();
    }

    public int getBaltopSize() {
        return baltopSize;
    }

    // --- Nuevos getters para la GUI de Baltop ---
    public String getBaltopGuiTitle() {
        return baltopGuiTitle;
    }

    public String getBaltopGuiItemName() {
        return baltopGuiItemName;
    }

    public java.util.List<String> getBaltopGuiItemLore() {
        return baltopGuiItemLore;
    }
    // ----------------------------------------------
}