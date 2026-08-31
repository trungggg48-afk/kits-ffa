package com.hyperffa.kit.command;

import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.manager.StatsManager;
import com.hyperffa.kit.model.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class StatsCommand implements CommandExecutor {

    private final StatsManager statsManager;
    private final MessageManager messageManager;

    public StatsCommand(StatsManager statsManager, MessageManager messageManager) {
        this.statsManager = statsManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(messageManager.parse("<red>Không tìm thấy người chơi: " + args[0] + "</red>"));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            messageManager.sendMessage(sender, "messages.player-only");
            return true;
        }

        PlayerStats stats = statsManager.getStats(target.getUniqueId());
        sender.sendMessage(messageManager.parse("<gradient:#3b82f6:#60a5fa><bold>── THỐNG KÊ CHIẾN TÍCH: " + target.getName() + " ──</bold></gradient>"));
        sender.sendMessage(messageManager.parse("<gray>Kills / Deaths:</gray> <yellow>" + stats.getKills() + "</yellow> / <red>" + stats.getDeaths() + "</red>"));
        sender.sendMessage(messageManager.parse("<gray>Chuỗi Kill Hiện Tại:</gray> <yellow>" + stats.getCurrentKillstreak() + "</yellow> (Tốt nhất: <gold>" + stats.getBestKillstreak() + "</gold>)"));
        sender.sendMessage(messageManager.parse("<gray>Thời Gian Chơi:</gray> <aqua>" + stats.getPlaytimeHours() + "h</aqua>"));
        sender.sendMessage(messageManager.parse("<gray>Xu:</gray> <yellow>" + String.format(Locale.ROOT, "%.0f", stats.getCoins()) + " 🪙</yellow> | <gray>Tiền:</gray> <gold>" + String.format(Locale.ROOT, "%.1f", stats.getMoney()) + " ₫</gold>"));
        sender.sendMessage(messageManager.parse("<gray>Tier Hiện Tại:</gray> <white>" + stats.getTier() + "</white>"));

        return true;
    }
}
