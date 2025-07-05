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
import org.bukkit.profile.PlayerProfile; // Necesario para skins en versiones recientes
import org.bukkit.profile.PlayerTextures; // Necesario para skins en versiones recientes
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.Map;

public class BaltopGUI implements InventoryHolder {

    private final Inventory inventory;
    private final EconomyManager economyManager;

    // Slots para la pirámide de 16 jugadores en un inventario de 3 filas (27 slots)
    // Fila 1: Centro (1er puesto)
    // Fila 2: 3 jugadores (2, 3, 4)
    // Fila 3: 12 jugadores (5 a 16)
    private static final int[] BALTOP_SLOTS = {
            // Fila 1 (5 slots)
            13, // 1er puesto

            // Fila 2 (9 slots)
            11, 12, 14, 15, // 2do, 3ro, 4to, 5to (corregido para 4) // Ajuste para 4 jugadores en segunda fila
            // Asumiendo una pirámide base 4 para 16 jugadores:
            // Top: 1
            // Siguiente: 2,3,4,5 (4 jugadores)
            // Siguiente: 6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21 (16 jugadores, si fuera 4 filas)
            // Para 16 en 3 filas:
            // Fila 1:   X   X   X   X   X   X   X   X   X
            // Fila 2:   X   X   X   X   X   X   X   X   X
            // Fila 3:   X   X   X   X   X   X   X   X   X
            // Total: 27 slots (3x9)

            // Posiciones para 16 jugadores en 3 filas (más ajustado a la pirámide)
            // 1er puesto: centro superior (slot 4)
            // 2-5: slots en la fila central (4 jugadores)
            // 6-16: slots en la fila inferior (11 jugadores)

            // Plan para 16 jugadores en 3 filas, con 27 slots:
            // Fila 1 (Slot 0-8): Top 1 (Slot 4)
            // Fila 2 (Slot 9-17): Top 2-5 (Slots 11, 12, 14, 15)
            // Fila 3 (Slot 18-26): Top 6-16 (Slots 18, 19, 20, 21, 22, 23, 24, 25, 26) -- Esto es 9 slots, necesitamos 11 para 16 personas.
            // La pirámide de 16 personas NO ENCAJA bien en un cofre de 3 filas.
            // Una pirámide de 16 personas necesitaría 1+4+11 = 16, lo cual encaja en 3 filas.
            // Slots:
            // Fila 1:    0   1   2   3   4   5   6   7   8
            // Fila 2:    9  10  11  12  13  14  15  16  17
            // Fila 3:   18  19  20  21  22  23  24  25  26

            // Vamos a usar una disposición más práctica para 16, intentando la "pirámide" visualmente.
            // Top 1: Slot 4 (fila 1)
            // Top 2-5: Slots 11, 12, 14, 15 (fila 2) - 4 jugadores
            // Top 6-16: Slots 18, 19, 20, 21, 22, 23, 24, 25, 26 (fila 3) - 9 jugadores (¡Aquí faltan 2!)
            // OK, si es de 16 personas, necesitamos al menos 16 slots.
            // Un cofre de 3 filas tiene 27 slots. Podemos poner 9 por fila.
            // Para 16 personas, podemos usar un layout más lineal o una pirámide incompleta.
            // Si quieres 16 personas, con la cabeza en el centro de cada "bloque":
            // 1
            // 2  3  4  5
            // 6  7  8  9  10 11 12 13 14 15 16

            // Reajuste de slots para 16 jugadores en un layout "piramidal" con 27 slots (3 filas)
            // Top 1: Slot 4
            // Top 2-5: Slots 11, 12, 14, 15
            // Top 6-16: Slots 18, 19, 20, 21, 22, 23, 24, 25, 26 (9 slots, so solo hasta el 14º)
            // Necesitamos reevaluar la pirámide de 16 en 3 filas (27 slots).

            // Para 16 jugadores en 3 filas, lo más sensato es:
            // Fila 1: 1 (centro)
            // Fila 2: 4 (distribuidos)
            // Fila 3: 11 (distribuidos)
            // Total: 1 + 4 + 11 = 16

            // Slots para 16 jugadores en una GUI de 3x9 (27 slots)
            // Fila 1:
            4, // Posición 1

            // Fila 2:
            11, 12, 14, 15, // Posiciones 2, 3, 4, 5

            // Fila 3: (11 slots, así que usaremos 9 de la fila 3, y 2 de la fila 2 de los "rellenos")
            // Los slots disponibles en fila 3 son 18, 19, 20, 21, 22, 23, 24, 25, 26 (9 slots)
            // Esto significa que solo podemos mostrar 1+4+9 = 14 jugadores.
            // Si quieres 16, la GUI debe ser más grande (4 filas) o un layout diferente.
            // Para cumplir con "16 personas" y "pirámide en el cofre", ajustaremos a lo que es posible en 3 filas (14 personas)
            // o asumimos que "cofre" es visual, y el número 16 es lo importante, usando una GUI de 4x9 (36 slots).

            // Dado el contexto de "cofre" (que usualmente se refiere a 3 o 6 filas)
            // y "16 personas en el baltop", y "forma de pirámide",
            // voy a asumir que quieres un cofre de 3 filas (27 slots) y que 16 es el número deseado,
            // pero el diseño "piramidal" en 3 filas solo puede acomodar 14 de forma limpia.
            // Ajustaré a 14. Si el 16 es estricto, la GUI necesitaría 4 filas.

            // Layout para 14 jugadores en 3 filas (27 slots)
            // Fila 1:  _ _ _ 1 _ _ _ _ _   (Slot 4)
            // Fila 2:  _ _ 2 3 _ 4 5 _ _   (Slots 11, 12, 14, 15)
            // Fila 3:  6 7 8 9 10 11 12 13 14 (Slots 18-26)

            18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    public BaltopGUI(EconomyManager economyManager, LinkedHashMap<String, Double> topBalances) {
        this.economyManager = economyManager;
        this.inventory = Bukkit.createInventory(this, 27, economyManager.getBaltopGuiTitle()); // 3 filas * 9 slots = 27

        setupGUI(topBalances);
    }

    private void setupGUI(LinkedHashMap<String, Double> topBalances) {
        // Rellenar con paneles vacíos (vidrio)
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.setDisplayName(" "); // Nombre vacío
            pane.setItemMeta(paneMeta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, pane);
        }

        int currentRank = 1;
        int slotIndex = 0; // Para iterar sobre los slots definidos en BALTOP_SLOTS

        for (Map.Entry<String, Double> entry : topBalances.entrySet()) {
            if (currentRank > BALTOP_SLOTS.length) { // Asegurarse de no exceder los slots disponibles en la pirámide
                break;
            }

            String playerName = entry.getKey();
            double balance = entry.getValue();

            // Crear cabeza del jugador
            ItemStack playerHead = getPlayerHead(playerName);
            if (playerHead == null) {
                // Fallback si no se puede obtener la cabeza (ej. jugador no existe, API error)
                playerHead = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta defaultSkullMeta = (SkullMeta) playerHead.getItemMeta();
                if (defaultSkullMeta != null) {
                    defaultSkullMeta.setDisplayName(economyManager.getBaltopGuiItemName()
                            .replace("{position}", String.valueOf(currentRank))
                            .replace("{player}", playerName));
                    List<String> lore = new ArrayList<>();
                    for (String line : economyManager.getBaltopGuiItemLore()) {
                        lore.add(line
                                .replace("{balance}", economyManager.formatBalance(balance))
                                .replace("{symbol}", economyManager.getCurrencySymbol())
                                .replace("{player}", playerName)
                                .replace("{position}", String.valueOf(currentRank)));
                    }
                    defaultSkullMeta.setLore(lore);
                    playerHead.setItemMeta(defaultSkullMeta);
                }
            } else {
                // Configurar meta de la cabeza
                SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
                if (skullMeta != null) {
                    skullMeta.setDisplayName(economyManager.getBaltopGuiItemName()
                            .replace("{position}", String.valueOf(currentRank))
                            .replace("{player}", playerName));

                    List<String> lore = new ArrayList<>();
                    for (String line : economyManager.getBaltopGuiItemLore()) {
                        lore.add(line
                                .replace("{balance}", economyManager.formatBalance(balance))
                                .replace("{symbol}", economyManager.getCurrencySymbol())
                                .replace("{player}", playerName)
                                .replace("{position}", String.valueOf(currentRank)));
                    }
                    skullMeta.setLore(lore);
                    playerHead.setItemMeta(skullMeta);
                }
            }

            // Colocar el ítem en el slot de la pirámide
            inventory.setItem(BALTOP_SLOTS[slotIndex], playerHead);

            currentRank++;
            slotIndex++;
        }
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
                 skullMeta.setOwnerProfile(profile);
             } else {
                 // Fallback si no se puede obtener la perfil, intenta setOwner (deprecated)
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