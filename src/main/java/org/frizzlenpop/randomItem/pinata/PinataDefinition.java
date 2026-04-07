package org.frizzlenpop.randomItem.pinata;

import org.bukkit.entity.EntityType;

import java.util.List;

public class PinataDefinition {
    private final String key;
    private final String name;
    private final EntityType entityType;
    private final double health;
    private final long coinScatter;
    private final List<PinataLootEntry> loot;

    public PinataDefinition(String key, String name, EntityType entityType, double health,
                            long coinScatter, List<PinataLootEntry> loot) {
        this.key = key;
        this.name = name;
        this.entityType = entityType;
        this.health = health;
        this.coinScatter = coinScatter;
        this.loot = loot;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public EntityType getEntityType() { return entityType; }
    public double getHealth() { return health; }
    public long getCoinScatter() { return coinScatter; }
    public List<PinataLootEntry> getLoot() { return loot; }
}
