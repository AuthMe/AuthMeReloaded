package fr.xephi.authme;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.api.API;
import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.DataSourceProvider;
import fr.xephi.authme.initialization.OnShutdownPlayerSaver;
import fr.xephi.authme.initialization.OnStartupTasks;
import fr.xephi.authme.initialization.SettingsProvider;
import fr.xephi.authme.initialization.TaskCloser;
import fr.xephi.authme.listener.BlockListener;
import fr.xephi.authme.listener.EntityListener;
import fr.xephi.authme.listener.PlayerListener;
import fr.xephi.authme.listener.PlayerListener16;
import fr.xephi.authme.listener.PlayerListener18;
import fr.xephi.authme.listener.PlayerListener19;
import fr.xephi.authme.listener.ServerListener;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PermissionsSystemType;
import fr.xephi.authme.security.crypts.SHA256;
import fr.xephi.authme.service.BackupService;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.GeoIpService;
import fr.xephi.authme.service.MigrationService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.task.CleanupTask;
import fr.xephi.authme.task.purge.PurgeService;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_MINUTE;
import static fr.xephi.authme.util.Utils.isClassLoaded;

/**
 * The AuthMe main class.
 */
public class AuthMe extends JavaPlugin {

    // Constants
    private static final String PLUGIN_NAME = "AuthMeReloaded";
    private static final String LOG_FILENAME = "authme.log";
    private static final int CLEANUP_INTERVAL = 5 * TICKS_PER_MINUTE;

    // Default version and build number values;
    private static String pluginVersion = "N/D";
    private static String pluginBuildNumber = "Unknown";

    // Private instances
    private CommandHandler commandHandler;
    private PermissionsManager permsMan;
    private Settings settings;
    private DataSource database;
    private BukkitService bukkitService;
    private Injector injector;
    private GeoIpService geoIpService;
    private PlayerCache playerCache;

    /**
     * Constructor.
     */
    public AuthMe() {
    }

    /*
     * Constructor for unit testing.
     */
    @VisibleForTesting
    @SuppressWarnings("deprecation") // the super constructor is deprecated to mark it for unit testing only
    protected AuthMe(final PluginLoader loader, final Server server, final PluginDescriptionFile description,
                     final File dataFolder, final File file) {
        super(loader, server, description, dataFolder, file);
    }

    /**
     * Get the plugin's name.
     *
     * @return The plugin's name.
     */
    public static String getPluginName() {
        return PLUGIN_NAME;
    }

    /**
     * Get the plugin's version.
     *
     * @return The plugin's version.
     */
    public static String getPluginVersion() {
        return pluginVersion;
    }

    /**
     * Get the plugin's build number.
     *
     * @return The plugin's build number.
     */
    public static String getPluginBuildNumber() {
        return pluginBuildNumber;
    }

    /**
     * Method used to obtain the plugin's api instance
     *
     * @return The plugin's api instance
     */
    public static NewAPI getApi() {
        return NewAPI.getInstance();
    }

    /**
     * Method called when the server enables the plugin.
     */
    @Override
    public void onEnable() {
        // Load the plugin version data from the plugin description file
        loadPluginInfo();

        // Initialize the plugin
        try {
            initialize();
        } catch (Exception e) {
            ConsoleLogger.logException("Aborting initialization of AuthMe:", e);
            stopOrUnload();
            return;
        }

        // Show settings warnings
        showSettingsWarnings();

        // If server is using PermissionsBukkit, print a warning that some features may not be supported
        if (PermissionsSystemType.PERMISSIONS_BUKKIT.equals(permsMan.getPermissionSystem())) {
            ConsoleLogger.warning("Warning! This server uses PermissionsBukkit for permissions. Some permissions features may not be supported!");
        }

        // Do a backup on start
        new BackupService(this, settings).doBackup(BackupService.BackupCause.START);

        // Set up Metrics
        OnStartupTasks.sendMetrics(this, settings);

        // Sponsor messages
        ConsoleLogger.info("Development builds are available on our jenkins, thanks to f14stelt.");
        ConsoleLogger.info("Do you want a good game server? Look at our sponsor GameHosting.it leader in Italy as Game Server Provider!");

        // Successful message
        ConsoleLogger.info("AuthMe " + getPluginVersion() + " build n." + getPluginBuildNumber() + " correctly enabled!");

        // Purge on start if enabled
        PurgeService purgeService = injector.getSingleton(PurgeService.class);
        purgeService.runAutoPurge();

        // Schedule clean up task
        CleanupTask cleanupTask = injector.getSingleton(CleanupTask.class);
        cleanupTask.runTaskTimerAsynchronously(this, CLEANUP_INTERVAL, CLEANUP_INTERVAL);
    }

