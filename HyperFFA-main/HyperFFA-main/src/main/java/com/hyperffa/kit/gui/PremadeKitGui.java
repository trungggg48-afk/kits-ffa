package com.hyperffa.kit.gui;

import com.hyperffa.kit.config.ConfigManager;
import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.config.SoundManager;
import com.hyperffa.kit.gui.framework.BaseGui;
import com.hyperffa.kit.manager.EditSessionManager;
import com.hyperffa.kit.manager.KitManager;
import com.hyperffa.kit.manager.PvPModeManager;
import com.hyperffa.kit.model.PremadeKit;
import com.hyperffa.kit.model.PvPMode;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PremadeKitGui extends BaseGui {

    private final String modeId;
    private final PvPModeManager modeManager;
    private final KitManager kitManager;
    private final EditSessionManager editSessionManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;
    private final ConfigManager configManager;
    private final boolean inEditSession;
    private final int slotNumber;

    public PremadeKitGui(Player player, String modeId, PvPModeManager modeManager,
                         KitManager kitManager, EditSessionManager editSessionManager,
                         MessageManager messageManager, SoundManager soundManager,
                         ConfigManager configManager, boolean inEditSession, int slotNumber) {
        super(player, 36, messageManager.parse(
                configManager.getGuisConfig().getString("titles.premade-kits", "<gradient:#a855f7:#7c3aed><bold>BỘ TRANG BỊ MẪU</bold></gradient> <dark_gray>»</dark_gray> <gray><mode></gray>"),
                Map.of("mode", modeManager.getMode(modeId) != null ? modeManager.getMode(modeId).getDisplayName() : modeId)
        ));
        this.modeId = modeId;
        this.modeManager = modeManager;
        this.kitManager = kitManager;
        this.editSessionManager = editSessionManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
        this.configManager = configManager;
        this.inEditSession = inEditSession;
        this.slotNumber = slotNumber;
    }

    @Override
    public void build() {
        fillBorders(createItem(Material.PURPLE_STAINED_GLASS_PANE, Component.empty()));

        PvPMode mode = modeManager.getMode(modeId);
        if (mode == null) return;

        int guiSlot = 10;
        for (PremadeKit premade : mode.getPremadeKits().values()) {
            if (guiSlot >= 26) break;
            if (guiSlot % 9 == 0) guiSlot++;
            if (guiSlot % 9 == 8) guiSlot += 2;

            ItemStack icon = premade.getIcon();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(messageManager.parse("<gradient:#d946ef:#a855f7><bold>" + premade.getName() + "</bold></gradient>"));
                List<Component> lore = new ArrayList<>();
                lore.add(messageManager.parse("<gray>Bộ trang bị mẫu thiết lập bởi Server.</gray>"));
                lore.add(Component.empty());
                lore.add(messageManager.parse("<yellow>▶ Nhấn để nạp vào túi đồ chỉnh sửa</yellow>"));
                meta.lore(lore);
                icon.setItemMeta(meta);
            }

            setItem(guiSlot, icon, event -> {
                if (inEditSession) {
                    if (premade.getKitData() != null) {
                        premade.getKitData().applyToPlayer(player);
                    }
                    messageManager.sendMessage(player, "messages.premade-applied", Map.of("premade_name", premade.getName()));
                    soundManager.playKitLoad(player);
                } else {
                    messageManager.sendMessage(player, "messages.action-blocked-in-editor");
                    soundManager.playClickError(player);
                }
            });

            guiSlot++;
        }

        // Back Button (Slot 31)
        ItemStack backBtn = createItem(Material.ARROW,
                messageManager.parse(configManager.getGuisConfig().getString("items.button-back.name", "<red><bold>« QUAY LẠI</bold></red>")),
                messageManager.parse("<gray>Nhấn để quay về menu trước.</gray>")
        );
        setItem(31, backBtn, event -> {
            soundManager.playClickSuccess(player);
            if (inEditSession) {
                new EditControlsGui(player, modeId, slotNumber, modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager).open();
            } else {
                new KitRoomGui(player, modeId, modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager).open();
            }
        });
    }
}
