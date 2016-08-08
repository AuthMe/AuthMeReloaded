package fr.xephi.authme;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.api.API;
import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.PlayerDataStorage;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.datasource.FlatFile;
import fr.xephi.authme.datasource.MySQL;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.MetricsManager;
import fr.xephi.authme.listener.BlockListener;
import fr.xephi.authme.listener.EntityListener;
import fr.xephi.authme.listener.PlayerListener;
import fr.xephi.authme.listener.PlayerListener16;
import fr.xephi.authme.listener.PlayerListener18;
import fr.xephi.authme.listener.ServerListener;
import fr.xephi.authme.output.ConsoleFilter;
import fr.xephi.authme.output.Log4JFilter;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PermissionsSystemType;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.crypts.SHA256;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SettingsMigrationService;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.settings.properties.SettingsFieldRetriever;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import fr.xephi.authme.task.CleanupTask;
import fr.xephi.authme.task.purge.PurgeService;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.GeoLiteAPI;
import fr.xephi.authme.util.MigrationService;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.ValidationService;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitWorker;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static fr.xephi.authme.settings.properties.EmailSettings.RECALL_PLAYERS;
import static fr.xephi.authme.util.BukkitService.TICKS_PER_MINUTE;

/**
 * The AuthMe main class.
 */
public class AuthMe extends JavaPlugin {

    // Costants
    private static final String PLUGIN_NAME = "AuthMeReloaded";
    private static final String LOG_FILENAME = "authme.log";
    private static final int SQLITE_MAX_SIZE = 4000;
    private final int CLEANUP_INTERVAL = 5 * TICKS_PER_MINUTE;

    // Default version and build number values;
    private static String pluginVersion = "N/D";
    private static String pluginBuildNumber = "Unknown";

    /*
     * Private instances
     */
    private Management management;
    private CommandHandler commandHandler;
    private PermissionsManager permsMan;
    private Settings settings;
    private Messages messages;
    private DataSource database;
    private PluginHooks pluginHooks;
    private SpawnLoader spawnLoader;
    private BukkitService bukkitService;
    private Injector injector;
    private GeoLiteAPI geoLiteApi;
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
     * Method called when the server enables the plugin.
     */
    @Override
    public void onEnable() {
        // Set the plugin instance and load plugin info from the plugin description.
        loadPluginInfo();

        // Set the Logger instance and log file path
        ConsoleLogger.setLogger(getLogger());
        ConsoleLogger.setLogFile(new File(getDataFolder(), LOG_FILENAME));

        // Load settings and custom configurations, if it fails, stop the server due to security reasons.
        settings = createSettings();
        if (settings == null) {
            ConsoleLogger.warning("Could not load the configuration file!"
                    + "The server is going to shutdown NOW!");
            setEnabled(false);
            getServer().shutdown();
            return;
        }

        // Apply settings to the logger
        ConsoleLogger.setLoggingOptions(settings);

        // Set console filter
        setupConsoleFilter();

        // Connect to the database and setup tables
        try {
            setupDatabase();
        } catch (Exception e) {
            ConsoleLogger.logException("Fatal error occurred during database connection! "
                + "Authme initialization aborted!", e);
            stopOrUnload();
            return;
        }
        // Convert deprecated PLAINTEXT hash entries
        MigrationService.changePlainTextToSha256(settings, database, new SHA256());

        // Injector initialization
        injector = new InjectorBuilder().addDefaultHandlers("fr.xephi.authme").create();

        // Register elements of the Bukkit / JavaPlugin environment
        injector.register(AuthMe.class, this);
        injector.register(Server.class, getServer());
        injector.register(PluginManager.class, getServer().getPluginManager());
        injector.register(BukkitScheduler.class, getServer().getScheduler());
        injector.provide(DataFolder.class, getDataFolder());

        // Register elements we instantiate manually
        injector.register(Settings.class, settings);
        injector.register(DataSource.class, database);

        instantiateServices(injector);

        // Set up Metrics
        MetricsManager.sendMetrics(this, settings);

        // Do a backup on start
        // TODO: maybe create a backup manager?
        new PerformBackup(this, settings).doBackup(PerformBackup.BackupCause.START);

        // Register event listeners
        registerEventListeners(injector);

        // Start Email recall task if needed
        scheduleRecallEmailTask();

        // Show settings warnings
        showSettingsWarnings();

        // If server is using PermissionsBukkit, print a warning that some features may not be supported
        if (PermissionsSystemType.PERMISSIONS_BUKKIT.equals(permsMan.getPermissionSystem())) {
            ConsoleLogger.warning("Warning! This server uses PermissionsBukkit for permissions. Some permissions features may not be supported!");
        }

        // Sponsor messages
        ConsoleLogger.info("Development builds are available on our jenkins, thanks to f14stelt.");
        ConsoleLogger.info("Do you want a good game server? Look at our sponsor GameHosting.it leader in Italy as Game Server Provider!");

        // Successful message
        ConsoleLogger.info("AuthMe " + getPluginVersion() + " build n°" + getPluginBuildNumber() + " correctly enabled!");

        // Purge on start if enabled
        PurgeService purgeService = injector.getSingleton(PurgeService.class);
        purgeService.runAutoPurge();

        // Schedule clean up task
        CleanupTask cleanupTask = injector.getSingleton(CleanupTask.class);
        cleanupTask.runTaskTimerAsynchronously(this, CLEANUP_INTERVAL, CLEANUP_INTERVAL);
    }

