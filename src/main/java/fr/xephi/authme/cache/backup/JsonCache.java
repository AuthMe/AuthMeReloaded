package fr.xephi.authme.cache.backup;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.*;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;

public class JsonCache {

    private final Gson gson;
    private final AuthMe plugin;
    private final File cacheDir;

    public JsonCache(AuthMe plugin) {
        this.plugin = plugin;
        cacheDir = Settings.CACHE_FOLDER;
        if (!cacheDir.exists() && !cacheDir.isDirectory() && !cacheDir.mkdir()) {
            ConsoleLogger.showError("Failed to create cache directory.");
        }
        gson = new GsonBuilder()
                .registerTypeAdapter(DataFileCache.class, new PlayerDataSerializer())
                .registerTypeAdapter(DataFileCache.class, new PlayerDataDeserializer())
                .setPrettyPrinting()
                .create();
    }

    public void createCache(Player player, DataFileCache playerData) {
        if (player == null) {
            return;
        }

        String path;
        try {
            path = player.getUniqueId().toString();
        } catch (Exception | Error e) {
            path = player.getName().toLowerCase();
        }

        File file = new File(cacheDir, path + File.separator + "cache.json");
        if (file.exists()) {
            return;
        }
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            return;
        }

        try {
            String data = gson.toJson(playerData);
            Files.touch(file);
            Files.write(data, file, Charsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DataFileCache readCache(Player player) {
        String path;
        try {
            path = player.getUniqueId().toString();
        } catch (Exception | Error e) {
            path = player.getName().toLowerCase();
        }

        File file = new File(cacheDir, path + File.separator + "cache.json");
        if (!file.exists()) {
            return null;
        }

        try {
            String str = Files.toString(file, Charsets.UTF_8);
            return gson.fromJson(str, DataFileCache.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private class PlayerDataSerializer implements JsonSerializer<DataFileCache> {
        @Override
        public JsonElement serialize(DataFileCache dataFileCache, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("group", dataFileCache.getGroup());
            jsonObject.addProperty("operator", dataFileCache.getOperator());
            jsonObject.addProperty("flying", dataFileCache.isFlying());

            JsonArray arr;
            ItemStack[] contents;

            // inventory
            contents = dataFileCache.getInventory();
            arr = new JsonArray();
            putItems(contents, arr);
            jsonObject.add("inventory", arr);

            // armour
            contents = dataFileCache.getArmour();
            arr = new JsonArray();
            putItems(contents, arr);
            jsonObject.add("armour", arr);

            return jsonObject;
        }

        private void putItems(ItemStack[] contents, JsonArray target) {
            for (ItemStack item : contents) {
                if (item == null) {
                    item = new ItemStack(Material.AIR);
                }
                JsonObject val = new JsonObject();
                if (item.getType() == Material.SKULL_ITEM) {
                    SkullMeta meta = (SkullMeta) item.getItemMeta();
                    if (meta.hasOwner() && (meta.getOwner() == null || meta.getOwner().isEmpty())) {
                        item.setItemMeta(plugin.getServer().getItemFactory().getItemMeta(Material.SKULL_ITEM));
                    }
                }
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    BukkitObjectOutputStream objectOut = new BukkitObjectOutputStream(baos);
                    objectOut.writeObject(item);
                    objectOut.close();
                    val.addProperty("item", Base64Coder.encodeLines(baos.toByteArray()));
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
                target.add(val);
            }
        }
    }

    private static class PlayerDataDeserializer implements JsonDeserializer<DataFileCache> {
        @Override
        public DataFileCache deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject == null) {
                return null;
            }
            JsonElement e;
            String group = null;
            boolean operator = false;
            boolean flying = false;

            if ((e = jsonObject.get("group")) != null) {
                group = e.getAsString();
            }
            if ((e = jsonObject.get("operator")) != null) {
                operator = e.getAsBoolean();
            }
            if ((e = jsonObject.get("flying")) != null) {
                flying = e.getAsBoolean();
            }

            JsonArray arr;
            ItemStack[] inv = null;
            ItemStack[] armour = null;

            if (jsonObject.has("inventory")) {
                arr = jsonObject.get("inventory").getAsJsonArray();
                inv = getItems(arr);
            }

            if (jsonObject.has("armour")) {
                arr = jsonObject.get("armour").getAsJsonArray();
                armour = getItems(arr);
            }

            return new DataFileCache(inv, armour, group, operator, flying);
        }

        private ItemStack[] getItems(JsonArray arr) {
            ItemStack[] contents = new ItemStack[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                JsonObject item = arr.get(i).getAsJsonObject();
                String encoded = item.get("item").getAsString();
                byte[] decoded = Base64Coder.decodeLines(encoded);
                try {
                    ByteArrayInputStream baos = new ByteArrayInputStream(decoded);
                    BukkitObjectInputStream objectIn = new BukkitObjectInputStream(baos);
                    contents[i] = (ItemStack) objectIn.readObject();
                    objectIn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return contents;
        }
    }

    public void removeCache(Player player) {
        String path;
        try {
            path = player.getUniqueId().toString();
        } catch (Exception | Error e) {
            path = player.getName().toLowerCase();
        }
        File file = new File(cacheDir, path);
        if (file.exists()) {
            Utils.purgeDirectory(file);
            if (!file.delete()) {
                ConsoleLogger.showError("Failed to remove" + player.getName() + "cache.");
            }
        }
    }

    public boolean doesCacheExist(Player player) {
        String path;
        try {
            path = player.getUniqueId().toString();
        } catch (Exception | Error e) {
            path = player.getName().toLowerCase();
        }
        File file = new File(cacheDir, path + File.separator + "cache.json");
        return file.exists();
    }

}
