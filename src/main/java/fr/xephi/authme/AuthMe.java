package fr.xephi.authme;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

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

import com.earth2me.essentials.Essentials;
import com.maxmind.geoip.LookupService;
import com.onarandombox.MultiverseCore.MultiverseCore;

import fr.xephi.authme.api.API;
import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.FileCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.commands.AdminCommand;
import fr.xephi.authme.commands.CaptchaCommand;
import fr.xephi.authme.commands.ChangePasswordCommand;
import fr.xephi.authme.commands.ConverterCommand;
import fr.xephi.authme.commands.EmailCommand;
import fr.xephi.authme.commands.LoginCommand;
import fr.xephi.authme.commands.LogoutCommand;
import fr.xephi.authme.commands.RegisterCommand;
import fr.xephi.authme.commands.UnregisterCommand;
import fr.xephi.authme.converter.Converter;
import fr.xephi.authme.converter.ForceFlatToSqlite;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DatabaseCalls;
import fr.xephi.authme.datasource.FlatFile;
import fr.xephi.authme.datasource.MySQL;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.listener.AuthMeBlockListener;
import fr.xephi.authme.listener.AuthMeChestShopListener;
import fr.xephi.authme.listener.AuthMeEntityListener;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.listener.AuthMeServerListener;
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

public class AuthMe extends JavaPlugin {

    public DataSource database = null;
    private Settings settings;
    private Messages m;
    public OtherAccounts otherAccounts;
    public static Server server;
    public static Logger authmeLogger = Logger.getLogger("AuthMe");
    public static AuthMe authme;
    public Permission permission;
    private Utils utils = Utils.getInstance();
    private FileCache playerBackup = new FileCache(this);
    public CitizensCommunicator citizens;
    public boolean isCitizensActive = false;
    public SendMailSSL mail = null;
    public boolean CombatTag = false;
    public double ChestShop = 0;
    public boolean BungeeCord = false;
    public Essentials ess;
    public NewAPI api;
    public Management management;
    public ConcurrentHashMap<String, Integer> captcha = new ConcurrentHashMap<String, Integer>();
    public ConcurrentHashMap<String, String> cap = new ConcurrentHashMap<String, String>();
    public ConcurrentHashMap<String, String> realIp = new ConcurrentHashMap<String, String>();
    public MultiverseCore multiverse = null;
    public Location essentialsSpawn;
    public LookupService ls = null;
    public boolean antibotMod = false;
    public boolean delayedAntiBot = true;
    protected static String vgUrl = "http://monitor-1.verygames.net/api/?action=ipclean-real-ip&out=raw&ip=%IP%&port=%PORT%";
    public DataManager dataManager;
    public ConcurrentHashMap<String, BukkitTask> sessions = new ConcurrentHashMap<String, BukkitTask>();

    public Settings getSettings() {
        return settings;
    }

    public DataSource getAuthMeDatabase() {
        return database;
    }

    public void setAuthMeDatabase(DataSource database) {
        this.database = database;
    }

    @Override
    public void onEnable() {
        authme = this;

        authmeLogger.setParent(this.getLogger());

        try {
            settings = new Settings(this);
        } catch (Exception e) {
            ConsoleLogger.showError("Can't load the configuration file... Something went wrong, to avoid security issues the server will shutdown!");
            this.getServer().shutdown();
            return;
        }

        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
            ConsoleLogger.info("Metrics started successfully!");
        } catch (IOException e) {
            // Failed to submit the metrics data
            ConsoleLogger.showError("Can't start Metrics! The plugin will work anyway...");
        }

        citizens = new CitizensCommunicator(this);

        if (Settings.enableAntiBot) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

