package fr.xephi.authme;

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
import fr.xephi.authme.hooks.BungeeCordMessage;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.initialization.AuthMeServiceInitializer;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.MetricsStarter;
import fr.xephi.authme.listener.AuthMeBlockListener;
import fr.xephi.authme.listener.AuthMeEntityListener;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.listener.AuthMePlayerListener16;
import fr.xephi.authme.listener.AuthMePlayerListener18;
import fr.xephi.authme.listener.AuthMeServerListener;
import fr.xephi.authme.output.ConsoleFilter;
import fr.xephi.authme.output.Log4JFilter;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PermissionsSystemType;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.SHA256;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SettingsMigrationService;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.settings.properties.SettingsFieldRetriever;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import fr.xephi.authme.task.PurgeService;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.GeoLiteAPI;
import fr.xephi.authme.util.MigrationService;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
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
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitWorker;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static fr.xephi.authme.settings.properties.EmailSettings.RECALL_PLAYERS;

/**
 * The AuthMe main class.
 */
public class AuthMe extends JavaPlugin {

    // Name of the plugin.
    private static final String PLUGIN_NAME = "AuthMeReloaded";

    // Default version and build number values;
    private static String pluginVersion = "N/D";
    private static String pluginBuildNumber = "Unknown";

    /*
     * Private instances
     */

    // Plugin instance
    private static AuthMe plugin;

    private Management management;
    private CommandHandler commandHandler;
    private PermissionsManager permsMan;
    private NewSetting newSettings;
    private Messages messages;
    private PasswordSecurity passwordSecurity;
    private DataSource database;
    private PluginHooks pluginHooks;
    private SpawnLoader spawnLoader;
    private BukkitService bukkitService;
    private AuthMeServiceInitializer initializer;
    private GeoLiteAPI geoLiteApi;

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
     * Get the plugin's instance.
     *
     * @return AuthMe
     */
    @Deprecated
    public static AuthMe getInstance() {
        return plugin;
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

    /**
     * Method called when the server enables the plugin.
     */
    @Override
    public void onEnable() {
        // Set the plugin instance and load plugin info from the plugin description.
        plugin = this;
        loadPluginInfo();

        // Set the Logger instance and log file path
        ConsoleLogger.setLogger(getLogger());
        ConsoleLogger.setLogFile(new File(getDataFolder(), "authme.log"));

        // Load settings and custom configurations, if it fails, stop the server due to security reasons.
        newSettings = createNewSetting();
        if (newSettings == null) {
            getLogger().warning("Could not load configuration. Aborting.");
            getServer().shutdown();
            setEnabled(false);
            return;
        }

        // Apply settings to the logger
        ConsoleLogger.setLoggingOptions(newSettings);

        // Old settings manager
        if (!loadSettings()) {
            getServer().shutdown();
            setEnabled(false);
            return;
        }

        // Connect to the database and setup tables
        try {
            setupDatabase(newSettings);
        } catch (Exception e) {
            ConsoleLogger.logException("Fatal error occurred during database connection! "
                + "Authme initialization aborted!", e);
            stopOrUnload();
            return;
        }
        // Convert deprecated PLAINTEXT hash entries
        MigrationService.changePlainTextToSha256(newSettings, database, new SHA256());

        // Injector initialization
        initializer = new AuthMeServiceInitializer("fr.xephi.authme");

        // Register elements of the Bukkit / JavaPlugin environment
        initializer.register(AuthMe.class, this);
        initializer.register(Server.class, getServer());
        initializer.register(PluginManager.class, getServer().getPluginManager());
        initializer.register(BukkitScheduler.class, getServer().getScheduler());
        initializer.provide(DataFolder.class, getDataFolder());

        // Register elements we instantiate manually
        initializer.register(NewSetting.class, newSettings);
        initializer.register(DataSource.class, database);

        instantiateServices(initializer);

        // Set up Metrics
        MetricsStarter.setupMetrics(this, newSettings);

        // Set console filter
        setupConsoleFilter();

        // Do a backup on start
        // TODO: maybe create a backup manager?
        new PerformBackup(this, newSettings).doBackup(PerformBackup.BackupCause.START);

        // Set up the BungeeCord hook
        setupBungeeCordHook();

        // Reload support hook
        reloadSupportHook();

        // Register event listeners
        registerEventListeners(initializer);
        // Start Email recall task if needed
        scheduleRecallEmailTask();

        // Show settings warnings
        showSettingsWarnings();

        // Sponsor messages
        ConsoleLogger.info("Development builds are available on our jenkins, thanks to f14stelt.");
        ConsoleLogger.info("Do you want a good game server? Look at our sponsor GameHosting.it leader in Italy as Game Server Provider!");

        // Successful message
        ConsoleLogger.info("AuthMe " + this.getDescription().getVersion() + " correctly enabled!");

        // If server is using PermissionsBukkit, print a warning that some features may not be supported
        if (PermissionsSystemType.PERMISSIONS_BUKKIT.equals(permsMan.getPermissionSystem())) {
            ConsoleLogger.showError("Warning! This server uses PermissionsBukkit for permissions. Some permissions features may not be supported!");
        }

        // Purge on start if enabled
        PurgeService purgeService = initializer.get(PurgeService.class);
        purgeService.runAutoPurge();
    }

    protected void instantiateServices(AuthMeServiceInitializer initializer) {
        // Some statically injected things
        initializer.register(PlayerCache.class, PlayerCache.getInstance());

        messages = initializer.get(Messages.class);
        permsMan = initializer.get(PermissionsManager.class);
        bukkitService = initializer.get(BukkitService.class);
        pluginHooks = initializer.get(PluginHooks.class);
        passwordSecurity = initializer.get(PasswordSecurity.class);
        spawnLoader = initializer.get(SpawnLoader.class);
        commandHandler = initializer.get(CommandHandler.class);
        management = initializer.get(Management.class);
        geoLiteApi = initializer.get(GeoLiteAPI.class);
        initializer.get(NewAPI.class);
        initializer.get(API.class);
    }

    /**
     * Show the settings warnings, for various risky settings.
     */
    private void showSettingsWarnings() {
        // Force single session disabled
        if (!newSettings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)) {
            ConsoleLogger.showError("WARNING!!! By disabling ForceSingleSession, your server protection is inadequate!");
        }

        // Session timeout disabled
        if (newSettings.getProperty(PluginSettings.SESSIONS_TIMEOUT) == 0
            && newSettings.getProperty(PluginSettings.SESSIONS_ENABLED)) {
            ConsoleLogger.showError("WARNING!!! You set session timeout to 0, this may cause security issues!");
        }
    }

