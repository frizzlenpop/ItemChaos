package org.frizzlenpop.randomItem.economy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ThreadLocalRandom;

public final class CoinDropUtil {

    private static NamespacedKey COIN_KEY;

    private CoinDropUtil() {
    }

    public static void initKey(Plugin plugin) {
        COIN_KEY = new NamespacedKey(plugin, "coin_value");
    }

    public static NamespacedKey getKey() {
        return COIN_KEY;
    }

    public static void scatterCoins(Location center, long totalCoins, int count, int radius) {
        if (totalCoins <= 0 || count <= 0) return;
        long perNugget = Math.max(1, totalCoins / count);
        long remainder = totalCoins - (perNugget * count);

        for (int i = 0; i < count; i++) {
            long value = perNugget + (i == 0 ? remainder : 0);
            ItemStack nugget = createCoinItem(value);
            Location dropLoc = center.clone().add(
                    ThreadLocalRandom.current().nextDouble(-radius, radius),
                    0.5,
                    ThreadLocalRandom.current().nextDouble(-radius, radius));
            center.getWorld().dropItemNaturally(dropLoc, nugget);
        }
    }

    public static ItemStack createCoinItem(long value) {
        ItemStack nugget = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = nugget.getItemMeta();
        meta.displayName(Component.text(value + " Coins", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(COIN_KEY, PersistentDataType.LONG, value);
        nugget.setItemMeta(meta);
        return nugget;
    }

    public static long getCoinValue(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Long value = item.getItemMeta().getPersistentDataContainer().get(COIN_KEY, PersistentDataType.LONG);
        return value != null ? value : 0;
    }
}