    // Get version and build number of the plugin
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

    protected void instantiateServices(Injector injector) {
        // PlayerCache is still injected statically sometimes
        playerCache = PlayerCache.getInstance();
        injector.register(PlayerCache.class, playerCache);

        messages = injector.getSingleton(Messages.class);
        permsMan = injector.getSingleton(PermissionsManager.class);
        bukkitService = injector.getSingleton(BukkitService.class);
        pluginHooks = injector.getSingleton(PluginHooks.class);
        spawnLoader = injector.getSingleton(SpawnLoader.class);
        commandHandler = injector.getSingleton(CommandHandler.class);
        management = injector.getSingleton(Management.class);
        geoLiteApi = injector.getSingleton(GeoLiteAPI.class);

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
     * Register all event listeners.
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
        try {
            Class.forName("org.bukkit.event.player.PlayerEditBookEvent");
            pluginManager.registerEvents(injector.getSingleton(PlayerListener16.class), this);
        } catch (ClassNotFoundException ignore) {
        }

        // Try to register 1.8 player listeners
        try {
            Class.forName("org.bukkit.event.player.PlayerInteractAtEntityEvent");
            pluginManager.registerEvents(injector.getSingleton(PlayerListener18.class), this);
        } catch (ClassNotFoundException ignore) {
        }
    }

    /**
     * Loads the plugin's settings.
     *
     * @return The settings instance, or null if it could not be constructed
     */
    private Settings createSettings() {
        File configFile = new File(getDataFolder(), "config.yml");
        PropertyMap properties = SettingsFieldRetriever.getAllPropertyFields();
        SettingsMigrationService migrationService = new SettingsMigrationService();
        return FileUtils.copyFileFromResource(configFile, "config.yml")
            ? new Settings(configFile, getDataFolder(), properties, migrationService)
            : null;
    }

    /**
     * Set up the console filter.
     */
    private void setupConsoleFilter() {
        if (!settings.getProperty(SecuritySettings.REMOVE_PASSWORD_FROM_CONSOLE)) {
            return;
        }
        // Try to set the log4j filter
        try {
            Class.forName("org.apache.logging.log4j.core.filter.AbstractFilter");
            setLog4JFilter();
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // log4j is not available
            ConsoleLogger.info("You're using Minecraft 1.6.x or older, Log4J support will be disabled");
            ConsoleFilter filter = new ConsoleFilter();
            getLogger().setFilter(filter);
            Bukkit.getLogger().setFilter(filter);
            Logger.getLogger("Minecraft").setFilter(filter);
        }
    }

    @Override
    public void onDisable() {
        // Save player data
        BukkitService bukkitService = injector.getIfAvailable(BukkitService.class);
        LimboCache limboCache = injector.getIfAvailable(LimboCache.class);
        ValidationService validationService = injector.getIfAvailable(ValidationService.class);

        if (bukkitService != null && limboCache != null && validationService != null) {
            Collection<? extends Player> players = bukkitService.getOnlinePlayers();
            for (Player player : players) {
                savePlayer(player, limboCache, validationService);
            }
        }

        // Do backup on stop if enabled
        if (settings != null) {
            new PerformBackup(this, settings).doBackup(PerformBackup.BackupCause.STOP);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Integer> pendingTasks = new ArrayList<>();
                //returns only the async takss
                for (BukkitWorker pendingTask : getServer().getScheduler().getActiveWorkers()) {
                    if (pendingTask.getOwner().equals(AuthMe.this)
                        //it's not a peridic task
                        && !getServer().getScheduler().isQueued(pendingTask.getTaskId())) {
                        pendingTasks.add(pendingTask.getTaskId());
                    }
                }

                getLogger().log(Level.INFO, "Waiting for {0} tasks to finish", pendingTasks.size());
                int progress = 0;

                //one minute + some time checking the running state
                int tries = 60;
                while (!pendingTasks.isEmpty()) {
                    if (tries <= 0) {
                        getLogger().log(Level.INFO, "Async tasks times out after to many tries {0}", pendingTasks);
                        break;
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    for (Iterator<Integer> iterator = pendingTasks.iterator(); iterator.hasNext(); ) {
                        int taskId = iterator.next();
                        if (!getServer().getScheduler().isCurrentlyRunning(taskId)) {
                            iterator.remove();
                            progress++;
                            getLogger().log(Level.INFO, "Progress: {0} / {1}", new Object[]{progress, pendingTasks.size()});
                        }
                    }

                    tries--;
                }

                if (database != null) {
                    database.close();
                }
            }
        }, "AuthMe-DataSource#close").start();

