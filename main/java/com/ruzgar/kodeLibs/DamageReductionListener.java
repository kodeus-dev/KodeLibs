package com.ruzgar.kodeLibs;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

public class DamageReductionListener implements Listener {

    private final KodeLibs plugin;

    public DamageReductionListener(KodeLibs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        PlayerInventory inv = player.getInventory();
        ItemStack[] armorContents = inv.getArmorContents();

        int totalReductionPercent = 0;
        NamespacedKey key = new NamespacedKey(plugin, "hasar_azaltma");

        for (ItemStack armor : armorContents) {
            if (armor == null || armor.getType().isAir()) continue;

            ItemMeta meta = armor.getItemMeta();
            if (meta == null) continue;

            if (meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
                int oran = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
                if (oran > 0) {
                    totalReductionPercent += oran;
                }
            }
        }

        if (totalReductionPercent > 100) totalReductionPercent = 100;

        if (totalReductionPercent > 0) {
            double damage = event.getDamage();
            double reduceAmount = damage * (totalReductionPercent / 100.0);
            double finalDamage = damage - reduceAmount;

            event.setDamage(finalDamage);
        }
    }
}
