package net.moonly.modules.Economy.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.moonly.Box;
import net.moonly.modules.Economy.EconomyManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

public class EconomyPlaceholder extends PlaceholderExpansion {

    private final Box plugin;
    private final EconomyManager economyManager;

    public EconomyPlaceholder(Box plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "box"; // Esto significa que tus placeholders serán como %box_balance%
    }

    @Override
    public @NotNull String getAuthor() {
        // Asegúrate de que tu plugin.yml tiene un autor definido
        return plugin.getDescription().getAuthors().isEmpty() ? "Unknown" : plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        // Verifica si PlaceholderAPI está presente. Esto ya se hace en Box.java, pero es buena práctica aquí también.
        return plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    @Override
    public boolean persist() {
        return true; // Los placeholders persisten entre recargas de plugins
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // Placeholder para el balance del jugador: %box_balance%
        if (params.equalsIgnoreCase("balance")) {
            try {
                // Bloquea el hilo hasta obtener el resultado.
                // Aunque no es ideal para grandes operaciones, PlaceholderAPI opera de forma síncrona para onRequest.
                double balance = economyManager.getPlayerBalance(player.getUniqueId()).get();
                return economyManager.formatBalance(balance) + economyManager.getCurrencySymbol();
            } catch (InterruptedException | ExecutionException e) {
                plugin.getLogger().severe("Error fetching balance for placeholder %box_balance% for player " + player.getName() + ": " + e.getMessage());
                return "ERROR";
            }
        }

        // Placeholders para baltop: %box_baltop_player_X%, %box_baltop_balance_X%
        if (params.startsWith("baltop_")) {
            String[] parts = params.split("_"); // Ejemplo: "baltop", "player", "1"
            if (parts.length == 3) {
                String type = parts[1]; // "player" o "balance"
                int position;
                try {
                    position = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    return ""; // Formato de posición inválido
                }

                if (position <= 0 || position > economyManager.getBaltopSize()) {
                    return ""; // Posición fuera de rango configurado
                }

                try {
                    // Obtener el top de balances. También se bloquea aquí.
                    java.util.LinkedHashMap<String, Double> top = economyManager.getDatabase().getTopBalances(economyManager.getBaltopSize()).get();
                    int currentPosition = 1;
                    for (java.util.Map.Entry<String, Double> entry : top.entrySet()) {
                        if (currentPosition == position) {
                            if (type.equalsIgnoreCase("player")) {
                                return entry.getKey();
                            } else if (type.equalsIgnoreCase("balance")) {
                                return economyManager.formatBalance(entry.getValue()) + economyManager.getCurrencySymbol();
                            }
                        }
                        currentPosition++;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    plugin.getLogger().severe("Error fetching baltop for placeholder: " + e.getMessage());
                    return "ERROR";
                }
            }
        }

        return null; // Retorna nulo si el placeholder no es reconocido
    }
}