    /**
     * Load the version and build number of the plugin from the description file.
     */
    private void loadPluginInfo() {
        String versionRaw = this.getDescription().getVersion();
        int index = versionRaw.lastIndexOf("-");
        if (index != -1) {
            pluginVersion = versionRaw.substring(0, index);
            pluginBuildNumber = versionRaw.substring(index + 1);
            if (pluginBuildNumber.startsWith("b")) {
                pluginBuildNumber = pluginBuildNumber.substring(1);
            }
        }
    }

    /**
     * Initialize the plugin and all the services.
     *
     * @throws Exception if the initialization fails
     */
    private void initialize() throws Exception {
        // Set the Logger instance and log file path
        ConsoleLogger.setLogger(getLogger());
        ConsoleLogger.setLogFile(new File(getDataFolder(), LOG_FILENAME));

        // Create plugin folder
        getDataFolder().mkdir();

        // Create injector, provide elements from the Bukkit environment and register providers
        injector = new InjectorBuilder().addDefaultHandlers("fr.xephi.authme").create();
        injector.register(AuthMe.class, this);
        injector.register(Server.class, getServer());
        injector.register(PluginManager.class, getServer().getPluginManager());
        injector.register(BukkitScheduler.class, getServer().getScheduler());
        injector.provide(DataFolder.class, getDataFolder());
        injector.registerProvider(Settings.class, SettingsProvider.class);
        injector.registerProvider(DataSource.class, DataSourceProvider.class);

        // Get settings and set up logger
        settings = injector.getSingleton(Settings.class);
        ConsoleLogger.setLoggingOptions(settings);
        OnStartupTasks.setupConsoleFilter(settings, getLogger());

        // Set all service fields on the AuthMe class
        instantiateServices(injector);

        // Convert deprecated PLAINTEXT hash entries
        MigrationService.changePlainTextToSha256(settings, database, new SHA256());

        // TODO: does this still make sense? -sgdc3
        // If the server is empty (fresh start) just set all the players as unlogged
        if (bukkitService.getOnlinePlayers().size() == 0) {
            database.purgeLogged();
        }

        // Register event listeners
        registerEventListeners(injector);

        // Start Email recall task if needed
        OnStartupTasks onStartupTasks = injector.newInstance(OnStartupTasks.class);
        onStartupTasks.scheduleRecallEmailTask();
    }

    /**
     * Instantiates all services.
     *
     * @param injector the injector
     */
    protected void instantiateServices(Injector injector) {
        // PlayerCache is still injected statically sometimes
        playerCache = PlayerCache.getInstance();
        injector.register(PlayerCache.class, playerCache);

        database = injector.getSingleton(DataSource.class);
        permsMan = injector.getSingleton(PermissionsManager.class);
        bukkitService = injector.getSingleton(BukkitService.class);
        commandHandler = injector.getSingleton(CommandHandler.class);
        geoIpService = injector.getSingleton(GeoIpService.class);

        // Trigger construction of API classes; they will keep track of the singleton
        injector.getSingleton(NewAPI.class);
        injector.getSingleton(API.class);
    }

    /**
     * Show the settings warnings, for various risky settings.
     */
    private void showSettingsWarnings() {
        // Force single session disabled
        if (!settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)) {
            ConsoleLogger.warning("WARNING!!! By disabling ForceSingleSession, your server protection is inadequate!");
        }

