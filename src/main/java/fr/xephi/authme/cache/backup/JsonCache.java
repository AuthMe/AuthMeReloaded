package fr.xephi.authme.cache.backup;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.*;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.Utils;
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

    /**
     * Method createCache.
     *
     * @param player     Player
     * @param playerData DataFileCache
     */
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

    /**
     * Method readCache.
     *
     * @param player Player
     *
     * @return DataFileCache
     */
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

    /**
     * Method removeCache.
     *
     * @param player Player
     */
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

    /**
     * Method doesCacheExist.
     *
     * @param player Player
     *
     * @return boolean
     */
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

    /**
     */
    private static class PlayerDataDeserializer implements JsonDeserializer<DataFileCache> {
        /**
         * Method deserialize.
         *
         * @param jsonElement                JsonElement
         * @param type                       Type
         * @param jsonDeserializationContext JsonDeserializationContext
         *
         * @return DataFileCache * @throws JsonParseException * @see com.google.gson.JsonDeserializer#deserialize(JsonElement, Type, JsonDeserializationContext)
         */
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

            return new DataFileCache(group, operator, flying);
        }
    }

    /**
     */
    private class PlayerDataSerializer implements JsonSerializer<DataFileCache> {
        /**
         * Method serialize.
         *
         * @param dataFileCache            DataFileCache
         * @param type                     Type
         * @param jsonSerializationContext JsonSerializationContext
         *
         * @return JsonElement
         */
        @Override
        public JsonElement serialize(DataFileCache dataFileCache, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("group", dataFileCache.getGroup());
            jsonObject.addProperty("operator", dataFileCache.getOperator());
            jsonObject.addProperty("flying", dataFileCache.isFlying());

            return jsonObject;
        }
    }

}