        // Disabled correctly
        ConsoleLogger.info("AuthMe " + this.getDescription().getVersion() + " disabled!");
        ConsoleLogger.close();
    }

    // Stop/unload the server/plugin as defined in the configuration
    public void stopOrUnload() {
        if (settings == null || settings.getProperty(SecuritySettings.STOP_SERVER_ON_PROBLEM)) {
            ConsoleLogger.warning("THE SERVER IS GOING TO SHUT DOWN AS DEFINED IN THE CONFIGURATION!");
            getServer().shutdown();
        } else {
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Sets up the data source.
     *
     * @throws ClassNotFoundException if no driver could be found for the datasource
     * @throws SQLException           when initialization of a SQL datasource failed
     * @throws IOException            if flat file cannot be read
     * @see AuthMe#database
     */
    public void setupDatabase() throws ClassNotFoundException, SQLException, IOException {
        if (this.database != null) {
            this.database.close();
        }

        DataSourceType dataSourceType = settings.getProperty(DatabaseSettings.BACKEND);
        DataSource dataSource;
        switch (dataSourceType) {
            case FILE:
                dataSource = new FlatFile(this);
                break;
            case MYSQL:
                dataSource = new MySQL(settings);
                break;
            case SQLITE:
                dataSource = new SQLite(settings);
                break;
            default:
                throw new UnsupportedOperationException("Unknown data source type '" + dataSourceType + "'");
        }

        DataSource convertedSource = MigrationService.convertFlatfileToSqlite(settings, dataSource);
        dataSource = convertedSource == null ? dataSource : convertedSource;

        if (settings.getProperty(DatabaseSettings.USE_CACHING)) {
            dataSource = new CacheDataSource(dataSource);
        }

        database = dataSource;
        if (DataSourceType.SQLITE == dataSourceType) {
            getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
                @Override
                public void run() {
                    int accounts = database.getAccountsRegistered();
                    if (accounts >= 4000) {
                        ConsoleLogger.warning("YOU'RE USING THE SQLITE DATABASE WITH "
                            + accounts + "+ ACCOUNTS; FOR BETTER PERFORMANCE, PLEASE UPGRADE TO MYSQL!!");
                    }
                }
            });
        }
    }

    // Set the console filter to remove the passwords
    private void setLog4JFilter() {
        org.apache.logging.log4j.core.Logger logger;
        logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logger.addFilter(new Log4JFilter());
    }

    // Save Player Data
    private void savePlayer(Player player, LimboCache limboCache, ValidationService validationService) {
        final String name = player.getName().toLowerCase();
        if (safeIsNpc(player) || validationService.isUnrestricted(name)) {
            return;
        }
        if (limboCache.hasPlayerData(name)) {
            limboCache.restoreData(player);
            limboCache.removeFromCache(player);
        } else {
            if (settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)) {
                Location loc = spawnLoader.getPlayerLocationOrSpawn(player);
                final PlayerAuth auth = PlayerAuth.builder()
                    .name(player.getName().toLowerCase())
                    .realName(player.getName())
                    .location(loc).build();
                database.updateQuitLoc(auth);
            }
            if (settings.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)
                && !settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
                PlayerDataStorage playerDataStorage = injector.getIfAvailable(PlayerDataStorage.class);
                if (playerDataStorage != null && !playerDataStorage.hasData(player)) {
                    playerDataStorage.saveData(player);
                }
            }
        }
        playerCache.removePlayer(name);
    }

    private boolean safeIsNpc(Player player) {
        return pluginHooks != null && pluginHooks.isNpc(player) || player.hasMetadata("NPC");
    }

    private void scheduleRecallEmailTask() {
        if (!settings.getProperty(RECALL_PLAYERS)) {
            return;
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                for (PlayerAuth auth : database.getLoggedPlayers()) {
                    String email = auth.getEmail();
                    if (StringUtils.isEmpty(email) || "your@email.com".equalsIgnoreCase(email)) {
                        Player player = bukkitService.getPlayerExact(auth.getRealName());
                        if (player != null) {
                            messages.send(player, MessageKey.ADD_EMAIL_MESSAGE);
                        }
                    }
                }
            }
        }, 1, 1200 * settings.getProperty(EmailSettings.DELAY_RECALL));
    }

    public String replaceAllInfo(String message, Player player) {
        String playersOnline = Integer.toString(bukkitService.getOnlinePlayers().size());
        String ipAddress = Utils.getPlayerIp(player);
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
            .replace("{COUNTRY}", geoLiteApi.getCountryName(ipAddress));
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

    // -------------
    // Service getters (deprecated)
    // Use @Inject fields instead
    // -------------

    /**
     * @return process manager
     *
     * @deprecated should be used in API classes only (temporarily)
     */
    @Deprecated
    public Management getManagement() {
        return management;
    }
}
