package org.frizzlenpop.randomItem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.frizzlenpop.randomItem.economy.CoinManager;
import org.frizzlenpop.randomItem.economy.ItemValueRegistry;
import org.frizzlenpop.randomItem.events.RandomEventManager;
import org.frizzlenpop.randomItem.upgrade.UpgradeManager;
import org.frizzlenpop.randomItem.upgrade.UpgradeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RandomDropListener implements Listener {

    private static final List<Material> VALID_ITEMS;
    private static final List<Material> RARE_ITEMS = List.of(
            Material.DIAMOND, Material.DIAMOND_BLOCK,
            Material.EMERALD, Material.EMERALD_BLOCK,
            Material.NETHERITE_INGOT, Material.NETHERITE_SCRAP,
            Material.NETHERITE_SWORD, Material.NETHERITE_PICKAXE,
            Material.NETHERITE_AXE, Material.NETHERITE_CHESTPLATE,
            Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_APPLE,
            Material.ELYTRA, Material.TOTEM_OF_UNDYING,
            Material.NETHER_STAR, Material.BEACON,
            Material.TRIDENT, Material.HEART_OF_THE_SEA,
            Material.SHULKER_SHELL, Material.END_CRYSTAL,
            Material.EXPERIENCE_BOTTLE, Material.DRAGON_BREATH,
            Material.CONDUIT, Material.WITHER_SKELETON_SKULL
    );

    // Spawn egg -> EntityType mapping for the 30% mob spawn feature
    // Ender Dragon and Wither eggs are excluded (always drop as eggs)
    private static final Map<Material, EntityType> SPAWN_EGG_MAP;

    static {
        VALID_ITEMS = new ArrayList<>();
        for (Material mat : Material.values()) {
            if (mat.isItem() && !mat.isAir()) {
                VALID_ITEMS.add(mat);
            }
        }

        // Build spawn egg map automatically from material names
        Map<Material, EntityType> eggMap = new java.util.HashMap<>();
        for (Material mat : Material.values()) {
            String name = mat.name();
            if (!name.endsWith("_SPAWN_EGG")) continue;

            // Skip Ender Dragon and Wither - these always drop as eggs
            if (name.equals("ENDER_DRAGON_SPAWN_EGG") || name.equals("WITHER_SPAWN_EGG")) continue;

            String entityName = name.replace("_SPAWN_EGG", "");
            try {
                EntityType type = EntityType.valueOf(entityName);
                eggMap.put(mat, type);
            } catch (IllegalArgumentException ignored) {
            }
        }
        SPAWN_EGG_MAP = Map.copyOf(eggMap);
    }

    private final RandomItem plugin;
    private final CoinManager coinManager;
    private final UpgradeManager upgradeManager;
    private final RandomEventManager randomEventManager;

    public RandomDropListener(RandomItem plugin, CoinManager coinManager, UpgradeManager upgradeManager,
                              RandomEventManager randomEventManager) {
        this.plugin = plugin;
        this.coinManager = coinManager;
        this.upgradeManager = upgradeManager;
        this.randomEventManager = randomEventManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.isChaosEnabled()) return;

        event.setDropItems(false);

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location dropLoc = event.getBlock().getLocation();

        Material item = rollItem(uuid);
        dropOrSpawn(item, dropLoc);
        long totalCoins = ItemValueRegistry.getValue(item);

        // Double drop check
        int ddLevel = upgradeManager.getLevel(uuid, UpgradeType.DOUBLE_DROP);
        if (ddLevel > 0 && ThreadLocalRandom.current().nextDouble() < ddLevel * 0.10) {
            Material bonusItem = rollItem(uuid);
            dropOrSpawn(bonusItem, dropLoc);
            totalCoins += ItemValueRegistry.getValue(bonusItem);
        }

        // Coin multiplier
        totalCoins = applyMultiplier(uuid, totalCoins);

        coinManager.addCoins(uuid, totalCoins);
        player.sendActionBar(Component.text("+" + totalCoins + " coins", NamedTextColor.GOLD));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.isChaosEnabled()) return;

        event.getDrops().clear();

        Player killer = event.getEntity().getKiller();
        Material item = killer != null ? rollItem(killer.getUniqueId()) : getRandomItem();
        Location dropLoc = event.getEntity().getLocation();

        // For entity death, spawn eggs that become mobs need special handling
        // since we can't use event.getDrops() for spawned mobs
        EntityType spawnType = SPAWN_EGG_MAP.get(item);
        if (spawnType != null && ThreadLocalRandom.current().nextDouble() < 0.30) {
            dropLoc.getWorld().spawnEntity(dropLoc, spawnType);
        } else {
            event.getDrops().add(new ItemStack(item));
        }

        if (killer == null) return;

        UUID uuid = killer.getUniqueId();
        long totalCoins = ItemValueRegistry.getValue(item);

        // Double drop check
        int ddLevel = upgradeManager.getLevel(uuid, UpgradeType.DOUBLE_DROP);
        if (ddLevel > 0 && ThreadLocalRandom.current().nextDouble() < ddLevel * 0.10) {
            Material bonusItem = rollItem(uuid);
            EntityType bonusSpawn = SPAWN_EGG_MAP.get(bonusItem);
            if (bonusSpawn != null && ThreadLocalRandom.current().nextDouble() < 0.30) {
                dropLoc.getWorld().spawnEntity(dropLoc, bonusSpawn);
            } else {
                event.getDrops().add(new ItemStack(bonusItem));
            }
            totalCoins += ItemValueRegistry.getValue(bonusItem);
        }

        totalCoins = applyMultiplier(uuid, totalCoins);

        coinManager.addCoins(uuid, totalCoins);
        killer.sendActionBar(Component.text("+" + totalCoins + " coins", NamedTextColor.GOLD));
    }

    /**
     * Drops an item naturally, or if it's a spawn egg (not dragon/wither),
     * has a 30% chance to spawn the mob instead.
     */
    private void dropOrSpawn(Material item, Location location) {
        EntityType spawnType = SPAWN_EGG_MAP.get(item);
        if (spawnType != null && ThreadLocalRandom.current().nextDouble() < 0.30) {
            location.getWorld().spawnEntity(location.clone().add(0.5, 0.5, 0.5), spawnType);
        } else {
            location.getWorld().dropItemNaturally(location, new ItemStack(item));
        }
    }

    private Material rollItem(UUID uuid) {
        // Legendary Minute event overrides all drops to rare
        if (randomEventManager != null && randomEventManager.isLegendaryMinuteActive()) {
            return RARE_ITEMS.get(ThreadLocalRandom.current().nextInt(RARE_ITEMS.size()));
        }

        int luckyLevel = upgradeManager.getLevel(uuid, UpgradeType.LUCKY_DROPS);
        if (luckyLevel > 0 && ThreadLocalRandom.current().nextDouble() < luckyLevel * 0.05) {
            return RARE_ITEMS.get(ThreadLocalRandom.current().nextInt(RARE_ITEMS.size()));
        }
        return getRandomItem();
    }

    private long applyMultiplier(UUID uuid, long baseCoins) {
        int cmLevel = upgradeManager.getLevel(uuid, UpgradeType.COIN_MULTIPLIER);
        double multiplier = switch (cmLevel) {
            case 1 -> 1.5;
            case 2 -> 2.0;
            case 3 -> 3.0;
            default -> 1.0;
        };
        long result = Math.round(baseCoins * multiplier);

        // Double Coins event
        if (randomEventManager != null && randomEventManager.isDoubleCoinsActive()) {
            result *= 2;
        }

        return result;
    }

    private Material getRandomItem() {
        return VALID_ITEMS.get(ThreadLocalRandom.current().nextInt(VALID_ITEMS.size()));
    }
}
