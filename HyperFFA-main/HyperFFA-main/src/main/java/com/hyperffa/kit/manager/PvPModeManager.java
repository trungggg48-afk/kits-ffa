package com.hyperffa.kit.manager;

import com.hyperffa.kit.model.ItemCategory;
import com.hyperffa.kit.model.KitData;
import com.hyperffa.kit.model.PremadeKit;
import com.hyperffa.kit.model.PvPMode;
import com.hyperffa.kit.storage.ItemDataSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class PvPModeManager {

    private final JavaPlugin plugin;
    private final Map<String, PvPMode> modes = new ConcurrentHashMap<>();
    private File modesFile;
    private FileConfiguration modesConfig;

    public PvPModeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.loadModes();
    }

    public void loadModes() {
        modes.clear();
        modesFile = new File(plugin.getDataFolder(), "modes.yml");
        if (!modesFile.exists()) {
            createDefaultModesFile();
        }
        modesConfig = YamlConfiguration.loadConfiguration(modesFile);

        ConfigurationSection section = modesConfig.getConfigurationSection("modes");
        if (section != null) {
            for (String modeKey : section.getKeys(false)) {
                ConfigurationSection modeSec = section.getConfigurationSection(modeKey);
                if (modeSec == null) continue;

                String displayName = modeSec.getString("display-name", modeKey);
                PvPMode mode = new PvPMode(modeKey, displayName);

                // Load Item Room Categories
                ConfigurationSection catSec = modeSec.getConfigurationSection("categories");
                if (catSec != null) {
                    for (String catKey : catSec.getKeys(false)) {
                        ItemCategory cat = ItemCategory.fromId(catKey);
                        if (cat != null) {
                            List<String> itemsB64 = catSec.getStringList(catKey);
                            List<ItemStack> items = new ArrayList<>();
                            for (String b64 : itemsB64) {
                                ItemStack item = ItemDataSerializer.deserializeSingleItem(b64);
                                if (item != null) items.add(item);
                            }
                            mode.setCategoryItems(cat, items);
                        }
                    }
                }

                // Load Premade Kits
                ConfigurationSection premadeSec = modeSec.getConfigurationSection("premade-kits");
                if (premadeSec != null) {
                    for (String pKey : premadeSec.getKeys(false)) {
                        ConfigurationSection pDataSec = premadeSec.getConfigurationSection(pKey);
                        if (pDataSec != null) {
                            String name = pDataSec.getString("name", pKey);
                            String cB64 = pDataSec.getString("contents", "");
                            String aB64 = pDataSec.getString("armor", "");
                            String oB64 = pDataSec.getString("offhand", "");
                            String iconB64 = pDataSec.getString("icon", "");

                            KitData kd = KitData.deserialize(cB64, aB64, oB64);
                            ItemStack icon = ItemDataSerializer.deserializeSingleItem(iconB64);
                            PremadeKit premade = new PremadeKit(name, kd, icon);
                            mode.addPremadeKit(premade);
                        }
                    }
                }

                // Load Item Limits
                ConfigurationSection limitSec = modeSec.getConfigurationSection("limits");
                if (limitSec != null) {
                    for (String matKey : limitSec.getKeys(false)) {
                        Material mat = Material.matchMaterial(matKey);
                        if (mat != null) {
                            int limit = limitSec.getInt(matKey, -1);
                            mode.setItemLimit(mat, limit);
                        }
                    }
                }

                modes.put(mode.getId(), mode);
            }
        }

        if (modes.isEmpty()) {
            createDefaultSwordMode();
        } else {
            boolean updated = false;
            for (PvPMode mode : modes.values()) {
                boolean needsPopulate = false;
                for (ItemCategory cat : ItemCategory.values()) {
                    List<ItemStack> list = mode.getCategoryItems(cat);
                    if (list == null || list.size() < 45) {
                        needsPopulate = true;
                        break;
                    }
                }
                if (needsPopulate) {
                    populateCategoryDefaults(mode);
                    updated = true;
                }
                if (mode.getId().equalsIgnoreCase("sword")) {
                    populateCategoryDefaults(mode);
                    for (Material m : new Material[]{Material.TOTEM_OF_UNDYING, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.ENDER_PEARL, Material.COBWEB}) {
                        mode.setItemLimit(m, -1);
                    }
                    KitData defaultKd = createScreenshotPremadeKitData();
                    ItemStack nSword = createEnchantedItem(Material.NETHERITE_SWORD, 1, Map.of(
                            org.bukkit.enchantments.Enchantment.SHARPNESS, 5,
                            org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2,
                            org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                            org.bukkit.enchantments.Enchantment.MENDING, 1,
                            org.bukkit.enchantments.Enchantment.SWEEPING_EDGE, 3,
                            org.bukkit.enchantments.Enchantment.LOOTING, 3
                    ));
                    PremadeKit defaultPremade = new PremadeKit("Default", defaultKd, nSword);
                    mode.addPremadeKit(defaultPremade);
                    mode.setDefaultTemplate(defaultPremade);
                    updated = true;
                }
            }
            if (updated) {
                saveModes();
            }
        }
    }

    public void saveModes() {
        if (modesConfig == null || modesFile == null) return;
        modesConfig.set("modes", null);

        for (PvPMode mode : modes.values()) {
            String path = "modes." + mode.getId();
            modesConfig.set(path + ".display-name", mode.getDisplayName());

            // Save Categories
            for (ItemCategory cat : ItemCategory.values()) {
                List<ItemStack> items = mode.getCategoryItems(cat);
                List<String> itemsB64 = new ArrayList<>();
                for (ItemStack item : items) {
                    if (item != null && !item.getType().isAir()) {
                        itemsB64.add(ItemDataSerializer.serializeSingleItem(item));
                    }
                }
                modesConfig.set(path + ".categories." + cat.getId(), itemsB64);
            }

            // Save Premade Kits
            for (PremadeKit premade : mode.getPremadeKits().values()) {
                String pPath = path + ".premade-kits." + premade.getName().toLowerCase(Locale.ROOT);
                modesConfig.set(pPath + ".name", premade.getName());
                modesConfig.set(pPath + ".contents", premade.getKitData().serializeContents());
                modesConfig.set(pPath + ".armor", premade.getKitData().serializeArmor());
                modesConfig.set(pPath + ".offhand", premade.getKitData().serializeOffhand());
                modesConfig.set(pPath + ".icon", ItemDataSerializer.serializeSingleItem(premade.getIcon()));
            }

            // Save Limits
            for (Map.Entry<Material, Integer> entry : mode.getItemLimits().entrySet()) {
                modesConfig.set(path + ".limits." + entry.getKey().name(), entry.getValue());
            }
        }

        try {
            modesConfig.save(modesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save modes.yml", e);
        }
    }

    private void createDefaultModesFile() {
        createDefaultSwordMode();
        saveModes();
    }

    private void createDefaultSwordMode() {
        PvPMode swordMode = new PvPMode("sword", "Sword PvP");
        populateCategoryDefaults(swordMode);

        KitData defaultKd = createScreenshotPremadeKitData();
        ItemStack nSword = createEnchantedItem(Material.NETHERITE_SWORD, 1, Map.of(
                org.bukkit.enchantments.Enchantment.SHARPNESS, 5,
                org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.SWEEPING_EDGE, 3,
                org.bukkit.enchantments.Enchantment.LOOTING, 3
        ));
        PremadeKit defaultPremade = new PremadeKit("Default", defaultKd, nSword);
        swordMode.addPremadeKit(defaultPremade);
        swordMode.setDefaultTemplate(defaultPremade);

        modes.put(swordMode.getId(), swordMode);
    }

    public void populateCategoryDefaults(PvPMode swordMode) {
        if (swordMode == null) return;
        for (ItemCategory cat : ItemCategory.values()) {
            swordMode.setCategoryItems(cat, new ArrayList<>());
        }

        // Helper enchanted items
        ItemStack nHelmet = createEnchantedItem(Material.NETHERITE_HELMET, 1, Map.of(
                org.bukkit.enchantments.Enchantment.PROTECTION, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.AQUA_AFFINITY, 1,
                org.bukkit.enchantments.Enchantment.RESPIRATION, 3
        ));
        ItemStack nChest = createEnchantedItem(Material.NETHERITE_CHESTPLATE, 1, Map.of(
                org.bukkit.enchantments.Enchantment.PROTECTION, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack nLegs = createEnchantedItem(Material.NETHERITE_LEGGINGS, 1, Map.of(
                org.bukkit.enchantments.Enchantment.PROTECTION, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.SWIFT_SNEAK, 3
        ));
        ItemStack nBoots = createEnchantedItem(Material.NETHERITE_BOOTS, 1, Map.of(
                org.bukkit.enchantments.Enchantment.PROTECTION, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.FEATHER_FALLING, 4,
                org.bukkit.enchantments.Enchantment.DEPTH_STRIDER, 3
        ));

        ItemStack dHelmet = createEnchantedItem(Material.DIAMOND_HELMET, 1, Map.of(
                org.bukkit.enchantments.Enchantment.PROTECTION, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.RESPIRATION, 3,
                org.bukkit.enchantments.Enchantment.AQUA_AFFINITY, 1
        ));
        ItemStack dChest = createEnchantedItem(Material.DIAMOND_CHESTPLATE, 1, Map.of(
                org.bukkit.enchantments.Enchantment.PROTECTION, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack dLegs = createEnchantedItem(Material.DIAMOND_LEGGINGS, 1, Map.of(
                org.bukkit.enchantments.Enchantment.PROTECTION, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.SWIFT_SNEAK, 3
        ));
        ItemStack dBoots = createEnchantedItem(Material.DIAMOND_BOOTS, 1, Map.of(
                org.bukkit.enchantments.Enchantment.PROTECTION, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.FEATHER_FALLING, 4,
                org.bukkit.enchantments.Enchantment.DEPTH_STRIDER, 3
        ));

        ItemStack turtleHelmet = createEnchantedItem(Material.TURTLE_HELMET, 1, Map.of(
                org.bukkit.enchantments.Enchantment.PROTECTION, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.RESPIRATION, 3,
                org.bukkit.enchantments.Enchantment.AQUA_AFFINITY, 1
        ));

        ItemStack nSword = createEnchantedItem(Material.NETHERITE_SWORD, 1, Map.of(
                org.bukkit.enchantments.Enchantment.SHARPNESS, 5,
                org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.SWEEPING_EDGE, 3,
                org.bukkit.enchantments.Enchantment.LOOTING, 3
        ));
        ItemStack dSword = createEnchantedItem(Material.DIAMOND_SWORD, 1, Map.of(
                org.bukkit.enchantments.Enchantment.SHARPNESS, 5,
                org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.SWEEPING_EDGE, 3,
                org.bukkit.enchantments.Enchantment.LOOTING, 3
        ));
        ItemStack nAxe = createEnchantedItem(Material.NETHERITE_AXE, 1, Map.of(
                org.bukkit.enchantments.Enchantment.SHARPNESS, 5,
                org.bukkit.enchantments.Enchantment.EFFICIENCY, 5,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack nPick = createEnchantedItem(Material.NETHERITE_PICKAXE, 1, Map.of(
                org.bukkit.enchantments.Enchantment.EFFICIENCY, 5,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.FORTUNE, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack nShovel = createEnchantedItem(Material.NETHERITE_SHOVEL, 1, Map.of(
                org.bukkit.enchantments.Enchantment.EFFICIENCY, 5,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack bow = createEnchantedItem(Material.BOW, 1, Map.of(
                org.bukkit.enchantments.Enchantment.POWER, 5,
                org.bukkit.enchantments.Enchantment.FLAME, 1,
                org.bukkit.enchantments.Enchantment.PUNCH, 2,
                org.bukkit.enchantments.Enchantment.INFINITY, 1,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3
        ));
        ItemStack xbow = createEnchantedItem(Material.CROSSBOW, 1, Map.of(
                org.bukkit.enchantments.Enchantment.QUICK_CHARGE, 3,
                org.bukkit.enchantments.Enchantment.MULTISHOT, 1,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack mace1 = createEnchantedItem(Material.MACE, 1, Map.of(
                org.bukkit.enchantments.Enchantment.BREACH, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack mace2 = createEnchantedItem(Material.MACE, 1, Map.of(
                org.bukkit.enchantments.Enchantment.WIND_BURST, 3,
                org.bukkit.enchantments.Enchantment.DENSITY, 5,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack trident = createEnchantedItem(Material.TRIDENT, 1, Map.of(
                org.bukkit.enchantments.Enchantment.IMPALING, 5,
                org.bukkit.enchantments.Enchantment.LOYALTY, 3,
                org.bukkit.enchantments.Enchantment.CHANNELING, 1,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack shield = createEnchantedItem(Material.SHIELD, 1, Map.of(
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack elytra = createEnchantedItem(Material.ELYTRA, 1, Map.of(
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack flintAndSteel = createEnchantedItem(Material.FLINT_AND_STEEL, 1, Map.of(
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3
        ));
        ItemStack shears = createEnchantedItem(Material.SHEARS, 1, Map.of(
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.EFFICIENCY, 5
        ));
        ItemStack fishingRod = createEnchantedItem(Material.FISHING_ROD, 1, Map.of(
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.LURE, 3,
                org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 3
        ));

        // ==========================================
        // 1. GEAR CATEGORY (45 items - Exact match to 1.png)
        // ==========================================
        // Row 0
        swordMode.addCategoryItem(ItemCategory.GEAR, nSword.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, dSword.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, nAxe.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, bow.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, turtleHelmet.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, nShovel.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, nHelmet.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, dHelmet.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, new ItemStack(Material.SPYGLASS));

        // Row 1
        swordMode.addCategoryItem(ItemCategory.GEAR, nSword.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, dSword.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, nAxe.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, xbow.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, trident.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, nPick.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, nChest.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, dChest.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, new ItemStack(Material.LAVA_BUCKET));

        // Row 2
        swordMode.addCategoryItem(ItemCategory.GEAR, nSword.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, dSword.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, nAxe.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, xbow.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, trident.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, mace1.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, nLegs.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, dLegs.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, new ItemStack(Material.POWDER_SNOW_BUCKET));

        // Row 3
        swordMode.addCategoryItem(ItemCategory.GEAR, nSword.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, dSword.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, nAxe.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, shield.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, elytra.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, new ItemStack(Material.GOAT_HORN));
        swordMode.addCategoryItem(ItemCategory.GEAR, nBoots.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, dBoots.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, nBoots.clone());

        // Row 4 (Mace 1 & Mace 2 side by side at slots 36 & 37)
        swordMode.addCategoryItem(ItemCategory.GEAR, mace1.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, mace2.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, nShovel.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, fishingRod.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, flintAndSteel.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, shears.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, nLegs.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, dLegs.clone());
        swordMode.addCategoryItem(ItemCategory.GEAR, dBoots.clone());

        // ==========================================
        // 2. POTIONS CATEGORY (45 items - Exact match to 2.png)
        // ==========================================
        // 5 rows of 7 splash potions + 2 tipped arrows
        org.bukkit.potion.PotionType[] rowPotions = new org.bukkit.potion.PotionType[]{
                org.bukkit.potion.PotionType.STRONG_SWIFTNESS,
                org.bukkit.potion.PotionType.LONG_WATER_BREATHING,
                org.bukkit.potion.PotionType.STRONG_HEALING,
                org.bukkit.potion.PotionType.STRONG_STRENGTH,
                org.bukkit.potion.PotionType.LONG_FIRE_RESISTANCE,
                org.bukkit.potion.PotionType.LONG_SLOW_FALLING,
                org.bukkit.potion.PotionType.STRONG_TURTLE_MASTER
        };

        ItemStack[][] arrowCols = new ItemStack[][]{
                { new ItemStack(Material.ARROW, 64), new ItemStack(Material.SPECTRAL_ARROW, 64) },
                { createTippedArrow(org.bukkit.potion.PotionType.LONG_WEAKNESS, 64), createTippedArrow(org.bukkit.potion.PotionType.LONG_SLOW_FALLING, 64) },
                { createTippedArrow(org.bukkit.potion.PotionType.STRONG_HARMING, 64), createTippedArrow(org.bukkit.potion.PotionType.STRONG_SLOWNESS, 64) },
                { createTippedArrow(org.bukkit.potion.PotionType.STRONG_HEALING, 64), createTippedArrow(org.bukkit.potion.PotionType.STRONG_STRENGTH, 64) },
                { createTippedArrow(org.bukkit.potion.PotionType.STRONG_POISON, 64), createTippedArrow(org.bukkit.potion.PotionType.STRONG_SWIFTNESS, 64) }
        };

        for (int r = 0; r < 5; r++) {
            for (org.bukkit.potion.PotionType pt : rowPotions) {
                swordMode.addCategoryItem(ItemCategory.POTIONS, createSplashPotion(pt));
            }
            swordMode.addCategoryItem(ItemCategory.POTIONS, arrowCols[r][0]);
            swordMode.addCategoryItem(ItemCategory.POTIONS, arrowCols[r][1]);
        }

        // ==========================================
        // 3. CONSUMABLES CATEGORY (45 items - Exact match to 3.png)
        // ==========================================
        // Row 0
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.ENDER_PEARL, 16));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.ENDER_PEARL, 16));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.ENDER_PEARL, 16));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.SUGAR, 64));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.MILK_BUCKET));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.FIREWORK_ROCKET, 64));

        // Row 1
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.ENDER_PEARL, 16));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.ENDER_PEARL, 16));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.ENDER_PEARL, 16));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.OBSIDIAN, 64));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.MILK_BUCKET));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.FIREWORK_ROCKET, 64));

        // Row 2
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.ENDER_PEARL, 16));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.ENDER_PEARL, 16));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.ENDER_PEARL, 16));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.GOLDEN_APPLE, 64));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.GOLDEN_APPLE, 64));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.CHORUS_FRUIT, 64));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.COBWEB, 64));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.MILK_BUCKET));
        swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.FIREWORK_ROCKET, 64));

        // Row 3 & 4 (18 Totems)
        for (int i = 0; i < 18; i++) {
            swordMode.addCategoryItem(ItemCategory.CONSUMABLES, new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        }

        // ==========================================
        // 4. MISCELLANEOUS CATEGORY (45 items - Exact match to 4.png with user replacements)
        // ==========================================
        // Row 0
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.WATER_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.WATER_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.WATER_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.POWDER_SNOW_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.POWDER_SNOW_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.LAVA_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.LAVA_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.LAVA_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.MUSHROOM_STEW, 1));

        // Row 1
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.WATER_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.WATER_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.WATER_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.POWDER_SNOW_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.POWDER_SNOW_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.LAVA_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.LAVA_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.LAVA_BUCKET));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.RAIL, 64));

        // Row 2
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.COBBLESTONE, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.OAK_TRAPDOOR, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.SPRUCE_TRAPDOOR, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.COBWEB, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.HONEY_BLOCK, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.HONEY_BOTTLE, 16));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.ARMOR_STAND, 16));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 1));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));

        // Row 3
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.COBBLESTONE, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.OAK_TRAPDOOR, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.SPRUCE_TRAPDOOR, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.OAK_PLANKS, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.HONEY_BLOCK, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.SLIME_BLOCK, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.BUNDLE, 1));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, 1));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, 1));

        // Row 4
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.LIGHT_BLUE_CONCRETE, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.LIME_CONCRETE, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.YELLOW_CONCRETE, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.ORANGE_CONCRETE, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.RED_CONCRETE, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.PINK_CONCRETE, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.PURPLE_CONCRETE, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.BROWN_CONCRETE, 64));
        swordMode.addCategoryItem(ItemCategory.MISCELLANEOUS, new ItemStack(Material.BLACK_CONCRETE, 64));

        // ==========================================
        // 5. EXPLOSIVES CATEGORY (45 items - Exact match to 5.png with user replacements)
        // ==========================================
        // Row 0: Minerals in exact order: Dong (Copper), Sat (Iron), Bac (Diamond), Vang (Gold), Luc Bao (Emerald), Luu Ly (Lapis), Netherite (Netherite), Redstone, Coal (All 64 stacks)
        swordMode.addCategoryItem(ItemCategory.EXPLOSIONS, new ItemStack(Material.COPPER_INGOT, 64));
        swordMode.addCategoryItem(ItemCategory.EXPLOSIONS, new ItemStack(Material.IRON_INGOT, 64));
        swordMode.addCategoryItem(ItemCategory.EXPLOSIONS, new ItemStack(Material.DIAMOND, 64));
        swordMode.addCategoryItem(ItemCategory.EXPLOSIONS, new ItemStack(Material.GOLD_INGOT, 64));
        swordMode.addCategoryItem(ItemCategory.EXPLOSIONS, new ItemStack(Material.EMERALD, 64));
        swordMode.addCategoryItem(ItemCategory.EXPLOSIONS, new ItemStack(Material.LAPIS_LAZULI, 64));
        swordMode.addCategoryItem(ItemCategory.EXPLOSIONS, new ItemStack(Material.NETHERITE_INGOT, 64));
        swordMode.addCategoryItem(ItemCategory.EXPLOSIONS, new ItemStack(Material.REDSTONE, 64));
        swordMode.addCategoryItem(ItemCategory.EXPLOSIONS, new ItemStack(Material.COAL, 64));

        // Row 1: Obsidian
        for (int i = 0; i < 9; i++) {
            swordMode.addCategoryItem(ItemCategory.EXPLOSIONS, new ItemStack(Material.OBSIDIAN, 64));
        }
        // Row 2: Powered Rails (Replaced anchor)
        for (int i = 0; i < 9; i++) {
            swordMode.addCategoryItem(ItemCategory.EXPLOSIONS, new ItemStack(Material.POWERED_RAIL, 64));
        }
        // Row 3: Glowstone
        for (int i = 0; i < 9; i++) {
            swordMode.addCategoryItem(ItemCategory.EXPLOSIONS, new ItemStack(Material.GLOWSTONE, 64));
        }
        // Row 4: TNT Minecarts
        for (int i = 0; i < 9; i++) {
            swordMode.addCategoryItem(ItemCategory.EXPLOSIONS, new ItemStack(Material.TNT_MINECART, 1));
        }
    }

    public KitData createScreenshotPremadeKitData() {
        ItemStack nHelmet = createEnchantedItem(Material.NETHERITE_HELMET, 1, Map.of(
                org.bukkit.enchantments.Enchantment.PROTECTION, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.AQUA_AFFINITY, 1,
                org.bukkit.enchantments.Enchantment.RESPIRATION, 3
        ));
        ItemStack nChest = createEnchantedItem(Material.NETHERITE_CHESTPLATE, 1, Map.of(
                org.bukkit.enchantments.Enchantment.PROTECTION, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack nLegs = createEnchantedItem(Material.NETHERITE_LEGGINGS, 1, Map.of(
                org.bukkit.enchantments.Enchantment.PROTECTION, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.SWIFT_SNEAK, 3
        ));
        ItemStack nBoots = createEnchantedItem(Material.NETHERITE_BOOTS, 1, Map.of(
                org.bukkit.enchantments.Enchantment.PROTECTION, 4,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.FEATHER_FALLING, 4,
                org.bukkit.enchantments.Enchantment.DEPTH_STRIDER, 3
        ));

        ItemStack nSword = createEnchantedItem(Material.NETHERITE_SWORD, 1, Map.of(
                org.bukkit.enchantments.Enchantment.SHARPNESS, 5,
                org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1,
                org.bukkit.enchantments.Enchantment.SWEEPING_EDGE, 3,
                org.bukkit.enchantments.Enchantment.LOOTING, 3
        ));
        ItemStack nAxe = createEnchantedItem(Material.NETHERITE_AXE, 1, Map.of(
                org.bukkit.enchantments.Enchantment.SHARPNESS, 5,
                org.bukkit.enchantments.Enchantment.EFFICIENCY, 5,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack nPick = createEnchantedItem(Material.NETHERITE_PICKAXE, 1, Map.of(
                org.bukkit.enchantments.Enchantment.EFFICIENCY, 5,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.FORTUNE, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack nShovel = createEnchantedItem(Material.NETHERITE_SHOVEL, 1, Map.of(
                org.bukkit.enchantments.Enchantment.EFFICIENCY, 5,
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack shield = createEnchantedItem(Material.SHIELD, 1, Map.of(
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));
        ItemStack elytra = createEnchantedItem(Material.ELYTRA, 1, Map.of(
                org.bukkit.enchantments.Enchantment.UNBREAKING, 3,
                org.bukkit.enchantments.Enchantment.MENDING, 1
        ));

        ItemStack[] contents = new ItemStack[36];
        // Hotbar (0 to 8)
        contents[0] = nSword;
        contents[1] = new ItemStack(Material.ARROW, 64);
        contents[2] = new ItemStack(Material.GOLDEN_APPLE, 64);
        contents[3] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[4] = new ItemStack(Material.EXPERIENCE_BOTTLE, 64);
        contents[5] = new ItemStack(Material.ARROW, 64);
        contents[6] = nPick;
        contents[7] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[8] = new ItemStack(Material.ENDER_PEARL, 16);

        // Row 1 (9 to 17)
        contents[9] = shield;
        contents[10] = new ItemStack(Material.WIND_CHARGE, 64);
        contents[11] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[12] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[13] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[14] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[15] = new ItemStack(Material.ENDER_PEARL, 16);
        contents[16] = new ItemStack(Material.EXPERIENCE_BOTTLE, 64);
        contents[17] = new ItemStack(Material.EXPERIENCE_BOTTLE, 64);

        // Row 2 (18 to 26)
        contents[18] = nAxe;
        contents[19] = elytra;
        contents[20] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[21] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[22] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[23] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[24] = new ItemStack(Material.ENDER_PEARL, 16);
        contents[25] = new ItemStack(Material.EXPERIENCE_BOTTLE, 64);
        contents[26] = new ItemStack(Material.OBSIDIAN, 64);

        // Row 3 (27 to 35)
        contents[27] = nShovel;
        contents[28] = createEnchantedItem(Material.FLINT_AND_STEEL, 1, Map.of(org.bukkit.enchantments.Enchantment.UNBREAKING, 3));
        contents[29] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[30] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[31] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[32] = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        contents[33] = new ItemStack(Material.ENDER_PEARL, 16);
        contents[34] = new ItemStack(Material.ENDER_PEARL, 16);
        contents[35] = new ItemStack(Material.ENDER_PEARL, 16);

        ItemStack[] armor = new ItemStack[]{
                nBoots,
                nLegs,
                nChest,
                nHelmet
        };
        ItemStack offhand = new ItemStack(Material.TOTEM_OF_UNDYING, 1);

        return new KitData(contents, armor, offhand);
    }

    private ItemStack createEnchantedItem(Material material, int amount, Map<org.bukkit.enchantments.Enchantment, Integer> enchants) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchants.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSplashPotion(org.bukkit.potion.PotionType type) {
        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(type);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTippedArrow(org.bukkit.potion.PotionType type, int amount) {
        ItemStack item = new ItemStack(Material.TIPPED_ARROW, amount);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(type);
            item.setItemMeta(meta);
        }
        return item;
    }

    public PvPMode getMode(String id) {
        if (id == null) return null;
        return modes.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<PvPMode> getAllModes() {
        return Collections.unmodifiableCollection(modes.values());
    }

    public void registerMode(PvPMode mode) {
        if (mode == null) return;
        modes.put(mode.getId(), mode);
        saveModes();
    }

    public boolean deleteMode(String id) {
        if (id == null) return false;
        PvPMode removed = modes.remove(id.toLowerCase(Locale.ROOT));
        if (removed != null) {
            saveModes();
            return true;
        }
        return false;
    }
}
