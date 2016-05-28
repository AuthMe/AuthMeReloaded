package fr.xephi.authme.settings;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Old settings manager. See {@link NewSetting} for the new manager.
 */
public final class Settings {

    public static List<String> getUnrestrictedName;
    public static List<String> getForcedWorlds;
    public static boolean isPermissionCheckEnabled;
    public static boolean isForcedRegistrationEnabled;
    public static boolean isTeleportToSpawnEnabled;
    public static boolean isSessionsEnabled;
    public static boolean isAllowRestrictedIp;
    public static boolean isForceSpawnLocOnJoinEnabled;
    public static boolean isSaveQuitLocationEnabled;
    public static boolean protectInventoryBeforeLogInEnabled;
    public static boolean isStopEnabled;
    public static boolean reloadSupport;
    public static boolean removePassword;
    public static boolean multiverse;
    public static boolean bungee;
    public static boolean forceRegLogin;
    public static boolean noTeleport;
    public static boolean isRemoveSpeedEnabled;
    public static String getUnloggedinGroup;
    public static String unRegisteredGroup;
    public static String getRegisteredGroup;
    public static String defaultWorld;
    public static String crazyloginFileName;
    public static int getSessionTimeout;
    public static int getNonActivatedGroup;
    public static int maxLoginTry;
    public static int captchaLength;
    public static int getMaxLoginPerIp;
    private static FileConfiguration configFile;

    /**
     * Constructor for Settings.
     *
     * @param pl AuthMe
     */
    public Settings(AuthMe pl) {
        configFile = pl.getConfig();
        loadVariables();
    }

    private static void loadVariables() {
        isPermissionCheckEnabled = load(PluginSettings.ENABLE_PERMISSION_CHECK);
        isForcedRegistrationEnabled = load(RegistrationSettings.FORCE);
        isTeleportToSpawnEnabled = load(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN);
        isSessionsEnabled = load(PluginSettings.SESSIONS_ENABLED);
        getSessionTimeout = configFile.getInt("settings.sessions.timeout", 10);
        isAllowRestrictedIp = load(RestrictionSettings.ENABLE_RESTRICTED_USERS);
        isRemoveSpeedEnabled = load(RestrictionSettings.REMOVE_SPEED);
        isForceSpawnLocOnJoinEnabled = load(RestrictionSettings.FORCE_SPAWN_LOCATION_AFTER_LOGIN);
        isSaveQuitLocationEnabled = configFile.getBoolean("settings.restrictions.SaveQuitLocation", false);
        getUnloggedinGroup = load(SecuritySettings.UNLOGGEDIN_GROUP);
        getNonActivatedGroup = configFile.getInt("ExternalBoardOptions.nonActivedUserGroup", -1);
        unRegisteredGroup = configFile.getString("GroupOptions.UnregisteredPlayerGroup", "");

        getUnrestrictedName = new ArrayList<>();
        for (String name : configFile.getStringList("settings.unrestrictions.UnrestrictedName")) {
            getUnrestrictedName.add(name.toLowerCase());
        }

        getRegisteredGroup = configFile.getString("GroupOptions.RegisteredPlayerGroup", "");
        protectInventoryBeforeLogInEnabled = load(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN);
        isStopEnabled = configFile.getBoolean("Security.SQLProblem.stopServer", true);
        reloadSupport = configFile.getBoolean("Security.ReloadCommand.useReloadCommandSupport", true);
        removePassword = configFile.getBoolean("Security.console.removePassword", true);
        maxLoginTry = configFile.getInt("Security.captcha.maxLoginTry", 5);
        captchaLength = configFile.getInt("Security.captcha.captchaLength", 5);
        multiverse = load(HooksSettings.MULTIVERSE);
        bungee = load(HooksSettings.BUNGEECORD);
        getForcedWorlds = load(RestrictionSettings.FORCE_SPAWN_ON_WORLDS);
        defaultWorld = configFile.getString("Purge.defaultWorld", "world");
        forceRegLogin = load(RegistrationSettings.FORCE_LOGIN_AFTER_REGISTER);
        getMaxLoginPerIp = load(RestrictionSettings.MAX_LOGIN_PER_IP);
        noTeleport = load(RestrictionSettings.NO_TELEPORT);
        crazyloginFileName = configFile.getString("Converter.CrazyLogin.fileName", "accounts.db");
    }

    /**
     * Load the value via the new Property setup for temporary support within this old settings manager.
     *
     * @param property The property to load
     * @param <T> The property type
     * @return The config value of the property
     */
    private static <T> T load(Property<T> property) {
        return property.getFromFile(configFile);
    }
}
