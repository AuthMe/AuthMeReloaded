package fr.xephi.authme;

import fr.xephi.authme.api.API;
import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.command.CommandInitializer;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.datasource.FlatFile;
import fr.xephi.authme.datasource.MySQL;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.hooks.BungeeCordMessage;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.initialization.AuthMeServiceInitializer;
import fr.xephi.authme.initialization.BaseCommands;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.MetricsStarter;
import fr.xephi.authme.listener.AuthMeBlockListener;
import fr.xephi.authme.listener.AuthMeInventoryPacketAdapter;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.listener.AuthMePlayerListener16;
import fr.xephi.authme.listener.AuthMePlayerListener18;
import fr.xephi.authme.listener.AuthMePlayerListener19;
import fr.xephi.authme.listener.AuthMeServerListener;
import fr.xephi.authme.listener.AuthMeTabCompletePacketAdapter;
import fr.xephi.authme.listener.AuthMeTablistPacketAdapter;
import fr.xephi.authme.mail.SendMailSSL;
import fr.xephi.authme.output.ConsoleFilter;
import fr.xephi.authme.output.Log4JFilter;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
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
import fr.xephi.authme.settings.properties.PurgeSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.settings.properties.SettingsFieldRetriever;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.CollectionUtils;
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
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static fr.xephi.authme.settings.properties.EmailSettings.MAIL_ACCOUNT;
import static fr.xephi.authme.settings.properties.EmailSettings.MAIL_PASSWORD;
import static fr.xephi.authme.settings.properties.EmailSettings.RECALL_PLAYERS;

/**
 * The AuthMe main class.
 */
public class AuthMe extends JavaPlugin {

    // Defines the name of the plugin.
    private static final String PLUGIN_NAME = "AuthMeReloaded";

    // Default version and build number values;
    private static String pluginVersion = "N/D";
    private static String pluginBuildNumber = "Unknown";

    // Private Instances
    private static AuthMe plugin;
    /*
     *  Maps and stuff
     */
    // TODO #601: Integrate CaptchaManager
    public final ConcurrentHashMap<String, BukkitTask> sessions = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, Integer> captcha = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, String> cap = new ConcurrentHashMap<>();

    /*
     * Public Instances
     */
    public NewAPI api;
    // TODO #655: Encapsulate mail
    public SendMailSSL mail;
    // TODO #656: Encapsulate data manager
    public DataManager dataManager;
    /*
     * Private instances
     * TODO #432: Move instantiation and management of these services
     */
    // TODO #604: Encapsulate ProtocolLib members
    public AuthMeInventoryPacketAdapter inventoryProtector;
    public AuthMeTabCompletePacketAdapter tabComplete;
    public AuthMeTablistPacketAdapter tablistHider;
    private Management management;
    private CommandHandler commandHandler;
    private PermissionsManager permsMan;
    private NewSetting newSettings;
    private Messages messages;
    private JsonCache playerBackup;
    private PasswordSecurity passwordSecurity;
    private DataSource database;
    private PluginHooks pluginHooks;
    private SpawnLoader spawnLoader;
    private boolean autoPurging;
    private BukkitService bukkitService;

    /**
     * Get the plugin's instance.
     *
     * @return AuthMe
     */
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

    /**
     * Get the Messages instance.
     *
     * @return Plugin's messages.
     */
    public Messages getMessages() {
        return messages;
    }

    /**
     * Get the plugin's NewSetting instance.
     *
     * @return NewSetting.
     */
    public NewSetting getSettings() {
        return newSettings;
    }

