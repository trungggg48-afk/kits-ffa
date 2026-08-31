package com.hyperffa.kit.gui.framework;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@SuppressFBWarnings({"CT_CONSTRUCTOR_THROW"})
public abstract class BaseGui {

    protected final Player player;
    protected final int size;
    protected final Component title;
    protected final Inventory inventory;
    protected final Map<Integer, GuiButton> buttons = new HashMap<>();

    public BaseGui(Player player, int size, Component title) {
        this.player = player;
        this.size = size;
        this.title = title;
        GuiHolder holder = new GuiHolder(this);
        this.inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);
    }

    public abstract void build();

    public void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        GuiButton button = new GuiButton(item, action);
        buttons.put(slot, button);
        inventory.setItem(slot, item);
    }

    public void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    public void fillBorders(ItemStack borderItem) {
        int rows = size / 9;
        for (int c = 0; c < 9; c++) {
            setItem(c, borderItem);
            setItem((rows - 1) * 9 + c, borderItem);
        }
        for (int r = 1; r < rows - 1; r++) {
            setItem(r * 9, borderItem);
            setItem(r * 9 + 8, borderItem);
        }
    }

    public void fillBackground(ItemStack filler) {
        for (int i = 0; i < size; i++) {
            if (!buttons.containsKey(i)) {
                setItem(i, filler);
            }
        }
    }

    public static ItemStack createItem(Material mat, Component name, Component... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.displayName(name);
            if (lore != null && lore.length > 0) {
                meta.lore(java.util.Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open() {
        build();
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < size) {
            GuiButton button = buttons.get(rawSlot);
            if (button != null) {
                button.onClick(event);
            }
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    public void handleClose(InventoryCloseEvent event) {
        // Subclasses can override for specific close actions
    }

    public Player getPlayer() {
        return player;
    }

    public Inventory getInventory() {
        return inventory;
    }
}
