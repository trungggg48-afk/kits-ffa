package com.hyperffa.kit.manager;

import com.hyperffa.kit.api.event.PlayerKitEditEvent;
import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.config.SoundManager;
import com.hyperffa.kit.model.EditSession;
import com.hyperffa.kit.model.KitData;
import com.hyperffa.kit.model.PremadeKit;
import com.hyperffa.kit.model.PvPMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EditSessionManager {

    private final KitManager kitManager;
    private final PvPModeManager modeManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;

    private final Map<UUID, EditSession> activeSessions = new ConcurrentHashMap<>();

    public EditSessionManager(KitManager kitManager, PvPModeManager modeManager,
                              MessageManager messageManager, SoundManager soundManager) {
        this.kitManager = kitManager;
        this.modeManager = modeManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
    }

    public boolean isEditing(Player player) {
        return player != null && activeSessions.containsKey(player.getUniqueId());
    }

    public EditSession getSession(Player player) {
        return player != null ? activeSessions.get(player.getUniqueId()) : null;
    }

    public boolean startEditSession(Player player, String modeId, int slot) {
        if (isEditing(player)) {
            return false;
        }

        PvPMode mode = modeManager.getMode(modeId);
        if (mode == null) {
            messageManager.sendMessage(player, "messages.mode-not-found", Map.of("mode", modeId));
            soundManager.playClickError(player);
            return false;
        }

        if (!kitManager.hasSlotPermission(player, slot)) {
            messageManager.sendMessage(player, "messages.slot-locked");
            soundManager.playClickError(player);
            return false;
        }

        PlayerKitEditEvent event = new PlayerKitEditEvent(player, mode, slot, PlayerKitEditEvent.Action.START);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        EditSession session = new EditSession(player, mode.getId(), slot);
        activeSessions.put(player.getUniqueId(), session);

        // Load existing kit or fallback template into player inventory for editing
        KitData existing = kitManager.getCachedKit(player.getUniqueId(), mode.getId(), slot);
        if (existing != null && !existing.isEmpty()) {
            existing.applyToPlayer(player);
        } else {
            PremadeKit template = mode.getDefaultTemplate();
            if (template != null && template.getKitData() != null) {
                template.getKitData().applyToPlayer(player);
            } else {
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                player.getInventory().setItemInOffHand(null);
            }
        }

        soundManager.playOpenMenu(player);
        return true;
    }

    public void saveAndEndSession(Player player) {
        EditSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        PvPMode mode = modeManager.getMode(session.getMode());
        if (mode != null) {
            KitData currentKit = KitData.fromPlayer(player);
            kitManager.savePlayerKit(player, mode.getId(), session.getSlot(), currentKit);

            PlayerKitEditEvent event = new PlayerKitEditEvent(player, mode, session.getSlot(), PlayerKitEditEvent.Action.SAVE);
            Bukkit.getPluginManager().callEvent(event);

            String slotName = (session.getSlot() == 1) ? "Default Slot #1" : "Premium Slot #" + session.getSlot();
            messageManager.sendMessage(player, "messages.kit-saved", Map.of("slot_name", slotName, "mode", mode.getDisplayName()));
            soundManager.playKitSave(player);
        }

        // Restore real inventory
        session.restoreRealInventory(player);
    }

    public void cancelAndEndSession(Player player) {
        EditSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        PvPMode mode = modeManager.getMode(session.getMode());
        if (mode != null) {
            PlayerKitEditEvent event = new PlayerKitEditEvent(player, mode, session.getSlot(), PlayerKitEditEvent.Action.CANCEL);
            Bukkit.getPluginManager().callEvent(event);
        }

        // Restore real inventory safely
        session.restoreRealInventory(player);
        soundManager.playClickError(player);
    }

    public void endAllSessionsOnDisable() {
        for (EditSession session : activeSessions.values()) {
            Player player = Bukkit.getPlayer(session.getPlayerUuid());
            if (player != null && player.isOnline()) {
                session.restoreRealInventory(player);
            }
        }
        activeSessions.clear();
    }
}
