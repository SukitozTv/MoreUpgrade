package me.flukky.Listeners;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.LivingEntity; // Import LivingEntity

import me.flukky.MoreUpgrade;

public class Events implements Listener {

    private MoreUpgrade plugin;
    
    public Events(MoreUpgrade plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity entity = event.getEntity();

        if (damager instanceof Player && entity instanceof LivingEntity) {
            Player player = (Player) damager;
            LivingEntity target = (LivingEntity) entity;
            ItemStack weapon = player.getInventory().getItemInMainHand();

            if (weapon != null && weapon.hasItemMeta()) {
                ItemMeta meta = weapon.getItemMeta();
                if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "level"), PersistentDataType.INTEGER)) {
                    int level = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "level"), PersistentDataType.INTEGER);
                    double chanceStatus = plugin.getStatusChance().get(level); // ดึงโอกาสติดพิษจาก config

                    if (new Random().nextDouble() < chanceStatus) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1)); // ติดพิษ 5 วินาที
                        //player.sendMessage(ChatColor.GREEN + "You poisoned " + target.getType() + "!");
                    }
                }
            }
        }

        // ถ้าเป้าหมายเป็นผู้เล่นและถูกโจมตี ให้เช็คการสะท้อนพิษ
        if (entity instanceof Player && damager instanceof LivingEntity) {
            Player player = (Player) entity;
            LivingEntity attacker = (LivingEntity) damager;

            // ตรวจสอบเกราะที่ผู้เล่นสวมใส่
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor != null && armor.hasItemMeta()) {
                    ItemMeta armorMeta = armor.getItemMeta();
                    if (armorMeta != null && armorMeta.getPersistentDataContainer().has(new NamespacedKey(plugin, "level"), PersistentDataType.INTEGER)) {
                        int level = armorMeta.getPersistentDataContainer().get(new NamespacedKey(plugin, "level"), PersistentDataType.INTEGER);
                        double reflectPoisonChance = plugin.getStatusChance().get(level); // ดึงโอกาสติดพิษจาก config

                        // โอกาสสะท้อนพิษ
                        if (new Random().nextDouble() < reflectPoisonChance) {
                            attacker.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1)); // สะท้อนพิษกลับ
                            //player.sendMessage(ChatColor.GREEN + "You reflected poison to " + attacker.getType().toString().toLowerCase() + "!");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() == InventoryType.SMITHING) {
            if (event.getViewers().size() > 0 && event.getViewers().get(0) instanceof Player) {
                Player player = (Player) event.getViewers().get(0);
                player.removeMetadata("UpgradeMessageDisplayed", plugin);
            }
        }
    }
}
