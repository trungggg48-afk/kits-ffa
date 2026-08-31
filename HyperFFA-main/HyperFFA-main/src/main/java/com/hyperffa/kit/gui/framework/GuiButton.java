package com.hyperffa.kit.gui.framework;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class GuiButton {

    private final ItemStack item;
    private final Consumer<InventoryClickEvent> action;

    public GuiButton(ItemStack item, Consumer<InventoryClickEvent> action) {
        this.item = item;
        this.action = action;
    }

    public GuiButton(ItemStack item) {
        this(item, null);
    }

    public ItemStack getItem() {
        return item != null ? item.clone() : null;
    }

    public Consumer<InventoryClickEvent> getAction() {
        return action;
    }

    public void onClick(InventoryClickEvent event) {
        if (action != null) {
            action.accept(event);
        }
    }
}
