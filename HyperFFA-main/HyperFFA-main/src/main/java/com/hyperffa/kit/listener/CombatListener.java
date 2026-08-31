package com.hyperffa.kit.listener;

import com.hyperffa.kit.config.ConfigManager;
import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.config.SoundManager;
import com.hyperffa.kit.manager.StatsManager;
import com.hyperffa.kit.model.PlayerStats;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressFBWarnings({"EI_EXPOSE_REP2"})
public final class CombatListener implements Listener {

    private final StatsManager statsManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;
    private final ConfigManager configManager;

    private final Map<UUID, Long> combatTags = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastAttackers = new ConcurrentHashMap<>();

    public CombatListener(StatsManager statsManager, MessageManager messageManager,
                          SoundManager soundManager, ConfigManager configManager) {
        this.statsManager = statsManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player damager) {
            long tagDuration = configManager.getConfig().getLong("settings.combat-tag-seconds", 15) * 1000L;
            long expireAt = System.currentTimeMillis() + tagDuration;

            combatTags.put(victim.getUniqueId(), expireAt);
            combatTags.put(damager.getUniqueId(), expireAt);
            lastAttackers.put(victim.getUniqueId(), damager.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        event.deathMessage(null); // Override default vanilla death message

        PlayerStats victimStats = statsManager.getStats(victim.getUniqueId());
        victimStats.incrementDeaths();

        Player killer = victim.getKiller();
        if (killer == null) {
            UUID lastAttackerUuid = lastAttackers.get(victim.getUniqueId());
            if (lastAttackerUuid != null) {
                Player potentialKiller = Bukkit.getPlayer(lastAttackerUuid);
                if (potentialKiller != null && potentialKiller.isOnline()) {
                    killer = potentialKiller;
                }
            }
        }

        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            PlayerStats killerStats = statsManager.getStats(killer.getUniqueId());
            killerStats.incrementKills();
            statsManager.checkAndUpdateServerBest(killerStats.getCurrentKillstreak(), killer.getName());

            int killerHp = (int) Math.ceil(killer.getHealth());
            Component deathMsg = messageManager.getMessage("combat.pvp-death", Map.of(
                    "victim", victim.getName(),
                    "killer", killer.getName(),
                    "hp", String.valueOf(killerHp)
            ));
            Bukkit.broadcast(deathMsg);

            int streak = killerStats.getCurrentKillstreak();
            List<Integer> milestones = configManager.getConfig().getIntegerList("killstreak.milestones");
            if (milestones.contains(streak)) {
                Component ksMsg = messageManager.getMessage("combat.killstreak-broadcast", Map.of(
                        "player", killer.getName(),
                        "streak", String.valueOf(streak)
                ));
                Bukkit.broadcast(ksMsg);
                soundManager.playSound(killer, "killstreak", 1.0f, 1.0f);
            } else {
                soundManager.playSound(killer, "kill", 1.0f, 1.2f);
            }
        } else {
            EntityDamageEvent lastDamage = victim.getLastDamageCause();
            if (lastDamage != null && lastDamage.getCause() == EntityDamageEvent.DamageCause.FALL) {
                Bukkit.broadcast(messageManager.getMessage("combat.fall-death", Map.of("victim", victim.getName())));
            } else {
                Bukkit.broadcast(messageManager.getMessage("combat.generic-death", Map.of("victim", victim.getName())));
            }
        }

        combatTags.remove(victim.getUniqueId());
        lastAttackers.remove(victim.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Long tagUntil = combatTags.remove(uuid);

        if (tagUntil != null && System.currentTimeMillis() < tagUntil) {
            Component penaltyMsg = messageManager.getMessage("combat.combat-log-penalty", Map.of(
                    "player", player.getName()
            ));
            Bukkit.broadcast(penaltyMsg);

            PlayerStats stats = statsManager.getStats(uuid);
            stats.incrementDeaths();

            UUID attackerUuid = lastAttackers.remove(uuid);
            if (attackerUuid != null) {
                Player attacker = Bukkit.getPlayer(attackerUuid);
                if (attacker != null && attacker.isOnline()) {
                    PlayerStats attackerStats = statsManager.getStats(attackerUuid);
                    attackerStats.incrementKills();
                }
            }
        }
    }
}
