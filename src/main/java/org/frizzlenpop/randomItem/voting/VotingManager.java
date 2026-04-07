package org.frizzlenpop.randomItem.voting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.boss.BossManager;
import org.frizzlenpop.randomItem.economy.CoinManager;
import org.frizzlenpop.randomItem.events.RandomEvent;
import org.frizzlenpop.randomItem.events.RandomEventManager;
import org.frizzlenpop.randomItem.pinata.PinataManager;
import org.frizzlenpop.randomItem.sabotage.SabotageManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class VotingManager {

    private final RandomItem plugin;
    private final CoinManager coinManager;
    private final RandomEventManager randomEventManager;
    private final SabotageManager sabotageManager;
    private final BossManager bossManager;
    private final PinataManager pinataManager;
    private final List<VoteOption> optionPool = new ArrayList<>();
    private WebServer webServer;
    private boolean enabled;
    private int port;
    private int voteDurationSeconds;
    private int voteIntervalSeconds;
    private int optionsPerRound;

    // Current round state
    private volatile List<VoteOption> currentOptions;
    private AtomicIntegerArray voteCounts;
    private final ConcurrentHashMap<String, Boolean> voterIPs = new ConcurrentHashMap<>();
    private volatile long roundEndTime;
    private volatile boolean roundActive;
    private BukkitTask intervalTask;
    private BukkitTask endRoundTask;

    public VotingManager(RandomItem plugin, CoinManager coinManager, RandomEventManager randomEventManager,
                         SabotageManager sabotageManager, BossManager bossManager, PinataManager pinataManager) {
        this.plugin = plugin;
        this.coinManager = coinManager;
        this.randomEventManager = randomEventManager;
        this.sabotageManager = sabotageManager;
        this.bossManager = bossManager;
        this.pinataManager = pinataManager;
    }

    public void initialize() {
        plugin.saveResource("voting.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "voting.yml"));

        this.enabled = config.getBoolean("enabled", true);
        this.port = config.getInt("port", 8080);
        this.voteDurationSeconds = config.getInt("vote-duration-seconds", 60);
        this.voteIntervalSeconds = config.getInt("vote-interval-seconds", 300);
        this.optionsPerRound = config.getInt("options-per-round", 4);

        List<?> optList = config.getList("options");
        if (optList != null) {
            for (Object obj : optList) {
                if (obj instanceof Map<?, ?> map) {
                    optionPool.add(new VoteOption(
                            String.valueOf(map.get("type")),
                            String.valueOf(map.containsKey("target") ? map.get("target") : ""),
                            String.valueOf(map.get("description"))
                    ));
                }
            }
        }

        if (enabled) {
            try {
                webServer = new WebServer(this, port);
                webServer.start();
                plugin.getLogger().info("Voting web server started on port " + port);
                scheduleNextRound();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to start voting web server on port " + port + ": " + e.getMessage());
                enabled = false;
            }
        }
    }

    public boolean isEnabled() { return enabled; }

    public void toggleEnabled() {
        enabled = !enabled;
        if (enabled) {
            try {
                if (webServer == null) {
                    webServer = new WebServer(this, port);
                    webServer.start();
                }
                scheduleNextRound();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to start voting web server: " + e.getMessage());
                enabled = false;
            }
        } else {
            if (roundActive) endRound();
            if (intervalTask != null) intervalTask.cancel();
        }
    }

    public void startRound() {
        if (optionPool.isEmpty()) return;
        if (roundActive) endRound();

        // Pick random options
        List<VoteOption> shuffled = new ArrayList<>(optionPool);
        Collections.shuffle(shuffled);
        currentOptions = shuffled.subList(0, Math.min(optionsPerRound, shuffled.size()));
        voteCounts = new AtomicIntegerArray(currentOptions.size());
        voterIPs.clear();
        roundEndTime = System.currentTimeMillis() + voteDurationSeconds * 1000L;
        roundActive = true;

        Bukkit.broadcast(Component.text("[VOTE] ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text("A new vote has started! Visit ", NamedTextColor.YELLOW))
                .append(Component.text("http://your-server:" + port, NamedTextColor.AQUA, TextDecoration.UNDERLINED))
                .append(Component.text(" to vote! (" + voteDurationSeconds + "s)", NamedTextColor.YELLOW)));

        for (int i = 0; i < currentOptions.size(); i++) {
            Bukkit.broadcast(Component.text("  " + (i + 1) + ". " + currentOptions.get(i).getDescription(), NamedTextColor.WHITE));
        }

        endRoundTask = Bukkit.getScheduler().runTaskLater(plugin, this::endRound, voteDurationSeconds * 20L);
    }

    public void endRound() {
        if (!roundActive) return;
        roundActive = false;
        if (endRoundTask != null) {
            endRoundTask.cancel();
            endRoundTask = null;
        }

        if (currentOptions == null || voteCounts == null) {
            scheduleNextRound();
            return;
        }

        // Find winner
        int winnerIdx = 0;
        int maxVotes = 0;
        int totalVotes = 0;
        for (int i = 0; i < currentOptions.size(); i++) {
            int count = voteCounts.get(i);
            totalVotes += count;
            if (count > maxVotes) {
                maxVotes = count;
                winnerIdx = i;
            }
        }

        if (totalVotes == 0) {
            Bukkit.broadcast(Component.text("[VOTE] ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text("No votes cast! Round skipped.", NamedTextColor.GRAY)));
        } else {
            VoteOption winner = currentOptions.get(winnerIdx);
            Bukkit.broadcast(Component.text("[VOTE] ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text("Winner: ", NamedTextColor.YELLOW))
                    .append(Component.text(winner.getDescription(), NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" (" + maxVotes + "/" + totalVotes + " votes)", NamedTextColor.GRAY)));

            Bukkit.getScheduler().runTask(plugin, () -> executeOption(winner));
        }

        if (enabled) scheduleNextRound();
    }

    public boolean castVote(String ip, int optionIndex) {
        if (!roundActive) return false;
        if (currentOptions == null || optionIndex < 0 || optionIndex >= currentOptions.size()) return false;
        if (voterIPs.putIfAbsent(ip, true) != null) return false; // Already voted
        voteCounts.incrementAndGet(optionIndex);
        return true;
    }

    public VoteStatus getStatus() {
        if (!roundActive || currentOptions == null) {
            return new VoteStatus(false, List.of(), new int[0], 0);
        }
        int[] counts = new int[currentOptions.size()];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = voteCounts.get(i);
        }
        long remaining = Math.max(0, roundEndTime - System.currentTimeMillis());
        return new VoteStatus(true, currentOptions, counts, remaining);
    }

    public void shutdown() {
        if (webServer != null) webServer.stop();
        if (intervalTask != null) intervalTask.cancel();
        if (endRoundTask != null) endRoundTask.cancel();
    }

    private void scheduleNextRound() {
        if (intervalTask != null) intervalTask.cancel();
        intervalTask = Bukkit.getScheduler().runTaskLater(plugin, this::startRound, voteIntervalSeconds * 20L);
    }

    private void executeOption(VoteOption option) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return;

        switch (option.getType()) {
            case "TRIGGER_EVENT" -> {
                RandomEvent event = randomEventManager.getEventByName(option.getTarget());
                if (event != null) randomEventManager.triggerEvent(event);
            }
            case "GLOBAL_SABOTAGE" -> {
                for (Player p : online) {
                    sabotageManager.applyRandomSabotage(p);
                }
            }
            case "SABOTAGE_PLAYER" -> {
                Player target = online.get(ThreadLocalRandom.current().nextInt(online.size()));
                sabotageManager.applyRandomSabotage(target);
                Bukkit.broadcast(Component.text("[VOTE] ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text(target.getName() + " was chosen for sabotage!", NamedTextColor.RED)));
            }
            case "GIVE_COINS" -> {
                long amount = Long.parseLong(option.getTarget());
                for (Player p : online) {
                    coinManager.addCoins(p.getUniqueId(), amount);
                    p.sendMessage(Component.text("+" + amount + " coins from vote!", NamedTextColor.GOLD));
                }
            }
            case "TAKE_COINS" -> {
                int pct = Integer.parseInt(option.getTarget());
                for (Player p : online) {
                    long coins = coinManager.getCoins(p.getUniqueId());
                    long tax = coins * pct / 100;
                    coinManager.removeCoins(p.getUniqueId(), tax);
                    p.sendMessage(Component.text("-" + tax + " coins from vote tax!", NamedTextColor.RED));
                }
            }
            case "SPAWN_BOSS" -> {
                Player target = online.get(ThreadLocalRandom.current().nextInt(online.size()));
                List<String> bossNames = bossManager.getConfig().getBossNames();
                if (!bossNames.isEmpty()) {
                    String boss = bossNames.get(ThreadLocalRandom.current().nextInt(bossNames.size()));
                    bossManager.spawnBoss(boss, target.getLocation());
                }
            }
            case "SPAWN_PINATA" -> {
                Player target = online.get(ThreadLocalRandom.current().nextInt(online.size()));
                List<String> pinataNames = pinataManager.getConfig().getPinataNames();
                if (!pinataNames.isEmpty()) {
                    String pinata = pinataNames.get(ThreadLocalRandom.current().nextInt(pinataNames.size()));
                    pinataManager.spawnPinata(pinata, target.getLocation());
                }
            }
            case "DOUBLE_COINS_BOOST" -> {
                RandomEvent event = randomEventManager.getEventByName("Double Coins");
                if (event != null) randomEventManager.triggerEvent(event);
            }
        }
    }

    public record VoteStatus(boolean active, List<VoteOption> options, int[] voteCounts, long timeRemainingMs) {}
}