                @Override
                public void run() {
                    delayedAntiBot = false;
                }
            }, 2400);
        }

        m = Messages.getInstance();

        otherAccounts = OtherAccounts.getInstance();

        server = getServer();

        // Find Permissions
        checkVault();

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
            } catch (ClassNotFoundException e) {
                ConsoleLogger.info("You're using Minecraft 1.6.x or older, Log4J support will be disabled");
            } catch (NoClassDefFoundError e) {
                ConsoleLogger.info("You're using Minecraft 1.6.x or older, Log4J support will be disabled");
            }
        }

        // Load MailApi
        if (!Settings.getmailAccount.isEmpty() && !Settings.getmailPassword.isEmpty())
            mail = new SendMailSSL(this);

        // Check Citizens Version
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

        /*
         * Back style on start if avalaible
         */
        if (Settings.isBackupActivated && Settings.isBackupOnStart) {
            Boolean Backup = new PerformBackup(this).DoBackup();
            if (Backup)
                ConsoleLogger.info("Backup performed correctly");
            else ConsoleLogger.showError("Error while performing the backup!");
        }

        setupDatabase();

        dataManager = new DataManager(this);

        // Setup API
        api = new NewAPI(this);
        new API(this);

        // Setup Management
        management = new Management(this);

        PluginManager pm = getServer().getPluginManager();
        if (Settings.bungee) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BungeeCordMessage(this));
        }

        pm.registerEvents(new AuthMePlayerListener(this), this);
        pm.registerEvents(new AuthMeBlockListener(this), this);
        pm.registerEvents(new AuthMeEntityListener(this), this);
        pm.registerEvents(new AuthMeServerListener(this), this);
        if (ChestShop != 0) {
            pm.registerEvents(new AuthMeChestShopListener(this), this);
            ConsoleLogger.info("Hooked successfully with ChestShop!");
        }

        this.getCommand("authme").setExecutor(new AdminCommand(this));
        this.getCommand("register").setExecutor(new RegisterCommand(this));
        this.getCommand("login").setExecutor(new LoginCommand(this));
        this.getCommand("changepassword").setExecutor(new ChangePasswordCommand(this));
        this.getCommand("logout").setExecutor(new LogoutCommand(this));
        this.getCommand("unregister").setExecutor(new UnregisterCommand(this));
        this.getCommand("email").setExecutor(new EmailCommand(this));
        this.getCommand("captcha").setExecutor(new CaptchaCommand(this));
        this.getCommand("converter").setExecutor(new ConverterCommand(this));

        if (!Settings.isForceSingleSessionEnabled) {
            ConsoleLogger.showError("WARNING!!! By disabling ForceSingleSession, your server protection is inadequate!");
        }

        if (Settings.getSessionTimeout == 0 && Settings.isSessionsEnabled) {
            ConsoleLogger.showError("WARNING!!! You set session timeout to 0, this may cause security issues!");
        }

        if (Settings.reloadSupport) {
            try {
                int playersOnline = 0;
                try {
                    if (Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).getReturnType() == Collection.class)
                        playersOnline = ((Collection<?>) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0])).size();
                    else playersOnline = ((Player[]) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0])).length;
                } catch (Exception ex) {
                }
                if (playersOnline < 1) {
                    try {
                        database.purgeLogged();
                    } catch (NullPointerException npe) {
                    }
                } else {
                    for (PlayerAuth auth : database.getLoggedPlayers()) {
                        if (auth == null)
                            continue;
                        auth.setLastLogin(new Date().getTime());
                        database.updateSession(auth);
                        PlayerCache.getInstance().addPlayer(auth);
                    }
                }
            } catch (Exception ex) {
            }
        }

        autoPurge();

        // Download GeoIp.dat file
        downloadGeoIp();

        // Start Email recall task if needed
        recallEmail();

        // Sponsor message
        ConsoleLogger.info("AuthMe hooks perfectly with the VERYGAMES server hosting!");
        ConsoleLogger.info("AuthMe builds are available on jenkins, thanks to our sponsor GameHosting.it - leader in Italy in Game Server Provider");

        ConsoleLogger.info("AuthMe " + this.getDescription().getVersion() + " correctly enabled!");
    }

    private void setLog4JFilter() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

            @Override
            public void run() {
                org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
                coreLogger.addFilter(new Log4JFilter());
            }
        });
    }

    public void checkVault() {
        if (this.getServer().getPluginManager().getPlugin("Vault") != null && this.getServer().getPluginManager().getPlugin("Vault").isEnabled()) {
            RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
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

    public void checkChestShop() {
        if (!Settings.chestshop) {
            this.ChestShop = 0;
            return;
        }
        if (this.getServer().getPluginManager().getPlugin("ChestShop") != null && this.getServer().getPluginManager().getPlugin("ChestShop").isEnabled()) {
            try {
                String ver = com.Acrobot.ChestShop.ChestShop.getVersion();
                try {
                    double version = Double.valueOf(ver.split(" ")[0]);
                    if (version >= 3.50) {
                        this.ChestShop = version;
                    } else {
                        ConsoleLogger.showError("Please Update your ChestShop version! Bugs may occur!");
                    }
                } catch (NumberFormatException nfe) {
                    try {
                        double version = Double.valueOf(ver.split("t")[0]);
                        if (version >= 3.50) {
                            this.ChestShop = version;
                        } else {
                            ConsoleLogger.showError("Please Update your ChestShop version! Bugs may occur!");
                        }
                    } catch (NumberFormatException nfee) {
                    }
                }
            } catch (Exception e) {
            }
        } else {
            this.ChestShop = 0;
        }
    }

    public void checkPerWorldInventories() {
        if (this.getServer().getPluginManager().getPlugin("PerWorldInventories") != null && this.getServer().getPluginManager().getPlugin("PerWorldInventories").isEnabled()) {
            try {
                String ver = Bukkit.getServer().getPluginManager().getPlugin("PerWorldInventories").getDescription().getVersion();
                try {
                    double version = Double.valueOf(ver.split(" ")[0]);
                    if (version < 1.57)
                        ConsoleLogger.showError("Please Update your PerWorldInventories version! INVENTORY WIPE may occur!");
                } catch (NumberFormatException nfe) {
                    try {
                        double version = Double.valueOf(ver.split("t")[0]);
                        if (version < 1.57)
                            ConsoleLogger.showError("Please Update your PerWorldInventories version! INVENTORY WIPE may occur!");
                    } catch (NumberFormatException nfee) {
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    public void checkMultiverse() {
        if (!Settings.multiverse) {
            multiverse = null;
            return;
        }
        if (this.getServer().getPluginManager().getPlugin("Multiverse-Core") != null && this.getServer().getPluginManager().getPlugin("Multiverse-Core").isEnabled()) {
            try {
                multiverse = (MultiverseCore) this.getServer().getPluginManager().getPlugin("Multiverse-Core");
                ConsoleLogger.info("Hooked correctly with Multiverse-Core");
            } catch (NullPointerException npe) {
                multiverse = null;
            } catch (ClassCastException cce) {
                multiverse = null;
            } catch (NoClassDefFoundError ncdfe) {
                multiverse = null;
            }
        } else {
            multiverse = null;
        }
    }

    public void checkEssentials() {
        if (this.getServer().getPluginManager().getPlugin("Essentials") != null && this.getServer().getPluginManager().getPlugin("Essentials").isEnabled()) {
            try {
                ess = (Essentials) this.getServer().getPluginManager().getPlugin("Essentials");
                ConsoleLogger.info("Hooked correctly with Essentials");
            } catch (NullPointerException npe) {
                ess = null;
            } catch (ClassCastException cce) {
                ess = null;
            } catch (NoClassDefFoundError ncdfe) {
                ess = null;
            }
        } else {
            ess = null;
        }
        if (this.getServer().getPluginManager().getPlugin("EssentialsSpawn") != null && this.getServer().getPluginManager().getPlugin("EssentialsSpawn").isEnabled()) {
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

    public void checkCombatTag() {
        if (this.getServer().getPluginManager().getPlugin("CombatTag") != null && this.getServer().getPluginManager().getPlugin("CombatTag").isEnabled()) {
            this.CombatTag = true;
        } else {
            this.CombatTag = false;
        }
    }

    public void checkCitizens() {
        if (this.getServer().getPluginManager().getPlugin("Citizens") != null && this.getServer().getPluginManager().getPlugin("Citizens").isEnabled())
            this.isCitizensActive = true;
        else this.isCitizensActive = false;
    }

    @Override
    public void onDisable() {
        int playersOnline = 0;
        try {
            if (Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).getReturnType() == Collection.class)
                playersOnline = ((Collection<?>) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0])).size();
            else playersOnline = ((Player[]) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0])).length;
        } catch (NoSuchMethodException ex) {
        } // can never happen
        catch (InvocationTargetException ex) {
        } // can also never happen
        catch (IllegalAccessException ex) {
        } // can still never happen
        if (playersOnline != 0)
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.savePlayer(player);
            }

        if (database != null) {
            try {
                database.close();
            } catch (Exception e) {
            }
        }

        if (Settings.isBackupActivated && Settings.isBackupOnStop) {
            Boolean Backup = new PerformBackup(this).DoBackup();
            if (Backup)
                ConsoleLogger.info("Backup performed correctly.");
            else ConsoleLogger.showError("Error while performing the backup!");
        }
        ConsoleLogger.info("AuthMe " + this.getDescription().getVersion() + " disabled!");
    }

    public static AuthMe getInstance() {
        return authme;
    }

    public void savePlayer(Player player) {
        try {
            if ((citizens.isNPC(player)) || (Utils.getInstance().isUnrestricted(player)) || (CombatTagComunicator.isNPC(player))) {
                return;
            }
        } catch (Exception e) {
        }
        try {
            String name = player.getName().toLowerCase();
            if (PlayerCache.getInstance().isAuthenticated(name) && !player.isDead() && Settings.isSaveQuitLocationEnabled) {
                final PlayerAuth auth = new PlayerAuth(player.getName().toLowerCase(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getWorld().getName(), player.getName());
                database.updateQuitLoc(auth);
            }
            if (LimboCache.getInstance().hasLimboPlayer(name)) {
                LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
                if (Settings.protectInventoryBeforeLogInEnabled.booleanValue()) {
                    player.getInventory().setArmorContents(limbo.getArmour());
                    player.getInventory().setContents(limbo.getInventory());
                }
                if (!Settings.noTeleport)
                    player.teleport(limbo.getLoc());
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
        } catch (Exception ex) {
        }
    }

    public CitizensCommunicator getCitizensCommunicator() {
        return citizens;
    }

    public void setMessages(Messages m) {
        this.m = m;
    }

    public Messages getMessages() {
        return m;
    }

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

    public boolean authmePermissible(Player player, String perm) {
        if (player.hasPermission(perm))
            return true;
        else if (permission != null) {
            return permission.playerHas(player, perm);
        }
        return false;
    }

    public boolean authmePermissible(CommandSender sender, String perm) {
        if (sender.hasPermission(perm))
            return true;
        else if (permission != null) {
            return permission.has(sender, perm);
        }
        return false;
    }

    private void autoPurge() {
        if (!Settings.usePurge) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -(Settings.purgeDelay));
        long until = calendar.getTimeInMillis();
        List<String> cleared = database.autoPurgeDatabase(until);
        if (cleared == null)
            return;
        if (cleared.isEmpty())
            return;
        ConsoleLogger.info("AutoPurging the Database: " + cleared.size() + " accounts removed!");
        if (Settings.purgeEssentialsFile && this.ess != null)
            dataManager.purgeEssentials(cleared); // name to UUID convertion
                                                  // needed with latest versions
        if (Settings.purgePlayerDat)
            dataManager.purgeDat(cleared); // name to UUID convertion needed
                                           // with latest versions of MC
        if (Settings.purgeLimitedCreative)
            dataManager.purgeLimitedCreative(cleared);
        if (Settings.purgeAntiXray)
            dataManager.purgeAntiXray(cleared); // IDK if it uses UUID or
                                                // names... (Actually it purges
                                                // only names!)
        if (Settings.purgePermissions)
            dataManager.purgePermissions(cleared, permission);
    }

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
        if (spawnLoc == null)
            spawnLoc = world.getSpawnLocation();
        return spawnLoc;
    }

    private Location getDefaultSpawn(World world) {
        return world.getSpawnLocation();
    }

    private Location getMultiverseSpawn(World world) {
        if (multiverse != null && Settings.multiverse) {
            try {
                return multiverse.getMVWorldManager().getMVWorld(world).getSpawnLocation();
            } catch (NullPointerException npe) {
            } catch (ClassCastException cce) {
            } catch (NoClassDefFoundError ncdfe) {
            }
        }
        return null;
    }

    private Location getEssentialsSpawn() {
        if (essentialsSpawn != null)
            return essentialsSpawn;
        return null;
    }

    private Location getAuthMeSpawn(Player player) {
        if ((!database.isAuthAvailable(player.getName().toLowerCase()) || !player.hasPlayedBefore()) && (Spawn.getInstance().getFirstSpawn() != null))
            return Spawn.getInstance().getFirstSpawn();
        if (Spawn.getInstance().getSpawn() != null)
            return Spawn.getInstance().getSpawn();
        return player.getWorld().getSpawnLocation();
    }

    public void downloadGeoIp() {
        ConsoleLogger.info("[LICENSE] This product uses data from the GeoLite API created by MaxMind, available at http://www.maxmind.com");
        File file = new File(getDataFolder(), "GeoIP.dat");
        if (!file.exists()) {
            try {
                String url = "http://geolite.maxmind.com/download/geoip/database/GeoLiteCountry/GeoIP.dat.gz";
                URL downloadUrl = new URL(url);
                URLConnection conn = downloadUrl.openConnection();
                conn.setConnectTimeout(10000);
                conn.connect();
                InputStream input = conn.getInputStream();
                if (url.endsWith(".gz"))
                    input = new GZIPInputStream(input);
                OutputStream output = new FileOutputStream(file);
                byte[] buffer = new byte[2048];
                int length = input.read(buffer);
                while (length >= 0) {
                    output.write(buffer, 0, length);
                    length = input.read(buffer);
                }
                output.close();
                input.close();
            } catch (Exception e) {
            }
        }
    }

    public String getCountryCode(String ip) {
        try {
            if (ls == null)
                ls = new LookupService(new File(getDataFolder(), "GeoIP.dat"));
            String code = ls.getCountry(ip).getCode();
            if (code != null && !code.isEmpty())
                return code;
        } catch (Exception e) {
        }
        return null;
    }

    public String getCountryName(String ip) {
        try {
            if (ls == null)
                ls = new LookupService(new File(getDataFolder(), "GeoIP.dat"));
            String code = ls.getCountry(ip).getName();
            if (code != null && !code.isEmpty())
                return code;
        } catch (Exception e) {
        }
        return null;
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
        int playersOnline = 0;
        try {
            if (Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).getReturnType() == Collection.class)
                playersOnline = ((Collection<?>) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0])).size();
            else playersOnline = ((Player[]) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0])).length;
        } catch (NoSuchMethodException ex) {
        } // can never happen
        catch (InvocationTargetException ex) {
        } // can also never happen
        catch (IllegalAccessException ex) {
        } // can still never happen
        try {
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
        } catch (Exception e) {
        }
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
        if (count >= Settings.getMaxLoginPerIp)
            return true;
        return false;
    }

    public boolean hasJoinedIp(String name, String ip) {
        int count = 0;
        for (Player player : this.getServer().getOnlinePlayers()) {
            if (ip.equalsIgnoreCase(getIP(player)) && !player.getName().equalsIgnoreCase(name))
                count++;
        }
        if (count >= Settings.getMaxJoinPerIp)
            return true;
        return false;
    }

    /**
     * Get Player real IP through VeryGames method
     * 
     * @param Player
     *            player
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
        } catch (Exception e) {
        }
        return realIP;
    }

    public void setupDatabase() {
        /*
         * Backend MYSQL - FILE - SQLITE
         */
        switch (Settings.getDataSource) {
            case FILE:
                database = new FlatFile();
                break;
            case MYSQL:
                database = new MySQL();
                break;
            case SQLITE:
                database = new SQLite();
                final int b = database.getAccountsRegistered();
                if (b >= 4000)
                    ConsoleLogger.showError("YOU'RE USING THE SQLITE DATABASE WITH " + b + "+ ACCOUNTS, FOR BETTER PERFORMANCES, PLEASE UPGRADE TO MYSQL!!");
                break;
        }

        if (Settings.isCachingEnabled) {
            database = new CacheDataSource(this, database);
        }

        database = new DatabaseCalls(database);

        if (Settings.getDataSource == DataSource.DataSourceType.FILE) {
            Converter converter = new ForceFlatToSqlite(database, this);
            try {
                Thread t = new Thread(converter);
                t.start();
            } catch (Exception e) {
            }
            ConsoleLogger.showError("FlatFile backend has been detected and is now deprecated, next time server starts up, it will be changed to SQLite... Conversion will be started Asynchronously, it will not drop down your performance !");
            ConsoleLogger.showError("If you want to keep FlatFile, set file again into config at backend, but this message and this change will appear again at the next restart");
        }
    }
}
