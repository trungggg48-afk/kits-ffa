package com.hyperffa.kit.validation;

import com.hyperffa.kit.model.KitData;
import com.hyperffa.kit.model.PvPMode;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class KitValidator {

    private KitValidator() {
    }

    public static KitData validateAndSanitize(KitData input, PvPMode mode) {
        if (input == null || mode == null) {
            return new KitData(new ItemStack[36], new ItemStack[4], null);
        }

        ItemStack[] contents = input.getContents();
        ItemStack[] armor = input.getArmor();
        ItemStack offhand = input.getOffHand();

        // 1. Whitelist validation (Remove illegal items)
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && !contents[i].getType().isAir()) {
                if (!mode.isItemAllowed(contents[i])) {
                    contents[i] = null;
                }
            }
        }

        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && !armor[i].getType().isAir()) {
                if (!mode.isItemAllowed(armor[i])) {
                    armor[i] = null;
                }
            }
        }

        if (offhand != null && !offhand.getType().isAir()) {
            if (!mode.isItemAllowed(offhand)) {
                offhand = null;
            }
        }

        // 2. Max Amount validation (Clamping)
        Map<Material, Integer> materialCounts = new HashMap<>();

        // Process armor first
        for (int i = 0; i < armor.length; i++) {
            ItemStack piece = armor[i];
            if (piece != null && !piece.getType().isAir()) {
                Material mat = piece.getType();
                int limit = mode.getItemLimit(mat);
                if (limit > 0) {
                    int currentCount = materialCounts.getOrDefault(mat, 0);
                    if (currentCount + piece.getAmount() > limit) {
                        int allowed = limit - currentCount;
                        if (allowed <= 0) {
                            armor[i] = null;
                        } else {
                            piece.setAmount(allowed);
                            materialCounts.put(mat, limit);
                        }
                    } else {
                        materialCounts.put(mat, currentCount + piece.getAmount());
                    }
                }
            }
        }

        // Process offhand
        if (offhand != null && !offhand.getType().isAir()) {
            Material mat = offhand.getType();
            int limit = mode.getItemLimit(mat);
            if (limit > 0) {
                int currentCount = materialCounts.getOrDefault(mat, 0);
                if (currentCount + offhand.getAmount() > limit) {
                    int allowed = limit - currentCount;
                    if (allowed <= 0) {
                        offhand = null;
                    } else {
                        offhand.setAmount(allowed);
                        materialCounts.put(mat, limit);
                    }
                } else {
                    materialCounts.put(mat, currentCount + offhand.getAmount());
                }
            }
        }

        // Process storage & hotbar contents
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                Material mat = item.getType();
                int limit = mode.getItemLimit(mat);
                if (limit > 0) {
                    int currentCount = materialCounts.getOrDefault(mat, 0);
                    if (currentCount >= limit) {
                        contents[i] = null; // Limit already reached
                    } else if (currentCount + item.getAmount() > limit) {
                        int allowed = limit - currentCount;
                        item.setAmount(allowed);
                        materialCounts.put(mat, limit);
                    } else {
                        materialCounts.put(mat, currentCount + item.getAmount());
                    }
                }
            }
        }

        return new KitData(contents, armor, offhand);
    }
}
