package com.ruzgar.kodeLibs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class NecromancyAbilityListener implements Listener {

    private final KodeLibs plugin;

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, List<Zombie>> activeZombies = new HashMap<>();
    private final Map<Zombie, BukkitRunnable> particleTasks = new HashMap<>();

    public NecromancyAbilityListener(KodeLibs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!player.isSneaking()) return;
        if (!event.getAction().isLeftClick()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(plugin, "nekromansi");
        Byte val = meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        if (val == null || val != 1) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        int cooldownTime = plugin.getConfig().getInt("cooldowns.nekromansi", 120) * 1000;

        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < cooldownTime) {
            long left = (cooldownTime - (now - cooldowns.get(uuid))) / 1000;
            String cooldownMsg = plugin.getConfig().getString("messages.nekromansi_cooldown", "&cNekromansi için {seconds} saniye beklemelisin.");
            cooldownMsg = cooldownMsg.replace("{seconds}", String.valueOf(left));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', cooldownMsg));
            return;
        }

        cooldowns.put(uuid, now);
        player.sendMessage(ChatColor.GREEN + "Nekromansi aktif edildi!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SKELETON_AMBIENT, 1f, 1f);

        activeZombies.put(uuid, new ArrayList<>());

        BukkitRunnable summonTask = new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= 5) {
                    cancel();
                    List<Zombie> zombies = activeZombies.get(uuid);
                    if (zombies != null) {
                        for (Zombie zom : zombies) {
                            if (!zom.isDead()) {
                                BukkitRunnable partTask = particleTasks.remove(zom);
                                if (partTask != null) partTask.cancel();

                                zom.remove();
                            }
                        }
                    }
                    activeZombies.remove(uuid);
                    return;
                }

                spawnBabyZombie(player);
                count++;
            }
        };

        summonTask.runTaskTimer(plugin, 0L, 20L * 5);
    }

    private void spawnBabyZombie(Player player) {
        Location playerLoc = player.getLocation();

        double offsetX = (Math.random() * 2) - 1;
        double offsetZ = (Math.random() * 2) - 1;

        Location spawnLoc = playerLoc.clone().add(offsetX, 0, offsetZ);
        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);

        Zombie zombie = (Zombie) player.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
        zombie.setBaby(true);
        zombie.setCustomName(ChatColor.translateAlternateColorCodes('&', "&4Yaşayan Ölü"));
        zombie.setCustomNameVisible(true);
        zombie.setPersistent(false);

        zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.30);

        UUID ownerUUID = player.getUniqueId();

        activeZombies.computeIfAbsent(ownerUUID, k -> new ArrayList<>()).add(zombie);

        zombie.setTarget(null);

        BukkitRunnable particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (zombie.isDead()) {
                    particleTasks.remove(zombie);
                    cancel();
                    return;
                }

                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, 1f);
                zombie.getWorld().spawnParticle(Particle.REDSTONE, zombie.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0, dustOptions);

                Player owner = plugin.getServer().getPlayer(ownerUUID);
                if (owner == null || !owner.isOnline()) return;

                if (zombie.getTarget() == null) {
                    Location zombieLoc = zombie.getLocation();
                    Location ownerLoc = owner.getLocation();

                    if (zombieLoc.distance(ownerLoc) > 7) {
                        zombie.teleport(ownerLoc.clone().add(
                                (Math.random() * 4) - 2,
                                0,
                                (Math.random() * 4) - 2
                        ));
                    } else {
                        if (Math.random() < 0.1) {
                            Vector randomDir = new Vector(
                                    (Math.random() * 2) - 1,
                                    0,
                                    (Math.random() * 2) - 1
                            ).normalize().multiply(0.3);
                            zombie.setVelocity(randomDir);
                        }
                    }
                }
            }
        };

        particleTask.runTaskTimer(plugin, 0L, 5L);
        particleTasks.put(zombie, particleTask);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;

        boolean isOurZombie = false;
        UUID ownerUUID = null;
        for (Map.Entry<UUID, List<Zombie>> entry : activeZombies.entrySet()) {
            if (entry.getValue().contains(zombie)) {
                isOurZombie = true;
                ownerUUID = entry.getKey();
                break;
            }
        }
        if (!isOurZombie) return;

        Entity target = event.getTarget();
        Player owner = plugin.getServer().getPlayer(ownerUUID);

        if (target == null) {
            event.setCancelled(true);
            zombie.setTarget(null);
            return;
        }

        // Sahibini hedef almasını iptal et
        if (owner != null && target.equals(owner)) {
            event.setCancelled(true);
            zombie.setTarget(null);
            return;
        }

        event.setCancelled(false);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Zombie damager)) return;
        if (!(event.getEntity() instanceof LivingEntity damaged)) return;

        UUID damagerOwner = null;
        UUID damagedOwner = null;

        for (Map.Entry<UUID, List<Zombie>> entry : activeZombies.entrySet()) {
            if (entry.getValue().contains(damager)) {
                damagerOwner = entry.getKey();
                break;
            }
        }

        if (damagerOwner == null) return; // Bizim zombi değilse çık

        if (damaged instanceof Player) {
            damagedOwner = damaged.getUniqueId();

            // Eğer hasar yiyen oyuncu zombi sahibiyse iptal et
            if (damagedOwner.equals(damagerOwner)) {
                event.setCancelled(true);
                return;
            }
        } else if (damaged instanceof Zombie) {
            for (Map.Entry<UUID, List<Zombie>> entry : activeZombies.entrySet()) {
                if (entry.getValue().contains(damaged)) {
                    damagedOwner = entry.getKey();
                    break;
                }
            }

            if (damagerOwner.equals(damagedOwner)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        List<Zombie> zombies = activeZombies.remove(uuid);
        if (zombies != null) {
            for (Zombie zom : zombies) {
                if (!zom.isDead()) {
                    BukkitRunnable partTask = particleTasks.remove(zom);
                    if (partTask != null) partTask.cancel();

                    zom.remove();
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;

        for (Map.Entry<UUID, List<Zombie>> entry : activeZombies.entrySet()) {
            if (entry.getValue().remove(zombie)) {
                BukkitRunnable partTask = particleTasks.remove(zombie);
                if (partTask != null) partTask.cancel();
                break;
            }
        }
    }
}
