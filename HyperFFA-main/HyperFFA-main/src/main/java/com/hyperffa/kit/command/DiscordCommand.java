package com.hyperffa.kit.command;

import com.hyperffa.kit.config.ConfigManager;
import com.hyperffa.kit.config.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DiscordCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final MessageManager messageManager;

    public DiscordCommand(ConfigManager configManager, MessageManager messageManager) {
        this.configManager = configManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String discordUrl = configManager.getConfig().getString("settings.discord-url", "https://discord.gg/vnsword");
        sender.sendMessage(messageManager.getMessage("combat.discord-link", Map.of("url", discordUrl)));
        return true;
    }
}
