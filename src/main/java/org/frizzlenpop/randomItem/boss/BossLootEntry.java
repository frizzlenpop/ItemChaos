package org.frizzlenpop.randomItem.boss;

import org.bukkit.Material;

public class BossLootEntry {
    private final Material material;
    private final int amount;

    public BossLootEntry(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
}
