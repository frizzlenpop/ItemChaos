package org.frizzlenpop.randomItem.pinata;

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

public class PinataConfig {

    private final File configFile;
    private final Map<String, PinataDefinition> pinatas = new LinkedHashMap<>();
    private final Map<String, Location> locations = new HashMap<>();

    public PinataConfig(RandomItem plugin) {
        plugin.saveResource("pinatas.yml", false);
        this.configFile = new File(plugin.getDataFolder(), "pinatas.yml");
        load();
    }

    private void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        ConfigurationSection pinataSection = config.getConfigurationSection("pinatas");
        if (pinataSection != null) {
            for (String key : pinataSection.getKeys(false)) {
                ConfigurationSection s = pinataSection.getConfigurationSection(key);
                if (s == null) continue;

                String name = s.getString("name", key);
                EntityType entityType = EntityType.valueOf(s.getString("entity-type", "PIG"));
                double health = s.getDouble("health", 100);
                long coinScatter = s.getLong("coin-scatter", 200);

                List<PinataLootEntry> loot = new ArrayList<>();
                List<?> lootList = s.getList("loot");
                if (lootList != null) {
                    for (Object obj : lootList) {
                        if (obj instanceof Map<?, ?> map) {
                            Material mat = Material.matchMaterial(String.valueOf(map.get("material")));
                            if (mat != null) {
                                loot.add(new PinataLootEntry(mat,
                                        ((Number) map.get("amount")).intValue(),
                                        ((Number) map.get("chance")).intValue()));
                            }
                        }
                    }
                }

                pinatas.put(key.toLowerCase(), new PinataDefinition(key, name, entityType, health, coinScatter, loot));
            }
        }

        ConfigurationSection locSection = config.getConfigurationSection("locations");
        if (locSection != null) {
            for (String locName : locSection.getKeys(false)) {
                ConfigurationSection l = locSection.getConfigurationSection(locName);
                if (l == null) continue;
                World world = Bukkit.getWorld(l.getString("world", "world"));
                if (world == null) continue;
                locations.put(locName.toLowerCase(), new Location(world,
                        l.getDouble("x"), l.getDouble("y"), l.getDouble("z")));
            }
        }
    }

    public PinataDefinition getPinata(String key) {
        return pinatas.get(key.toLowerCase());
    }

    public Collection<PinataDefinition> getAllPinatas() {
        return pinatas.values();
    }

    public List<String> getPinataNames() {
        return new ArrayList<>(pinatas.keySet());
    }

    public void setLocation(String name, Location loc) {
        locations.put(name.toLowerCase(), loc);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("locations." + name.toLowerCase() + ".world", loc.getWorld().getName());
        config.set("locations." + name.toLowerCase() + ".x", loc.getX());
        config.set("locations." + name.toLowerCase() + ".y", loc.getY());
        config.set("locations." + name.toLowerCase() + ".z", loc.getZ());
        try { config.save(configFile); } catch (IOException e) { e.printStackTrace(); }
    }
}
