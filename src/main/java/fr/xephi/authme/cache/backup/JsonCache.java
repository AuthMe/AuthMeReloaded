package fr.xephi.authme.cache.backup;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.Settings;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 */
public class JsonCache {

    private final Gson gson;
    private final File cacheDir;

    public JsonCache() {
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

    public void removeCache(Player player) {
        String path;
        try {
            path = player.getUniqueId().toString();
        } catch (Exception | Error e) {
            path = player.getName().toLowerCase();
        }
        File file = new File(cacheDir, path);
        if (file.exists()) {
            purgeDirectory(file);
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

    private class PlayerDataDeserializer implements JsonDeserializer<DataFileCache> {
        @Override
        public DataFileCache deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject == null) {
                return null;
            }
            String group = null;
            boolean operator = false;
            boolean fly = false;

            JsonElement e;
            if ((e = jsonObject.get("group")) != null) {
                group = e.getAsString();
            }
            if ((e = jsonObject.get("operator")) != null) {
                operator = e.getAsBoolean();
            }
            if ((e = jsonObject.get("fly")) != null) {
                fly = e.getAsBoolean();
            }

            return new DataFileCache(group, operator, fly);
        }
    }

    private class PlayerDataSerializer implements JsonSerializer<DataFileCache> {
        @Override
        public JsonElement serialize(DataFileCache dataFileCache, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("group", dataFileCache.getGroup());
            jsonObject.addProperty("operator", dataFileCache.getOperator());
            jsonObject.addProperty("fly", dataFileCache.isFlyEnabled());
            return jsonObject;
        }
    }

    /**
     * Delete a given directory and all its content.
     *
     * @param directory The directory to remove
     */
    private static void purgeDirectory(File directory) {
        if (!directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File target : files) {
            if (target.isDirectory()) {
                purgeDirectory(target);
            }
            target.delete();
        }
    }

}
