package net.moonly.modules.Kit.editor;


import net.moonly.modules.Kit.Kit;
import net.moonly.modules.Kit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class KitSettingsEditorGUI {

    private final KitManager kitManager;
    private final Kit kit;

    public static final Map<UUID, String> playersEditingKitContents = new HashMap<>();
    public static final Map<UUID, String> playersWaitingForCooldownInput = new HashMap<>();


    public KitSettingsEditorGUI(KitManager kitManager, Kit kit) {
        this.kitManager = kitManager;
        this.kit = kit;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Editar Kit: " + kit.getName());

        ItemStack currentIcon = kit.getGuiIcon();
        if (currentIcon == null) {
            currentIcon = new ItemStack(Material.CHEST);
        }
        ItemMeta iconMeta = currentIcon.getItemMeta();
        if (iconMeta != null) {
            iconMeta.setDisplayName(ChatColor.YELLOW + "Icono del Kit");
            iconMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Haz click para abrir un selector",
                    ChatColor.GRAY + "y cambiar el icono del kit.",
                    ChatColor.GRAY + "(Actual: " + currentIcon.getType().name() + ")"
            ));
            currentIcon.setItemMeta(iconMeta);
        }
        gui.setItem(4, currentIcon);

        ItemStack contentsButton = new ItemStack(Material.BLAST_FURNACE);
        ItemMeta contentsMeta = contentsButton.getItemMeta();
        if (contentsMeta != null) {
            contentsMeta.setDisplayName(ChatColor.GREEN + "Editar Contenido del Kit");
            contentsMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Haz click para editar el contenido",
                    ChatColor.GRAY + "del kit en una nueva GUI.",
                    ChatColor.YELLOW + "Asegúrate de tener los ítems listos."
            ));
            contentsButton.setItemMeta(contentsMeta);
        }
        gui.setItem(11, contentsButton);

        ItemStack slotButton = new ItemStack(Material.STONE_BUTTON);
        ItemMeta slotMeta = slotButton.getItemMeta();
        if (slotMeta != null) {
            slotMeta.setDisplayName(ChatColor.AQUA + "Cambiar Slot en el Menú Principal");
            slotMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Haz click y luego clickea un",
                    ChatColor.GRAY + "slot vacío en el menú principal",
                    ChatColor.GRAY + "de kits para cambiar su posición.",
                    ChatColor.GRAY + "(Actual: " + (kit.getGuiSlot() == -1 ? "Auto" : kit.getGuiSlot()) + ")"
            ));
            slotButton.setItemMeta(slotMeta);
        }
        gui.setItem(13, slotButton);

        ItemStack cooldownNameButton = new ItemStack(Material.CLOCK);
        ItemMeta cnMeta = cooldownNameButton.getItemMeta();
        if (cnMeta != null) {
            cnMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Cambiar Cooldown / Nombre");
            cnMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click para cambiar cooldown (chat input).",
                    ChatColor.GRAY + "Para cambiar nombre: " + ChatColor.YELLOW + "/kit rename " + kit.getName() + " <nuevo_nombre>",
                    ChatColor.GRAY + "(Actual Cooldown: " + TimeUnit.MILLISECONDS.toSeconds(kit.getCooldown()) + "s)"
            ));
            cooldownNameButton.setItemMeta(cnMeta);
        }
        gui.setItem(15, cooldownNameButton);

        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "Volver");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(22, backButton);

        player.openInventory(gui);
    }
}