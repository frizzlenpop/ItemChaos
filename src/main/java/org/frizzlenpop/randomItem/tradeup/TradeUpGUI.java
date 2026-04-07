package org.frizzlenpop.randomItem.tradeup;

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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.frizzlenpop.randomItem.economy.CoinManager;
import org.frizzlenpop.randomItem.economy.ItemValueRegistry;

import java.util.*;

public class TradeUpGUI implements Listener {

    private static final Component GUI_TITLE = Component.text("Trade-Up", NamedTextColor.GOLD, TextDecoration.BOLD);
    private static final int[] INPUT_SLOTS = {11, 12, 13, 14, 15};
    private static final int CONFIRM_SLOT = 31;
    private static final int INFO_SLOT = 4;

    private final TradeUpConfig config;
    private final CoinManager coinManager;
    private final Set<UUID> openGUIs = new HashSet<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TradeUpGUI(TradeUpConfig config, CoinManager coinManager) {
        this.config = config;
        this.coinManager = coinManager;
    }

    public void openGUI(Player player) {
        if (!config.isEnabled()) {
            player.sendMessage(Component.text("Trade-Up is disabled!", NamedTextColor.RED));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);

        // Fill with glass
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, filler);
        }

        // Clear input slots (allow item placement)
        for (int slot : INPUT_SLOTS) {
            inv.setItem(slot, null);
        }

        // Info item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("Trade-Up System", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.text("Place " + config.getInputCount() + " items below", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        infoLore.add(Component.text("Click Confirm to trade up!", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        infoLore.add(Component.text("Get 1 item from a higher tier", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        if (config.getCost() > 0) {
            infoLore.add(Component.empty());
            infoLore.add(Component.text("Cost: " + config.getCost() + " coins", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
        }
        if (config.getCooldownSeconds() > 0) {
            infoLore.add(Component.text("Cooldown: " + config.getCooldownSeconds() + "s", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        infoMeta.lore(infoLore);
        info.setItemMeta(infoMeta);
        inv.setItem(INFO_SLOT, info);

        // Confirm button
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(Component.text("CONFIRM TRADE-UP", NamedTextColor.GREEN, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        confirm.setItemMeta(confirmMeta);
        inv.setItem(CONFIRM_SLOT, confirm);

        openGUIs.add(player.getUniqueId());
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!GUI_TITLE.equals(event.getView().title())) return;

        int slot = event.getRawSlot();

        // Allow clicks in input slots (top inventory)
        if (isInputSlot(slot)) return;

        // Allow clicks in player inventory (bottom) for picking up items
        if (slot >= 36) return;

        event.setCancelled(true);

        if (slot == CONFIRM_SLOT) {
            handleConfirm(player, event.getView().getTopInventory());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!GUI_TITLE.equals(event.getView().title())) return;

        // Only allow drags into input slots
        for (int dragSlot : event.getRawSlots()) {
            if (!isInputSlot(dragSlot) && dragSlot < 36) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!openGUIs.remove(player.getUniqueId())) return;
        if (!GUI_TITLE.equals(event.getView().title())) return;

        // Return items from input slots
        Inventory inv = event.getView().getTopInventory();
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                inv.setItem(slot, null);
            }
        }
    }

    private void handleConfirm(Player player, Inventory inv) {
        List<ItemStack> inputItems = new ArrayList<>();
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                inputItems.add(item);
            }
        }

        if (inputItems.size() < config.getInputCount()) {
            player.sendMessage(Component.text("Need " + config.getInputCount() + " items! You have " + inputItems.size(), NamedTextColor.RED));
            return;
        }

        // Cooldown check
        if (config.getCooldownSeconds() > 0) {
            long now = System.currentTimeMillis();
            Long lastUse = cooldowns.get(player.getUniqueId());
            if (lastUse != null && (now - lastUse) < config.getCooldownSeconds() * 1000L) {
                int remaining = (int) ((config.getCooldownSeconds() * 1000L - (now - lastUse)) / 1000) + 1;
                player.sendMessage(Component.text("Cooldown! Wait " + remaining + "s", NamedTextColor.RED));
                return;
            }
        }

        // Coin cost check
        if (config.getCost() > 0) {
            if (!coinManager.removeCoins(player.getUniqueId(), config.getCost())) {
                player.sendMessage(Component.text("Not enough coins! Need " + config.getCost(), NamedTextColor.RED));
                return;
            }
        }

        // Calculate average value
        int totalValue = 0;
        for (ItemStack item : inputItems) {
            totalValue += ItemValueRegistry.getValue(item.getType());
        }
        int avgValue = totalValue / inputItems.size();

        TradeUpConfig.Tier currentTier = config.getTierForValue(avgValue);
        TradeUpConfig.Tier nextTier = config.getNextTier(currentTier);

        if (nextTier == null) {
            player.sendMessage(Component.text("Already at the highest tier! Can't trade up further.", NamedTextColor.RED));
            return;
        }

        // Clear input slots
        for (int slot : INPUT_SLOTS) {
            inv.setItem(slot, null);
        }

        // Give result
        Material result = config.getRandomItemInTier(nextTier);
        ItemStack resultItem = new ItemStack(result);
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(resultItem);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        // Set cooldown
        if (config.getCooldownSeconds() > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }

        player.sendMessage(Component.text("Trade-Up! " + currentTier.display() + " -> " + nextTier.display() + ": " + result.name(), NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    private boolean isInputSlot(int slot) {
        for (int s : INPUT_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }
}
