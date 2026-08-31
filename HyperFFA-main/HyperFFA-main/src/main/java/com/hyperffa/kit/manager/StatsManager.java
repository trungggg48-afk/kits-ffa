package com.hyperffa.kit.manager;

import com.hyperffa.kit.model.PlayerStats;
import com.hyperffa.kit.scheduler.PlatformScheduler;
import com.hyperffa.kit.scheduler.TaskHandle;
import com.hyperffa.kit.storage.DatabaseManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@SuppressFBWarnings({"EI_EXPOSE_REP2"})
public final class StatsManager {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> joinTimestamps = new ConcurrentHashMap<>();
    private TaskHandle autoSaveTask;
    private volatile int serverBestKillstreak = 0;
    private volatile String serverBestKillstreakPlayer = "None";

    public StatsManager(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        startPlaytimeTracker();
        CompletableFuture.runAsync(this::refreshServerBestKillstreak);
    }

    public int getServerBestKillstreak() {
        return serverBestKillstreak;
    }

    public String getServerBestKillstreakPlayer() {
        return serverBestKillstreakPlayer;
    }

    public void checkAndUpdateServerBest(int streak, String playerName) {
        if (streak > serverBestKillstreak) {
            serverBestKillstreak = streak;
            if (playerName != null && !playerName.isEmpty()) {
                serverBestKillstreakPlayer = playerName;
            }
        }
    }

    public void refreshServerBestKillstreak() {
        String sql = "SELECT name, best_killstreak FROM hyperkit_player_stats ORDER BY best_killstreak DESC LIMIT 1;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int ks = rs.getInt("best_killstreak");
                String name = rs.getString("name");
                if (ks > this.serverBestKillstreak) {
                    this.serverBestKillstreak = ks;
                    this.serverBestKillstreakPlayer = (name != null) ? name : "None";
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load server best killstreak", e);
        }
    }

