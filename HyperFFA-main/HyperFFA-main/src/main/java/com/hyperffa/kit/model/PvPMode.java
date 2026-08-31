package com.hyperffa.kit.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PvPMode {

    private final String id;
    private String displayName;
    private final Map<ItemCategory, List<ItemStack>> categoryItems = new ConcurrentHashMap<>();
    private final Map<String, PremadeKit> premadeKits = new ConcurrentHashMap<>();
    private final Map<Material, Integer> itemLimits = new ConcurrentHashMap<>();
    private PremadeKit defaultTemplate;

    public PvPMode(String id, String displayName) {
        this.id = id.toLowerCase(Locale.ROOT);
        this.displayName = displayName != null ? displayName : id;
        for (ItemCategory cat : ItemCategory.values()) {
            categoryItems.put(cat, new ArrayList<>());
        }
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<ItemStack> getCategoryItems(ItemCategory category) {
        List<ItemStack> list = categoryItems.get(category);
        if (list == null) return Collections.emptyList();
        List<ItemStack> copy = new ArrayList<>(list.size());
        for (ItemStack item : list) {
            if (item != null) copy.add(item.clone());
        }
        return copy;
    }

    public void setCategoryItems(ItemCategory category, List<ItemStack> items) {
        List<ItemStack> sanitized = new ArrayList<>();
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null && !item.getType().isAir()) {
                    sanitized.add(item.clone());
                }
            }
        }
        categoryItems.put(category, sanitized);
    }

    public void addCategoryItem(ItemCategory category, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        categoryItems.computeIfAbsent(category, k -> new ArrayList<>()).add(item.clone());
    }

    public Map<String, PremadeKit> getPremadeKits() {
        return Collections.unmodifiableMap(premadeKits);
    }

    public PremadeKit getPremadeKit(String name) {
        return premadeKits.get(name.toLowerCase(Locale.ROOT));
    }

    public void addPremadeKit(PremadeKit premadeKit) {
        if (premadeKit == null) return;
        premadeKits.put(premadeKit.getName().toLowerCase(Locale.ROOT), premadeKit);
        if (defaultTemplate == null) {
            defaultTemplate = premadeKit;
        }
    }

    public void removePremadeKit(String name) {
        premadeKits.remove(name.toLowerCase(Locale.ROOT));
    }

    public PremadeKit getDefaultTemplate() {
        if (defaultTemplate != null) return defaultTemplate;
        if (!premadeKits.isEmpty()) {
            return premadeKits.values().iterator().next();
        }
        return null;
    }

    public void setDefaultTemplate(PremadeKit defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public Map<Material, Integer> getItemLimits() {
        return Collections.unmodifiableMap(itemLimits);
    }

    public int getItemLimit(Material material) {
        return itemLimits.getOrDefault(material, -1);
    }

    public void setItemLimit(Material material, int limit) {
        if (limit <= 0) {
            itemLimits.remove(material);
        } else {
            itemLimits.put(material, limit);
        }
    }

    public boolean isItemAllowed(ItemStack item) {
        if (item == null || item.getType().isAir()) return true;
        Material mat = item.getType();
        // Disallow only administrative or game-breaking items
        return mat != Material.BEDROCK && mat != Material.BARRIER
                && mat != Material.COMMAND_BLOCK && mat != Material.CHAIN_COMMAND_BLOCK
                && mat != Material.REPEATING_COMMAND_BLOCK && mat != Material.STRUCTURE_BLOCK
                && mat != Material.STRUCTURE_VOID && mat != Material.JIGSAW
                && mat != Material.LIGHT && mat != Material.DEBUG_STICK;
    }
}
