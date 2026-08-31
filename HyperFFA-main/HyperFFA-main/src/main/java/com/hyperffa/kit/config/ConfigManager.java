package com.hyperffa.kit.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration guisConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        File guisFile = new File(plugin.getDataFolder(), "guis.yml");
        if (!guisFile.exists()) {
            plugin.saveResource("guis.yml", false);
        }
        this.guisConfig = YamlConfiguration.loadConfiguration(guisFile);
        InputStream defaultStream = plugin.getResource("guis.yml");
        if (defaultStream != null) {
            YamlConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            this.guisConfig.setDefaults(def);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getGuisConfig() {
        return guisConfig;
    }

    public String getDefaultMode() {
        return config.getString("settings.default-mode", "sword");
    }

    public int getMaxSlotsPerMode() {
        return config.getInt("settings.max-slots-per-mode", 6);
    }

    public int getDefaultSlotsCount() {
        return config.getInt("settings.default-slots-count", 3);
    }

    public boolean isAutoSaveOnClose() {
        return config.getBoolean("settings.auto-save-on-close", true);
    }

    public boolean isPreventItemLeak() {
        return config.getBoolean("settings.prevent-item-leak", true);
    }

    public boolean isEnableSounds() {
        return config.getBoolean("settings.enable-sounds", true);
    }
}
