package net.moonly.modules.TemporaryBlocks;

import net.moonly.Box;
import net.moonly.utils.LocationSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TemporaryBlockManager {

    private final Box plugin;
    private FileConfiguration config;
    private File configFile;
    private File dataFile;

    private boolean enabled;
    private int disappearanceDelaySeconds;
    private boolean applyToAllBlocks;
    private List<Material> specificBlocks;
    private boolean logRemovals;

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, TemporaryBlock>> activeTemporaryBlocks;

    public TemporaryBlockManager(Box plugin) {
        this.plugin = plugin;
        this.activeTemporaryBlocks = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "modules/temporaryblocks/temporary_blocks.json");

        if (!dataFile.getParentFile().exists()) {
            dataFile.getParentFile().mkdirs();
        }

        loadConfig();
        loadTemporaryBlocks();
        startDisappearanceTask();
    }

    public Box getPlugin() {
        return plugin;
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "modules/temporaryblocks/temporaryblocks.yml");
        if (!configFile.exists()) {
            plugin.saveResource("modules/temporaryblocks/temporaryblocks.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        this.enabled = config.getBoolean("settings.enabled", true);
        this.disappearanceDelaySeconds = config.getInt("settings.disappearance-delay-seconds", 300);
        this.applyToAllBlocks = config.getBoolean("settings.apply-to-all-blocks", false);

        this.specificBlocks = new ArrayList<>();
        List<String> materialNames = config.getStringList("settings.specific-blocks");
        for (String name : materialNames) {
            try {
                Material material = Material.valueOf(name.toUpperCase());
                this.specificBlocks.add(material);
            } catch (IllegalArgumentException e) {
                // Removed plugin.getLogger().warning()
            }
        }
        this.logRemovals = config.getBoolean("settings.log-removals", true); // Setting still exists but has no effect now

        // Removed plugin.getLogger().info()
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getDisappearanceDelaySeconds() {
        return disappearanceDelaySeconds;
    }

    public boolean isApplyToAllBlocks() {
        return applyToAllBlocks;
    }

    public List<Material> getSpecificBlocks() {
        return specificBlocks;
    }

    public boolean shouldLogRemovals() {
        return logRemovals;
    }

    private void loadTemporaryBlocks() {
        if (!dataFile.exists()) return;

        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(dataFile)) {
            JSONObject jsonObject = (JSONObject) parser.parse(reader);

            for (Object worldKey : jsonObject.keySet()) {
                String worldName = (String) worldKey;
                ConcurrentHashMap<String, TemporaryBlock> worldBlocks = new ConcurrentHashMap<>();
                JSONArray blocksArray = (JSONArray) jsonObject.get(worldName);

                for (Object obj : blocksArray) {
                    JSONObject blockJson = (JSONObject) obj;

                    Map<String, Object> locationMap = (Map<String, Object>) blockJson.get("location");
                    Location location = LocationSerializer.deserializeLocation(locationMap);

                    String materialName = (String) blockJson.get("originalMaterial");
                    Material originalMaterial = null;
                    if (materialName != null) {
                        try {
                            originalMaterial = Material.valueOf(materialName);
                        } catch (IllegalArgumentException e) {
                            // Removed plugin.getLogger().warning()
                        }
                    }

                    String placerUUIDString = (String) blockJson.get("placerUUID");
                    UUID placerUUID = null;
                    if (placerUUIDString != null) {
                        try {
                            placerUUID = UUID.fromString(placerUUIDString);
                        } catch (IllegalArgumentException e) {
                            // Removed plugin.getLogger().warning()
                        }
                    }

                    Long placedTimeMillis = (Long) blockJson.get("placedTimeMillis");


                    if (location != null && originalMaterial != null && placerUUID != null && placedTimeMillis != null) {
                        TemporaryBlock tempBlock = new TemporaryBlock(location, originalMaterial, placerUUID, placedTimeMillis);
                        worldBlocks.put(locationToKey(location), tempBlock);
                    } else {
                        // Removed plugin.getLogger().warning()
                    }
                }
                activeTemporaryBlocks.put(worldName, worldBlocks);
            }
            // Removed plugin.getLogger().info()

        } catch (IOException | ParseException | ClassCastException e) {
            // Removed plugin.getLogger().severe()
        }
    }

    public void saveTemporaryBlocks() {
        JSONObject mainJson = new JSONObject();

        for (Map.Entry<String, ConcurrentHashMap<String, TemporaryBlock>> worldEntry : activeTemporaryBlocks.entrySet()) {
            String worldName = worldEntry.getKey();
            JSONArray blocksArray = new JSONArray();

            for (TemporaryBlock tempBlock : worldEntry.getValue().values()) {
                JSONObject blockJson = new JSONObject();
                blockJson.put("location", LocationSerializer.serializeLocation(tempBlock.getLocation()));
                blockJson.put("originalMaterial", tempBlock.getOriginalMaterial().name());
                blockJson.put("placerUUID", tempBlock.getPlacerUUID().toString());
                blockJson.put("placedTimeMillis", tempBlock.getPlacedTimeMillis());
                blocksArray.add(blockJson);
            }
            mainJson.put(worldName, blocksArray);
        }

        try (FileWriter writer = new FileWriter(dataFile)) {
            writer.write(mainJson.toJSONString());
            writer.flush();
        } catch (IOException e) {
            // Removed plugin.getLogger().severe()
        }
    }

    public void addTemporaryBlock(Location location, Material originalMaterial, UUID placerUUID) {
        String worldName = location.getWorld().getName();
        String locKey = locationToKey(location);

        activeTemporaryBlocks.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>())
                .put(locKey, new TemporaryBlock(location, originalMaterial, placerUUID));
    }

    public void removeTemporaryBlock(Location location) {
        String worldName = location.getWorld().getName();
        String locKey = locationToKey(location);

        if (activeTemporaryBlocks.containsKey(worldName)) {
            activeTemporaryBlocks.get(worldName).remove(locKey);
        }
    }

    public TemporaryBlock getTemporaryBlock(Location location) {
        String worldName = location.getWorld().getName();
        String locKey = locationToKey(location);
        if (activeTemporaryBlocks.containsKey(worldName)) {
            return activeTemporaryBlocks.get(worldName).get(locKey);
        }
        return null;
    }

    private String locationToKey(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private void startDisappearanceTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!enabled) return;

                long currentTime = System.currentTimeMillis();
                long disappearanceThreshold = TimeUnit.SECONDS.toMillis(disappearanceDelaySeconds);

                List<TemporaryBlock> blocksToRemove = new ArrayList<>();

                activeTemporaryBlocks.forEach((worldName, worldBlocks) -> {
                    Iterator<Map.Entry<String, TemporaryBlock>> iterator = worldBlocks.entrySet().iterator();
                    while (iterator.hasNext()) {
                        TemporaryBlock tempBlock = iterator.next().getValue();
                        if (currentTime - tempBlock.getPlacedTimeMillis() >= disappearanceThreshold) {
                            blocksToRemove.add(tempBlock);
                            iterator.remove();
                        }
                    }
                });

                if (!blocksToRemove.isEmpty()) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (TemporaryBlock tempBlock : blocksToRemove) {
                                if (tempBlock.getLocation().getBlock().getType() != Material.AIR) {
                                    tempBlock.getLocation().getBlock().setType(Material.AIR);
                                    if (logRemovals) {
                                        // Removed plugin.getLogger().info()
                                    }
                                } else {
                                    if (logRemovals) {
                                        // Removed plugin.getLogger().info()
                                    }
                                }
                            }
                            saveTemporaryBlocks();
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L);
    }

    public void shutdown() {
        saveTemporaryBlocks();
        // Removed plugin.getLogger().info()
    }
}