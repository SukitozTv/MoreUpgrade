package me.flukky.Listeners;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.flukky.MoreUpgrade;

public class Upgrade implements Listener {
    private MoreUpgrade plugin;

    public Upgrade(MoreUpgrade plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onPrepareSmithing(PrepareSmithingEvent event) {
        ItemStack baseWeapon = event.getInventory().getItem(1);
        ItemStack upgradeItem = event.getInventory().getItem(2);
        Player player = (Player) event.getViewers().get(0);

        if (baseWeapon != null && upgradeItem != null) {
            String weaponName = baseWeapon.getType().toString().toLowerCase();
            ItemMeta meta = baseWeapon.getItemMeta();
            int currentLevel = 0;

            // ใช้ PersistentDataContainer แทน CustomModelData
            if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "level"), PersistentDataType.INTEGER)) {
                currentLevel = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "level"), PersistentDataType.INTEGER);
            }

            int nextLevel = currentLevel + 1;

            if (plugin.getUpgradeItems().containsKey(weaponName) && plugin.isValidUpgradeItem(upgradeItem, player, nextLevel)) {
                if (!plugin.getUpgradeItems().get(weaponName).contains(nextLevel)) {
                    event.setResult(null);
                    return; 
                }

                // ตรวจสอบการล้มเหลวของการอัปเกรด
                if (new Random().nextDouble() < plugin.getFailChance().get(nextLevel)) { // หากสุ่มได้ค่า <= 0.3 (30%)
                    player.sendMessage(ChatColor.RED + "Upgrade failed! Please try again.");
                    event.setResult(null); // ไม่มีผลลัพธ์
                    ItemStack materialItem = event.getInventory().getItem(2);
                    if (materialItem != null && materialItem.getAmount() > 0) {
                        materialItem.setAmount(materialItem.getAmount() - 1);
                        event.getInventory().setItem(2, materialItem.getAmount() == 0 ? null : materialItem);
                    }
                    
                    return; // ออกจากฟังก์ชัน
                }

                double additionalDamage = plugin.getDamage().get(nextLevel);
                double additionalSpeed = plugin.getSpeed().get(nextLevel);
                int additionalArmor = plugin.getArmor().get(nextLevel);
                int additionalToughness = plugin.getToughness().get(nextLevel);
                Integer customModelData = plugin.getModelData(weaponName, nextLevel);
                double additionalFail = plugin.getFailChance().get(nextLevel);
                String customName = plugin.getCurrentName(weaponName, nextLevel);

                ItemStack result = new ItemStack(Material.valueOf(weaponName.toUpperCase()));
                ItemMeta resultMeta = result.getItemMeta();
                

                if (resultMeta != null) {
                    List<String> lore = new ArrayList<>();
                    Material material = result.getType();
                
                    if (isArmor(material)) {
                        EquipmentSlot slot;
                        // ตรวจสอบว่าเป็น slot ไหน แล้วกำหนดเฉพาะ slot นั้น
                        if (material == Material.LEATHER_HELMET || material == Material.IRON_HELMET || material == Material.DIAMOND_HELMET || material == Material.NETHERITE_HELMET || material == Material.TURTLE_HELMET) {
                            slot = EquipmentSlot.HEAD;
                        } else if (material == Material.LEATHER_CHESTPLATE || material == Material.IRON_CHESTPLATE || material == Material.DIAMOND_CHESTPLATE || material == Material.NETHERITE_CHESTPLATE) {
                            slot = EquipmentSlot.CHEST;
                        } else if (material == Material.LEATHER_LEGGINGS || material == Material.IRON_LEGGINGS || material == Material.DIAMOND_LEGGINGS || material == Material.NETHERITE_LEGGINGS) {
                            slot = EquipmentSlot.LEGS;
                        } else if (material == Material.LEATHER_BOOTS || material == Material.IRON_BOOTS || material == Material.DIAMOND_BOOTS || material == Material.NETHERITE_BOOTS) {
                            slot = EquipmentSlot.FEET;
                        } else {
                            return; // หากไม่ตรงกับเงื่อนไขใดๆ
                        }
                    
                        resultMeta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(UUID.randomUUID(), "generic.armor", additionalArmor, AttributeModifier.Operation.ADD_NUMBER, slot));
                        resultMeta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(UUID.randomUUID(), "generic.armorToughness", additionalToughness, AttributeModifier.Operation.ADD_NUMBER, slot));
                    
                        lore.add("");
                        lore.add("§eArmor: +" + additionalArmor); // สีเหลือง
                        lore.add("§9Toughness: +" + additionalToughness); // สีน้ำเงิน
                    } else if (isWeapon(material)) {
                        resultMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(), "generic.attackDamage", additionalDamage, AttributeModifier.Operation.ADD_NUMBER));
                        resultMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(UUID.randomUUID(), "generic.attackSpeed", additionalSpeed, AttributeModifier.Operation.ADD_NUMBER));

                        lore.add("");
                        lore.add("§eDamage: +" + additionalDamage); // สีเหลือง
                        lore.add("§9Speed: +" + additionalSpeed); // สีน้ำเงิน
                        lore.add("§cมีโอกาศตีติดพิษ: " + additionalFail + "%"); // สีแดง
                    }
                
                    // ตั้งค่า PersistentDataContainer สำหรับ level และ custom model
                    resultMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "level"), PersistentDataType.INTEGER, nextLevel);
                    resultMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_model_data"), PersistentDataType.INTEGER, customModelData);
                
                    // ตั้งค่า CustomModelData โดยตรง
                    if (customModelData != null) {
                        resultMeta.setCustomModelData(customModelData);
                        resultMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_model_data"), PersistentDataType.INTEGER, customModelData);
                        player.sendMessage(ChatColor.GREEN + "customModelData: " + customModelData + "!");
                    }

                
                    // ตั้งชื่อไอเท็มและเพิ่ม lore
                    resultMeta.setDisplayName(ChatColor.DARK_PURPLE + customName + " +" + nextLevel);
                    lore.add("");
                    lore.add("§7This has been upgraded to level " + nextLevel + "!");
                    resultMeta.setLore(lore);
                
                    // เพิ่มความคงทน
                    resultMeta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                    result.setItemMeta(resultMeta);
                }
                event.setResult(result);
            } 
        } else {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onSmithingTableClick(InventoryClickEvent event) {
        if (event.getInventory().getType() == InventoryType.SMITHING) {
            if (event.getSlotType() == InventoryType.SlotType.RESULT) {
                ItemStack upgradedWeapon = event.getInventory().getItem(3);

                // ตรวจสอบว่า upgradedWeapon ไม่เป็น null
                if (upgradedWeapon != null && upgradedWeapon.getType() != Material.AIR) {
                    String weaponName = upgradedWeapon.getType().toString().toLowerCase();
                    ItemMeta meta = upgradedWeapon.getItemMeta();

                    if (meta != null) {
                        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "level"), PersistentDataType.INTEGER)) {
                            int level = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "level"), PersistentDataType.INTEGER);
                            String customName = plugin.getCurrentName(weaponName, level);
                            meta.setDisplayName(ChatColor.DARK_PURPLE + customName + " +" + level);
                            upgradedWeapon.setItemMeta(meta);
                        } else {
                            return;
                        }

                        event.getWhoClicked().getInventory().addItem(upgradedWeapon);
                        event.getInventory().setItem(1, null);

                        ItemStack materialItem = event.getInventory().getItem(2);
                        if (materialItem != null && materialItem.getAmount() > 0) {
                            materialItem.setAmount(materialItem.getAmount() - 1);
                            event.getInventory().setItem(2, materialItem.getAmount() == 0 ? null : materialItem);
                        }
                        
                        event.getInventory().setItem(3, null);
                        event.setCurrentItem(null);

                        String playerName = event.getWhoClicked().getName();
                        String upgradeMessage = ChatColor.GOLD + playerName + " ได้อัปเกรดอาวุธเป็น " + meta.getDisplayName() + "!";
                        Bukkit.broadcastMessage(upgradeMessage);
                    }
                } else {
                    // หาก upgradedWeapon เป็น null หรือ AIR ให้ส่งข้อความเตือน
                    event.getWhoClicked().sendMessage(ChatColor.RED + "ไม่มีอาวุธในช่องผลลัพธ์!");
                }
            }

            // บล็อกการวางวัสดุในช่อง 2 หากมีไอเท็มอยู่ในช่อง 3
            if (event.getSlot() == 2 && event.getInventory().getItem(3) != null) {
                event.setCancelled(true); // บล็อกการคลิกในช่อง 2
            }

            if (event.getSlot() == 2) {
                if (event.getViewers().size() > 0 && event.getViewers().get(0) instanceof Player) {
                    Player player = (Player) event.getViewers().get(0);
                    player.removeMetadata("UpgradeMessageDisplayed", plugin);
                }
            }
        }
    }

    private boolean isWeapon(Material material) {
        return material == Material.WOODEN_SWORD || material == Material.STONE_SWORD || material == Material.IRON_SWORD ||
            material == Material.DIAMOND_SWORD || material == Material.NETHERITE_SWORD || material == Material.WOODEN_AXE ||
            material == Material.STONE_AXE || material == Material.IRON_AXE || material == Material.DIAMOND_AXE ||
            material == Material.NETHERITE_AXE || material == Material.BOW || material == Material.CROSSBOW || 
            material == Material.TRIDENT;
    }

    private boolean isArmor(Material material) {
        return material == Material.LEATHER_HELMET || material == Material.LEATHER_CHESTPLATE || material == Material.LEATHER_LEGGINGS || material == Material.LEATHER_BOOTS ||
            material == Material.IRON_HELMET || material == Material.IRON_CHESTPLATE || material == Material.IRON_LEGGINGS || material == Material.IRON_BOOTS ||
            material == Material.DIAMOND_HELMET || material == Material.DIAMOND_CHESTPLATE || material == Material.DIAMOND_LEGGINGS || material == Material.DIAMOND_BOOTS ||
            material == Material.NETHERITE_HELMET || material == Material.NETHERITE_CHESTPLATE || material == Material.NETHERITE_LEGGINGS || material == Material.NETHERITE_BOOTS ||
            material == Material.CHAINMAIL_HELMET || material == Material.CHAINMAIL_CHESTPLATE || material == Material.CHAINMAIL_LEGGINGS || material == Material.CHAINMAIL_BOOTS ||
            material == Material.GOLDEN_HELMET || material == Material.GOLDEN_CHESTPLATE || material == Material.GOLDEN_LEGGINGS || material == Material.GOLDEN_BOOTS ||
            material == Material.TURTLE_HELMET || material == Material.SHIELD;
    }
}
