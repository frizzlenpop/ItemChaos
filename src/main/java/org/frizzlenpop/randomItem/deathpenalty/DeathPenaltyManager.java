package org.frizzlenpop.randomItem.deathpenalty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.economy.CoinDropUtil;
import org.frizzlenpop.randomItem.economy.CoinManager;

import java.io.File;

public class DeathPenaltyManager implements Listener {

    private final CoinManager coinManager;
    private boolean enabled;
    private int dropPercentage;
    private int scatterRadius;
    private int nuggetCount;

    public DeathPenaltyManager(RandomItem plugin, CoinManager coinManager) {
        this.coinManager = coinManager;

        plugin.saveResource("deathpenalty.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "deathpenalty.yml"));

        this.enabled = config.getBoolean("enabled", true);
        this.dropPercentage = config.getInt("drop-percentage", 20);
        this.scatterRadius = config.getInt("scatter-radius", 3);
        this.nuggetCount = config.getInt("nugget-count", 5);
    }

    public void toggle() {
        enabled = !enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!enabled) return;
        Player victim = event.getEntity();
        long coins = coinManager.getCoins(victim.getUniqueId());
        if (coins <= 0) return;

        long dropAmount = coins * dropPercentage / 100;
        if (dropAmount <= 0) return;

        coinManager.removeCoins(victim.getUniqueId(), dropAmount);
        CoinDropUtil.scatterCoins(victim.getLocation(), dropAmount, nuggetCount, scatterRadius);

        victim.sendMessage(Component.text("You dropped " + dropAmount + " coins!", NamedTextColor.RED));
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Item itemEntity = event.getItem();
        long coinValue = CoinDropUtil.getCoinValue(itemEntity.getItemStack());
        if (coinValue <= 0) return;

        event.setCancelled(true);
        itemEntity.remove();
        coinManager.addCoins(player.getUniqueId(), coinValue);
        player.sendActionBar(Component.text("+" + coinValue + " coins picked up!", NamedTextColor.GOLD));
    }
}
