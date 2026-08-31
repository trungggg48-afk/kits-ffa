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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class KitRoomGui extends BaseGui {

    private final String modeId;
    private final PvPModeManager modeManager;
    private final KitManager kitManager;
    private final EditSessionManager editSessionManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;
    private final ConfigManager configManager;

    public KitRoomGui(Player player, String modeId, PvPModeManager modeManager, KitManager kitManager,
                      EditSessionManager editSessionManager, MessageManager messageManager,
                      SoundManager soundManager, ConfigManager configManager) {
        super(player, 54, messageManager.parse(
                configManager.getGuisConfig().getString("titles.kit-room", "KIT ROOM")
        ));
        this.modeId = modeId;
        this.modeManager = modeManager;
        this.kitManager = kitManager;
        this.editSessionManager = editSessionManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
        this.configManager = configManager;
    }

    @Override
    public void build() {
        // Clear inventory first
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, null);
        }

        // 1. Only outer perimeter border has Black/Gray Stained Glass Pane
        ItemStack borderPane = createItem(Material.BLACK_STAINED_GLASS_PANE, Component.empty());
        for (int col = 0; col < 9; col++) {
            setItem(col, borderPane, event -> event.setCancelled(true));       // Top row 0-8
            setItem(45 + col, borderPane, event -> event.setCancelled(true));  // Bottom row 45-53
        }
        for (int row = 1; row <= 4; row++) {
            setItem(row * 9, borderPane, event -> event.setCancelled(true));     // Left column 9, 18, 27, 36
            setItem(row * 9 + 8, borderPane, event -> event.setCancelled(true)); // Right column 17, 26, 35, 44
        }

        PvPMode mode = modeManager.getMode(modeId);
        if (mode == null) return;

        // 2. Default Slots (Slots 19, 20, 21 for Slot 1, Slot 2, Slot 3)
        int[] defaultSlotGuiIndices = new int[]{19, 20, 21};
        for (int i = 0; i < defaultSlotGuiIndices.length; i++) {
            final int slotNum = i + 1;
            int guiSlot = defaultSlotGuiIndices[i];
            boolean hasPerm = kitManager.hasSlotPermission(player, slotNum);

            ItemStack item;
            if (hasPerm) {
                item = new ItemStack(Material.CHEST);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(messageManager.parse("<green>Default slot #" + slotNum + "</green>"));
                    List<Component> lore = new ArrayList<>();
                    lore.add(messageManager.parse("<gold>L-CLICK:</gold> <white>LOAD KIT</white>"));
                    lore.add(messageManager.parse("<blue>R-CLICK:</blue> <white>EDIT KIT</white>"));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
                setItem(guiSlot, item, event -> handleSlotClick(event.getClick(), mode, slotNum));
            } else {
                item = new ItemStack(Material.GRAY_DYE);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(messageManager.parse("<green>Default slot #" + slotNum + "</green>"));
                    List<Component> lore = new ArrayList<>();
                    lore.add(messageManager.parse("<red>You don't have access to this kit!</red>"));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
                setItem(guiSlot, item, event -> {
                    messageManager.sendMessage(player, "messages.slot-locked");
                    soundManager.playClickError(player);
                });
            }
        }

        // 3. Premium Slots (Slots 23, 24, 25 for Slot 4, Slot 5, Slot 6 / Premium #1, #2, #3)
        int[] premiumSlotGuiIndices = new int[]{23, 24, 25};
        for (int i = 0; i < premiumSlotGuiIndices.length; i++) {
            final int slotNum = i + 4;
            final int premiumIndex = i + 1;
            int guiSlot = premiumSlotGuiIndices[i];
            boolean hasPerm = kitManager.hasSlotPermission(player, slotNum);

            ItemStack item;
            if (hasPerm) {
                item = new ItemStack(Material.CHEST);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(messageManager.parse("<yellow>Premium slot #" + premiumIndex + "</yellow>"));
                    List<Component> lore = new ArrayList<>();
                    lore.add(messageManager.parse("<gold>L-CLICK:</gold> <white>LOAD KIT</white>"));
                    lore.add(messageManager.parse("<blue>R-CLICK:</blue> <white>EDIT KIT</white>"));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
                setItem(guiSlot, item, event -> handleSlotClick(event.getClick(), mode, slotNum));
            } else {
                item = new ItemStack(Material.GRAY_DYE);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(messageManager.parse("<yellow>Premium slot #" + premiumIndex + "</yellow>"));
                    List<Component> lore = new ArrayList<>();
                    lore.add(messageManager.parse("<red>You don't have access to this kit!</red>"));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
                setItem(guiSlot, item, event -> {
                    messageManager.sendMessage(player, "messages.slot-locked");
                    soundManager.playClickError(player);
                });
            }
        }

        // 4. Tools Row shifted 1 slot to the left (Slots 39, 40, 41)
        // Clear Inventory (Slot 39, TNT)
        ItemStack clearBtn = createItem(Material.TNT, messageManager.parse("<red>Clear Inventory</red>"));
        setItem(39, clearBtn, event -> {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().setItemInOffHand(null);
            player.updateInventory();
            messageManager.sendMessage(player, "messages.kit-cleared");
            soundManager.playKitClear(player);
        });

        // Item Room (Slot 40, BARREL)
        ItemStack itemRoomBtn = createItem(Material.BARREL, messageManager.parse("<aqua>Item Room</aqua>"));
        setItem(40, itemRoomBtn, event -> {
            soundManager.playClickSuccess(player);
            new ItemRoomGui(player, modeId, ItemCategory.GEAR, modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager).open();
        });

        // Premade Kit (Slot 41, WRITABLE_BOOK) -> Directly apply Netherite template kit from screenshot
        ItemStack premadeBtn = createItem(Material.WRITABLE_BOOK, messageManager.parse("<green>Premade Kit</green>"));
        setItem(41, premadeBtn, event -> {
            player.closeInventory();
            com.hyperffa.kit.model.KitData netheriteKit = modeManager.createScreenshotPremadeKitData();
            netheriteKit.applyToPlayer(player);
            messageManager.sendMessage(player, "messages.kit-loaded-template");
            soundManager.playKitLoad(player);
        });
    }

    private void handleSlotClick(ClickType clickType, PvPMode mode, int slotNum) {
        if (clickType == ClickType.RIGHT) {
            // Right Click: Open Kit Editor Sub-Menu (Image 2)
            player.closeInventory();
            new KitEditorGui(player, mode.getId(), slotNum, modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager).open();
        } else {
            // Left Click: Load Kit
            player.closeInventory();
            kitManager.applyKitToPlayer(player, mode.getId(), slotNum);
        }
    }
}
