package com.hyperffa.kit.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class PremadeKit {

    private final String name;
    private final KitData kitData;
    private final ItemStack icon;

    public PremadeKit(String name, KitData kitData, ItemStack icon) {
        this.name = name;
        this.kitData = kitData;
        this.icon = (icon != null && !icon.getType().isAir()) ? icon.clone() : new ItemStack(Material.CHEST);
    }

    public String getName() {
        return name;
    }

    public KitData getKitData() {
        return kitData;
    }

    public ItemStack getIcon() {
        return icon.clone();
    }
}
