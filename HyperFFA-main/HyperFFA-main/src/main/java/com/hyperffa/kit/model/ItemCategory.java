package com.hyperffa.kit.model;

import org.bukkit.Material;

public enum ItemCategory {
    GEAR("gear", "Gear", Material.NETHERITE_SWORD, "<gradient:#eab308:#ca8a04><bold>⚔ GEAR</bold></gradient>"),
    POTIONS("potions", "Potions", Material.POTION, "<gradient:#d946ef:#a855f7><bold>🧪 POTIONS</bold></gradient>"),
    CONSUMABLES("consumables", "Consumables", Material.TOTEM_OF_UNDYING, "<gradient:#f59e0b:#d97706><bold>🍏 CONSUMABLES</bold></gradient>"),
    EXPLOSIONS("explosions", "Explosives", Material.END_CRYSTAL, "<gradient:#ef4444:#b91c1c><bold>💣 EXPLOSIVES</bold></gradient>"),
    MISCELLANEOUS("miscellaneous", "Miscellaneous", Material.PURPLE_SHULKER_BOX, "<gradient:#8b5cf6:#6d28d9><bold>📦 MISCELLANEOUS</bold></gradient>");

    private final String id;
    private final String displayName;
    private final Material icon;
    private final String gradientTitle;

    ItemCategory(String id, String displayName, Material icon, String gradientTitle) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.gradientTitle = gradientTitle;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getGradientTitle() {
        return gradientTitle;
    }

    public static ItemCategory fromId(String id) {
        if (id == null) return null;
        if (id.equalsIgnoreCase("blocks") || id.equalsIgnoreCase("items")) {
            return MISCELLANEOUS;
        }
        for (ItemCategory cat : values()) {
            if (cat.id.equalsIgnoreCase(id) || cat.name().equalsIgnoreCase(id)) {
                return cat;
            }
        }
        return null;
    }
}
