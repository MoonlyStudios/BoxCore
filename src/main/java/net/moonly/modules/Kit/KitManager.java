package net.moonly.modules.Kit;

import net.moonly.Box; // Changed import
import net.moonly.utils.ItemSerializer; // Changed import
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class KitManager {

    private final Box plugin; // Changed type
    private final Map<String, Kit> kits;
    private final File kitsFolder;
    private FileConfiguration kitConfig;
    private File kitConfigFile;

    private int mainGuiRows;
    private String mainGuiTitle;

    public KitManager(Box plugin) { // Changed type
        this.plugin = plugin;
        this.kits = new LinkedHashMap<>();
        this.kitsFolder = new File(plugin.getDataFolder(), "modules/kits");
        if (!kitsFolder.exists()) {
            kitsFolder.mkdirs();
        }
        loadKitConfig();
        loadKits();
    }

    public Box getPlugin() { // Changed return type
        return plugin;
    }

    private void loadKitConfig() {
        kitConfigFile = new File(plugin.getDataFolder(), "modules/kits/kit.yml"); // Adjusted path
        if (!kitConfigFile.exists()) {
            plugin.saveResource("modules/kits/kit.yml", false); // Save default kit.yml from resources
        }
        kitConfig = YamlConfiguration.loadConfiguration(kitConfigFile);
        
        this.mainGuiRows = kitConfig.getInt("gui-settings.main-gui-rows", 3);
        if (this.mainGuiRows < 1 || this.mainGuiRows > 6) {
            this.mainGuiRows = 3;
            plugin.getLogger().warning("Invalid main-gui-rows in kit.yml. Defaulting to 3.");
        }
        this.mainGuiTitle = kitConfig.getString("gui-settings.main-gui-title", "&8Available Kits");

        plugin.getLogger().info("Loaded kit.yml configuration.");
    }

    public void saveKitConfig() {
        kitConfig.set("gui-settings.main-gui-rows", this.mainGuiRows);
        kitConfig.set("gui-settings.main-gui-title", this.mainGuiTitle);
        try {
            kitConfig.save(kitConfigFile);
            plugin.getLogger().info("Saved kit.yml configuration.");
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving kit.yml: " + e.getMessage());
        }
    }

    public String getMessage(String path) {
        String message = kitConfig.getString("messages." + path, "&cMessage not found for path: " + path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getGuiTitle() {
        return ChatColor.translateAlternateColorCodes('&', mainGuiTitle);
    }

    public int getMainGuiRows() {
        return mainGuiRows;
    }

    public void setMainGuiRows(int mainGuiRows) {
        if (mainGuiRows >= 1 && mainGuiRows <= 6) {
            this.mainGuiRows = mainGuiRows;
            saveKitConfig();
        } else {
            plugin.getLogger().warning("Attempted to set invalid main GUI rows: " + mainGuiRows);
        }
    }

    public void setMainGuiTitle(String mainGuiTitle) {
        this.mainGuiTitle = mainGuiTitle;
        saveKitConfig();
    }

    public Map<String, Kit> getKits() {
        return kits.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(Kit::getGuiSlot)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }


    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public boolean createKit(String name, long cooldown, ItemStack[] contents) {
        if (kits.containsKey(name.toLowerCase())) {
            return false;
        }
        Kit newKit = new Kit(name, cooldown, contents);
        newKit.setGuiSlot(findNextAvailableGuiSlot());
        kits.put(name.toLowerCase(), newKit);
        saveKit(newKit);
        return true;
    }

    public boolean deleteKit(String name) {
        Kit kit = kits.remove(name.toLowerCase());
        if (kit != null) {
            File kitFile = new File(kitsFolder, name.toLowerCase() + ".yml");
            return kitFile.delete();
        }
        return false;
    }

    public void saveKit(Kit kit) {
        File kitFile = new File(kitsFolder, kit.getName().toLowerCase() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(kitFile);

        config.set("name", kit.getName());
        config.set("cooldown", kit.getCooldown());
        try {
            config.set("contents", ItemSerializer.itemStackArrayToBase64(kit.getContents()));
        } catch (IllegalStateException e) {
            plugin.getLogger().severe("Error saving contents for kit " + kit.getName() + ": " + e.getMessage());
            return;
        }

        config.set("gui-slot", kit.getGuiSlot());
        try {
            config.set("gui-icon", kit.getGuiIcon() != null ? ItemSerializer.itemStackToBase64(kit.getGuiIcon()) : null);
        } catch (IllegalStateException e) {
            plugin.getLogger().severe("Error saving GUI icon for kit " + kit.getName() + ": " + e.getMessage());
            return;
        }

        Map<String, Long> lastClaimedMapStrings = kit.getLastClaimedMap().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().toString(), Map.Entry::getValue));
        config.set("lastClaimed", lastClaimedMapStrings);

        try {
            config.save(kitFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving kit " + kit.getName() + ": " + e.getMessage());
        }
    }

    public void loadKits() {
        kits.clear();
        File[] kitFiles = kitsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (kitFiles == null) return;

        for (File file : kitFiles) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String name = config.getString("name");
            long cooldown = config.getLong("cooldown");
            String contentsBase64 = config.getString("contents");

            int guiSlot = config.getInt("gui-slot", -1);
            ItemStack guiIcon = null;
            String guiIconBase64 = config.getString("gui-icon");
            if (guiIconBase64 != null && !guiIconBase64.isEmpty()) {
                try {
                    guiIcon = ItemSerializer.itemStackFromBase64(guiIconBase64);
                } catch (IOException e) {
                    plugin.getLogger().warning("Error loading GUI icon for kit " + name + " from " + file.getName() + ": " + e.getMessage());
                }
            }


            if (name == null || contentsBase64 == null) {
                plugin.getLogger().warning("Skipping malformed kit file: " + file.getName());
                continue;
            }

            try {
                ItemStack[] contents = ItemSerializer.itemStackArrayFromBase64(contentsBase64);
                Kit kit = new Kit(name, cooldown, contents, guiSlot, guiIcon);

                if (config.isConfigurationSection("lastClaimed")) {
                    Map<UUID, Long> lastClaimedMap = new HashMap<>();
                    for (String uuidString : config.getConfigurationSection("lastClaimed").getKeys(false)) {
                        try {
                            UUID playerUUID = UUID.fromString(uuidString);
                            long timestamp = config.getLong("lastClaimed." + uuidString);
                            lastClaimedMap.put(playerUUID, timestamp);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid UUID in lastClaimed for kit " + name + ": " + uuidString);
                        }
                    }
                    kit.setLastClaimedMap(lastClaimedMap);
                }

                kits.put(name.toLowerCase(), kit);
            } catch (IOException e) {
                plugin.getLogger().severe("Error loading contents for kit " + name + " from " + file.getName() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + kits.size() + " kits.");
    }

    private int findNextAvailableGuiSlot() {
        Set<Integer> usedSlots = kits.values().stream()
                .filter(kit -> kit.getGuiSlot() != -1)
                .map(Kit::getGuiSlot)
                .collect(Collectors.toSet());

        for (int i = 0; i < getMainGuiRows() * 9; i++) {
            if (!usedSlots.contains(i)) {
                return i;
            }
        }
        return -1;
    }
}