    /**
     * Register all event listeners.
     */
    protected void registerEventListeners(AuthMeServiceInitializer initializer) {
        // Get the plugin manager instance
        PluginManager pluginManager = getServer().getPluginManager();

        // Register event listeners
        pluginManager.registerEvents(initializer.get(AuthMePlayerListener.class), this);
        pluginManager.registerEvents(initializer.get(AuthMeBlockListener.class), this);
        pluginManager.registerEvents(initializer.get(AuthMeEntityListener.class), this);
        pluginManager.registerEvents(initializer.get(AuthMeServerListener.class), this);

        // Try to register 1.6 player listeners
        try {
            Class.forName("org.bukkit.event.player.PlayerEditBookEvent");
            pluginManager.registerEvents(initializer.get(AuthMePlayerListener16.class), this);
        } catch (ClassNotFoundException ignore) {
        }

        // Try to register 1.8 player listeners
        try {
            Class.forName("org.bukkit.event.player.PlayerInteractAtEntityEvent");
            pluginManager.registerEvents(initializer.get(AuthMePlayerListener18.class), this);
        } catch (ClassNotFoundException ignore) {
        }
    }

    private void reloadSupportHook() {
        if (database != null) {
            int playersOnline = bukkitService.getOnlinePlayers().size();
            if (playersOnline < 1) {
                database.purgeLogged();
            } else if (Settings.reloadSupport) {
                for (PlayerAuth auth : database.getLoggedPlayers()) {
                    if (auth == null) {
                        continue;
                    }
                    auth.setLastLogin(new Date().getTime());
                    database.updateSession(auth);
                    PlayerCache.getInstance().addPlayer(auth);
                }
            }
        }
    }

    /**
     * Set up the BungeeCord hook.
     */
    private void setupBungeeCordHook() {
        if (newSettings.getProperty(HooksSettings.BUNGEECORD)) {
            Messenger messenger = Bukkit.getMessenger();
            messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
            messenger.registerIncomingPluginChannel(plugin, "BungeeCord", initializer.get(BungeeCordMessage.class));
        }
    }

    /**
     * Load the plugin's settings.
     *
     * @return True on success, false on failure.
     */
    private boolean loadSettings() {
        try {
            new Settings(this);
            return true;
        } catch (Exception e) {
            ConsoleLogger.logException("Can't load the configuration file... Something went wrong. "
                + "To avoid security issues the server will shut down!", e);
            getServer().shutdown();
        }
        return false;
    }

    private NewSetting createNewSetting() {
        File configFile = new File(getDataFolder(), "config.yml");
        PropertyMap properties = SettingsFieldRetriever.getAllPropertyFields();
        SettingsMigrationService migrationService = new SettingsMigrationService();
        return FileUtils.copyFileFromResource(configFile, "config.yml")
            ? new NewSetting(configFile, getDataFolder(), properties, migrationService)
            : null;
    }

