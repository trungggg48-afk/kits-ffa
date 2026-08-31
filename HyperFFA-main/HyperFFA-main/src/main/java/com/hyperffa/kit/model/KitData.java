package com.hyperffa.kit.model;

import com.hyperffa.kit.storage.ItemDataSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.Objects;

public class KitData {

    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack offHand;

    public KitData(ItemStack[] contents, ItemStack[] armor, ItemStack offHand) {
        this.contents = contents != null ? Arrays.copyOf(contents, contents.length) : new ItemStack[36];
        this.armor = armor != null ? Arrays.copyOf(armor, armor.length) : new ItemStack[4];
        this.offHand = offHand != null ? offHand.clone() : null;
    }

    public static KitData fromPlayer(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] storage = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            storage[i] = (item != null && !item.getType().isAir()) ? item.clone() : null;
        }

        ItemStack[] armor = new ItemStack[4];
        ItemStack[] currentArmor = inv.getArmorContents();
        for (int i = 0; i < 4; i++) {
            ItemStack piece = (i < currentArmor.length) ? currentArmor[i] : null;
            armor[i] = (piece != null && !piece.getType().isAir()) ? piece.clone() : null;
        }

        ItemStack offhand = inv.getItemInOffHand();
        ItemStack offhandClone = !offhand.getType().isAir() ? offhand.clone() : null;

        return new KitData(storage, armor, offhandClone);
    }

    public void applyToPlayer(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();

        for (int i = 0; i < Math.min(36, contents.length); i++) {
            inv.setItem(i, (contents[i] != null && !contents[i].getType().isAir()) ? contents[i].clone() : null);
        }

        ItemStack[] armorClones = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            armorClones[i] = (armor != null && i < armor.length && armor[i] != null && !armor[i].getType().isAir()) ? armor[i].clone() : null;
        }
        inv.setArmorContents(armorClones);

        inv.setItemInOffHand((offHand != null && !offHand.getType().isAir()) ? offHand.clone() : null);
        player.updateInventory();
    }

    public ItemStack[] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    public ItemStack[] getArmor() {
        return Arrays.copyOf(armor, armor.length);
    }

    public ItemStack getOffHand() {
        return offHand != null ? offHand.clone() : null;
    }

    public String serializeContents() {
        return ItemDataSerializer.serializeItemArray(contents);
    }

    public String serializeArmor() {
        return ItemDataSerializer.serializeItemArray(armor);
    }

    public String serializeOffhand() {
        return ItemDataSerializer.serializeSingleItem(offHand);
    }

    public static KitData deserialize(String contentsB64, String armorB64, String offhandB64) {
        ItemStack[] c = ItemDataSerializer.deserializeItemArray(contentsB64);
        ItemStack[] a = ItemDataSerializer.deserializeItemArray(armorB64);
        ItemStack o = ItemDataSerializer.deserializeSingleItem(offhandB64);
        return new KitData(c, a, o);
    }

    public boolean isEmpty() {
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) return false;
        }
        for (ItemStack item : armor) {
            if (item != null && !item.getType().isAir()) return false;
        }
        return offHand == null || offHand.getType().isAir();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KitData kitData = (KitData) o;
        return Arrays.equals(contents, kitData.contents) &&
                Arrays.equals(armor, kitData.armor) &&
                Objects.equals(offHand, kitData.offHand);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(offHand);
        result = 31 * result + Arrays.hashCode(contents);
        result = 31 * result + Arrays.hashCode(armor);
        return result;
    }
}
