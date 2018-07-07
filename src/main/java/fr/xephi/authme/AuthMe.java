package fr.xephi.authme;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.command.CommandHandler;
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
import fr.xephi.authme.listener.PlayerListener111;
import fr.xephi.authme.listener.PlayerListener16;
import fr.xephi.authme.listener.PlayerListener18;
import fr.xephi.authme.listener.PlayerListener19;
import fr.xephi.authme.listener.PlayerListener19Spigot;
import fr.xephi.authme.listener.ServerListener;
import fr.xephi.authme.security.crypts.Sha256;
import fr.xephi.authme.service.BackupService;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.MigrationService;
import fr.xephi.authme.service.bungeecord.BungeeReceiver;
import fr.xephi.authme.service.yaml.YamlParseException;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SettingsWarner;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.task.CleanupTask;
import fr.xephi.authme.task.purge.PurgeService;
import fr.xephi.authme.util.ExceptionUtils;
import org.apache.commons.lang.SystemUtils;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
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

    // Default version and build number values
    private static String pluginVersion = "N/D";
    private static String pluginBuildNumber = "Unknown";

    // Private instances
    private CommandHandler commandHandler;
    private Settings settings;
    private DataSource database;
    private BukkitService bukkitService;
    private Injector injector;
    private BackupService backupService;

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
    protected AuthMe(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
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
     * Method used to obtain the v2 plugin's api instance
     * @deprecated Will be removed in 5.5, use {@link fr.xephi.authme.api.v3.AuthMeApi} instead
     *
     * @return The plugin's api instance
     */
    @Deprecated
    public static NewAPI getApi() {
        return NewAPI.getInstance();
    }

    /**
     * Method called when the server enables the plugin.
     */
    @Override
    public void onEnable() {
        // Load the plugin version data from the plugin description file
        loadPluginInfo(getDescription().getVersion());

        // Initialize the plugin
        try {
            initialize();
        } catch (Throwable th) {
            YamlParseException yamlParseException = ExceptionUtils.findThrowableInCause(YamlParseException.class, th);
            if (yamlParseException == null) {
                ConsoleLogger.logException("Aborting initialization of AuthMe:", th);
            } else {
                ConsoleLogger.logException("File '" + yamlParseException.getFile() + "' contains invalid YAML. "
                    + "Please run its contents through http://yamllint.com", yamlParseException);
            }
            stopOrUnload();
            return;
        }

        // Show settings warnings
        injector.getSingleton(SettingsWarner.class).logWarningsForMisconfigurations();

        // Do a backup on start
        backupService.doBackup(BackupService.BackupCause.START);

        // Set up Metrics
        OnStartupTasks.sendMetrics(this, settings);

        // Sponsor messages
        ConsoleLogger.info("Development builds are available on our jenkins, thanks to FastVM.io");
        ConsoleLogger.info("Do you want a good vps for your game server? Look at our sponsor FastVM.io leader "
            + "as virtual server provider!");

        // Successful message
        ConsoleLogger.info("AuthMe " + getPluginVersion() + " build n." + getPluginBuildNumber()
            + " correctly enabled!");

        // Purge on start if enabled
        PurgeService purgeService = injector.getSingleton(PurgeService.class);
        purgeService.runAutoPurge();

        // Schedule clean up task
        CleanupTask cleanupTask = injector.getSingleton(CleanupTask.class);
        cleanupTask.runTaskTimerAsynchronously(this, CLEANUP_INTERVAL, CLEANUP_INTERVAL);
    }

    /**
     * Load the version and build number of the plugin from the description file.
     *
     * @param versionRaw the version as given by the plugin description file
     */
    private static void loadPluginInfo(String versionRaw) {
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
     */
    private void initialize() {
        // Set the Logger instance and log file path
        ConsoleLogger.setLogger(getLogger());
        ConsoleLogger.setLogFile(new File(getDataFolder(), LOG_FILENAME));

        // Check java version
        if(!SystemUtils.isJavaVersionAtLeast(1.8f)) {
            throw new IllegalStateException("You need Java 1.8 or above to run this plugin!");
        }

        // Create plugin folder
        getDataFolder().mkdir();

        // Create injector, provide elements from the Bukkit environment and register providers
        injector = new InjectorBuilder()
            .addDefaultHandlers("fr.xephi.authme")
            .create();
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
        MigrationService.changePlainTextToSha256(settings, database, new Sha256());

        //TODO: does this still make sense? -sgdc3
        // If the server is empty (fresh start) just set all the players as unlogged
        if (bukkitService.getOnlinePlayers().isEmpty()) {
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
    void instantiateServices(Injector injector) {
        database = injector.getSingleton(DataSource.class);
        bukkitService = injector.getSingleton(BukkitService.class);
        commandHandler = injector.getSingleton(CommandHandler.class);
        backupService = injector.getSingleton(BackupService.class);

        // Trigger instantiation (class not used elsewhere)
        injector.getSingleton(BungeeReceiver.class);

        // Trigger construction of API classes; they will keep track of the singleton
        injector.getSingleton(fr.xephi.authme.api.v3.AuthMeApi.class);
        injector.getSingleton(NewAPI.class);
    }

    /**
     * Registers all event listeners.
     *
     * @param injector the injector
     */
    void registerEventListeners(Injector injector) {
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

        // Try to register 1.9 spigot player listeners
        if (isClassLoaded("org.spigotmc.event.player.PlayerSpawnLocationEvent")) {
            pluginManager.registerEvents(injector.getSingleton(PlayerListener19Spigot.class), this);
        }

        // Register listener for 1.11 events if available
        if (isClassLoaded("org.bukkit.event.entity.EntityAirChangeEvent")) {
            pluginManager.registerEvents(injector.getSingleton(PlayerListener111.class), this);
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
        if (backupService != null) {
            backupService.doBackup(BackupService.BackupCause.STOP);
        }

        // Wait for tasks and close data source
        new TaskCloser(this, database).run();

        // Disabled correctly
        ConsoleLogger.info("AuthMe " + this.getDescription().getVersion() + " disabled!");
        ConsoleLogger.close();
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
