package org.frizzlenpop.randomItem.blockadex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.frizzlenpop.randomItem.RandomItem;

import java.io.*;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class BlockadexManager implements Listener {

    private static final Type MAP_TYPE = new TypeToken<Map<String, List<String>>>() {}.getType();

    private final RandomItem plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File file;
    private final Map<UUID, Set<Material>> collected = new HashMap<>();
    private final List<BlockadexMilestone> milestones = new ArrayList<>();
    private final int totalItems;
    private boolean enabled;

    public BlockadexManager(RandomItem plugin) {
        this.plugin = plugin;

        // Count all valid items (same filter as RandomDropListener)
        int count = 0;
        for (Material mat : Material.values()) {
            if (mat.isItem() && !mat.isAir()) count++;
        }
        this.totalItems = count;

        // Load config
        plugin.saveResource("blockadex.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "blockadex.yml"));
        this.enabled = config.getBoolean("enabled", true);

        ConfigurationSection milestoneSection = config.getConfigurationSection("milestones");
        if (milestoneSection != null) {
            for (String key : milestoneSection.getKeys(false)) {
                ConfigurationSection s = milestoneSection.getConfigurationSection(key);
                if (s == null) continue;
                milestones.add(new BlockadexMilestone(
                        Integer.parseInt(key),
                        s.getString("name", "Milestone"),
                        s.getString("effect", "SPEED"),
                        s.getInt("amplifier", 0),
                        s.getString("description", "")
                ));
            }
        }
        milestones.sort(Comparator.comparingInt(BlockadexMilestone::getPercentage));

        // Load persistent data
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "blockadex.json");
        load();

        // Auto-save
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::save, 6000L, 6000L);
    }

    public boolean isEnabled() { return enabled; }

    public void toggle() { enabled = !enabled; }

    public void recordItem(UUID uuid, Material material) {
        if (!enabled) return;

        Set<Material> playerSet = collected.computeIfAbsent(uuid, k -> new HashSet<>());
        if (!playerSet.add(material)) return; // Already had it

        // Check if a milestone was just crossed
        int oldCount = playerSet.size() - 1;
        int newCount = playerSet.size();
        int oldPct = (int) ((oldCount * 100L) / totalItems);
        int newPct = (int) ((newCount * 100L) / totalItems);

        for (BlockadexMilestone milestone : milestones) {
            if (oldPct < milestone.getPercentage() && newPct >= milestone.getPercentage()) {
                unlockMilestone(uuid, milestone);
            }
        }
    }

    public int getCollectedCount(UUID uuid) {
        Set<Material> set = collected.get(uuid);
        return set != null ? set.size() : 0;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getPercentage(UUID uuid) {
        return (int) ((getCollectedCount(uuid) * 100L) / totalItems);
    }

    public List<BlockadexMilestone> getMilestones() {
        return milestones;
    }

    public Map<UUID, Integer> getTopCollectors(int limit) {
        return collected.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size(),
                        (a, b) -> a, LinkedHashMap::new));
    }

    public void applyEffects(Player player) {
        if (!enabled) return;
        int pct = getPercentage(player.getUniqueId());

        for (BlockadexMilestone milestone : milestones) {
            if (pct >= milestone.getPercentage()) {
                PotionEffectType effectType = PotionEffectType.getByKey(
                        org.bukkit.NamespacedKey.minecraft(milestone.getEffectName().toLowerCase()));
                if (effectType != null) {
                    player.addPotionEffect(new PotionEffect(
                            effectType, Integer.MAX_VALUE, milestone.getAmplifier(),
                            true, false, true));
                }
            }
        }
    }

    private void unlockMilestone(UUID uuid, BlockadexMilestone milestone) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        // Apply the new effect
        applyEffects(player);

        // Title announcement to the player
        player.showTitle(Title.title(
                Component.text("BLOCKADEX", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text(milestone.getName() + " - " + milestone.getDescription(), NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))));

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        // Server broadcast
        Bukkit.broadcast(Component.text("[Blockadex] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" reached ", NamedTextColor.GOLD))
                .append(Component.text(milestone.getPercentage() + "% ", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text("- " + milestone.getName() + "! ", NamedTextColor.GOLD))
                .append(Component.text("(" + milestone.getDescription() + ")", NamedTextColor.AQUA)));
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
        Map<String, List<String>> serializable = new HashMap<>();
        for (Map.Entry<UUID, Set<Material>> entry : collected.entrySet()) {
            List<String> materials = new ArrayList<>();
            for (Material mat : entry.getValue()) {
                materials.add(mat.name());
            }
            serializable.put(entry.getKey().toString(), materials);
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
            Map<String, List<String>> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw != null) {
                for (Map.Entry<String, List<String>> entry : raw.entrySet()) {
                    Set<Material> set = new HashSet<>();
                    for (String matName : entry.getValue()) {
                        try {
                            set.add(Material.valueOf(matName));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    collected.put(UUID.fromString(entry.getKey()), set);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
