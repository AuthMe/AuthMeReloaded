package fr.xephi.authme;

import com.earth2me.essentials.Essentials;
import com.maxmind.geoip.LookupService;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.zaxxer.hikari.pool.PoolInitializationException;
import fr.xephi.authme.api.API;
import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.commands.*;
import fr.xephi.authme.converter.Converter;
import fr.xephi.authme.converter.ForceFlatToSqlite;
import fr.xephi.authme.datasource.*;
import fr.xephi.authme.listener.*;
import fr.xephi.authme.plugin.manager.BungeeCordMessage;
import fr.xephi.authme.plugin.manager.CitizensCommunicator;
import fr.xephi.authme.plugin.manager.CombatTagComunicator;
import fr.xephi.authme.plugin.manager.EssSpawn;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.OtherAccounts;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.Spawn;
import net.milkbowl.vault.permission.Permission;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.mcstats.Metrics;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class AuthMe extends JavaPlugin {

    public static Server server;
    public static Logger authmeLogger = Logger.getLogger("AuthMe");
    public static AuthMe authme;
    public Management management;
    public NewAPI api;
    private Utils utils = Utils.getInstance();
    public SendMailSSL mail = null;
    private Settings settings;
    private Messages m;
    public DataManager dataManager;
    public DataSource database;

    private JsonCache playerBackup;
    public OtherAccounts otherAccounts;
    public Permission permission;
    public Essentials ess;
    public Location essentialsSpawn;
    public MultiverseCore multiverse = null;
    public LookupService lookupService = null;
    public CitizensCommunicator citizens;
    public boolean isCitizensActive = false;
    public boolean CombatTag = false;
    public boolean legacyChestShop = false;
    public boolean BungeeCord = false;
    public boolean antibotMod = false;
    public boolean delayedAntiBot = true;
    public ConcurrentHashMap<String, BukkitTask> sessions = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Integer> captcha = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, String> cap = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, String> realIp = new ConcurrentHashMap<>();
    protected static String vgUrl = "http://monitor-1.verygames.net/api/?action=ipclean-real-ip&out=raw&ip=%IP%&port=%PORT%";

    public static AuthMe getInstance() {
        return authme;
    }

    public Settings getSettings() {
        return settings;
    }

    public DataSource getAuthMeDatabase() {
        return database;
    }

    public void setAuthMeDatabase(DataSource database) {
        this.database = database;
    }

    public void setMessages(Messages m) {
        this.m = m;
    }

    public Messages getMessages() {
        return m;
    }

    public CitizensCommunicator getCitizensCommunicator() {
        return citizens;
    }

    @Override
    public void onEnable() {

        // TODO: split the plugin in more modules
        // TODO: remove vault as hard dependency

        server = getServer();
        PluginManager pm = server.getPluginManager();

        // Setup the Logger
        authmeLogger.setParent(this.getLogger());

        // Set the Instance
        authme = this;

        // Setup otherAccounts file
        otherAccounts = OtherAccounts.getInstance();

        // Load settings and custom configurations
        // TODO: new configuration style (more files)
        try {
            settings = new Settings(this);
        } catch (Exception e) {
            ConsoleLogger.showError("Can't load the configuration file... Something went wrong, to avoid security issues the server will shutdown!");
            this.getServer().shutdown();
            return;
        }
        // Configuration Security Warnings
        if (!Settings.isForceSingleSessionEnabled) {
            ConsoleLogger.showError("WARNING!!! By disabling ForceSingleSession, your server protection is inadequate!");
        }
        if (Settings.getSessionTimeout == 0 && Settings.isSessionsEnabled) {
            ConsoleLogger.showError("WARNING!!! You set session timeout to 0, this may cause security issues!");
        }

        // Setup messages
        m = Messages.getInstance();

        // Start the metrics service
        // TODO: add a setting to disable metrics
        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
            ConsoleLogger.info("Metrics started successfully!");
        } catch (IOException e) {
            // Failed to submit the metrics data
            ConsoleLogger.showError("Can't start Metrics! The plugin will work anyway...");
        }

        // Set Console Filter
        if (Settings.removePassword) {
            ConsoleFilter filter = new ConsoleFilter();
            this.getLogger().setFilter(filter);
            Bukkit.getLogger().setFilter(filter);
            Logger.getLogger("Minecraft").setFilter(filter);
            authmeLogger.setFilter(filter);
            // Set Log4J Filter
            try {
                Class.forName("org.apache.logging.log4j.core.Filter");
                setLog4JFilter();
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                ConsoleLogger.info("You're using Minecraft 1.6.x or older, Log4J support will be disabled");
            }
        }

        // AntiBot delay
        if (Settings.enableAntiBot) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

                @Override
                public void run() {
                    delayedAntiBot = false;
                }
            }, 2400);
        }

        // Download GeoIp.dat file
        downloadGeoIp();

        // Load MailApi if needed
        if (!Settings.getmailAccount.isEmpty() && !Settings.getmailPassword.isEmpty()) {
            mail = new SendMailSSL(this);
        }

        // Find Permissions
        checkVault();

        // Check Citizens Version
        citizens = new CitizensCommunicator(this);
        checkCitizens();

        // Check Combat Tag Version
        checkCombatTag();

        // Check Multiverse
        checkMultiverse();

        // Check PerWorldInventories Version
        checkPerWorldInventories();

        // Check ChestShop
        checkChestShop();

        // Check Essentials
        checkEssentials();

        // Do backup on start if enabled
        if (Settings.isBackupActivated && Settings.isBackupOnStart) {
            // Do backup and check return value!
            if (new PerformBackup(this).DoBackup()) {
                ConsoleLogger.info("Backup performed correctly");
            } else {
                ConsoleLogger.showError("Error while performing the backup!");
            }
        }

        // Connect to the database and setup tables
        try {
            setupDatabase();
        } catch (ClassNotFoundException | SQLException | PoolInitializationException ex) {
            ConsoleLogger.writeStackTrace(ex);
            ConsoleLogger.showError("Fatal error occurred during database connection! Authme initialization ABORTED!");
            stopOrUnload();
            return;
        }

        // Setup the inventory backup
        playerBackup = new JsonCache(this);

        // Set the DataManager
        dataManager = new DataManager(this);

        // Setup the new API
        api = new NewAPI(this);
        // Setup the old deprecated API
        new API(this);

        // Setup Management
        management = new Management(this);

        // Bungeecord hook
        if (Settings.bungee) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BungeeCordMessage(this));
        }

        // Legacy chestshop hook
        if (legacyChestShop) {
            pm.registerEvents(new AuthMeChestShopListener(this), this);
            ConsoleLogger.info("Hooked successfully with ChestShop!");
        }

        // Reload support hook
        if (Settings.reloadSupport) {
            int playersOnline = Utils.getOnlinePlayers().size();
            if (database != null) {
                if (playersOnline < 1) {
                    database.purgeLogged();
                } else {
                    for (PlayerAuth auth : database.getLoggedPlayers()) {
                        if (auth == null)
                            continue;
                        auth.setLastLogin(new Date().getTime());
                        database.updateSession(auth);
                        PlayerCache.getInstance().addPlayer(auth);
                    }
                }
            }
        }

        // Register events
        pm.registerEvents(new AuthMePlayerListener(this), this);
        pm.registerEvents(new AuthMeBlockListener(this), this);
        pm.registerEvents(new AuthMeEntityListener(this), this);
        pm.registerEvents(new AuthMeServerListener(this), this);

        // Register commands
        this.getCommand("authme").setExecutor(new AdminCommand(this));
        this.getCommand("register").setExecutor(new RegisterCommand(this));
        this.getCommand("login").setExecutor(new LoginCommand(this));
        this.getCommand("changepassword").setExecutor(new ChangePasswordCommand(this));
        this.getCommand("logout").setExecutor(new LogoutCommand(this));
        this.getCommand("unregister").setExecutor(new UnregisterCommand(this));
        this.getCommand("email").setExecutor(new EmailCommand(this));
        this.getCommand("captcha").setExecutor(new CaptchaCommand(this));
        this.getCommand("converter").setExecutor(new ConverterCommand(this));

        // Purge on start if enabled
        autoPurge();

        // Start Email recall task if needed
        recallEmail();

        // Sponsor messages
        ConsoleLogger.info("AuthMe hooks perfectly with the VERYGAMES server hosting!");
        ConsoleLogger.info("Development builds are available on our jenkins, thanks to f14stelt.");
        ConsoleLogger.info("Do you want a good gameserver? Look at our sponsor GameHosting.it leader in Italy as Game Server Provider!");

        // Successful message
        ConsoleLogger.info("AuthMe " + this.getDescription().getVersion() + " correctly enabled!");
    }

    @Override
    public void onDisable() {
        // Save player data
        Collection<? extends Player> players = Utils.getOnlinePlayers();
        if (players != null) {
            for (Player player : players) {
                this.savePlayer(player);
            }
        }

        // Close the database
        if (database != null) {
            database.close();
        }

        // Do backup on stop if enabled
        if (Settings.isBackupActivated && Settings.isBackupOnStop) {
            Boolean Backup = new PerformBackup(this).DoBackup();
            if (Backup)
                ConsoleLogger.info("Backup performed correctly.");
            else ConsoleLogger.showError("Error while performing the backup!");
        }

        // Disabled correctly
        ConsoleLogger.info("AuthMe " + this.getDescription().getVersion() + " disabled!");
    }

    // Stop/unload the server/plugin as defined in the configuration
    public void stopOrUnload() {
        if (Settings.isStopEnabled) {
            ConsoleLogger.showError("THE SERVER IS GOING TO SHUTDOWN AS DEFINED IN THE CONFIGURATION!");
            AuthMe.getInstance().getServer().shutdown();
        } else {
            server.getPluginManager().disablePlugin(AuthMe.getInstance());
        }
    }

    // Show the exception message and stop/unload the server/plugin as defined in the configuration
    public void stopOrUnload(Exception e) {
        ConsoleLogger.showError(e.getMessage());
        stopOrUnload();
    }

    // Initialize and setup the database
    public void setupDatabase() throws ClassNotFoundException, PoolInitializationException, SQLException {
        // Backend MYSQL - FILE - SQLITE - SQLITEHIKARI
        int accounts;
        switch (Settings.getDataSource) {
            case FILE:
                database = new FlatFile();
                break;
            case MYSQL:
                database = new MySQL();
                break;
            case SQLITE:
                database = new SQLite();
                accounts = database.getAccountsRegistered();
                if (accounts >= 4000)
                    ConsoleLogger.showError("YOU'RE USING THE SQLITE DATABASE WITH " + accounts + "+ ACCOUNTS, FOR BETTER PERFORMANCES, PLEASE UPGRADE TO MYSQL!!");
                break;
            case SQLITEHIKARI:
                database = new SQLite_HIKARI();
                accounts = database.getAccountsRegistered();
                if (accounts >= 8000)
                    ConsoleLogger.showError("YOU'RE USING THE SQLITE DATABASE WITH " + accounts + "+ ACCOUNTS, FOR BETTER PERFORMANCES, PLEASE UPGRADE TO MYSQL!!");
                break;
        }

        if (Settings.isCachingEnabled) {
            database = new CacheDataSource(this, database);
        }

        database = new DatabaseCalls(database);

        if (Settings.getDataSource == DataSource.DataSourceType.FILE) {
            Converter converter = new ForceFlatToSqlite(database, this);
            getServer().getScheduler().runTaskAsynchronously(this, converter);
            ConsoleLogger.showError("FlatFile backend has been detected and is now deprecated, next time server starts up, it will be changed to SQLite... Conversion will be started Asynchronously, it will not drop down your performance !");
            ConsoleLogger.showError("If you want to keep FlatFile, set file again into config at backend, but this message and this change will appear again at the next restart");
        }
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

    // Check the presence of the Vault plugin and a permissions provider
    public void checkVault() {
        if (server.getPluginManager().isPluginEnabled("Vault")) {
            RegisteredServiceProvider<Permission> permissionProvider = server.getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
            if (permissionProvider != null) {
                permission = permissionProvider.getProvider();
                ConsoleLogger.info("Vault detected, hooking with the " + permission.getName() + " permissions system...");
            } else {
                ConsoleLogger.showError("Vault detected, but I can't find any permissions plugin to hook with!");
            }
        } else {
            permission = null;
        }
    }

    // Check the version of the ChestShop plugin
    public void checkChestShop() {
        if (Settings.legacyChestShop && server.getPluginManager().isPluginEnabled("ChestShop")) {
            String rawver = com.Acrobot.ChestShop.ChestShop.getVersion();
            double version;
            try {
                version = Double.valueOf(rawver.split(" ")[0]);
            } catch (NumberFormatException nfe) {
                try {
                    version = Double.valueOf(rawver.split("t")[0]);
                } catch (NumberFormatException nfee) {
                    legacyChestShop = false;
                    return;
                }
            }
            if (version >= 3.813) {
                return;
            }
            if (version < 3.50) {
                ConsoleLogger.showError("Please Update your ChestShop version! Bugs may occur!");
                return;
            }
            legacyChestShop = true;
        } else {
            legacyChestShop = false;
        }
    }

    // Check PerWorldInventories version
    public void checkPerWorldInventories() {
        if (server.getPluginManager().isPluginEnabled("PerWorldInventories")) {
            double version = 0;
            String ver = server.getPluginManager().getPlugin("PerWorldInventories").getDescription().getVersion();
            try {
                version = Double.valueOf(ver.split(" ")[0]);
            } catch (NumberFormatException nfe) {
                try {
                    version = Double.valueOf(ver.split("t")[0]);
                } catch (NumberFormatException ignore) {
                }
            }
            if (version < 1.57) {
                ConsoleLogger.showError("Please Update your PerWorldInventories version! INVENTORY WIPE may occur!");
            }
        }
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
            } catch (Exception | NoClassDefFoundError ingnored) {
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
    public void checkCombatTag() {
        this.CombatTag = this.getServer().getPluginManager().isPluginEnabled("CombatTag");
    }

    // Check if Citizens is active
    public void checkCitizens() {
        this.isCitizensActive = this.getServer().getPluginManager().isPluginEnabled("Citizens");
    }

    // Check if a player/command sender have a permission
    public boolean authmePermissible(Player player, String perm) {
        if (player.hasPermission(perm)) {
            return true;
        } else if (permission != null) {
            return permission.playerHas(player, perm);
        }
        return false;
    }

    public boolean authmePermissible(CommandSender sender, String perm) {
        if (sender.hasPermission(perm)) {
            return true;
        } else if (permission != null) {
            return permission.has(sender, perm);
        }
        return false;
    }

    // Save Player Data
    public void savePlayer(Player player) {
        if ((citizens.isNPC(player)) || (Utils.getInstance().isUnrestricted(player)) || (CombatTagComunicator.isNPC(player))) {
            return;
        }
        String name = player.getName().toLowerCase();
        if (PlayerCache.getInstance().isAuthenticated(name) && !player.isDead() && Settings.isSaveQuitLocationEnabled) {
            final PlayerAuth auth = new PlayerAuth(player.getName().toLowerCase(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getWorld().getName(), player.getName());
            database.updateQuitLoc(auth);
        }
        if (LimboCache.getInstance().hasLimboPlayer(name)) {
            LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
            if (Settings.protectInventoryBeforeLogInEnabled) {
                player.getInventory().setArmorContents(limbo.getArmour());
                player.getInventory().setContents(limbo.getInventory());
            }
            if (!Settings.noTeleport) {
                player.teleport(limbo.getLoc());
            }
            this.utils.addNormal(player, limbo.getGroup());
            player.setOp(limbo.getOperator());
            limbo.getTimeoutTaskId().cancel();
            LimboCache.getInstance().deleteLimboPlayer(name);
            if (this.playerBackup.doesCacheExist(player)) {
                this.playerBackup.removeCache(player);
            }
        }
        PlayerCache.getInstance().removePlayer(name);
        database.setUnlogged(name);
        player.saveData();
    }

    // Select the player to kick when a vip player join the server when full
    public Player generateKickPlayer(Collection<? extends Player> collection) {
        Player player = null;
        for (Player p : collection) {
            if (!(authmePermissible(p, "authme.vip"))) {
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
            dataManager.purgeEssentials(cleared); // name to UUID convertion needed with latest versions
        if (Settings.purgePlayerDat)
            dataManager.purgeDat(cleared); // name to UUID convertion needed with latest versions of MC
        if (Settings.purgeLimitedCreative)
            dataManager.purgeLimitedCreative(cleared);
        if (Settings.purgeAntiXray)
            dataManager.purgeAntiXray(cleared); // IDK if it uses UUID or names... (Actually it purges only names!)
        if (Settings.purgePermissions)
            dataManager.purgePermissions(cleared, permission);
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

    // Return the default spawnpoint of a world
    private Location getDefaultSpawn(World world) {
        return world.getSpawnLocation();
    }

    // Return the multiverse spawnpoint of a world
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

    // Return the essentials spawnpoint
    private Location getEssentialsSpawn() {
        if (essentialsSpawn != null) {
            return essentialsSpawn;
        }
        return null;
    }

    // Return the authme soawnpoint
    private Location getAuthMeSpawn(Player player) {
        if ((!database.isAuthAvailable(player.getName().toLowerCase()) || !player.hasPlayedBefore()) && (Spawn.getInstance().getFirstSpawn() != null)) {
            return Spawn.getInstance().getFirstSpawn();
        }
        if (Spawn.getInstance().getSpawn() != null) {
            return Spawn.getInstance().getSpawn();
        }
        return player.getWorld().getSpawnLocation();
    }

    // Download GeoIp data
    public void downloadGeoIp() {
        ConsoleLogger.info("[LICENSE] This product uses data from the GeoLite API created by MaxMind, available at http://www.maxmind.com");
        File file = new File(getDataFolder(), "GeoIP.dat");
        try {
            if (file.exists()) {
                if (lookupService == null) {
                    lookupService = new LookupService(file);
                }
            } else {
                String url = "http://geolite.maxmind.com/download/geoip/database/GeoLiteCountry/GeoIP.dat.gz";
                URL downloadUrl = new URL(url);
                URLConnection conn = downloadUrl.openConnection();
                conn.setConnectTimeout(10000);
                conn.connect();
                InputStream input = conn.getInputStream();
                if (url.endsWith(".gz")) {
                    input = new GZIPInputStream(input);
                }
                OutputStream output = new FileOutputStream(file);
                byte[] buffer = new byte[2048];
                int length = input.read(buffer);
                while (length >= 0) {
                    output.write(buffer, 0, length);
                    length = input.read(buffer);
                }
                output.close();
                input.close();
            }
        } catch (Exception e) {
            ConsoleLogger.writeStackTrace(e);
        }
    }

    // TODO: Need to review the code below!

    public String getCountryCode(String ip) {
        if (lookupService != null) {
            return lookupService.getCountry(ip).getCode();
        }
        return "--";
    }

    public String getCountryName(String ip) {
        if (lookupService != null) {
            return lookupService.getCountry(ip).getName();
        }
        return "N/A";
    }

    public void switchAntiBotMod(boolean mode) {
        this.antibotMod = mode;
        Settings.switchAntiBotMod(mode);
    }

    private void recallEmail() {
        if (!Settings.recallEmail)
            return;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {

            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isOnline()) {
                        String name = player.getName().toLowerCase();
                        if (database.isAuthAvailable(name))
                            if (PlayerCache.getInstance().isAuthenticated(name)) {
                                String email = database.getAuth(name).getEmail();
                                if (email == null || email.isEmpty() || email.equalsIgnoreCase("your@email.com"))
                                    m.send(player, "add_email");
                            }
                    }
                }
            }
        }, 1, 1200 * Settings.delayRecall);
    }

    public String replaceAllInfos(String message, Player player) {
        int playersOnline = Utils.getOnlinePlayers().size();
        message = message.replace("&", "\u00a7");
        message = message.replace("{PLAYER}", player.getName());
        message = message.replace("{ONLINE}", "" + playersOnline);
        message = message.replace("{MAXPLAYERS}", "" + this.getServer().getMaxPlayers());
        message = message.replace("{IP}", getIP(player));
        message = message.replace("{LOGINS}", "" + PlayerCache.getInstance().getLogged());
        message = message.replace("{WORLD}", player.getWorld().getName());
        message = message.replace("{SERVER}", this.getServer().getServerName());
        message = message.replace("{VERSION}", this.getServer().getBukkitVersion());
        message = message.replace("{COUNTRY}", this.getCountryName(getIP(player)));
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
            if (getVeryGamesIP(player) != null)
                ip = getVeryGamesIP(player);
        return ip;
    }

    public boolean isLoggedIp(String name, String ip) {
        int count = 0;
        for (Player player : this.getServer().getOnlinePlayers()) {
            if (ip.equalsIgnoreCase(getIP(player)) && database.isLogged(player.getName().toLowerCase()) && !player.getName().equalsIgnoreCase(name))
                count++;
        }
        return count >= Settings.getMaxLoginPerIp;
    }

    public boolean hasJoinedIp(String name, String ip) {
        int count = 0;
        for (Player player : this.getServer().getOnlinePlayers()) {
            if (ip.equalsIgnoreCase(getIP(player)) && !player.getName().equalsIgnoreCase(name))
                count++;
        }
        return count >= Settings.getMaxJoinPerIp;
    }

    /**
     * Get Player real IP through VeryGames method
     *
     * @param player player
     */
    @Deprecated
    public String getVeryGamesIP(Player player) {
        String realIP = player.getAddress().getAddress().getHostAddress();
        String sUrl = vgUrl;
        sUrl = sUrl.replace("%IP%", player.getAddress().getAddress().getHostAddress()).replace("%PORT%", "" + player.getAddress().getPort());
        try {
            URL url = new URL(sUrl);
            URLConnection urlc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
            String inputLine = in.readLine();
            if (inputLine != null && !inputLine.isEmpty() && !inputLine.equalsIgnoreCase("error") && !inputLine.contains("error")) {
                realIP = inputLine;
            }
        } catch (Exception ignored) {
        }
        return realIP;
    }


}
