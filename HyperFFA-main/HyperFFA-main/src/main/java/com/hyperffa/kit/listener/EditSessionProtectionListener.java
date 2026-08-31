package com.hyperffa.kit.listener;

import com.hyperffa.kit.config.MessageManager;
import com.hyperffa.kit.config.SoundManager;
import com.hyperffa.kit.gui.framework.GuiHolder;
import com.hyperffa.kit.manager.EditSessionManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.util.Locale;

@SuppressFBWarnings({"EI_EXPOSE_REP2"})
public final class EditSessionProtectionListener implements Listener {

    private final EditSessionManager editSessionManager;
    private final MessageManager messageManager;
    private final SoundManager soundManager;

    public EditSessionProtectionListener(EditSessionManager editSessionManager,
                                         MessageManager messageManager,
                                         SoundManager soundManager) {
        this.editSessionManager = editSessionManager;
        this.messageManager = messageManager;
        this.soundManager = soundManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (editSessionManager.isEditing(player)) {
            event.setCancelled(true);
            messageManager.sendMessage(player, "messages.action-blocked-in-editor");
            soundManager.playClickError(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (editSessionManager.isEditing(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (editSessionManager.isEditing(player)) {
                // Only allow opening our own GUI holder
                if (!(event.getInventory().getHolder() instanceof GuiHolder)) {
                    event.setCancelled(true);
                    messageManager.sendMessage(player, "messages.action-blocked-in-editor");
                    soundManager.playClickError(player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (editSessionManager.isEditing(player) && event.hasBlock()) {
            Block block = event.getClickedBlock();
            if (block == null) return;
            switch (block.getType()) {
                case CHEST, TRAPPED_CHEST, BARREL, SHULKER_BOX, BLACK_SHULKER_BOX,
                     BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, CYAN_SHULKER_BOX, GRAY_SHULKER_BOX,
                     GREEN_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX,
                     LIME_SHULKER_BOX, MAGENTA_SHULKER_BOX, ORANGE_SHULKER_BOX, PINK_SHULKER_BOX,
                     PURPLE_SHULKER_BOX, RED_SHULKER_BOX, WHITE_SHULKER_BOX, YELLOW_SHULKER_BOX,
                     ENDER_CHEST, HOPPER, DISPENSER, DROPPER, BREWING_STAND, FURNACE,
                     BLAST_FURNACE, SMOKER, BEACON, ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL,
                     CRAFTING_TABLE, ENCHANTING_TABLE, SMITHING_TABLE, GRINDSTONE,
                     STONECUTTER, CARTOGRAPHY_TABLE, LOOM, JUKEBOX, CRAFTER -> {
                    event.setCancelled(true);
                    messageManager.sendMessage(player, "messages.action-blocked-in-editor");
                    soundManager.playClickError(player);
                }
                default -> {}
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (editSessionManager.isEditing(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (editSessionManager.isEditing(player)) {
            String msg = event.getMessage().toLowerCase(Locale.ROOT).trim();
            if (!msg.startsWith("/kit") && !msg.startsWith("/kits") && !msg.startsWith("/kitroom")) {
                event.setCancelled(true);
                messageManager.sendMessage(player, "messages.action-blocked-in-editor");
                soundManager.playClickError(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (editSessionManager.isEditing(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (editSessionManager.isEditing(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (editSessionManager.isEditing(player)) {
            event.getDrops().clear();
            event.setKeepInventory(true);
            editSessionManager.cancelAndEndSession(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (editSessionManager.isEditing(player)) {
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND ||
                event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
                editSessionManager.cancelAndEndSession(player);
            }
        }
    }
}
