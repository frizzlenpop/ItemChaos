package org.frizzlenpop.randomItem.boss;

import org.bukkit.entity.EntityType;

import java.util.List;

public class BossDefinition {
    private final String key;
    private final String name;
    private final EntityType entityType;
    private final double health;
    private final double speed;
    private final double damage;
    private final long coinReward;
    private final int proximityRadius;
    private final boolean spawnAdds;
    private final EntityType addEntityType;
    private final int addCount;
    private final int addIntervalSeconds;
    private final List<BossLootEntry> loot;

    public BossDefinition(String key, String name, EntityType entityType, double health, double speed,
                          double damage, long coinReward, int proximityRadius, boolean spawnAdds,
                          EntityType addEntityType, int addCount, int addIntervalSeconds,
                          List<BossLootEntry> loot) {
        this.key = key;
        this.name = name;
        this.entityType = entityType;
        this.health = health;
        this.speed = speed;
        this.damage = damage;
        this.coinReward = coinReward;
        this.proximityRadius = proximityRadius;
        this.spawnAdds = spawnAdds;
        this.addEntityType = addEntityType;
        this.addCount = addCount;
        this.addIntervalSeconds = addIntervalSeconds;
        this.loot = loot;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public EntityType getEntityType() { return entityType; }
    public double getHealth() { return health; }
    public double getSpeed() { return speed; }
    public double getDamage() { return damage; }
    public long getCoinReward() { return coinReward; }
    public int getProximityRadius() { return proximityRadius; }
    public boolean isSpawnAdds() { return spawnAdds; }
    public EntityType getAddEntityType() { return addEntityType; }
    public int getAddCount() { return addCount; }
    public int getAddIntervalSeconds() { return addIntervalSeconds; }
    public List<BossLootEntry> getLoot() { return loot; }
}
