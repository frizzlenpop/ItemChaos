package org.frizzlenpop.randomItem.shop;

import org.bukkit.Material;

public class ShopItem {

    private final Material material;
    private final String displayName;
    private final int amount;
    private final long cost;

    public ShopItem(Material material, String displayName, int amount, long cost) {
        this.material = material;
        this.displayName = displayName;
        this.amount = amount;
        this.cost = cost;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getAmount() {
        return amount;
    }

    public long getCost() {
        return cost;
    }
}
