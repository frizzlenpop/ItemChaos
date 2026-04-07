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
            new String[]{"+2 hearts", "+4 hearts", "+6 hearts"});

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

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getCost(int level) {
        return costs[level];
    }

    public String getDescription(int level) {
        return descriptions[level];
    }
}