        // Session timeout disabled
        if (settings.getProperty(PluginSettings.SESSIONS_TIMEOUT) == 0
            && settings.getProperty(PluginSettings.SESSIONS_ENABLED)) {
            ConsoleLogger.warning("WARNING!!! You set session timeout to 0, this may cause security issues!");
        }
    }

    /**
     * Registers all event listeners.
     *
     * @param injector the injector
     */
    protected void registerEventListeners(Injector injector) {
        // Get the plugin manager instance
        PluginManager pluginManager = getServer().getPluginManager();

        // Register event listeners
        pluginManager.registerEvents(injector.getSingleton(PlayerListener.class), this);
        pluginManager.registerEvents(injector.getSingleton(BlockListener.class), this);
        pluginManager.registerEvents(injector.getSingleton(EntityListener.class), this);
        pluginManager.registerEvents(injector.getSingleton(ServerListener.class), this);

        // Try to register 1.6 player listeners
        if (isClassLoaded("org.bukkit.event.player.PlayerEditBookEvent")) {
            pluginManager.registerEvents(injector.getSingleton(PlayerListener16.class), this);
        }

        // Try to register 1.8 player listeners
        if (isClassLoaded("org.bukkit.event.player.PlayerInteractAtEntityEvent")) {
            pluginManager.registerEvents(injector.getSingleton(PlayerListener18.class), this);
        }

        // Try to register 1.9 player listeners
        if (isClassLoaded("org.bukkit.event.player.PlayerSwapHandItemsEvent")) {
            pluginManager.registerEvents(injector.getSingleton(PlayerListener19.class), this);
        }
    }

    /**
     * Stops the server or disables the plugin, as defined in the configuration.
     */
    public void stopOrUnload() {
        if (settings == null || settings.getProperty(SecuritySettings.STOP_SERVER_ON_PROBLEM)) {
            ConsoleLogger.warning("THE SERVER IS GOING TO SHUT DOWN AS DEFINED IN THE CONFIGURATION!");
            setEnabled(false);
            getServer().shutdown();
        } else {
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        // onDisable is also called when we prematurely abort, so any field may be null
        OnShutdownPlayerSaver onShutdownPlayerSaver = injector == null
            ? null
            : injector.createIfHasDependencies(OnShutdownPlayerSaver.class);
        if (onShutdownPlayerSaver != null) {
            onShutdownPlayerSaver.saveAllPlayers();
        }

        // Do backup on stop if enabled
        if (settings != null) {
            new BackupService(this, settings).doBackup(BackupService.BackupCause.STOP);
        }

        // Wait for tasks and close data source
        new TaskCloser(this, database, bukkitService).run();

        // Disabled correctly
        ConsoleLogger.info("AuthMe " + this.getDescription().getVersion() + " disabled!");
        ConsoleLogger.close();
    }

    public String replaceAllInfo(String message, Player player) {
        String playersOnline = Integer.toString(bukkitService.getOnlinePlayers().size());
        String ipAddress = PlayerUtils.getPlayerIp(player);
        Server server = getServer();
        return message
            .replace("&", "\u00a7")
            .replace("{PLAYER}", player.getName())
            .replace("{ONLINE}", playersOnline)
            .replace("{MAXPLAYERS}", Integer.toString(server.getMaxPlayers()))
            .replace("{IP}", ipAddress)
            .replace("{LOGINS}", Integer.toString(playerCache.getLogged()))
            .replace("{WORLD}", player.getWorld().getName())
            .replace("{SERVER}", server.getServerName())
            .replace("{VERSION}", server.getBukkitVersion())
            // TODO: We should cache info like this, maybe with a class that extends Player?
            .replace("{COUNTRY}", geoIpService.getCountryName(ipAddress));
    }

    /**
     * Handle Bukkit commands.
     *
     * @param sender       The command sender (Bukkit).
     * @param cmd          The command (Bukkit).
     * @param commandLabel The command label (Bukkit).
     * @param args         The command arguments (Bukkit).
     *
     * @return True if the command was executed, false otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
                             String commandLabel, String[] args) {
        // Make sure the command handler has been initialized
        if (commandHandler == null) {
            getLogger().severe("AuthMe command handler is not available");
            return false;
        }

        // Handle the command
        return commandHandler.processCommand(sender, commandLabel, args);
    }
}
