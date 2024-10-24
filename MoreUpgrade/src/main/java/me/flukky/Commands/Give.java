package me.flukky.Commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.flukky.MoreUpgrade;

public class Give implements CommandExecutor, TabCompleter {
    private MoreUpgrade plugin;

    public Give(MoreUpgrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // ตรวจสอบว่า sender เป็นผู้เล่นและมี OP
        if (!(sender instanceof Player) || !sender.isOp()) {
            sender.sendMessage("คุณต้องเป็นผู้เล่นที่มี OP ถึงจะใช้คำสั่งนี้!");
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String level = args[1];
            String playerName = args[2];

            Player targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer == null) {
                sender.sendMessage("ผู้เล่น '" + playerName + "' ไม่ออนไลน์!");
                return true;
            }

            // กรณีที่ต้องการแจกทุกเลเวล
            if (level.equalsIgnoreCase("all")) {
                for (String lvl : plugin.getLevel().getKeys(false)) {
                    giveItem(targetPlayer, lvl);
                }
                return true;
            }

            // กรณีแจกเลเวลเดี่ยว
            if (plugin.getLevel().contains(level)) {
                giveItem(targetPlayer, level);
                return true;
            } else {
                sender.sendMessage("ไม่มีเลเวล " + level + " ในไฟล์ levels.yml");
            }
            return true;
        } else {
            sender.sendMessage("การใช้คำสั่งไม่ถูกต้อง! ใช้ /mu give <level> <player> หรือ /mu give all <player>");
        }
        return false;
    }

    private void giveItem(Player player, String level) {
        String materialName = plugin.getLevel().getString(level + ".upgrade_item.material");
        String materialNameDisplay = plugin.getLevel().getString(level + ".upgrade_item.name");
        int materialCustomModelData = plugin.getLevel().getInt(level + ".upgrade_item.material_customModelData");

        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material != null) {
            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + materialNameDisplay + " +" + level); // ตั้งชื่อไอเทม
                meta.setCustomModelData(materialCustomModelData);
                item.setItemMeta(meta);
            }

            player.getInventory().addItem(item); // เพิ่มไอเทมในอินเวนทอรีของผู้เล่นที่เลือก
            player.sendMessage("คุณได้รับไอเทมเลเวล " + level + " พร้อม custom_model_data: " + materialCustomModelData);
        } else {
            player.sendMessage("วัสดุไม่ถูกต้องในไฟล์ levels.yml: " + materialName);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // เติมคำสั่ง "give"
            if ("give".startsWith(args[0].toLowerCase())) {
                completions.add("give");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // เติมเลเวลจาก levels.yml
            for (String level : plugin.getLevel().getConfigurationSection("").getKeys(false)) {
                if (level.startsWith(args[1].toLowerCase())) {
                    completions.add(level);
                    completions.add("all");
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // เติมชื่อผู้เล่นที่ออนไลน์
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(onlinePlayer.getName());
                }
            }
        }

        return completions;
    }
}
