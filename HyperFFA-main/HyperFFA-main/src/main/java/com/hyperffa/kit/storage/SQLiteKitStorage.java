package com.hyperffa.kit.storage;

import com.hyperffa.kit.model.KitData;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class SQLiteKitStorage {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;

    public SQLiteKitStorage(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<KitData> loadPlayerKit(UUID uuid, String mode, int slot) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT contents, armor, offhand FROM hyperkit_player_kits WHERE uuid = ? AND mode = ? AND slot = ?;";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, mode.toLowerCase(Locale.ROOT));
                ps.setInt(3, slot);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String contentsB64 = rs.getString("contents");
                        String armorB64 = rs.getString("armor");
                        String offhandB64 = rs.getString("offhand");
                        return KitData.deserialize(contentsB64, armorB64, offhandB64);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading kit for " + uuid + " in mode " + mode + " slot " + slot, e);
            }
            return null;
        });
    }

    public CompletableFuture<Map<Integer, KitData>> loadAllPlayerKits(UUID uuid, String mode) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Integer, KitData> result = new HashMap<>();
            String sql = "SELECT slot, contents, armor, offhand FROM hyperkit_player_kits WHERE uuid = ? AND mode = ?;";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, mode.toLowerCase(Locale.ROOT));

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int slot = rs.getInt("slot");
                        String contentsB64 = rs.getString("contents");
                        String armorB64 = rs.getString("armor");
                        String offhandB64 = rs.getString("offhand");
                        result.put(slot, KitData.deserialize(contentsB64, armorB64, offhandB64));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading all kits for " + uuid + " in mode " + mode, e);
            }
            return result;
        });
    }

    public CompletableFuture<Void> savePlayerKit(UUID uuid, String mode, int slot, KitData kitData) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO hyperkit_player_kits (uuid, mode, slot, contents, armor, offhand, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid, mode, slot) DO UPDATE SET
                    contents = excluded.contents,
                    armor = excluded.armor,
                    offhand = excluded.offhand,
                    updated_at = excluded.updated_at;
            """;
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, mode.toLowerCase(Locale.ROOT));
                ps.setInt(3, slot);
                ps.setString(4, kitData.serializeContents());
                ps.setString(5, kitData.serializeArmor());
                ps.setString(6, kitData.serializeOffhand());
                ps.setLong(7, System.currentTimeMillis());

                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving kit for " + uuid + " in mode " + mode + " slot " + slot, e);
            }
        });
    }

    public CompletableFuture<Void> deletePlayerKit(UUID uuid, String mode, int slot) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM hyperkit_player_kits WHERE uuid = ? AND mode = ? AND slot = ?;";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, mode.toLowerCase(Locale.ROOT));
                ps.setInt(3, slot);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error deleting kit for " + uuid + " in mode " + mode + " slot " + slot, e);
            }
        });
    }

    public CompletableFuture<Integer> getSelectedSlot(UUID uuid, String mode) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT slot FROM hyperkit_selected_kits WHERE uuid = ? AND mode = ?;";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, mode.toLowerCase(Locale.ROOT));

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("slot");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error getting selected kit for " + uuid + " in mode " + mode, e);
            }
            return 1; // Default to Slot 1 if not explicitly selected
        });
    }

    public CompletableFuture<Void> setSelectedSlot(UUID uuid, String mode, int slot) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO hyperkit_selected_kits (uuid, mode, slot)
                VALUES (?, ?, ?)
                ON CONFLICT(uuid, mode) DO UPDATE SET
                    slot = excluded.slot;
            """;
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, mode.toLowerCase(Locale.ROOT));
                ps.setInt(3, slot);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error setting selected kit for " + uuid + " in mode " + mode, e);
            }
        });
    }

    public CompletableFuture<Void> saveSharedCode(String code, String mode, KitData kitData, UUID creator) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO hyperkit_shared_codes (code, mode, contents, armor, offhand, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(code) DO UPDATE SET
                    mode = excluded.mode,
                    contents = excluded.contents,
                    armor = excluded.armor,
                    offhand = excluded.offhand,
                    created_by = excluded.created_by,
                    created_at = excluded.created_at;
            """;
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, code.toUpperCase(Locale.ROOT));
                ps.setString(2, mode.toLowerCase(Locale.ROOT));
                ps.setString(3, kitData.serializeContents());
                ps.setString(4, kitData.serializeArmor());
                ps.setString(5, kitData.serializeOffhand());
                ps.setString(6, creator != null ? creator.toString() : "CONSOLE");
                ps.setLong(7, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving shared code " + code, e);
            }
        });
    }

    public CompletableFuture<KitData> loadSharedCode(String code) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT contents, armor, offhand FROM hyperkit_shared_codes WHERE code = ?;";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, code.toUpperCase(Locale.ROOT));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return KitData.deserialize(
                                rs.getString("contents"),
                                rs.getString("armor"),
                                rs.getString("offhand")
                        );
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading shared code " + code, e);
            }
            return null;
        });
    }
}
