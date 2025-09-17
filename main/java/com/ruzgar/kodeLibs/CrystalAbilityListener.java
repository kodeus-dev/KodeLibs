package com.ruzgar.kodeLibs;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class CrystalAbilityListener implements Listener {

    private final KodeLibs plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> frozenUntil = new HashMap<>();

    public CrystalAbilityListener(KodeLibs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(plugin, "kristallestirme");
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;

        long currentTime = System.currentTimeMillis();
        long cooldown = plugin.getConfig().getInt("cooldowns.kristallestirme", 20) * 1000L;
        long lastUse = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (currentTime - lastUse < cooldown) {
            long secondsLeft = (cooldown - (currentTime - lastUse)) / 1000;
            String msg = plugin.getConfig().getString("messages.kristallestirme_cooldown", "&cKristalleştirme için {seconds} saniye beklemelisin.")
                    .replace("{seconds}", String.valueOf(secondsLeft));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            return;
        }

        cooldowns.put(player.getUniqueId(), currentTime);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.kristallestirme_aktif", "&aKristalleştirme yeteneği aktifleştirildi!")));

        // Partikül ve ses efekti
        player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation(), 60, 1, 1, 1, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1f);

        // 7 blok içindeki tüm canlılar (oyuncu hariç)
        List<Entity> nearby = player.getNearbyEntities(7, 7, 7);

        int freezeDurationTicks = 2 * 20; // 2 saniye donma
        int slowDurationSeconds = plugin.getConfig().getInt("effects.slow_duration_seconds", 7);
        int slowDurationTicks = slowDurationSeconds * 20;

        long freezeEndTime = System.currentTimeMillis() + (freezeDurationTicks * 50L);

        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity target)) continue;
            if (target.getUniqueId().equals(player.getUniqueId())) continue;

            frozenUntil.put(target.getUniqueId(), freezeEndTime);

            // Mor partiküller
            target.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.1);

            // Vurulabilsin diye no damage ticks sıfırla
            target.setNoDamageTicks(0);

            if (target instanceof Player targetPlayer) {
                targetPlayer.sendMessage(ChatColor.GRAY + "Donma etkisi uygulandı.");
            }
        }

        // 2 saniye sonra donma bittiğinde slow efekti vermek için task
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Entity entity : nearby) {
                if (!(entity instanceof LivingEntity target)) continue;
                if (target.getUniqueId().equals(player.getUniqueId())) continue;

                if (target instanceof Player targetPlayer) {
                    targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowDurationTicks, 1, false, false, true));
                    targetPlayer.sendMessage(ChatColor.GRAY + "Yavaşlatma efekti başladı.");
                }
            }
        }, freezeDurationTicks);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        if (frozenUntil.containsKey(uuid)) {
            long freezeEnd = frozenUntil.get(uuid);
            if (System.currentTimeMillis() < freezeEnd) {
                if (!event.getFrom().toVector().equals(event.getTo().toVector())) {
                    event.setTo(event.getFrom());
                }
            } else {
                frozenUntil.remove(uuid);
                event.getPlayer().sendMessage(ChatColor.GRAY + "Donma etkisi sona erdi.");
            }
        }
    }
}