    // Get version and build number of the plugin
    private void setPluginInfos() {
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
        // Set various instances
        plugin = this;
        ConsoleLogger.setLogger(getLogger());
        setPluginInfos();

        // Load settings and custom configurations, if it fails, stop the server due to security reasons.
        newSettings = createNewSetting();
        if (newSettings == null) {
            ConsoleLogger.showError("Could not load configuration. Aborting.");
            getServer().shutdown();
            return;
        }
        ConsoleLogger.setLoggingOptions(newSettings.getProperty(SecuritySettings.USE_LOGGING),
            new File(getDataFolder(), "authme.log"));

        // Old settings manager
        if (!loadSettings()) {
            getServer().shutdown();
            setEnabled(false);
            return;
        }

        messages = new Messages(newSettings.getMessagesFile(), newSettings.getDefaultMessagesFile());

        // Connect to the database and setup tables
        try {
            setupDatabase(newSettings);
        } catch (Exception e) {
            ConsoleLogger.logException("Fatal error occurred during database connection! "
                + "Authme initialization aborted!", e);
            stopOrUnload();
            return;
        }
        MigrationService.changePlainTextToSha256(newSettings, database, new SHA256());


        AuthMeServiceInitializer initializer = new AuthMeServiceInitializer("fr.xephi.authme");
        // Register elements of the Bukkit / JavaPlugin environment
        initializer.register(AuthMe.class, this);
        initializer.register(Server.class, getServer());
        initializer.register(PluginManager.class, getServer().getPluginManager());
        initializer.register(BukkitScheduler.class, getServer().getScheduler());
        initializer.provide(DataFolder.class, getDataFolder());

        // Register elements we instantiate manually
        initializer.register(NewSetting.class, newSettings);
        initializer.register(Messages.class, messages);
        initializer.register(DataSource.class, database);

        // Some statically injected things
        initializer.register(PlayerCache.class, PlayerCache.getInstance());
        initializer.register(LimboCache.class, LimboCache.getInstance());
        initializer.provide(BaseCommands.class, CommandInitializer.buildCommands(initializer));

        permsMan         = initializer.get(PermissionsManager.class);
        bukkitService    = initializer.get(BukkitService.class);
        pluginHooks      = initializer.get(PluginHooks.class);
        passwordSecurity = initializer.get(PasswordSecurity.class);
        spawnLoader      = initializer.get(SpawnLoader.class);
        commandHandler   = initializer.get(CommandHandler.class);
        api              = initializer.get(NewAPI.class);
        management       = initializer.get(Management.class);
        dataManager      = initializer.get(DataManager.class);
        initializer.get(API.class);

        // Set up Metrics
        MetricsStarter.setupMetrics(this, newSettings);

        // Set console filter
        setupConsoleFilter();

        // Download and load GeoIp.dat file if absent
        GeoLiteAPI.isDataAvailable();

        // Set up the mail API
        setupMailApi();

        // Check if the ProtocolLib is available. If so we could listen for
        // inventory protection
        checkProtocolLib();
        // End of Hooks

        // Do a backup on start
        new PerformBackup(this, newSettings).doBackup(PerformBackup.BackupCause.START);


        // Setup the inventory backup
        playerBackup = new JsonCache();


        // Set up the BungeeCord hook
        setupBungeeCordHook(newSettings);

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

        // Purge on start if enabled
        runAutoPurge();
    }

    /**
     * Reload certain components.
     *
     * @throws Exception if an error occurs
     */
    public void reload() throws Exception {
        newSettings.reload();
        // We do not change database type for consistency issues, but we'll output a note in the logs
        if (!newSettings.getProperty(DatabaseSettings.BACKEND).equals(database.getType())) {
            ConsoleLogger.info("Note: cannot change database type during /authme reload");
        }
        database.reload();
        messages.reload(newSettings.getMessagesFile());
        passwordSecurity.reload();
        spawnLoader.initialize(newSettings);
    }

