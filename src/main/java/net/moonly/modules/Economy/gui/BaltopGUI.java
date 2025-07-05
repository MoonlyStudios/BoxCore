package net.moonly.modules.Economy.gui;

import net.moonly.modules.Economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

public class BaltopGUI implements InventoryHolder {

    private final Inventory inventory;
    private final EconomyManager economyManager;

    // Ya no necesitamos un array BALTOP_SLOTS estático aquí, el layout viene de la config.

    public BaltopGUI(EconomyManager economyManager, LinkedHashMap<String, Double> topBalances) {
        this.economyManager = economyManager;
        this.inventory = Bukkit.createInventory(this, economyManager.getBaltopGuiSize(), economyManager.getBaltopGuiTitle());

        setupGUI(topBalances);
    }

    private void setupGUI(LinkedHashMap<String, Double> topBalances) {
        // Mapa temporal para un acceso rápido a los datos del jugador por su posición en el top
        Map<Integer, Map.Entry<String, Double>> topPlayersByPosition = new HashMap<>();
        int currentRank = 1;
        for (Map.Entry<String, Double> entry : topBalances.entrySet()) {
            topPlayersByPosition.put(currentRank, entry);
            currentRank++;
        }

        List<String> layoutRows = economyManager.getBaltopGuiLayout();
        int slotCounter = 0;

        for (String row : layoutRows) {
            String[] itemsInRow = row.split(",");
            for (String itemSymbol : itemsInRow) {
                itemSymbol = itemSymbol.trim(); // Eliminar espacios en blanco

                if (slotCounter >= inventory.getSize()) { // Si ya llenamos la GUI, salimos
                    break;
                }

                ItemStack item = null;
                try {
                    // Intenta parsear el símbolo como un número de posición en el top
                    int position = Integer.parseInt(itemSymbol);
                    if (position > 0 && topPlayersByPosition.containsKey(position)) {
                        Map.Entry<String, Double> playerEntry = topPlayersByPosition.get(position);
                        String playerName = playerEntry.getKey();
                        double balance = playerEntry.getValue();

                        item = getPlayerHead(playerName);
                        if (item == null) { // Fallback si no se puede obtener la cabeza
                            item = new ItemStack(Material.PLAYER_HEAD);
                        }

                        // Configurar meta de la cabeza del jugador
                        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
                        if (skullMeta != null) {
                            skullMeta.setDisplayName(economyManager.getBaltopPlayerHeadName()
                                    .replace("{position}", String.valueOf(position))
                                    .replace("{player}", playerName));

                            List<String> lore = new ArrayList<>();
                            for (String line : economyManager.getBaltopPlayerHeadLore()) {
                                lore.add(line
                                        .replace("{balance}", economyManager.formatBalance(balance))
                                        .replace("{symbol}", economyManager.getCurrencySymbol())
                                        .replace("{player}", playerName)
                                        .replace("{position}", String.valueOf(position)));
                            }
                            skullMeta.setLore(lore);
                            item.setItemMeta(skullMeta);
                        }
                    } else {
                        // Si la posición no existe en el top (ej: un '15' si solo hay 10 jugadores)
                        // o si el número es 0, usamos un ítem de relleno por defecto o un vacío.
                        item = createDefaultFillerItem(); // O puedes definir un filler específico para "posición vacía"
                    }
                } catch (NumberFormatException e) {
                    // Si no es un número, entonces es un símbolo de ítem de relleno (ej: "F")
                    Map<String, Object> fillerData = economyManager.getGuiItemDefinitions().get(itemSymbol);
                    if (fillerData != null) {
                        String materialName = (String) fillerData.get("material");
                        Material material = Material.matchMaterial(materialName);
                        if (material == null) {
                            economyManager.getPlugin().getLogger().log(Level.WARNING, "Material '" + materialName + "' not found in topmenu.yml for item symbol '" + itemSymbol + "'. Using BARRIER.");
                            material = Material.BARRIER; // Fallback
                        }
                        item = new ItemStack(material);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName((String) fillerData.get("name"));
                            meta.setLore((List<String>) fillerData.get("lore"));
                            item.setItemMeta(meta);
                        }
                    } else {
                        // Si el símbolo no está definido en 'items', usar un ítem vacío/por defecto
                        item = createDefaultFillerItem();
                    }
                }
                inventory.setItem(slotCounter, item);
                slotCounter++;
            }
        }
    }

    private ItemStack createDefaultFillerItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE); // O cualquier otro material de relleno por defecto
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }


    @SuppressWarnings("deprecation") // Para setOwner(String) en versiones antiguas de Bukkit
    private ItemStack getPlayerHead(String playerName) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();

        if (skullMeta == null) return null; // No debería pasar, pero por seguridad

        // Para versiones modernas de Minecraft (1.17+), se usa PlayerProfile
        if (Bukkit.getPluginManager().isPluginEnabled("Paper") || Bukkit.getVersion().contains("1.17") || Bukkit.getVersion().contains("1.18") || Bukkit.getVersion().contains("1.19") || Bukkit.getVersion().contains("1.20") || Bukkit.getVersion().contains("1.21")) {
            // Intenta obtener la PlayerProfile (puede ser asíncrono y lento si el jugador no estuvo online recientemente)
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer != null) {
                PlayerProfile profile = offlinePlayer.getPlayerProfile();
                if (profile != null) {
                    skullMeta.setOwnerProfile(profile);
                } else {
                    // Fallback si PlayerProfile es null, intentar setOwningPlayer
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
                }
            } else {
                // Fallback si offlinePlayer es null, intentar setOwningPlayer con el nombre
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName)); // Este es el método más moderno para UUID
            }
        } else {
            // Para versiones antiguas (pre-1.17)
            skullMeta.setOwner(playerName);
        }
        playerHead.setItemMeta(skullMeta);
        return playerHead;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}