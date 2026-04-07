package org.frizzlenpop.randomItem.boss;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.frizzlenpop.randomItem.RandomItem;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BossConfig {

    private final File configFile;
    private final Map<String, BossDefinition> bosses = new LinkedHashMap<>();
    private final Map<String, Location> anchors = new HashMap<>();

    public BossConfig(RandomItem plugin) {
        plugin.saveResource("bosses.yml", false);
        this.configFile = new File(plugin.getDataFolder(), "bosses.yml");
        load();
    }

    private void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        ConfigurationSection bossSection = config.getConfigurationSection("bosses");
        if (bossSection != null) {
            for (String key : bossSection.getKeys(false)) {
                ConfigurationSection s = bossSection.getConfigurationSection(key);
                if (s == null) continue;

                String name = s.getString("name", key);
                EntityType entityType = EntityType.valueOf(s.getString("entity-type", "ZOMBIE"));
                double health = s.getDouble("health", 100);
                double speed = s.getDouble("speed", 0.25);
                double damage = s.getDouble("damage", 10);
                long coinReward = s.getLong("coin-reward", 1000);
                int proximityRadius = s.getInt("proximity-radius", 50);
                boolean spawnAdds = s.getBoolean("spawn-adds", false);
                EntityType addType = spawnAdds ? EntityType.valueOf(s.getString("add-entity-type", "ZOMBIE")) : null;
                int addCount = s.getInt("add-count", 5);
                int addInterval = s.getInt("add-interval-seconds", 15);

                List<BossLootEntry> loot = new ArrayList<>();
                List<?> lootList = s.getList("loot");
                if (lootList != null) {
                    for (Object obj : lootList) {
                        if (obj instanceof Map<?, ?> map) {
                            Material mat = Material.matchMaterial(String.valueOf(map.get("material")));
                            if (mat != null) {
                                loot.add(new BossLootEntry(mat, ((Number) map.get("amount")).intValue()));
                            }
                        }
                    }
                }

                bosses.put(key.toLowerCase(), new BossDefinition(key, name, entityType, health, speed,
                        damage, coinReward, proximityRadius, spawnAdds, addType, addCount, addInterval, loot));
            }
        }

        ConfigurationSection anchorSection = config.getConfigurationSection("anchors");
        if (anchorSection != null) {
            for (String teamName : anchorSection.getKeys(false)) {
                ConfigurationSection a = anchorSection.getConfigurationSection(teamName);
                if (a == null) continue;
                World world = Bukkit.getWorld(a.getString("world", "world"));
                if (world == null) continue;
                anchors.put(teamName.toLowerCase(), new Location(world,
                        a.getDouble("x"), a.getDouble("y"), a.getDouble("z")));
            }
        }
    }

    public BossDefinition getBoss(String key) {
        return bosses.get(key.toLowerCase());
    }

    public Collection<BossDefinition> getAllBosses() {
        return bosses.values();
    }

    public List<String> getBossNames() {
        return new ArrayList<>(bosses.keySet());
    }

    public Location getAnchor(String teamName) {
        return anchors.get(teamName.toLowerCase());
    }

    public void setAnchor(String teamName, Location loc) {
        anchors.put(teamName.toLowerCase(), loc);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("anchors." + teamName.toLowerCase() + ".world", loc.getWorld().getName());
        config.set("anchors." + teamName.toLowerCase() + ".x", loc.getX());
        config.set("anchors." + teamName.toLowerCase() + ".y", loc.getY());
        config.set("anchors." + teamName.toLowerCase() + ".z", loc.getZ());
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