    private void startPlaytimeTracker() {
        this.autoSaveTask = PlatformScheduler.runAsyncTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                Long lastCheck = joinTimestamps.get(uuid);
                if (lastCheck != null) {
                    long deltaSeconds = (now - lastCheck) / 1000;
                    if (deltaSeconds > 0) {
                        PlayerStats stats = getStats(uuid);
                        stats.addPlaytimeSeconds(deltaSeconds);
                        joinTimestamps.put(uuid, now);
                    }
                } else {
                    joinTimestamps.put(uuid, now);
                }
            }
            saveAllStats();
            refreshServerBestKillstreak();
        }, 1200L, 1200L); // Every 60 seconds
    }

    public CompletableFuture<PlayerStats> loadStats(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT name, kills, deaths, playtime, current_killstreak, best_killstreak, coins, money, tier FROM hyperkit_player_stats WHERE uuid = ?;";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String dbName = rs.getString("name");
                        int kills = rs.getInt("kills");
                        int deaths = rs.getInt("deaths");
                        long playtime = rs.getLong("playtime");
                        int curKs = rs.getInt("current_killstreak");
                        int bestKs = rs.getInt("best_killstreak");
                        double coins = rs.getDouble("coins");
                        double money = rs.getDouble("money");
                        String tier = rs.getString("tier");

                        PlayerStats stats = new PlayerStats(uuid, (name != null ? name : dbName), kills, deaths, playtime, curKs, bestKs, coins, money, tier);
                        cache.put(uuid, stats);
                        return stats;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load player stats for " + uuid, e);
            }

            // Create new record
            PlayerStats newStats = new PlayerStats(uuid, name != null ? name : "Unknown");
            cache.put(uuid, newStats);
            saveStats(newStats);
            return newStats;
        });
    }

    public PlayerStats getStats(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> {
            Player p = Bukkit.getPlayer(uuid);
            return new PlayerStats(uuid, p != null ? p.getName() : "Unknown");
        });
    }

    public CompletableFuture<Void> saveStats(PlayerStats stats) {
        if (stats == null) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO hyperkit_player_stats (uuid, name, kills, deaths, playtime, current_killstreak, best_killstreak, coins, money, tier)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    name = excluded.name,
                    kills = excluded.kills,
                    deaths = excluded.deaths,
                    playtime = excluded.playtime,
                    current_killstreak = excluded.current_killstreak,
                    best_killstreak = excluded.best_killstreak,
                    coins = excluded.coins,
                    money = excluded.money,
                    tier = excluded.tier;
            """;
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, stats.getUuid().toString());
                ps.setString(2, stats.getName());
                ps.setInt(3, stats.getKills());
                ps.setInt(4, stats.getDeaths());
                ps.setLong(5, stats.getPlaytimeSeconds());
                ps.setInt(6, stats.getCurrentKillstreak());
                ps.setInt(7, stats.getBestKillstreak());
                ps.setDouble(8, stats.getCoins());
                ps.setDouble(9, stats.getMoney());
                ps.setString(10, stats.getTier());

                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player stats for " + stats.getUuid(), e);
            }
        });
    }

    public void onPlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        joinTimestamps.put(uuid, System.currentTimeMillis());
        loadStats(uuid, player.getName());
    }

    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        Long joined = joinTimestamps.remove(uuid);
        if (joined != null) {
            long delta = (System.currentTimeMillis() - joined) / 1000;
            PlayerStats stats = getStats(uuid);
            stats.addPlaytimeSeconds(delta);
            saveStats(stats);
        }
        cache.remove(uuid);
    }

    public void saveAllStats() {
        for (PlayerStats stats : cache.values()) {
            saveStats(stats);
        }
    }

    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Long joined = joinTimestamps.get(uuid);
            if (joined != null) {
                long delta = (now - joined) / 1000;
                PlayerStats stats = getStats(uuid);
                stats.addPlaytimeSeconds(delta);
            }
        }
        saveAllStats();
    }

    private static final String SQL_TOP_KILLS = "SELECT uuid, name, kills, deaths, playtime, current_killstreak, best_killstreak, coins, money, tier FROM hyperkit_player_stats ORDER BY kills DESC LIMIT ?;";
    private static final String SQL_TOP_DEATHS = "SELECT uuid, name, kills, deaths, playtime, current_killstreak, best_killstreak, coins, money, tier FROM hyperkit_player_stats ORDER BY deaths DESC LIMIT ?;";
    private static final String SQL_TOP_PLAYTIME = "SELECT uuid, name, kills, deaths, playtime, current_killstreak, best_killstreak, coins, money, tier FROM hyperkit_player_stats ORDER BY playtime DESC LIMIT ?;";

    public CompletableFuture<List<PlayerStats>> getTopKills(int limit) {
        return CompletableFuture.supplyAsync(() -> queryLeaderboard(SQL_TOP_KILLS, limit));
    }

    public CompletableFuture<List<PlayerStats>> getTopDeaths(int limit) {
        return CompletableFuture.supplyAsync(() -> queryLeaderboard(SQL_TOP_DEATHS, limit));
    }

    public CompletableFuture<List<PlayerStats>> getTopPlaytime(int limit) {
        return CompletableFuture.supplyAsync(() -> queryLeaderboard(SQL_TOP_PLAYTIME, limit));
    }

    private List<PlayerStats> queryLeaderboard(String sql, int limit) {
        List<PlayerStats> result = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    int kills = rs.getInt("kills");
                    int deaths = rs.getInt("deaths");
                    long playtime = rs.getLong("playtime");
                    int curKs = rs.getInt("current_killstreak");
                    int bestKs = rs.getInt("best_killstreak");
                    double coins = rs.getDouble("coins");
                    double money = rs.getDouble("money");
                    String tier = rs.getString("tier");

                    result.add(new PlayerStats(uuid, name, kills, deaths, playtime, curKs, bestKs, coins, money, tier));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to query top leaderboard", e);
        }
        return result;
    }
}
