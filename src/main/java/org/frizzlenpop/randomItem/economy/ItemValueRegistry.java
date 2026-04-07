package org.frizzlenpop.randomItem.economy;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public final class ItemValueRegistry {

    private static final Map<Material, Integer> VALUES = new HashMap<>();

    private static final String[] LEGENDARY_KEYWORDS = {
            "NETHERITE", "DRAGON", "BEACON", "ELYTRA", "NETHER_STAR",
            "TOTEM", "TRIDENT", "END_CRYSTAL", "CONDUIT", "HEART_OF_THE_SEA",
            "ENCHANTED_GOLDEN_APPLE"
    };

    private static final String[] RARE_KEYWORDS = {
            "DIAMOND", "EMERALD", "SHULKER", "ENCHANTED_BOOK",
            "WITHER_SKELETON_SKULL", "MUSIC_DISC", "LODESTONE",
            "RESPAWN_ANCHOR", "ENDER_PEARL", "BLAZE_ROD",
            "GHAST_TEAR", "EXPERIENCE_BOTTLE", "GOLDEN_APPLE"
    };

    private static final String[] UNCOMMON_KEYWORDS = {
            "IRON", "GOLD", "LAPIS", "REDSTONE", "AMETHYST",
            "COPPER", "QUARTZ", "PRISMARINE", "OBSIDIAN", "ANVIL",
            "BREWING", "ENDER_EYE", "BOOK", "SADDLE", "NAME_TAG"
    };

    private static final String[] COMMON_KEYWORDS = {
            "STONE", "WOOD", "OAK", "SPRUCE", "BIRCH", "JUNGLE",
            "ACACIA", "DARK_OAK", "MANGROVE", "CHERRY", "BAMBOO",
            "COAL", "LEATHER", "WOOL", "TERRACOTTA", "BRICK",
            "GLASS", "SAND", "GRAVEL", "CLAY"
    };

    static {
        for (Material mat : Material.values()) {
            if (!mat.isItem() || mat.isAir()) continue;
            VALUES.put(mat, classify(mat));
        }
    }

    private ItemValueRegistry() {
    }

    private static int classify(Material mat) {
        String name = mat.name();
        for (String keyword : LEGENDARY_KEYWORDS) {
            if (name.contains(keyword)) return 50;
        }
        for (String keyword : RARE_KEYWORDS) {
            if (name.contains(keyword)) return 20;
        }
        for (String keyword : UNCOMMON_KEYWORDS) {
            if (name.contains(keyword)) return 8;
        }
        for (String keyword : COMMON_KEYWORDS) {
            if (name.contains(keyword)) return 3;
        }
        return 1;
    }

    public static int getValue(Material mat) {
        return VALUES.getOrDefault(mat, 1);
    }
}
