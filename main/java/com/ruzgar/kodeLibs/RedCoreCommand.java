package com.ruzgar.kodeLibs;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Material;

public class RedCoreCommand implements CommandExecutor {

    private final KodeLibs plugin;

    public RedCoreCommand(KodeLibs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.sadece_oyuncu")));
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Bu komutu kullanmak için OP olmalısın!");
            return true;
        }

        if (args.length >= 3) {
            String anaKomut = args[0];
            String altKomut = args[1];
            String ozellik = args[2];

            if (anaKomut.equals("islev") && altKomut.equals("ekle")) {

                ItemStack item = player.getInventory().getItemInMainHand();

                if (item == null || item.getType().isAir()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.yeterli_esya")));
                    return true;
                }

                ItemMeta meta = item.getItemMeta();
                if (meta == null) {
                    player.sendMessage(ChatColor.RED + "Özellik eklenemiyor! (Hata Kodu: 1)");
                    return true;
                }

                if (ozellik.equals("kristallestirme")) {
                    NamespacedKey key = new NamespacedKey(plugin, "kristallestirme");
                    meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
                    item.setItemMeta(meta);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.kristallestirme_eklendi")));
                    return true;
                } else if (ozellik.equals("nekromansi")) {
                    NamespacedKey key = new NamespacedKey(plugin, "nekromansi");
                    meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
                    item.setItemMeta(meta);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.nekromansi_eklendi")));
                    return true;
                } else if (ozellik.equals("karanlik_yildiz")) {
                    NamespacedKey key = new NamespacedKey(plugin, "karanlik_yildiz");
                    meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
                    item.setItemMeta(meta);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.karanlik_yildiz_eklendi")));
                    return true;
                } else if (ozellik.equals("hasar_azaltma")) {

                    if (args.length < 4) {
                        player.sendMessage(ChatColor.RED + "Lütfen bir oran giriniz. Örnek: /redcore islev ekle hasar_azaltma 10");
                        return true;
                    }

                    int oran;
                    try {
                        oran = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Geçerli bir sayı giriniz.");
                        return true;
                    }

                    if (oran < 1 || oran > 100) {
                        player.sendMessage(ChatColor.RED + "Lütfen 1 ile 100 arasında bir değer giriniz.");
                        return true;
                    }

                    Material type = item.getType();
                    if (!(type == Material.LEATHER_HELMET || type == Material.CHAINMAIL_HELMET || type == Material.IRON_HELMET
                            || type == Material.GOLDEN_HELMET || type == Material.DIAMOND_HELMET || type == Material.NETHERITE_HELMET
                            || type == Material.LEATHER_CHESTPLATE || type == Material.CHAINMAIL_CHESTPLATE || type == Material.IRON_CHESTPLATE
                            || type == Material.GOLDEN_CHESTPLATE || type == Material.DIAMOND_CHESTPLATE || type == Material.NETHERITE_CHESTPLATE
                            || type == Material.LEATHER_LEGGINGS || type == Material.CHAINMAIL_LEGGINGS || type == Material.IRON_LEGGINGS
                            || type == Material.GOLDEN_LEGGINGS || type == Material.DIAMOND_LEGGINGS || type == Material.NETHERITE_LEGGINGS
                            || type == Material.LEATHER_BOOTS || type == Material.CHAINMAIL_BOOTS || type == Material.IRON_BOOTS
                            || type == Material.GOLDEN_BOOTS || type == Material.DIAMOND_BOOTS || type == Material.NETHERITE_BOOTS)) {
                        player.sendMessage(ChatColor.RED + "Hasar azaltma özelliği sadece zırh parçalarına eklenebilir!");
                        return true;
                    }

                    NamespacedKey key = new NamespacedKey(plugin, "hasar_azaltma");
                    meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, oran);
                    item.setItemMeta(meta);

                    String msg = plugin.getConfig().getString("messages.hasar_azaltma_eklendi");
                    if (msg != null) {
                        msg = msg.replace("%value%", String.valueOf(oran));
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    } else {
                        player.sendMessage(ChatColor.GREEN + "Hasar azaltma özelliği %" + oran + " olarak eklendi!");
                    }
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "Bilinmeyen özellik: " + ozellik);
                    return true;
                }
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            player.sendMessage(ChatColor.GREEN + "Config dosyası yenilendi!");
            return true;
        }

        player.sendMessage(ChatColor.RED + "Komut kullanımı hatalı!");
        return true;
    }
}
