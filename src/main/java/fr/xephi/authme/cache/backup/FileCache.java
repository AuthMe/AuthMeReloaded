package fr.xephi.authme.cache.backup;

import com.google.common.io.BaseEncoding;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.api.API;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class FileCache {

    private final File cacheDir;
    private AuthMe plugin;

    public FileCache(AuthMe plugin) {
        this.plugin = plugin;
        cacheDir = new File(plugin.getDataFolder() + File.separator + "cache");
        if (!cacheDir.exists() && !cacheDir.isDirectory() && !cacheDir.mkdir()) {
            ConsoleLogger.showError("Failed to create cache directory.");
        }
    }

    public void createCache(Player player, DataFileCache playerData,
                            String group, boolean operator, boolean flying) {
        if (player == null) {
            return;
        }

        String path;
        try {
            path = player.getUniqueId().toString();
        } catch (Exception | Error e) {
            path = player.getName().toLowerCase();
        }

        File playerDir = new File(cacheDir, path);
        if (!playerDir.exists() && !playerDir.isDirectory() && !playerDir.mkdir()) {
            return;
        }

        File datafile = new File(playerDir, "playerdatas.cache");
        if (datafile.exists()) {
            return;
        }

        FileWriter writer;
        try {
            datafile.createNewFile();
            writer = new FileWriter(datafile);
            writer.write(group + API.newline);
            writer.write(String.valueOf(operator) + API.newline);
            writer.write(String.valueOf(flying) + API.newline);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        BaseEncoding base64 = BaseEncoding.base64();

        File invDir = new File(playerDir, "inventory");
        if (!invDir.isDirectory() && !invDir.mkdir())
            return;

        ItemStack[] inv = playerData.getInventory();
        for (int i = 0; i < inv.length; i++) {
            ItemStack item = inv[i];
            if (item == null) {
                item = new ItemStack(Material.AIR);
            }
            if (item.getType() == Material.SKULL_ITEM) {
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                if (meta.hasOwner() && (meta.getOwner() == null || meta.getOwner().isEmpty())) {
                    item.setItemMeta(plugin.getServer().getItemFactory().getItemMeta(Material.SKULL_ITEM));
                }
            }

            try {
                File cacheFile = new File(invDir, i + ".cache");
                if (!cacheFile.isFile() && !cacheFile.createNewFile()) {
                    continue;
                }
                writer = new FileWriter(cacheFile);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BukkitObjectOutputStream objectOut = new BukkitObjectOutputStream(baos);
                objectOut.writeObject(item);
                objectOut.close();
                writer.write(base64.encode(baos.toByteArray()));
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        File armourDir = new File(cacheDir, path + File.separator + "armours");
        if (!armourDir.isDirectory() && !armourDir.mkdir())
            return;

        ItemStack[] armors = playerData.getArmour();
        for (int i = 0; i < armors.length; i++) {
            ItemStack item = armors[i];
            if (item == null) {
                item = new ItemStack(Material.AIR);
            }
            if (item.getType() == Material.SKULL_ITEM) {
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                if (meta.hasOwner() && (meta.getOwner() == null || meta.getOwner().isEmpty())) {
                    item.setItemMeta(plugin.getServer().getItemFactory().getItemMeta(Material.SKULL_ITEM));
                }
            }

            try {
                File cacheFile = new File(armourDir, i + ".cache");
                if (!cacheFile.isFile() && !cacheFile.createNewFile()) {
                    continue;
                }
                writer = new FileWriter(cacheFile);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BukkitObjectOutputStream objectOut = new BukkitObjectOutputStream(baos);
                objectOut.writeObject(item);
                objectOut.close();
                writer.write(base64.encode(baos.toByteArray()));
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public DataFileCache readCache(Player player) {
        if (player == null) {
            return null;
        }

        String path;
        try {
            path = player.getUniqueId().toString();
        } catch (Exception | Error e) {
            path = player.getName().toLowerCase();
        }

        File playerDir = new File(cacheDir, path);
        if (!playerDir.exists() && !playerDir.isDirectory()) {
            return null;
        }

        try {
            File datafile = new File(playerDir, "playerdatas.cache");
            if (!datafile.exists() || !datafile.isFile()) {
                return null;
            }
            ItemStack[] inv = new ItemStack[36];
            ItemStack[] armours = new ItemStack[4];
            String group = null;
            boolean op = false;
            boolean flying = false;

            Scanner reader;
            try {
                reader = new Scanner(datafile);
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
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            BaseEncoding base64 = BaseEncoding.base64();

            File invDir = new File(playerDir, "inventory");
            for (int i = 0; i < inv.length; i++) {
                byte[] bytes = Files.readAllBytes(Paths.get(invDir.getPath(), i + ".cache"));
                String encodedItem = new String(bytes);
                bytes = base64.decode(encodedItem);
                ByteArrayInputStream baos = new ByteArrayInputStream(bytes);
                BukkitObjectInputStream objectIn = new BukkitObjectInputStream(baos);
                ItemStack item = (ItemStack) objectIn.readObject();
                objectIn.close();
                if (item == null) {
                    inv[i] = new ItemStack(Material.AIR);
                } else {
                    inv[i] = item;
                }
            }

            File armourDir = new File(playerDir, "armours");
            for (int i = 0; i < armours.length; i++) {
                byte[] bytes = Files.readAllBytes(Paths.get(armourDir.getPath(), i + ".cache"));
                String encodedItem = new String(bytes);
                bytes = base64.decode(encodedItem);
                ByteArrayInputStream baos = new ByteArrayInputStream(bytes);
                BukkitObjectInputStream objectIn = new BukkitObjectInputStream(baos);
                ItemStack item = (ItemStack) objectIn.readObject();
                objectIn.close();
                if (item == null) {
                    armours[i] = new ItemStack(Material.AIR);
                } else {
                    armours[i] = item;
                }
            }

            return new DataFileCache(inv, armours, group, op, flying);

        } catch (Exception e) {
            e.printStackTrace();
            ConsoleLogger.showError("Error while reading file for " + player.getName() + ", some wipe inventory incoming...");
            return null;
        }
    }

    public void removeCache(Player player) {
        String path;
        try {
            path = player.getUniqueId().toString();
        } catch (Exception | Error e) {
            path = player.getName().toLowerCase();
        }
        try {
            File file = new File(cacheDir, path);
            if (file.list() != null) {
                Utils.purgeDirectory(file);
                if (!file.delete()) {
                    ConsoleLogger.showError("Failed to remove" + player.getName() + "cache.");
                }
            } else {
                file = new File(cacheDir, player.getName().toLowerCase() + ".cache");
                if (file.isFile() && !file.delete()) {
                    ConsoleLogger.showError("Failed to remove" + player.getName() + "cache.");
                }
            }
        } catch (Exception e) {
            ConsoleLogger.showError("Failed to remove" + player.getName() + "cache :/");
        }
    }

    public boolean doesCacheExist(Player player) {
        String path;
        try {
            path = player.getUniqueId().toString();
        } catch (Exception | Error e) {
            path = player.getName().toLowerCase();
        }
        File file = new File(cacheDir, path + File.separator + "playerdatas.cache");
        if (!file.exists()) {
            file = new File(cacheDir, player.getName().toLowerCase() + ".cache");
        }

        return file.exists();
    }

}
