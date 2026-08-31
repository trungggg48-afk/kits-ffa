package com.hyperffa.kit.listener;

import com.hyperffa.kit.config.ConfigManager;
import com.hyperffa.kit.manager.EditSessionManager;
import com.hyperffa.kit.manager.KitManager;
import com.hyperffa.kit.manager.StatsManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@SuppressFBWarnings({"EI_EXPOSE_REP2"})
public class PlayerConnectionListener implements Listener {

    private final KitManager kitManager;
    private final EditSessionManager editSessionManager;
    private final StatsManager statsManager;
    private final ConfigManager configManager;

    public PlayerConnectionListener(KitManager kitManager, EditSessionManager editSessionManager,
                                    StatsManager statsManager, ConfigManager configManager) {
        this.kitManager = kitManager;
        this.editSessionManager = editSessionManager;
        this.statsManager = statsManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        kitManager.preloadPlayerData(player.getUniqueId());
        statsManager.onPlayerJoin(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (editSessionManager.isEditing(player)) {
            if (configManager.isAutoSaveOnClose()) {
                editSessionManager.saveAndEndSession(player);
            } else {
                editSessionManager.cancelAndEndSession(player);
            }
        }
        kitManager.unloadPlayerData(player.getUniqueId());
        statsManager.onPlayerQuit(player);
    }
}
