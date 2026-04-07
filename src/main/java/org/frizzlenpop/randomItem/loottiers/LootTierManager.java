package org.frizzlenpop.randomItem.loottiers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.economy.ItemValueRegistry;

import java.io.*;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class LootTierManager implements Listener {

    private static final Type MAP_TYPE = new TypeToken<Map<String, Long>>() {}.getType();

    private final RandomItem plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File file;
    private final Map<UUID, Long> blocksMined = new HashMap<>();
    private final List<LootTier> tiers = new ArrayList<>();
    private final Map<String, List<Material>> tierItemPools = new LinkedHashMap<>();
    private boolean enabled;

    public LootTierManager(RandomItem plugin) {
        this.plugin = plugin;

        plugin.saveResource("loottiers.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "loottiers.yml"));

        this.enabled = config.getBoolean("enabled", false);

        // Parse tiers
        ConfigurationSection tierSection = config.getConfigurationSection("tiers");
        if (tierSection != null) {
            for (String key : tierSection.getKeys(false)) {
                ConfigurationSection s = tierSection.getConfigurationSection(key);
                if (s == null) continue;
                tiers.add(new LootTier(
                        key,
                        s.getLong("blocks-required", 0),
                        s.getInt("max-item-value", 1),
                        s.getString("name", key),
                        s.getString("description", "")
                ));
            }
        }
        tiers.sort(Comparator.comparingLong(LootTier::blocksRequired));

        // Pre-build filtered item pools for each tier
        // Each pool includes all items from that tier AND all lower tiers
        for (LootTier tier : tiers) {
            List<Material> pool = new ArrayList<>();
            for (Material mat : Material.values()) {
                if (!mat.isItem() || mat.isAir()) continue;
                if (ItemValueRegistry.getValue(mat) <= tier.maxItemValue()) {
                    pool.add(mat);
                }
            }
            // Safety: if a tier has no items somehow, add at least some basics
            if (pool.isEmpty()) {
                pool.add(Material.STICK);
                pool.add(Material.COBBLESTONE);
            }
            tierItemPools.put(tier.key(), pool);
        }

        // Load persistence
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "loottiers.json");
        load();

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::save, 6000L, 6000L);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }

    public void recordBlockMined(UUID uuid) {
        if (!enabled) return;
        long oldCount = blocksMined.getOrDefault(uuid, 0L);
        long newCount = oldCount + 1;
        blocksMined.put(uuid, newCount);

        // Check for tier unlock
        LootTier oldTier = getTierForCount(oldCount);
        LootTier newTier = getTierForCount(newCount);

        if (oldTier != newTier && newTier != null) {
            unlockTier(uuid, newTier);
        }
    }

    @SuppressWarnings("deprecation")
    public List<Material> getItemPoolForPlayer(UUID uuid) {
        long count = blocksMined.getOrDefault(uuid, 0L);
        LootTier tier = getTierForCount(count);
        if (tier == null) {
            // Fallback to first tier or full list
            return tiers.isEmpty() ? null : tierItemPools.get(tiers.getFirst().key());
        }
        return tierItemPools.get(tier.key());
    }

    public Material getRandomItemForPlayer(UUID uuid) {
        List<Material> pool = getItemPoolForPlayer(uuid);
        if (pool == null || pool.isEmpty()) return Material.COBBLESTONE;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    public long getBlocksMined(UUID uuid) {
        return blocksMined.getOrDefault(uuid, 0L);
    }

    @SuppressWarnings("deprecation")
    public LootTier getCurrentTier(UUID uuid) {
        long count = blocksMined.getOrDefault(uuid, 0L);
        LootTier tier = getTierForCount(count);
        return tier != null ? tier : (tiers.isEmpty() ? null : tiers.getFirst());
    }

    public LootTier getNextTier(UUID uuid) {
        long count = blocksMined.getOrDefault(uuid, 0L);
        for (LootTier tier : tiers) {
            if (tier.blocksRequired() > count) return tier;
        }
        return null;
    }

    public List<LootTier> getTiers() {
        return tiers;
    }

    public Map<UUID, Long> getTopMiners(int limit) {
        return blocksMined.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    public void resetPlayer(UUID uuid) {
        blocksMined.remove(uuid);
    }

    private LootTier getTierForCount(long count) {
        LootTier result = null;
        for (LootTier tier : tiers) {
            if (count >= tier.blocksRequired()) {
                result = tier;
            } else {
                break;
            }
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private void unlockTier(UUID uuid, LootTier tier) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        String coloredName = ChatColor.translateAlternateColorCodes('&', tier.name());

        player.showTitle(Title.title(
                Component.text("DIMENSION BREACH!", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD),
                Component.text(coloredName, NamedTextColor.GOLD),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(4), Duration.ofMillis(500))));

        Bukkit.broadcast(Component.text("[Loot Tiers] ", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" broke into the ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(coloredName, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.LIGHT_PURPLE)));

        Bukkit.broadcast(Component.text("  " + tier.description(), NamedTextColor.GRAY));

        // Item count info
        List<Material> pool = tierItemPools.get(tier.key());
        if (pool != null) {
            Bukkit.broadcast(Component.text("  " + pool.size() + " items now available!", NamedTextColor.AQUA));
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        LootTier tier = getCurrentTier(player.getUniqueId());
        if (tier != null) {
            String coloredName = ChatColor.translateAlternateColorCodes('&', tier.name());
            player.sendMessage(Component.text("[Loot Tiers] ", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                    .append(Component.text("Current dimension: ", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(coloredName, NamedTextColor.GOLD)));
        }
    }

    public void save() {
        Map<String, Long> serializable = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : blocksMined.entrySet()) {
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
                    blocksMined.put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public record LootTier(String key, long blocksRequired, int maxItemValue, String name, String description) {}
}
