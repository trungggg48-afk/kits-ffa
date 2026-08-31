package com.hyperffa.kit.config;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public final class SoundManager {

    private static final Logger LOGGER = Logger.getLogger(SoundManager.class.getName());
    private final ConfigManager configManager;

    public SoundManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void playSound(Player player, String soundKeyPath, float volume, float pitch) {
        if (!configManager.isEnableSounds() || player == null) {
            return;
        }
        String keyString = configManager.getConfig().getString("sounds." + soundKeyPath);
        if (keyString == null || keyString.isEmpty()) {
            return;
        }
        try {
            Key key = Key.key(keyString);
            Sound sound = Sound.sound(key, Sound.Source.PLAYER, volume, pitch);
            player.playSound(sound);
        } catch (IllegalArgumentException e) {
            LOGGER.fine(() -> "Invalid sound key configured for " + soundKeyPath + ": " + keyString);
        }
    }

    public void playKitLoad(Player player) {
        playSound(player, "kit-load", 1.0f, 1.2f);
    }

    public void playKitSave(Player player) {
        playSound(player, "kit-save", 1.0f, 1.5f);
    }

    public void playKitClear(Player player) {
        playSound(player, "kit-clear", 1.0f, 1.0f);
    }

    public void playClickSuccess(Player player) {
        playSound(player, "click-success", 0.7f, 1.2f);
    }

    public void playClickError(Player player) {
        playSound(player, "click-error", 0.8f, 0.8f);
    }

    public void playOpenMenu(Player player) {
        playSound(player, "open-menu", 0.6f, 1.0f);
    }
}
