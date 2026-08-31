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

public class ItemRoomGui extends BaseGui {

    private final String modeId;
    private ItemCategory currentCategory;
    private final PvPModeManager modeManager;
    private final KitManager kitManager;
    private final EditSessionManager editSessionManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;
    private final ConfigManager configManager;

    public ItemRoomGui(Player player, String modeId, ItemCategory initialCategory,
                       PvPModeManager modeManager, KitManager kitManager,
                       EditSessionManager editSessionManager, MessageManager messageManager,
                       SoundManager soundManager, ConfigManager configManager) {
        super(player, 54, messageManager.parse(
                configManager.getGuisConfig().getString("titles.item-room", "ITEM ROOM")
        ));
        this.modeId = modeId;
        this.currentCategory = initialCategory != null ? initialCategory : ItemCategory.GEAR;
        this.modeManager = modeManager;
        this.kitManager = kitManager;
        this.editSessionManager = editSessionManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
        this.configManager = configManager;
    }

    private final java.util.Map<ItemCategory, java.util.Set<Integer>> takenSlots = new java.util.HashMap<>();

    @Override
    public void build() {
        // Clear buttons
        buttons.clear();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, null);
        }

        PvPMode mode = modeManager.getMode(modeId);
        if (mode == null) return;

        java.util.Set<Integer> takenInCurrent = takenSlots.getOrDefault(currentCategory, java.util.Collections.emptySet());

        // 1. Render Category Items (Rows 1-5, slots 0 to 44)
        List<ItemStack> items = mode.getCategoryItems(currentCategory);
        for (int i = 0; i < Math.min(45, items.size()); i++) {
            if (takenInCurrent.contains(i)) {
                continue; // Item has already been taken from Kit Room!
            }

            ItemStack item = items.get(i);
            if (item == null || item.getType().isAir()) continue;

            final int slotIndex = i;
            final ItemStack toGive = item.clone();
            setItem(slotIndex, toGive, event -> {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(toGive.clone());
                if (overflow.isEmpty()) {
                    // Item taken: Remove it from Item Room GUI
                    takenSlots.computeIfAbsent(currentCategory, k -> new java.util.HashSet<>()).add(slotIndex);
                    inventory.setItem(slotIndex, null);
                    buttons.remove(slotIndex);
                    soundManager.playClickSuccess(player);
                } else {
                    soundManager.playClickError(player);
                }
                player.updateInventory();
            });
        }

        // Tab 1: Refill (Slot 45, AMETHYST_SHARD) -> Làm đầy lại toàn bộ các ô đồ trong Item Room
        ItemStack refillTab = createItem(Material.AMETHYST_SHARD, messageManager.parse("<light_purple><bold>Refill</bold></light_purple>"),
                messageManager.parse("<gray>Nhấn để làm đầy lại toàn bộ vật phẩm trong kho đồ này.</gray>"));
        setItem(45, refillTab, event -> {
            soundManager.playKitLoad(player);
            takenSlots.clear();
            messageManager.sendMessage(player, "messages.item-room-refilled");
            build();
        });

        // Tab 2: Gear (Slot 47, NETHERITE_SWORD)
        ItemStack gearTab = createItem(Material.NETHERITE_SWORD, messageManager.parse("<aqua><bold>Gear</bold></aqua>"));
        setItem(47, gearTab, event -> switchCategory(ItemCategory.GEAR));

        // Tab 3: Potions (Slot 48, POTION)
        ItemStack potTab = createItem(Material.POTION, messageManager.parse("<aqua><bold>Potions</bold></aqua>"));
        setItem(48, potTab, event -> switchCategory(ItemCategory.POTIONS));

        // Tab 4: Consumables (Slot 49, TOTEM_OF_UNDYING)
        ItemStack conTab = createItem(Material.TOTEM_OF_UNDYING, messageManager.parse("<yellow><bold>Consumables</bold></yellow>"));
        setItem(49, conTab, event -> switchCategory(ItemCategory.CONSUMABLES));

        // Tab 5: Explosives (Slot 50, END_CRYSTAL)
        ItemStack expTab = createItem(Material.END_CRYSTAL, messageManager.parse("<light_purple><bold>Explosives</bold></light_purple>"));
        setItem(50, expTab, event -> switchCategory(ItemCategory.EXPLOSIONS));

        // Tab 6: Miscellaneous (Slot 51, PURPLE_SHULKER_BOX)
        ItemStack miscTab = createItem(Material.PURPLE_SHULKER_BOX, messageManager.parse("<dark_purple><bold>Miscellaneous</bold></dark_purple>"));
        setItem(51, miscTab, event -> switchCategory(ItemCategory.MISCELLANEOUS));

        // Back to Kit Menu (Slot 53, BARRIER)
        ItemStack backBtn = createItem(Material.BARRIER, messageManager.parse("<red><bold>Quay Lại / Lưu Kit</bold></red>"));
        setItem(53, backBtn, event -> {
            soundManager.playClickSuccess(player);
            new KitRoomGui(player, modeId, modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager).open();
        });
    }

    private void switchCategory(ItemCategory newCategory) {
        if (this.currentCategory == newCategory) return;
        this.currentCategory = newCategory;
        soundManager.playClickSuccess(player);
        build();
    }
}
