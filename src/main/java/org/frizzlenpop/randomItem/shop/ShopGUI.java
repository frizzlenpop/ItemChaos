package org.frizzlenpop.randomItem.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.frizzlenpop.randomItem.economy.CoinManager;

import java.util.*;

public class ShopGUI implements Listener {

    private static final Component GUI_TITLE = Component.text("Item Shop", NamedTextColor.DARK_GREEN, TextDecoration.BOLD);
    private static final int ITEMS_PER_PAGE = 45;

    private final ShopConfig shopConfig;
    private final CoinManager coinManager;
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public ShopGUI(ShopConfig shopConfig, CoinManager coinManager) {
        this.shopConfig = shopConfig;
        this.coinManager = coinManager;
    }

    public void openGUI(Player player, int page) {
        List<ShopItem> items = shopConfig.getItems();
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);
        long playerCoins = coinManager.getCoins(player.getUniqueId());

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, items.size());

        for (int i = start; i < end; i++) {
            ShopItem shopItem = items.get(i);
            inv.setItem(i - start, buildShopIcon(shopItem, playerCoins));
        }

        // Navigation bar (row 6)
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Page indicator
        ItemStack pageIndicator = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageIndicator.getItemMeta();
        pageMeta.displayName(Component.text("Page " + (page + 1) + "/" + totalPages, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        pageMeta.lore(List.of(Component.text("Your coins: " + playerCoins, NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)));
        pageIndicator.setItemMeta(pageMeta);
        inv.setItem(49, pageIndicator);

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(Component.text("Previous Page", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            prev.setItemMeta(prevMeta);
            inv.setItem(45, prev);
        }

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.displayName(Component.text("Next Page", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            next.setItemMeta(nextMeta);
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }

    private ItemStack buildShopIcon(ShopItem shopItem, long playerCoins) {
        ItemStack icon = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
        ItemMeta meta = icon.getItemMeta();

        if (shopItem.getDisplayName() != null) {
            meta.displayName(Component.text(shopItem.getDisplayName())
                    .decoration(TextDecoration.ITALIC, false));
        }

        NamedTextColor costColor = playerCoins >= shopItem.getCost() ? NamedTextColor.GREEN : NamedTextColor.RED;
        meta.lore(List.of(
                Component.text("Amount: " + shopItem.getAmount(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Cost: " + shopItem.getCost() + " coins", costColor)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click to purchase!", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        icon.setItemMeta(meta);
        return icon;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!GUI_TITLE.equals(event.getView().title())) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot > 53) return;

        // Navigation
        Integer currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
        if (slot == 45 && currentPage > 0) {
            openGUI(player, currentPage - 1);
            return;
        }
        if (slot == 53) {
            int totalPages = Math.max(1, (int) Math.ceil((double) shopConfig.getItems().size() / ITEMS_PER_PAGE));
            if (currentPage < totalPages - 1) {
                openGUI(player, currentPage + 1);
                return;
            }
        }

        // Shop item purchase
        if (slot >= 45) return;

        int itemIndex = currentPage * ITEMS_PER_PAGE + slot;
        List<ShopItem> items = shopConfig.getItems();
        if (itemIndex >= items.size()) return;

        ShopItem shopItem = items.get(itemIndex);
        if (!coinManager.removeCoins(player.getUniqueId(), shopItem.getCost())) {
            player.sendMessage(Component.text("Not enough coins! Need " + shopItem.getCost(), NamedTextColor.RED));
            return;
        }

        ItemStack purchased = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(purchased);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        player.sendMessage(Component.text("Purchased " + shopItem.getAmount() + "x " +
                shopItem.getMaterial().name() + "!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        openGUI(player, currentPage);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!GUI_TITLE.equals(event.getView().title())) return;
        event.setCancelled(true);
    }
}
