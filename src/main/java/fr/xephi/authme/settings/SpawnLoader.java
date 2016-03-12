package fr.xephi.authme.settings;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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
public class SpawnLoader {

    private final File authMeConfigurationFile;
    private final PluginHooks pluginHooks;
    private FileConfiguration authMeConfiguration;
    private String[] spawnPriority;
    private Location essentialsSpawn;

    /**
     * Constructor.
     *
     * @param pluginFolder The AuthMe data folder
     * @param settings The setting instance
     * @param pluginHooks The plugin hooks instance
     */
    public SpawnLoader(File pluginFolder, NewSetting settings, PluginHooks pluginHooks) {
        File spawnFile = new File(pluginFolder, "spawn.yml");
        // TODO ljacqu 20160312: Check if resource could be copied and handle the case if not
        FileUtils.copyFileFromResource(spawnFile, "spawn.yml");
        this.authMeConfigurationFile = new File(pluginFolder, "spawn.yml");
        this.pluginHooks = pluginHooks;
        initialize(settings);
    }

    /**
     * Retrieve the relevant settings and load the AuthMe spawn.yml file.
     *
     * @param settings The settings instance
     */
    public void initialize(NewSetting settings) {
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
        File essentialsFolder = pluginHooks.getEssentialsDataFolder();
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
     * Return the spawn location for the given player. The source of the spawn location varies
     * depending on the spawn priority setting.
     *
     * @param player The player to retrieve the spawn point for
     * @return The spawn location, or the default spawn location upon failure
     * @see RestrictionSettings#SPAWN_PRIORITY
     */
    public Location getSpawnLocation(Player player) {
        AuthMe plugin = AuthMe.getInstance();
        if (plugin == null || player == null || player.getWorld() == null) {
            return null;
        }

        World world = player.getWorld();
        Location spawnLoc = null;
        // TODO ljacqu 20160312: We should trim() the entries
        for (String priority : spawnPriority) {
            switch (priority.toLowerCase()) {
                case "default":
                    if (world.getSpawnLocation() != null) {
                        spawnLoc = world.getSpawnLocation();
                    }
                    break;
                case "multiverse":
                    if (Settings.multiverse) {
                        spawnLoc = pluginHooks.getMultiverseSpawn(world);
                    }
                    break;
                case "essentials":
                    spawnLoc = essentialsSpawn;
                    break;
                case "authme":
                    String playerNameLower = player.getName().toLowerCase();
                    if (PlayerCache.getInstance().isAuthenticated(playerNameLower)) {
                        spawnLoc = getSpawn();
                    } else if (getFirstSpawn() != null && (!player.hasPlayedBefore() ||
                        !plugin.getDataSource().isAuthAvailable(playerNameLower))) {
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

    /**
     * Save the location under the given prefix.
     *
     * @param prefix The prefix to save the spawn under
     * @param location The location to persist
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
        // TODO ljacqu 20160312: Investigate whether this utility should be put in a Utils class
        try {
            authMeConfiguration.save(authMeConfigurationFile);
            return true;
        } catch (IOException e) {
            ConsoleLogger.logException("Could not save spawn config (" + authMeConfigurationFile + ")", e);
        }
        return false;
    }

    /**
     * Build a {@link Location} object from the given path in the file configuration.
     *
     * @param configuration The file configuration to read from
     * @param pathPrefix The path to get the spawn point from
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
     * Return whether the file configuration contains all fields necessary to define a spawn
     * under the given path.
     *
     * @param configuration The file configuration to use
     * @param pathPrefix The path to verify
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
     * Retrieve a property as a float from the given file configuration.
     *
     * @param configuration The file configuration to use
     * @param path The path of the property to retrieve
     * @return The float
     */
    private static float getFloat(FileConfiguration configuration, String path) {
        Object value = configuration.get(path);
        // This behavior is consistent with FileConfiguration#getDouble
        return (value instanceof Number) ? ((Number) value).floatValue() : 0;
    }
}
