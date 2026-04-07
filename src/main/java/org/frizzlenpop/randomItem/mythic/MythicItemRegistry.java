package org.frizzlenpop.randomItem.mythic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.frizzlenpop.randomItem.RandomItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MythicItemRegistry {

    private static final Component MYTHIC_TAG = Component.text("MYTHIC", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false);

    private final List<ItemStack> mythicItems = new ArrayList<>();
    private boolean enabled;
    private int dropChanceInLegendary;
    private int mythicTierBlocks;

    public MythicItemRegistry(RandomItem plugin) {
        plugin.saveResource("mythic.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "mythic.yml"));

        this.enabled = config.getBoolean("enabled", true);
        this.dropChanceInLegendary = config.getInt("drop-chance-in-legendary", 5);
        this.mythicTierBlocks = config.getInt("mythic-tier-blocks", 2000);

        buildAllItems();
    }

    public boolean isEnabled() { return enabled; }
    public int getDropChanceInLegendary() { return dropChanceInLegendary; }
    public int getMythicTierBlocks() { return mythicTierBlocks; }

    public ItemStack getRandomMythicItem() {
        if (mythicItems.isEmpty()) return new ItemStack(Material.DIAMOND_SWORD);
        return mythicItems.get(ThreadLocalRandom.current().nextInt(mythicItems.size())).clone();
    }

    public List<ItemStack> getAllItems() {
        return mythicItems;
    }

    private void buildAllItems() {
        // ===== WEAPONS (10) =====
        mythicItems.add(buildItem(Material.NETHERITE_SWORD, "&d&lGodslayer", "The blade that ended gods",
                e(Enchantment.SHARPNESS, 5), e(Enchantment.FIRE_ASPECT, 2), e(Enchantment.LOOTING, 3),
                e(Enchantment.KNOCKBACK, 2), e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_SWORD, "&5&lVoid Reaper", "Harvests souls from the void",
                e(Enchantment.SHARPNESS, 5), e(Enchantment.SWEEPING_EDGE, 3), e(Enchantment.SMITE, 5),
                e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.DIAMOND_SWORD, "&b&lThunderfang", "Crackles with lightning",
                e(Enchantment.SHARPNESS, 5), e(Enchantment.FIRE_ASPECT, 2), e(Enchantment.KNOCKBACK, 2),
                e(Enchantment.LOOTING, 3), e(Enchantment.UNBREAKING, 3)));

        mythicItems.add(buildItem(Material.NETHERITE_SWORD, "&4&lSoul Drinker", "Drains the life of its victims",
                e(Enchantment.SHARPNESS, 5), e(Enchantment.LOOTING, 3), e(Enchantment.MENDING, 1),
                e(Enchantment.UNBREAKING, 3), e(Enchantment.FIRE_ASPECT, 2)));

        mythicItems.add(buildItem(Material.DIAMOND_SWORD, "&f&lFrostbite", "Frozen to the core",
                e(Enchantment.SHARPNESS, 5), e(Enchantment.KNOCKBACK, 2), e(Enchantment.SWEEPING_EDGE, 3),
                e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_SWORD, "&8&lWarden's Fang", "Forged in the deep dark",
                e(Enchantment.SHARPNESS, 5), e(Enchantment.SMITE, 5), e(Enchantment.LOOTING, 3),
                e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.DIAMOND_SWORD, "&5&lEnder Blade", "Tears through dimensions",
                e(Enchantment.SHARPNESS, 5), e(Enchantment.LOOTING, 3), e(Enchantment.KNOCKBACK, 2),
                e(Enchantment.UNBREAKING, 3), e(Enchantment.FIRE_ASPECT, 2)));

        mythicItems.add(buildItem(Material.NETHERITE_AXE, "&c&lBerserker Axe", "Unstoppable fury",
                e(Enchantment.SHARPNESS, 5), e(Enchantment.EFFICIENCY, 5), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_AXE, "&4&lExecutioner", "One swing, one kill",
                e(Enchantment.SHARPNESS, 5), e(Enchantment.SMITE, 5), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.DIAMOND_AXE, "&e&lStorm Breaker", "Splits the sky",
                e(Enchantment.SHARPNESS, 5), e(Enchantment.EFFICIENCY, 5), e(Enchantment.KNOCKBACK, 2),
                e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));

        // ===== HELMETS (5) =====
        mythicItems.add(buildItem(Material.NETHERITE_HELMET, "&1&lCrown of the Abyss", "See through the darkness",
                e(Enchantment.PROTECTION, 4), e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1),
                e(Enchantment.RESPIRATION, 3), e(Enchantment.AQUA_AFFINITY, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_HELMET, "&6&lDragon Helm", "Scales of the Ender Dragon",
                e(Enchantment.PROTECTION, 4), e(Enchantment.FIRE_PROTECTION, 4), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.DIAMOND_HELMET, "&8&lWarden's Gaze", "Eyes of the sculk",
                e(Enchantment.PROTECTION, 4), e(Enchantment.THORNS, 3), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1), e(Enchantment.RESPIRATION, 3)));

        mythicItems.add(buildItem(Material.NETHERITE_HELMET, "&7&lPhantom Visor", "Untouchable by arrows",
                e(Enchantment.PROJECTILE_PROTECTION, 4), e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1),
                e(Enchantment.RESPIRATION, 3)));

        mythicItems.add(buildItem(Material.NETHERITE_HELMET, "&3&lTitan's Crown", "Withstands explosions",
                e(Enchantment.BLAST_PROTECTION, 4), e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1),
                e(Enchantment.AQUA_AFFINITY, 1)));

        // ===== CHESTPLATES (5) =====
        mythicItems.add(buildItem(Material.NETHERITE_CHESTPLATE, "&6&lImmortal Plate", "Cannot be broken",
                e(Enchantment.PROTECTION, 4), e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1),
                e(Enchantment.THORNS, 3)));

        mythicItems.add(buildItem(Material.NETHERITE_CHESTPLATE, "&c&lDragon Scale", "Fireproof scales",
                e(Enchantment.PROTECTION, 4), e(Enchantment.FIRE_PROTECTION, 4), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_CHESTPLATE, "&5&lVoid Armor", "Absorbs all impact",
                e(Enchantment.BLAST_PROTECTION, 4), e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1),
                e(Enchantment.THORNS, 3)));

        mythicItems.add(buildItem(Material.DIAMOND_CHESTPLATE, "&b&lGuardian Core", "Deflects all projectiles",
                e(Enchantment.PROTECTION, 4), e(Enchantment.PROJECTILE_PROTECTION, 4), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_CHESTPLATE, "&4&lWarlord's Aegis", "Worn by ancient warlords",
                e(Enchantment.PROTECTION, 4), e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));

        // ===== LEGGINGS (5) =====
        mythicItems.add(buildItem(Material.NETHERITE_LEGGINGS, "&1&lAbyssal Greaves", "From the ocean floor",
                e(Enchantment.PROTECTION, 4), e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1),
                e(Enchantment.THORNS, 3)));

        mythicItems.add(buildItem(Material.NETHERITE_LEGGINGS, "&6&lDragon Legs", "Scales of fire",
                e(Enchantment.PROTECTION, 4), e(Enchantment.FIRE_PROTECTION, 4), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.DIAMOND_LEGGINGS, "&7&lSpectral Leggings", "Move like a ghost",
                e(Enchantment.PROTECTION, 4), e(Enchantment.SWIFT_SNEAK, 3), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_LEGGINGS, "&3&lTitan's Guard", "Blast resistant",
                e(Enchantment.BLAST_PROTECTION, 4), e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_LEGGINGS, "&8&lShadow Greaves", "Arrow-proof legs",
                e(Enchantment.PROTECTION, 4), e(Enchantment.PROJECTILE_PROTECTION, 4), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        // ===== BOOTS (5) =====
        mythicItems.add(buildItem(Material.NETHERITE_BOOTS, "&e&lHermes' Stride", "Speed of the gods",
                e(Enchantment.PROTECTION, 4), e(Enchantment.FEATHER_FALLING, 4), e(Enchantment.DEPTH_STRIDER, 3),
                e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1), e(Enchantment.SOUL_SPEED, 3)));

        mythicItems.add(buildItem(Material.NETHERITE_BOOTS, "&f&lFrost Walker Boots", "Freeze the ocean",
                e(Enchantment.PROTECTION, 4), e(Enchantment.FROST_WALKER, 2), e(Enchantment.FEATHER_FALLING, 4),
                e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_BOOTS, "&6&lDragon Treads", "Walk through fire",
                e(Enchantment.FIRE_PROTECTION, 4), e(Enchantment.FEATHER_FALLING, 4), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1), e(Enchantment.SOUL_SPEED, 3)));

        mythicItems.add(buildItem(Material.DIAMOND_BOOTS, "&3&lTitan's March", "Unstoppable movement",
                e(Enchantment.PROTECTION, 4), e(Enchantment.FEATHER_FALLING, 4), e(Enchantment.DEPTH_STRIDER, 3),
                e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_BOOTS, "&5&lVoid Walkers", "Step between worlds",
                e(Enchantment.PROTECTION, 4), e(Enchantment.FEATHER_FALLING, 4), e(Enchantment.FROST_WALKER, 2),
                e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));

        // ===== TOOLS (10) =====
        mythicItems.add(buildItem(Material.NETHERITE_PICKAXE, "&6&lWorld Eater", "Devours mountains",
                e(Enchantment.EFFICIENCY, 5), e(Enchantment.FORTUNE, 3), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_PICKAXE, "&b&lDrill of the Ancients", "Precision mining",
                e(Enchantment.EFFICIENCY, 5), e(Enchantment.SILK_TOUCH, 1), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.DIAMOND_PICKAXE, "&c&lMolten Pick", "Burns with inner fire",
                e(Enchantment.EFFICIENCY, 5), e(Enchantment.FORTUNE, 3), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_SHOVEL, "&a&lTerra Shatter", "Moves earth itself",
                e(Enchantment.EFFICIENCY, 5), e(Enchantment.FORTUNE, 3), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_SHOVEL, "&2&lEarth Splitter", "Silk-smooth excavation",
                e(Enchantment.EFFICIENCY, 5), e(Enchantment.SILK_TOUCH, 1), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_AXE, "&6&lLumber Lord", "Forests bow before you",
                e(Enchantment.EFFICIENCY, 5), e(Enchantment.FORTUNE, 3), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_AXE, "&2&lNature's Wrath", "Harvests with precision",
                e(Enchantment.EFFICIENCY, 5), e(Enchantment.SILK_TOUCH, 1), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_HOE, "&5&lVoid Hoe", "Reaps across dimensions",
                e(Enchantment.EFFICIENCY, 5), e(Enchantment.FORTUNE, 3), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.NETHERITE_PICKAXE, "&e&lStar Breaker", "Mines the stars",
                e(Enchantment.EFFICIENCY, 5), e(Enchantment.FORTUNE, 3), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.DIAMOND_PICKAXE, "&b&lCrystal Drill", "Perfect extraction",
                e(Enchantment.EFFICIENCY, 5), e(Enchantment.SILK_TOUCH, 1), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        // ===== RANGED (5) =====
        mythicItems.add(buildItem(Material.BOW, "&e&lArtemis' Bow", "Never misses its mark",
                e(Enchantment.POWER, 5), e(Enchantment.FLAME, 1), e(Enchantment.INFINITY, 1),
                e(Enchantment.PUNCH, 2), e(Enchantment.UNBREAKING, 3)));

        mythicItems.add(buildItem(Material.BOW, "&5&lVoid Sniper", "Shots from another dimension",
                e(Enchantment.POWER, 5), e(Enchantment.PUNCH, 2), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.BOW, "&c&lDragon Breath Bow", "Arrows of dragonfire",
                e(Enchantment.POWER, 5), e(Enchantment.FLAME, 1), e(Enchantment.PUNCH, 2),
                e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.CROSSBOW, "&e&lThundershot", "Rapid multi-fire",
                e(Enchantment.QUICK_CHARGE, 3), e(Enchantment.MULTISHOT, 1), e(Enchantment.PIERCING, 4),
                e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.CROSSBOW, "&8&lWarden's Fury", "Pierces all defenses",
                e(Enchantment.QUICK_CHARGE, 3), e(Enchantment.PIERCING, 4), e(Enchantment.UNBREAKING, 3),
                e(Enchantment.MENDING, 1)));

        // ===== UTILITY/SPECIAL (5) =====
        mythicItems.add(buildItem(Material.TOTEM_OF_UNDYING, "&6&lTotem of Infinite Lives", "Death itself fears this totem", 1));

        mythicItems.add(buildItem(Material.ENDER_PEARL, "&5&lEnder King's Pearl", "Teleport with royal precision", 16));

        mythicItems.add(buildItem(Material.ENCHANTED_GOLDEN_APPLE, "&d&lGolden Feast", "A feast fit for legends", 4));

        mythicItems.add(buildItem(Material.TRIDENT, "&b&lGod Trident", "Command the seas and storms",
                e(Enchantment.LOYALTY, 3), e(Enchantment.CHANNELING, 1), e(Enchantment.IMPALING, 5),
                e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));

        mythicItems.add(buildItem(Material.ELYTRA, "&5&lElytra of the Void", "Wings woven from the End",
                e(Enchantment.UNBREAKING, 3), e(Enchantment.MENDING, 1)));
    }

    @SuppressWarnings("deprecation")
    private ItemStack buildItem(Material material, String name, String loreText, EnchantEntry... enchants) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String coloredName = org.bukkit.ChatColor.translateAlternateColorCodes('&', name);
        meta.displayName(Component.text(coloredName)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(MYTHIC_TAG);
        lore.add(Component.empty());
        lore.add(Component.text(loreText, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);

        for (EnchantEntry enchant : enchants) {
            meta.addEnchant(enchant.enchantment(), enchant.level(), true);
        }

        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    private ItemStack buildItem(Material material, String name, String loreText, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        String coloredName = org.bukkit.ChatColor.translateAlternateColorCodes('&', name);
        meta.displayName(Component.text(coloredName)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(MYTHIC_TAG);
        lore.add(Component.empty());
        lore.add(Component.text(loreText, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static EnchantEntry e(Enchantment enchantment, int level) {
        return new EnchantEntry(enchantment, level);
    }

    private record EnchantEntry(Enchantment enchantment, int level) {}
}
