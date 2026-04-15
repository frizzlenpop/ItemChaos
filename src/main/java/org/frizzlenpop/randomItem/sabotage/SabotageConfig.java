package org.frizzlenpop.randomItem.sabotage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.frizzlenpop.randomItem.RandomItem;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class SabotageConfig {

    private final RandomItem plugin;
    private final File configFile;
    private YamlConfiguration yaml;

    private boolean enabled;
    private boolean broadcastSabotages;
    private boolean shieldDeflectionEnabled;
    private double[] shieldDeflectChances;

    private final Map<SabotageType, Integer> costs = new EnumMap<>(SabotageType.class);
    private final Map<SabotageType, Integer> durations = new EnumMap<>(SabotageType.class);
    private final Map<SabotageType, Boolean> enabledTypes = new EnumMap<>(SabotageType.class);

    public SabotageConfig(RandomItem plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "sabotage.yml");
    }

    public void load() {
        if (!configFile.exists()) {
            plugin.saveResource("sabotage.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(configFile);

        enabled = yaml.getBoolean("enabled", true);
        broadcastSabotages = yaml.getBoolean("broadcast-sabotages", true);
        shieldDeflectionEnabled = yaml.getBoolean("shield-deflection-enabled", true);

        List<Double> chances = yaml.getDoubleList("shield-deflect-chances");
        if (chances.size() >= 3) {
            shieldDeflectChances = new double[]{chances.get(0), chances.get(1), chances.get(2)};
        } else {
            shieldDeflectChances = new double[]{0.15, 0.30, 0.50};
        }

        ConfigurationSection sabotages = yaml.getConfigurationSection("sabotages");
        for (SabotageType type : SabotageType.values()) {
            if (sabotages != null && sabotages.contains(type.name())) {
                ConfigurationSection section = sabotages.getConfigurationSection(type.name());
                costs.put(type, section.getInt("cost", type.getDefaultCost()));
                durations.put(type, section.getInt("duration", type.getDefaultDuration()));
                enabledTypes.put(type, section.getBoolean("enabled", true));
            } else {
                costs.put(type, type.getDefaultCost());
                durations.put(type, type.getDefaultDuration());
                enabledTypes.put(type, true);
            }
        }
    }

    public void save() {
        yaml.set("enabled", enabled);
        yaml.set("broadcast-sabotages", broadcastSabotages);
        yaml.set("shield-deflection-enabled", shieldDeflectionEnabled);

        for (SabotageType type : SabotageType.values()) {
            String path = "sabotages." + type.name();
            yaml.set(path + ".cost", costs.getOrDefault(type, type.getDefaultCost()));
            yaml.set(path + ".duration", durations.getOrDefault(type, type.getDefaultDuration()));
            yaml.set(path + ".enabled", enabledTypes.getOrDefault(type, true));
        }

        try {
            yaml.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save sabotage.yml", e);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isBroadcastSabotages() {
        return broadcastSabotages;
    }

    public boolean isShieldDeflectionEnabled() {
        return shieldDeflectionEnabled;
    }

    public double[] getShieldDeflectChances() {
        return shieldDeflectChances;
    }

    public int getCost(SabotageType type) {
        return costs.getOrDefault(type, type.getDefaultCost());
    }

    public int getDuration(SabotageType type) {
        return durations.getOrDefault(type, type.getDefaultDuration());
    }

    public boolean isTypeEnabled(SabotageType type) {
        return enabledTypes.getOrDefault(type, true);
    }

    public void setCost(SabotageType type, int cost) {
        costs.put(type, cost);
    }

    public void setDuration(SabotageType type, int duration) {
        durations.put(type, duration);
    }

    public void setTypeEnabled(SabotageType type, boolean enabled) {
        enabledTypes.put(type, enabled);
    }
}
