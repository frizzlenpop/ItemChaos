package org.frizzlenpop.randomItem.shop;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.frizzlenpop.randomItem.RandomItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShopConfig {

    private final List<ShopItem> items = new ArrayList<>();

    public ShopConfig(RandomItem plugin) {
        plugin.saveResource("shop.yml", false);
        File file = new File(plugin.getDataFolder(), "shop.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        List<?> itemList = config.getList("items");
        if (itemList == null) return;

        for (Object obj : itemList) {
            if (!(obj instanceof ConfigurationSection section)) {
                if (obj instanceof java.util.Map<?, ?> map) {
                    loadFromMap(map);
                }
                continue;
            }
            loadFromSection(section);
        }
    }

    @SuppressWarnings("deprecation")
    private void loadFromMap(java.util.Map<?, ?> map) {
        String materialName = String.valueOf(map.get("material"));
        Material material = Material.matchMaterial(materialName);
        if (material == null) return;

        String name = map.containsKey("name")
                ? ChatColor.translateAlternateColorCodes('&', String.valueOf(map.get("name")))
                : null;
        int amount = map.containsKey("amount") ? ((Number) map.get("amount")).intValue() : 1;
        long cost = map.containsKey("cost") ? ((Number) map.get("cost")).longValue() : 1;

        items.add(new ShopItem(material, name, amount, cost));
    }

    @SuppressWarnings("deprecation")
    private void loadFromSection(ConfigurationSection section) {
        String materialName = section.getString("material");
        Material material = Material.matchMaterial(materialName != null ? materialName : "");
        if (material == null) return;

        String name = section.contains("name")
                ? ChatColor.translateAlternateColorCodes('&', section.getString("name", ""))
                : null;
        int amount = section.getInt("amount", 1);
        long cost = section.getLong("cost", 1);

        items.add(new ShopItem(material, name, amount, cost));
    }

    public List<ShopItem> getItems() {
        return items;
    }
}
