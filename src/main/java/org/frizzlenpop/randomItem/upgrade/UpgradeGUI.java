package org.frizzlenpop.randomItem.upgrade;

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
import org.frizzlenpop.randomItem.economy.CoinManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UpgradeGUI implements Listener {

    private static final Component GUI_TITLE = Component.text("Upgrades", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD);
    private static final int[] UPGRADE_SLOTS = {
            11, 12, 13, 14, 15,   // Row 2: first 5 upgrades
            20, 21, 22, 23, 24,   // Row 3: next 5 upgrades
            29, 30, 31, 32, 33    // Row 4: last 5 upgrades
    };

    private final UpgradeManager upgradeManager;
    private final CoinManager coinManager;

    public UpgradeGUI(UpgradeManager upgradeManager, CoinManager coinManager) {
        this.upgradeManager = upgradeManager;
        this.coinManager = coinManager;
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);
        UUID uuid = player.getUniqueId();
        long playerCoins = coinManager.getCoins(uuid);

        // Fill with glass panes
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Place upgrade icons
        UpgradeType[] types = UpgradeType.values();
        for (int i = 0; i < types.length; i++) {
            inv.setItem(UPGRADE_SLOTS[i], buildUpgradeItem(types[i], uuid, playerCoins));
        }

        player.openInventory(inv);
    }

    private ItemStack buildUpgradeItem(UpgradeType type, UUID uuid, long playerCoins) {
        int currentLevel = upgradeManager.getLevel(uuid, type);
        boolean maxed = currentLevel >= type.getMaxLevel();

        ItemStack item = new ItemStack(type.getIcon(), Math.max(1, currentLevel));
        ItemMeta meta = item.getItemMeta();

        NamedTextColor nameColor = maxed ? NamedTextColor.GOLD : NamedTextColor.GREEN;
        meta.displayName(Component.text(type.getDisplayName(), nameColor)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Level " + currentLevel + "/" + type.getMaxLevel(),
                        maxed ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (currentLevel > 0) {
            lore.add(Component.text("Current: " + type.getDescription(currentLevel - 1), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }

        if (maxed) {
            lore.add(Component.text("MAXED", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            int cost = type.getCost(currentLevel);
            lore.add(Component.text("Next: " + type.getDescription(currentLevel), NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            NamedTextColor costColor = playerCoins >= cost ? NamedTextColor.GREEN : NamedTextColor.RED;
            lore.add(Component.text("Cost: " + cost + " coins", costColor)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Your coins: " + playerCoins, NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!GUI_TITLE.equals(event.getView().title())) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        UpgradeType type = getUpgradeForSlot(slot);
        if (type == null) return;

        upgradeManager.tryPurchase(player, type);
        openGUI(player);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!GUI_TITLE.equals(event.getView().title())) return;
        event.setCancelled(true);
    }

    private UpgradeType getUpgradeForSlot(int slot) {
        UpgradeType[] types = UpgradeType.values();
        for (int i = 0; i < UPGRADE_SLOTS.length; i++) {
            if (UPGRADE_SLOTS[i] == slot) return types[i];
        }
        return null;
    }
}