    /**
     * Set up the console filter.
     */
    private void setupConsoleFilter() {
        if (!newSettings.getProperty(SecuritySettings.REMOVE_PASSWORD_FROM_CONSOLE)) {
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
        BukkitService bukkitService = initializer.getIfAvailable(BukkitService.class);
        LimboCache limboCache = initializer.getIfAvailable(LimboCache.class);

        if (bukkitService != null && limboCache != null) {
            Collection<? extends Player> players = bukkitService.getOnlinePlayers();
            for (Player player : players) {
                savePlayer(player, limboCache);
            }
        }

        // Do backup on stop if enabled
        if (newSettings != null) {
            new PerformBackup(this, newSettings).doBackup(PerformBackup.BackupCause.STOP);
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
        if (Settings.isStopEnabled) {
            ConsoleLogger.showError("THE SERVER IS GOING TO SHUT DOWN AS DEFINED IN THE CONFIGURATION!");
            getServer().shutdown();
        } else {
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Sets up the data source.
     *
     * @param settings The settings instance
     *
     * @throws ClassNotFoundException if no driver could be found for the datasource
     * @throws SQLException           when initialization of a SQL datasource failed
     * @see AuthMe#database
     */
    public void setupDatabase(NewSetting settings) throws ClassNotFoundException, SQLException {
        if (this.database != null) {
            this.database.close();
        }

        DataSourceType dataSourceType = settings.getProperty(DatabaseSettings.BACKEND);
        DataSource dataSource;
        switch (dataSourceType) {
            case FILE:
                dataSource = new FlatFile();
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

        DataSource convertedSource = MigrationService.convertFlatfileToSqlite(newSettings, dataSource);
        dataSource = convertedSource == null ? dataSource : convertedSource;

        if (newSettings.getProperty(DatabaseSettings.USE_CACHING)) {
            dataSource = new CacheDataSource(dataSource);
        }

        database = dataSource;
        if (DataSourceType.SQLITE == dataSourceType) {
            getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
                @Override
                public void run() {
                    int accounts = database.getAccountsRegistered();
                    if (accounts >= 4000) {
                        ConsoleLogger.showError("YOU'RE USING THE SQLITE DATABASE WITH "
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
    private void savePlayer(Player player, LimboCache limboCache) {
        if (safeIsNpc(player) || Utils.isUnrestricted(player)) {
            return;
        }
        String name = player.getName().toLowerCase();
        if (limboCache.hasPlayerData(name)) {
            limboCache.restoreData(player);
            limboCache.removeFromCache(player);
        } else {
            if (newSettings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)) {
                Location loc = spawnLoader.getPlayerLocationOrSpawn(player);
                final PlayerAuth auth = PlayerAuth.builder()
                    .name(player.getName().toLowerCase())
                    .realName(player.getName())
                    .location(loc).build();
                database.updateQuitLoc(auth);
            }
            if (newSettings.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)
                && !newSettings.getProperty(RestrictionSettings.NO_TELEPORT)) {
                PlayerDataStorage playerDataStorage = initializer.getIfAvailable(PlayerDataStorage.class);
                if (playerDataStorage != null && !playerDataStorage.hasData(player)) {
                    playerDataStorage.saveData(player);
                }
            }
        }
        PlayerCache.getInstance().removePlayer(name);
    }

    private boolean safeIsNpc(Player player) {
        return pluginHooks != null && pluginHooks.isNpc(player) || player.hasMetadata("NPC");
    }

    // Return the spawn location of a player
    @Deprecated
    public Location getSpawnLocation(Player player) {
        return spawnLoader.getSpawnLocation(player);
    }

    private void scheduleRecallEmailTask() {
        if (!newSettings.getProperty(RECALL_PLAYERS)) {
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
        }, 1, 1200 * newSettings.getProperty(EmailSettings.DELAY_RECALL));
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
            .replace("{LOGINS}", Integer.toString(PlayerCache.getInstance().getLogged()))
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
     * @return permission manager
     *
     * @deprecated should be used in API classes only (temporarily)
     */
    @Deprecated
    public PermissionsManager getPermissionsManager() {
        return this.permsMan;
    }

    /**
     * @return process manager
     *
     * @deprecated should be used in API classes only (temporarily)
     */
    @Deprecated
    public Management getManagement() {
        return management;
    }

    /**
     * @return the datasource
     *
     * @deprecated should be used in API classes only (temporarily)
     */
    @Deprecated
    public DataSource getDataSource() {
        return database;
    }

    /**
     * @return password manager
     *
     * @deprecated should be used in API classes only (temporarily)
     */
    @Deprecated
    public PasswordSecurity getPasswordSecurity() {
        return passwordSecurity;
    }

    /**
     * @return plugin hooks
     *
     * @deprecated should be used in API classes only (temporarily)
     */
    @Deprecated
    public PluginHooks getPluginHooks() {
        return pluginHooks;
    }
}
