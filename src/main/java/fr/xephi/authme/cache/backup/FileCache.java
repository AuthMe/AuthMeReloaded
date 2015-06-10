package fr.xephi.authme.cache.backup;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.comphenix.attribute.Attributes;
import com.comphenix.attribute.Attributes.Attribute;
import com.comphenix.attribute.Attributes.Attribute.Builder;
import com.comphenix.attribute.Attributes.AttributeType;
import com.comphenix.attribute.Attributes.Operation;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.api.API;
import fr.xephi.authme.settings.Settings;

public class FileCache {

    private AuthMe plugin;

    public FileCache(AuthMe plugin) {
        this.plugin = plugin;
        final File file = new File(plugin.getDataFolder() + File.separator + "cache");
        if (!file.exists())
            file.mkdir();
    }

    public void createCache(Player player, DataFileCache playerData,
            String group, boolean operator, boolean flying) {
        String path = "";
        if (player == null)
            return;
        try {
            path = player.getUniqueId().toString();
        } catch (Exception e) {
            path = player.getName();
        } catch (Error e) {
            path = player.getName();
        }
        File file = new File(plugin.getDataFolder() + File.separator + "cache" + File.separator + path + File.separator + "playerdatas.cache");

        if (!file.getParentFile().exists())
            file.getParentFile().mkdirs();
        if (file.exists()) {
            return;
        }

        FileWriter writer = null;
        try {
            file.createNewFile();

            writer = new FileWriter(file);
            writer.write(group + API.newline);
            writer.write(String.valueOf(operator) + API.newline);
            writer.write(String.valueOf(flying) + API.newline);
            writer.close();

            file = new File(plugin.getDataFolder() + File.separator + "cache" + File.separator + path + File.separator + "inventory");

            file.mkdirs();
            ItemStack[] inv = playerData.getInventory();
            for (int i = 0; i < inv.length; i++) {
                ItemStack item = inv[i];
                file = new File(plugin.getDataFolder() + File.separator + "cache" + File.separator + path + File.separator + "inventory" + File.separator + i + ".cache");
                file.createNewFile();
                writer = new FileWriter(file);
                if (item != null) {
                    if (item.getType() == Material.AIR) {
                        writer.write("AIR");
                        writer.close();
                        continue;
                    }
                    writer.write(item.getType().name() + API.newline);
                    writer.write(item.getDurability() + API.newline);
                    writer.write(item.getAmount() + API.newline);
                    writer.flush();
                    if (item.hasItemMeta()) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta.hasDisplayName())
                            writer.write("name=" + meta.getDisplayName() + API.newline);
                        if (meta.hasLore()) {
                            String lores = "";
                            for (String lore : meta.getLore())
                                lores = lore + "%newline%";
                            writer.write("lore=" + lores + API.newline);
                        }
                        if (meta.hasEnchants()) {
                            for (Enchantment ench : meta.getEnchants().keySet()) {
                                writer.write("metaenchant=" + ench.getName() + ":" + meta.getEnchants().get(ench) + API.newline);
                            }
                        }
                        writer.flush();
                    }
                    for (Enchantment ench : item.getEnchantments().keySet()) {
                        writer.write("enchant=" + ench.getName() + ":" + item.getEnchantments().get(ench) + API.newline);
                    }
                    if (Settings.customAttributes) {
                        try {
                            Attributes attributes = new Attributes(item);
                            if (attributes != null) {
                                Iterator<Attribute> iter = attributes.values().iterator();
                                Attribute a = null;
                                while (iter.hasNext()) {
                                    Attribute b = iter.next();
                                    if (a != null && a == b)
                                        break;
                                    a = b;
                                    if (a != null) {
                                        if (a.getName() != null && a.getAttributeType() != null && a.getOperation() != null && a.getUUID() != null)
                                            writer.write("attribute=" + a.getName() + ";" + a.getAttributeType().getMinecraftId() + ";" + a.getAmount() + ";" + a.getOperation().getId() + ";" + a.getUUID().toString());
                                    }
                                }
                            }
                        } catch (Exception e) {
                        } catch (Error e) {
                        }
                    }
                } else {
                    writer.write("AIR");
                }
                writer.close();
            }

            file = new File(plugin.getDataFolder() + File.separator + "cache" + File.separator + path + File.separator + "armours");
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            file.mkdirs();

