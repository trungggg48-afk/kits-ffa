package com.hyperffa.kit.api.event;

import com.hyperffa.kit.model.PvPMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerKitEditEvent extends PlayerEvent implements Cancellable {

    public enum Action {
        START,
        SAVE,
        CANCEL
    }

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final PvPMode mode;
    private final int slot;
    private final Action action;

    public PlayerKitEditEvent(Player who, PvPMode mode, int slot, Action action) {
        super(who);
        this.mode = mode;
        this.slot = slot;
        this.action = action;
    }

    public PvPMode getMode() {
        return mode;
    }

    public int getSlot() {
        return slot;
    }

    public Action getAction() {
        return action;
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
