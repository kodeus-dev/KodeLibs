package com.ruzgar.kodeLibs;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DarkStarListener implements Listener {

    private final KodeLibs plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> inAir = new HashSet<>();

    public DarkStarListener(KodeLibs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onShiftLeftClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!player.isSneaking() || !event.getAction().isLeftClick()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(plugin, "karanlik_yildiz");
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        int cooldownSeconds = plugin.getConfig().getInt("cooldowns.karanlik_yildiz", 60);

        if (cooldowns.containsKey(uuid)) {
            long lastUsed = cooldowns.get(uuid);
            long timePassed = (now - lastUsed) / 1000;
            if (timePassed < cooldownSeconds) {
                long left = cooldownSeconds - timePassed;
                String msg = plugin.getConfig().getString("messages.karanlik_yildiz_cooldown", "&cKaranlık Yıldız için {seconds} saniye beklemelisin.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg.replace("{seconds}", String.valueOf(left))));
                return;
            }
        }

        cooldowns.put(uuid, now);
        inAir.add(uuid);

        // Havaya fırlatma
        player.setVelocity(player.getVelocity().setY(1.2));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 0.6f);

        // Yukarı çıkarken partikül
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ > 20 || !player.isOnline()) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation(), 10, 0.3, 0.3, 0.3, 0);
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @EventHandler
    public void onPlayerLand(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!inAir.contains(uuid)) return;

        if (player.isOnGround()) {
            inAir.remove(uuid);

            Location loc = player.getLocation();
            World world = player.getWorld();

            // Ses ve efektler
            world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1f);
            world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0);
            world.spawnParticle(Particle.SQUID_INK, loc, 50, 2, 1, 2, 0.2);

            // 10 blok içindeki tüm canlılara etki (yet. kullanıcısı hariç)
            for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
                if (!(entity instanceof LivingEntity target)) continue;
                if (entity.getUniqueId().equals(uuid)) continue;

                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 1));
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 5, 0));
                target.getWorld().spawnParticle(Particle.SMOKE_LARGE, target.getLocation(), 20, 0.5, 1, 0.5, 0.01);
            }

            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.karanlik_yildiz_kullanildi", "&5Karanlık yıldız enerjisini saldın!")));
        }
    }
}
