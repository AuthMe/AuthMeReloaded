package fr.xephi.authme.settings;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;

/**
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class Spawn extends CustomConfiguration {

    private static Spawn spawn;
    private static String[] spawnPriority;

    private Spawn() {
        super(new File(Settings.PLUGIN_FOLDER, "spawn.yml"));
        reload();
    }

    public static void reload() {
        getInstance().load();
        getInstance().save();
        spawnPriority = Settings.spawnPriority.split(",");
    }

    /**
     * Method getInstance.
     *
     * @return Spawn
     */
    public static Spawn getInstance() {
        if (spawn == null) {
            spawn = new Spawn();
        }
        return spawn;
    }

    public boolean setSpawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        set("spawn.world", location.getWorld().getName());
        set("spawn.x", location.getX());
        set("spawn.y", location.getY());
        set("spawn.z", location.getZ());
        set("spawn.yaw", location.getYaw());
        set("spawn.pitch", location.getPitch());
        save();
        return true;
    }

    public boolean setFirstSpawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        set("firstspawn.world", location.getWorld().getName());
        set("firstspawn.x", location.getX());
        set("firstspawn.y", location.getY());
        set("firstspawn.z", location.getZ());
        set("firstspawn.yaw", location.getYaw());
        set("firstspawn.pitch", location.getPitch());
        save();
        return true;
    }

    public Location getSpawn() {
        try {
            String worldName;
            World world;
            if (StringUtils.isEmpty(worldName = getString("spawn.world")) ||
                (world = Bukkit.getWorld(worldName)) == null) {
                return null;
            }
            return new Location(world, getDouble("spawn.x"), getDouble("spawn.y"), getDouble("spawn.z"),
                Float.parseFloat(getString("spawn.yaw")), Float.parseFloat(getString("spawn.pitch")));
        } catch (NumberFormatException e) {
            ConsoleLogger.writeStackTrace(e);
            return null;
        }
    }

    public Location getFirstSpawn() {
        try {
            String worldName;
            World world;
            if (StringUtils.isEmpty(worldName = getString("firstspawn.world")) ||
                (world = Bukkit.getWorld(worldName)) == null) {
                return null;
            }
            return new Location(world, getDouble("firstspawn.x"), getDouble("firstspawn.y"), getDouble("firstspawn.z"),
                Float.parseFloat(getString("firstspawn.yaw")), Float.parseFloat(getString("firstspawn.pitch")));
        } catch (NumberFormatException e) {
            ConsoleLogger.writeStackTrace(e);
            return null;
        }
    }

    // Return the spawn location of a player
    public Location getSpawnLocation(Player player) {
        AuthMe plugin = AuthMe.getInstance();
        World world;
        if (plugin == null || player == null || (world = player.getWorld()) == null) {
            return null;
        }

        Location spawnLoc = null;
        for (String priority : spawnPriority) {
            switch (priority.toLowerCase()) {
                case "default":
                    if (world.getSpawnLocation() != null) {
                        spawnLoc = world.getSpawnLocation();
                    }
                    break;
                case "multiverse":
                    if (Settings.multiverse && plugin.multiverse != null) {
                        MVWorldManager manager = plugin.multiverse.getMVWorldManager();
                        if (manager.isMVWorld(world)) {
                            spawnLoc = manager.getMVWorld(world).getSpawnLocation();
                        }
                    }
                    break;
                case "essentials":
                    spawnLoc = plugin.essentialsSpawn;
                    break;
                case "authme":
                    String playerNameLower = player.getName().toLowerCase();
                    if (PlayerCache.getInstance().isAuthenticated(playerNameLower)) {
                        spawnLoc = getSpawn();
                    } else if ((getFirstSpawn() != null) && (!player.hasPlayedBefore() ||
                        (!plugin.getDataSource().isAuthAvailable(playerNameLower)))) {
                        spawnLoc = getFirstSpawn();
                    } else {
                        spawnLoc = getSpawn();
                    }
                    break;
            }
            if (spawnLoc != null) {
                return spawnLoc;
            }
        }
        return world.getSpawnLocation(); // return default location
    }
}
