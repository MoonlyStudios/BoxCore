// src/main/java/es/minespark/utils/LocationSerializer.java
package net.moonly.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

public class LocationSerializer {

    public static Map<String, Object> serializeLocation(Location location) {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("world", location.getWorld().getName());
        serialized.put("x", location.getX());
        serialized.put("y", location.getY());
        serialized.put("z", location.getZ());
        serialized.put("yaw", location.getYaw());
        serialized.put("pitch", location.getPitch());
        return serialized;
    }

    public static Location deserializeLocation(Map<String, Object> serialized) {
        if (serialized == null) {
            return null;
        }
        World world = Bukkit.getWorld((String) serialized.get("world"));
        if (world == null) {
            return null; // World not found
        }
        double x = (Double) serialized.get("x");
        double y = (Double) serialized.get("y");
        double z = (Double) serialized.get("z");
        float yaw = ((Double) serialized.get("yaw")).floatValue();
        float pitch = ((Double) serialized.get("pitch")).floatValue();
        return new Location(world, x, y, z, yaw, pitch);
    }
}