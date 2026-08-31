package com.hyperffa.kit.command;

import com.hyperffa.kit.config.ConfigManager;
import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.config.SoundManager;
import com.hyperffa.kit.manager.KitManager;
import com.hyperffa.kit.manager.PvPModeManager;
import com.hyperffa.kit.model.ItemCategory;
import com.hyperffa.kit.model.KitData;
import com.hyperffa.kit.model.PremadeKit;
import com.hyperffa.kit.model.PvPMode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressFBWarnings({"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
public class KitAdminCommand implements CommandExecutor, TabCompleter {

    private final PvPModeManager modeManager;
    private final KitManager kitManager;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;

    public KitAdminCommand(PvPModeManager modeManager, KitManager kitManager,
                           ConfigManager configManager, MessageManager messageManager,
                           SoundManager soundManager) {
        this.modeManager = modeManager;
        this.kitManager = kitManager;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("hyperkit.admin")) {
            messageManager.sendMessage(sender, "messages.no-permission");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "reload" -> {
                configManager.reload();
                messageManager.reload();
                modeManager.loadModes();
                messageManager.sendMessage(sender, "admin.reloaded");
                if (sender instanceof Player p) soundManager.playClickSuccess(p);
                return true;
            }

            case "mode" -> {
                if (args.length < 3) {
                    sendUsage(sender);
                    return true;
                }
                String action = args[1].toLowerCase(Locale.ROOT);
                String modeName = args[2].toLowerCase(Locale.ROOT);

                if (action.equals("create")) {
                    if (modeManager.getMode(modeName) != null) {
                        messageManager.sendMessage(sender, "admin.mode-already-exists", Map.of("mode", modeName));
                        return true;
                    }
                    PvPMode newMode = new PvPMode(modeName, modeName.substring(0, 1).toUpperCase(Locale.ROOT) + modeName.substring(1));
                    modeManager.registerMode(newMode);
                    messageManager.sendMessage(sender, "admin.mode-created", Map.of("mode", modeName));
                    if (sender instanceof Player p) soundManager.playClickSuccess(p);
                    return true;
                } else if (action.equals("delete")) {
                    if (modeManager.deleteMode(modeName)) {
                        messageManager.sendMessage(sender, "admin.mode-deleted", Map.of("mode", modeName));
                        if (sender instanceof Player p) soundManager.playClickSuccess(p);
                    } else {
                        messageManager.sendMessage(sender, "messages.mode-not-found", Map.of("mode", modeName));
                    }
                    return true;
                }
            }

            case "setpremade" -> {
                if (!(sender instanceof Player player)) {
                    messageManager.sendMessage(sender, "messages.player-only");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(messageManager.parse("<yellow>Cách dùng: /kitadmin setpremade <mode> <tên_kit></yellow>"));
                    return true;
                }
                String modeId = args[1].toLowerCase(Locale.ROOT);
                String kitName = args[2];

                PvPMode mode = modeManager.getMode(modeId);
                if (mode == null) {
                    messageManager.sendMessage(player, "messages.mode-not-found", Map.of("mode", modeId));
                    return true;
                }

                KitData kd = KitData.fromPlayer(player);
                ItemStack icon = player.getInventory().getItemInMainHand();
                if (icon.getType().isAir()) {
                    icon = new ItemStack(Material.CHEST);
                }
                PremadeKit premade = new PremadeKit(kitName, kd, icon);
                mode.addPremadeKit(premade);
                modeManager.saveModes();

                messageManager.sendMessage(player, "admin.premade-saved", Map.of("premade_name", kitName, "mode", mode.getDisplayName()));
                soundManager.playKitSave(player);
                return true;
            }

            case "setcategory" -> {
                if (!(sender instanceof Player player)) {
                    messageManager.sendMessage(sender, "messages.player-only");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(messageManager.parse("<yellow>Cách dùng: /kitadmin setcategory <mode> <gear|potions|consumables|blocks|refill|items></yellow>"));
                    return true;
                }
                String modeId = args[1].toLowerCase(Locale.ROOT);
                String catId = args[2].toLowerCase(Locale.ROOT);

                PvPMode mode = modeManager.getMode(modeId);
                if (mode == null) {
                    messageManager.sendMessage(player, "messages.mode-not-found", Map.of("mode", modeId));
                    return true;
                }

                ItemCategory category = ItemCategory.fromId(catId);
                if (category == null) {
                    player.sendMessage(messageManager.parse("<red>Danh mục không hợp lệ! Chọn: gear, potions, consumables, blocks, refill, items.</red>"));
                    return true;
                }

                List<ItemStack> items = new ArrayList<>();
                ItemStack[] contents = player.getInventory().getContents();
                for (ItemStack item : contents) {
                    if (item != null && !item.getType().isAir()) {
                        items.add(item.clone());
                        // Automatically record item limits based on the stack count in Admin's inventory
                        mode.setItemLimit(item.getType(), item.getAmount());
                    }
                }

                mode.setCategoryItems(category, items);
                modeManager.saveModes();

                messageManager.sendMessage(player, "admin.category-saved", Map.of("category", category.getDisplayName(), "mode", mode.getDisplayName()));
                soundManager.playKitSave(player);
                return true;
            }

            case "give" -> {
                if (args.length < 4) {
                    sender.sendMessage(messageManager.parse("<yellow>Cách dùng: /kitadmin give <player> <mode> <slot></yellow>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    messageManager.sendMessage(sender, "admin.target-not-found");
                    return true;
                }
                String modeId = args[2].toLowerCase(Locale.ROOT);
                int slot;
                try {
                    slot = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(messageManager.parse("<red>Slot phải là số nguyên dương!</red>"));
                    return true;
                }

                if (kitManager.applyKitToPlayer(target, modeId, slot)) {
                    messageManager.sendMessage(sender, "admin.give-success", Map.of("slot", String.valueOf(slot), "mode", modeId, "player", target.getName()));
                }
                return true;
            }

            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        messageManager.sendMessage(sender, "admin.usage");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("hyperkit.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filter(List.of("mode", "setpremade", "setcategory", "give", "reload"), args[0]);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "mode" -> {
                    return filter(List.of("create", "delete"), args[1]);
                }
                case "setpremade", "setcategory" -> {
                    List<String> modes = new ArrayList<>();
                    for (PvPMode mode : modeManager.getAllModes()) {
                        modes.add(mode.getId());
                    }
                    return filter(modes, args[1]);
                }
                case "give" -> {
                    List<String> players = new ArrayList<>();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        players.add(p.getName());
                    }
                    return filter(players, args[1]);
                }
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "mode" -> {
                    if (args[1].equalsIgnoreCase("delete")) {
                        List<String> modes = new ArrayList<>();
                        for (PvPMode mode : modeManager.getAllModes()) {
                            modes.add(mode.getId());
                        }
                        return filter(modes, args[2]);
                    }
                }
                case "setcategory" -> {
                    List<String> cats = new ArrayList<>();
                    for (ItemCategory cat : ItemCategory.values()) {
                        cats.add(cat.getId());
                    }
                    return filter(cats, args[2]);
                }
                case "give" -> {
                    List<String> modes = new ArrayList<>();
                    for (PvPMode mode : modeManager.getAllModes()) {
                        modes.add(mode.getId());
                    }
                    return filter(modes, args[2]);
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return filter(List.of("1", "2", "3", "4", "5"), args[3]);
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
