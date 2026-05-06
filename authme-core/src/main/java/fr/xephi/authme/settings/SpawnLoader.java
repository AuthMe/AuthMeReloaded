package fr.xephi.authme.settings;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.platform.TeleportAdapter;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Manager for spawn points. It loads spawn definitions from AuthMe and third-party plugins
 * and is responsible for returning the correct spawn point as per the settings.
 * <p>
 * The spawn priority setting defines from which sources and in which order the spawn point
 * should be taken from. In AuthMe, we can distinguish between the regular spawn and a "first spawn",
 * to which players will be teleported who have joined for the first time.
 */
public class SpawnLoader implements Reloadable {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(SpawnLoader.class);
    
    private final File authMeConfigurationFile;
    private final Settings settings;
    private final PluginHookService pluginHookService;
    private final TeleportAdapter teleportAdapter;
    private FileConfiguration authMeConfiguration;
    private String[] spawnPriority;
    private Location essentialsSpawn;
    private Location cmiSpawn;

    /**
     * Constructor.
     *
     * @param pluginFolder The AuthMe data folder
     * @param settings     The setting instance
     * @param pluginHookService  The plugin hooks instance
     * @param teleportAdapter The platform-specific teleport adapter
     */
    @Inject
    SpawnLoader(@DataFolder File pluginFolder, Settings settings, PluginHookService pluginHookService,
                TeleportAdapter teleportAdapter) {
        File spawnFile = new File(pluginFolder, "spawn.yml");
        FileUtils.copyFileFromResource(spawnFile, "spawn.yml");
        this.authMeConfigurationFile = spawnFile;
        this.settings = settings;
        this.pluginHookService = pluginHookService;
        this.teleportAdapter = teleportAdapter;
        reload();
    }

    /**
     * (Re)loads the spawn file and relevant settings.
     */
    @Override
    public void reload() {
        spawnPriority = settings.getProperty(RestrictionSettings.SPAWN_PRIORITY).split(",");
        authMeConfiguration = YamlConfiguration.loadConfiguration(authMeConfigurationFile);
        loadEssentialsSpawn();
    }

    /**
     * Return the AuthMe spawn location.
     *
     * @return The location of the regular AuthMe spawn point
     */
    public Location getSpawn() {
        return getLocationFromConfiguration(authMeConfiguration, "spawn");
    }

    /**
     * Set the AuthMe spawn point.
     *
     * @param location The location to use
     *
     * @return True upon success, false otherwise
     */
    public boolean setSpawn(Location location) {
        return setLocation("spawn", location);
    }

    /**
     * Return the AuthMe first spawn location.
     *
     * @return The location of the AuthMe spawn point for first timers
     */
    public Location getFirstSpawn() {
        return getLocationFromConfiguration(authMeConfiguration, "firstspawn");
    }

    /**
     * Set the AuthMe first spawn location.
     *
     * @param location The location to use
     *
     * @return True upon success, false otherwise
     */
    public boolean setFirstSpawn(Location location) {
        return setLocation("firstspawn", location);
    }

    /**
     * Load the spawn point defined in EssentialsSpawn.
     */
    public void loadEssentialsSpawn() {
        // EssentialsSpawn cannot run without Essentials, so it's fine to get the Essentials data folder
        File essentialsFolder = pluginHookService.getEssentialsDataFolder();
        if (essentialsFolder == null) {
            return;
        }

        File essentialsSpawnFile = new File(essentialsFolder, "spawn.yml");
        if (essentialsSpawnFile.exists()) {
            essentialsSpawn = getLocationFromConfiguration(
                YamlConfiguration.loadConfiguration(essentialsSpawnFile), "spawns.default");
        } else {
            essentialsSpawn = null;
            logger.info("Essentials spawn file not found: '" + essentialsSpawnFile.getAbsolutePath() + "'");
        }
    }

    /**
     * Unset the spawn point defined in EssentialsSpawn.
     */
    public void unloadEssentialsSpawn() {
        essentialsSpawn = null;
    }

