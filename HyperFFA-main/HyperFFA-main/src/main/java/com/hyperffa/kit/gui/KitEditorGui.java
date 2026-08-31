package com.hyperffa.kit.gui;

import com.hyperffa.kit.config.ConfigManager;
import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.config.SoundManager;
import com.hyperffa.kit.gui.framework.BaseGui;
import com.hyperffa.kit.manager.EditSessionManager;
import com.hyperffa.kit.manager.KitManager;
import com.hyperffa.kit.manager.PvPModeManager;
import com.hyperffa.kit.model.KitData;
import com.hyperffa.kit.model.PremadeKit;
import com.hyperffa.kit.model.PvPMode;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Random;

public class KitEditorGui extends BaseGui {

    private final String modeId;
    private final int slotNumber;
    private final KitManager kitManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;

    private ItemStack[] currentContents;
    private ItemStack[] currentArmor;
    private ItemStack currentOffhand;

    public KitEditorGui(Player player, String modeId, int slotNumber,
                        PvPModeManager modeManager, KitManager kitManager,
                        EditSessionManager editSessionManager, MessageManager messageManager,
                        SoundManager soundManager, ConfigManager configManager) {
        super(player, 54, messageManager.parse(
                configManager.getGuisConfig().getString("titles.kit-editor", "KIT EDITOR")
        ));
        this.modeId = modeId;
        this.slotNumber = slotNumber;
        this.kitManager = kitManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;

        loadInitialKitData();
    }

    private void loadInitialKitData() {
        KitData kd = kitManager.getPlayerKit(player.getUniqueId(), modeId, slotNumber);
        if (kd != null && !kd.isEmpty()) {
            this.currentContents = kd.getContents() != null ? kd.getContents().clone() : new ItemStack[36];
            this.currentArmor = kd.getArmor() != null ? kd.getArmor().clone() : new ItemStack[4];
            this.currentOffhand = kd.getOffHand() != null ? kd.getOffHand().clone() : null;
        } else {
            // Kit 1, 2, 3... start completely EMPTY by default!
            this.currentContents = new ItemStack[36];
            this.currentArmor = new ItemStack[4];
            this.currentOffhand = null;
        }
    }

