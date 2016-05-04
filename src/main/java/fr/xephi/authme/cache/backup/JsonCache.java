package fr.xephi.authme.cache.backup;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;

public class JsonCache {

    private final Gson gson;
    private final File cacheDir;

    public JsonCache() {
        cacheDir = new File(AuthMe.getInstance().getDataFolder(), "cache");
        if (!cacheDir.exists() && !cacheDir.isDirectory() && !cacheDir.mkdir()) {
            ConsoleLogger.showError("Failed to create cache directory.");
        }
        gson = new GsonBuilder()
            .registerTypeAdapter(PlayerData.class, new PlayerDataSerializer())
            .registerTypeAdapter(PlayerData.class, new PlayerDataDeserializer())
            .setPrettyPrinting()
            .create();
    }

    public PlayerData readCache(Player player) {
        String name = player.getName().toLowerCase();
        File file = new File(cacheDir, name + File.separator + "cache.json");
        if (!file.exists()) {
            return null;
        }

        try {
            String str = Files.toString(file, Charsets.UTF_8);
            return gson.fromJson(str, PlayerData.class);
        } catch (IOException e) {
            ConsoleLogger.writeStackTrace(e);
            return null;
        }
    }

    public void removeCache(Player player) {
        String name = player.getName().toLowerCase();
        File file = new File(cacheDir, name);
        if (file.exists()) {
            purgeDirectory(file);
            if (!file.delete()) {
                ConsoleLogger.showError("Failed to remove" + player.getName() + "cache.");
            }
        }
    }

    public boolean doesCacheExist(Player player) {
        String name = player.getName().toLowerCase();
        File file = new File(cacheDir, name + File.separator + "cache.json");
        return file.exists();
    }

    private class PlayerDataDeserializer implements JsonDeserializer<PlayerData> {
        @Override
        public PlayerData deserialize(JsonElement jsonElement, Type type,
                                      JsonDeserializationContext context) {
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

            return new PlayerData(group, operator, fly);
        }
    }

    private class PlayerDataSerializer implements JsonSerializer<PlayerData> {
        @Override
        public JsonElement serialize(PlayerData playerData, Type type,
                                     JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("group", playerData.getGroup());
            jsonObject.addProperty("operator", playerData.getOperator());
            jsonObject.addProperty("fly", playerData.isFlyEnabled());
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