    /**
     * Load the spawn point defined in CMI.
     */
    public void loadCmiSpawn() {
        File cmiFolder = pluginHookService.getCmiDataFolder();
        if (cmiFolder == null) {
            return;
        }

        File cmiConfig = new File(cmiFolder, "config.yml");
        if (cmiConfig.exists()) {
            cmiSpawn = getLocationFromCmiConfiguration(YamlConfiguration.loadConfiguration(cmiConfig));
        } else {
            cmiSpawn = null;
            logger.info("CMI config file not found: '" + cmiConfig.getAbsolutePath() + "'");
        }
    }

    /**
     * Unset the spawn point defined in CMI.
     */
    public void unloadCmiSpawn() {
        cmiSpawn = null;
    }

    /**
     * Return the spawn location for the given player. The source of the spawn location varies
     * depending on the spawn priority setting.
     *
     * @param player The player to retrieve the spawn point for
     *
     * @return The spawn location, or the default spawn location upon failure
     *
     * @see RestrictionSettings#SPAWN_PRIORITY
     */
    public Location getSpawnLocation(Player player) {
        if (player == null) {
            return null;
        }
        return getSpawnLocation(player.getWorld());
    }

    /**
     * Return the spawn location for the given world. The source of the spawn location varies
     * depending on the spawn priority setting.
     *
     * @param world The world to retrieve the spawn point for
     *
     * @return The spawn location, or the default spawn location upon failure
     */
    public Location getSpawnLocation(World world) {
        if (world == null) {
            return null;
        }

        Location spawnLoc = null;
        for (String priority : spawnPriority) {
            switch (priority.toLowerCase(Locale.ROOT).trim()) {
                case "server":
                    spawnLoc = getServerSpawnLocation(world);
                    break;
                case "default":
                    if (world.getSpawnLocation() != null) {
                        if (!isValidSpawnPoint(world.getSpawnLocation())) {
                            for (World spawnWorld : Bukkit.getWorlds()) {
                                if (isValidSpawnPoint(spawnWorld.getSpawnLocation())) {
                                    world = spawnWorld;
                                    break;
                                }
                            }
                            logger.warning("Seems like AuthMe is unable to find a proper spawn location. "
                                + "Set a location with the command '/authme setspawn'");
                        }
                        spawnLoc = world.getSpawnLocation();
                    }
                    break;
                case "multiverse":
                    if (settings.getProperty(HooksSettings.MULTIVERSE)) {
                        spawnLoc = pluginHookService.getMultiverseSpawn(world);
                    }
                    break;
                case "essentials":
                    spawnLoc = essentialsSpawn;
                    break;
                case "cmi":
                    spawnLoc = cmiSpawn;
                    break;
                case "authme":
                    spawnLoc = getSpawn();
                    break;
                default:
                    // ignore
            }
            if (spawnLoc != null) {
                logger.debug("Spawn location determined as `{0}` for world `{1}`", spawnLoc, world.getName());
                return spawnLoc;
            }
        }
        logger.debug("Fall back to default world spawn location. World: `{0}`", world.getName());

        return world.getSpawnLocation(); // return default location
    }

    /**
     * Returns a spawn location based on the world's native spawn, applying the {@code spawnRadius} gamerule
     * when its value is positive. This mimics vanilla behavior where players are placed at a random
     * position within the configured radius around the world spawn.
     *
     * @param world The world to retrieve the spawn point from
     * @return A location within spawnRadius of the world spawn, or the exact world spawn if radius is 0
     */
    private Location getServerSpawnLocation(World world) {
        Location worldSpawn = world.getSpawnLocation();
        Integer radius = world.getGameRuleValue(GameRule.SPAWN_RADIUS);
        if (radius == null || radius <= 0) {
            return worldSpawn;
        }
        int dx = (int) (Math.random() * (radius * 2 + 1)) - radius;
        int dz = (int) (Math.random() * (radius * 2 + 1)) - radius;
        int x = (int) worldSpawn.getX() + dx;
        int z = (int) worldSpawn.getZ() + dz;
        Integer y = getSafeSpawnY(world, x, z, worldSpawn.getBlockY());
        if (y == null) {
            return worldSpawn;
        }
        double spawnX = x + 0.5;
        double spawnZ = z + 0.5;
        float yaw = (float) Math.toDegrees(Math.atan2(-(worldSpawn.getX() - spawnX), worldSpawn.getZ() - spawnZ));
        return new Location(world, spawnX, y, spawnZ, yaw, 0.0f);
    }

