package com.hyperffa.kit.api.event;

import com.hyperffa.kit.model.KitData;
import com.hyperffa.kit.model.PvPMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerKitLoadEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final PvPMode mode;
    private final int slot;
    private KitData kitData;
    private final boolean isDefaultFallback;

    public PlayerKitLoadEvent(Player who, PvPMode mode, int slot, KitData kitData, boolean isDefaultFallback) {
        super(who);
        this.mode = mode;
        this.slot = slot;
        this.kitData = kitData;
        this.isDefaultFallback = isDefaultFallback;
    }

    public PvPMode getMode() {
        return mode;
    }

    public int getSlot() {
        return slot;
    }

    public KitData getKitData() {
        return kitData;
    }

    public void setKitData(KitData kitData) {
        this.kitData = kitData;
    }

    public boolean isDefaultFallback() {
        return isDefaultFallback;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
