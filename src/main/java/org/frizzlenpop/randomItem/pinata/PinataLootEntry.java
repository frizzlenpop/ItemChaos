package org.frizzlenpop.randomItem.pinata;

import org.bukkit.Material;

public class PinataLootEntry {
    private final Material material;
    private final int amount;
    private final int chance;

    public PinataLootEntry(Material material, int amount, int chance) {
        this.material = material;
        this.amount = amount;
        this.chance = chance;
    }

    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    public int getChance() { return chance; }
}
