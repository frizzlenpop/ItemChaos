package org.frizzlenpop.randomItem.upgrade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.economy.CoinManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UpgradeManager implements Listener {

    private static final Type MAP_TYPE =
            new TypeToken<Map<String, Map<String, Integer>>>() {}.getType();
    private static final int[] HEALTH_AMPLIFIERS = {1, 3, 5};

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final RandomItem plugin;
    private final CoinManager coinManager;
    private final File file;
    private final Map<UUID, PlayerUpgradeData> data = new HashMap<>();

    public UpgradeManager(RandomItem plugin, CoinManager coinManager) {
        this.plugin = plugin;
        this.coinManager = coinManager;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "upgrades.json");
        load();

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::save, 6000L, 6000L);

        // Magnet upgrade tick — every 0.5 seconds, pull nearby items toward players
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickMagnet, 20L, 10L);
    }

    public int getLevel(UUID uuid, UpgradeType type) {
        return getPlayerData(uuid).getLevel(type);
    }

    public boolean tryPurchase(Player player, UpgradeType type) {
        UUID uuid = player.getUniqueId();
        PlayerUpgradeData playerData = getPlayerData(uuid);
        int currentLevel = playerData.getLevel(type);

        if (currentLevel >= type.getMaxLevel()) {
            player.sendMessage(Component.text("Already maxed!", NamedTextColor.RED));
            return false;
        }

        int cost = type.getCost(currentLevel);
        if (!coinManager.removeCoins(uuid, cost)) {
            player.sendMessage(Component.text("Not enough coins! Need " + cost, NamedTextColor.RED));
            return false;
        }

        playerData.setLevel(type, currentLevel + 1);

        if (type == UpgradeType.HASTE || type == UpgradeType.HEALTH_BOOST
                || type == UpgradeType.NIGHT_VISION || type == UpgradeType.FIRE_RESISTANCE) {
            applyEffects(player);
        }

        player.sendMessage(Component.text(type.getDisplayName() + " upgraded to level " +
                (currentLevel + 1) + "!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        return true;
    }

    public void applyEffects(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerUpgradeData playerData = getPlayerData(uuid);

        int hasteLevel = playerData.getLevel(UpgradeType.HASTE);
        if (hasteLevel > 0) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE, Integer.MAX_VALUE, hasteLevel - 1,
                    true, false, true));
        }

        int healthLevel = playerData.getLevel(UpgradeType.HEALTH_BOOST);
        if (healthLevel > 0) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE, HEALTH_AMPLIFIERS[healthLevel - 1],
                    true, false, true));
        }

        int nightVisionLevel = playerData.getLevel(UpgradeType.NIGHT_VISION);
        if (nightVisionLevel > 0) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0,
                    true, false, true));
        }

        int fireResLevel = playerData.getLevel(UpgradeType.FIRE_RESISTANCE);
        if (fireResLevel == 3) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0,
                    true, false, true));
        }
    }

    public PlayerUpgradeData getPlayerData(UUID uuid) {
        return data.computeIfAbsent(uuid, k -> new PlayerUpgradeData());
    }

    private void tickMagnet() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int magnetLevel = getLevel(player.getUniqueId(), UpgradeType.MAGNET);
            if (magnetLevel <= 0) continue;
            double radius = magnetLevel * 5.0;
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof Item item) {
                    item.teleport(player.getLocation());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyEffects(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyEffects(player), 1L);
    }

    public void save() {
        Map<String, Map<String, Integer>> serializable = new HashMap<>();
        for (Map.Entry<UUID, PlayerUpgradeData> entry : data.entrySet()) {
            serializable.put(entry.getKey().toString(), entry.getValue().toMap());
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
            Map<String, Map<String, Integer>> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw != null) {
                for (Map.Entry<String, Map<String, Integer>> entry : raw.entrySet()) {
                    data.put(UUID.fromString(entry.getKey()),
                            PlayerUpgradeData.fromMap(entry.getValue()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
