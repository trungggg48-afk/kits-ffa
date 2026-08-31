package com.hyperffa.kit.gui.framework;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GuiHolder implements InventoryHolder {

    private final BaseGui gui;
    private Inventory inventory;

    public GuiHolder(BaseGui gui) {
        this.gui = gui;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public BaseGui getGui() {
        return gui;
    }
}
