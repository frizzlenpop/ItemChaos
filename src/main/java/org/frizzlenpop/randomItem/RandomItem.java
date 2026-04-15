package org.frizzlenpop.randomItem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.frizzlenpop.randomItem.blockadex.BlockadexManager;
import org.frizzlenpop.randomItem.blockadex.BlockadexMilestone;
import org.frizzlenpop.randomItem.boss.BossConfig;
import org.frizzlenpop.randomItem.boss.BossDefinition;
import org.frizzlenpop.randomItem.boss.BossManager;
import org.frizzlenpop.randomItem.bounty.BountyManager;
import org.frizzlenpop.randomItem.crate.CrateConfig;
import org.frizzlenpop.randomItem.crate.CrateManager;
import org.frizzlenpop.randomItem.deathpenalty.DeathPenaltyManager;
import org.frizzlenpop.randomItem.economy.CoinDropUtil;
import org.frizzlenpop.randomItem.economy.CoinManager;
import org.frizzlenpop.randomItem.events.RandomEvent;
import org.frizzlenpop.randomItem.events.RandomEventManager;
import org.frizzlenpop.randomItem.gambling.GamblingManager;
import org.frizzlenpop.randomItem.hotzone.HotZoneManager;
import org.frizzlenpop.randomItem.leaderboard.LeaderboardManager;
import org.frizzlenpop.randomItem.loottiers.LootTierManager;
import org.frizzlenpop.randomItem.mythic.MythicItemRegistry;
import org.frizzlenpop.randomItem.pinata.PinataConfig;
import org.frizzlenpop.randomItem.pinata.PinataManager;
import org.frizzlenpop.randomItem.sabotage.SabotageConfig;
import org.frizzlenpop.randomItem.sabotage.SabotageGUI;
import org.frizzlenpop.randomItem.sabotage.SabotageManager;
import org.frizzlenpop.randomItem.shop.ShopConfig;
import org.frizzlenpop.randomItem.shop.ShopGUI;
import org.frizzlenpop.randomItem.teams.Team;
import org.frizzlenpop.randomItem.teams.TeamManager;
import org.frizzlenpop.randomItem.tradeup.TradeUpConfig;
import org.frizzlenpop.randomItem.tradeup.TradeUpGUI;
import org.frizzlenpop.randomItem.upgrade.UpgradeConfig;
import org.frizzlenpop.randomItem.upgrade.UpgradeGUI;
import org.frizzlenpop.randomItem.upgrade.UpgradeManager;
import org.frizzlenpop.randomItem.voting.VotingManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RandomItem extends JavaPlugin {

    private boolean chaosEnabled = true;
    private CoinManager coinManager;
    private UpgradeManager upgradeManager;
    private UpgradeGUI upgradeGUI;
    private ShopConfig shopConfig;
    private ShopGUI shopGUI;
    private TeamManager teamManager;
    private SabotageManager sabotageManager;
    private SabotageGUI sabotageGUI;
    private BossConfig bossConfig;
    private BossManager bossManager;
    private PinataConfig pinataConfig;
    private PinataManager pinataManager;
    private BountyManager bountyManager;
    private CrateConfig crateConfig;
    private CrateManager crateManager;
    private GamblingManager gamblingManager;
    private HotZoneManager hotZoneManager;
    private RandomEventManager randomEventManager;
    private TradeUpConfig tradeUpConfig;
    private TradeUpGUI tradeUpGUI;
    private DeathPenaltyManager deathPenaltyManager;
    private LeaderboardManager leaderboardManager;
    private BlockadexManager blockadexManager;
    private LootTierManager lootTierManager;
    private MythicItemRegistry mythicItemRegistry;
    private VotingManager votingManager;

    @Override
    public void onEnable() {
        // Main config
        MainConfig mainConfig = new MainConfig(this);
        mainConfig.load();

        // Foundation
        CoinDropUtil.initKey(this);
        coinManager = new CoinManager(this);
        UpgradeConfig upgradeConfig = new UpgradeConfig(this);
        upgradeConfig.load();
        upgradeManager = new UpgradeManager(this, coinManager, upgradeConfig);
        teamManager = new TeamManager(this);

        // Existing systems
        upgradeGUI = new UpgradeGUI(upgradeManager, coinManager, upgradeConfig);
        shopConfig = new ShopConfig(this);
        shopGUI = new ShopGUI(shopConfig, coinManager);
        SabotageConfig sabotageConfig = new SabotageConfig(this);
        sabotageConfig.load();
        sabotageManager = new SabotageManager(this, coinManager, teamManager, upgradeManager, sabotageConfig);
        sabotageGUI = new SabotageGUI(sabotageManager, coinManager, teamManager, sabotageConfig);

        // New systems
        bossConfig = new BossConfig(this);
        bossManager = new BossManager(this, bossConfig, coinManager, teamManager);
        pinataConfig = new PinataConfig(this);
        pinataManager = new PinataManager(this, pinataConfig, coinManager);
        bountyManager = new BountyManager(this, coinManager);
        crateConfig = new CrateConfig(this);
        crateManager = new CrateManager(this, crateConfig);
        gamblingManager = new GamblingManager(this, coinManager, sabotageManager);
        hotZoneManager = new HotZoneManager(this, coinManager);
        randomEventManager = new RandomEventManager(this, coinManager);
        tradeUpConfig = new TradeUpConfig(this);
        tradeUpGUI = new TradeUpGUI(tradeUpConfig, coinManager);
        deathPenaltyManager = new DeathPenaltyManager(this, coinManager, upgradeManager);
        leaderboardManager = new LeaderboardManager(this, coinManager, teamManager);
        blockadexManager = new BlockadexManager(this);
        lootTierManager = new LootTierManager(this);
        mythicItemRegistry = new MythicItemRegistry(this);
        randomEventManager.setMythicItemRegistry(mythicItemRegistry);
        votingManager = new VotingManager(this, coinManager, randomEventManager,
                sabotageManager, bossManager, pinataManager);
        votingManager.initialize();

        // Register all listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new RandomDropListener(this, coinManager, upgradeManager, randomEventManager, blockadexManager, lootTierManager, mythicItemRegistry, mainConfig), this);
        pm.registerEvents(upgradeManager, this);
        pm.registerEvents(upgradeGUI, this);
        pm.registerEvents(shopGUI, this);
        pm.registerEvents(sabotageManager, this);
        pm.registerEvents(sabotageGUI, this);
        pm.registerEvents(bossManager, this);
        pm.registerEvents(pinataManager, this);
        pm.registerEvents(bountyManager, this);
        pm.registerEvents(crateManager, this);
        pm.registerEvents(randomEventManager, this);
        pm.registerEvents(tradeUpGUI, this);
        pm.registerEvents(deathPenaltyManager, this);
        pm.registerEvents(leaderboardManager, this);
        pm.registerEvents(blockadexManager, this);
        pm.registerEvents(lootTierManager, this);

        getLogger().info("Pure Random Drops enabled with all systems!");
    }

    @Override
    public void onDisable() {
        if (coinManager != null) coinManager.save();
        if (upgradeManager != null) upgradeManager.save();
        if (teamManager != null) teamManager.save();
        if (bountyManager != null) bountyManager.save();
        if (blockadexManager != null) blockadexManager.save();
        if (lootTierManager != null) lootTierManager.save();
        if (votingManager != null) votingManager.shutdown();
    }

    public boolean isChaosEnabled() {
        return chaosEnabled;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "chaos" -> handleChaos(sender, args);
            case "upgrade" -> handlePlayerCommand(sender, p -> upgradeGUI.openGUI(p));
            case "shop" -> handlePlayerCommand(sender, p -> shopGUI.openGUI(p, 0));
            case "sabotage" -> handlePlayerCommand(sender, p -> sabotageGUI.openTargetGUI(p));
            case "teams" -> handleTeams(sender, args);
            case "boss" -> handleBoss(sender, args);
            case "pinata" -> handlePinata(sender, args);
            case "bounty" -> handleBounty(sender, args);
            case "crate" -> handleCrate(sender, args);
            case "gamble" -> handleGamble(sender, args);
            case "hotzone" -> handleHotZone(sender, args);
            case "event" -> handleEvent(sender, args);
            case "tradeup" -> handlePlayerCommand(sender, p -> tradeUpGUI.openGUI(p));
            case "deathpenalty" -> handleDeathPenalty(sender, args);
            case "leaderboard" -> handleLeaderboard(sender, args);
            case "winners" -> handleWinners(sender);
            case "coins" -> handleCoins(sender, args);
            case "blockadex" -> handleBlockadex(sender, args);
            case "loottiers" -> handleLootTiers(sender, args);
            case "mythic" -> handleMythic(sender, args);
            case "vote" -> handleVote(sender, args);
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("chaos") && args.length == 1) {
            return filter(List.of("toggle", "coins"), args[0]);
        }
        if (cmd.equals("teams")) {
            if (args.length == 1) return filter(List.of("create", "add", "remove", "list", "disband"), args[0]);
            if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("disband"))) {
                List<String> names = new ArrayList<>();
                for (Team t : teamManager.getAllTeams()) names.add(t.getName());
                return filter(names, args[1]);
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("add")) return null;
            if (args.length == 2 && args[0].equalsIgnoreCase("remove")) return null;
        }
        if (cmd.equals("boss")) {
            if (args.length == 1) return filter(List.of("spawn", "setanchor", "list"), args[0]);
            if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) return filter(bossConfig.getBossNames(), args[1]);
            if (args.length == 2 && args[0].equalsIgnoreCase("setanchor")) {
                List<String> names = new ArrayList<>();
                for (Team t : teamManager.getAllTeams()) names.add(t.getName());
                return filter(names, args[1]);
            }
        }
        if (cmd.equals("pinata")) {
            if (args.length == 1) return filter(List.of("spawn", "setlocation", "list"), args[0]);
            if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) return filter(pinataConfig.getPinataNames(), args[1]);
        }
        if (cmd.equals("bounty")) {
            if (args.length == 1) return filter(List.of("set", "list", "toggle"), args[0]);
            if (args.length == 2 && args[0].equalsIgnoreCase("set")) return null;
        }
        if (cmd.equals("crate") && args.length == 1) return filter(List.of("spawn", "toggle"), args[0]);
        if (cmd.equals("gamble") && args.length == 1) return filter(List.of("toggle"), args[0]);
        if (cmd.equals("hotzone") && args.length == 1) return filter(List.of("start", "stop", "setcenter"), args[0]);
        if (cmd.equals("event")) {
            if (args.length == 1) return filter(List.of("trigger", "toggle", "list"), args[0]);
            if (args.length == 2 && args[0].equalsIgnoreCase("trigger")) {
                List<String> names = new ArrayList<>();
                for (RandomEvent e : randomEventManager.getEventList()) names.add(e.getName());
                return filter(names, args[1]);
            }
        }
        if (cmd.equals("deathpenalty") && args.length == 1) return filter(List.of("toggle"), args[0]);
        if (cmd.equals("leaderboard") && args.length == 1) return filter(List.of("toggle", "reset"), args[0]);
        if (cmd.equals("blockadex") && args.length == 1) return filter(List.of("top", "toggle"), args[0]);
        if (cmd.equals("vote") && args.length == 1) return filter(List.of("toggle", "start", "stop"), args[0]);
        if (cmd.equals("mythic")) {
            if (args.length == 1) return filter(List.of("give", "list"), args[0]);
            if (args.length == 2 && args[0].equalsIgnoreCase("give")) return null; // player names
            if (args.length == 3 && args[0].equalsIgnoreCase("give")) return filter(List.of("random"), args[2]);
        }
        if (cmd.equals("loottiers")) {
            if (args.length == 1) return filter(List.of("top", "toggle", "reset"), args[0]);
            if (args.length == 2 && args[0].equalsIgnoreCase("reset")) return null; // player names
        }
        if (cmd.equals("coins")) {
            if (args.length == 1) return filter(List.of("give", "remove", "set"), args[0]);
            if (args.length == 2) return null;
        }

        return List.of();
    }

    // ===== Command Handlers =====

    private boolean handleChaos(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /chaos <toggle|coins>", NamedTextColor.YELLOW));
            return true;
        }
        if (args[0].equalsIgnoreCase("toggle")) {
            if (!sender.hasPermission("chaos.admin")) {
                sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                return true;
            }
            chaosEnabled = !chaosEnabled;
            String state = chaosEnabled ? "\u00a7aENABLED" : "\u00a7cDISABLED";
            sender.sendMessage("\u00a76[Chaos] \u00a7fRandom drops are now " + state);
            return true;
        }
        if (args[0].equalsIgnoreCase("coins")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can check coins.");
                return true;
            }
            long coins = coinManager.getCoins(player.getUniqueId());
            player.sendMessage(Component.text("Your coins: " + coins, NamedTextColor.GOLD));
            return true;
        }
        sender.sendMessage(Component.text("Usage: /chaos <toggle|coins>", NamedTextColor.YELLOW));
        return true;
    }

    private boolean handlePlayerCommand(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        action.accept(player);
        return true;
    }

    private boolean handleTeams(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /teams <create|add|remove|list|disband>", NamedTextColor.YELLOW));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /teams create <name>", NamedTextColor.YELLOW)); return true; }
                if (teamManager.createTeam(args[1]))
                    sender.sendMessage(Component.text("Team '" + args[1] + "' created!", NamedTextColor.GREEN));
                else
                    sender.sendMessage(Component.text("Failed! Max 4 teams or name taken.", NamedTextColor.RED));
            }
            case "add" -> {
                if (args.length < 3) { sender.sendMessage(Component.text("Usage: /teams add <team> <player>", NamedTextColor.YELLOW)); return true; }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) { sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED)); return true; }
                if (teamManager.addPlayer(args[1], target.getUniqueId()))
                    sender.sendMessage(Component.text(target.getName() + " added to '" + args[1] + "'!", NamedTextColor.GREEN));
                else
                    sender.sendMessage(Component.text("Failed! Team not found or player on a team.", NamedTextColor.RED));
            }
            case "remove" -> {
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /teams remove <player>", NamedTextColor.YELLOW)); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED)); return true; }
                if (teamManager.removePlayer(target.getUniqueId()))
                    sender.sendMessage(Component.text(args[1] + " removed from their team!", NamedTextColor.GREEN));
                else
                    sender.sendMessage(Component.text("Player not on any team.", NamedTextColor.RED));
            }
            case "list" -> {
                if (teamManager.getAllTeams().isEmpty()) {
                    sender.sendMessage(Component.text("No teams created.", NamedTextColor.GRAY));
                    return true;
                }
                sender.sendMessage(Component.text("=== Teams ===", NamedTextColor.GOLD, TextDecoration.BOLD));
                for (Team team : teamManager.getAllTeams()) {
                    List<String> names = new ArrayList<>();
                    for (UUID uuid : team.getMembers()) {
                        Player p = Bukkit.getPlayer(uuid);
                        names.add(p != null ? p.getName() : uuid.toString().substring(0, 8) + "...");
                    }
                    String members = names.isEmpty() ? "(empty)" : String.join(", ", names);
                    sender.sendMessage(Component.text(team.getName() + ": ", NamedTextColor.AQUA)
                            .append(Component.text(members, NamedTextColor.WHITE)));
                }
            }
            case "disband" -> {
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /teams disband <team>", NamedTextColor.YELLOW)); return true; }
                if (teamManager.disbandTeam(args[1]))
                    sender.sendMessage(Component.text("Team '" + args[1] + "' disbanded!", NamedTextColor.GREEN));
                else
                    sender.sendMessage(Component.text("Team not found!", NamedTextColor.RED));
            }
            default -> sender.sendMessage(Component.text("Usage: /teams <create|add|remove|list|disband>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleBoss(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /boss <spawn|setanchor|list>", NamedTextColor.YELLOW)); return true; }
        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /boss spawn <name>", NamedTextColor.YELLOW)); return true; }
                if (!(sender instanceof Player player)) { sender.sendMessage("Must be a player."); return true; }
                BossDefinition def = bossConfig.getBoss(args[1]);
                if (def == null) { sender.sendMessage(Component.text("Boss not found! Use /boss list", NamedTextColor.RED)); return true; }
                bossManager.spawnBoss(args[1], player.getLocation());
                sender.sendMessage(Component.text("Boss spawned!", NamedTextColor.GREEN));
            }
            case "setanchor" -> {
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /boss setanchor <team>", NamedTextColor.YELLOW)); return true; }
                if (!(sender instanceof Player player)) { sender.sendMessage("Must be a player."); return true; }
                if (teamManager.getTeam(args[1]) == null) { sender.sendMessage(Component.text("Team not found!", NamedTextColor.RED)); return true; }
                bossConfig.setAnchor(args[1], player.getLocation());
                sender.sendMessage(Component.text("Anchor set for team '" + args[1] + "'!", NamedTextColor.GREEN));
            }
            case "list" -> {
                sender.sendMessage(Component.text("=== Bosses ===", NamedTextColor.DARK_RED, TextDecoration.BOLD));
                for (BossDefinition def : bossConfig.getAllBosses()) {
                    sender.sendMessage(Component.text("- " + def.getKey() + ": ", NamedTextColor.RED)
                            .append(Component.text(def.getEntityType() + " | HP: " + def.getHealth() + " | Reward: " + def.getCoinReward(), NamedTextColor.GRAY)));
                }
            }
            default -> sender.sendMessage(Component.text("Usage: /boss <spawn|setanchor|list>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handlePinata(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /pinata <spawn|setlocation|list>", NamedTextColor.YELLOW)); return true; }
        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /pinata spawn <type>", NamedTextColor.YELLOW)); return true; }
                if (!(sender instanceof Player player)) { sender.sendMessage("Must be a player."); return true; }
                if (pinataConfig.getPinata(args[1]) == null) { sender.sendMessage(Component.text("Pinata not found! Use /pinata list", NamedTextColor.RED)); return true; }
                pinataManager.spawnPinata(args[1], player.getLocation());
                sender.sendMessage(Component.text("Pinata spawned!", NamedTextColor.GREEN));
            }
            case "setlocation" -> {
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /pinata setlocation <name>", NamedTextColor.YELLOW)); return true; }
                if (!(sender instanceof Player player)) { sender.sendMessage("Must be a player."); return true; }
                pinataConfig.setLocation(args[1], player.getLocation());
                sender.sendMessage(Component.text("Location '" + args[1] + "' saved!", NamedTextColor.GREEN));
            }
            case "list" -> {
                sender.sendMessage(Component.text("=== Pinatas ===", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
                for (var def : pinataConfig.getAllPinatas()) {
                    sender.sendMessage(Component.text("- " + def.getKey() + ": ", NamedTextColor.LIGHT_PURPLE)
                            .append(Component.text(def.getEntityType() + " | HP: " + def.getHealth() + " | Coins: " + def.getCoinScatter(), NamedTextColor.GRAY)));
                }
            }
            default -> sender.sendMessage(Component.text("Usage: /pinata <spawn|setlocation|list>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleBounty(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /bounty <set|list|toggle>", NamedTextColor.YELLOW)); return true; }
        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (args.length < 3) { sender.sendMessage(Component.text("Usage: /bounty set <player> <amount>", NamedTextColor.YELLOW)); return true; }
                if (!(sender instanceof Player player)) { sender.sendMessage("Must be a player."); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED)); return true; }
                try {
                    long amount = Long.parseLong(args[2]);
                    bountyManager.placeBounty(player, target, amount);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid amount!", NamedTextColor.RED));
                }
            }
            case "list" -> {
                Map<UUID, Long> bounties = bountyManager.getActiveBounties();
                if (bounties.isEmpty()) { sender.sendMessage(Component.text("No active bounties.", NamedTextColor.GRAY)); return true; }
                sender.sendMessage(Component.text("=== Active Bounties ===", NamedTextColor.RED, TextDecoration.BOLD));
                for (Map.Entry<UUID, Long> entry : bounties.entrySet()) {
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    if (name == null) name = entry.getKey().toString().substring(0, 8);
                    sender.sendMessage(Component.text(name + ": ", NamedTextColor.YELLOW)
                            .append(Component.text(entry.getValue() + " coins", NamedTextColor.GOLD)));
                }
            }
            case "toggle" -> {
                if (!sender.hasPermission("chaos.admin")) { sender.sendMessage(Component.text("No permission.", NamedTextColor.RED)); return true; }
                bountyManager.toggle();
                sender.sendMessage(Component.text("Bounties " + (bountyManager.isEnabled() ? "enabled" : "disabled") + "!", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Usage: /bounty <set|list|toggle>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleCrate(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /crate <spawn|toggle>", NamedTextColor.YELLOW)); return true; }
        switch (args[0].toLowerCase()) {
            case "spawn" -> { crateManager.forceSpawn(); sender.sendMessage(Component.text("Crate spawned!", NamedTextColor.GREEN)); }
            case "toggle" -> { crateManager.toggle(); sender.sendMessage(Component.text("Crates " + (crateManager.isEnabled() ? "enabled" : "disabled") + "!", NamedTextColor.GREEN)); }
            default -> sender.sendMessage(Component.text("Usage: /crate <spawn|toggle>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleGamble(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /gamble <amount|toggle>", NamedTextColor.YELLOW)); return true; }
        if (args[0].equalsIgnoreCase("toggle")) {
            if (!sender.hasPermission("chaos.admin")) { sender.sendMessage(Component.text("No permission.", NamedTextColor.RED)); return true; }
            gamblingManager.toggle();
            sender.sendMessage(Component.text("Gambling " + (gamblingManager.isEnabled() ? "enabled" : "disabled") + "!", NamedTextColor.GREEN));
            return true;
        }
        if (!(sender instanceof Player player)) { sender.sendMessage("Must be a player."); return true; }
        try {
            long amount = Long.parseLong(args[0]);
            gamblingManager.gamble(player, amount);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Usage: /gamble <amount>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleHotZone(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /hotzone <start|stop|setcenter>", NamedTextColor.YELLOW)); return true; }
        switch (args[0].toLowerCase()) {
            case "start" -> { hotZoneManager.startZone(); sender.sendMessage(Component.text("Hot zone started!", NamedTextColor.GREEN)); }
            case "stop" -> { hotZoneManager.stopZone(); sender.sendMessage(Component.text("Hot zone stopped!", NamedTextColor.GREEN)); }
            case "setcenter" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Must be a player."); return true; }
                hotZoneManager.setCenter(player.getLocation());
                sender.sendMessage(Component.text("Hot zone center set!", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Usage: /hotzone <start|stop|setcenter>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleEvent(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /event <trigger|toggle|list>", NamedTextColor.YELLOW)); return true; }
        switch (args[0].toLowerCase()) {
            case "trigger" -> {
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /event trigger <name>", NamedTextColor.YELLOW)); return true; }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                RandomEvent event = randomEventManager.getEventByName(name);
                if (event == null) { sender.sendMessage(Component.text("Event not found! Use /event list", NamedTextColor.RED)); return true; }
                randomEventManager.triggerEvent(event);
                sender.sendMessage(Component.text("Event triggered!", NamedTextColor.GREEN));
            }
            case "toggle" -> {
                randomEventManager.toggle();
                sender.sendMessage(Component.text("Random events " + (randomEventManager.isEnabled() ? "enabled" : "disabled") + "!", NamedTextColor.GREEN));
            }
            case "list" -> {
                sender.sendMessage(Component.text("=== Random Events ===", NamedTextColor.GOLD, TextDecoration.BOLD));
                for (RandomEvent event : randomEventManager.getEventList()) {
                    sender.sendMessage(Component.text("- " + event.getName() + ": ", NamedTextColor.YELLOW)
                            .append(Component.text(event.getDescription() + " (" + event.getDurationSeconds() + "s)", NamedTextColor.GRAY)));
                }
            }
            default -> sender.sendMessage(Component.text("Usage: /event <trigger|toggle|list>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleDeathPenalty(CommandSender sender, String[] args) {
        if (args.length < 1 || !args[0].equalsIgnoreCase("toggle")) {
            sender.sendMessage(Component.text("Usage: /deathpenalty toggle", NamedTextColor.YELLOW));
            return true;
        }
        deathPenaltyManager.toggle();
        sender.sendMessage(Component.text("Death penalty " + (deathPenaltyManager.isEnabled() ? "enabled" : "disabled") + "!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleLeaderboard(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /leaderboard <toggle|reset>", NamedTextColor.YELLOW)); return true; }
        switch (args[0].toLowerCase()) {
            case "toggle" -> {
                leaderboardManager.toggle();
                sender.sendMessage(Component.text("Leaderboard " + (leaderboardManager.isEnabled() ? "enabled" : "disabled") + "!", NamedTextColor.GREEN));
            }
            case "reset" -> {
                leaderboardManager.reset();
                sender.sendMessage(Component.text("Leaderboard reset!", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Usage: /leaderboard <toggle|reset>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleWinners(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Must be a player."); return true; }
        leaderboardManager.showWinners(player);
        return true;
    }

    private boolean handleVote(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /vote <toggle|start|stop>", NamedTextColor.YELLOW)); return true; }
        if (!sender.hasPermission("chaos.admin")) { sender.sendMessage(Component.text("No permission.", NamedTextColor.RED)); return true; }
        switch (args[0].toLowerCase()) {
            case "toggle" -> {
                votingManager.toggleEnabled();
                sender.sendMessage(Component.text("Voting " + (votingManager.isEnabled() ? "enabled" : "disabled") + "!", NamedTextColor.GREEN));
            }
            case "start" -> {
                votingManager.startRound();
                sender.sendMessage(Component.text("Vote round started!", NamedTextColor.GREEN));
            }
            case "stop" -> {
                votingManager.endRound();
                sender.sendMessage(Component.text("Vote round stopped!", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Usage: /vote <toggle|start|stop>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleMythic(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /mythic <give|list>", NamedTextColor.YELLOW)); return true; }
        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(Component.text("=== Mythic Items (50) ===", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
            List<ItemStack> items = mythicItemRegistry.getAllItems();
            for (int i = 0; i < items.size(); i++) {
                Component name = items.get(i).getItemMeta().displayName();
                if (name == null) name = Component.text(items.get(i).getType().name());
                sender.sendMessage(Component.text((i + 1) + ". ", NamedTextColor.GRAY).append(name));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) { sender.sendMessage(Component.text("Usage: /mythic give <player> <number|random>", NamedTextColor.YELLOW)); return true; }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED)); return true; }

            ItemStack item;
            if (args[2].equalsIgnoreCase("random")) {
                item = mythicItemRegistry.getRandomMythicItem();
            } else {
                try {
                    int index = Integer.parseInt(args[2]) - 1;
                    List<ItemStack> items = mythicItemRegistry.getAllItems();
                    if (index < 0 || index >= items.size()) {
                        sender.sendMessage(Component.text("Invalid number! Use 1-" + items.size(), NamedTextColor.RED));
                        return true;
                    }
                    item = items.get(index).clone();
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                    return true;
                }
            }

            HashMap<Integer, ItemStack> overflow = target.getInventory().addItem(item);
            for (ItemStack leftover : overflow.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), leftover);
            }

            Component itemName = item.getItemMeta().displayName();
            if (itemName == null) itemName = Component.text(item.getType().name());
            sender.sendMessage(Component.text("Gave ", NamedTextColor.GREEN).append(itemName)
                    .append(Component.text(" to " + target.getName(), NamedTextColor.GREEN)));
            return true;
        }
        sender.sendMessage(Component.text("Usage: /mythic <give|list>", NamedTextColor.YELLOW));
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleLootTiers(CommandSender sender, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("toggle")) {
            if (!sender.hasPermission("chaos.admin")) { sender.sendMessage(Component.text("No permission.", NamedTextColor.RED)); return true; }
            lootTierManager.toggle();
            sender.sendMessage(Component.text("Loot Tiers " + (lootTierManager.isEnabled() ? "enabled" : "disabled") + "!", NamedTextColor.GREEN));
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("chaos.admin")) { sender.sendMessage(Component.text("No permission.", NamedTextColor.RED)); return true; }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED)); return true; }
            lootTierManager.resetPlayer(target.getUniqueId());
            sender.sendMessage(Component.text("Reset " + target.getName() + "'s loot tier progress!", NamedTextColor.GREEN));
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("top")) {
            sender.sendMessage(Component.text("=== Top Miners ===", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
            Map<UUID, Long> top = lootTierManager.getTopMiners(5);
            int rank = 1;
            for (Map.Entry<UUID, Long> entry : top.entrySet()) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) name = entry.getKey().toString().substring(0, 8);
                LootTierManager.LootTier tier = lootTierManager.getCurrentTier(entry.getKey());
                String tierName = tier != null ? org.bukkit.ChatColor.translateAlternateColorCodes('&', tier.name()) : "None";
                sender.sendMessage(Component.text("#" + rank + " ", NamedTextColor.GRAY)
                        .append(Component.text(name, NamedTextColor.YELLOW))
                        .append(Component.text(" - " + entry.getValue() + " blocks - " + tierName, NamedTextColor.GOLD)));
                rank++;
            }
            return true;
        }

        // Default: show own progress
        if (!(sender instanceof Player player)) { sender.sendMessage("Must be a player."); return true; }
        UUID uuid = player.getUniqueId();
        long mined = lootTierManager.getBlocksMined(uuid);
        LootTierManager.LootTier current = lootTierManager.getCurrentTier(uuid);
        LootTierManager.LootTier next = lootTierManager.getNextTier(uuid);

        player.sendMessage(Component.text("=== Loot Tiers ===", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        player.sendMessage(Component.text("Blocks mined: ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(String.valueOf(mined), NamedTextColor.GREEN, TextDecoration.BOLD)));

        if (current != null) {
            String coloredName = org.bukkit.ChatColor.translateAlternateColorCodes('&', current.name());
            player.sendMessage(Component.text("Current dimension: ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(coloredName, NamedTextColor.GOLD, TextDecoration.BOLD)));
        }

        // Show all tiers with progress
        player.sendMessage(Component.text("Dimensions:", NamedTextColor.LIGHT_PURPLE));
        for (LootTierManager.LootTier tier : lootTierManager.getTiers()) {
            boolean unlocked = mined >= tier.blocksRequired();
            NamedTextColor color = unlocked ? NamedTextColor.GREEN : NamedTextColor.GRAY;
            String prefix = unlocked ? "\u2714 " : "\u2718 ";
            String coloredName = org.bukkit.ChatColor.translateAlternateColorCodes('&', tier.name());
            player.sendMessage(Component.text("  " + prefix, color)
                    .append(Component.text(coloredName))
                    .append(Component.text(" (" + tier.blocksRequired() + " blocks)", NamedTextColor.DARK_GRAY)));
        }

        if (next != null) {
            long remaining = next.blocksRequired() - mined;
            String nextName = org.bukkit.ChatColor.translateAlternateColorCodes('&', next.name());
            player.sendMessage(Component.text("Next: ", NamedTextColor.YELLOW)
                    .append(Component.text(nextName))
                    .append(Component.text(" in " + remaining + " blocks", NamedTextColor.AQUA)));
        } else {
            player.sendMessage(Component.text("All dimensions unlocked!", NamedTextColor.GOLD, TextDecoration.BOLD));
        }

        player.sendMessage(Component.text("Status: ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(lootTierManager.isEnabled() ? "ACTIVE" : "INACTIVE",
                        lootTierManager.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)));

        return true;
    }

    private boolean handleBlockadex(CommandSender sender, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("toggle")) {
            if (!sender.hasPermission("chaos.admin")) { sender.sendMessage(Component.text("No permission.", NamedTextColor.RED)); return true; }
            blockadexManager.toggle();
            sender.sendMessage(Component.text("Blockadex " + (blockadexManager.isEnabled() ? "enabled" : "disabled") + "!", NamedTextColor.GREEN));
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("top")) {
            sender.sendMessage(Component.text("=== Blockadex Top Collectors ===", NamedTextColor.GOLD, TextDecoration.BOLD));
            Map<UUID, Integer> top = blockadexManager.getTopCollectors(5);
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : top.entrySet()) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) name = entry.getKey().toString().substring(0, 8);
                int pct = (int) ((entry.getValue() * 100L) / blockadexManager.getTotalItems());
                sender.sendMessage(Component.text("#" + rank + " ", NamedTextColor.GRAY)
                        .append(Component.text(name, NamedTextColor.YELLOW))
                        .append(Component.text(" - " + entry.getValue() + "/" + blockadexManager.getTotalItems() + " (" + pct + "%)", NamedTextColor.GOLD)));
                rank++;
            }
            return true;
        }

        // Default: show own progress
        if (!(sender instanceof Player player)) { sender.sendMessage("Must be a player."); return true; }
        UUID uuid = player.getUniqueId();
        int collected = blockadexManager.getCollectedCount(uuid);
        int total = blockadexManager.getTotalItems();
        int pct = blockadexManager.getPercentage(uuid);

        player.sendMessage(Component.text("=== Blockadex ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("Collected: ", NamedTextColor.YELLOW)
                .append(Component.text(collected + "/" + total, NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" (" + pct + "%)", NamedTextColor.AQUA)));

        player.sendMessage(Component.text("Milestones:", NamedTextColor.GOLD));
        for (BlockadexMilestone milestone : blockadexManager.getMilestones()) {
            boolean reached = pct >= milestone.getPercentage();
            NamedTextColor color = reached ? NamedTextColor.GREEN : NamedTextColor.GRAY;
            String prefix = reached ? "\u2714 " : "\u2718 ";
            player.sendMessage(Component.text("  " + prefix + milestone.getPercentage() + "% - " + milestone.getName(), color)
                    .append(Component.text(" (" + milestone.getDescription() + ")", NamedTextColor.DARK_GRAY)));
        }

        // Show next milestone
        for (BlockadexMilestone milestone : blockadexManager.getMilestones()) {
            if (pct < milestone.getPercentage()) {
                int needed = (int) Math.ceil((milestone.getPercentage() / 100.0) * total) - collected;
                player.sendMessage(Component.text("Next: ", NamedTextColor.YELLOW)
                        .append(Component.text(milestone.getName() + " at " + milestone.getPercentage() + "% (" + needed + " more items)", NamedTextColor.AQUA)));
                break;
            }
        }
        return true;
    }

    private boolean handleCoins(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(Component.text("Usage: /coins <give|remove|set> <player> <amount>", NamedTextColor.YELLOW)); return true; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED)); return true; }
        long amount;
        try { amount = Long.parseLong(args[2]); } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount!", NamedTextColor.RED)); return true;
        }
        switch (args[0].toLowerCase()) {
            case "give" -> {
                coinManager.addCoins(target.getUniqueId(), amount);
                sender.sendMessage(Component.text("Gave " + amount + " coins to " + target.getName(), NamedTextColor.GREEN));
                target.sendMessage(Component.text("You received " + amount + " coins!", NamedTextColor.GOLD));
            }
            case "remove" -> {
                if (coinManager.removeCoins(target.getUniqueId(), amount))
                    sender.sendMessage(Component.text("Removed " + amount + " coins from " + target.getName(), NamedTextColor.GREEN));
                else
                    sender.sendMessage(Component.text("Player doesn't have enough coins!", NamedTextColor.RED));
            }
            case "set" -> {
                coinManager.setCoins(target.getUniqueId(), amount);
                sender.sendMessage(Component.text("Set " + target.getName() + "'s coins to " + amount, NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Usage: /coins <give|remove|set> <player> <amount>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
    }
}
