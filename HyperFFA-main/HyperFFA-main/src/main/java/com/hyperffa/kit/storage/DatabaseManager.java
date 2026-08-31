package com.hyperffa.kit.storage;

import com.hyperffa.kit.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

@SuppressFBWarnings({"EI_EXPOSE_REP2"})
public final class DatabaseManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataSource = createPool();
        createTables();
    }

    private HikariDataSource createPool() {
        String fileName = "hyperkit.db";
        FileConfiguration config = configManager.getConfig();
        if (config != null) {
            String cfgName = config.getString("database.file", "hyperkit.db");
            if (cfgName != null && !cfgName.isEmpty()) {
                fileName = cfgName;
            }
        }

        File dataFolder = plugin.getDataFolder();
        File dbFile = new File(dataFolder, fileName);
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            try {
                Files.createDirectories(parent.toPath());
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create database directories", e);
            }
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("HyperKit-SQLite-Pool");
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());

        int maxPool = 5;
        int minIdle = 2;
        long connTimeout = 30000;
        long idleTimeout = 600000;
        long maxLifetime = 1800000;

        if (config != null) {
            maxPool = config.getInt("database.pool.maximum-pool-size", 5);
            minIdle = config.getInt("database.pool.minimum-idle", 2);
            connTimeout = config.getLong("database.pool.connection-timeout", 30000);
            idleTimeout = config.getLong("database.pool.idle-timeout", 600000);
            maxLifetime = config.getLong("database.pool.max-lifetime", 1800000);
        }

        hikariConfig.setMaximumPoolSize(maxPool);
        hikariConfig.setMinimumIdle(minIdle);
        hikariConfig.setConnectionTimeout(connTimeout);
        hikariConfig.setIdleTimeout(idleTimeout);
        hikariConfig.setMaxLifetime(maxLifetime);

        // SQLite specific pragmas
        hikariConfig.addDataSourceProperty("journal_mode", "WAL");
        hikariConfig.addDataSourceProperty("synchronous", "NORMAL");
        hikariConfig.addDataSourceProperty("busy_timeout", "5000");

        return new HikariDataSource(hikariConfig);
    }

    private void createTables() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Player Custom Kits table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS hyperkit_player_kits (
                    uuid VARCHAR(36) NOT NULL,
                    mode VARCHAR(32) NOT NULL,
                    slot INTEGER NOT NULL,
                    contents TEXT NOT NULL,
                    armor TEXT NOT NULL,
                    offhand TEXT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    PRIMARY KEY (uuid, mode, slot)
                );
            """);

            // Selected Kit per mode table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS hyperkit_selected_kits (
                    uuid VARCHAR(36) NOT NULL,
                    mode VARCHAR(32) NOT NULL,
                    slot INTEGER NOT NULL,
                    PRIMARY KEY (uuid, mode)
                );
            """);

            // Modes & Category Items configuration storage
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS hyperkit_modes (
                    id VARCHAR(32) PRIMARY KEY,
                    display_name VARCHAR(64) NOT NULL,
                    data TEXT NOT NULL
                );
            """);

            // Player Stats & Killstreaks table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS hyperkit_player_stats (
                    uuid VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(32) NOT NULL,
                    kills INTEGER NOT NULL DEFAULT 0,
                    deaths INTEGER NOT NULL DEFAULT 0,
                    playtime BIGINT NOT NULL DEFAULT 0,
                    current_killstreak INTEGER NOT NULL DEFAULT 0,
                    best_killstreak INTEGER NOT NULL DEFAULT 0,
                    coins DOUBLE NOT NULL DEFAULT 0.0,
                    money DOUBLE NOT NULL DEFAULT 0.0,
                    tier VARCHAR(16) NOT NULL DEFAULT 'LT5'
                );
            """);

            // Shared Kit Codes table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS hyperkit_shared_codes (
                    code VARCHAR(16) PRIMARY KEY,
                    mode VARCHAR(32) NOT NULL,
                    contents TEXT NOT NULL,
                    armor TEXT NOT NULL,
                    offhand TEXT NOT NULL,
                    created_by VARCHAR(36) NOT NULL,
                    created_at BIGINT NOT NULL
                );
            """);

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite database tables", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("HikariDataSource is closed or not initialized.");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("SQLite connection pool closed successfully.");
        }
    }
}
