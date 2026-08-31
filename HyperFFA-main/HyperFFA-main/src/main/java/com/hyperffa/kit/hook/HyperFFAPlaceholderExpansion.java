package com.hyperffa.kit.hook;

import com.hyperffa.kit.HyperFFAKitPlugin;
import com.hyperffa.kit.manager.StatsManager;
import com.hyperffa.kit.model.PlayerStats;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@SuppressFBWarnings({"EI_EXPOSE_REP2", "HE_INHERITS_EQUALS_USE_HASHCODE"})
public final class HyperFFAPlaceholderExpansion extends PlaceholderExpansion {

    private final HyperFFAKitPlugin plugin;
    private final StatsManager statsManager;

    public HyperFFAPlaceholderExpansion(HyperFFAKitPlugin plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hyperffa";
    }

    @Override
    public @NotNull String getAuthor() {
        return "HyperTeam";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String lower = params.toLowerCase(Locale.ROOT);

        // Server-wide best killstreak placeholders (player can be null or offline)
        if (lower.equals("best_killstreak") || lower.equals("best_streak") || lower.equals("max_killstreak") || lower.equals("max_streak")) {
            return String.valueOf(statsManager.getServerBestKillstreak());
        }
        if (lower.equals("best_killstreak_player") || lower.equals("best_streak_player")) {
            return statsManager.getServerBestKillstreakPlayer();
        }

        // Player's personal current killstreak
        if (player == null) {
            return "";
        }

        PlayerStats stats = statsManager.getStats(player.getUniqueId());
        if (lower.equals("killstreak") || lower.equals("streak") || lower.equals("current_killstreak") || lower.equals("current_streak")) {
            return String.valueOf(stats.getCurrentKillstreak());
        }

        // Player's personal best killstreak
        if (lower.equals("player_best_killstreak") || lower.equals("player_best_streak")) {
            return String.valueOf(stats.getBestKillstreak());
        }

        return null;
    }
}
