package org.frizzlenpop.randomItem.bounty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.economy.CoinManager;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BountyManager implements Listener {

    private static final Type MAP_TYPE = new TypeToken<Map<String, Long>>() {}.getType();

    private final CoinManager coinManager;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File file;
    private final Map<UUID, Long> bounties = new HashMap<>();
    private boolean enabled;
    private long minBounty;
    private long maxBounty;

    public BountyManager(RandomItem plugin, CoinManager coinManager) {
        this.coinManager = coinManager;

        plugin.saveResource("bounties.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "bounties.yml"));

        this.enabled = config.getBoolean("enabled", true);
        this.minBounty = config.getLong("min-bounty", 50);
        this.maxBounty = config.getLong("max-bounty", 10000);

        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "bounties.json");
        load();

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::save, 6000L, 6000L);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }

    public boolean placeBounty(Player placer, Player target, long amount) {
        if (!enabled) {
            placer.sendMessage(Component.text("Bounties are disabled!", NamedTextColor.RED));
            return false;
        }
        if (amount < minBounty || amount > maxBounty) {
            placer.sendMessage(Component.text("Bounty must be between " + minBounty + " and " + maxBounty + " coins!", NamedTextColor.RED));
            return false;
        }
        if (!coinManager.removeCoins(placer.getUniqueId(), amount)) {
            placer.sendMessage(Component.text("Not enough coins!", NamedTextColor.RED));
            return false;
        }

        bounties.merge(target.getUniqueId(), amount, Long::sum);
        long total = bounties.get(target.getUniqueId());

        Bukkit.broadcast(Component.text("[Bounty] ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text(placer.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" placed a ", NamedTextColor.RED))
                .append(Component.text(amount + " coin", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" bounty on ", NamedTextColor.RED))
                .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                .append(Component.text("! Total: " + total, NamedTextColor.GOLD)));

        return true;
    }

    public Map<UUID, Long> getActiveBounties() {
        return Map.copyOf(bounties);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!enabled) return;
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;

        Long bounty = bounties.remove(victim.getUniqueId());
        if (bounty == null || bounty <= 0) return;

        coinManager.addCoins(killer.getUniqueId(), bounty);

        Bukkit.broadcast(Component.text("[Bounty] ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text(killer.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" collected the ", NamedTextColor.GREEN))
                .append(Component.text(bounty + " coin", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" bounty on ", NamedTextColor.GREEN))
                .append(Component.text(victim.getName(), NamedTextColor.YELLOW))
                .append(Component.text("!", NamedTextColor.GREEN)));
    }

    public void save() {
        Map<String, Long> serializable = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : bounties.entrySet()) {
            serializable.put(entry.getKey().toString(), entry.getValue());
        }
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(serializable, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Map<String, Long> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw != null) {
                for (Map.Entry<String, Long> entry : raw.entrySet()) {
                    bounties.put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
