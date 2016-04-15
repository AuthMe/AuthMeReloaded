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
    public static List<String> forceRegisterCommands;
    public static List<String> forceRegisterCommandsAsConsole;
    public static HashAlgorithm getPasswordHash;
    public static Pattern nickPattern;
    public static boolean isChatAllowed, isPermissionCheckEnabled,
        isForcedRegistrationEnabled, isTeleportToSpawnEnabled,
        isSessionsEnabled, isAllowRestrictedIp,
        isMovementAllowed, isKickNonRegisteredEnabled,
        isForceSingleSessionEnabled, isForceSpawnLocOnJoinEnabled,
        isSaveQuitLocationEnabled, isForceSurvivalModeEnabled,
        protectInventoryBeforeLogInEnabled, isStopEnabled, reloadSupport,
        rakamakUseIp, noConsoleSpam, removePassword, displayOtherAccounts,
        emailRegistration, multiverse, bungee,
        banUnsafeIp, sessionExpireOnIpChange, useEssentialsMotd,
        enableProtection, recallEmail, useWelcomeMessage,
        broadcastWelcomeMessage, forceRegKick, forceRegLogin,
        removeJoinMessage, removeLeaveMessage, delayJoinMessage,
        noTeleport, allowAllCommandsIfRegIsOptional,
        isRemoveSpeedEnabled, preventOtherCase, hideChat;
    public static String getNickRegex, getUnloggedinGroup,
        unRegisteredGroup, backupWindowsPath, getRegisteredGroup,
        rakamakUsers, rakamakUsersIp, defaultWorld,
        spawnPriority, crazyloginFileName;
    public static int getWarnMessageInterval, getSessionTimeout,
        getRegistrationTimeout, getMaxNickLength, getMinNickLength,
        getMovementRadius, getNonActivatedGroup,
        maxLoginTry, captchaLength, saltLength,
        bCryptLog2Rounds, getMaxLoginPerIp, getMaxJoinPerIp;
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
        isForcedRegistrationEnabled = configFile.getBoolean("settings.registration.force", true);
        isTeleportToSpawnEnabled = load(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN);
        getWarnMessageInterval = load(RegistrationSettings.MESSAGE_INTERVAL);
        isSessionsEnabled = load(PluginSettings.SESSIONS_ENABLED);
        getSessionTimeout = configFile.getInt("settings.sessions.timeout", 10);
        getRegistrationTimeout = load(RestrictionSettings.TIMEOUT);
        isChatAllowed = load(RestrictionSettings.ALLOW_CHAT);
        getMaxNickLength = configFile.getInt("settings.restrictions.maxNicknameLength", 20);
        getMinNickLength = configFile.getInt("settings.restrictions.minNicknameLength", 3);
        getNickRegex = configFile.getString("settings.restrictions.allowedNicknameCharacters", "[a-zA-Z0-9_?]*");
        nickPattern = Pattern.compile(getNickRegex);
        isAllowRestrictedIp = load(RestrictionSettings.ENABLE_RESTRICTED_USERS);
        isMovementAllowed = configFile.getBoolean("settings.restrictions.allowMovement", false);
        isRemoveSpeedEnabled = configFile.getBoolean("settings.restrictions.removeSpeed", true);
        getMovementRadius = configFile.getInt("settings.restrictions.allowedMovementRadius", 100);
        isKickNonRegisteredEnabled = configFile.getBoolean("settings.restrictions.kickNonRegistered", false);
        isForceSingleSessionEnabled = configFile.getBoolean("settings.restrictions.ForceSingleSession", true);
        isForceSpawnLocOnJoinEnabled = configFile.getBoolean("settings.restrictions.ForceSpawnLocOnJoinEnabled", false);
        isSaveQuitLocationEnabled = configFile.getBoolean("settings.restrictions.SaveQuitLocation", false);
        isForceSurvivalModeEnabled = configFile.getBoolean("settings.GameMode.ForceSurvivalMode", false);
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

        allowAllCommandsIfRegIsOptional = configFile.getBoolean("settings.restrictions.allowAllCommandsIfRegistrationIsOptional", false);
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
        noConsoleSpam = load(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE);
        removePassword = configFile.getBoolean("Security.console.removePassword", true);
        displayOtherAccounts = configFile.getBoolean("settings.restrictions.displayOtherAccounts", true);
        maxLoginTry = configFile.getInt("Security.captcha.maxLoginTry", 5);
        captchaLength = configFile.getInt("Security.captcha.captchaLength", 5);
        emailRegistration = load(RegistrationSettings.USE_EMAIL_REGISTRATION);
        saltLength = configFile.getInt("settings.security.doubleMD5SaltLength", 8);
        multiverse = load(HooksSettings.MULTIVERSE);
        bungee = configFile.getBoolean("Hooks.bungeecord", false);
        getForcedWorlds = configFile.getStringList("settings.restrictions.ForceSpawnOnTheseWorlds");
        banUnsafeIp = configFile.getBoolean("settings.restrictions.banUnsafedIP", false);
        sessionExpireOnIpChange = configFile.getBoolean("settings.sessions.sessionExpireOnIpChange", true);
        bCryptLog2Rounds = configFile.getInt("ExternalBoardOptions.bCryptLog2Round", 10);
        useEssentialsMotd = configFile.getBoolean("Hooks.useEssentialsMotd", false);
        defaultWorld = configFile.getString("Purge.defaultWorld", "world");
        enableProtection = configFile.getBoolean("Protection.enableProtection", false);
        countries = configFile.getStringList("Protection.countries");
        recallEmail = configFile.getBoolean("Email.recallPlayers", false);
        useWelcomeMessage = load(RegistrationSettings.USE_WELCOME_MESSAGE);
        countriesBlacklist = configFile.getStringList("Protection.countriesBlacklist");
        broadcastWelcomeMessage = load(RegistrationSettings.BROADCAST_WELCOME_MESSAGE);
        forceRegKick = configFile.getBoolean("settings.registration.forceKickAfterRegister", false);
        forceRegLogin = load(RegistrationSettings.FORCE_LOGIN_AFTER_REGISTER);
        spawnPriority = load(RestrictionSettings.SPAWN_PRIORITY);
        getMaxLoginPerIp = load(RestrictionSettings.MAX_LOGIN_PER_IP);
        getMaxJoinPerIp = load(RestrictionSettings.MAX_JOIN_PER_IP);
        removeJoinMessage = load(RegistrationSettings.REMOVE_JOIN_MESSAGE);
        removeLeaveMessage = load(RegistrationSettings.REMOVE_LEAVE_MESSAGE);
        delayJoinMessage = load(RegistrationSettings.DELAY_JOIN_MESSAGE);
        noTeleport = load(RestrictionSettings.NO_TELEPORT);
        crazyloginFileName = configFile.getString("Converter.CrazyLogin.fileName", "accounts.db");
        forceRegisterCommands = configFile.getStringList("settings.forceRegisterCommands");
        forceRegisterCommandsAsConsole = configFile.getStringList("settings.forceRegisterCommandsAsConsole");
        preventOtherCase = configFile.getBoolean("settings.preventOtherCase", false);
        hideChat = load(RestrictionSettings.HIDE_CHAT);
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
