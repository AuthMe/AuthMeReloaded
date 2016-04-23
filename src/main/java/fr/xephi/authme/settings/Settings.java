package fr.xephi.authme.settings;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.Wrapper;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Old settings manager. See {@link NewSetting} for the new manager.
 */
public final class Settings {

    public static final File PLUGIN_FOLDER = Wrapper.getInstance().getDataFolder();
    public static final File CACHE_FOLDER = new File(PLUGIN_FOLDER, "cache");
    public static List<String> allowCommands;
    public static List<String> getUnrestrictedName;
    public static List<String> getForcedWorlds;
    public static List<String> countries;
    public static List<String> countriesBlacklist;
    public static HashAlgorithm getPasswordHash;
    public static Pattern nickPattern;
    public static boolean isPermissionCheckEnabled,
        isForcedRegistrationEnabled, isTeleportToSpawnEnabled,
        isSessionsEnabled, isAllowRestrictedIp,
        isForceSingleSessionEnabled, isForceSpawnLocOnJoinEnabled,
        isSaveQuitLocationEnabled, protectInventoryBeforeLogInEnabled,
        isStopEnabled, reloadSupport, rakamakUseIp,
        removePassword, multiverse, bungee,
        enableProtection, forceRegLogin, noTeleport,
        allowAllCommandsIfRegIsOptional, isRemoveSpeedEnabled;
    public static String getNickRegex, getUnloggedinGroup,
        unRegisteredGroup, backupWindowsPath, getRegisteredGroup,
        rakamakUsers, rakamakUsersIp, defaultWorld, crazyloginFileName;
    public static int getWarnMessageInterval, getSessionTimeout,
        getRegistrationTimeout, getMaxNickLength, getMinNickLength,
        getNonActivatedGroup, maxLoginTry, captchaLength, getMaxLoginPerIp;
    protected static FileConfiguration configFile;

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
        getWarnMessageInterval = load(RegistrationSettings.MESSAGE_INTERVAL);
        isSessionsEnabled = load(PluginSettings.SESSIONS_ENABLED);
        getSessionTimeout = configFile.getInt("settings.sessions.timeout", 10);
        getRegistrationTimeout = load(RestrictionSettings.TIMEOUT);
        getMaxNickLength = configFile.getInt("settings.restrictions.maxNicknameLength", 20);
        getMinNickLength = configFile.getInt("settings.restrictions.minNicknameLength", 3);
        getNickRegex = configFile.getString("settings.restrictions.allowedNicknameCharacters", "[a-zA-Z0-9_?]*");
        nickPattern = Pattern.compile(getNickRegex);
        isAllowRestrictedIp = load(RestrictionSettings.ENABLE_RESTRICTED_USERS);
        isRemoveSpeedEnabled = load(RestrictionSettings.REMOVE_SPEED);
        isForceSingleSessionEnabled = load(RestrictionSettings.FORCE_SINGLE_SESSION);
        isForceSpawnLocOnJoinEnabled = load(RestrictionSettings.FORCE_SPAWN_LOCATION_AFTER_LOGIN);
        isSaveQuitLocationEnabled = configFile.getBoolean("settings.restrictions.SaveQuitLocation", false);
        getPasswordHash = load(SecuritySettings.PASSWORD_HASH);
        getUnloggedinGroup = load(SecuritySettings.UNLOGGEDIN_GROUP);
        getNonActivatedGroup = configFile.getInt("ExternalBoardOptions.nonActivedUserGroup", -1);
        unRegisteredGroup = configFile.getString("GroupOptions.UnregisteredPlayerGroup", "");

        getUnrestrictedName = new ArrayList<>();
        for (String name : configFile.getStringList("settings.unrestrictions.UnrestrictedName")) {
            getUnrestrictedName.add(name.toLowerCase());
        }

        getRegisteredGroup = configFile.getString("GroupOptions.RegisteredPlayerGroup", "");
        protectInventoryBeforeLogInEnabled = load(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN);
        backupWindowsPath = configFile.getString("BackupSystem.MysqlWindowsPath", "C:\\Program Files\\MySQL\\MySQL Server 5.1\\");
        isStopEnabled = configFile.getBoolean("Security.SQLProblem.stopServer", true);
        reloadSupport = configFile.getBoolean("Security.ReloadCommand.useReloadCommandSupport", true);

        allowAllCommandsIfRegIsOptional = load(RestrictionSettings.ALLOW_ALL_COMMANDS_IF_REGISTRATION_IS_OPTIONAL);
        allowCommands = new ArrayList<>();
        allowCommands.addAll(Arrays.asList("/login", "/l", "/register", "/reg", "/email", "/captcha"));
        for (String cmd : configFile.getStringList("settings.restrictions.allowCommands")) {
            cmd = cmd.toLowerCase();
            if (!allowCommands.contains(cmd)) {
                allowCommands.add(cmd);
            }
        }

        rakamakUsers = configFile.getString("Converter.Rakamak.fileName", "users.rak");
        rakamakUsersIp = configFile.getString("Converter.Rakamak.ipFileName", "UsersIp.rak");
        rakamakUseIp = configFile.getBoolean("Converter.Rakamak.useIp", false);
        removePassword = configFile.getBoolean("Security.console.removePassword", true);
        maxLoginTry = configFile.getInt("Security.captcha.maxLoginTry", 5);
        captchaLength = configFile.getInt("Security.captcha.captchaLength", 5);
        multiverse = load(HooksSettings.MULTIVERSE);
        bungee = load(HooksSettings.BUNGEECORD);
        getForcedWorlds = configFile.getStringList("settings.restrictions.ForceSpawnOnTheseWorlds");
        defaultWorld = configFile.getString("Purge.defaultWorld", "world");
        enableProtection = configFile.getBoolean("Protection.enableProtection", false);
        countries = configFile.getStringList("Protection.countries");
        countriesBlacklist = configFile.getStringList("Protection.countriesBlacklist");
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
