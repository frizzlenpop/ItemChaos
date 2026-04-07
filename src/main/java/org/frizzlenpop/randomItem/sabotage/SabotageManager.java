package org.frizzlenpop.randomItem.sabotage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.economy.CoinManager;
import org.frizzlenpop.randomItem.teams.TeamManager;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SabotageManager implements Listener {

    private static final String SABOTAGE_TAG = "sabotage_entity";

    private final RandomItem plugin;
    private final CoinManager coinManager;
    private final TeamManager teamManager;
    private final Map<UUID, Set<BukkitTask>> activeTasks = new HashMap<>();

    public SabotageManager(RandomItem plugin, CoinManager coinManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.coinManager = coinManager;
        this.teamManager = teamManager;
    }

    public CoinManager getCoinManager() {
        return coinManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public boolean executeSabotage(Player attacker, Player victim, SabotageType type) {
        if (!coinManager.removeCoins(attacker.getUniqueId(), type.getCost())) {
            attacker.sendMessage(Component.text("Not enough coins! Need " + type.getCost(), NamedTextColor.RED));
            return false;
        }

        applySabotage(victim, type);

        Bukkit.broadcast(Component.text("[Sabotage] ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .append(Component.text(attacker.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" used ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(type.getDisplayName(), NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" on ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(victim.getName(), NamedTextColor.YELLOW))
                .append(Component.text("!", NamedTextColor.LIGHT_PURPLE)));

        return true;
    }

    public boolean executeSabotageOnTeam(Player attacker, String teamName, SabotageType type) {
        List<Player> members = teamManager.getOnlineTeamMembers(teamName);
        if (members.isEmpty()) {
            attacker.sendMessage(Component.text("No online players in that team!", NamedTextColor.RED));
            return false;
        }

        long totalCost = (long) type.getCost() * members.size();
        if (!coinManager.removeCoins(attacker.getUniqueId(), totalCost)) {
            attacker.sendMessage(Component.text("Not enough coins! Need " + totalCost +
                    " (" + type.getCost() + " x " + members.size() + " members)", NamedTextColor.RED));
            return false;
        }

        for (Player member : members) {
            applySabotage(member, type);
        }

        Bukkit.broadcast(Component.text("[Sabotage] ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .append(Component.text(attacker.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" used ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(type.getDisplayName(), NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" on team ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(teamName, NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.LIGHT_PURPLE)));

        return true;
    }

    public void applyRandomSabotage(Player victim) {
        SabotageType[] types = SabotageType.values();
        applySabotage(victim, types[ThreadLocalRandom.current().nextInt(types.length)]);
    }

    public void applySabotage(Player victim, SabotageType type) {
        switch (type) {
            case INVENTORY_SHUFFLE -> applyInventoryShuffle(victim);
            case BUTTER_FINGERS -> applyButterFingers(victim);
            case DRUNK_VISION -> applyDrunkVision(victim);
            case GRAVITY_FLIP -> applyGravityFlip(victim);
            case TNT_RAIN -> applyTntRain(victim);
            case CHICKEN_SWARM -> applyChickenSwarm(victim);
            case PHANTOM_MENACE -> applyPhantomMenace(victim);
            case FAKE_DEATH -> applyFakeDeath(victim);
            case HOT_POTATO -> applyHotPotato(victim);
            case ANVIL_RAIN -> applyAnvilRain(victim);
            case COBWEB_TRAP -> applyCobwebTrap(victim);
            case LIGHTNING_STRIKE -> applyLightningStrike(victim);
            case MOB_MAGNET -> applyMobMagnet(victim);
            case NO_JUMP -> applyNoJump(victim);
            case REVERSE_CONTROLS -> applyReverseControls(victim);
            case PIG_ARMY -> applyPigArmy(victim);
            case GLASS_CANNON -> applyGlassCannon(victim);
            case BEES -> applyBees(victim);
        }
    }

    private void applyInventoryShuffle(Player victim) {
        PlayerInventory inv = victim.getInventory();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            items.add(inv.getItem(i));
            inv.setItem(i, null);
        }
        Collections.shuffle(items);
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, items.get(i));
        }
        victim.playSound(victim.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        victim.showTitle(Title.title(
                Component.text("SHUFFLED!", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("Your inventory has been scrambled!", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500))));
    }

    private void applyButterFingers(Player victim) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!victim.isOnline() || ticks >= 15) {
                    cancel();
                    removeTask(victim.getUniqueId(), this.getTaskId());
                    return;
                }
                ticks++;
                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                    ItemStack mainHand = victim.getInventory().getItemInMainHand();
                    if (mainHand.getType() != Material.AIR) {
                        victim.getWorld().dropItemNaturally(victim.getLocation(), mainHand.clone());
                        victim.getInventory().setItemInMainHand(null);
                        victim.playSound(victim.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 0.5f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 60L);
        trackTask(victim.getUniqueId(), task);
    }

    private void applyDrunkVision(Player victim) {
        victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 1200, 0, false, true, true));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1200, 1, false, true, true));
        victim.playSound(victim.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1f, 1f);
    }

    private void applyGravityFlip(Player victim) {
        BukkitTask task = new BukkitRunnable() {
            int launches = 0;

            @Override
            public void run() {
                if (!victim.isOnline() || launches >= 6) {
                    cancel();
                    removeTask(victim.getUniqueId(), this.getTaskId());
                    return;
                }
                launches++;
                victim.setVelocity(new Vector(0, 1.5, 0));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, true, true));
                victim.playSound(victim.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
            }
        }.runTaskTimer(plugin, 0L, 100L);
        trackTask(victim.getUniqueId(), task);
    }

    private void applyTntRain(Player victim) {
        BukkitTask task = new BukkitRunnable() {
            int spawns = 0;

            @Override
            public void run() {
                if (!victim.isOnline() || spawns >= 10) {
                    cancel();
                    removeTask(victim.getUniqueId(), this.getTaskId());
                    return;
                }
                spawns++;
                Location loc = victim.getLocation().clone().add(
                        ThreadLocalRandom.current().nextInt(-3, 4),
                        10,
                        ThreadLocalRandom.current().nextInt(-3, 4));
                TNTPrimed tnt = victim.getWorld().spawn(loc, TNTPrimed.class);
                tnt.setFuseTicks(40);
                tnt.setYield(1.0f);
                tnt.setIsIncendiary(false);
                tnt.customName(Component.text(SABOTAGE_TAG));
                tnt.setCustomNameVisible(false);
                victim.playSound(victim.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1f);
            }
        }.runTaskTimer(plugin, 0L, 60L);
        trackTask(victim.getUniqueId(), task);
    }

    private void applyChickenSwarm(Player victim) {
        Location center = victim.getLocation();
        List<Entity> chickens = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Location spawnLoc = center.clone().add(
                    ThreadLocalRandom.current().nextDouble(-3, 3),
                    0,
                    ThreadLocalRandom.current().nextDouble(-3, 3));
            Chicken chicken = victim.getWorld().spawn(spawnLoc, Chicken.class);
            chicken.customName(Component.text(SABOTAGE_TAG));
            chicken.setCustomNameVisible(false);
            chickens.add(chicken);
        }
        victim.playSound(victim.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 1f, 1f);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity e : chickens) {
                    if (e.isValid()) e.remove();
                }
                removeTask(victim.getUniqueId(), this.getTaskId());
            }
        }.runTaskLater(plugin, 45 * 20L);
        trackTask(victim.getUniqueId(), task);
    }

    private void applyPhantomMenace(Player victim) {
        List<Entity> phantoms = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Location spawnLoc = victim.getLocation().clone().add(
                    ThreadLocalRandom.current().nextDouble(-5, 5),
                    8,
                    ThreadLocalRandom.current().nextDouble(-5, 5));
            Phantom phantom = victim.getWorld().spawn(spawnLoc, Phantom.class);
            phantom.customName(Component.text(SABOTAGE_TAG));
            phantom.setCustomNameVisible(false);
            phantoms.add(phantom);
        }
        victim.playSound(victim.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 1f, 0.5f);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity e : phantoms) {
                    if (e.isValid()) e.remove();
                }
                removeTask(victim.getUniqueId(), this.getTaskId());
            }
        }.runTaskLater(plugin, 60 * 20L);
        trackTask(victim.getUniqueId(), task);
    }

    private void applyFakeDeath(Player victim) {
        Bukkit.broadcast(Component.text(victim.getName() + " was sabotaged to death!", NamedTextColor.RED));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false, false));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, false, false, false));
        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1f, 1f);
        victim.playSound(victim.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1f);
    }

    // ===== NEW SABOTAGES =====

    private void applyHotPotato(Player victim) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!victim.isOnline() || ticks >= 15) {
                    cancel();
                    removeTask(victim.getUniqueId(), this.getTaskId());
                    return;
                }
                ticks++;
                victim.setFireTicks(60);
                victim.playSound(victim.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1f, 1f);
            }
        }.runTaskTimer(plugin, 0L, 40L);
        trackTask(victim.getUniqueId(), task);
    }

    private void applyAnvilRain(Player victim) {
        BukkitTask task = new BukkitRunnable() {
            int spawns = 0;
            @Override
            public void run() {
                if (!victim.isOnline() || spawns >= 10) {
                    cancel();
                    removeTask(victim.getUniqueId(), this.getTaskId());
                    return;
                }
                spawns++;
                Location loc = victim.getLocation().clone().add(
                        ThreadLocalRandom.current().nextInt(-2, 3), 15,
                        ThreadLocalRandom.current().nextInt(-2, 3));
                loc.getWorld().spawn(loc, org.bukkit.entity.FallingBlock.class, fb -> {
                    fb.setBlockData(org.bukkit.Material.ANVIL.createBlockData());
                    fb.setDropItem(false);
                    fb.setHurtEntities(true);
                    fb.customName(Component.text(SABOTAGE_TAG));
                    fb.setCustomNameVisible(false);
                });
                victim.playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1f);
            }
        }.runTaskTimer(plugin, 0L, 60L);
        trackTask(victim.getUniqueId(), task);
    }

    private void applyCobwebTrap(Player victim) {
        List<Location> cobwebLocations = new ArrayList<>();
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!victim.isOnline() || ticks >= 4) {
                    cancel();
                    // Clean up cobwebs
                    for (Location loc : cobwebLocations) {
                        if (loc.getBlock().getType() == Material.COBWEB) {
                            loc.getBlock().setType(Material.AIR);
                        }
                    }
                    removeTask(victim.getUniqueId(), this.getTaskId());
                    return;
                }
                ticks++;
                // Remove old cobwebs
                for (Location loc : cobwebLocations) {
                    if (loc.getBlock().getType() == Material.COBWEB) {
                        loc.getBlock().setType(Material.AIR);
                    }
                }
                cobwebLocations.clear();
                // Place new cobwebs
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Location loc = victim.getLocation().clone().add(dx, 0, dz);
                        if (loc.getBlock().getType() == Material.AIR) {
                            loc.getBlock().setType(Material.COBWEB);
                            cobwebLocations.add(loc);
                        }
                    }
                }
                victim.playSound(victim.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1f, 0.5f);
            }
        }.runTaskTimer(plugin, 0L, 100L);
        trackTask(victim.getUniqueId(), task);
    }

    private void applyLightningStrike(Player victim) {
        BukkitTask task = new BukkitRunnable() {
            int strikes = 0;
            @Override
            public void run() {
                if (!victim.isOnline() || strikes >= 6) {
                    cancel();
                    removeTask(victim.getUniqueId(), this.getTaskId());
                    return;
                }
                strikes++;
                victim.getWorld().strikeLightning(victim.getLocation());
            }
        }.runTaskTimer(plugin, 0L, 100L);
        trackTask(victim.getUniqueId(), task);
    }

    private void applyMobMagnet(Player victim) {
        BukkitTask task = new BukkitRunnable() {
            int waves = 0;
            @Override
            public void run() {
                if (!victim.isOnline() || waves >= 5) {
                    cancel();
                    removeTask(victim.getUniqueId(), this.getTaskId());
                    return;
                }
                waves++;
                org.bukkit.entity.EntityType[] mobs = {
                        org.bukkit.entity.EntityType.ZOMBIE, org.bukkit.entity.EntityType.SKELETON,
                        org.bukkit.entity.EntityType.SPIDER, org.bukkit.entity.EntityType.CREEPER
                };
                for (int i = 0; i < 3; i++) {
                    Location loc = victim.getLocation().clone().add(
                            ThreadLocalRandom.current().nextDouble(-5, 5), 0,
                            ThreadLocalRandom.current().nextDouble(-5, 5));
                    Entity e = victim.getWorld().spawnEntity(loc,
                            mobs[ThreadLocalRandom.current().nextInt(mobs.length)]);
                    e.customName(Component.text(SABOTAGE_TAG));
                    e.setCustomNameVisible(false);
                }
                victim.playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1f, 0.5f);
            }
        }.runTaskTimer(plugin, 0L, 160L);
        trackTask(victim.getUniqueId(), task);
    }

    private void applyNoJump(Player victim) {
        victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 30 * 20, 128,
                false, false, true));
        victim.playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.5f);
    }

    private void applyReverseControls(Player victim) {
        victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 20, 1, false, true, true));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 20, 1, false, true, true));
        BukkitTask task = new BukkitRunnable() {
            int pushes = 0;
            @Override
            public void run() {
                if (!victim.isOnline() || pushes >= 7) {
                    cancel();
                    removeTask(victim.getUniqueId(), this.getTaskId());
                    return;
                }
                pushes++;
                double x = ThreadLocalRandom.current().nextDouble(-0.8, 0.8);
                double z = ThreadLocalRandom.current().nextDouble(-0.8, 0.8);
                victim.setVelocity(new Vector(x, 0.2, z));
                victim.playSound(victim.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 2f);
            }
        }.runTaskTimer(plugin, 0L, 60L);
        trackTask(victim.getUniqueId(), task);
    }

    private void applyPigArmy(Player victim) {
        Location center = victim.getLocation();
        List<Entity> pigs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Location loc = center.clone().add(
                    ThreadLocalRandom.current().nextDouble(-4, 4), 0,
                    ThreadLocalRandom.current().nextDouble(-4, 4));
            // Spawn stacked pigs (2 high)
            org.bukkit.entity.Pig bottom = victim.getWorld().spawn(loc, org.bukkit.entity.Pig.class);
            org.bukkit.entity.Pig top = victim.getWorld().spawn(loc, org.bukkit.entity.Pig.class);
            bottom.addPassenger(top);
            bottom.customName(Component.text(SABOTAGE_TAG));
            top.customName(Component.text(SABOTAGE_TAG));
            bottom.setCustomNameVisible(false);
            top.setCustomNameVisible(false);
            pigs.add(bottom);
            pigs.add(top);
        }
        victim.playSound(victim.getLocation(), Sound.ENTITY_PIG_AMBIENT, 1f, 0.5f);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity e : pigs) {
                    if (e.isValid()) e.remove();
                }
                removeTask(victim.getUniqueId(), this.getTaskId());
            }
        }.runTaskLater(plugin, 45 * 20L);
        trackTask(victim.getUniqueId(), task);
    }

    private void applyGlassCannon(Player victim) {
        // Save armor, remove it, give Strength II
        PlayerInventory inv = victim.getInventory();
        ItemStack[] savedArmor = {
                inv.getHelmet() != null ? inv.getHelmet().clone() : null,
                inv.getChestplate() != null ? inv.getChestplate().clone() : null,
                inv.getLeggings() != null ? inv.getLeggings().clone() : null,
                inv.getBoots() != null ? inv.getBoots().clone() : null
        };
        inv.setHelmet(null);
        inv.setChestplate(null);
        inv.setLeggings(null);
        inv.setBoots(null);

        victim.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 45 * 20, 1, false, true, true));
        victim.playSound(victim.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 0.5f);

        victim.showTitle(Title.title(
                Component.text("GLASS CANNON!", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("Strength II but no armor for 45s!", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500))));

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (victim.isOnline()) {
                    PlayerInventory pInv = victim.getInventory();
                    if (savedArmor[0] != null) pInv.setHelmet(savedArmor[0]);
                    if (savedArmor[1] != null) pInv.setChestplate(savedArmor[1]);
                    if (savedArmor[2] != null) pInv.setLeggings(savedArmor[2]);
                    if (savedArmor[3] != null) pInv.setBoots(savedArmor[3]);
                    victim.sendMessage(Component.text("Your armor has been returned!", NamedTextColor.GREEN));
                }
                removeTask(victim.getUniqueId(), this.getTaskId());
            }
        }.runTaskLater(plugin, 45 * 20L);
        trackTask(victim.getUniqueId(), task);
    }

    private void applyBees(Player victim) {
        List<Entity> bees = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Location loc = victim.getLocation().clone().add(
                    ThreadLocalRandom.current().nextDouble(-3, 3), 1,
                    ThreadLocalRandom.current().nextDouble(-3, 3));
            org.bukkit.entity.Bee bee = victim.getWorld().spawn(loc, org.bukkit.entity.Bee.class);
            bee.setAnger(600); // Max anger
            bee.setTarget(victim);
            bee.customName(Component.text(SABOTAGE_TAG));
            bee.setCustomNameVisible(false);
            bees.add(bee);
        }
        victim.playSound(victim.getLocation(), Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1f, 1f);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity e : bees) {
                    if (e.isValid()) e.remove();
                }
                removeTask(victim.getUniqueId(), this.getTaskId());
            }
        }.runTaskLater(plugin, 30 * 20L);
        trackTask(victim.getUniqueId(), task);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity().customName() != null &&
                event.getEntity().customName().equals(Component.text(SABOTAGE_TAG))) {
            event.blockList().clear();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Set<BukkitTask> tasks = activeTasks.remove(uuid);
        if (tasks != null) {
            for (BukkitTask task : tasks) {
                task.cancel();
            }
        }
        // Clean up sabotage entities near the player
        for (Entity entity : event.getPlayer().getNearbyEntities(50, 50, 50)) {
            if (entity.customName() != null &&
                    entity.customName().equals(Component.text(SABOTAGE_TAG))) {
                entity.remove();
            }
        }
    }

    private void trackTask(UUID uuid, BukkitTask task) {
        activeTasks.computeIfAbsent(uuid, k -> new HashSet<>()).add(task);
    }

    private void removeTask(UUID uuid, int taskId) {
        Set<BukkitTask> tasks = activeTasks.get(uuid);
        if (tasks != null) {
            tasks.removeIf(t -> t.getTaskId() == taskId);
        }
    }
}
