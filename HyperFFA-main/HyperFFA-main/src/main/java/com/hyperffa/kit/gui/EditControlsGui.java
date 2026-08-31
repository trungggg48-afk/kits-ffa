package com.hyperffa.kit.gui;

import com.hyperffa.kit.config.ConfigManager;
import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.config.SoundManager;
import com.hyperffa.kit.gui.framework.BaseGui;
import com.hyperffa.kit.manager.EditSessionManager;
import com.hyperffa.kit.manager.KitManager;
import com.hyperffa.kit.manager.PvPModeManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class EditControlsGui extends BaseGui {

    private final String modeId;
    private final int slotNumber;
    private final PvPModeManager modeManager;
    private final KitManager kitManager;
    private final EditSessionManager editSessionManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;
    private final ConfigManager configManager;

    public EditControlsGui(Player player, String modeId, int slotNumber,
                           PvPModeManager modeManager, KitManager kitManager,
                           EditSessionManager editSessionManager, MessageManager messageManager,
                           SoundManager soundManager, ConfigManager configManager) {
        super(player, 27, messageManager.parse(
                configManager.getGuisConfig().getString("titles.edit-kit", "<gradient:#eab308:#ca8a04><bold>CHỈNH SỬA KIT</bold></gradient> <dark_gray>»</dark_gray> <gray><slot_name></gray>"),
                Map.of("slot_name", (slotNumber == 1) ? "Default Slot #1" : "Premium Slot #" + slotNumber)
        ));
        this.modeId = modeId;
        this.slotNumber = slotNumber;
        this.modeManager = modeManager;
        this.kitManager = kitManager;
        this.editSessionManager = editSessionManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
        this.configManager = configManager;
    }

    @Override
    public void build() {
        fillBorders(createItem(Material.GRAY_STAINED_GLASS_PANE, Component.empty()));

        // 1. Item Room (Slot 10)
        ItemStack itemRoomBtn = createItem(Material.CHEST_MINECART,
                messageManager.parse(configManager.getGuisConfig().getString("items.tool-item-room.name", "<gradient:#06b6d4:#0891b2><bold>KHO VẬT PHẨM (ITEM ROOM)</bold></gradient>")),
                messageManager.parse("<gray>Lấy các vật phẩm, giáp, thuốc để dựng kit.</gray>"),
                Component.empty(),
                messageManager.parse("<yellow>▶ Nhấn để mở kho</yellow>")
        );
        setItem(10, itemRoomBtn, event -> {
            soundManager.playClickSuccess(player);
            new ItemRoomGui(player, modeId, com.hyperffa.kit.model.ItemCategory.GEAR, modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager).open();
        });

        // 2. Premade Kits (Slot 12)
        ItemStack premadeBtn = createItem(Material.BOOKSHELF,
                messageManager.parse(configManager.getGuisConfig().getString("items.tool-premade-kits.name", "<gradient:#a855f7:#7c3aed><bold>BỘ TRANG BỊ MẪU (PREMADE)</bold></gradient>")),
                messageManager.parse("<gray>Nạp bố cục mẫu của server vào túi đồ.</gray>"),
                Component.empty(),
                messageManager.parse("<yellow>▶ Nhấn để chọn bộ mẫu</yellow>")
        );
        setItem(12, premadeBtn, event -> {
            soundManager.playClickSuccess(player);
            new PremadeKitGui(player, modeId, modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager, true, slotNumber).open();
        });

        // 3. Clear Inventory (Slot 14)
        ItemStack clearBtn = createItem(Material.LAVA_BUCKET,
                messageManager.parse(configManager.getGuisConfig().getString("items.tool-clear-inv.name", "<gradient:#ef4444:#b91c1c><bold>XÓA TẤT CẢ VẬT PHẨM</bold></gradient>")),
                messageManager.parse("<gray>Xóa sạch toàn bộ đồ trong túi đồ chỉnh sửa.</gray>"),
                Component.empty(),
                messageManager.parse("<red>▶ Nhấn để xóa sạch</red>")
        );
        setItem(14, clearBtn, event -> {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().setItemInOffHand(null);
            player.updateInventory();
            messageManager.sendMessage(player, "messages.kit-cleared");
            soundManager.playKitClear(player);
        });

        // 4. Save & Exit (Slot 16)
        ItemStack saveBtn = createItem(Material.EMERALD_BLOCK,
                messageManager.parse(configManager.getGuisConfig().getString("items.tool-save-exit.name", "<gradient:#22c55e:#15803d><bold>LƯU & THOÁT</bold></gradient>")),
                messageManager.parse("<gray>Lưu toàn bộ bố cục trang bị vào slot này.</gray>"),
                Component.empty(),
                messageManager.parse("<green>▶ Nhấn để hoàn tất</green>")
        );
        setItem(16, saveBtn, event -> {
            player.closeInventory();
            editSessionManager.saveAndEndSession(player);
        });

        // 5. Cancel (Slot 22)
        ItemStack cancelBtn = createItem(Material.REDSTONE_BLOCK,
                messageManager.parse("<red><bold>HỦY BỎ & THOÁT</bold></red>"),
                messageManager.parse("<gray>Thoát chế độ chỉnh sửa mà không lưu.</gray>")
        );
        setItem(22, cancelBtn, event -> {
            player.closeInventory();
            editSessionManager.cancelAndEndSession(player);
        });
    }
}
