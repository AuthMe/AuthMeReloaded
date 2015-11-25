package fr.xephi.authme;

import com.comphenix.protocol.ProtocolLibrary;
import com.earth2me.essentials.Essentials;
import com.onarandombox.MultiverseCore.MultiverseCore;
import fr.xephi.authme.api.API;
import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.converter.Converter;
import fr.xephi.authme.converter.ForceFlatToSqlite;
import fr.xephi.authme.datasource.*;
import fr.xephi.authme.hooks.BungeeCordMessage;
import fr.xephi.authme.hooks.EssSpawn;
import fr.xephi.authme.listener.*;
import fr.xephi.authme.modules.ModuleManager;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.OtherAccounts;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.Spawn;
import fr.xephi.authme.util.GeoLiteAPI;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
import net.minelink.ctplus.CombatTagPlus;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * The AuthMe main class.
 */
public class AuthMe extends JavaPlugin {

    /**
     * Defines the name of the plugin.
     */
    private static final String PLUGIN_NAME = "AuthMeReloaded";
    /**
     * Defines the current AuthMeReloaded version name.
     */
    private static final String PLUGIN_VERSION_NAME = "5.1-SNAPSHOT";
    /**
     * Defines the current AuthMeReloaded version code.
     */
    // TODO: Increase this number by one when an update is release
    private static final int PLUGIN_VERSION_CODE = 100;

    private static AuthMe plugin;
    private static Server server;

    public Management management;
    public NewAPI api;
    public SendMailSSL mail;
    public DataManager dataManager;
    public DataSource database;
    public OtherAccounts otherAccounts;
    public Location essentialsSpawn;

    // Hooks TODO: Move into modules
    public Essentials ess;
    public MultiverseCore multiverse;
    public CombatTagPlus combatTagPlus;
    public AuthMeInventoryPacketAdapter inventoryProtector;

    // Data maps and stuff
    // TODO: Move into a manager
    public final ConcurrentHashMap<String, BukkitTask> sessions = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, Integer> captcha = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, String> cap = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, String> realIp = new ConcurrentHashMap<>();

    // If cache is enabled, prevent any connection before the players data caching is completed.
    // TODO: Move somewhere
    private boolean canConnect = true;

    private CommandHandler commandHandler = null;
    private PermissionsManager permsMan = null;
    private Settings settings;
    private Messages messages;
    private JsonCache playerBackup;
    private ModuleManager moduleManager;

    /**
     * Returns the plugin's instance.
     *
     * @return AuthMe
     */
    public static AuthMe getInstance() {
        return plugin;
    }

    /**
     * Get the plugin's name.
     *
     * @return Plugin name.
     */
    public static String getPluginName() {
        return PLUGIN_NAME;
    }

    /**
     * Get the current installed AuthMeReloaded version name.
     *
     * @return The version name of the currently installed AuthMeReloaded instance.
     */
    public static String getVersionName() {
        return PLUGIN_VERSION_NAME;
    }

    /**
     * Get the current installed AuthMeReloaded version code.
     *
     * @return The version code of the currently installed AuthMeReloaded instance.
     */
    public static int getVersionCode() {
        return PLUGIN_VERSION_CODE;
    }

    /**
     * Returns the plugin's Settings.
     *
     * @return Settings
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Returns the Messages instance.
     *
     * @return Messages
     */

    public Messages getMessages() {
        return messages;
    }

    /**
     * Set the Messages instance.
     *
     * @param m Messages
     */
    public void setMessages(Messages m) {
        this.messages = m;
    }

    /**
     * Returns if players are allowed to join the server.
     *
     * @return boolean
     */
    public boolean canConnect() {
        return canConnect;
    }

    /**
     * Define if players are allowed to join the server.
     *
     * @param canConnect boolean
     */
    public void setCanConnect(boolean canConnect) {
        this.canConnect = canConnect;
    }

