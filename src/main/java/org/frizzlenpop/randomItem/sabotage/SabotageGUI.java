package org.frizzlenpop.randomItem.sabotage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.frizzlenpop.randomItem.economy.CoinManager;
import org.frizzlenpop.randomItem.teams.Team;
import org.frizzlenpop.randomItem.teams.TeamManager;

import java.util.*;

public class SabotageGUI implements Listener {

    private static final Component TITLE_TARGET =
            Component.text("Sabotage - Pick Target", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD);
    private static final Component TITLE_TYPE =
            Component.text("Sabotage - Pick Type", NamedTextColor.RED, TextDecoration.BOLD);
    private static final Material[] TEAM_BANNERS = {
            Material.RED_BANNER, Material.BLUE_BANNER, Material.GREEN_BANNER, Material.YELLOW_BANNER
    };
    // 18 sabotage types across rows 2-4 of a 54-slot chest
    private static final int[] TYPE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,  // Row 2
            19, 20, 21, 22, 23, 24, 25,  // Row 3
            28, 29, 30, 31               // Row 4
    };

    private final SabotageManager sabotageManager;
    private final CoinManager coinManager;
    private final TeamManager teamManager;
    private final SabotageConfig sabotageConfig;

    // Target selection state: UUID of attacker -> target (UUID for player, String for team name)
    private final Map<UUID, Object> selectedTargets = new HashMap<>();
    // Map player slots to target UUIDs for the target GUI
    private final Map<UUID, Map<Integer, UUID>> playerSlotMap = new HashMap<>();
    // Map team slots to team names
    private final Map<UUID, Map<Integer, String>> teamSlotMap = new HashMap<>();

    public SabotageGUI(SabotageManager sabotageManager, CoinManager coinManager, TeamManager teamManager, SabotageConfig sabotageConfig) {
        this.sabotageManager = sabotageManager;
        this.coinManager = coinManager;
        this.teamManager = teamManager;
        this.sabotageConfig = sabotageConfig;
    }

    public void openTargetGUI(Player attacker) {
        if (!sabotageConfig.isEnabled()) {
            attacker.sendMessage(Component.text("Sabotage is currently disabled!", NamedTextColor.RED));
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_TARGET);

        // Fill with glass
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Player heads (rows 1-4, slots 0-35)
        Map<Integer, UUID> playerSlots = new HashMap<>();
        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(attacker)) continue;
            if (slot >= 36) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            meta.displayName(Component.text(online.getName(), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Click to sabotage this player", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            head.setItemMeta(meta);
            inv.setItem(slot, head);
            playerSlots.put(slot, online.getUniqueId());
            slot++;
        }
        playerSlotMap.put(attacker.getUniqueId(), playerSlots);

        // Team banners (row 5, slots 36-44)
        Map<Integer, String> teamSlots = new HashMap<>();
        int teamSlot = 36;
        int bannerIndex = 0;
        for (Team team : teamManager.getAllTeams()) {
            if (teamSlot >= 45) break;

            Material bannerMat = TEAM_BANNERS[bannerIndex % TEAM_BANNERS.length];
            ItemStack banner = new ItemStack(bannerMat);
            ItemMeta meta = banner.getItemMeta();
            meta.displayName(Component.text("Team: " + team.getName(), NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(team.getMembers().size() + " members", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Click to sabotage entire team", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("(Cost multiplied by online members)", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            banner.setItemMeta(meta);

            inv.setItem(teamSlot, banner);
            teamSlots.put(teamSlot, team.getName());
            teamSlot++;
            bannerIndex++;
        }
        teamSlotMap.put(attacker.getUniqueId(), teamSlots);

        attacker.openInventory(inv);
    }

    private void openTypeGUI(Player attacker) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_TYPE);
        long playerCoins = coinManager.getCoins(attacker.getUniqueId());

        // Fill with glass
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Determine cost multiplier for team targets
        Object target = selectedTargets.get(attacker.getUniqueId());
        int costMultiplier = 1;
        if (target instanceof String teamName) {
            List<Player> onlineMembers = teamManager.getOnlineTeamMembers(teamName);
            costMultiplier = Math.max(1, onlineMembers.size());
        }

        SabotageType[] types = SabotageType.values();
        int slotIndex = 0;
        for (SabotageType type : types) {
            if (slotIndex >= TYPE_SLOTS.length) break;
            if (!sabotageConfig.isTypeEnabled(type)) continue;

            int unitCost = sabotageConfig.getCost(type);
            long totalCost = (long) unitCost * costMultiplier;
            int duration = sabotageConfig.getDuration(type);

            ItemStack icon = new ItemStack(type.getIcon());
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(Component.text(type.getDisplayName(), NamedTextColor.RED, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(type.getDescription(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());

            if (duration > 0) {
                lore.add(Component.text("Duration: " + duration + "s", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("Duration: Instant", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }

            NamedTextColor costColor = playerCoins >= totalCost ? NamedTextColor.GREEN : NamedTextColor.RED;
            String costText = costMultiplier > 1
                    ? "Cost: " + totalCost + " coins (" + unitCost + " x " + costMultiplier + ")"
                    : "Cost: " + totalCost + " coins";
            lore.add(Component.text(costText, costColor)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Your coins: " + playerCoins, NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            icon.setItemMeta(meta);
            inv.setItem(TYPE_SLOTS[slotIndex], icon);
            slotIndex++;
        }

        attacker.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();

        if (TITLE_TARGET.equals(title)) {
            event.setCancelled(true);
            handleTargetClick(player, event.getRawSlot());
        } else if (TITLE_TYPE.equals(title)) {
            event.setCancelled(true);
            handleTypeClick(player, event.getRawSlot());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Component title = event.getView().title();
        if (TITLE_TARGET.equals(title) || TITLE_TYPE.equals(title)) {
            event.setCancelled(true);
        }
    }

    private void handleTargetClick(Player attacker, int slot) {
        UUID attackerUUID = attacker.getUniqueId();

        // Check player slots
        Map<Integer, UUID> playerSlots = playerSlotMap.get(attackerUUID);
        if (playerSlots != null && playerSlots.containsKey(slot)) {
            UUID targetUUID = playerSlots.get(slot);
            Player target = Bukkit.getPlayer(targetUUID);
            if (target == null || !target.isOnline()) {
                attacker.sendMessage(Component.text("That player is no longer online!", NamedTextColor.RED));
                return;
            }
            selectedTargets.put(attackerUUID, targetUUID);
            openTypeGUI(attacker);
            return;
        }

        // Check team slots
        Map<Integer, String> teamSlots = teamSlotMap.get(attackerUUID);
        if (teamSlots != null && teamSlots.containsKey(slot)) {
            String teamName = teamSlots.get(slot);
            selectedTargets.put(attackerUUID, teamName);
            openTypeGUI(attacker);
        }
    }

    private void handleTypeClick(Player attacker, int slot) {
        SabotageType type = getTypeForSlot(slot);
        if (type == null) return;

        Object target = selectedTargets.remove(attacker.getUniqueId());
        if (target == null) return;

        attacker.closeInventory();

        if (target instanceof UUID targetUUID) {
            Player victim = Bukkit.getPlayer(targetUUID);
            if (victim == null || !victim.isOnline()) {
                attacker.sendMessage(Component.text("Target is no longer online!", NamedTextColor.RED));
                return;
            }
            sabotageManager.executeSabotage(attacker, victim, type);
        } else if (target instanceof String teamName) {
            sabotageManager.executeSabotageOnTeam(attacker, teamName, type);
        }

        // Clean up slot maps
        playerSlotMap.remove(attacker.getUniqueId());
        teamSlotMap.remove(attacker.getUniqueId());
    }

    private SabotageType getTypeForSlot(int slot) {
        int slotIndex = 0;
        for (SabotageType type : SabotageType.values()) {
            if (slotIndex >= TYPE_SLOTS.length) break;
            if (!sabotageConfig.isTypeEnabled(type)) continue;
            if (TYPE_SLOTS[slotIndex] == slot) return type;
            slotIndex++;
        }
        return null;
    }
}
