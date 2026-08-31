package com.hyperffa.kit.storage;

import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.Base64;

public final class ItemDataSerializer {

    private ItemDataSerializer() {
    }

    public static String serializeItemArray(ItemStack[] items) {
        if (items == null || items.length == 0) {
            return "";
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeInt(items.length);
            for (ItemStack item : items) {
                if (item == null || item.getType().isAir()) {
                    dos.writeInt(0);
                } else {
                    byte[] bytes = item.serializeAsBytes();
                    dos.writeInt(bytes.length);
                    dos.write(bytes);
                }
            }
            dos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize ItemStack array", e);
        }
    }

    public static ItemStack[] deserializeItemArray(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return new ItemStack[0];
        }
        try {
            byte[] rawBytes = Base64.getDecoder().decode(base64);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(rawBytes);
                 DataInputStream dis = new DataInputStream(bais)) {

                int length = dis.readInt();
                ItemStack[] items = new ItemStack[length];

                for (int i = 0; i < length; i++) {
                    int byteLength = dis.readInt();
                    if (byteLength == 0) {
                        items[i] = null;
                    } else {
                        byte[] itemBytes = new byte[byteLength];
                        dis.readFully(itemBytes);
                        items[i] = ItemStack.deserializeBytes(itemBytes);
                    }
                }
                return items;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize ItemStack array", e);
        }
    }

    public static String serializeSingleItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }
        byte[] bytes = item.serializeAsBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static ItemStack deserializeSingleItem(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        byte[] bytes = Base64.getDecoder().decode(base64);
        return ItemStack.deserializeBytes(bytes);
    }
}
