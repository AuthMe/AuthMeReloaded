package fr.xephi.authme.data.backup;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * Class used to store player's data (OP, flying, speed, position) to disk.
 */
public class LimboPlayerStorage {

    private final Gson gson;
    private final File cacheDir;
    private PermissionsManager permissionsManager;
    private SpawnLoader spawnLoader;
    private BukkitService bukkitService;

    @Inject
    LimboPlayerStorage(@DataFolder File dataFolder, PermissionsManager permsMan,
                       SpawnLoader spawnLoader, BukkitService bukkitService) {
        this.permissionsManager = permsMan;
        this.spawnLoader = spawnLoader;
        this.bukkitService = bukkitService;

        cacheDir = new File(dataFolder, "playerdata");
        if (!cacheDir.exists() && !cacheDir.isDirectory() && !cacheDir.mkdir()) {
            ConsoleLogger.warning("Failed to create userdata directory.");
        }
        gson = new GsonBuilder()
            .registerTypeAdapter(LimboPlayer.class, new LimboPlayerSerializer())
            .registerTypeAdapter(LimboPlayer.class, new LimboPlayerDeserializer())
            .setPrettyPrinting()
            .create();
    }

    /**
     * Read and construct new PlayerData from existing player data.
     *
     * @param player player to read
     *
     * @return PlayerData object if the data is exist, null otherwise.
     */
    public LimboPlayer readData(Player player) {
        String id = PlayerUtils.getUUIDorName(player);
        File file = new File(cacheDir, id + File.separator + "data.json");
        if (!file.exists()) {
            return null;
        }

        try {
            String str = Files.toString(file, StandardCharsets.UTF_8);
            return gson.fromJson(str, LimboPlayer.class);
        } catch (IOException e) {
            ConsoleLogger.logException("Could not read player data on disk for '" + player.getName() + "'", e);
            return null;
        }
    }

    /**
     * Save player data (OP, flying, location, etc) to disk.
     *
     * @param player player to save
     */
    public void saveData(Player player) {
        String id = PlayerUtils.getUUIDorName(player);
        Location location = spawnLoader.getPlayerLocationOrSpawn(player);
        String group = "";
        if (permissionsManager.hasGroupSupport()) {
            group = permissionsManager.getPrimaryGroup(player);
        }
        boolean operator = player.isOp();
        boolean canFly = player.getAllowFlight();
        float walkSpeed = player.getWalkSpeed();
        float flySpeed = player.getFlySpeed();
        LimboPlayer limboPlayer = new LimboPlayer(location, operator, group, canFly, walkSpeed, flySpeed);
        try {
            File file = new File(cacheDir, id + File.separator + "data.json");
            Files.createParentDirs(file);
            Files.touch(file);
            Files.write(gson.toJson(limboPlayer), file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ConsoleLogger.logException("Failed to write " + player.getName() + " data.", e);
        }
    }

    /**
     * Remove player data, this will delete
     * "playerdata/&lt;uuid or name&gt;/" folder from disk.
     *
     * @param player player to remove
     */
    public void removeData(Player player) {
        String id = PlayerUtils.getUUIDorName(player);
        File file = new File(cacheDir, id);
        if (file.exists()) {
            FileUtils.purgeDirectory(file);
            if (!file.delete()) {
                ConsoleLogger.warning("Failed to remove " + player.getName() + " cache.");
            }
        }
    }

    /**
     * Use to check is player data is exist.
     *
     * @param player player to check
     *
     * @return true if data exist, false otherwise.
     */
    public boolean hasData(Player player) {
        String id = PlayerUtils.getUUIDorName(player);
        File file = new File(cacheDir, id + File.separator + "data.json");
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
            float flySpeed = 0.2f;

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
            if ((e = jsonObject.get("fly-speed")) != null) {
                flySpeed = e.getAsFloat();
            }

            return new LimboPlayer(loc, operator, group, canFly, walkSpeed, flySpeed);
        }
    }

    private class LimboPlayerSerializer implements JsonSerializer<LimboPlayer> {
        @Override
        public JsonElement serialize(LimboPlayer limboPlayer, Type type,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("group", limboPlayer.getGroup());

            Location loc = limboPlayer.getLocation();
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
            obj.addProperty("fly-speed", limboPlayer.getFlySpeed());
            return obj;
        }
    }


}
