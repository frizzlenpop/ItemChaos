package org.frizzlenpop.randomItem.pinata;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.economy.CoinDropUtil;
import org.frizzlenpop.randomItem.economy.CoinManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PinataManager implements Listener {

    private final RandomItem plugin;
    private final PinataConfig config;
    private final CoinManager coinManager;
    private final Map<UUID, PinataDefinition> activePinatas = new HashMap<>();

    public PinataManager(RandomItem plugin, PinataConfig config, CoinManager coinManager) {
        this.plugin = plugin;
        this.config = config;
        this.coinManager = coinManager;
    }

    public PinataConfig getConfig() {
        return config;
    }

    @SuppressWarnings("deprecation")
    public void spawnPinata(String pinataKey, Location location) {
        PinataDefinition def = config.getPinata(pinataKey);
        if (def == null) return;

        Entity entity = location.getWorld().spawnEntity(location, def.getEntityType());
        if (!(entity instanceof LivingEntity living)) {
            entity.remove();
            return;
        }

        living.customName(Component.text(ChatColor.translateAlternateColorCodes('&', def.getName())));
        living.setCustomNameVisible(true);
        living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false, false));
        living.setRemoveWhenFarAway(false);

        if (living.getAttribute(Attribute.MAX_HEALTH) != null) {
            living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(def.getHealth());
            living.setHealth(def.getHealth());
        }

        // Make passive mobs not run away by reducing speed
        if (living.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            living.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1);
        }

        activePinatas.put(entity.getUniqueId(), def);

        Bukkit.broadcast(Component.text("[PINATA] ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .append(Component.text(ChatColor.translateAlternateColorCodes('&', def.getName()), NamedTextColor.YELLOW))
                .append(Component.text(" has appeared! Beat it for loot!", NamedTextColor.GREEN)));

        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        PinataDefinition def = activePinatas.remove(event.getEntity().getUniqueId());
        if (def == null) return;

        event.getDrops().clear();
        event.setDroppedExp(0);

        Location deathLoc = event.getEntity().getLocation();

        // Scatter loot items
        for (PinataLootEntry entry : def.getLoot()) {
            if (ThreadLocalRandom.current().nextInt(100) < entry.getChance()) {
                ItemStack item = new ItemStack(entry.getMaterial(), entry.getAmount());
                deathLoc.getWorld().dropItemNaturally(deathLoc.clone().add(
                        ThreadLocalRandom.current().nextDouble(-2, 2), 0.5,
                        ThreadLocalRandom.current().nextDouble(-2, 2)), item);
            }
        }

        // Scatter coins
        CoinDropUtil.scatterCoins(deathLoc, def.getCoinScatter(), 10, 4);

        // Visual effects
        deathLoc.getWorld().spawnParticle(Particle.FIREWORK, deathLoc.clone().add(0, 1, 0), 100, 2, 2, 2, 0.1);
        deathLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, deathLoc.clone().add(0, 1, 0), 50, 2, 1, 2, 0);

        Bukkit.broadcast(Component.text("[PINATA] ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .append(Component.text(ChatColor.stripColor(
                        ChatColor.translateAlternateColorCodes('&', def.getName())), NamedTextColor.YELLOW))
                .append(Component.text(" has been destroyed! Loot everywhere!", NamedTextColor.GREEN)));

        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.2f);
        }
    }
}
