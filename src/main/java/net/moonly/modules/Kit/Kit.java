package net.moonly.modules.Kit;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class Kit {
    private String name;
    private long cooldown;
    private ItemStack[] contents;
    private Map<UUID, Long> lastClaimed;

    private int guiSlot;
    private ItemStack guiIcon;

    public Kit(String name, long cooldown, ItemStack[] contents) {
        this.name = name;
        this.cooldown = cooldown;
        this.contents = contents;
        this.lastClaimed = new HashMap<>();
        this.guiSlot = -1;
        this.guiIcon = null;
    }

    public Kit(String name, long cooldown, ItemStack[] contents, int guiSlot, ItemStack guiIcon) {
        this.name = name;
        this.cooldown = cooldown;
        this.contents = contents;
        this.lastClaimed = new HashMap<>();
        this.guiSlot = guiSlot;
        this.guiIcon = guiIcon;
    }

    public String getName() {
        return name;
    }

    public long getCooldown() {
        return cooldown;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public Map<UUID, Long> getLastClaimedMap() {
        return lastClaimed;
    }

    public void setContents(ItemStack[] contents) {
        this.contents = contents;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }

    public void setLastClaimedMap(Map<UUID, Long> lastClaimed) {
        this.lastClaimed = lastClaimed;
    }

    public int getGuiSlot() {
        return guiSlot;
    }

    public void setGuiSlot(int guiSlot) {
        this.guiSlot = guiSlot;
    }

    public ItemStack getGuiIcon() {
        return guiIcon;
    }

    public void setGuiIcon(ItemStack guiIcon) {
        this.guiIcon = guiIcon;
    }

    public boolean canClaim(UUID playerUUID) {
        if (!lastClaimed.containsKey(playerUUID)) {
            return true;
        }
        long lastTime = lastClaimed.get(playerUUID);
        return (System.currentTimeMillis() - lastTime) >= cooldown;
    }

    public long getTimeLeft(UUID playerUUID) {
        if (!lastClaimed.containsKey(playerUUID)) {
            return 0; // Can claim immediately
        }
        long lastTime = lastClaimed.get(playerUUID);
        long timeLeft = (lastTime + cooldown) - System.currentTimeMillis();
        return Math.max(0, timeLeft);
    }

    public void recordClaim(UUID playerUUID) {
        lastClaimed.put(playerUUID, System.currentTimeMillis());
    }
}