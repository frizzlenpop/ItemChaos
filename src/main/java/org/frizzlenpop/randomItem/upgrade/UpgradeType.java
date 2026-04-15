package org.frizzlenpop.randomItem.upgrade;

import org.bukkit.Material;

public enum UpgradeType {

    DOUBLE_DROP("Double Drop", Material.DROPPER,
            new int[]{100, 300, 600},
            new String[]{"10% chance for 2 drops", "20% chance for 2 drops", "30% chance for 2 drops"}),

    LUCKY_DROPS("Lucky Drops", Material.DIAMOND,
            new int[]{150, 400, 800},
            new String[]{"5% rare item chance", "10% rare item chance", "15% rare item chance"}),

    COIN_MULTIPLIER("Coin Multiplier", Material.GOLD_INGOT,
            new int[]{200, 500, 1000},
            new String[]{"1.5x coins", "2x coins", "3x coins"}),

    HASTE("Haste", Material.GOLDEN_PICKAXE,
            new int[]{100, 250, 500},
            new String[]{"Haste I", "Haste II", "Haste III"}),

    HEALTH_BOOST("Health Boost", Material.GOLDEN_APPLE,
            new int[]{150, 350, 700},
            new String[]{"+2 hearts", "+4 hearts", "+6 hearts"}),

    NIGHT_VISION("Night Vision", Material.ENDER_EYE,
            new int[]{100, 250, 500},
            new String[]{"Night Vision I", "Night Vision II", "Night Vision III"}),

    FIRE_RESISTANCE("Fire Resistance", Material.MAGMA_CREAM,
            new int[]{150, 350, 700},
            new String[]{"30s fire res on hit", "60s fire res on hit", "Permanent fire res"}),

    MYTHIC_HUNTER("Mythic Hunter", Material.DRAGON_HEAD,
            new int[]{500, 1000, 2000},
            new String[]{"+2% mythic chance", "+4% mythic chance", "+6% mythic chance"}),

    XP_MAGNET("XP Magnet", Material.EXPERIENCE_BOTTLE,
            new int[]{100, 300, 600},
            new String[]{"+5 XP per break", "+10 XP per break", "+20 XP per break"}),

    COIN_SHIELD("Coin Shield", Material.SHIELD,
            new int[]{200, 500, 1000},
            new String[]{"25% less coin loss", "50% less coin loss", "75% less coin loss"}),

    BLAST_MINING("Blast Mining", Material.TNT,
            new int[]{300, 700, 1500},
            new String[]{"5% chain break", "10% chain break", "15% chain break"}),

    AUTO_SMELT("Auto Smelt", Material.BLAST_FURNACE,
            new int[]{200, 500, 1000},
            new String[]{"15% smelt chance", "30% smelt chance", "50% smelt chance"}),

    MAGNET("Magnet", Material.IRON_INGOT,
            new int[]{150, 400, 800},
            new String[]{"5 block radius", "10 block radius", "15 block radius"}),

    SABOTAGE_SHIELD("Sabotage Shield", Material.TOTEM_OF_UNDYING,
            new int[]{300, 600, 1200},
            new String[]{"15% deflect chance", "30% deflect chance", "50% deflect chance"}),

    MYTHIC_AURA("Mythic Aura", Material.BEACON,
            new int[]{400, 800, 1500},
            new String[]{"50 coins nearby (5 blocks)", "100 coins nearby (10 blocks)", "200 coins nearby (15 blocks)"});

    private final String displayName;
    private final Material icon;
    private final int[] costs;
    private final String[] descriptions;
    private final int maxLevel;

    UpgradeType(String displayName, Material icon, int[] costs, String[] descriptions) {
        this.displayName = displayName;
        this.icon = icon;
        this.costs = costs;
        this.descriptions = descriptions;
        this.maxLevel = costs.length;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getDefaultMaxLevel() {
        return maxLevel;
    }

    public int getDefaultCost(int level) {
        return costs[level];
    }

    public int[] getDefaultCosts() {
        return costs.clone();
    }

    public String getDescription(int level) {
        return descriptions[level];
    }
}
