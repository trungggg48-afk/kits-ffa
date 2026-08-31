package com.hyperffa.kit.manager;

import com.hyperffa.kit.api.event.PlayerKitLoadEvent;
import com.hyperffa.kit.api.event.PlayerKitSaveEvent;
import com.hyperffa.kit.config.ConfigManager;
import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.config.SoundManager;
import com.hyperffa.kit.model.KitData;
import com.hyperffa.kit.model.PremadeKit;
import com.hyperffa.kit.model.PvPMode;
import com.hyperffa.kit.storage.SQLiteKitStorage;
import com.hyperffa.kit.validation.KitValidator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class KitManager {

    private final SQLiteKitStorage storage;
    private final PvPModeManager modeManager;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;

    // Cache: UUID -> Mode -> (Slot -> KitData)
    private final Map<UUID, Map<String, Map<Integer, KitData>>> kitCache = new ConcurrentHashMap<>();
    // Selected slot cache: UUID -> Mode -> Slot
    private final Map<UUID, Map<String, Integer>> selectedSlotCache = new ConcurrentHashMap<>();

    public KitManager(SQLiteKitStorage storage, PvPModeManager modeManager,
                      ConfigManager configManager, MessageManager messageManager, SoundManager soundManager) {
        this.storage = storage;
        this.modeManager = modeManager;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
    }

    public boolean hasSlotPermission(Player player, int slot) {
        int defaultSlots = configManager.getDefaultSlotsCount();
        if (slot <= defaultSlots) {
            return true; // Slots 1, 2, 3 are free for all default players
        }
        if (player.hasPermission("hyperkit.admin")
                || player.hasPermission("hyperkit.vip")
                || player.hasPermission("hyperkit.premium")
                || player.hasPermission("hyperkit.slot.*")) {
            return true;
        }
        for (int s = slot; s <= configManager.getMaxSlotsPerMode(); s++) {
            if (player.hasPermission("hyperkit.slot." + s)) {
                return true;
            }
        }
        return player.hasPermission("hyperkit.slot." + slot);
    }

    public CompletableFuture<Void> preloadPlayerData(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            for (PvPMode mode : modeManager.getAllModes()) {
                String modeId = mode.getId();
                Map<Integer, KitData> kits = storage.loadAllPlayerKits(uuid, modeId).join();
                kitCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(modeId, new ConcurrentHashMap<>(kits));

                int selected = storage.getSelectedSlot(uuid, modeId).join();
                selectedSlotCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(modeId, selected);
            }
        });
    }

    public void unloadPlayerData(UUID uuid) {
        kitCache.remove(uuid);
        selectedSlotCache.remove(uuid);
    }

    public KitData getCachedKit(UUID uuid, String mode, int slot) {
        Map<String, Map<Integer, KitData>> modeMap = kitCache.get(uuid);
        if (modeMap != null) {
            Map<Integer, KitData> slots = modeMap.get(mode.toLowerCase(Locale.ROOT));
            if (slots != null) {
                return slots.get(slot);
            }
        }
        return null;
    }

    public int getSelectedSlot(UUID uuid, String mode) {
        Map<String, Integer> modeMap = selectedSlotCache.get(uuid);
        if (modeMap != null) {
            Integer slot = modeMap.get(mode.toLowerCase(Locale.ROOT));
            if (slot != null) return slot;
        }
        return 1;
    }

    public void setSelectedSlot(Player player, String mode, int slot) {
        String modeId = mode.toLowerCase(Locale.ROOT);
        selectedSlotCache.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(modeId, slot);
        storage.setSelectedSlot(player.getUniqueId(), modeId, slot);
    }

    public boolean applyKitToPlayer(Player player, String modeId, int slot) {
        PvPMode mode = modeManager.getMode(modeId);
        if (mode == null) {
            messageManager.sendMessage(player, "messages.mode-not-found", Map.of("mode", modeId));
            soundManager.playClickError(player);
            return false;
        }

        if (!hasSlotPermission(player, slot)) {
            messageManager.sendMessage(player, "messages.slot-locked");
            soundManager.playClickError(player);
            return false;
        }

        KitData kd = getCachedKit(player.getUniqueId(), modeId, slot);

        if (kd == null || kd.isEmpty()) {
            messageManager.sendMessage(player, "messages.slot-empty", Map.of("slot", String.valueOf(slot)));
            soundManager.playClickError(player);
            return false;
        }

        // Trigger custom event
        PlayerKitLoadEvent event = new PlayerKitLoadEvent(player, mode, slot, kd, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        event.getKitData().applyToPlayer(player);
        setSelectedSlot(player, modeId, slot);

        String slotName = (slot <= 3) ? "Default Slot #" + slot : "Premium Slot #" + (slot - 3);
        messageManager.sendMessage(player, "messages.kit-loaded", Map.of("slot_name", slotName, "mode", mode.getDisplayName()));
        soundManager.playKitLoad(player);
        return true;
    }

    public CompletableFuture<Void> savePlayerKit(Player player, String modeId, int slot, KitData rawKitData) {
        PvPMode mode = modeManager.getMode(modeId);
        if (mode == null) return CompletableFuture.completedFuture(null);

        // Sanitize and validate
        KitData validated = KitValidator.validateAndSanitize(rawKitData, mode);

        // Trigger custom event
        PlayerKitSaveEvent event = new PlayerKitSaveEvent(player, mode, slot, validated);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return CompletableFuture.completedFuture(null);
        }

        KitData finalKit = event.getKitData();
        UUID uuid = player.getUniqueId();
        String mId = mode.getId();

        kitCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(mId, k -> new ConcurrentHashMap<>())
                .put(slot, finalKit);

        return storage.savePlayerKit(uuid, mId, slot, finalKit);
    }

    public SQLiteKitStorage getStorage() {
        return storage;
    }

    public KitData getPlayerKit(UUID uuid, String mode, int slot) {
        return getCachedKit(uuid, mode, slot);
    }
}
