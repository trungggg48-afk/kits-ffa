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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemRoomItemsGui extends BaseGui {

    private final String modeId;
    private final ItemCategory category;
    private final int page;
    private final PvPModeManager modeManager;
    private final KitManager kitManager;
    private final EditSessionManager editSessionManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;
    private final ConfigManager configManager;
    private final boolean inEditSession;
    private final int slotNumber;

    public ItemRoomItemsGui(Player player, String modeId, ItemCategory category, int page,
                            PvPModeManager modeManager, KitManager kitManager,
                            EditSessionManager editSessionManager, MessageManager messageManager,
                            SoundManager soundManager, ConfigManager configManager,
                            boolean inEditSession, int slotNumber) {
        super(player, 54, messageManager.parse(
                configManager.getGuisConfig().getString("titles.item-room-items", "<gradient:#06b6d4:#0891b2><bold>ITEM ROOM</bold></gradient> <dark_gray>»</dark_gray> <aqua><category></aqua>"),
                Map.of("category", category.getDisplayName())
        ));
        this.modeId = modeId;
        this.category = category;
        this.page = page;
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
        // Fill bottom row as control border
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, Component.empty());
        for (int i = 45; i < 54; i++) {
            setItem(i, border);
        }

        PvPMode mode = modeManager.getMode(modeId);
        if (mode == null) return;

        List<ItemStack> items = mode.getCategoryItems(category);
        int pageSize = 45;
        int totalPages = (int) Math.ceil((double) items.size() / pageSize);
        if (totalPages == 0) totalPages = 1;

        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            ItemStack item = items.get(i);
            if (item == null || item.getType().isAir()) continue;

            final ItemStack toGive = item.clone();

            setItem(slot, toGive, event -> {
                if (inEditSession) {
                    HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(toGive.clone());
                    if (!overflow.isEmpty()) {
                        messageManager.sendMessage(player, "messages.item-limit-exceeded", Map.of("max", "Full Inventory", "item", toGive.getType().name()));
                        soundManager.playClickError(player);
                    } else {
                        soundManager.playClickSuccess(player);
                    }
                    player.updateInventory();
                } else {
                    messageManager.sendMessage(player, "messages.action-blocked-in-editor");
                    soundManager.playClickError(player);
                }
            });
        }

        // Previous Page Button (Slot 45)
        if (page > 0) {
            ItemStack prevBtn = createItem(Material.ARROW,
                    messageManager.parse(configManager.getGuisConfig().getString("items.button-previous-page.name", "<yellow><bold>« Trang Trước</bold></yellow>"))
            );
            setItem(45, prevBtn, event -> {
                soundManager.playClickSuccess(player);
                new ItemRoomItemsGui(player, modeId, category, page - 1, modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager, inEditSession, slotNumber).open();
            });
        }

        // Back to Categories (Slot 49)
        ItemStack backBtn = createItem(Material.BARRIER,
                messageManager.parse(configManager.getGuisConfig().getString("items.button-back.name", "<red><bold>« QUAY LẠI</bold></red>"))
        );
        setItem(49, backBtn, event -> {
            soundManager.playClickSuccess(player);
            new ItemRoomCategoryGui(player, modeId, modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager, inEditSession, slotNumber).open();
        });

        // Next Page Button (Slot 53)
        if (page + 1 < totalPages) {
            ItemStack nextBtn = createItem(Material.ARROW,
                    messageManager.parse(configManager.getGuisConfig().getString("items.button-next-page.name", "<yellow><bold>Trang Tiếp Theo »</bold></yellow>"))
            );
            setItem(53, nextBtn, event -> {
                soundManager.playClickSuccess(player);
                new ItemRoomItemsGui(player, modeId, category, page + 1, modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager, inEditSession, slotNumber).open();
            });
        }
    }
}