    /**
     * Set up the mail API, if enabled.
     */
    private void setupMailApi() {
        // Make sure the mail API is enabled
        if (!newSettings.getProperty(MAIL_ACCOUNT).isEmpty() && !newSettings.getProperty(MAIL_PASSWORD).isEmpty()) {
            this.mail = new SendMailSSL(this, newSettings);
        }
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
    private void registerEventListeners(AuthMeServiceInitializer initializer) {
        // Get the plugin manager instance
        PluginManager pluginManager = getServer().getPluginManager();

        // Register event listeners
        pluginManager.registerEvents(initializer.get(AuthMePlayerListener.class), this);
        pluginManager.registerEvents(initializer.get(AuthMeBlockListener.class),  this);
        pluginManager.registerEvents(initializer.get(AuthMePlayerListener.class), this);
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

        // Try to register 1.9 player listeners
        try {
            Class.forName("org.spigotmc.event.player.PlayerSpawnLocationEvent");
            pluginManager.registerEvents(initializer.get(AuthMePlayerListener19.class), this);
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
    private void setupBungeeCordHook(NewSetting settings) {
        if (settings.getProperty(HooksSettings.BUNGEECORD)) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            Bukkit.getMessenger().registerIncomingPluginChannel(
                this, "BungeeCord", new BungeeCordMessage(this));
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
        if (Settings.removePassword) {
            ConsoleFilter filter = new ConsoleFilter();
            getLogger().setFilter(filter);
            Bukkit.getLogger().setFilter(filter);
            Logger.getLogger("Minecraft").setFilter(filter);
            // Set Log4J Filter
            try {
                Class.forName("org.apache.logging.log4j.core.Filter");
                setLog4JFilter();
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                ConsoleLogger.info("You're using Minecraft 1.6.x or older, Log4J support will be disabled");
            }
        }
    }

    @Override
    public void onDisable() {
        // Save player data
        if (bukkitService != null) {
            Collection<? extends Player> players = bukkitService.getOnlinePlayers();
            for (Player player : players) {
                savePlayer(player);
            }
        }

        // Do backup on stop if enabled
        if (newSettings != null) {
            new PerformBackup(this, newSettings).doBackup(PerformBackup.BackupCause.STOP);
        }
        final AuthMe pluginInstance = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Integer> pendingTasks = new ArrayList<>();
                for (BukkitTask pendingTask : getServer().getScheduler().getPendingTasks()) {
                    if (pendingTask.getOwner().equals(pluginInstance) && !pendingTask.isSync()) {
                        pendingTasks.add(pendingTask.getTaskId());
                    }
                }
                ConsoleLogger.info("Waiting for " + pendingTasks.size() + " tasks to finish");
                int progress = 0;
                for (int taskId : pendingTasks) {
                    int maxTries = 5;
                    while (getServer().getScheduler().isCurrentlyRunning(taskId)) {
                        if (maxTries <= 0) {
                            ConsoleLogger.info("Async task " + taskId + " times out after to many tries");
                            break;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                        maxTries--;
                    }

                    progress++;
                    ConsoleLogger.info("Progress: " + progress + " / " + pendingTasks.size());
                }
                if (database != null) {
                    database.close();
                }
            }
        }, "AuthMe-DataSource#close").start();

        // Close the database

        // Disabled correctly
        ConsoleLogger.info("AuthMe " + this.getDescription().getVersion() + " disabled!");
    }

    // Stop/unload the server/plugin as defined in the configuration
    public void stopOrUnload() {
        if (Settings.isStopEnabled) {
            ConsoleLogger.showError("THE SERVER IS GOING TO SHUT DOWN AS DEFINED IN THE CONFIGURATION!");
            getServer().shutdown();
        } else {
            getServer().getPluginManager().disablePlugin(AuthMe.getInstance());
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
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                org.apache.logging.log4j.core.Logger logger;
                logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
                logger.addFilter(new Log4JFilter());
                logger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger("net.minecraft");
                logger.addFilter(new Log4JFilter());
            }
        });
    }

    // Check the presence of the ProtocolLib plugin
    public void checkProtocolLib() {
        if (!getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            if (newSettings.getProperty(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN)) {
                ConsoleLogger.showError("WARNING! The protectInventory feature requires ProtocolLib! Disabling it...");
                Settings.protectInventoryBeforeLogInEnabled = false;
                newSettings.setProperty(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN, false);
                newSettings.save();
            }
            return;
        }

        if (newSettings.getProperty(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN) && inventoryProtector == null) {
            inventoryProtector = new AuthMeInventoryPacketAdapter(this);
            inventoryProtector.register();
        } else if (inventoryProtector != null) {
            inventoryProtector.unregister();
            inventoryProtector = null;
        }
        if (newSettings.getProperty(RestrictionSettings.DENY_TABCOMPLETE_BEFORE_LOGIN) && tabComplete == null) {
            tabComplete = new AuthMeTabCompletePacketAdapter(this);
            tabComplete.register();
        } else if (tabComplete != null) {
            tabComplete.unregister();
            tabComplete = null;
        }
        if (newSettings.getProperty(RestrictionSettings.HIDE_TABLIST_BEFORE_LOGIN) && tablistHider == null) {
            tablistHider = new AuthMeTablistPacketAdapter(this, bukkitService);
            tablistHider.register();
        } else if (tablistHider != null) {
            tablistHider.unregister();
            tablistHider = null;
        }
    }

    // Save Player Data
    private void savePlayer(Player player) {
        if (safeIsNpc(player) || Utils.isUnrestricted(player)) {
            return;
        }
        String name = player.getName().toLowerCase();
        if (PlayerCache.getInstance().isAuthenticated(name) && !player.isDead() && Settings.isSaveQuitLocationEnabled) {
            final PlayerAuth auth = PlayerAuth.builder()
                .name(player.getName().toLowerCase())
                .realName(player.getName())
                .location(player.getLocation()).build();
            database.updateQuitLoc(auth);
        }
        if (LimboCache.getInstance().hasLimboPlayer(name)) {
            LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
            if (!Settings.noTeleport) {
                player.teleport(limbo.getLoc());
            }

            Utils.addNormal(player, limbo.getGroup());
            player.setOp(limbo.isOperator());
            limbo.getTimeoutTask().cancel();
            LimboCache.getInstance().deleteLimboPlayer(name);
            if (this.playerBackup.doesCacheExist(player)) {
                this.playerBackup.removeCache(player);
            }
        }
        PlayerCache.getInstance().removePlayer(name);
    }

    private boolean safeIsNpc(Player player) {
        return pluginHooks != null && pluginHooks.isNpc(player) || player.hasMetadata("NPC");
    }

    // Select the player to kick when a vip player joins the server when full
    public Player generateKickPlayer(Collection<? extends Player> collection) {
        for (Player player : collection) {
            if (!getPermissionsManager().hasPermission(player, PlayerStatePermission.IS_VIP)) {
                return player;
            }
        }
        return null;
    }

    // Purge inactive players from the database, as defined in the configuration
    private void runAutoPurge() {
        if (!newSettings.getProperty(PurgeSettings.USE_AUTO_PURGE) || autoPurging) {
            return;
        }
        autoPurging = true;
        getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                ConsoleLogger.info("AutoPurging the Database...");
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, -newSettings.getProperty(PurgeSettings.DAYS_BEFORE_REMOVE_PLAYER));
                long until = calendar.getTimeInMillis();
                List<String> cleared = database.autoPurgeDatabase(until);
                if (CollectionUtils.isEmpty(cleared)) {
                    return;
                }
                ConsoleLogger.info("AutoPurging the Database: " + cleared.size() + " accounts removed!");
                if (newSettings.getProperty(PurgeSettings.REMOVE_ESSENTIALS_FILES) && pluginHooks.isEssentialsAvailable())
                    dataManager.purgeEssentials(cleared);
                if (newSettings.getProperty(PurgeSettings.REMOVE_PLAYER_DAT))
                    dataManager.purgeDat(cleared);
                if (newSettings.getProperty(PurgeSettings.REMOVE_LIMITED_CREATIVE_INVENTORIES))
                    dataManager.purgeLimitedCreative(cleared);
                if (newSettings.getProperty(PurgeSettings.REMOVE_ANTI_XRAY_FILE))
                    dataManager.purgeAntiXray(cleared);
                if (newSettings.getProperty(PurgeSettings.REMOVE_PERMISSIONS))
                    dataManager.purgePermissions(cleared);
                ConsoleLogger.info("AutoPurge Finished!");
                autoPurging = false;
            }
        });
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
            .replace("{COUNTRY}", GeoLiteAPI.getCountryName(ipAddress));
    }

    public boolean isLoggedIp(String name, String ip) {
        int count = 0;
        for (Player player : bukkitService.getOnlinePlayers()) {
            if (ip.equalsIgnoreCase(Utils.getPlayerIp(player))
                && database.isLogged(player.getName().toLowerCase())
                && !player.getName().equalsIgnoreCase(name)) {
                ++count;
            }
        }
        return count >= Settings.getMaxLoginPerIp;
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

    /**
     * Get the permissions manager instance.
     *
     * @return Permissions Manager instance.
     */
    public PermissionsManager getPermissionsManager() {
        return this.permsMan;
    }

    /**
     * Return the management instance.
     *
     * @return management The Management
     */
    public Management getManagement() {
        return management;
    }

    public DataSource getDataSource() {
        return database;
    }

    public PasswordSecurity getPasswordSecurity() {
        return passwordSecurity;
    }

    public PluginHooks getPluginHooks() {
        return pluginHooks;
    }

}
