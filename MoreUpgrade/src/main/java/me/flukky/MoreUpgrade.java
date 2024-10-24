package me.flukky;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import me.flukky.Commands.Give;
import me.flukky.Listeners.Events;
import me.flukky.Listeners.Upgrade;

import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoreUpgrade extends JavaPlugin implements Listener {

    private Map<String, List<Integer>> upgradeItemMap = new HashMap<>();
    private Map<Integer, Double> additionalDamageMap = new HashMap<>();
    private Map<Integer, Double> additionalSpeedMap = new HashMap<>();

    private Map<Integer, Integer> additionalArmorMap = new HashMap<>();
    private Map<Integer, Integer> additionalToughnessMap = new HashMap<>();

    private Map<Integer, Integer> additionalModelDataMap = new HashMap<>();
    private Map<Integer, Double> additionalChance = new HashMap<>();
    private Map<Integer, Double> additionalChanceStatus = new HashMap<>();

    private FileConfiguration levelsConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLevelsConfig();
        loadUpgradeConfig();
        registerRecipes();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new Upgrade(this), this);
        getServer().getPluginManager().registerEvents(new Events(this), this);

        this.getCommand("mu").setExecutor(new Give(this));
    }

    @Override
    public void onDisable() {
        saveDefaultConfig();
    }

    public FileConfiguration getLevel() {
        return levelsConfig;
    }

    public Map<String, List<Integer>> getUpgradeItems() {
        return upgradeItemMap;
    } 

    public Map<Integer, Double> getDamage() {
        return additionalDamageMap;
    } 

    public Map<Integer, Double> getSpeed() {
        return additionalSpeedMap;
    } 

    public Map<Integer, Integer> getArmor() {
        return additionalArmorMap;
    } 

    public Map<Integer, Integer> getToughness() {
        return additionalToughnessMap;
    } 

    public Map<Integer, Double> getFailChance() {
        return additionalChance;
    } 

    public Map<Integer, Double> getStatusChance() {
        return additionalChanceStatus;
    }

    public Integer getModelData(String weaponName, int level) {
        File upgradeFile = new File(getDataFolder(), "upgrades/" + weaponName + ".yml");
        if (!upgradeFile.exists()) {
            getLogger().warning("Upgrade file for weapon " + weaponName + " not found!");
            return null; // คืนค่า null หากไม่พบไฟล์
        }
    
        YamlConfiguration config = YamlConfiguration.loadConfiguration(upgradeFile);
    
        if (config.contains("customModelData")) {
            return config.getInt("customModelData." + level, -1); // คืนค่า -1 หากไม่พบค่า custom model data
        }
    
        return null; // คืนค่า null หากไม่พบข้อมูล
    }

    public String getCurrentName(String weaponName, int level) {
        File upgradeFile = new File(getDataFolder(), "upgrades/" + weaponName + ".yml");
        if (!upgradeFile.exists()) {
            getLogger().warning("Upgrade file for weapon " + weaponName + " not found!");
            return null; // คืนค่า null หากไม่พบไฟล์
        }
    
        YamlConfiguration config = YamlConfiguration.loadConfiguration(upgradeFile);
    
        if (config.contains("name")) {
            return config.getString("name"); // คืนค่าชื่อโดยตรง
        }
    
        return null; // คืนค่า null หากไม่พบข้อมูล
    }

    private void loadUpgradeConfig() {
        File upgradeFolder = new File(getDataFolder(), "upgrades"); // สร้างพาธไปยังโฟลเดอร์ Upgrades
    
        if (!upgradeFolder.exists() || !upgradeFolder.isDirectory()) {
            getLogger().severe("Upgrades folder is missing or not a directory!");
            return; // หากโฟลเดอร์ไม่พบ หรือไม่ใช่โฟลเดอร์ ให้หยุดทำงาน
        }
    
        File[] files = upgradeFolder.listFiles((dir, name) -> name.endsWith(".yml")); 
        if (files == null || files.length == 0) {
            getLogger().severe("No upgrade files found in the Upgrades folder!");
            return;
        }
    
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file); // โหลด config จากแต่ละไฟล์ .yml
    
                String key = file.getName().replace(".yml", ""); // ใช้ชื่อไฟล์เป็น key (ลบส่วน .yml ออก)
                List<Integer> upgrades = config.getIntegerList("upgrades");
                upgradeItemMap.put(key, upgrades);
    
                String itemName = config.getString("name");
                int maxLevel = config.getInt("max_level");
                getLogger().info("Loaded upgrade for: " + itemName + " (Max Level: " + maxLevel + ")");
    
                ConfigurationSection customModelDataSection = config.getConfigurationSection("customModelData");
                if (customModelDataSection != null) {
                    for (String modelDataKey : customModelDataSection.getKeys(false)) {
                        int level = Integer.parseInt(modelDataKey);
                        int customModelDataValue = customModelDataSection.getInt(modelDataKey);
                        additionalModelDataMap.put(level, customModelDataValue);
                    }
                }
            } catch (Exception e) {
                getLogger().severe("Error loading upgrade file: " + file.getName());
                e.printStackTrace();
            }
        }
    }    

    private void loadLevelsConfig() {
        File levelsFile = new File(getDataFolder(), "levels.yml");
        if (!levelsFile.exists()) {
            saveResource("levels.yml", false);
        }
        levelsConfig = YamlConfiguration.loadConfiguration(levelsFile);

        for (String levelKey : levelsConfig.getKeys(false)) {
            int level = Integer.parseInt(levelKey);
            additionalDamageMap.put(level, levelsConfig.getDouble(levelKey + ".additional_damage"));
            additionalSpeedMap.put(level, levelsConfig.getDouble(levelKey + ".additional_speed"));
            
            additionalArmorMap.put(level, levelsConfig.getInt(levelKey + ".additional_armor"));
            additionalToughnessMap.put(level, levelsConfig.getInt(levelKey + ".additional_toughness"));
            
            additionalChance.put(level, levelsConfig.getDouble(levelKey + ".fail_chance"));
            additionalChanceStatus.put(level, levelsConfig.getDouble(levelKey + ".chance_status"));

            
        }
    }
   
    public boolean isValidUpgradeItem(ItemStack item, Player player, int level) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
    
        String expectedUpgradeItem = levelsConfig.getString(level + ".upgrade_item.name");
        int expectedModelData = levelsConfig.getInt(level + ".upgrade_item.material_customModelData");

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasCustomModelData()) {
                int modelData = meta.getCustomModelData(); // ใช้ getCustomModelData แทน
                
                if (modelData != expectedModelData) {
                    if (!player.hasMetadata("UpgradeMessageDisplayed")) {
                        player.sendMessage(ChatColor.RED + "ต้องใช้ " + ChatColor.GOLD + expectedUpgradeItem + level + ChatColor.RED + " เพื่อไป +" + level);
                        player.setMetadata("UpgradeMessageDisplayed", new FixedMetadataValue(this, true));
                    }
                    return false;
                }
            } else {
                if (!player.hasMetadata("UpgradeMessageDisplayed")) {
                    player.sendMessage(ChatColor.RED + "ไม่มีข้อมูล Custom Model Data! ต้องใช้ " + ChatColor.GOLD + expectedUpgradeItem + ChatColor.RED + " เพื่อไป +" + level);
                    player.setMetadata("UpgradeMessageDisplayed", new FixedMetadataValue(this, true));
                }
                return false;
            }
        } else {
            if (!player.hasMetadata("UpgradeMessageDisplayed")) {
                player.sendMessage(ChatColor.RED + "ไม่มีข้อมูล Custom Model Data! ต้องใช้ " + ChatColor.GOLD + expectedUpgradeItem + ChatColor.RED + " เพื่อไป +" + level);
                player.setMetadata("UpgradeMessageDisplayed", new FixedMetadataValue(this, true));
            }
            return false;
        }
    
        return true; // คืนค่า true ถ้าไอเทมตรง
    }

    private void registerRecipes() {
        File upgradeFolder = new File(getDataFolder(), "upgrades"); // พาธไปยังโฟลเดอร์ Upgrades
    
        if (!upgradeFolder.exists() || !upgradeFolder.isDirectory()) {
            getLogger().severe("Upgrades folder is missing or not a directory!");
            return;
        }
    
        File[] files = upgradeFolder.listFiles((dir, name) -> name.endsWith(".yml")); // ดึงไฟล์ .yml ทั้งหมดในโฟลเดอร์ Upgrades
        if (files == null || files.length == 0) {
            getLogger().severe("No upgrade files found in the Upgrades folder!");
            return;
        }
    
        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file); // โหลด config จากไฟล์
    
            String weaponName = file.getName().replace(".yml", ""); // ใช้ชื่อไฟล์เป็น key
            String itemName = config.getString("name"); // ชื่อที่จะแสดง
            int maxLevel = config.getInt("max_level");
    
            // ตรวจสอบว่ามีคีย์ "upgrades" ใน config หรือไม่
            if (config.contains("upgrades")) {
                for (int level : config.getIntegerList("upgrades")) {
                    // อ่านค่าจาก customModelData
                    int customModelDataValue = config.getInt("customModelData." + level, 1); // ค่าเริ่มต้นเป็น 1 ถ้าค่าไม่พบ
    
                    // สร้าง ItemStack สำหรับผลลัพธ์
                    ItemStack result = new ItemStack(Material.valueOf(weaponName.toUpperCase()));
                    RecipeChoice weaponChoice = new RecipeChoice.MaterialChoice(Material.valueOf(weaponName.toUpperCase()));
                    RecipeChoice templateChoice = new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);

                    // ตรวจสอบวัสดุอัปเกรดจาก levelsConfig
                    String upgradeItemMaterial = levelsConfig.getString(level + ".upgrade_item.material").toUpperCase();
                    int upgradeItemCustomModelData = levelsConfig.getInt(level + ".upgrade_item.material_customModelData", 1);
    
                    // ตั้งค่า Custom Model Data
                    ItemMeta resultMeta = result.getItemMeta();
                    if (resultMeta != null) {
                        resultMeta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_model_data"), PersistentDataType.INTEGER, upgradeItemCustomModelData);
                        resultMeta.setDisplayName(ChatColor.GOLD + itemName); // ตั้งชื่อไอเทม
                        resultMeta.setCustomModelData(upgradeItemCustomModelData);
                        result.setItemMeta(resultMeta);
                    }
    
                    // สร้าง RecipeChoice สำหรับวัสดุอัปเกรด
                    try {
                        RecipeChoice materialChoice = new RecipeChoice.MaterialChoice(Material.valueOf(upgradeItemMaterial));
    
                        // สร้าง SmithingTransformRecipe
                        NamespacedKey key = new NamespacedKey(this, "custom_smithing_" + weaponName + "_level_" + level);
                        SmithingTransformRecipe recipe = new SmithingTransformRecipe(key, result, templateChoice, weaponChoice, materialChoice);
                        Bukkit.addRecipe(recipe);
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid material: " + upgradeItemMaterial + " for weapon: " + weaponName + " level: " + level);
                    }
                }
            } else {
                getLogger().warning("No 'upgrades' section found in: " + file.getName());
            }
        }
    }
    
}
