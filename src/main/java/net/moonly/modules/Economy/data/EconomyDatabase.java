package net.moonly.modules.Economy.data;

import net.moonly.Box;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class EconomyDatabase {

    private final Box plugin;
    private Connection connection;
    private final String databasePath;

    public EconomyDatabase(Box plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder() + File.separator + "modules" + File.separator + "economy" + File.separator + "economy.db";
        initializeDatabase();
    }

    private void initializeDatabase() {
        File databaseFile = new File(databasePath);
        File parentDir = databaseFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        try {
            Class.forName("org.sqlite.JDBC"); // Cargar el driver SQLite
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            createTable();
            plugin.getLogger().info("SQLite database for Economy initialized successfully.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to SQLite database for Economy: " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite JDBC driver not found! " + e.getMessage(), e);
        }
    }

    private void createTable() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_balances (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "player_name VARCHAR(16) NOT NULL," +
                        "balance DOUBLE NOT NULL DEFAULT 0.0" +
                        ");"
        )) {
            ps.executeUpdate();
            plugin.getLogger().info("Table 'player_balances' checked/created.");
        }
    }

    public CompletableFuture<Double> getBalance(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT balance FROM player_balances WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get balance for " + uuid.toString(), e);
            }
            return 0.0; // Devuelve 0.0 si el jugador no existe o hay un error
        });
    }

    public CompletableFuture<Void> setBalance(UUID uuid, String playerName, double amount) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO player_balances (uuid, player_name, balance) VALUES (?, ?, ?)"
            )) {
                ps.setString(1, uuid.toString());
                ps.setString(2, playerName);
                ps.setDouble(3, amount);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set balance for " + uuid.toString(), e);
            }
        });
    }

    public CompletableFuture<Void> addBalance(UUID uuid, String playerName, double amount) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO player_balances (uuid, player_name, balance) VALUES (?, ?, ?) " +
                            "ON CONFLICT(uuid) DO UPDATE SET balance = balance + EXCLUDED.balance"
            )) {
                ps.setString(1, uuid.toString());
                ps.setString(2, playerName);
                ps.setDouble(3, amount);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add balance for " + uuid.toString(), e);
            }
        });
    }

    public CompletableFuture<Void> takeBalance(UUID uuid, String playerName, double amount) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO player_balances (uuid, player_name, balance) VALUES (?, ?, ?) " +
                            "ON CONFLICT(uuid) DO UPDATE SET balance = balance - EXCLUDED.balance"
            )) {
                ps.setString(1, uuid.toString());
                ps.setString(2, playerName);
                ps.setDouble(3, amount);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to take balance for " + uuid.toString(), e);
            }
        });
    }

    public CompletableFuture<String> getPlayerName(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT player_name FROM player_balances WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString("player_name");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player name for " + uuid.toString(), e);
            }
            return null;
        });
    }

    public CompletableFuture<UUID> getPlayerUUID(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM player_balances WHERE player_name = ? COLLATE NOCASE")) { // NOCASE para búsqueda insensible a mayúsculas/minúsculas
                ps.setString(1, playerName);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player UUID for " + playerName, e);
            }
            return null;
        });
    }

    public CompletableFuture<LinkedHashMap<String, Double>> getTopBalances(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            LinkedHashMap<String, Double> top = new LinkedHashMap<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT player_name, balance FROM player_balances ORDER BY balance DESC LIMIT ?"
            )) {
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    top.put(rs.getString("player_name"), rs.getDouble("balance"));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get top balances.", e);
            }
            return top;
        });
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("SQLite database connection for Economy closed.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to close SQLite connection: " + e.getMessage(), e);
            }
        }
    }
}