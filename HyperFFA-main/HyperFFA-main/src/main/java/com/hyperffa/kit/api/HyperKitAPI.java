package com.hyperffa.kit.api;

import com.hyperffa.kit.gui.KitRoomGui;
import com.hyperffa.kit.manager.EditSessionManager;
import com.hyperffa.kit.manager.KitManager;
import com.hyperffa.kit.manager.PvPModeManager;
import com.hyperffa.kit.model.KitData;
import com.hyperffa.kit.model.PvPMode;
import com.hyperffa.kit.config.ConfigManager;
import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.config.SoundManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_EXPOSE_REP"})
public class HyperKitAPI {

    private static HyperKitAPI instance;

    private final PvPModeManager modeManager;
    private final KitManager kitManager;
    private final EditSessionManager editSessionManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;
    private final ConfigManager configManager;

    public HyperKitAPI(PvPModeManager modeManager, KitManager kitManager,
                       EditSessionManager editSessionManager, MessageManager messageManager,
                       SoundManager soundManager, ConfigManager configManager) {
        this.modeManager = modeManager;
        this.kitManager = kitManager;
        this.editSessionManager = editSessionManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
        this.configManager = configManager;
        instance = this;
    }

    public static HyperKitAPI getInstance() {
        return instance;
    }

    public boolean loadKit(Player player, String mode, int slot) {
        return kitManager.applyKitToPlayer(player, mode, slot);
    }

    public boolean loadSelectedKit(Player player, String mode) {
        int selectedSlot = kitManager.getSelectedSlot(player.getUniqueId(), mode);
        return kitManager.applyKitToPlayer(player, mode, selectedSlot);
    }

    public KitData getKitData(UUID uuid, String mode, int slot) {
        return kitManager.getCachedKit(uuid, mode, slot);
    }

    public int getSelectedSlot(UUID uuid, String mode) {
        return kitManager.getSelectedSlot(uuid, mode);
    }

    public void setSelectedSlot(Player player, String mode, int slot) {
        kitManager.setSelectedSlot(player, mode, slot);
    }

    public void openKitRoom(Player player, String mode) {
        PvPMode pvpMode = modeManager.getMode(mode);
        if (pvpMode == null) return;
        new KitRoomGui(player, pvpMode.getId(), modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager).open();
    }

    public PvPMode getMode(String modeId) {
        return modeManager.getMode(modeId);
    }

    public Collection<PvPMode> getAllModes() {
        return modeManager.getAllModes();
    }

    public boolean isEditing(Player player) {
        return editSessionManager.isEditing(player);
    }
}
