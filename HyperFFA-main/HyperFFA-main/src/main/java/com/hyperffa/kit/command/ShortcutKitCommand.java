package com.hyperffa.kit.command;

import com.hyperffa.kit.config.ConfigManager;
import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.manager.KitManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShortcutKitCommand implements CommandExecutor {

    private final int slot;
    private final KitManager kitManager;
    private final ConfigManager configManager;
    private final MessageManager messageManager;

    public ShortcutKitCommand(int slot, KitManager kitManager, ConfigManager configManager, MessageManager messageManager) {
        this.slot = slot;
        this.kitManager = kitManager;
        this.configManager = configManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "messages.player-only");
            return true;
        }

        String defaultMode = configManager.getConfig().getString("settings.default-mode", "sword");
        kitManager.applyKitToPlayer(player, defaultMode, slot);
        return true;
    }
}
