package com.hyperffa.kit;

import com.hyperffa.kit.api.HyperKitAPI;
import com.hyperffa.kit.command.*;
import com.hyperffa.kit.config.ConfigManager;
import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.config.SoundManager;
import com.hyperffa.kit.gui.framework.GuiListener;
import com.hyperffa.kit.listener.CombatListener;
import com.hyperffa.kit.listener.EditSessionProtectionListener;
import com.hyperffa.kit.listener.PlayerConnectionListener;
import com.hyperffa.kit.manager.EditSessionManager;
import com.hyperffa.kit.manager.KitManager;
import com.hyperffa.kit.manager.PvPModeManager;
import com.hyperffa.kit.manager.StatsManager;
import com.hyperffa.kit.storage.DatabaseManager;
import com.hyperffa.kit.storage.SQLiteKitStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_EXPOSE_REP", "REC_CATCH_EXCEPTION"})
public class HyperFFAKitPlugin extends JavaPlugin {

    private static HyperFFAKitPlugin instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private SoundManager soundManager;

    private DatabaseManager databaseManager;
    private SQLiteKitStorage storage;

    private PvPModeManager modeManager;
    private KitManager kitManager;
    private EditSessionManager editSessionManager;
    private StatsManager statsManager;

    private HyperKitAPI api;

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();

        getLogger().info("Initializing HyperFFA / HyperKit System for Paper 1.21.x...");

        try {
            // 1. Config & Messages
            this.configManager = new ConfigManager(this);
            this.messageManager = new MessageManager(this);
            this.soundManager = new SoundManager(configManager);

            // 2. Database Layer
            this.databaseManager = new DatabaseManager(this, configManager);
            this.storage = new SQLiteKitStorage(this, databaseManager);

            // 3. Managers
            this.modeManager = new PvPModeManager(this);
            this.kitManager = new KitManager(storage, modeManager, configManager, messageManager, soundManager);
            this.editSessionManager = new EditSessionManager(kitManager, modeManager, messageManager, soundManager);
            this.statsManager = new StatsManager(this, databaseManager);

            // 4. API Singleton
            this.api = new HyperKitAPI(modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager);

            // 5. Listeners
            PluginManager pm = getServer().getPluginManager();
            pm.registerEvents(new GuiListener(), this);
            pm.registerEvents(new PlayerConnectionListener(kitManager, editSessionManager, statsManager, configManager), this);
            pm.registerEvents(new EditSessionProtectionListener(editSessionManager, messageManager, soundManager), this);
            pm.registerEvents(new CombatListener(statsManager, messageManager, soundManager, configManager), this);

            // 6. Commands
            PluginCommand kitCmd = getCommand("kit");
            if (kitCmd != null) {
                KitCommand kitExecutor = new KitCommand(modeManager, kitManager, editSessionManager, messageManager, soundManager, configManager);
                kitCmd.setExecutor(kitExecutor);
                kitCmd.setTabCompleter(kitExecutor);
            }

            PluginCommand adminCmd = getCommand("kitadmin");
            if (adminCmd != null) {
                KitAdminCommand adminExecutor = new KitAdminCommand(modeManager, kitManager, configManager, messageManager, soundManager);
                adminCmd.setExecutor(adminExecutor);
                adminCmd.setTabCompleter(adminExecutor);
            }

            // Shortcuts /kit1 to /kit6
            for (int i = 1; i <= 6; i++) {
                PluginCommand scCmd = getCommand("kit" + i);
                if (scCmd != null) {
                    scCmd.setExecutor(new ShortcutKitCommand(i, kitManager, configManager, messageManager));
                }
            }

            // Stats, Killstreak, Leaderboards, Discord, Rtpq
            PluginCommand ksCmd = getCommand("killstreak");
            if (ksCmd != null) {
                ksCmd.setExecutor(new KillstreakCommand(statsManager, messageManager));
            }

            PluginCommand statsCmd = getCommand("stats");
            if (statsCmd != null) {
                statsCmd.setExecutor(new StatsCommand(statsManager, messageManager));
            }

            LeaderboardCommand lbExecutor = new LeaderboardCommand(statsManager, messageManager);
            PluginCommand topkCmd = getCommand("topkills");
            if (topkCmd != null) topkCmd.setExecutor(lbExecutor);
            PluginCommand topdCmd = getCommand("topdeaths");
            if (topdCmd != null) topdCmd.setExecutor(lbExecutor);
            PluginCommand toptCmd = getCommand("toptime");
            if (toptCmd != null) toptCmd.setExecutor(lbExecutor);

            PluginCommand discordCmd = getCommand("discord");
            if (discordCmd != null) {
                discordCmd.setExecutor(new DiscordCommand(configManager, messageManager));
            }

            PluginCommand rtpqCmd = getCommand("rtpq");
            if (rtpqCmd != null) {
                rtpqCmd.setExecutor(new RtpqCommand(messageManager));
            }

            // 7. Online players preload
            for (Player player : Bukkit.getOnlinePlayers()) {
                kitManager.preloadPlayerData(player.getUniqueId());
                statsManager.onPlayerJoin(player);
            }

            // 8. PlaceholderAPI Hook
            if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                new com.hyperffa.kit.hook.HyperFFAPlaceholderExpansion(this, statsManager).register();
                getLogger().info("Successfully hooked into PlaceholderAPI!");
            }

            long elapsed = System.currentTimeMillis() - start;
            getLogger().info("HyperFFA Kit Engine enabled successfully in " + elapsed + "ms!");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable HyperFFA Kit Plugin!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down HyperFFA Kit Plugin...");

        if (statsManager != null) {
            statsManager.shutdown();
        }

        if (editSessionManager != null) {
            editSessionManager.endAllSessionsOnDisable();
        }

        if (modeManager != null) {
            modeManager.saveModes();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("HyperFFA Kit Plugin disabled cleanly.");
    }

    public static HyperFFAKitPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SQLiteKitStorage getStorage() {
        return storage;
    }

    public PvPModeManager getModeManager() {
        return modeManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public EditSessionManager getEditSessionManager() {
        return editSessionManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public HyperKitAPI getApi() {
        return api;
    }
}
