package net.moonly.modules.TemporaryBlocks;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.UUID;

public class TemporaryBlock {
    private Location location;
    private Material originalMaterial;
    private long placedTimeMillis;
    private UUID placerUUID;

    public TemporaryBlock(Location location, Material originalMaterial, UUID placerUUID) {
        this.location = location;
        this.originalMaterial = originalMaterial;
        this.placerUUID = placerUUID;
        this.placedTimeMillis = System.currentTimeMillis();
    }

    // Constructor for loading from storage
    public TemporaryBlock(Location location, Material originalMaterial, UUID placerUUID, long placedTimeMillis) {
        this.location = location;
        this.originalMaterial = originalMaterial;
        this.placerUUID = placerUUID;
        this.placedTimeMillis = placedTimeMillis;
    }

    public Location getLocation() {
        return location;
    }

    public Material getOriginalMaterial() {
        return originalMaterial;
    }

    public long getPlacedTimeMillis() {
        return placedTimeMillis;
    }

    public UUID getPlacerUUID() {
        return placerUUID;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setOriginalMaterial(Material originalMaterial) {
        this.originalMaterial = originalMaterial;
    }

    public void setPlacedTimeMillis(long placedTimeMillis) {
        this.placedTimeMillis = placedTimeMillis;
    }

    public void setPlacerUUID(UUID placerUUID) {
        this.placerUUID = placerUUID;
    }
}