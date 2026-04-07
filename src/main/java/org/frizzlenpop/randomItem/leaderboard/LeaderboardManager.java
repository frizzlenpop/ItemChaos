package org.frizzlenpop.randomItem.leaderboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.frizzlenpop.randomItem.RandomItem;
import org.frizzlenpop.randomItem.economy.CoinManager;
import org.frizzlenpop.randomItem.teams.Team;
import org.frizzlenpop.randomItem.teams.TeamManager;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class LeaderboardManager implements Listener {

    private final RandomItem plugin;
    private final CoinManager coinManager;
    private final TeamManager teamManager;
    private boolean enabled;
    private int maxEntries;
    private boolean showTeamTotals;
    private String title;
    private BukkitTask updateTask;
    private Scoreboard scoreboard;

    public LeaderboardManager(RandomItem plugin, CoinManager coinManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.coinManager = coinManager;
        this.teamManager = teamManager;

        plugin.saveResource("leaderboard.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "leaderboard.yml"));

        this.enabled = config.getBoolean("enabled", true);
        this.maxEntries = config.getInt("max-entries", 10);
        int updateInterval = config.getInt("update-interval-ticks", 100);
        this.showTeamTotals = config.getBoolean("show-team-totals", true);
        this.title = ChatColor.translateAlternateColorCodes('&',
                config.getString("title", "&6&lCoin Leaderboard"));

        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        if (enabled) {
            startUpdating(updateInterval);
        }
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) {
            startUpdating(100);
        } else {
            stopUpdating();
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void reset() {
        refreshScoreboard();
    }

    public void showWinners(Player player) {
        List<Map.Entry<UUID, Long>> sorted = coinManager.getAllCoins().entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(maxEntries)
                .collect(Collectors.toList());

        player.sendMessage(Component.text("=== Final Standings ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        int rank = 1;
        for (Map.Entry<UUID, Long> entry : sorted) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = entry.getKey().toString().substring(0, 8);
            NamedTextColor color = rank <= 3 ? NamedTextColor.YELLOW : NamedTextColor.WHITE;
            player.sendMessage(Component.text("#" + rank + " ", NamedTextColor.GRAY)
                    .append(Component.text(name, color))
                    .append(Component.text(" - " + entry.getValue() + " coins", NamedTextColor.GOLD)));
            rank++;
        }

        if (showTeamTotals && !teamManager.getAllTeams().isEmpty()) {
            player.sendMessage(Component.text("=== Team Totals ===", NamedTextColor.AQUA, TextDecoration.BOLD));
            for (Team team : teamManager.getAllTeams()) {
                long total = 0;
                for (UUID uuid : team.getMembers()) {
                    total += coinManager.getCoins(uuid);
                }
                player.sendMessage(Component.text(team.getName() + ": ", NamedTextColor.AQUA)
                        .append(Component.text(total + " coins", NamedTextColor.GOLD)));
            }
        }
    }

    private void startUpdating(int interval) {
        if (updateTask != null) updateTask.cancel();
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshScoreboard, 0L, interval);
    }

    private void stopUpdating() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void refreshScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = scoreboard.registerNewObjective("coins", Criteria.DUMMY,
                Component.text(title));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<Map.Entry<UUID, Long>> sorted = coinManager.getAllCoins().entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(maxEntries)
                .collect(Collectors.toList());

        int score = sorted.size();
        for (Map.Entry<UUID, Long> entry : sorted) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = entry.getKey().toString().substring(0, 8);
            String display = ChatColor.YELLOW + name + ChatColor.GRAY + ": " +
                    ChatColor.GOLD + entry.getValue();
            if (display.length() > 40) display = display.substring(0, 40);
            obj.getScore(display).setScore(score--);
        }

        if (showTeamTotals && !teamManager.getAllTeams().isEmpty()) {
            obj.getScore(ChatColor.AQUA + "--- Teams ---").setScore(score--);
            for (Team team : teamManager.getAllTeams()) {
                long total = 0;
                for (UUID uuid : team.getMembers()) {
                    total += coinManager.getCoins(uuid);
                }
                String display = ChatColor.AQUA + team.getName() + ChatColor.GRAY + ": " +
                        ChatColor.GOLD + total;
                if (display.length() > 40) display = display.substring(0, 40);
                obj.getScore(display).setScore(score--);
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(scoreboard);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (enabled) {
            event.getPlayer().setScoreboard(scoreboard);
        }
    }
}
