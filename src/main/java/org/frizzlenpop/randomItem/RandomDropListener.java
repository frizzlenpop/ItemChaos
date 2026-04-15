package org.frizzlenpop.randomItem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.frizzlenpop.randomItem.blockadex.BlockadexManager;
import org.frizzlenpop.randomItem.economy.CoinManager;
import org.frizzlenpop.randomItem.economy.ItemValueRegistry;
import org.frizzlenpop.randomItem.events.RandomEventManager;
import org.frizzlenpop.randomItem.loottiers.LootTierManager;
import org.frizzlenpop.randomItem.mythic.MythicItemRegistry;
import org.frizzlenpop.randomItem.upgrade.UpgradeManager;
import org.frizzlenpop.randomItem.upgrade.UpgradeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static final Set<Material> BLOCKED_MATERIALS = Set.of(
            Material.DEBUG_STICK, Material.COMMAND_BLOCK, Material.COMMAND_BLOCK_MINECART,
            Material.STRUCTURE_BLOCK, Material.JIGSAW, Material.BARRIER,
            Material.LIGHT, Material.STRUCTURE_VOID
    );

    static {
        VALID_ITEMS = new ArrayList<>();
        for (Material mat : Material.values()) {
            if (mat.isItem() && !mat.isAir() && !BLOCKED_MATERIALS.contains(mat)) {
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
    private final BlockadexManager blockadexManager;
    private final LootTierManager lootTierManager;
    private final MythicItemRegistry mythicItemRegistry;
    private final MainConfig mainConfig;

    public RandomDropListener(RandomItem plugin, CoinManager coinManager, UpgradeManager upgradeManager,
                              RandomEventManager randomEventManager, BlockadexManager blockadexManager,
                              LootTierManager lootTierManager, MythicItemRegistry mythicItemRegistry,
                              MainConfig mainConfig) {
        this.plugin = plugin;
        this.coinManager = coinManager;
        this.upgradeManager = upgradeManager;
        this.randomEventManager = randomEventManager;
        this.blockadexManager = blockadexManager;
        this.lootTierManager = lootTierManager;
        this.mythicItemRegistry = mythicItemRegistry;
        this.mainConfig = mainConfig;
    }

    /**
     * Checks if a block is "instant-break" (hardness <= 0), e.g. leaves, grass, flowers, crops.
     */
    private boolean isInstantBreak(Block block) {
        return block.getType().getHardness() <= 0;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.isChaosEnabled()) return;

        // Skip instant-break blocks (leaves, grass, flowers, etc.) if disabled
        if (!mainConfig.isAllowInstantBreakDrops() && isInstantBreak(event.getBlock())) {
            return; // Let vanilla handle the drop normally
        }

        event.setDropItems(false);

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location dropLoc = event.getBlock().getLocation();

        // Track block mined for loot tier progression
        lootTierManager.recordBlockMined(uuid);

        // Check for mythic drop first
        ItemStack mythicDrop = rollMythicDrop(uuid);
        long totalCoins;

        if (mythicDrop != null) {
            dropLoc.getWorld().dropItemNaturally(dropLoc, mythicDrop);
            totalCoins = 100; // Mythic items are always worth 100 coins
            announceMythicDrop(player, mythicDrop);
        } else {
            Material item = applyAutoSmelt(uuid, rollItem(uuid));
            dropOrSpawn(item, dropLoc);
            blockadexManager.recordItem(uuid, item);
            totalCoins = ItemValueRegistry.getValue(item);
        }

        // Double drop check
        int ddLevel = upgradeManager.getLevel(uuid, UpgradeType.DOUBLE_DROP);
        if (ddLevel > 0 && ThreadLocalRandom.current().nextDouble() < ddLevel * 0.10) {
            ItemStack bonusMythic = rollMythicDrop(uuid);
            if (bonusMythic != null) {
                dropLoc.getWorld().dropItemNaturally(dropLoc, bonusMythic);
                totalCoins += 100;
                announceMythicDrop(player, bonusMythic);
            } else {
                Material bonusItem = applyAutoSmelt(uuid, rollItem(uuid));
                dropOrSpawn(bonusItem, dropLoc);
                blockadexManager.recordItem(uuid, bonusItem);
                totalCoins += ItemValueRegistry.getValue(bonusItem);
            }
        }

        // Coin multiplier
        totalCoins = applyMultiplier(uuid, totalCoins);

        coinManager.addCoins(uuid, totalCoins);
        player.sendActionBar(Component.text("+" + totalCoins + " coins", NamedTextColor.GOLD));

        // XP Magnet upgrade
        int xpLevel = upgradeManager.getLevel(uuid, UpgradeType.XP_MAGNET);
        if (xpLevel > 0) {
            int[] xpAmounts = {5, 10, 20};
            player.giveExp(xpAmounts[xpLevel - 1]);
        }

        // Fire Resistance upgrade (levels 1-2: temporary on block break)
        int fireResLevel = upgradeManager.getLevel(uuid, UpgradeType.FIRE_RESISTANCE);
        if (fireResLevel > 0 && fireResLevel < 3) {
            int durationTicks = fireResLevel == 1 ? 600 : 1200;
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, durationTicks, 0, true, false, true));
        }

        // Blast Mining upgrade — chance to break adjacent blocks
        int blastLevel = upgradeManager.getLevel(uuid, UpgradeType.BLAST_MINING);
        if (blastLevel > 0 && ThreadLocalRandom.current().nextDouble() < blastLevel * 0.05) {
            Block origin = event.getBlock();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block adj = origin.getRelative(dx, dy, dz);
                        if (adj.getType().isAir()) continue;
                        adj.setType(Material.AIR);
                        Material bonusItem = rollItem(uuid);
                        bonusItem = applyAutoSmelt(uuid, bonusItem);
                        dropOrSpawn(bonusItem, adj.getLocation());
                        blockadexManager.recordItem(uuid, bonusItem);
                        coinManager.addCoins(uuid, applyMultiplier(uuid, ItemValueRegistry.getValue(bonusItem)));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.isChaosEnabled()) return;

        // Skip sabotage TNT (handled by SabotageManager)
        if (event.getEntity().customName() != null &&
                event.getEntity().customName().equals(Component.text("sabotage_entity"))) {
            return;
        }

        for (Block block : event.blockList()) {
            Material item = getRandomItem(null);
            dropOrSpawn(item, block.getLocation());
        }
        // Clear the block list so normal drops don't happen, but blocks still get destroyed
        event.setYield(0);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.isChaosEnabled()) return;

        event.getDrops().clear();

        Player killer = event.getEntity().getKiller();
        Location dropLoc = event.getEntity().getLocation();

        // Check for mythic drop if killer exists
        ItemStack mythicDrop = killer != null ? rollMythicDrop(killer.getUniqueId()) : null;

        if (mythicDrop != null) {
            event.getDrops().add(mythicDrop);
        } else {
            Material item = killer != null ? rollItem(killer.getUniqueId()) : getRandomItem(null);
            EntityType spawnType = SPAWN_EGG_MAP.get(item);
            if (spawnType != null && ThreadLocalRandom.current().nextDouble() < 0.30) {
                dropLoc.getWorld().spawnEntity(dropLoc, spawnType);
            } else {
                event.getDrops().add(new ItemStack(item));
            }
            if (killer != null) blockadexManager.recordItem(killer.getUniqueId(), item);
        }

        if (killer == null) return;

        UUID uuid = killer.getUniqueId();
        long totalCoins = mythicDrop != null ? 100 : ItemValueRegistry.getValue(event.getDrops().isEmpty() ? Material.STONE : event.getDrops().getFirst().getType());

        if (mythicDrop != null) announceMythicDrop(killer, mythicDrop);

        // Double drop check
        int ddLevel = upgradeManager.getLevel(uuid, UpgradeType.DOUBLE_DROP);
        if (ddLevel > 0 && ThreadLocalRandom.current().nextDouble() < ddLevel * 0.10) {
            ItemStack bonusMythic = rollMythicDrop(uuid);
            if (bonusMythic != null) {
                event.getDrops().add(bonusMythic);
                totalCoins += 100;
                announceMythicDrop(killer, bonusMythic);
            } else {
                Material bonusItem = rollItem(uuid);
                blockadexManager.recordItem(uuid, bonusItem);
                EntityType bonusSpawn = SPAWN_EGG_MAP.get(bonusItem);
                if (bonusSpawn != null && ThreadLocalRandom.current().nextDouble() < 0.30) {
                    dropLoc.getWorld().spawnEntity(dropLoc, bonusSpawn);
                } else {
                    event.getDrops().add(new ItemStack(bonusItem));
                }
                totalCoins += ItemValueRegistry.getValue(bonusItem);
            }
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
        // Don't drop banned items — re-roll
        if (mainConfig.isItemBanned(item)) {
            item = getSafeRandomItem(null);
        }

        EntityType spawnType = SPAWN_EGG_MAP.get(item);
        if (spawnType != null && !mainConfig.isMobSpawnBanned(spawnType)
                && ThreadLocalRandom.current().nextDouble() < 0.30) {
            location.getWorld().spawnEntity(location.clone().add(0.5, 0.5, 0.5), spawnType);
        } else {
            location.getWorld().dropItemNaturally(location, new ItemStack(item));
        }
    }

    /**
     * Returns a random item that is not banned. Falls back to COBBLESTONE if stuck.
     */
    private Material getSafeRandomItem(UUID uuid) {
        for (int i = 0; i < 10; i++) {
            Material mat = getRandomItem(uuid);
            if (!mainConfig.isItemBanned(mat)) return mat;
        }
        return Material.COBBLESTONE;
    }

    private Material rollItem(UUID uuid) {
        // Legendary Minute event overrides all drops to rare (bypasses tier restrictions)
        if (randomEventManager != null && randomEventManager.isLegendaryMinuteActive()) {
            return RARE_ITEMS.get(ThreadLocalRandom.current().nextInt(RARE_ITEMS.size()));
        }

        int luckyLevel = upgradeManager.getLevel(uuid, UpgradeType.LUCKY_DROPS);
        if (luckyLevel > 0 && ThreadLocalRandom.current().nextDouble() < luckyLevel * 0.05) {
            return RARE_ITEMS.get(ThreadLocalRandom.current().nextInt(RARE_ITEMS.size()));
        }
        return getRandomItem(uuid);
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

    /**
     * Returns a random item. If loot tiers are enabled and uuid is provided,
     * filters by the player's current tier. Otherwise uses the full item pool.
     */
    private Material getRandomItem(UUID uuid) {
        if (uuid != null && lootTierManager.isEnabled()) {
            return lootTierManager.getRandomItemForPlayer(uuid);
        }
        return VALID_ITEMS.get(ThreadLocalRandom.current().nextInt(VALID_ITEMS.size()));
    }

    /**
     * Checks if a mythic item should drop. Returns the mythic ItemStack or null.
     * Drop chance depends on whether the player is in the Mythic loot tier or just Legendary.
     */
    private ItemStack rollMythicDrop(UUID uuid) {
        if (!mythicItemRegistry.isEnabled()) return null;

        int chance = mythicItemRegistry.getDropChanceInLegendary();

        // Mythic Hunter upgrade bonus
        int mythicHunterLevel = upgradeManager.getLevel(uuid, UpgradeType.MYTHIC_HUNTER);
        if (mythicHunterLevel > 0) {
            chance += mythicHunterLevel * 2; // +2/+4/+6%
        }

        // If loot tiers enabled, check if player is in the mythic tier
        if (lootTierManager.isEnabled()) {
            long blocksMined = lootTierManager.getBlocksMined(uuid);
            if (blocksMined < mythicItemRegistry.getMythicTierBlocks()) {
                return null; // Haven't unlocked mythic tier yet
            }
            // In mythic tier — use the configured chance
        } else {
            // Loot tiers disabled — mythic can proc on any drop at the configured chance
        }

        if (ThreadLocalRandom.current().nextInt(100) < chance) {
            return mythicItemRegistry.getRandomMythicItem();
        }
        return null;
    }

    private void announceMythicDrop(Player player, ItemStack item) {
        Component itemName = item.getItemMeta().displayName();
        if (itemName == null) itemName = Component.text(item.getType().name());

        Bukkit.broadcast(Component.text("[MYTHIC] ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" found ", NamedTextColor.LIGHT_PURPLE))
                .append(itemName)
                .append(Component.text("!", NamedTextColor.LIGHT_PURPLE)));

        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);

        // Mythic Aura upgrade — nearby players get bonus coins
        int auraLevel = upgradeManager.getLevel(player.getUniqueId(), UpgradeType.MYTHIC_AURA);
        if (auraLevel > 0) {
            int[] radii = {5, 10, 15};
            long[] bonusCoins = {50, 100, 200};
            double radius = radii[auraLevel - 1];
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof Player nearby && !nearby.equals(player)) {
                    coinManager.addCoins(nearby.getUniqueId(), bonusCoins[auraLevel - 1]);
                    nearby.sendActionBar(Component.text("+" + bonusCoins[auraLevel - 1] + " Mythic Aura coins!", NamedTextColor.LIGHT_PURPLE));
                }
            }
        }
    }

    private static final Map<Material, Material> SMELT_MAP = Map.of(
            Material.RAW_IRON, Material.IRON_INGOT,
            Material.RAW_GOLD, Material.GOLD_INGOT,
            Material.RAW_COPPER, Material.COPPER_INGOT,
            Material.COBBLESTONE, Material.STONE,
            Material.SAND, Material.GLASS,
            Material.CLAY_BALL, Material.BRICK,
            Material.WET_SPONGE, Material.SPONGE,
            Material.CACTUS, Material.GREEN_DYE
    );

    private Material applyAutoSmelt(UUID uuid, Material original) {
        int autoSmeltLevel = upgradeManager.getLevel(uuid, UpgradeType.AUTO_SMELT);
        if (autoSmeltLevel <= 0) return original;
        double[] chances = {0.15, 0.30, 0.50};
        if (ThreadLocalRandom.current().nextDouble() < chances[autoSmeltLevel - 1]) {
            return SMELT_MAP.getOrDefault(original, original);
        }
        return original;
    }
}