            ItemStack[] armors = playerData.getArmour();
            for (int i = 0; i < armors.length; i++) {
                ItemStack item = armors[i];
                file = new File(plugin.getDataFolder() + File.separator + "cache" + File.separator + path + File.separator + "armours" + File.separator + i + ".cache");
                file.createNewFile();
                writer = new FileWriter(file);
                if (item != null) {
                    if (item.getType() == Material.AIR) {
                        writer.write("AIR");
                        writer.close();
                        continue;
                    }
                    writer.write(item.getType().name() + API.newline);
                    writer.write(item.getDurability() + API.newline);
                    writer.write(item.getAmount() + API.newline);
                    writer.flush();
                    if (item.hasItemMeta()) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta.hasDisplayName())
                            writer.write("name=" + meta.getDisplayName() + API.newline);
                        if (meta.hasLore()) {
                            String lores = "";
                            for (String lore : meta.getLore())
                                lores = lore + "%newline%";
                            writer.write("lore=" + lores + API.newline);
                        }
                        writer.flush();
                    }
                    for (Enchantment ench : item.getEnchantments().keySet()) {
                        writer.write("enchant=" + ench.getName() + ":" + item.getEnchantments().get(ench) + API.newline);
                    }
                    if (Settings.customAttributes) {
                        try {
                            Attributes attributes = new Attributes(item);
                            if (attributes != null)
                                while (attributes.values().iterator().hasNext()) {
                                    Attribute a = attributes.values().iterator().next();
                                    if (a != null) {
                                        if (a.getName() != null && a.getAttributeType() != null && a.getOperation() != null && a.getUUID() != null && a.getAttributeType().getMinecraftId() != null)
                                            writer.write("attribute=" + a.getName() + ";" + a.getAttributeType().getMinecraftId() + ";" + a.getAmount() + ";" + a.getOperation().getId() + ";" + a.getUUID().toString());
                                    }
                                }
                        } catch (Exception e) {
                        }
                    }
                } else {
                    writer.write("AIR" + API.newline);
                }
                writer.close();
            }
        } catch (final Exception e) {
            ConsoleLogger.showError("Some error on creating file cache...");
        }
    }

    public DataFileCache readCache(Player player) {
        String path = "";
        try {
            path = player.getUniqueId().toString();
        } catch (Exception e) {
            path = player.getName();
        } catch (Error e) {
            path = player.getName();
        }
        try {
            File file = new File(plugin.getDataFolder() + File.separator + "cache" + File.separator + path + File.separator + ".playerdatas.cache");
            String playername = player.getName().toLowerCase();
            if (!file.exists()) {
                // OLD METHOD
                file = new File("cache/" + playername + ".cache");
                ItemStack[] stacki = new ItemStack[36];
                ItemStack[] stacka = new ItemStack[4];
                if (!file.exists()) {
                    return new DataFileCache(stacki, stacka);
                }
                String group = null;
                boolean op = false;
                boolean flying = false;

                Scanner reader = null;
                try {
                    reader = new Scanner(file);

                    int i = 0;
                    int a = 0;
                    while (reader.hasNextLine()) {
                        String line = reader.nextLine();

                        if (!line.contains(":")) {
                            // the fist line represent the player group,
                            // operator
                            // status
                            // and flying status
                            final String[] playerInfo = line.split(";");
                            group = playerInfo[0];

                            if (Integer.parseInt(playerInfo[1]) == 1) {
                                op = true;
                            } else op = false;
                            if (playerInfo.length > 2) {
                                if (Integer.parseInt(playerInfo[2]) == 1)
                                    flying = true;
                                else flying = false;
                            }

                            continue;
                        }

                        if (!line.startsWith("i") && !line.startsWith("w")) {
                            continue;
                        }
                        String lores = "";
                        String name = "";
                        if (line.split("\\*").length > 1) {
                            lores = line.split("\\*")[1];
                            line = line.split("\\*")[0];
                        }
                        if (line.split(";").length > 1) {
                            name = line.split(";")[1];
                            line = line.split(";")[0];
                        }
                        final String[] in = line.split(":");
                        // can enchant item? size ofstring in file - 4 all / 2 =
                        // number
                        // of enchant
                        if (in[0].equals("i")) {
                            stacki[i] = new ItemStack(Material.getMaterial(in[1]), Integer.parseInt(in[2]), Short.parseShort((in[3])));
                            if (in.length > 4 && !in[4].isEmpty()) {
                                for (int k = 4; k < in.length - 1; k++) {
                                    stacki[i].addUnsafeEnchantment(Enchantment.getByName(in[k]), Integer.parseInt(in[k + 1]));
                                    k++;
                                }
                            }
                            try {
                                ItemMeta meta = plugin.getServer().getItemFactory().getItemMeta(stacki[i].getType());
                                if (!name.isEmpty()) {
                                    meta.setDisplayName(name);
                                }
                                if (!lores.isEmpty()) {
                                    List<String> loreList = new ArrayList<String>();
                                    for (String s : lores.split("%newline%")) {
                                        loreList.add(s);
                                    }
                                    meta.setLore(loreList);
                                }
                                if (meta != null)
                                    stacki[i].setItemMeta(meta);
                            } catch (Exception e) {
                            }
                            i++;
                        } else {
                            stacka[a] = new ItemStack(Material.getMaterial(in[1]), Integer.parseInt(in[2]), Short.parseShort((in[3])));
                            if (in.length > 4 && !in[4].isEmpty()) {
                                for (int k = 4; k < in.length - 1; k++) {
                                    stacka[a].addUnsafeEnchantment(Enchantment.getByName(in[k]), Integer.parseInt(in[k + 1]));
                                    k++;
                                }
                            }
                            try {
                                ItemMeta meta = plugin.getServer().getItemFactory().getItemMeta(stacka[a].getType());
                                if (!name.isEmpty())
                                    meta.setDisplayName(name);
                                if (!lores.isEmpty()) {
                                    List<String> loreList = new ArrayList<String>();
                                    for (String s : lores.split("%newline%")) {
                                        loreList.add(s);
                                    }
                                    meta.setLore(loreList);
                                }
                                if (meta != null)
                                    stacki[i].setItemMeta(meta);
                            } catch (Exception e) {
                            }
                            a++;
                        }
                    }
                } catch (final Exception e) {
                    ConsoleLogger.showError("Error on creating a file cache for " + player.getName() + ", maybe wipe inventory...");
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
                return new DataFileCache(stacki, stacka, group, op, flying);
            } else {
                // NEW METHOD
                ItemStack[] inv = new ItemStack[36];
                ItemStack[] armours = new ItemStack[4];
                String group = null;
                boolean op = false;
                boolean flying = false;

                Scanner reader = null;
                try {
                    reader = new Scanner(file);

                    int count = 1;
                    while (reader.hasNextLine()) {
                        String line = reader.nextLine();
                        switch (count) {
                            case 1:
                                group = line;
                                break;
                            case 2:
                                op = Boolean.parseBoolean(line);
                                break;
                            case 3:
                                flying = Boolean.parseBoolean(line);
                                break;
                            default:
                                break;
                        }
                        count++;
                    }
                    if (reader != null)
                        reader.close();
                    for (int i = 0; i < inv.length; i++) {
                        reader = new Scanner(new File(plugin.getDataFolder() + File.separator + "cache" + File.separator + path + File.separator + "inventory" + File.separator + i + ".cache"));
                        ItemStack item = new ItemStack(Material.AIR);
                        ItemMeta meta = null;
                        Attributes attributes = null;
                        count = 1;
                        boolean v = true;
                        while (reader.hasNextLine() && v == true) {
                            String line = reader.nextLine();
                            switch (count) {
                                case 1:
                                    item.setType(Material.getMaterial(line));
                                    if (item.getType() == Material.AIR)
                                        v = false;
                                    continue;
                                case 2:
                                    item.setDurability(Short.parseShort(line));
                                    continue;
                                case 3:
                                    item.setAmount(Integer.parseInt(line));
                                    continue;
                                case 4:
                                    meta = Bukkit.getItemFactory().getItemMeta(item.getType());
                                    break;
                                default:
                                    break;
                            }
                            if (line.startsWith("name=")) {
                                line = line.substring(5);
                                meta.setDisplayName(line);
                                item.setItemMeta(meta);
                                continue;
                            }
                            if (line.startsWith("lore=")) {
                                line = line.substring(5);
                                List<String> lore = new ArrayList<String>();
                                for (String s : line.split("%newline%"))
                                    lore.add(s);
                                meta.setLore(lore);
                                item.setItemMeta(meta);
                                continue;
                            }
                            if (line.startsWith("enchant=")) {
                                line = line.substring(8);
                                item.addEnchantment(Enchantment.getByName(line.split(":")[0]), Integer.parseInt(line.split(":")[1]));
                            }
                            if (Settings.customAttributes) {
                                if (line.startsWith("attribute=")) {
                                    if (attributes == null)
                                        attributes = new Attributes(item);
                                    try {
                                        line = line.substring(10);
                                        String[] args = line.split(";");
                                        if (args.length != 5)
                                            continue;
                                        String name = args[0];
                                        AttributeType type = AttributeType.fromId(args[1]);
                                        double amount = Double.parseDouble(args[2]);
                                        Operation operation = Operation.fromId(Integer.parseInt(args[3]));
                                        UUID uuid = UUID.fromString(args[4]);
                                        Attribute attribute = new Attribute(new Builder(amount, operation, type, name, uuid));
                                        attributes.add(attribute);
                                    } catch (Exception e) {
                                    }
                                }
                            }
                            count++;
                        }
                        if (reader != null)
                            reader.close();
                        if (attributes != null)
                            inv[i] = attributes.getStack();
                        else inv[i] = item;
                    }
                    for (int i = 0; i < armours.length; i++) {
                        reader = new Scanner(new File(plugin.getDataFolder() + File.separator + "cache" + File.separator + path + File.separator + "armours" + File.separator + i + ".cache"));
                        ItemStack item = new ItemStack(Material.AIR);
                        ItemMeta meta = null;
                        Attributes attributes = null;
                        count = 1;
                        boolean v = true;
                        while (reader.hasNextLine() && v == true) {
                            String line = reader.nextLine();
                            switch (count) {
                                case 1:
                                    item.setType(Material.getMaterial(line));
                                    if (item.getType() == Material.AIR)
                                        v = false;
                                    continue;
                                case 2:
                                    item.setDurability(Short.parseShort(line));
                                    continue;
                                case 3:
                                    item.setAmount(Integer.parseInt(line));
                                    continue;
                                case 4:
                                    meta = Bukkit.getItemFactory().getItemMeta(item.getType());
                                    break;
                                default:
                                    break;
                            }
                            if (line.startsWith("name=")) {
                                line = line.substring(5);
                                meta.setDisplayName(line);
                                item.setItemMeta(meta);
                                continue;
                            }
                            if (line.startsWith("lore=")) {
                                line = line.substring(5);
                                List<String> lore = new ArrayList<String>();
                                for (String s : line.split("%newline%"))
                                    lore.add(s);
                                meta.setLore(lore);
                                item.setItemMeta(meta);
                                continue;
                            }
                            if (line.startsWith("enchant=")) {
                                line = line.substring(8);
                                item.addEnchantment(Enchantment.getByName(line.split(":")[0]), Integer.parseInt(line.split(":")[1]));
                            }
                            if (Settings.customAttributes) {
                                if (line.startsWith("attribute=")) {
                                    if (attributes == null)
                                        attributes = new Attributes(item);
                                    try {
                                        line = line.substring(10);
                                        String[] args = line.split(";");
                                        if (args.length != 5)
                                            continue;
                                        String name = args[0];
                                        AttributeType type = AttributeType.fromId(args[1]);
                                        double amount = Double.parseDouble(args[2]);
                                        Operation operation = Operation.fromId(Integer.parseInt(args[3]));
                                        UUID uuid = UUID.fromString(args[4]);
                                        Attribute attribute = new Attribute(new Builder(amount, operation, type, name, uuid));
                                        attributes.add(attribute);
                                    } catch (Exception e) {
                                    }
                                }
                            }
                            count++;
                        }
                        if (reader != null)
                            reader.close();
                        if (attributes != null)
                            armours[i] = attributes.getStack();
                        else armours[i] = item;
                    }

                } catch (final Exception e) {
                    ConsoleLogger.showError("Error while reading file for " + player.getName() + ", some wipe inventory incoming...");
                } finally {
                    if (reader != null)
                        reader.close();
                }
                return new DataFileCache(inv, armours, group, op, flying);
            }
        } catch (Exception e) {
            ConsoleLogger.showError("Error while reading file for " + player.getName() + ", some wipe inventory incoming...");
            return null;
        }
    }

    public void removeCache(Player player) {
        String path = "";
        try {
            path = player.getUniqueId().toString();
        } catch (Exception e) {
            path = player.getName();
        } catch (Error e) {
            path = player.getName();
        }
        try {
            File file = new File(plugin.getDataFolder() + File.separator + "cache" + File.separator + path);
            if (!file.exists()) {
                file = new File("cache/" + player.getName().toLowerCase() + ".cache");
            }
            if (file.exists()) {
                if (file.isDirectory()) {
                    for (File f : file.listFiles()) {
                        if (f.isDirectory()) {
                            for (File a : f.listFiles()) {
                                a.delete();
                            }
                            f.delete();
                        } else f.delete();
                    }
                    file.delete();
                } else file.delete();
            }
        } catch (Exception e) {
            ConsoleLogger.showError("File cannot be removed correctly :/");
        }
    }

    public boolean doesCacheExist(Player player) {
        String path = "";
        try {
            path = player.getUniqueId().toString();
        } catch (Exception e) {
            path = player.getName();
        } catch (Error e) {
            path = player.getName();
        }
        File file = new File(plugin.getDataFolder() + File.separator + "cache" + File.separator + path + File.separator + "playerdatas.cache");
        if (!file.exists()) {
            file = new File("cache/" + player.getName().toLowerCase() + ".cache");
        }

        if (file.exists()) {
            return true;
        }
        return false;
    }

}
