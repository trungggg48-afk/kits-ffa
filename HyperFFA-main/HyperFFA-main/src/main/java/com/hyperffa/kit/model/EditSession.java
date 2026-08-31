package com.hyperffa.kit.model;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.UUID;

public class EditSession {

    private final UUID playerUuid;
    private final String mode;
    private final int slot;
    private final KitData originalInventorySnapshot;
    private final float originalExp;
    private final int originalLevel;
    private final GameMode originalGameMode;
    private final long startedAt;
    private boolean saved;

    public EditSession(Player player, String mode, int slot) {
        this.playerUuid = player.getUniqueId();
        this.mode = mode;
        this.slot = slot;
        this.originalInventorySnapshot = KitData.fromPlayer(player);
        this.originalExp = player.getExp();
        this.originalLevel = player.getLevel();
        this.originalGameMode = player.getGameMode();
        this.startedAt = System.currentTimeMillis();
        this.saved = false;
    }

    public void restoreRealInventory(Player player) {
        if (player == null || !player.isOnline()) return;
        originalInventorySnapshot.applyToPlayer(player);
        player.setExp(originalExp);
        player.setLevel(originalLevel);
        player.setGameMode(originalGameMode);
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getMode() {
        return mode;
    }

    public int getSlot() {
        return slot;
    }

    public KitData getOriginalInventorySnapshot() {
        return originalInventorySnapshot;
    }

    public float getOriginalExp() {
        return originalExp;
    }

    public int getOriginalLevel() {
        return originalLevel;
    }

    public GameMode getOriginalGameMode() {
        return originalGameMode;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }
}
