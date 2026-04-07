package org.frizzlenpop.randomItem.hotzone;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.economy.CoinManager;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class HotZoneManager {

    private final RandomItem plugin;
    private final CoinManager coinManager;
    private final File configFile;
    private boolean enabled;
    private int durationSeconds;
    private int coinRatePerSecond;
    private int radius;
    private int broadcastIntervalSeconds;
    private boolean useFixedCenter;
    private String worldName;
    private double fixedX, fixedY, fixedZ;
    private int rMinX, rMaxX, rMinZ, rMaxZ;
    private String randomWorldName;

    private boolean active;
    private Location center;
    private BukkitTask coinTask;
    private BukkitTask particleTask;
    private BukkitTask broadcastTask;

    public HotZoneManager(RandomItem plugin, CoinManager coinManager) {
        this.plugin = plugin;
        this.coinManager = coinManager;
        this.configFile = new File(plugin.getDataFolder(), "hotzone.yml");

        plugin.saveResource("hotzone.yml", false);
        loadConfig();
    }

    private void loadConfig() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        this.enabled = config.getBoolean("enabled", true);
        this.durationSeconds = config.getInt("zone-duration-seconds", 120);
        this.coinRatePerSecond = config.getInt("coin-rate-per-second", 5);
        this.radius = config.getInt("zone-radius", 10);
        this.broadcastIntervalSeconds = config.getInt("broadcast-interval-seconds", 15);
        this.useFixedCenter = config.getBoolean("use-fixed-center", false);
        this.worldName = config.getString("center.world", "world");
        this.fixedX = config.getDouble("center.x", 0);
        this.fixedY = config.getDouble("center.y", 64);
        this.fixedZ = config.getDouble("center.z", 0);
        this.randomWorldName = config.getString("random-bounds.world", "world");
        this.rMinX = config.getInt("random-bounds.min-x", -200);
        this.rMaxX = config.getInt("random-bounds.max-x", 200);
        this.rMinZ = config.getInt("random-bounds.min-z", -200);
        this.rMaxZ = config.getInt("random-bounds.max-z", 200);
    }

    public boolean isEnabled() { return enabled; }
    public boolean isActive() { return active; }

    public void toggle() {
        enabled = !enabled;
        if (!enabled && active) stopZone();
    }

    public void startZone() {
        if (active) stopZone();

        if (useFixedCenter) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) return;
            center = new Location(world, fixedX, fixedY, fixedZ);
        } else {
            World world = Bukkit.getWorld(randomWorldName);
            if (world == null) return;
            int x = ThreadLocalRandom.current().nextInt(rMinX, rMaxX + 1);
            int z = ThreadLocalRandom.current().nextInt(rMinZ, rMaxZ + 1);
            int y = world.getHighestBlockYAt(x, z) + 1;
            center = new Location(world, x, y, z);
        }

        active = true;

        Title title = Title.title(
                Component.text("HOT ZONE!", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("Zone at " + center.getBlockX() + " " + center.getBlockY() + " " + center.getBlockZ(), NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500)));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
        }

        Bukkit.broadcast(Component.text("[Hot Zone] ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("Zone active at ", NamedTextColor.YELLOW))
                .append(Component.text(center.getBlockX() + " " + center.getBlockY() + " " + center.getBlockZ(),
                        NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(" for " + durationSeconds + "s! Earn " + coinRatePerSecond + " coins/sec!", NamedTextColor.YELLOW)));

        // Coin award task - every second
        coinTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(center.getWorld()) && p.getLocation().distance(center) <= radius) {
                    coinManager.addCoins(p.getUniqueId(), coinRatePerSecond);
                    p.sendActionBar(Component.text("+" + coinRatePerSecond + " zone coins!", NamedTextColor.GOLD));
                }
            }
        }, 0L, 20L);

        // Particle border
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (center == null) return;
            for (int angle = 0; angle < 360; angle += 10) {
                double rad = Math.toRadians(angle);
                double x = center.getX() + 0.5 + radius * Math.cos(rad);
                double z = center.getZ() + 0.5 + radius * Math.sin(rad);
                center.getWorld().spawnParticle(Particle.FLAME,
                        new Location(center.getWorld(), x, center.getY() + 1, z), 1, 0, 0, 0, 0);
            }
        }, 0L, 10L);

        // Broadcast who's in zone
        broadcastTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<String> inZone = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(center.getWorld()) && p.getLocation().distance(center) <= radius) {
                    inZone.add(p.getName());
                }
            }
            if (!inZone.isEmpty()) {
                Bukkit.broadcast(Component.text("[Hot Zone] ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("In zone: " + String.join(", ", inZone), NamedTextColor.YELLOW)));
            }
        }, broadcastIntervalSeconds * 20L, broadcastIntervalSeconds * 20L);

        // End timer
        Bukkit.getScheduler().runTaskLater(plugin, this::stopZone, durationSeconds * 20L);
    }

    public void stopZone() {
        if (!active) return;
        active = false;
        if (coinTask != null) { coinTask.cancel(); coinTask = null; }
        if (particleTask != null) { particleTask.cancel(); particleTask = null; }
        if (broadcastTask != null) { broadcastTask.cancel(); broadcastTask = null; }
        center = null;

        Bukkit.broadcast(Component.text("[Hot Zone] ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("The hot zone has ended!", NamedTextColor.GRAY)));
    }

    public void setCenter(Location loc) {
        useFixedCenter = true;
        worldName = loc.getWorld().getName();
        fixedX = loc.getX();
        fixedY = loc.getY();
        fixedZ = loc.getZ();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("use-fixed-center", true);
        config.set("center.world", worldName);
        config.set("center.x", fixedX);
        config.set("center.y", fixedY);
        config.set("center.z", fixedZ);
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
