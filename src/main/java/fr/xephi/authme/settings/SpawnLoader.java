package fr.xephi.authme.settings;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

/**
 * Manager for spawn points. It loads spawn definitions from AuthMe and third-party plugins
 * and is responsible for returning the correct spawn point as per the settings.
 * <p>
 * The spawn priority setting defines from which sources and in which order the spawn point
 * should be taken from. In AuthMe, we can distinguish between the regular spawn and a "first spawn",
 * to which players will be teleported who have joined for the first time.
 */
public class SpawnLoader implements Reloadable {

    private final File authMeConfigurationFile;
    private final Settings settings;
    private final PluginHookService pluginHookService;
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
     */
    @Inject
    SpawnLoader(@DataFolder File pluginFolder, Settings settings, PluginHookService pluginHookService) {
        File spawnFile = new File(pluginFolder, "spawn.yml");
        FileUtils.copyFileFromResource(spawnFile, "spawn.yml");
        this.authMeConfigurationFile = spawnFile;
        this.settings = settings;
        this.pluginHookService = pluginHookService;
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
            ConsoleLogger.info("Essentials spawn file not found: '" + essentialsSpawnFile.getAbsolutePath() + "'");
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
            ConsoleLogger.info("CMI config file not found: '" + cmiConfig.getAbsolutePath() + "'");
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
        if (player == null || player.getWorld() == null) {
            return null;
        }

        World world = player.getWorld();
        Location spawnLoc = null;
        for (String priority : spawnPriority) {
            switch (priority.toLowerCase().trim()) {
                case "default":
                    if (world.getSpawnLocation() != null) {
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
                ConsoleLogger.debug("Spawn location determined as `{0}` for world `{1}`", spawnLoc, world.getName());
                return spawnLoc;
            }
        }
        ConsoleLogger.debug("Fall back to default world spawn location. World: `{0}`", world.getName());
        return world.getSpawnLocation(); // return default location
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
            ConsoleLogger.logException("Could not save spawn config (" + authMeConfigurationFile + ")", e);
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
        if (player.isOnline() && player.isDead()) {
            return getSpawnLocation(player);
        }
        return player.getLocation();
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
            if (!StringUtils.isEmpty(worldName) && world != null) {
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
            if (!StringUtils.isEmpty(worldName) && world != null) {
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
