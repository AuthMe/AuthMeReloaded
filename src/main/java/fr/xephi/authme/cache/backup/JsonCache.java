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
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.Utils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;

public class JsonCache {

    private final Gson gson;
    private final File cacheDir;
    @Inject
    private AuthMe plugin;
    @Inject
    private PermissionsManager permissionsManager;
    @Inject
    private SpawnLoader spawnLoader;
    @Inject
    private BukkitService bukkitService;

    public JsonCache() {
        cacheDir = new File(plugin.getDataFolder(), "cache");
        if (!cacheDir.exists() && !cacheDir.isDirectory() && !cacheDir.mkdir()) {
            ConsoleLogger.showError("Failed to create cache directory.");
        }
        gson = new GsonBuilder()
            .registerTypeAdapter(LimboPlayer.class, new LimboPlayerSerializer())
            .registerTypeAdapter(LimboPlayer.class, new LimboPlayerDeserializer())
            .setPrettyPrinting()
            .create();
    }

    public LimboPlayer readCache(Player player) {
        String id = Utils.getUUIDorName(player);
        File file = new File(cacheDir, id + File.separator + "cache.json");
        if (!file.exists()) {
            return null;
        }

        try {
            String str = Files.toString(file, Charsets.UTF_8);
            return gson.fromJson(str, LimboPlayer.class);
        } catch (IOException e) {
            ConsoleLogger.writeStackTrace(e);
            return null;
        }
    }

    public void writeCache(Player player) {
        String id = Utils.getUUIDorName(player);
        String name = player.getName().toLowerCase();
        Location location = player.isDead() ? spawnLoader.getSpawnLocation(player) : player.getLocation();
        String group = permissionsManager.getPrimaryGroup(player);
        boolean operator = player.isOp();
        boolean canFly = player.getAllowFlight();
        float walkSpeed = player.getWalkSpeed();
        LimboPlayer limboPlayer = new LimboPlayer(name, location, operator, group, canFly, walkSpeed);
        try {
            File file = new File(cacheDir, id + File.separator + "cache.json");
            Files.write(gson.toJson(limboPlayer), file, Charsets.UTF_8);
        } catch (IOException e) {
            ConsoleLogger.logException("Failed to write " + player.getName() + " cache.", e);
        }
    }

    public void removeCache(Player player) {
        String id = Utils.getUUIDorName(player);
        File file = new File(cacheDir, id);
        if (file.exists()) {
            FileUtils.purgeDirectory(file);
            if (!file.delete()) {
                ConsoleLogger.showError("Failed to remove " + player.getName() + " cache.");
            }
        }
    }

    public boolean doesCacheExist(Player player) {
        String id = Utils.getUUIDorName(player);
        File file = new File(cacheDir, id + File.separator + "cache.json");
        return file.exists();
    }

    private class LimboPlayerDeserializer implements JsonDeserializer<LimboPlayer> {
        @Override
        public LimboPlayer deserialize(JsonElement jsonElement, Type type,
                                       JsonDeserializationContext context) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject == null) {
                return null;
            }

            Location loc = null;
            String group = "";
            boolean operator = false;
            boolean canFly = false;
            float walkSpeed = 0.2f;

            JsonElement e;
            if ((e = jsonObject.getAsJsonObject("location")) != null) {
                JsonObject obj = e.getAsJsonObject();
                World world = bukkitService.getWorld(obj.get("world").getAsString());
                if (world != null) {
                    double x = obj.get("x").getAsDouble();
                    double y = obj.get("y").getAsDouble();
                    double z = obj.get("z").getAsDouble();
                    float yaw = obj.get("yaw").getAsFloat();
                    float pitch = obj.get("pitch").getAsFloat();
                    loc = new Location(world, x, y, z, yaw, pitch);
                }
            }
            if ((e = jsonObject.get("group")) != null) {
                group = e.getAsString();
            }
            if ((e = jsonObject.get("operator")) != null) {
                operator = e.getAsBoolean();
            }
            if ((e = jsonObject.get("can-fly")) != null) {
                canFly = e.getAsBoolean();
            }
            if ((e = jsonObject.get("walk-speed")) != null) {
                walkSpeed = e.getAsFloat();
            }

            return new LimboPlayer("", loc, operator, group, canFly, walkSpeed);
        }
    }

    private class LimboPlayerSerializer implements JsonSerializer<LimboPlayer> {
        @Override
        public JsonElement serialize(LimboPlayer limboPlayer, Type type,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("group", limboPlayer.getGroup());

            Location loc = limboPlayer.getLoc();
            JsonObject obj2 = new JsonObject();
            obj2.addProperty("world", loc.getWorld().getName());
            obj2.addProperty("x", loc.getX());
            obj2.addProperty("y", loc.getY());
            obj2.addProperty("z", loc.getZ());
            obj2.addProperty("yaw", loc.getYaw());
            obj2.addProperty("pitch", loc.getPitch());
            obj.add("location", obj2);

            obj.addProperty("operator", limboPlayer.isOperator());
            obj.addProperty("can-fly", limboPlayer.isCanFly());
            obj.addProperty("walk-speed", limboPlayer.getWalkSpeed());
            return obj;
        }
    }


}
