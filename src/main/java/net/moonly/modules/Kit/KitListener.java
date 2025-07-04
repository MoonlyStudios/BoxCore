package net.moonly.modules.Kit; // Changed package


import net.moonly.modules.Kit.editor.KitConfigEditorGUI;
import net.moonly.modules.Kit.editor.KitEditorMainGUI;
import net.moonly.modules.Kit.editor.KitListEditorGUI;
import net.moonly.modules.Kit.editor.KitSettingsEditorGUI;
import net.moonly.commands.staff.KitCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KitListener implements Listener {

    private final KitManager kitManager;

    public static final Map<UUID, String> playersSelectingIcon = new HashMap<>();

    public KitListener(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (kitManager.getPlugin().getKitManager() == null) { // Access via plugin's getter
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedInventory == null) {
            return;
        }

        String title = event.getView().getTitle();

        if (title.equals(kitManager.getGuiTitle())) {
            if (KitCommand.playerEditingKitSlot.containsKey(player.getUniqueId())) {
                handleSlotSelection(player, event.getSlot());
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String kitName = ChatColor.stripColor(meta.getDisplayName());
                Kit kit = kitManager.getKit(kitName);

                if (kit != null) {
                    if (event.isLeftClick()) {
                        claimKit(player, kit);
                        player.closeInventory();
                    } else if (event.isRightClick() || event.isShiftClick()) {
                        openKitContentsViewerGUI(player, kit);
                    }
                }
            }
            return;
        }

        if (title.equals(ChatColor.DARK_AQUA + "Kit Editor - Main Menu")) {
            event.setCancelled(true);
            if (!player.hasPermission("core.kit.editor")) { // Changed permission prefix
                player.sendMessage(kitManager.getMessage("no-permission-command"));
                player.closeInventory();
                return;
            }

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta() != null) {
                String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
                if (displayName.equals("Gestionar Kits")) {
                    new KitListEditorGUI(kitManager).open(player);
                } else if (displayName.equals("Configuración del Menú Principal")) {
                    new KitConfigEditorGUI(kitManager).open(player);
                }
            }
            return;
        }

        if (title.equals(ChatColor.DARK_AQUA + "Kit Editor - Kits")) {
            event.setCancelled(true);
            if (!player.hasPermission("core.kit.edit")) { // Changed permission prefix
                player.sendMessage(kitManager.getMessage("no-permission-command"));
                player.closeInventory();
                return;
            }

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta() != null) {
                String kitName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
                Kit kit = kitManager.getKit(kitName);

                if (kit != null) {
                    if (event.isLeftClick()) {
                        new KitSettingsEditorGUI(kitManager, kit).open(player);
                    } else if (event.isRightClick()) {
                        if (kitManager.deleteKit(kitName)) {
                            player.sendMessage(kitManager.getMessage("kit-deleted").replace("{kit_name}", kitName));
                            // Recalculate GUI size for KitListEditorGUI
                            int newSize = ((kitManager.getKits().size() / 9) + 1) * 9;
                            if (newSize == 0) newSize = 9;
                            if (newSize > 54) newSize = 54;

                            if(event.getInventory().getSize() != newSize) {
                                // If the number of rows changed, simply close and re-open to ensure correct size
                                player.closeInventory();
                                Bukkit.getScheduler().runTaskLater(kitManager.getPlugin(), () -> {
                                    if (player.isOnline()) new KitListEditorGUI(kitManager).open(player);
                                }, 1L);
                            } else {
                                // If size is the same, just refresh the content of the current open inventory
                                // This is more efficient than re-opening a whole new GUI
                                new KitListEditorGUI(kitManager).open(player); // Re-open (this handles populating correctly)
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Error al eliminar el kit '" + kitName + "'.");
                        }
                    }
                }
            }
            return;
        }

        if (title.startsWith(ChatColor.DARK_AQUA + "Editar Kit:")) {
            event.setCancelled(true);
            if (!player.hasPermission("core.kit.edit")) { // Changed permission prefix
                player.sendMessage(kitManager.getMessage("no-permission-command"));
                player.closeInventory();
                return;
            }

            String kitName = ChatColor.stripColor(title.replace(ChatColor.DARK_AQUA + "Editar Kit: ", ""));
            Kit kit = kitManager.getKit(kitName);
            if (kit == null) {
                player.sendMessage(kitManager.getMessage("kit-does-not-exist").replace("{kit_name}", kitName));
                player.closeInventory();
                return;
            }

            if (event.getSlot() == 4) {
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    openKitIconSelectionGUI(player, kit);
                    return;
                }
            }

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = ChatColor.stripColor(meta.getDisplayName());

                if (displayName.equals("Editar Contenido del Kit")) {
                    openKitContentsEditorGUI(player, kit);
                } else if (displayName.equals("Cambiar Slot en el Menú Principal")) {
                    KitCommand.playerEditingKitSlot.put(player.getUniqueId(), kitName);
                    player.sendMessage(ChatColor.YELLOW + "Ahora abre el menú principal de kits (/kit) y haz click en el slot deseado para '" + kit.getName() + "'.");
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(kitManager.getPlugin(), () -> {
                        if (player.isOnline()) {
                            new KitCommand(kitManager.getPlugin(), kitManager).openKitGUI(player);
                        }
                    }, 5L);
                } else if (displayName.equals("Volver")) {
                    new KitListEditorGUI(kitManager).open(player);
                } else if (displayName.equals("Cambiar Cooldown / Nombre")) {
                    KitSettingsEditorGUI.playersWaitingForCooldownInput.put(player.getUniqueId(), kitName);
                    player.sendMessage(ChatColor.YELLOW + "Escribe el nuevo cooldown para el kit '" + kit.getName() + "' en el chat (ej: 1h, 30m, 7d, 0).");
                    player.sendMessage(ChatColor.GRAY + "Para cambiar nombre: " + ChatColor.YELLOW + "/kit rename " + kit.getName() + " <nuevo_nombre>");
                    player.closeInventory();
                }
            }
            return;
        }

        if (title.startsWith(ChatColor.DARK_BLUE + "Editar Contenido del Kit:")) {
            event.setCancelled(false);

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            if (event.getSlot() == 53 && clickedItem.getType() == Material.EMERALD && clickedItem.hasItemMeta() &&
                    ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Guardar Contenido")) {

                event.setCancelled(true);
                String kitName = ChatColor.stripColor(title.replace(ChatColor.DARK_BLUE + "Editar Contenido del Kit: ", ""));
                Kit kit = kitManager.getKit(kitName);

                if (kit != null) {
                    ItemStack[] newContents = event.getInventory().getContents();
                    ItemStack[] filteredContents = Arrays.stream(newContents)
                            .filter(item -> item != null && item.getType() != Material.AIR &&
                                    !(item.getType() == Material.EMERALD && item.hasItemMeta() &&
                                            ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Guardar Contenido")))
                            .toArray(ItemStack[]::new);

                    kit.setContents(filteredContents);
                    kitManager.saveKit(kit);
                    player.sendMessage(ChatColor.GREEN + "Contenido del kit '" + kit.getName() + "' guardado.");
                    KitSettingsEditorGUI.playersEditingKitContents.remove(player.getUniqueId());
                    Bukkit.getScheduler().runTaskLater(kitManager.getPlugin(), () -> {
                        if (player.isOnline()) {
                            new KitSettingsEditorGUI(kitManager, kit).open(player);
                        }
                    }, 1L);
                } else {
                    player.sendMessage(ChatColor.RED + "Error: No se pudo encontrar el kit para guardar el contenido.");
                }
                return;
            }
            return;
        }

        if (title.equals(ChatColor.DARK_AQUA + "Configuración del Menú Principal")) {
            event.setCancelled(true);
            if (!player.hasPermission("core.kit.edit")) { // Changed permission prefix
                player.sendMessage(kitManager.getMessage("no-permission-command"));
                player.closeInventory();
                return;
            }

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = ChatColor.stripColor(meta.getDisplayName());

                if (displayName.equals("Cambiar Filas del Menú Principal")) {
                    int currentRows = kitManager.getMainGuiRows();
                    int newRows = currentRows % 6 + 1;
                    kitManager.setMainGuiRows(newRows);
                    player.sendMessage(ChatColor.GREEN + "Filas del menú principal actualizadas a: " + newRows);
                    new KitConfigEditorGUI(kitManager).open(player);
                } else if (displayName.equals("Cambiar Título del Menú Principal")) {
                    KitConfigEditorGUI.playersWaitingForTitleInput.put(player.getUniqueId(), true);
                    player.sendMessage(ChatColor.YELLOW + "Escribe el nuevo título para el menú principal en el chat.");
                    player.sendMessage(ChatColor.GRAY + "(Actual: " + kitManager.getGuiTitle() + ")");
                    player.closeInventory();
                } else if (displayName.equals("Volver")) {
                    new KitEditorMainGUI(kitManager).open(player);
                }
            }
            return;
        }

        if (title.equals(ChatColor.DARK_AQUA + "Seleccionar Icono del Kit")) {
            event.setCancelled(false); // Allow normal drag/drop/click in this GUI

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                // If they click on an empty spot in this GUI or player inventory, it's allowed but does nothing specific.
                return;
            }

            // Handle the "Guardar Icono" emerald button click
            if (event.getSlot() == 7 && clickedItem.getType() == Material.EMERALD &&
                    clickedItem.hasItemMeta() && ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Guardar Icono")) {

                event.setCancelled(true); // Cancel click on save button
                String kitName = playersSelectingIcon.get(player.getUniqueId());
                Kit kit = kitManager.getKit(kitName);

                if (kit != null) {
                    ItemStack iconInSlot = event.getInventory().getItem(4); // Get the item from the icon slot (slot 4)

                    if (iconInSlot != null && iconInSlot.getType() != Material.AIR) {
                        kit.setGuiIcon(iconInSlot.clone());
                        kitManager.saveKit(kit);
                        player.sendMessage(ChatColor.GREEN + "Icono del kit '" + kit.getName() + "' guardado.");
                    } else {
                        kit.setGuiIcon(null);
                        kitManager.saveKit(kit);
                        player.sendMessage(ChatColor.GREEN + "Icono del kit '" + kit.getName() + "' restablecido al predeterminado.");
                    }
                    playersSelectingIcon.remove(player.getUniqueId());
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(kitManager.getPlugin(), () -> {
                        if (player.isOnline()) {
                            new KitSettingsEditorGUI(kitManager, kit).open(player);
                        }
                    }, 1L);
                } else {
                    player.sendMessage(ChatColor.RED + "Error: No se pudo encontrar el kit para guardar el icono.");
                }
                return;
            }


            // Handle the "Volver" (Barrier) button click
            if (event.getSlot() == 8 && clickedItem.getType() == Material.BARRIER &&
                    clickedItem.hasItemMeta() && ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Volver")) {

                event.setCancelled(true);
                String kitName = playersSelectingIcon.remove(player.getUniqueId());
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(kitManager.getPlugin(), () -> {
                    if (player.isOnline()) {
                        Kit kit = kitManager.getKit(kitName);
                        if (kit != null) {
                            new KitSettingsEditorGUI(kitManager, kit).open(player);
                        } else {
                            player.sendMessage(ChatColor.RED + "Error: Kit no encontrado al volver del selector de iconos.");
                        }
                    }
                }, 1L);
                return;
            }
            return;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // --- NEW: Check if Kits module is enabled ---
        if (kitManager.getPlugin().getKitManager() == null) {
            event.setCancelled(true); // Always cancel if module is disabled
            return;
        }
        // --- END NEW CHECK ---

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals(ChatColor.DARK_AQUA + "Seleccionar Icono del Kit")) {
            int iconTargetSlot = 4;

            if (event.getRawSlots().contains(iconTargetSlot) && event.getInventorySlots().contains(iconTargetSlot)) {
                if (!player.hasPermission("core.kit.edit")) { // Changed permission prefix
                    event.setCancelled(true);
                    player.sendMessage(kitManager.getMessage("no-permission-command"));
                    return;
                }
                // Allow the drag to happen. Saving is done via the emerald button.
            }
        }
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // --- NEW: Check if Kits module is enabled ---
        if (kitManager.getPlugin().getKitManager() == null) {
            return;
        }
        // --- END NEW CHECK ---

        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String title = event.getView().getTitle();

        if (title.startsWith(ChatColor.DARK_BLUE + "Editar Contenido del Kit:") && KitSettingsEditorGUI.playersEditingKitContents.containsKey(playerUUID)) {
            KitSettingsEditorGUI.playersEditingKitContents.remove(playerUUID);
            Bukkit.getScheduler().runTaskLater(kitManager.getPlugin(), () -> {
                if (player.isOnline()) {
                    String kitName = ChatColor.stripColor(title.replace(ChatColor.DARK_BLUE + "Editar Contenido del Kit: ", ""));
                    Kit kit = kitManager.getKit(kitName);
                    if(kit != null) {
                        new KitSettingsEditorGUI(kitManager, kit).open(player);
                    } else {
                        player.sendMessage(ChatColor.RED + "El kit no se encontró después de cerrar el editor de contenido.");
                    }
                }
            }, 5L);
            return;
        }

        if (title.equals(ChatColor.DARK_AQUA + "Seleccionar Icono del Kit") && playersSelectingIcon.containsKey(playerUUID)) {
            String kitName = playersSelectingIcon.remove(playerUUID);
            Bukkit.getScheduler().runTaskLater(kitManager.getPlugin(), () -> {
                if (player.isOnline()) {
                    Kit kit = kitManager.getKit(kitName);
                    if (kit != null) {
                        new KitSettingsEditorGUI(kitManager, kit).open(player);
                    } else {
                        player.sendMessage(ChatColor.RED + "Error: Kit no encontrado al volver del selector de iconos.");
                    }
                }
            }, 1L);
            return;
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        // --- NEW: Check if Kits module is enabled ---
        if (kitManager.getPlugin().getKitManager() == null) {
            return;
        }
        // --- END NEW CHECK ---

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (KitSettingsEditorGUI.playersWaitingForCooldownInput.containsKey(playerUUID)) {
            event.setCancelled(true);
            String kitName = KitSettingsEditorGUI.playersWaitingForCooldownInput.remove(playerUUID);
            Kit kit = kitManager.getKit(kitName);

            if (kit != null) {
                String input = event.getMessage().toLowerCase();
                long newCooldownMillis = parseCooldownString(input);

                if (newCooldownMillis >= 0) {
                    kit.setCooldown(newCooldownMillis);
                    kitManager.saveKit(kit);
                    player.sendMessage(ChatColor.GREEN + "Cooldown del kit '" + kit.getName() + "' cambiado a " + formatTime(newCooldownMillis) + ".");
                } else {
                    player.sendMessage(ChatColor.RED + "Formato de cooldown inválido. Usa formatos como '1h', '30m', '7d' o '0' para sin cooldown.");
                }
                Bukkit.getScheduler().runTaskLater(kitManager.getPlugin(), () -> {
                    if (player.isOnline()) {
                        new KitSettingsEditorGUI(kitManager, kit).open(player);
                    }
                }, 1L);
            }
            return;
        }

        if (KitConfigEditorGUI.playersWaitingForTitleInput.containsKey(playerUUID)) {
            event.setCancelled(true);
            KitConfigEditorGUI.playersWaitingForTitleInput.remove(playerUUID);

            String newTitle = event.getMessage();
            kitManager.setMainGuiTitle(newTitle);
            player.sendMessage(ChatColor.GREEN + "Título del menú principal actualizado a: " + newTitle);

            Bukkit.getScheduler().runTaskLater(kitManager.getPlugin(), () -> {
                if (player.isOnline()) {
                    new KitConfigEditorGUI(kitManager).open(player);
                }
            }, 1L);
            return;
        }
    }

    private void handleSlotSelection(Player player, int clickedSlot) {
        String kitName = KitCommand.playerEditingKitSlot.remove(player.getUniqueId());
        Kit kit = kitManager.getKit(kitName);

        if (kit != null) {
            boolean slotTaken = kitManager.getKits().values().stream()
                    .anyMatch(k -> k.getGuiSlot() == clickedSlot && !k.getName().equalsIgnoreCase(kitName));

            if (slotTaken) {
                player.sendMessage(ChatColor.RED + "Ese slot ya está ocupado por otro kit. Por favor, selecciona uno vacío.");
                KitCommand.playerEditingKitSlot.put(player.getUniqueId(), kitName);
                return;
            }

            kit.setGuiSlot(clickedSlot);
            kitManager.saveKit(kit);
            player.sendMessage(ChatColor.GREEN + "Slot del kit '" + kit.getName() + "' actualizado a " + clickedSlot + ".");
            player.closeInventory();

            Bukkit.getScheduler().runTaskLater(kitManager.getPlugin(), () -> {
                if (player.isOnline()) {
                    new KitSettingsEditorGUI(kitManager, kit).open(player);
                }
            }, 5L);
        } else {
            player.sendMessage(ChatColor.RED + "Error: El kit para el que intentabas cambiar el slot no se encontró.");
        }
    }

    private void claimKit(Player player, Kit kit) {
        if (!kit.canClaim(player.getUniqueId())) {
            player.sendMessage(kitManager.getMessage("kit-on-cooldown")
                    .replace("{kit_name}", kit.getName())
                    .replace("{time}", formatTime(kit.getTimeLeft(player.getUniqueId()))));
            return;
        }

        giveKitContents(player, kit);
        kit.recordClaim(player.getUniqueId());
        kitManager.saveKit(kit);
        player.sendMessage(kitManager.getMessage("kit-claimed").replace("{kit_name}", kit.getName()));
    }

    private void giveKitContents(Player player, Kit kit) {
        ItemStack[] validContents = Arrays.stream(kit.getContents())
                .filter(item -> item != null && item.getType() != Material.AIR)
                .toArray(ItemStack[]::new);

        if (player.getInventory().getContents().length + validContents.length > player.getInventory().getSize()) {
            player.sendMessage(kitManager.getMessage("inventory-full"));
        }

        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(validContents);
        if (!remaining.isEmpty()) {
            for (ItemStack item : remaining.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
    }

    private void openKitContentsViewerGUI(Player player, Kit kit) {
        int size = ((kit.getContents().length / 9) + 1) * 9;
        if (size > 54) size = 54;
        if (size == 0) size = 9;

        Inventory contentsGUI = Bukkit.createInventory(null, size, ChatColor.DARK_BLUE + "Contenido del Kit: " + kit.getName());

        ItemStack[] filteredContents = Arrays.stream(kit.getContents())
                .filter(item -> item != null && item.getType() != Material.AIR)
                .toArray(ItemStack[]::new);
        contentsGUI.setContents(filteredContents);

        player.openInventory(contentsGUI);
    }

    private void openKitContentsEditorGUI(Player player, Kit kit) {
        int size = 54;
        Inventory contentsEditorGUI = Bukkit.createInventory(null, size, ChatColor.DARK_BLUE + "Editar Contenido del Kit: " + kit.getName());

        ItemStack[] filteredContents = Arrays.stream(kit.getContents())
                .filter(item -> item != null && item.getType() != Material.AIR)
                .toArray(ItemStack[]::new);

        for (int i = 0; i < filteredContents.length; i++) {
            if (i < contentsEditorGUI.getSize()) {
                contentsEditorGUI.setItem(i, filteredContents[i]);
            }
        }

        ItemStack saveButton = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = saveButton.getItemMeta();
        if (saveMeta != null) {
            saveMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Guardar Contenido");
            saveMeta.setLore(Arrays.asList(ChatColor.GRAY + "Haz click para guardar los cambios."));
            saveButton.setItemMeta(saveMeta);
        }
        contentsEditorGUI.setItem(53, saveButton);

        KitSettingsEditorGUI.playersEditingKitContents.put(player.getUniqueId(), kit.getName());

        player.openInventory(contentsEditorGUI);
    }

    private void openKitIconSelectionGUI(Player player, Kit kit) {
        Inventory iconSelectionGUI = Bukkit.createInventory(null, 9, ChatColor.DARK_AQUA + "Seleccionar Icono del Kit");

        ItemStack currentIcon = kit.getGuiIcon();
        if (currentIcon == null) {
            currentIcon = new ItemStack(Material.CHEST);
        }
        ItemMeta iconMeta = currentIcon.getItemMeta();
        if (iconMeta != null) {
            iconMeta.setDisplayName(ChatColor.YELLOW + "Arrastra aquí el nuevo Icono");
            iconMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "El icono actual es: " + currentIcon.getType().name()
            ));
            currentIcon.setItemMeta(iconMeta);
        }
        iconSelectionGUI.setItem(4, currentIcon);

        ItemStack saveButton = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = saveButton.getItemMeta();
        if (saveMeta != null) {
            saveMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Guardar Icono");
            saveMeta.setLore(Arrays.asList(ChatColor.GRAY + "Haz click para guardar el icono.", ChatColor.GRAY + "Deja el slot vacío para el icono por defecto."));
            saveButton.setItemMeta(saveMeta);
        }
        iconSelectionGUI.setItem(7, saveButton);

        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "Volver");
            backButton.setItemMeta(backMeta);
        }
        iconSelectionGUI.setItem(8, backButton);

        playersSelectingIcon.put(player.getUniqueId(), kit.getName());
        player.openInventory(iconSelectionGUI);
    }


    private String formatTime(long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long days = TimeUnit.MILLISECONDS.toDays(millis);

        if (days > 0) {
            return String.format("%d días, %d horas, %d minutos, %d segundos", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%d horas, %d minutos, %d segundos", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d minutos, %d segundos", minutes, seconds);
        } else {
            return String.format("%d segundos", seconds);
        }
    }

    private long parseCooldownString(String input) {
        Pattern pattern = Pattern.compile("(\\d+)([smhd])");
        Matcher matcher = pattern.matcher(input);

        if (input.equals("0")) {
            return 0;
        }

        if (matcher.matches()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "s": return TimeUnit.SECONDS.toMillis(value);
                case "m": return TimeUnit.MINUTES.toMillis(value);
                case "h": return TimeUnit.HOURS.toMillis(value);
                case "d": return TimeUnit.DAYS.toMillis(value);
                default: return -1;
            }
        }
        return -1;
    }
}