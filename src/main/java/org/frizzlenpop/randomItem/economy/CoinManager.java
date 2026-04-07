package org.frizzlenpop.randomItem.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.frizzlenpop.randomItem.RandomItem;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CoinManager {

    private static final Type MAP_TYPE = new TypeToken<Map<String, Long>>() {}.getType();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File file;
    private final Map<UUID, Long> coins = new HashMap<>();

    public CoinManager(RandomItem plugin) {
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "coins.json");
        load();

        // Auto-save every 5 minutes (6000 ticks)
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::save, 6000L, 6000L);
    }

    public long getCoins(UUID uuid) {
        return coins.getOrDefault(uuid, 0L);
    }

    public void addCoins(UUID uuid, long amount) {
        coins.put(uuid, getCoins(uuid) + amount);
    }

    public boolean removeCoins(UUID uuid, long amount) {
        long current = getCoins(uuid);
        if (current < amount) return false;
        coins.put(uuid, current - amount);
        return true;
    }

    public void setCoins(UUID uuid, long amount) {
        coins.put(uuid, amount);
    }

    public Map<UUID, Long> getAllCoins() {
        return Collections.unmodifiableMap(coins);
    }

    public void save() {
        Map<String, Long> serializable = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : coins.entrySet()) {
            serializable.put(entry.getKey().toString(), entry.getValue());
        }
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(serializable, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Map<String, Long> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw != null) {
                for (Map.Entry<String, Long> entry : raw.entrySet()) {
                    coins.put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
