package org.frizzlenpop.randomItem.boss;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.economy.CoinManager;
import org.frizzlenpop.randomItem.teams.TeamManager;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BossManager implements Listener {

    private static final String BOSS_TAG = "chaos_boss";
    private static final String ADD_TAG = "chaos_boss_add";

    private final RandomItem plugin;
    private final BossConfig config;
    private final CoinManager coinManager;
    private final TeamManager teamManager;
    private final Map<UUID, BossDefinition> activeBosses = new HashMap<>();
    private final Map<UUID, Map<UUID, Double>> damageTrackers = new HashMap<>();
    private final Map<UUID, BukkitTask> addSpawnTasks = new HashMap<>();
    private final Set<UUID> addEntities = new HashSet<>();

    public BossManager(RandomItem plugin, BossConfig config, CoinManager coinManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.config = config;
        this.coinManager = coinManager;
        this.teamManager = teamManager;
    }

    public BossConfig getConfig() {
        return config;
    }

    @SuppressWarnings("deprecation")
    public void spawnBoss(String bossKey, Location location) {
        BossDefinition def = config.getBoss(bossKey);
        if (def == null) return;

        Entity entity = location.getWorld().spawnEntity(location, def.getEntityType());
        if (!(entity instanceof LivingEntity living)) {
            entity.remove();
            return;
        }

        living.customName(Component.text(ChatColor.translateAlternateColorCodes('&', def.getName())));
        living.setCustomNameVisible(true);
        living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false, false));

        if (living.getAttribute(Attribute.MAX_HEALTH) != null) {
            living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(def.getHealth());
            living.setHealth(def.getHealth());
        }
        if (living.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            living.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(def.getSpeed());
        }
        if (living.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            living.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(def.getDamage());
        }

        living.setRemoveWhenFarAway(false);

        activeBosses.put(entity.getUniqueId(), def);
        damageTrackers.put(entity.getUniqueId(), new HashMap<>());

        // Spawn adds if configured
        if (def.isSpawnAdds()) {
            BukkitTask addTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!entity.isValid()) return;
                for (int i = 0; i < def.getAddCount(); i++) {
                    Location spawnLoc = entity.getLocation().clone().add(
                            ThreadLocalRandom.current().nextDouble(-5, 5), 0,
                            ThreadLocalRandom.current().nextDouble(-5, 5));
                    Entity add = entity.getWorld().spawnEntity(spawnLoc, def.getAddEntityType());
                    add.customName(Component.text(ADD_TAG));
                    add.setCustomNameVisible(false);
                    addEntities.add(add.getUniqueId());
                }
            }, def.getAddIntervalSeconds() * 20L, def.getAddIntervalSeconds() * 20L);
            addSpawnTasks.put(entity.getUniqueId(), addTask);
        }

        Bukkit.broadcast(Component.text("[BOSS] ", NamedTextColor.DARK_RED, TextDecoration.BOLD)
                .append(Component.text(ChatColor.translateAlternateColorCodes('&', def.getName()), NamedTextColor.RED))
                .append(Component.text(" has spawned!", NamedTextColor.YELLOW)));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.5f);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        UUID bossId = event.getEntity().getUniqueId();
        if (!activeBosses.containsKey(bossId)) return;

        Player damager = null;
        if (event.getDamager() instanceof Player p) {
            damager = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            damager = p;
        }
        if (damager == null) return;

        damageTrackers.get(bossId).merge(damager.getUniqueId(), event.getFinalDamage(), Double::sum);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        UUID entityId = event.getEntity().getUniqueId();

        // Clean up add entities silently
        if (addEntities.remove(entityId)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            return;
        }

        BossDefinition def = activeBosses.remove(entityId);
        if (def == null) return;

        // Cancel add spawner
        BukkitTask addTask = addSpawnTasks.remove(entityId);
        if (addTask != null) addTask.cancel();

        // Clean up remaining adds
        for (Entity e : event.getEntity().getNearbyEntities(50, 50, 50)) {
            if (addEntities.remove(e.getUniqueId())) e.remove();
        }

        // Drop loot
        event.getDrops().clear();
        event.setDroppedExp(0);
        Location deathLoc = event.getEntity().getLocation();
        for (BossLootEntry loot : def.getLoot()) {
            deathLoc.getWorld().dropItemNaturally(deathLoc, new ItemStack(loot.getMaterial(), loot.getAmount()));
        }

        // Distribute coins proportionally
        Map<UUID, Double> damages = damageTrackers.remove(entityId);
        if (damages != null && !damages.isEmpty()) {
            double totalDamage = damages.values().stream().mapToDouble(Double::doubleValue).sum();
            for (Map.Entry<UUID, Double> entry : damages.entrySet()) {
                double proportion = entry.getValue() / totalDamage;
                long reward = Math.round(def.getCoinReward() * proportion);
                if (reward > 0) {
                    coinManager.addCoins(entry.getKey(), reward);
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p != null) {
                        p.sendMessage(Component.text("+" + reward + " coins from boss kill!", NamedTextColor.GOLD));
                    }
                }
            }
        }

        // Effects
        deathLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, deathLoc, 3, 1, 1, 1, 0);

        Bukkit.broadcast(Component.text("[BOSS] ", NamedTextColor.DARK_RED, TextDecoration.BOLD)
                .append(Component.text(ChatColor.stripColor(
                        ChatColor.translateAlternateColorCodes('&', def.getName())), NamedTextColor.RED))
                .append(Component.text(" has been defeated!", NamedTextColor.GREEN)));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
    }
}
