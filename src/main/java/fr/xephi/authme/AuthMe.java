package fr.xephi.authme;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;

import com.earth2me.essentials.Essentials;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.onarandombox.MultiverseCore.MultiverseCore;

import fr.xephi.authme.api.API;
import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.command.CommandInitializer;
import fr.xephi.authme.command.CommandMapper;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.converter.ForceFlatToSqlite;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.FlatFile;
import fr.xephi.authme.datasource.MySQL;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.hooks.BungeeCordMessage;
import fr.xephi.authme.hooks.EssSpawn;
import fr.xephi.authme.listener.AuthMeBlockListener;
import fr.xephi.authme.listener.AuthMeEntityListener;
import fr.xephi.authme.listener.AuthMeInventoryPacketAdapter;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.listener.AuthMePlayerListener16;
import fr.xephi.authme.listener.AuthMePlayerListener18;
import fr.xephi.authme.listener.AuthMeServerListener;
import fr.xephi.authme.listener.AuthMeTabCompletePacketAdapter;
import fr.xephi.authme.mail.SendMailSSL;
import fr.xephi.authme.modules.ModuleManager;
import fr.xephi.authme.output.ConsoleFilter;
import fr.xephi.authme.output.Log4JFilter;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.OtherAccounts;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.Spawn;
import fr.xephi.authme.util.GeoLiteAPI;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.Wrapper;
import net.minelink.ctplus.CombatTagPlus;

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
    private static Server server;
    private static Wrapper wrapper = Wrapper.getInstance();
    private Management management;
    private CommandHandler commandHandler = null;
    private PermissionsManager permsMan = null;
    private Settings settings;
    private Messages messages;
    private JsonCache playerBackup;
    private ModuleManager moduleManager;
    private PasswordSecurity passwordSecurity;
    private DataSource database;

    // Public Instances
    public NewAPI api;
    public SendMailSSL mail;
    public DataManager dataManager;
    public OtherAccounts otherAccounts;
    public Location essentialsSpawn;

    /*
     * Plugin Hooks
     * TODO: Move into modules
     */
    public Essentials ess;
    public MultiverseCore multiverse;
    public CombatTagPlus combatTagPlus;
    public AuthMeInventoryPacketAdapter inventoryProtector;
    public AuthMeTabCompletePacketAdapter tabComplete;

    /*
     *  Maps and stuff
     *  TODO: Clean up and Move into a manager
     */
    public final ConcurrentHashMap<String, BukkitTask> sessions = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, Integer> captcha = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, String> cap = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, String> realIp = new ConcurrentHashMap<>();

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
     * Get the plugin's Settings.
     *
     * @return Plugin's settings.
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Get the Messages instance.
     *
     * @return Plugin's messages.
     */
    public Messages getMessages() {
        return messages;
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
        server = getServer();
        plugin = this;

        setPluginInfos();

        // Load settings and custom configurations, if it fails, stop the server due to security reasons.
        if (loadSettings()) {
            server.shutdown();
            setEnabled(false);
            return;
        }

        // Set up messages & password security
        messages = Messages.getInstance();

        // Connect to the database and setup tables
        try {
            setupDatabase();
        } catch (Exception e) {
            ConsoleLogger.writeStackTrace(e);
            ConsoleLogger.showError(e.getMessage());
            ConsoleLogger.showError("Fatal error occurred during database connection! Authme initialization ABORTED!");
            stopOrUnload();
            return;
        }

        passwordSecurity = new PasswordSecurity(getDataSource(), Settings.getPasswordHash,
            Bukkit.getPluginManager(), Settings.supportOldPassword);

        // Set up the permissions manager and command handler
        permsMan = initializePermissionsManager();
        commandHandler = initializeCommandHandler(permsMan, messages, passwordSecurity);

        // Set up the module manager
        setupModuleManager();

        // Setup otherAccounts file
        this.otherAccounts = OtherAccounts.getInstance();


        // Set up Metrics
        setupMetrics();

        // Set console filter
        setupConsoleFilter();

        // AntiBot delay
        AntiBot.setupAntiBotService();

        // Download and load GeoIp.dat file if absent
        GeoLiteAPI.isDataAvailable();

        // Set up the mail API
        setupMailApi();

        // Hooks
        // Check Combat Tag Plus Version
        checkCombatTagPlus();

        // Check Multiverse
        checkMultiverse();

        // Check Essentials
        checkEssentials();

        // Check if the ProtocolLib is available. If so we could listen for
        // inventory protection
        checkProtocolLib();
        // End of Hooks

        // Do a backup on start
        new PerformBackup(plugin).doBackup(PerformBackup.BackupCause.START);


        // Setup the inventory backup
        playerBackup = new JsonCache();

        // Set the DataManager
        dataManager = new DataManager(this);

        // Set up the new API
        setupApi();

        // Set up the management
        management = new Management(this);

        // Set up the BungeeCord hook
        setupBungeeCordHook();

        // Reload support hook
        reloadSupportHook();

        // Register event listeners
        registerEventListeners();

        // Purge on start if enabled
        autoPurge();

        // Start Email recall task if needed
        recallEmail();

        // Show settings warnings
        showSettingsWarnings();

        // Sponsor messages
        ConsoleLogger.info("AuthMe hooks perfectly with the VeryGames server hosting!");
        ConsoleLogger.info("Development builds are available on our jenkins, thanks to f14stelt.");
        ConsoleLogger.info("Do you want a good game server? Look at our sponsor GameHosting.it leader in Italy as Game Server Provider!");

        // Successful message
        ConsoleLogger.info("AuthMe " + this.getDescription().getVersion() + " correctly enabled!");
    }

    /**
     * Set up the module manager.
     */
    private void setupModuleManager() {
        // TODO: Clean this up!
        // TODO: split the plugin in more modules
        // TODO: log number of loaded modules

        // Define the module manager instance
        moduleManager = new ModuleManager(this);

        // Load the modules
        // int loaded = moduleManager.loadModules();
    }

    /**
     * Set up the mail API, if enabled.
     */
    private void setupMailApi() {
        // Make sure the mail API is enabled
        if (Settings.getmailAccount.isEmpty() || Settings.getmailPassword.isEmpty()) {
            return;
        }

        // Set up the mail API
        this.mail = new SendMailSSL(this);
    }

    /**
     * Show the settings warnings, for various risky settings.
     */
    private void showSettingsWarnings() {
        // Force single session disabled
        if (!Settings.isForceSingleSessionEnabled) {
            ConsoleLogger.showError("WARNING!!! By disabling ForceSingleSession, your server protection is inadequate!");
        }

        // Session timeout disabled
        if (Settings.getSessionTimeout == 0 && Settings.isSessionsEnabled) {
            ConsoleLogger.showError("WARNING!!! You set session timeout to 0, this may cause security issues!");
        }
    }

    /**
     * Register all event listeners.
     */
    private void registerEventListeners() {
        // Get the plugin manager instance
        PluginManager pluginManager = server.getPluginManager();

        // Register event listeners
        pluginManager.registerEvents(new AuthMePlayerListener(this), this);
        pluginManager.registerEvents(new AuthMeBlockListener(), this);
        pluginManager.registerEvents(new AuthMeEntityListener(), this);
        pluginManager.registerEvents(new AuthMeServerListener(this), this);

        // Try to register 1.6 player listeners
        try {
            Class.forName("org.bukkit.event.player.PlayerEditBookEvent");
            pluginManager.registerEvents(new AuthMePlayerListener16(), this);
        } catch (ClassNotFoundException ignore) {
        }

        // Try to register 1.8 player listeners
        try {
            Class.forName("org.bukkit.event.player.PlayerInteractAtEntityEvent");
            pluginManager.registerEvents(new AuthMePlayerListener18(), this);
        } catch (ClassNotFoundException ignore) {
        }
    }

    private void reloadSupportHook() {
        if (database != null) {
            int playersOnline = Utils.getOnlinePlayers().size();
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
        if (Settings.bungee) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BungeeCordMessage(this));
        }
    }

    private CommandHandler initializeCommandHandler(PermissionsManager permissionsManager, Messages messages,
                                                    PasswordSecurity passwordSecurity) {
        HelpProvider helpProvider = new HelpProvider(permissionsManager);
        Set<CommandDescription> baseCommands = CommandInitializer.buildCommands();
        CommandMapper mapper = new CommandMapper(baseCommands, messages, permissionsManager, helpProvider);
        CommandService commandService = new CommandService(this, mapper, helpProvider, messages, passwordSecurity);
        return new CommandHandler(commandService);
    }

    /**
     * Set up the API. This sets up the new and the old API.
     */
    @SuppressWarnings("deprecation")
    private void setupApi() {
        // Set up the API
        api = new NewAPI(this);

        // Set up the deprecated API
        new API(this);
    }

    /**
     * Load the plugin's settings.
     *
     * @return True on success, false on failure.
     */
    private boolean loadSettings() {
        // TODO: new configuration style (more files)
        try {
            settings = new Settings(this);
            Settings.reload();
        } catch (Exception e) {
            ConsoleLogger.writeStackTrace(e);
            ConsoleLogger.showError("Can't load the configuration file... Something went wrong, to avoid security issues the server will shutdown!");
            server.shutdown();
            return true;
        }
        return false;
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

    /**
     * Set up Metrics.
     */
    private void setupMetrics() {
        try {
            Metrics metrics = new Metrics(this);
            Graph messagesLanguage = metrics.createGraph("Messages language");
            Graph databaseBackend = metrics.createGraph("Database backend");

            // Custom graphs
            if (Settings.messageFile.exists()) {
                messagesLanguage.addPlotter(new Metrics.Plotter(Settings.messagesLanguage) {

                    @Override
                    public int getValue() {
                        return 1;
                    }
                });
            }
            databaseBackend.addPlotter(new Metrics.Plotter(Settings.getDataSource.toString()) {

                @Override
                public int getValue() {
                    return 1;
                }
            });

            metrics.start();
            ConsoleLogger.info("Metrics started successfully!");
        } catch (Exception e) {
            // Failed to submit the metrics data
            ConsoleLogger.writeStackTrace(e);
            ConsoleLogger.showError("Can't start Metrics! The plugin will work anyway...");
        }
    }

    @Override
    public void onDisable() {
        // Save player data
        Collection<? extends Player> players = Utils.getOnlinePlayers();
        for (Player player : players) {
            savePlayer(player);
        }

        // Do backup on stop if enabled
        new PerformBackup(plugin).doBackup(PerformBackup.BackupCause.STOP);

        // Unload modules
        if (moduleManager != null) {
            moduleManager.unloadModules();
        }

        // Close the database
        if (database != null) {
            database.close();
        }

        // Disabled correctly
        ConsoleLogger.info("AuthMe " + this.getDescription().getVersion() + " disabled!");
    }

    // Stop/unload the server/plugin as defined in the configuration
    public void stopOrUnload() {
        if (Settings.isStopEnabled) {
            ConsoleLogger.showError("THE SERVER IS GOING TO SHUT DOWN AS DEFINED IN THE CONFIGURATION!");
            server.shutdown();
        } else {
            server.getPluginManager().disablePlugin(AuthMe.getInstance());
        }
    }

    public void setupDatabase() throws Exception {
        if (database != null)
            database.close();
        // Backend MYSQL - FILE - SQLITE - SQLITEHIKARI
        boolean isSQLite = false;
        switch (Settings.getDataSource) {
            case FILE:
                database = new FlatFile();
                break;
            case MYSQL:
                database = new MySQL();
                break;
            case SQLITE:
                database = new SQLite();
                isSQLite = true;
                break;
        }

        if (isSQLite) {
            server.getScheduler().runTaskAsynchronously(this, new Runnable() {
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

        if (Settings.getDataSource == DataSource.DataSourceType.FILE) {
            ConsoleLogger.showError("FlatFile backend has been detected and is now deprecated, it will be changed " +
                "to SQLite... Connection will be impossible until conversion is done!");
            ForceFlatToSqlite converter = new ForceFlatToSqlite(database);
            DataSource source = converter.run();
            if (source != null) {
                database = source;
            }
        }

        // TODO: Move this to another place maybe ?
        if (Settings.getPasswordHash == HashAlgorithm.PLAINTEXT) {
            ConsoleLogger.showError("Your HashAlgorithm has been detected as plaintext and is now deprecated; " +
                "it will be changed and hashed now to the AuthMe default hashing method");
            for (PlayerAuth auth : database.getAllAuths()) {
                HashedPassword hashedPassword = passwordSecurity.computeHash(
                    HashAlgorithm.SHA256, auth.getPassword().getHash(), auth.getNickname());
                auth.setPassword(hashedPassword);
                database.updatePassword(auth);
            }
            Settings.setValue("settings.security.passwordHash", "SHA256");
            Settings.reload();
        }

        if (Settings.isCachingEnabled) {
            database = new CacheDataSource(database);
        }
    }

    /**
     * Set up the permissions manager.
     */
    private PermissionsManager initializePermissionsManager() {
        PermissionsManager manager = new PermissionsManager(Bukkit.getServer(), this, getLogger());
        manager.setup();
        return manager;
    }

    /**
     * Get the permissions manager instance.
     *
     * @return Permissions Manager instance.
     */
    public PermissionsManager getPermissionsManager() {
        return this.permsMan;
    }

    // Set the console filter to remove the passwords
    private void setLog4JFilter() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

            @Override
            public void run() {
                org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
                coreLogger.addFilter(new Log4JFilter());
            }
        });
    }

    // Get the Multiverse plugin
    public void checkMultiverse() {
        if (Settings.multiverse && server.getPluginManager().isPluginEnabled("Multiverse-Core")) {
            try {
                multiverse = (MultiverseCore) server.getPluginManager().getPlugin("Multiverse-Core");
                ConsoleLogger.info("Hooked correctly with Multiverse-Core");
            } catch (Exception | NoClassDefFoundError ignored) {
                multiverse = null;
            }
        } else {
            multiverse = null;
        }
    }

    // Get the Essentials plugin
    public void checkEssentials() {
        if (server.getPluginManager().isPluginEnabled("Essentials")) {
            try {
                ess = (Essentials) server.getPluginManager().getPlugin("Essentials");
                ConsoleLogger.info("Hooked correctly with Essentials");
            } catch (Exception | NoClassDefFoundError ignored) {
                ess = null;
            }
        } else {
            ess = null;
        }
        if (server.getPluginManager().isPluginEnabled("EssentialsSpawn")) {
            try {
                essentialsSpawn = new EssSpawn().getLocation();
                ConsoleLogger.info("Hooked correctly with EssentialsSpawn");
            } catch (Exception e) {
                essentialsSpawn = null;
                ConsoleLogger.showError("Can't read the /plugins/Essentials/spawn.yml file!");
            }
        } else {
            essentialsSpawn = null;
        }
    }

    // Check the presence of CombatTag
    public void checkCombatTagPlus() {
        if (server.getPluginManager().isPluginEnabled("CombatTagPlus")) {
            try {
                combatTagPlus = (CombatTagPlus) server.getPluginManager().getPlugin("CombatTagPlus");
                ConsoleLogger.info("Hooked correctly with CombatTagPlus");
            } catch (Exception | NoClassDefFoundError ignored) {
                combatTagPlus = null;
            }
        } else {
            combatTagPlus = null;
        }
    }

    // Check the presence of the ProtocolLib plugin
    public void checkProtocolLib() {
        if (!server.getPluginManager().isPluginEnabled("ProtocolLib")) {
            if (Settings.protectInventoryBeforeLogInEnabled) {
                ConsoleLogger.showError("WARNING!!! The protectInventory feature requires ProtocolLib! Disabling it...");
                Settings.protectInventoryBeforeLogInEnabled = false;
                getSettings().set("settings.restrictions.ProtectInventoryBeforeLogIn", false);
            }
            return;
        }

        if (Settings.protectInventoryBeforeLogInEnabled) {
            if (inventoryProtector == null) {
                inventoryProtector = new AuthMeInventoryPacketAdapter(this);
                inventoryProtector.register();
            }
        } else {
            if (inventoryProtector != null) {
                inventoryProtector.unregister();
                inventoryProtector = null;
            }
        }
        if (tabComplete == null) {
            tabComplete = new AuthMeTabCompletePacketAdapter(this);
            tabComplete.register();
        }
    }

    // Save Player Data
    private void savePlayer(Player player) {
        if (Utils.isNPC(player) || Utils.isUnrestricted(player)) {
            return;
        }
        String name = player.getName().toLowerCase();
        if (PlayerCache.getInstance().isAuthenticated(name) && !player.isDead() && Settings.isSaveQuitLocationEnabled) {
            final PlayerAuth auth = new PlayerAuth(player.getName().toLowerCase(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getWorld().getName(), player.getName());
            database.updateQuitLoc(auth);
        }
        if (LimboCache.getInstance().hasLimboPlayer(name)) {
            LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
            if (!Settings.noTeleport) {
                player.teleport(limbo.getLoc());
            }

            Utils.addNormal(player, limbo.getGroup());
            player.setOp(limbo.getOperator());
            limbo.getTimeoutTaskId().cancel();
            LimboCache.getInstance().deleteLimboPlayer(name);
            if (this.playerBackup.doesCacheExist(player)) {
                this.playerBackup.removeCache(player);
            }
        }
        PlayerCache.getInstance().removePlayer(name);
    }

    // Select the player to kick when a vip player join the server when full
    public Player generateKickPlayer(Collection<? extends Player> collection) {
        Player player = null;
        for (Player p : collection) {
            if (!getPermissionsManager().hasPermission(p, PlayerPermission.IS_VIP)) {
                player = p;
                break;
            }
        }
        return player;
    }

    // Purge inactive players from the database, as defined in the configuration
    private void autoPurge() {
        if (!Settings.usePurge) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -(Settings.purgeDelay));
        long until = calendar.getTimeInMillis();
        List<String> cleared = database.autoPurgeDatabase(until);
        if (cleared == null) {
            return;
        }
        if (cleared.isEmpty()) {
            return;
        }
        ConsoleLogger.info("AutoPurging the Database: " + cleared.size() + " accounts removed!");
        if (Settings.purgeEssentialsFile && this.ess != null)
            dataManager.purgeEssentials(cleared);
        if (Settings.purgePlayerDat)
            dataManager.purgeDat(cleared);
        if (Settings.purgeLimitedCreative)
            dataManager.purgeLimitedCreative(cleared);
        if (Settings.purgeAntiXray)
            dataManager.purgeAntiXray(cleared);
        if (Settings.purgePermissions)
            dataManager.purgePermissions(cleared);
    }

    // Return the spawn location of a player
    public Location getSpawnLocation(Player player) {
        World world = player.getWorld();
        String[] spawnPriority = Settings.spawnPriority.split(",");
        Location spawnLoc = world.getSpawnLocation();
        for (int i = spawnPriority.length - 1; i >= 0; i--) {
            String s = spawnPriority[i];
            if (s.equalsIgnoreCase("default") && getDefaultSpawn(world) != null)
                spawnLoc = getDefaultSpawn(world);
            if (s.equalsIgnoreCase("multiverse") && getMultiverseSpawn(world) != null)
                spawnLoc = getMultiverseSpawn(world);
            if (s.equalsIgnoreCase("essentials") && getEssentialsSpawn() != null)
                spawnLoc = getEssentialsSpawn();
            if (s.equalsIgnoreCase("authme") && getAuthMeSpawn(player) != null)
                spawnLoc = getAuthMeSpawn(player);
        }
        if (spawnLoc == null) {
            spawnLoc = world.getSpawnLocation();
        }
        return spawnLoc;
    }

    // Return the default spawn point of a world
    private Location getDefaultSpawn(World world) {
        return world.getSpawnLocation();
    }

    // Return the multiverse spawn point of a world
    private Location getMultiverseSpawn(World world) {
        if (multiverse != null && Settings.multiverse) {
            try {
                return multiverse.getMVWorldManager().getMVWorld(world).getSpawnLocation();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // Return the essentials spawn point
    private Location getEssentialsSpawn() {
        if (essentialsSpawn != null) {
            return essentialsSpawn;
        }
        return null;
    }

    // Return the AuthMe spawn point
    private Location getAuthMeSpawn(Player player) {
        if ((!database.isAuthAvailable(player.getName().toLowerCase()) || !player.hasPlayedBefore())
            && (Spawn.getInstance().getFirstSpawn() != null)) {
            return Spawn.getInstance().getFirstSpawn();
        } else if (Spawn.getInstance().getSpawn() != null) {
            return Spawn.getInstance().getSpawn();
        }
        return player.getWorld().getSpawnLocation();
    }

    private void recallEmail() {
        if (!Settings.recallEmail)
            return;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {

            @Override
            public void run() {
                for (Player player : Utils.getOnlinePlayers()) {
                    if (player.isOnline()) {
                        String name = player.getName().toLowerCase();
                        if (database.isAuthAvailable(name))
                            if (PlayerCache.getInstance().isAuthenticated(name)) {
                                String email = database.getAuth(name).getEmail();
                                if (email == null || email.isEmpty() || email.equalsIgnoreCase("your@email.com"))
                                    messages.send(player, MessageKey.ADD_EMAIL_MESSAGE);
                            }
                    }
                }
            }
        }, 1, 1200 * Settings.delayRecall);
    }

    public String replaceAllInfo(String message, Player player) {
        int playersOnline = Utils.getOnlinePlayers().size();
        message = message.replace("&", "\u00a7");
        message = message.replace("{PLAYER}", player.getName());
        message = message.replace("{ONLINE}", "" + playersOnline);
        message = message.replace("{MAXPLAYERS}", "" + server.getMaxPlayers());
        message = message.replace("{IP}", getIP(player));
        message = message.replace("{LOGINS}", "" + PlayerCache.getInstance().getLogged());
        message = message.replace("{WORLD}", player.getWorld().getName());
        message = message.replace("{SERVER}", server.getServerName());
        message = message.replace("{VERSION}", server.getBukkitVersion());
        message = message.replace("{COUNTRY}", GeoLiteAPI.getCountryName(getIP(player)));
        return message;
    }

    /**
     * Gets a player's real IP through VeryGames method.
     *
     * @param player The player to process.
     */
    @Deprecated
    public void getVerygamesIp(final Player player) {
        final String name = player.getName().toLowerCase();
        String currentIp = player.getAddress().getAddress().getHostAddress();
        if (realIp.containsKey(name)) {
            currentIp = realIp.get(name);
        }
        String sUrl = "http://monitor-1.verygames.net/api/?action=ipclean-real-ip&out=raw&ip=%IP%&port=%PORT%";
        sUrl = sUrl.replace("%IP%", currentIp).replace("%PORT%", "" + player.getAddress().getPort());
        try {
            String result = Resources.toString(new URL(sUrl), Charsets.UTF_8);
            if (!StringUtils.isEmpty(result) && !result.equalsIgnoreCase("error") && !result.contains("error")) {
                currentIp = result;
                realIp.put(name, currentIp);
            }
        } catch (IOException e) {
            ConsoleLogger.showError("Could not fetch Very Games API with URL '" +
                sUrl + "' - " + StringUtils.formatException(e));
        }
    }

    public String getIP(final Player player) {
        final String name = player.getName().toLowerCase();
        String ip = player.getAddress().getAddress().getHostAddress();
        if (realIp.containsKey(name)) {
            ip = realIp.get(name);
        }
        return ip;
    }

    public boolean isLoggedIp(String name, String ip) {
        int count = 0;
        for (Player player : Utils.getOnlinePlayers()) {
            if (ip.equalsIgnoreCase(getIP(player)) && database.isLogged(player.getName().toLowerCase()) && !player.getName().equalsIgnoreCase(name))
                count++;
        }
        return count >= Settings.getMaxLoginPerIp;
    }

    public boolean hasJoinedIp(String name, String ip) {
        int count = 0;
        for (Player player : Utils.getOnlinePlayers()) {
            if (ip.equalsIgnoreCase(getIP(player)) && !player.getName().equalsIgnoreCase(name))
                count++;
        }
        return count >= Settings.getMaxJoinPerIp;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
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
            wrapper.getLogger().severe("AuthMe command handler is not available");
            return false;
        }

        // Handle the command
        return commandHandler.processCommand(sender, commandLabel, args);
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

}
