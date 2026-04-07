package org.frizzlenpop.randomItem.crate;

import org.bukkit.Material;

public class CrateLootEntry {

    private final Material material;
    private final int amount;
    private final int chance;

    public CrateLootEntry(Material material, int amount, int chance) {
        this.material = material;
        this.amount = amount;
        this.chance = chance;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public int getChance() {
        return chance;
    }
}
