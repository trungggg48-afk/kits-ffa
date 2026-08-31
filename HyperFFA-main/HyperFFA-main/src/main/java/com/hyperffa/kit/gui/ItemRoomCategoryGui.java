package com.hyperffa.kit.gui;

import com.hyperffa.kit.config.ConfigManager;
import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.config.SoundManager;
import com.hyperffa.kit.gui.framework.BaseGui;
import com.hyperffa.kit.manager.EditSessionManager;
import com.hyperffa.kit.manager.KitManager;
import com.hyperffa.kit.manager.PvPModeManager;
import com.hyperffa.kit.model.ItemCategory;
import com.hyperffa.kit.model.PvPMode;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ItemRoomCategoryGui extends BaseGui {

    private final String modeId;
    private final PvPModeManager modeManager;
    private final KitManager kitManager;
    private final EditSessionManager editSessionManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;
    private final ConfigManager configManager;
    private final boolean inEditSession;
    private final int slotNumber;

    public ItemRoomCategoryGui(Player player, String modeId, PvPModeManager modeManager,
                               KitManager kitManager, EditSessionManager editSessionManager,
                               MessageManager messageManager, SoundManager soundManager,
                               ConfigManager configManager, boolean inEditSession, int slotNumber) {
        super(player, 27, messageManager.parse(
                configManager.getGuisConfig().getString("titles.item-room-categories", "<gradient:#06b6d4:#0891b2><bold>ITEM ROOM</bold></gradient> <dark_gray>»</dark_gray> <gray>Danh Mục</gray>")
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
        fillBorders(createItem(Material.CYAN_STAINED_GLASS_PANE, Component.empty()));

        PvPMode mode = modeManager.getMode(modeId);
        if (mode == null) return;

        ItemCategory[] categories = new ItemCategory[]{
                ItemCategory.GEAR,
                ItemCategory.POTIONS,
                ItemCategory.CONSUMABLES,
                ItemCategory.EXPLOSIONS,
                ItemCategory.MISCELLANEOUS
        };

        int[] guiSlots = new int[]{11, 12, 13, 14, 15};

        for (int i = 0; i < categories.length; i++) {
            ItemCategory cat = categories[i];
            int slot = guiSlots[i];
            int itemCount = mode.getCategoryItems(cat).size();

            ItemStack icon = createItem(cat.getIcon(),
                    messageManager.parse(cat.getGradientTitle()),
                    messageManager.parse("<gray>" + cat.getDisplayName() + "</gray>"),
                    messageManager.parse("<dark_gray>Số lượng: <yellow>" + itemCount + " vật phẩm</yellow></dark_gray>"),
                    Component.empty(),
                    messageManager.parse("<yellow>▶ Nhấn để mở danh mục</yellow>")
            );

            setItem(slot, icon, event -> {
                soundManager.playClickSuccess(player);
                new ItemRoomItemsGui(player, modeId, cat, 0, modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager, inEditSession, slotNumber).open();
            });
        }

        // Back Button (Slot 22)
        ItemStack backBtn = createItem(Material.ARROW,
                messageManager.parse(configManager.getGuisConfig().getString("items.button-back.name", "<red><bold>« QUAY LẠI</bold></red>")),
                messageManager.parse("<gray>Nhấn để quay về menu trước.</gray>")
        );
        setItem(22, backBtn, event -> {
            soundManager.playClickSuccess(player);
            if (inEditSession) {
                new EditControlsGui(player, modeId, slotNumber, modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager).open();
            } else {
                new KitRoomGui(player, modeId, modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager).open();
            }
        });
    }
}