    @Override
    public void build() {
        buttons.clear();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, null);
        }

        // 1. Row 0: Armor & Offhand (Slots 0, 1, 2, 3, 4)
        if (currentArmor != null && currentArmor.length >= 4) {
            // KitData armor order: 0=Boots, 1=Leggings, 2=Chestplate, 3=Helmet
            setItem(0, currentArmor[3] != null ? currentArmor[3].clone() : null, null); // Helmet
            setItem(1, currentArmor[2] != null ? currentArmor[2].clone() : null, null); // Chestplate
            setItem(2, currentArmor[1] != null ? currentArmor[1].clone() : null, null); // Leggings
            setItem(3, currentArmor[0] != null ? currentArmor[0].clone() : null, null); // Boots
        }
        if (currentOffhand != null) {
            setItem(4, currentOffhand.clone(), null); // Offhand
        }

        // 2. Rows 1, 2, 3 (Slots 9-35 = 27 slots): Main Storage Items (indices 9-35)
        if (currentContents != null) {
            for (int i = 9; i < Math.min(36, currentContents.length); i++) {
                ItemStack item = currentContents[i];
                if (item != null && !item.getType().isAir()) {
                    setItem(i, item.clone(), null);
                }
            }

            // 3. Row 4 (Slots 36-44 = 9 slots): Hotbar Items (indices 0-8)
            for (int i = 0; i < Math.min(9, currentContents.length); i++) {
                ItemStack item = currentContents[i];
                if (item != null && !item.getType().isAir()) {
                    setItem(36 + i, item.clone(), null);
                }
            }
        }

        // 4. Row 5 (Bottom control bar, Slots 45-53)
        ItemStack borderPane = createItem(Material.BLACK_STAINED_GLASS_PANE, Component.empty());
        int[] emptyBottomSlots = new int[]{45, 47, 49, 51, 53};
        for (int slot : emptyBottomSlots) {
            setItem(slot, borderPane, event -> event.setCancelled(true));
        }

        // A. LIME_DYE (Slot 46) -> Save Kit
        ItemStack saveBtn = createItem(Material.LIME_DYE,
                messageManager.parse("<green><bold>LƯU BỐ CỤC KIT (SAVE)</bold></green>"),
                messageManager.parse("<gray>Lưu toàn bộ trang bị này vào slot #" + slotNumber + "</gray>")
        );
        setItem(46, saveBtn, event -> saveKit());

        // B. ARMOR_STAND (Slot 48) -> Import from Player's inventory & armor
        ItemStack importBtn = createItem(Material.ARMOR_STAND,
                messageManager.parse("<aqua><bold>IMPORT TỪ TÚI ĐỒ (IMPORT)</bold></aqua>"),
                messageManager.parse("<gray>Nạp toàn bộ đồ trên người bạn vào kit này.</gray>")
        );
        setItem(48, importBtn, event -> importFromPlayerInventory());

        // C. ANVIL (Slot 50) -> Reset Kit
        ItemStack anvilResetBtn = createItem(Material.ANVIL,
                messageManager.parse("<red><bold>RESET KIT</bold></red>"),
                messageManager.parse("<gray>Xóa sạch toàn bộ đồ trong kit slot này.</gray>")
        );
        setItem(50, anvilResetBtn, event -> resetKit());

        // D. NAME_TAG (Slot 52) -> Generate Share Code
        ItemStack shareCodeBtn = createItem(Material.NAME_TAG,
                messageManager.parse("<gold><bold>TẠO MÃ CHIA SẺ (SHARE CODE)</bold></gold>"),
                messageManager.parse("<gray>Tạo mã để người khác có thể import kit của bạn.</gray>")
        );
        setItem(52, shareCodeBtn, event -> generateShareCode());
    }

    private void importFromPlayerInventory() {
        KitData fromInv = KitData.fromPlayer(player);
        this.currentContents = fromInv.getContents();
        this.currentArmor = fromInv.getArmor();
        this.currentOffhand = fromInv.getOffHand();

        soundManager.playClickSuccess(player);
        messageManager.sendMessage(player, "messages.kit-imported-from-inv");
        build();
    }

    private void resetKit() {
        this.currentContents = new ItemStack[36];
        this.currentArmor = new ItemStack[4];
        this.currentOffhand = null;

        soundManager.playKitClear(player);
        messageManager.sendMessage(player, "messages.kit-cleared");
        build();
    }

    private void saveKit() {
        KitData toSave = new KitData(currentContents, currentArmor, currentOffhand);
        kitManager.savePlayerKit(player, modeId, slotNumber, toSave);
        soundManager.playKitSave(player);
        messageManager.sendMessage(player, "messages.kit-saved", Map.of("slot_name", "Slot #" + slotNumber));
        player.closeInventory();
    }

    private void generateShareCode() {
        // Generate random 8-character code (e.g. K7X9-2AB4)
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append("-");
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String code = sb.toString();

        KitData toSave = new KitData(currentContents, currentArmor, currentOffhand);
        kitManager.getStorage().saveSharedCode(code, modeId, toSave, player.getUniqueId()).thenRun(() -> {
            player.sendMessage(messageManager.parse("<gradient:#3b82f6:#60a5fa><bold>── MÃ CHIA SẺ KIT ──</bold></gradient>"));
            player.sendMessage(messageManager.parse("<green>Mã chia sẻ của bạn: </green><yellow><bold>" + code + "</bold></yellow>"));
            player.sendMessage(messageManager.parse("<gray>Người khác dùng lệnh:</gray> <aqua>/kit import " + code + " kit1</aqua> <gray>để nạp!</gray>"));
            soundManager.playClickSuccess(player);
        });
    }
}
