package org.frizzlenpop.randomItem.crate;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.frizzlenpop.randomItem.RandomItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CrateConfig {

    private boolean enabled;
    private int spawnIntervalMinutes;
    private String worldName;
    private int minX, maxX, minY, maxY, minZ, maxZ;
    private final List<CrateLootEntry> lootTable = new ArrayList<>();

    public CrateConfig(RandomItem plugin) {
        plugin.saveResource("crates.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "crates.yml"));

        this.enabled = config.getBoolean("enabled", true);
        this.spawnIntervalMinutes = config.getInt("spawn-interval-minutes", 10);
        this.worldName = config.getString("bounds.world", "world");
        this.minX = config.getInt("bounds.min-x", -500);
        this.maxX = config.getInt("bounds.max-x", 500);
        this.minY = config.getInt("bounds.min-y", 60);
        this.maxY = config.getInt("bounds.max-y", 120);
        this.minZ = config.getInt("bounds.min-z", -500);
        this.maxZ = config.getInt("bounds.max-z", 500);

        List<?> lootList = config.getList("loot");
        if (lootList != null) {
            for (Object obj : lootList) {
                if (obj instanceof Map<?, ?> map) {
                    Material mat = Material.matchMaterial(String.valueOf(map.get("material")));
                    if (mat == null) continue;
                    int amount = ((Number) map.get("amount")).intValue();
                    int chance = ((Number) map.get("chance")).intValue();
                    lootTable.add(new CrateLootEntry(mat, amount, chance));
                }
            }
        }
    }

    public boolean isEnabled() { return enabled; }
    public int getSpawnIntervalMinutes() { return spawnIntervalMinutes; }
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public List<CrateLootEntry> getLootTable() { return lootTable; }
}
