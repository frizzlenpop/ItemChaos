package org.frizzlenpop.randomItem.upgrade;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.frizzlenpop.randomItem.RandomItem;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class UpgradeConfig {

    private final RandomItem plugin;
    private final File configFile;
    private YamlConfiguration yaml;

    private final Map<UpgradeType, int[]> costs = new EnumMap<>(UpgradeType.class);
    private final Map<UpgradeType, Boolean> enabledTypes = new EnumMap<>(UpgradeType.class);

    public UpgradeConfig(RandomItem plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "upgrades.yml");
    }

    public void load() {
        if (!configFile.exists()) {
            plugin.saveResource("upgrades.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(configFile);

        ConfigurationSection upgrades = yaml.getConfigurationSection("upgrades");
        for (UpgradeType type : UpgradeType.values()) {
            if (upgrades != null && upgrades.contains(type.name())) {
                ConfigurationSection section = upgrades.getConfigurationSection(type.name());
                List<Integer> costList = section.getIntegerList("costs");
                if (!costList.isEmpty()) {
                    costs.put(type, costList.stream().mapToInt(Integer::intValue).toArray());
                } else {
                    costs.put(type, type.getDefaultCosts());
                }
                enabledTypes.put(type, section.getBoolean("enabled", true));
            } else {
                costs.put(type, type.getDefaultCosts());
                enabledTypes.put(type, true);
            }
        }
    }

    public void save() {
        for (UpgradeType type : UpgradeType.values()) {
            String path = "upgrades." + type.name();
            int[] typeCosts = costs.getOrDefault(type, type.getDefaultCosts());
            List<Integer> costList = new java.util.ArrayList<>();
            for (int c : typeCosts) costList.add(c);
            yaml.set(path + ".costs", costList);
            yaml.set(path + ".enabled", enabledTypes.getOrDefault(type, true));
        }

        try {
            yaml.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save upgrades.yml", e);
        }
    }

    public int getCost(UpgradeType type, int level) {
        int[] typeCosts = costs.getOrDefault(type, type.getDefaultCosts());
        if (level < 0 || level >= typeCosts.length) return Integer.MAX_VALUE;
        return typeCosts[level];
    }

    public int getMaxLevel(UpgradeType type) {
        int[] typeCosts = costs.getOrDefault(type, type.getDefaultCosts());
        return typeCosts.length;
    }

    public boolean isTypeEnabled(UpgradeType type) {
        return enabledTypes.getOrDefault(type, true);
    }

    public void setCost(UpgradeType type, int level, int cost) {
        int[] typeCosts = costs.getOrDefault(type, type.getDefaultCosts()).clone();
        if (level >= 0 && level < typeCosts.length) {
            typeCosts[level] = cost;
            costs.put(type, typeCosts);
        }
    }

    public void setTypeEnabled(UpgradeType type, boolean enabled) {
        enabledTypes.put(type, enabled);
    }
}
