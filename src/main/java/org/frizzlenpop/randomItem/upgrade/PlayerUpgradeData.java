package org.frizzlenpop.randomItem.upgrade;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class PlayerUpgradeData {

    private final Map<UpgradeType, Integer> levels;

    public PlayerUpgradeData() {
        levels = new EnumMap<>(UpgradeType.class);
        for (UpgradeType type : UpgradeType.values()) {
            levels.put(type, 0);
        }
    }

    public int getLevel(UpgradeType type) {
        return levels.getOrDefault(type, 0);
    }

    public void setLevel(UpgradeType type, int level) {
        levels.put(type, level);
    }

    public boolean isMaxed(UpgradeType type) {
        return getLevel(type) >= type.getMaxLevel();
    }

    public Map<String, Integer> toMap() {
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<UpgradeType, Integer> entry : levels.entrySet()) {
            map.put(entry.getKey().name(), entry.getValue());
        }
        return map;
    }

    public static PlayerUpgradeData fromMap(Map<String, Integer> map) {
        PlayerUpgradeData data = new PlayerUpgradeData();
        if (map == null) return data;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            try {
                UpgradeType type = UpgradeType.valueOf(entry.getKey());
                data.setLevel(type, entry.getValue());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }
}
