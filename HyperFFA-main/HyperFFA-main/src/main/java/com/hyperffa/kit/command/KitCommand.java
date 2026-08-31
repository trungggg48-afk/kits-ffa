package com.hyperffa.kit.command;

import com.hyperffa.kit.config.ConfigManager;
import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.config.SoundManager;
import com.hyperffa.kit.gui.EditControlsGui;
import com.hyperffa.kit.gui.KitRoomGui;
import com.hyperffa.kit.manager.EditSessionManager;
import com.hyperffa.kit.manager.KitManager;
import com.hyperffa.kit.manager.PvPModeManager;
import com.hyperffa.kit.model.EditSession;
import com.hyperffa.kit.model.PvPMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class KitCommand implements CommandExecutor, TabCompleter {

    private final PvPModeManager modeManager;
    private final KitManager kitManager;
    private final EditSessionManager editSessionManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;
    private final ConfigManager configManager;

    public KitCommand(PvPModeManager modeManager, KitManager kitManager,
                      EditSessionManager editSessionManager, MessageManager messageManager,
                      SoundManager soundManager, ConfigManager configManager) {
        this.modeManager = modeManager;
        this.kitManager = kitManager;
        this.editSessionManager = editSessionManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "messages.player-only");
            return true;
        }

        // If player is in edit session, check for save/cancel/menu commands
        if (editSessionManager.isEditing(player)) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("save")) {
                    editSessionManager.saveAndEndSession(player);
                    return true;
                } else if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("quit")) {
                    editSessionManager.cancelAndEndSession(player);
                    return true;
                }
            }
            EditSession session = editSessionManager.getSession(player);
            if (session != null) {
                new EditControlsGui(player, session.getMode(), session.getSlot(), modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager).open();
            }
            return true;
        }

        String defaultMode = configManager.getDefaultMode();

        // Check if arg0 is "import" -> /kit import <code> [slot]
        if (args.length > 0 && args[0].equalsIgnoreCase("import") && args.length >= 2) {
            String code = args[1];
            int targetSlot = 1;
            if (args.length >= 3) {
                String slotStr = args[2].toLowerCase(Locale.ROOT).replace("kit", "");
                if (isInteger(slotStr)) {
                    targetSlot = Integer.parseInt(slotStr);
                }
            }
            final int finalSlot = targetSlot;
            kitManager.getStorage().loadSharedCode(code).thenAccept(kd -> {
                if (kd == null || kd.isEmpty()) {
                    player.sendMessage(messageManager.parse("<red>Không tìm thấy mã chia sẻ kit: " + code + "</red>"));
                    soundManager.playClickError(player);
                    return;
                }
                kitManager.savePlayerKit(player, defaultMode, finalSlot, kd).thenRun(() -> {
                    player.sendMessage(messageManager.parse("<green>Đã import thành công bộ kit từ mã <yellow>" + code + "</yellow> vào <gold>Slot #" + finalSlot + "</gold>!</green>"));
                    soundManager.playKitLoad(player);
                });
            });
            return true;
        }

        if (args.length == 0) {
            // /kit -> Open KitRoom for default mode
            openKitRoom(player, defaultMode);
            return true;
        }

        // Check if arg0 is an integer (e.g. /kit 1 or /kit 2)
        if (isInteger(args[0])) {
            int slot = Integer.parseInt(args[0]);
            kitManager.applyKitToPlayer(player, defaultMode, slot);
            return true;
        }

        // Check if arg0 is a shorthand kitX (e.g. /kit kit1 or /kit kit2)
        if (args[0].toLowerCase(Locale.ROOT).startsWith("kit") && isInteger(args[0].substring(3))) {
            int slot = Integer.parseInt(args[0].substring(3));
            kitManager.applyKitToPlayer(player, defaultMode, slot);
            return true;
        }

        // Check if arg0 is a mode name (e.g. /kit sword)
        PvPMode mode = modeManager.getMode(args[0]);
        if (mode != null) {
            if (args.length == 1) {
                // /kit sword -> Open KitRoom for sword
                openKitRoom(player, mode.getId());
                return true;
            } else if (isInteger(args[1])) {
                // /kit sword 1 -> Directly load slot 1 for sword
                int slot = Integer.parseInt(args[1]);
                kitManager.applyKitToPlayer(player, mode.getId(), slot);
                return true;
            }
        }

        messageManager.sendMessage(player, "messages.mode-not-found", Map.of("mode", args[0]));
        soundManager.playClickError(player);
        return true;
    }

    private void openKitRoom(Player player, String modeId) {
        PvPMode mode = modeManager.getMode(modeId);
        if (mode == null) {
            mode = modeManager.getAllModes().stream().findFirst().orElse(null);
            if (mode == null) {
                messageManager.sendMessage(player, "messages.mode-not-found", Map.of("mode", modeId));
                return;
            }
        }
        new KitRoomGui(player, mode.getId(), modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager).open();
        soundManager.playOpenMenu(player);
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (editSessionManager.isEditing(player)) {
            return filter(List.of("save", "cancel", "menu"), args[0]);
        }

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("import");
            for (PvPMode mode : modeManager.getAllModes()) {
                suggestions.add(mode.getId());
            }
            int maxSlots = configManager.getMaxSlotsPerMode();
            for (int i = 1; i <= maxSlots; i++) {
                suggestions.add(String.valueOf(i));
                suggestions.add("kit" + i);
            }
            return filter(suggestions, args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("import")) {
                return List.of("<mã_code>");
            }
            if (modeManager.getMode(args[0]) != null) {
                List<String> slots = new ArrayList<>();
                int maxSlots = configManager.getMaxSlotsPerMode();
                for (int i = 1; i <= maxSlots; i++) {
                    slots.add(String.valueOf(i));
                }
                return filter(slots, args[1]);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("import")) {
            List<String> slots = new ArrayList<>();
            int maxSlots = configManager.getMaxSlotsPerMode();
            for (int i = 1; i <= maxSlots; i++) {
                slots.add("kit" + i);
            }
            return filter(slots, args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(s);
            }
        }
        return result;
    }
}
