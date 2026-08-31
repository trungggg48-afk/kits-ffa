package com.hyperffa.kit.command;

import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.manager.StatsManager;
import com.hyperffa.kit.model.PlayerStats;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

public class LeaderboardCommand implements CommandExecutor {

    private final StatsManager statsManager;
    private final MessageManager messageManager;

    public LeaderboardCommand(StatsManager statsManager, MessageManager messageManager) {
        this.statsManager = statsManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        if (cmd.contains("kill")) {
            statsManager.getTopKills(10).thenAccept(list -> {
                sender.sendMessage(messageManager.getMessage("leaderboard.kills-header"));
                for (int i = 0; i < list.size(); i++) {
                    PlayerStats ps = list.get(i);
                    sender.sendMessage(messageManager.getMessage("leaderboard.line-entry", Map.of(
                            "rank", String.valueOf(i + 1),
                            "player", ps.getName(),
                            "color", "<yellow>",
                            "value", String.format(Locale.ROOT, "%,d", ps.getKills())
                    )));
                }
                if (sender instanceof Player p) {
                    PlayerStats myStats = statsManager.getStats(p.getUniqueId());
                    sender.sendMessage(messageManager.getMessage("leaderboard.player-rank-kills", Map.of(
                            "player", p.getName(),
                            "value", String.valueOf(myStats.getKills())
                    )));
                }
            });
        } else if (cmd.contains("death")) {
            statsManager.getTopDeaths(10).thenAccept(list -> {
                sender.sendMessage(messageManager.getMessage("leaderboard.deaths-header"));
                for (int i = 0; i < list.size(); i++) {
                    PlayerStats ps = list.get(i);
                    sender.sendMessage(messageManager.getMessage("leaderboard.line-entry", Map.of(
                            "rank", String.valueOf(i + 1),
                            "player", ps.getName(),
                            "color", "<red>",
                            "value", String.format(Locale.ROOT, "%,d", ps.getDeaths())
                    )));
                }
                if (sender instanceof Player p) {
                    PlayerStats myStats = statsManager.getStats(p.getUniqueId());
                    sender.sendMessage(messageManager.getMessage("leaderboard.player-rank-deaths", Map.of(
                            "player", p.getName(),
                            "value", String.valueOf(myStats.getDeaths())
                    )));
                }
            });
        } else {
            // Playtime
            statsManager.getTopPlaytime(10).thenAccept(list -> {
                sender.sendMessage(messageManager.getMessage("leaderboard.playtime-header"));
                for (int i = 0; i < list.size(); i++) {
                    PlayerStats ps = list.get(i);
                    sender.sendMessage(messageManager.getMessage("leaderboard.line-entry", Map.of(
                            "rank", String.valueOf(i + 1),
                            "player", ps.getName(),
                            "color", "<aqua>",
                            "value", ps.getPlaytimeHours() + "h"
                    )));
                }
                if (sender instanceof Player p) {
                    PlayerStats myStats = statsManager.getStats(p.getUniqueId());
                    sender.sendMessage(messageManager.getMessage("leaderboard.player-rank-time", Map.of(
                            "player", p.getName(),
                            "value", myStats.getPlaytimeHours() + "h"
                    )));
                }
            });
        }

        return true;
    }
}
