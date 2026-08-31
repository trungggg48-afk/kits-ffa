package com.hyperffa.kit.command;

import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.manager.StatsManager;
import com.hyperffa.kit.model.PlayerStats;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class KillstreakCommand implements CommandExecutor {

    private final StatsManager statsManager;
    private final MessageManager messageManager;

    public KillstreakCommand(StatsManager statsManager, MessageManager messageManager) {
        this.statsManager = statsManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "messages.player-only");
            return true;
        }

        PlayerStats stats = statsManager.getStats(player.getUniqueId());
        messageManager.sendMessage(player, "combat.killstreak-status", Map.of(
                "current", String.valueOf(stats.getCurrentKillstreak()),
                "best", String.valueOf(stats.getBestKillstreak())
        ));
        return true;
    }
}
