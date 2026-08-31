package com.hyperffa.kit.command;

import com.hyperffa.kit.config.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class RtpqCommand implements CommandExecutor {

    private final MessageManager messageManager;

    public RtpqCommand(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "messages.player-only");
            return true;
        }

        Bukkit.broadcast(messageManager.getMessage("combat.queue-join", Map.of("player", player.getName())));
        return true;
    }
}
