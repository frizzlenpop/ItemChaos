package org.frizzlenpop.randomItem;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class MainConfig {

    private final RandomItem plugin;
    private final File configFile;

    private boolean allowInstantBreakDrops;
    private final Set<Material> bannedItems = EnumSet.noneOf(Material.class);
    private final Set<EntityType> bannedMobSpawns = EnumSet.noneOf(EntityType.class);

    public MainConfig(RandomItem plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void load() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);

        allowInstantBreakDrops = yaml.getBoolean("allow-instant-break-drops", false);

        bannedItems.clear();
        List<String> itemList = yaml.getStringList("banned-items");
        for (String name : itemList) {
            try {
                bannedItems.add(Material.valueOf(name));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, "Unknown banned item in config.yml: " + name);
            }
        }

        bannedMobSpawns.clear();
        List<String> mobList = yaml.getStringList("banned-mob-spawns");
        for (String name : mobList) {
            try {
                bannedMobSpawns.add(EntityType.valueOf(name));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, "Unknown banned mob in config.yml: " + name);
            }
        }
    }

    public boolean isAllowInstantBreakDrops() {
        return allowInstantBreakDrops;
    }

    public boolean isItemBanned(Material material) {
        return bannedItems.contains(material);
    }

    public boolean isMobSpawnBanned(EntityType entityType) {
        return bannedMobSpawns.contains(entityType);
    }

    public Set<Material> getBannedItems() {
        return bannedItems;
    }
}