    /**
     * Method called when the server enables the plugin.
     *
     * @see org.bukkit.plugin.Plugin#onEnable()
     */
    @Override
    public void onEnable() {
        // Set various instances
        server = getServer();
        plugin = this;

        // Set up the permissions manager
        setupPermissionsManager();

        // Set up and initialize the command handler
        setupCommandHandler();

        // Set up the module manager
        setupModuleManager();

        // Load settings and custom configurations, if it fails, stop the server due to security reasons.
        if (loadSettings()) {
            server.shutdown();
            setEnabled(false);
            return;
        }

        // Setup otherAccounts file
        this.otherAccounts = OtherAccounts.getInstance();

        // Setup messages
        this.messages = Messages.getInstance();

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
        pluginManager.registerEvents(new AuthMeBlockListener(this), this);
        pluginManager.registerEvents(new AuthMeEntityListener(this), this);
        pluginManager.registerEvents(new AuthMeServerListener(this), this);

        // Try to register 1.6 player listeners
        try {
            Class.forName("org.bukkit.event.player.PlayerEditBookEvent");
            pluginManager.registerEvents(new AuthMePlayerListener16(this), this);
        } catch (ClassNotFoundException ignore) {
        }

        // Try to register 1.8 player listeners
        try {
            Class.forName("org.bukkit.event.player.PlayerInteractAtEntityEvent");
            pluginManager.registerEvents(new AuthMePlayerListener18(this), this);
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

    /**
     * Set up the API. This sets up the new and the old API.
     */
    @SuppressWarnings("deprecation")
    private void setupApi() {
        // Set up the API
        api = new NewAPI(this);

        // Setup the old deprecated API
        new API(this);
    }

    /**
     * Set up the command handler.
     */
    private void setupCommandHandler() {
        this.commandHandler = new CommandHandler(false);
        this.commandHandler.init();
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

    // Show the exception message and stop/unload the server/plugin as defined
    // in the configuration

    /**
     * Method onDisable.
     *
     * @see org.bukkit.plugin.Plugin#onDisable()
     */
    @Override
    public void onDisable() {
        // Save player data
        Collection<? extends Player> players = Utils.getOnlinePlayers();
        if (players != null) {
            for (Player player : players) {
                this.savePlayer(player);
            }
        }

        // Do backup on stop if enabled
        new PerformBackup(plugin).doBackup(PerformBackup.BackupCause.STOP);

        // Unload modules
        moduleManager.unloadModules();

        // Close the database
        if (database != null) {
            database.close();
        }

        // Disabled correctly
        ConsoleLogger.info("AuthMe " + this.getDescription().getVersion() + " disabled!");
    }

    // Initialize and setup the database

    // Stop/unload the server/plugin as defined in the configuration
    public void stopOrUnload() {
        if (Settings.isStopEnabled) {
            ConsoleLogger.showError("THE SERVER IS GOING TO SHUTDOWN AS DEFINED IN THE CONFIGURATION!");
            server.shutdown();
        } else {
            server.getPluginManager().disablePlugin(AuthMe.getInstance());
        }
    }

    /**
     * Method setupDatabase.
     */
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
                    if (accounts >= 4000)
                        ConsoleLogger.showError("YOU'RE USING THE SQLITE DATABASE WITH " + accounts + "+ ACCOUNTS, FOR BETTER PERFORMANCES, PLEASE UPGRADE TO MYSQL!!");
                }
            });
        }

        if (Settings.isCachingEnabled) {
            database = new CacheDataSource(this, database);
        } else {
            database = new DatabaseCalls(database);
        }

        if (Settings.getDataSource == DataSource.DataSourceType.FILE) {
            Converter converter = new ForceFlatToSqlite(database, this);
            server.getScheduler().runTaskAsynchronously(this, converter);
            ConsoleLogger.showError("FlatFile backend has been detected and is now deprecated, next time server starts up, it will be changed to SQLite... Conversion will be started Asynchronously, it will not drop down your performance !");
            ConsoleLogger.showError("If you want to keep FlatFile, set file again into config at backend, but this message and this change will appear again at the next restart");
        }
    }

    /**
     * Set up the permissions manager.
     */
    public void setupPermissionsManager() {
        this.permsMan = new PermissionsManager(Bukkit.getServer(), this, getLogger());
        this.permsMan.setup();
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
                ProtocolLibrary.getProtocolManager().removePacketListener(inventoryProtector);
                inventoryProtector = null;
            }
        }
    }

    // Save Player Data
    public void savePlayer(Player player) {
        if ((Utils.isNPC(player)) || (Utils.isUnrestricted(player))) {
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
        player.saveData();
    }

    // Select the player to kick when a vip player join the server when full
    public Player generateKickPlayer(Collection<? extends Player> collection) {
        Player player = null;
        for (Player p : collection) {
            if (!getPermissionsManager().hasPermission(p, "authme.vip")) {
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
        if ((!database.isAuthAvailable(player.getName().toLowerCase()) || !player.hasPlayedBefore()) && (Spawn.getInstance().getFirstSpawn() != null)) {
            return Spawn.getInstance().getFirstSpawn();
        }
        if (Spawn.getInstance().getSpawn() != null) {
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
                                    messages.send(player, "add_email");
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

    public String getIP(Player player) {
        String name = player.getName().toLowerCase();
        String ip = player.getAddress().getAddress().getHostAddress();
        if (Settings.bungee) {
            if (realIp.containsKey(name))
                ip = realIp.get(name);
        }
        if (Settings.checkVeryGames)
            if (getVeryGamesIp(player) != null)
                ip = getVeryGamesIp(player);
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
     * Gets a player's real IP through VeryGames method.
     *
     * @param player The player to process.
     *
     * @return The real IP of the player.
     */
    // TODO: Cache the result
    @Deprecated
    public String getVeryGamesIp(Player player) {
        String realIP = player.getAddress().getAddress().getHostAddress();
        String sUrl = "http://monitor-1.verygames.net/api/?action=ipclean-real-ip&out=raw&ip=%IP%&port=%PORT%";
        sUrl = sUrl.replace("%IP%", player.getAddress().getAddress().getHostAddress())
                   .replace("%PORT%", "" + player.getAddress().getPort());
        try {
            URL url = new URL(sUrl);
            URLConnection urlCon = url.openConnection();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(urlCon.getInputStream()))) {
                String inputLine = in.readLine();
                if (!StringUtils.isEmpty(inputLine) && !inputLine.equalsIgnoreCase("error")
                        && !inputLine.contains("error")) {
                    realIP = inputLine;
                }
            } catch (IOException e) {
                ConsoleLogger.showError("Could not read from Very Games API - " + StringUtils.formatException(e));
            }
        } catch (IOException e) {
            ConsoleLogger.showError("Could not fetch Very Games API with URL '" + sUrl + "' - "
                + StringUtils.formatException(e));
        }
        return realIP;
    }

    public CommandHandler getCommandHandler() {
        return this.commandHandler;
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
        // Get the command handler, and make sure it's valid
        CommandHandler commandHandler = this.getCommandHandler();
        if (commandHandler == null)
            return false;

        // Handle the command, return the result
        return commandHandler.onCommand(sender, cmd, commandLabel, args);
    }

    /**
     * Return the management instance.
     */
    public Management getManagement() {
        return management;
    }

}
