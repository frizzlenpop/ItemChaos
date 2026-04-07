package org.frizzlenpop.randomItem.gambling;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.economy.CoinManager;
import org.frizzlenpop.randomItem.sabotage.SabotageManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GamblingManager {

    private static final List<Material> RARE_ITEMS = List.of(
            Material.DIAMOND, Material.DIAMOND_BLOCK, Material.EMERALD, Material.NETHERITE_INGOT,
            Material.ENCHANTED_GOLDEN_APPLE, Material.ELYTRA, Material.TOTEM_OF_UNDYING,
            Material.NETHER_STAR, Material.TRIDENT, Material.BEACON
    );

    private final RandomItem plugin;
    private final CoinManager coinManager;
    private final SabotageManager sabotageManager;
    private boolean enabled;
    private long minBet;
    private long maxBet;
    private int cooldownSeconds;
    private final List<GambleOutcome> outcomes = new ArrayList<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public GamblingManager(RandomItem plugin, CoinManager coinManager, SabotageManager sabotageManager) {
        this.plugin = plugin;
        this.coinManager = coinManager;
        this.sabotageManager = sabotageManager;

        plugin.saveResource("gambling.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "gambling.yml"));

        this.enabled = config.getBoolean("enabled", true);
        this.minBet = config.getLong("min-bet", 10);
        this.maxBet = config.getLong("max-bet", 5000);
        this.cooldownSeconds = config.getInt("cooldown-seconds", 10);

        List<?> outcomeList = config.getList("outcomes");
        if (outcomeList != null) {
            for (Object obj : outcomeList) {
                if (obj instanceof Map<?, ?> map) {
                    outcomes.add(new GambleOutcome(
                            String.valueOf(map.get("type")),
                            String.valueOf(map.get("display")),
                            ((Number) map.get("weight")).intValue()
                    ));
                }
            }
        }
    }

    public boolean isEnabled() { return enabled; }

    public void toggle() { enabled = !enabled; }

    public void gamble(Player player, long amount) {
        if (!enabled) {
            player.sendMessage(Component.text("Gambling is disabled!", NamedTextColor.RED));
            return;
        }
        if (amount < minBet || amount > maxBet) {
            player.sendMessage(Component.text("Bet must be between " + minBet + " and " + maxBet + " coins!", NamedTextColor.RED));
            return;
        }

        long now = System.currentTimeMillis();
        Long lastGamble = cooldowns.get(player.getUniqueId());
        if (lastGamble != null && (now - lastGamble) < cooldownSeconds * 1000L) {
            int remaining = (int) ((cooldownSeconds * 1000L - (now - lastGamble)) / 1000) + 1;
            player.sendMessage(Component.text("Cooldown! Wait " + remaining + "s", NamedTextColor.RED));
            return;
        }

        if (!coinManager.removeCoins(player.getUniqueId(), amount)) {
            player.sendMessage(Component.text("Not enough coins!", NamedTextColor.RED));
            return;
        }

        cooldowns.put(player.getUniqueId(), now);
        GambleOutcome result = selectWeightedRandom();

        // Animated spin
        new BukkitRunnable() {
            int tick = 0;
            final String[] spinFrames = {"| ? | ? | ? |", "| ! | ? | ! |", "| ? | ! | ? |"};

            @Override
            public void run() {
                if (tick < 3) {
                    player.sendMessage(Component.text("[Gamble] Spinning... " + spinFrames[tick], NamedTextColor.YELLOW));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f + tick * 0.2f);
                    tick++;
                } else {
                    cancel();
                    applyResult(player, amount, result);
                }
            }
        }.runTaskTimer(plugin, 0L, 15L);
    }

    @SuppressWarnings("deprecation")
    private void applyResult(Player player, long amount, GambleOutcome outcome) {
        String display = org.bukkit.ChatColor.translateAlternateColorCodes('&', outcome.display());

        switch (outcome.type()) {
            case "WIN_2X" -> {
                long winnings = amount * 2;
                coinManager.addCoins(player.getUniqueId(), winnings);
                player.sendMessage(Component.text("[Gamble] " + display + " Won " + winnings + " coins!", NamedTextColor.GREEN));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
            case "WIN_3X" -> {
                long winnings = amount * 3;
                coinManager.addCoins(player.getUniqueId(), winnings);
                player.sendMessage(Component.text("[Gamble] " + display + " Won " + winnings + " coins!", NamedTextColor.GOLD, TextDecoration.BOLD));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                Bukkit.broadcast(Component.text("[Gamble] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" hit a 3x JACKPOT! Won " + winnings + " coins!", NamedTextColor.GOLD)));
            }
            case "LOSE" -> {
                player.sendMessage(Component.text("[Gamble] " + display + " Lost " + amount + " coins!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
            case "RANDOM_ITEM" -> {
                Material mat = RARE_ITEMS.get(ThreadLocalRandom.current().nextInt(RARE_ITEMS.size()));
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(mat));
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                player.sendMessage(Component.text("[Gamble] " + display + " Got " + mat.name() + "!", NamedTextColor.LIGHT_PURPLE));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            }
            case "SELF_SABOTAGE" -> {
                player.sendMessage(Component.text("[Gamble] " + display, NamedTextColor.DARK_RED));
                sabotageManager.applyRandomSabotage(player);
            }
        }
    }

    private GambleOutcome selectWeightedRandom() {
        int totalWeight = outcomes.stream().mapToInt(GambleOutcome::weight).sum();
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int running = 0;
        for (GambleOutcome outcome : outcomes) {
            running += outcome.weight();
            if (roll < running) return outcome;
        }
        return outcomes.getLast();
    }

    private record GambleOutcome(String type, String display, int weight) {}
}