    /**
     * Returns a safe Y coordinate for spawning at (x, z), searching near the world spawn's Y.
     * Checks if the foot and head blocks are passable at the base Y; if the foot is already
     * passable the search goes downward (looking for a floor), otherwise upward (looking for a gap).
     * A margin of 10 blocks is applied in each direction. Returns {@code null} if no safe spot
     * is found, in which case the caller should fall back to the exact world spawn location.
     */
    private Integer getSafeSpawnY(World world, int x, int z, int baseY) {
        if (isPassable(world, x, baseY, z) && isPassable(world, x, baseY + 1, z)) {
            return baseY;
        }
        int margin = 10;
        if (world.getBlockAt(x, baseY, z).isPassable()) {
            for (int dy = 1; dy <= margin; dy++) {
                int y = baseY - dy;
                if (isPassable(world, x, y, z) && isPassable(world, x, y + 1, z)) {
                    return y;
                }
            }
        } else {
            for (int dy = 1; dy <= margin; dy++) {
                int y = baseY + dy;
                if (isPassable(world, x, y, z) && isPassable(world, x, y + 1, z)) {
                    return y;
                }
            }
        }
        return null;
    }

    private boolean isPassable(World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z).isPassable();
    }

    /**
     * Checks if a given location is a valid spawn point [!= null && != (0,0,0)].
     *
     * @param location The location to check
     *
     * @return True upon success, false otherwise
     */
    private boolean isValidSpawnPoint(Location location) {
        if (location == null) {
            return false;
        }
        if (location.getX() == 0 && location.getY() == 0 && location.getZ() == 0) {
            return false;
        }
        return true;
    }

    /**
     * Save the location under the given prefix.
     *
     * @param prefix   The prefix to save the spawn under
     * @param location The location to persist
     *
     * @return True upon success, false otherwise
     */
    private boolean setLocation(String prefix, Location location) {
        if (location != null && location.getWorld() != null) {
            authMeConfiguration.set(prefix + ".world", location.getWorld().getName());
            authMeConfiguration.set(prefix + ".x", location.getX());
            authMeConfiguration.set(prefix + ".y", location.getY());
            authMeConfiguration.set(prefix + ".z", location.getZ());
            authMeConfiguration.set(prefix + ".yaw", location.getYaw());
            authMeConfiguration.set(prefix + ".pitch", location.getPitch());
            return saveAuthMeConfig();
        }
        return false;
    }

    private boolean saveAuthMeConfig() {
        try {
            authMeConfiguration.save(authMeConfigurationFile);
            return true;
        } catch (IOException e) {
            logger.logException("Could not save spawn config (" + authMeConfigurationFile + ")", e);
        }
        return false;
    }

    /**
     * Return player's location if player is alive, or player's spawn location if dead.
     *
     * @param player player to retrieve
     *
     * @return location of the given player if alive, spawn location if dead.
     */
    public Location getPlayerLocationOrSpawn(Player player) {
        if (player.getHealth() <= 0.0) {
            return getPlayerRespawnLocationOrSpawn(player);
        }
        return player.getLocation();
    }

    /**
     * Return the player's effective respawn location if available, or the configured spawn location as fallback.
     * <p>
     * We need this extra check for players who die and disconnect before actually respawning: in that case we want
     * to preserve the server's real respawn target instead of collapsing everything back to AuthMe's configured spawn.
     * The version-specific API call lives in {@link TeleportAdapter}: legacy builds use bed spawn, newer builds can
     * use {@code Player#getRespawnLocation()} directly.
     *
     * @param player player to retrieve
     * @return the player's respawn location if available, otherwise the configured spawn
     */
    public Location getPlayerRespawnLocationOrSpawn(Player player) {
        Location respawnLocation = teleportAdapter.getPlayerRespawnLocation(player);
        if (respawnLocation != null && respawnLocation.getWorld() != null) {
            return respawnLocation;
        }
        return getSpawnLocation(player);
    }

    /**
     * Build a {@link Location} object from the given path in the file configuration.
     *
     * @param configuration The file configuration to read from
     * @param pathPrefix    The path to get the spawn point from
     *
     * @return Location corresponding to the values in the path
     */
    private static Location getLocationFromConfiguration(FileConfiguration configuration, String pathPrefix) {
        if (containsAllSpawnFields(configuration, pathPrefix)) {
            String prefix = pathPrefix + ".";
            String worldName = configuration.getString(prefix + "world");
            World world = Bukkit.getWorld(worldName);
            if (!StringUtils.isBlank(worldName) && world != null) {
                return new Location(world, configuration.getDouble(prefix + "x"),
                    configuration.getDouble(prefix + "y"), configuration.getDouble(prefix + "z"),
                    getFloat(configuration, prefix + "yaw"), getFloat(configuration, prefix + "pitch"));
            }
        }
        return null;
    }

    /**
     * Build a {@link Location} object based on the CMI configuration.
     *
     * @param configuration The CMI file configuration to read from
     *
     * @return Location corresponding to the values in the path
     */
    private static Location getLocationFromCmiConfiguration(FileConfiguration configuration) {
        final String pathPrefix = "Spawn.Main";
        if (isLocationCompleteInCmiConfig(configuration, pathPrefix)) {
            String prefix = pathPrefix + ".";
            String worldName = configuration.getString(prefix + "World");
            World world = Bukkit.getWorld(worldName);
            if (!StringUtils.isBlank(worldName) && world != null) {
                return new Location(world, configuration.getDouble(prefix + "X"),
                    configuration.getDouble(prefix + "Y"), configuration.getDouble(prefix + "Z"),
                    getFloat(configuration, prefix + "Yaw"), getFloat(configuration, prefix + "Pitch"));
            }
        }
        return null;
    }

    /**
     * Return whether the file configuration contains all fields necessary to define a spawn
     * under the given path.
     *
     * @param configuration The file configuration to use
     * @param pathPrefix    The path to verify
     *
     * @return True if all spawn fields are present, false otherwise
     */
    private static boolean containsAllSpawnFields(FileConfiguration configuration, String pathPrefix) {
        String[] fields = {"world", "x", "y", "z", "yaw", "pitch"};
        for (String field : fields) {
            if (!configuration.contains(pathPrefix + "." + field)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return whether the CMI file configuration contains all spawn fields under the given path.
     *
     * @param cmiConfiguration The file configuration from CMI
     * @param pathPrefix       The path to verify
     *
     * @return True if all spawn fields are present, false otherwise
     */
    private static boolean isLocationCompleteInCmiConfig(FileConfiguration cmiConfiguration, String pathPrefix) {
        String[] fields = {"World", "X", "Y", "Z", "Yaw", "Pitch"};
        for (String field : fields) {
            if (!cmiConfiguration.contains(pathPrefix + "." + field)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retrieve a property as a float from the given file configuration.
     *
     * @param configuration The file configuration to use
     * @param path          The path of the property to retrieve
     *
     * @return The float
     */
    private static float getFloat(FileConfiguration configuration, String path) {
        Object value = configuration.get(path);
        // This behavior is consistent with FileConfiguration#getDouble
        return (value instanceof Number) ? ((Number) value).floatValue() : 0;
    }

}
