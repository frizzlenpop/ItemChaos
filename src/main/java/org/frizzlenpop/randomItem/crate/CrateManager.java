package org.frizzlenpop.randomItem.crate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.frizzlenpop.randomItem.RandomItem;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CrateManager implements Listener {

    private final RandomItem plugin;
    private final CrateConfig config;
    private boolean enabled;
    private BukkitTask spawnTask;
    private BukkitTask particleTask;
    // Support multiple active crates: location -> crate ID
    private final Map<Location, Integer> activeCrates = new HashMap<>();
    private int nextCrateId = 0;

    public CrateManager(RandomItem plugin, CrateConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.enabled = config.isEnabled();

        if (enabled) {
            startSpawnTimer();
            startParticleTask();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) {
            startSpawnTimer();
            startParticleTask();
        } else {
            if (spawnTask != null) spawnTask.cancel();
            if (particleTask != null) particleTask.cancel();
            removeAllCrates();
        }
    }

    public void forceSpawn() {
        spawnCrate();
    }

    private void startSpawnTimer() {
        if (spawnTask != null) spawnTask.cancel();
        long interval = config.getSpawnIntervalMinutes() * 60L * 20L;
        spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnCrate, interval, interval);
    }

    private void startParticleTask() {
        if (particleTask != null) particleTask.cancel();
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Location loc : activeCrates.keySet()) {
                Location particleLoc = loc.clone().add(0.5, 1, 0.5);
                for (int i = 0; i < 20; i++) {
                    loc.getWorld().spawnParticle(Particle.END_ROD, particleLoc.clone().add(0, i * 0.5, 0), 1, 0, 0, 0, 0);
                }
            }
        }, 0L, 10L);
    }

    private void spawnCrate() {
        World world = Bukkit.getWorld(config.getWorldName());
        if (world == null) return;

        int x = ThreadLocalRandom.current().nextInt(config.getMinX(), config.getMaxX() + 1);
        int z = ThreadLocalRandom.current().nextInt(config.getMinZ(), config.getMaxZ() + 1);
        int y = world.getHighestBlockYAt(x, z) + 1;

        Location loc = new Location(world, x, y, z);
        loc.getBlock().setType(Material.ENDER_CHEST);
        activeCrates.put(loc, nextCrateId++);

        Bukkit.broadcast(Component.text("[CRATE] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text("A mystery crate has appeared at ", NamedTextColor.YELLOW))
                .append(Component.text(x + " " + y + " " + z, NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.YELLOW)));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
        }
    }

    private void removeCrate(Location loc) {
        if (loc.getBlock().getType() == Material.ENDER_CHEST) {
            loc.getBlock().setType(Material.AIR);
        }
        activeCrates.remove(loc);
    }

    private void removeAllCrates() {
        Iterator<Location> it = activeCrates.keySet().iterator();
        while (it.hasNext()) {
            Location loc = it.next();
            if (loc.getBlock().getType() == Material.ENDER_CHEST) {
                loc.getBlock().setType(Material.AIR);
            }
            it.remove();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (activeCrates.isEmpty()) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Location clickedLoc = clicked.getLocation();
        if (!activeCrates.containsKey(clickedLoc)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        // Roll loot
        for (CrateLootEntry entry : config.getLootTable()) {
            if (ThreadLocalRandom.current().nextInt(100) < entry.getChance()) {
                ItemStack item = new ItemStack(entry.getMaterial(), entry.getAmount());
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        }

        Bukkit.broadcast(Component.text("[CRATE] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" claimed a mystery crate!", NamedTextColor.GREEN)));

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        clickedLoc.getWorld().spawnParticle(Particle.FIREWORK,
                clickedLoc.clone().add(0.5, 1, 0.5), 50, 0.5, 1, 0.5, 0.1);

        removeCrate(clickedLoc);
    }
}
