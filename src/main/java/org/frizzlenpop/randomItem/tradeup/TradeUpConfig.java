package org.frizzlenpop.randomItem.tradeup;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.economy.ItemValueRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("deprecation")
public class TradeUpConfig {

    private boolean enabled;
    private int inputCount;
    private final List<Tier> tiers = new ArrayList<>();

    public TradeUpConfig(RandomItem plugin) {
        plugin.saveResource("tradeup.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "tradeup.yml"));

        this.enabled = config.getBoolean("enabled", true);
        this.inputCount = config.getInt("input-count", 5);

        ConfigurationSection tierSection = config.getConfigurationSection("tiers");
        if (tierSection != null) {
            for (String key : tierSection.getKeys(false)) {
                ConfigurationSection s = tierSection.getConfigurationSection(key);
                if (s == null) continue;
                tiers.add(new Tier(key,
                        s.getInt("max-value", 1),
                        ChatColor.translateAlternateColorCodes('&', s.getString("display", key))));
            }
        }
        tiers.sort((a, b) -> Integer.compare(a.maxValue(), b.maxValue()));
    }

    public boolean isEnabled() { return enabled; }
    public int getInputCount() { return inputCount; }

    public Tier getTierForValue(int value) {
        for (Tier tier : tiers) {
            if (value <= tier.maxValue()) return tier;
        }
        return tiers.isEmpty() ? null : tiers.getLast();
    }

    public Tier getNextTier(Tier current) {
        int idx = tiers.indexOf(current);
        if (idx < 0 || idx >= tiers.size() - 1) return null;
        return tiers.get(idx + 1);
    }

    public Material getRandomItemInTier(Tier tier) {
        int prevMax = 0;
        int idx = tiers.indexOf(tier);
        if (idx > 0) prevMax = tiers.get(idx - 1).maxValue();

        List<Material> candidates = new ArrayList<>();
        for (Material mat : Material.values()) {
            if (!mat.isItem() || mat.isAir()) continue;
            int val = ItemValueRegistry.getValue(mat);
            if (val > prevMax && val <= tier.maxValue()) {
                candidates.add(mat);
            }
        }
        if (candidates.isEmpty()) return Material.DIAMOND;
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    public record Tier(String name, int maxValue, String display) {}
}
