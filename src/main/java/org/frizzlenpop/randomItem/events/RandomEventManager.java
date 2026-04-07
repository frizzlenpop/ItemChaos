package org.frizzlenpop.randomItem.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.economy.CoinManager;
import org.frizzlenpop.randomItem.mythic.MythicItemRegistry;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RandomEventManager implements Listener {

    private static final String EVENT_ENTITY_TAG = "event_entity";

    private final RandomItem plugin;
    private final CoinManager coinManager;
    private MythicItemRegistry mythicItemRegistry;
    private final List<RandomEvent> events = new ArrayList<>();
    private boolean enabled;
    private int minIntervalSeconds;
    private int maxIntervalSeconds;
    private RandomEvent activeEvent;
    private BukkitTask schedulerTask;
    private BukkitTask activeEventTask;
    private boolean doubleCoinsActive;
    private boolean legendaryMinuteActive;

    public RandomEventManager(RandomItem plugin, CoinManager coinManager) {
        this.plugin = plugin;
        this.coinManager = coinManager;

        plugin.saveResource("events.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "events.yml"));

        this.enabled = config.getBoolean("enabled", true);
        this.minIntervalSeconds = config.getInt("min-interval-seconds", 120);
        this.maxIntervalSeconds = config.getInt("max-interval-seconds", 300);

        List<?> eventList = config.getList("events");
        if (eventList != null) {
            for (Object obj : eventList) {
                if (obj instanceof Map<?, ?> map) {
                    events.add(new RandomEvent(
                            String.valueOf(map.get("name")),
                            String.valueOf(map.get("description")),
                            ((Number) map.get("duration-seconds")).intValue(),
                            ((Number) map.get("weight")).intValue(),
                            String.valueOf(map.get("type"))
                    ));
                }
            }
        }

        if (enabled) {
            scheduleNextEvent();
        }
    }

    public void setMythicItemRegistry(MythicItemRegistry registry) {
        this.mythicItemRegistry = registry;
    }

    public boolean isDoubleCoinsActive() { return doubleCoinsActive; }
    public boolean isLegendaryMinuteActive() { return legendaryMinuteActive; }
    public boolean isEnabled() { return enabled; }

    public void toggle() {
        enabled = !enabled;
        if (enabled) {
            scheduleNextEvent();
        } else {
            if (schedulerTask != null) schedulerTask.cancel();
            endEvent();
        }
    }

    public List<RandomEvent> getEventList() { return events; }

    public void triggerEvent(RandomEvent event) {
        if (activeEvent != null) endEvent();
        activeEvent = event;

        Title title = Title.title(
                Component.text(event.getName(), NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text(event.getDescription(), NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500)));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }

        Bukkit.broadcast(Component.text("[Event] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(event.getName() + " - " + event.getDescription(), NamedTextColor.YELLOW)));

        int dur = event.getDurationSeconds() * 20;

        switch (event.getType()) {
            case "DOUBLE_COINS" -> doubleCoinsActive = true;
            case "GRAVITY_MADNESS" -> applyToAll(PotionEffectType.JUMP_BOOST, dur, 3, PotionEffectType.SLOW_FALLING, dur, 0);
            case "LEGENDARY_MINUTE" -> legendaryMinuteActive = true;
            case "FREEZE_TAG" -> applyToAll(PotionEffectType.SLOWNESS, dur, 2);
            case "COIN_RAIN" -> activeEventTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (online.isEmpty()) return;
                Player lucky = online.get(ThreadLocalRandom.current().nextInt(online.size()));
                long bonus = ThreadLocalRandom.current().nextLong(10, 51);
                coinManager.addCoins(lucky.getUniqueId(), bonus);
                lucky.sendActionBar(Component.text("+" + bonus + " coin rain!", NamedTextColor.GOLD));
                lucky.playSound(lucky.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
            }, 0L, 100L);

            // ===== NEW EVENTS =====
            case "INVISIBILITY_CHAOS" -> applyToAll(PotionEffectType.INVISIBILITY, dur, 0);
            case "SPEED_DEMONS" -> applyToAll(PotionEffectType.SPEED, dur, 2, PotionEffectType.JUMP_BOOST, dur, 1);
            case "DARKNESS_FALLS" -> applyToAll(PotionEffectType.BLINDNESS, dur, 0, PotionEffectType.DARKNESS, dur, 0);

            case "TNT_PARTY" -> activeEventTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (online.isEmpty()) return;
                Player target = online.get(ThreadLocalRandom.current().nextInt(online.size()));
                Location loc = target.getLocation().clone().add(
                        ThreadLocalRandom.current().nextInt(-3, 4), 10,
                        ThreadLocalRandom.current().nextInt(-3, 4));
                TNTPrimed tnt = target.getWorld().spawn(loc, TNTPrimed.class);
                tnt.setFuseTicks(40);
                tnt.setYield(2.0f);
                tnt.setIsIncendiary(false);
                tnt.customName(Component.text(EVENT_ENTITY_TAG));
                tnt.setCustomNameVisible(false);
            }, 0L, 100L);

            case "MOB_MAYHEM" -> activeEventTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    EntityType[] mobs = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER};
                    EntityType mob = mobs[ThreadLocalRandom.current().nextInt(mobs.length)];
                    Location loc = p.getLocation().clone().add(
                            ThreadLocalRandom.current().nextDouble(-5, 5), 0,
                            ThreadLocalRandom.current().nextDouble(-5, 5));
                    Entity e = p.getWorld().spawnEntity(loc, mob);
                    e.customName(Component.text(EVENT_ENTITY_TAG));
                    e.setCustomNameVisible(false);
                }
            }, 0L, 200L);

            case "INVENTORY_ROULETTE" -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    shuffleInventory(p);
                }
            }

            case "COIN_TAX" -> {
                long totalTax = 0;
                List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                for (Player p : online) {
                    long coins = coinManager.getCoins(p.getUniqueId());
                    long tax = coins / 10;
                    if (tax > 0) {
                        coinManager.removeCoins(p.getUniqueId(), tax);
                        totalTax += tax;
                        p.sendMessage(Component.text("Taxed " + tax + " coins!", NamedTextColor.RED));
                    }
                }
                if (totalTax > 0 && !online.isEmpty()) {
                    long perPlayer = totalTax / online.size();
                    for (Player p : online) {
                        coinManager.addCoins(p.getUniqueId(), perPlayer);
                        p.sendMessage(Component.text("Redistributed +" + perPlayer + " coins!", NamedTextColor.GREEN));
                    }
                }
            }

            case "SIZE_SWAP" -> applyToAll(PotionEffectType.SLOWNESS, dur, 2, PotionEffectType.RESISTANCE, dur, 1);

            case "LAVA_RISING" -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, dur, 0, false, true, true));
                }
                activeEventTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.setFireTicks(40);
                        p.playSound(p.getLocation(), Sound.BLOCK_LAVA_POP, 1f, 1f);
                    }
                }, 40L, 40L);
            }

            case "PHANTOM_STORM" -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    for (int i = 0; i < 3; i++) {
                        Location loc = p.getLocation().clone().add(
                                ThreadLocalRandom.current().nextDouble(-5, 5), 8,
                                ThreadLocalRandom.current().nextDouble(-5, 5));
                        Phantom phantom = p.getWorld().spawn(loc, Phantom.class);
                        phantom.customName(Component.text(EVENT_ENTITY_TAG));
                        phantom.setCustomNameVisible(false);
                    }
                }
                // Clean up after duration
                Bukkit.getScheduler().runTaskLater(plugin, this::cleanupEventEntities, dur);
            }

            case "TOOL_SWAP" -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    shuffleHotbar(p);
                }
            }

            case "JACKPOT_RAIN" -> activeEventTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (mythicItemRegistry == null) return;
                List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (online.isEmpty()) return;
                Player lucky = online.get(ThreadLocalRandom.current().nextInt(online.size()));
                ItemStack mythic = mythicItemRegistry.getRandomMythicItem();
                lucky.getWorld().dropItemNaturally(lucky.getLocation().clone().add(0, 5, 0), mythic);
                Bukkit.broadcast(Component.text("[Jackpot] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("A mythic item dropped near " + lucky.getName() + "!", NamedTextColor.YELLOW)));
                lucky.playSound(lucky.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }, 0L, 100L);

            case "HUNGER_GAMES" -> applyToAll(PotionEffectType.HUNGER, dur, 2);

            case "XP_SHOWER" -> activeEventTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Location loc = p.getLocation().clone().add(
                            ThreadLocalRandom.current().nextDouble(-3, 3), 2,
                            ThreadLocalRandom.current().nextDouble(-3, 3));
                    p.getWorld().spawn(loc, ExperienceOrb.class).setExperience(
                            ThreadLocalRandom.current().nextInt(5, 20));
                }
            }, 0L, 20L);

            case "CREEPER_SURPRISE" -> activeEventTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (online.isEmpty()) return;
                Player target = online.get(ThreadLocalRandom.current().nextInt(online.size()));
                Location loc = target.getLocation().clone().add(
                        ThreadLocalRandom.current().nextDouble(-5, 5), 0,
                        ThreadLocalRandom.current().nextDouble(-5, 5));
                Creeper creeper = target.getWorld().spawn(loc, Creeper.class);
                creeper.setPowered(true);
                creeper.customName(Component.text(EVENT_ENTITY_TAG));
                creeper.setCustomNameVisible(false);
                creeper.setExplosionRadius(0); // No terrain damage
            }, 0L, 160L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::endEvent, dur);
    }

    public RandomEvent getEventByName(String name) {
        for (RandomEvent event : events) {
            if (event.getName().equalsIgnoreCase(name)) return event;
        }
        return null;
    }

    private void endEvent() {
        if (activeEvent == null) return;

        doubleCoinsActive = false;
        legendaryMinuteActive = false;
        if (activeEventTask != null) {
            activeEventTask.cancel();
            activeEventTask = null;
        }

        cleanupEventEntities();

        Bukkit.broadcast(Component.text("[Event] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(activeEvent.getName() + " has ended!", NamedTextColor.GRAY)));

        activeEvent = null;

        if (enabled) {
            scheduleNextEvent();
        }
    }

    private void cleanupEventEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.customName() != null &&
                        entity.customName().equals(Component.text(EVENT_ENTITY_TAG))) {
                    entity.remove();
                }
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity().customName() != null &&
                event.getEntity().customName().equals(Component.text(EVENT_ENTITY_TAG))) {
            event.blockList().clear();
        }
    }

    private void applyToAll(PotionEffectType type, int duration, int amplifier) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.addPotionEffect(new PotionEffect(type, duration, amplifier, false, true, true));
        }
    }

    private void applyToAll(PotionEffectType type1, int dur1, int amp1,
                            PotionEffectType type2, int dur2, int amp2) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.addPotionEffect(new PotionEffect(type1, dur1, amp1, false, true, true));
            p.addPotionEffect(new PotionEffect(type2, dur2, amp2, false, true, true));
        }
    }

    private void shuffleInventory(Player player) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            items.add(inv.getItem(i));
            inv.setItem(i, null);
        }
        Collections.shuffle(items);
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, items.get(i));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }

    private void shuffleHotbar(Player player) {
        PlayerInventory inv = player.getInventory();
        List<ItemStack> hotbar = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            hotbar.add(inv.getItem(i));
            inv.setItem(i, null);
        }
        Collections.shuffle(hotbar);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, hotbar.get(i));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
    }

    private void scheduleNextEvent() {
        if (schedulerTask != null) schedulerTask.cancel();
        int delay = ThreadLocalRandom.current().nextInt(minIntervalSeconds, maxIntervalSeconds + 1) * 20;
        schedulerTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!enabled || events.isEmpty()) return;
            triggerEvent(selectWeightedRandom());
        }, delay);
    }

    private RandomEvent selectWeightedRandom() {
        int totalWeight = events.stream().mapToInt(RandomEvent::getWeight).sum();
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int running = 0;
        for (RandomEvent event : events) {
            running += event.getWeight();
            if (roll < running) return event;
        }
        return events.getLast();
    }
}
