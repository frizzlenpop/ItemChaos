package org.frizzlenpop.randomItem.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.economy.CoinManager;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class RandomEventManager {

    private final RandomItem plugin;
    private final CoinManager coinManager;
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

    public boolean isDoubleCoinsActive() {
        return doubleCoinsActive;
    }

    public boolean isLegendaryMinuteActive() {
        return legendaryMinuteActive;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) {
            scheduleNextEvent();
        } else {
            if (schedulerTask != null) schedulerTask.cancel();
            endEvent();
        }
    }

    public List<RandomEvent> getEventList() {
        return events;
    }

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

        switch (event.getType()) {
            case "DOUBLE_COINS" -> doubleCoinsActive = true;
            case "GRAVITY_MADNESS" -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, event.getDurationSeconds() * 20, 3, false, true, true));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, event.getDurationSeconds() * 20, 0, false, true, true));
                }
            }
            case "LEGENDARY_MINUTE" -> legendaryMinuteActive = true;
            case "FREEZE_TAG" -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, event.getDurationSeconds() * 20, 2, false, true, true));
                }
            }
            case "COIN_RAIN" -> {
                activeEventTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                    if (online.isEmpty()) return;
                    Player lucky = online.get(ThreadLocalRandom.current().nextInt(online.size()));
                    long bonus = ThreadLocalRandom.current().nextLong(10, 51);
                    coinManager.addCoins(lucky.getUniqueId(), bonus);
                    lucky.sendActionBar(Component.text("+" + bonus + " coin rain!", NamedTextColor.GOLD));
                    lucky.playSound(lucky.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                }, 0L, 100L);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::endEvent, event.getDurationSeconds() * 20L);
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

        Bukkit.broadcast(Component.text("[Event] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(activeEvent.getName() + " has ended!", NamedTextColor.GRAY)));

        activeEvent = null;

        if (enabled) {
            scheduleNextEvent();
        }
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
