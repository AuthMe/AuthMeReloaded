package fr.xephi.authme.settings;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.DatabaseSettings;
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
    public static final File MODULE_FOLDER = new File(PLUGIN_FOLDER, "modules");
    public static final File CACHE_FOLDER = new File(PLUGIN_FOLDER, "cache");
    // This is not an option!
    public static boolean antiBotInAction = false;
    public static List<String> allowCommands;
    public static List<String> getJoinPermissions;
    public static List<String> getUnrestrictedName;
    public static List<String> getRestrictedIp;
    public static List<String> getForcedWorlds;
    public static List<String> countries;
    public static List<String> countriesBlacklist;
    public static List<String> forceCommands;
    public static List<String> forceCommandsAsConsole;
    public static List<String> forceRegisterCommands;
    public static List<String> forceRegisterCommandsAsConsole;
    public static List<String> unsafePasswords;
    public static List<String> emailBlacklist;
    public static List<String> emailWhitelist;
    public static DataSourceType getDataSource;
    public static HashAlgorithm getPasswordHash;
    public static Pattern nickPattern;
    public static boolean useLogging = false;
    public static int purgeDelay = 60;
    public static boolean isChatAllowed, isPermissionCheckEnabled, isRegistrationEnabled,
        isForcedRegistrationEnabled, isTeleportToSpawnEnabled,
        isSessionsEnabled, isAllowRestrictedIp,
        isMovementAllowed, isKickNonRegisteredEnabled,
        isForceSingleSessionEnabled, isForceSpawnLocOnJoinEnabled,
        isSaveQuitLocationEnabled, isForceSurvivalModeEnabled,
        isKickOnWrongPasswordEnabled, enablePasswordConfirmation,
        protectInventoryBeforeLogInEnabled, isStopEnabled, reloadSupport,
        rakamakUseIp, noConsoleSpam, removePassword, displayOtherAccounts,
        useCaptcha, emailRegistration, multiverse, bungee,
        banUnsafeIp, doubleEmailCheck, sessionExpireOnIpChange,
        disableSocialSpy, useEssentialsMotd, usePurge,
        purgePlayerDat, purgeEssentialsFile,
        purgeLimitedCreative, purgeAntiXray, purgePermissions,
        enableProtection, enableAntiBot, recallEmail, useWelcomeMessage,
        broadcastWelcomeMessage, forceRegKick, forceRegLogin,
        checkVeryGames, removeJoinMessage, removeLeaveMessage, delayJoinMessage,
        noTeleport, applyBlindEffect, hideTablistBeforeLogin, denyTabcompleteBeforeLogin,
        kickPlayersBeforeStopping, allowAllCommandsIfRegIsOptional,
        customAttributes, generateImage, isRemoveSpeedEnabled, preventOtherCase;
    public static String getNickRegex, getUnloggedinGroup,
        getMySQLColumnGroup, unRegisteredGroup,
        backupWindowsPath, getRegisteredGroup,
        rakamakUsers, rakamakUsersIp, getmailAccount, defaultWorld,
        spawnPriority, crazyloginFileName, getPassRegex, sendPlayerTo;
    public static int getWarnMessageInterval, getSessionTimeout,
        getRegistrationTimeout, getMaxNickLength, getMinNickLength,
        getPasswordMinLen, getMovementRadius, getmaxRegPerIp,
        getNonActivatedGroup, passwordMaxLength, getRecoveryPassLength,
        getMailPort, maxLoginTry, captchaLength, saltLength,
        getmaxRegPerEmail, bCryptLog2Rounds,
        antiBotSensibility, antiBotDuration, delayRecall, getMaxLoginPerIp,
        getMaxJoinPerIp;
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
        isRegistrationEnabled = configFile.getBoolean("settings.registration.enabled", true);
        isTeleportToSpawnEnabled = load(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN);
        getWarnMessageInterval = load(RegistrationSettings.MESSAGE_INTERVAL);
        isSessionsEnabled = load(PluginSettings.SESSIONS_ENABLED);
        getSessionTimeout = configFile.getInt("settings.sessions.timeout", 10);
        getRegistrationTimeout = load(RestrictionSettings.TIMEOUT);
        isChatAllowed = load(RestrictionSettings.ALLOW_CHAT);
        getMaxNickLength = configFile.getInt("settings.restrictions.maxNicknameLength", 20);
        getMinNickLength = configFile.getInt("settings.restrictions.minNicknameLength", 3);
        getPasswordMinLen = configFile.getInt("settings.security.minPasswordLength", 4);
        getNickRegex = configFile.getString("settings.restrictions.allowedNicknameCharacters", "[a-zA-Z0-9_?]*");
        nickPattern = Pattern.compile(getNickRegex);
        isAllowRestrictedIp = load(RestrictionSettings.ENABLE_RESTRICTED_USERS);
        getRestrictedIp = load(RestrictionSettings.ALLOWED_RESTRICTED_USERS);
        isMovementAllowed = configFile.getBoolean("settings.restrictions.allowMovement", false);
        isRemoveSpeedEnabled = configFile.getBoolean("settings.restrictions.removeSpeed", true);
        getMovementRadius = configFile.getInt("settings.restrictions.allowedMovementRadius", 100);
        getJoinPermissions = configFile.getStringList("GroupOptions.Permissions.PermissionsOnJoin");
        isKickOnWrongPasswordEnabled = configFile.getBoolean("settings.restrictions.kickOnWrongPassword", false);
        isKickNonRegisteredEnabled = configFile.getBoolean("settings.restrictions.kickNonRegistered", false);
        isForceSingleSessionEnabled = configFile.getBoolean("settings.restrictions.ForceSingleSession", true);
        isForceSpawnLocOnJoinEnabled = configFile.getBoolean("settings.restrictions.ForceSpawnLocOnJoinEnabled", false);
        isSaveQuitLocationEnabled = configFile.getBoolean("settings.restrictions.SaveQuitLocation", false);
        isForceSurvivalModeEnabled = configFile.getBoolean("settings.GameMode.ForceSurvivalMode", false);
        getmaxRegPerIp = configFile.getInt("settings.restrictions.maxRegPerIp", 1);
        getPasswordHash = load(SecuritySettings.PASSWORD_HASH);
        getUnloggedinGroup = load(SecuritySettings.UNLOGGEDIN_GROUP);
        getDataSource = load(DatabaseSettings.BACKEND);
        getMySQLColumnGroup = configFile.getString("ExternalBoardOptions.mySQLColumnGroup", "");
        getNonActivatedGroup = configFile.getInt("ExternalBoardOptions.nonActivedUserGroup", -1);
        unRegisteredGroup = configFile.getString("GroupOptions.UnregisteredPlayerGroup", "");

        getUnrestrictedName = new ArrayList<>();
        for (String name : configFile.getStringList("settings.unrestrictions.UnrestrictedName")) {
            getUnrestrictedName.add(name.toLowerCase());
        }

        getRegisteredGroup = configFile.getString("GroupOptions.RegisteredPlayerGroup", "");
        enablePasswordConfirmation = load(RestrictionSettings.ENABLE_PASSWORD_CONFIRMATION);

        protectInventoryBeforeLogInEnabled = load(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN);
        denyTabcompleteBeforeLogin = load(RestrictionSettings.DENY_TABCOMPLETE_BEFORE_LOGIN);
        hideTablistBeforeLogin = load(RestrictionSettings.HIDE_TABLIST_BEFORE_LOGIN);

        passwordMaxLength = load(SecuritySettings.MAX_PASSWORD_LENGTH);
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
        noConsoleSpam = configFile.getBoolean("Security.console.noConsoleSpam", false);
        removePassword = configFile.getBoolean("Security.console.removePassword", true);
        getmailAccount = configFile.getString("Email.mailAccount", "");
        getMailPort = configFile.getInt("Email.mailPort", 465);
        getRecoveryPassLength = configFile.getInt("Email.RecoveryPasswordLength", 8);
        displayOtherAccounts = configFile.getBoolean("settings.restrictions.displayOtherAccounts", true);
        useCaptcha = configFile.getBoolean("Security.captcha.useCaptcha", false);
        maxLoginTry = configFile.getInt("Security.captcha.maxLoginTry", 5);
        captchaLength = configFile.getInt("Security.captcha.captchaLength", 5);
        emailRegistration = load(RegistrationSettings.USE_EMAIL_REGISTRATION);
        saltLength = configFile.getInt("settings.security.doubleMD5SaltLength", 8);
        getmaxRegPerEmail = configFile.getInt("Email.maxRegPerEmail", 1);
        multiverse = load(HooksSettings.MULTIVERSE);
        bungee = configFile.getBoolean("Hooks.bungeecord", false);
        getForcedWorlds = configFile.getStringList("settings.restrictions.ForceSpawnOnTheseWorlds");
        banUnsafeIp = configFile.getBoolean("settings.restrictions.banUnsafedIP", false);
        doubleEmailCheck = configFile.getBoolean("settings.registration.doubleEmailCheck", false);
        sessionExpireOnIpChange = configFile.getBoolean("settings.sessions.sessionExpireOnIpChange", true);
        useLogging = configFile.getBoolean("Security.console.logConsole", false);
        disableSocialSpy = configFile.getBoolean("Hooks.disableSocialSpy", true);
        bCryptLog2Rounds = configFile.getInt("ExternalBoardOptions.bCryptLog2Round", 10);
        useEssentialsMotd = configFile.getBoolean("Hooks.useEssentialsMotd", false);
        usePurge = configFile.getBoolean("Purge.useAutoPurge", false);
        purgeDelay = configFile.getInt("Purge.daysBeforeRemovePlayer", 60);
        purgePlayerDat = configFile.getBoolean("Purge.removePlayerDat", false);
        purgeEssentialsFile = configFile.getBoolean("Purge.removeEssentialsFile", false);
        defaultWorld = configFile.getString("Purge.defaultWorld", "world");
        purgeLimitedCreative = configFile.getBoolean("Purge.removeLimitedCreativesInventories", false);
        purgeAntiXray = configFile.getBoolean("Purge.removeAntiXRayFile", false);
        purgePermissions = configFile.getBoolean("Purge.removePermissions", false);
        enableProtection = configFile.getBoolean("Protection.enableProtection", false);
        countries = configFile.getStringList("Protection.countries");
        enableAntiBot = configFile.getBoolean("Protection.enableAntiBot", false);
        antiBotSensibility = configFile.getInt("Protection.antiBotSensibility", 5);
        antiBotDuration = configFile.getInt("Protection.antiBotDuration", 10);
        forceCommands = configFile.getStringList("settings.forceCommands");
        forceCommandsAsConsole = configFile.getStringList("settings.forceCommandsAsConsole");
        recallEmail = configFile.getBoolean("Email.recallPlayers", false);
        delayRecall = configFile.getInt("Email.delayRecall", 5);
        useWelcomeMessage = configFile.getBoolean("settings.useWelcomeMessage", true);
        unsafePasswords = configFile.getStringList("settings.security.unsafePasswords");
        countriesBlacklist = configFile.getStringList("Protection.countriesBlacklist");
        broadcastWelcomeMessage = configFile.getBoolean("settings.broadcastWelcomeMessage", false);
        forceRegKick = configFile.getBoolean("settings.registration.forceKickAfterRegister", false);
        forceRegLogin = load(RegistrationSettings.FORCE_LOGIN_AFTER_REGISTER);
        spawnPriority = load(RestrictionSettings.SPAWN_PRIORITY);
        getMaxLoginPerIp = load(RestrictionSettings.MAX_LOGIN_PER_IP);
        getMaxJoinPerIp = load(RestrictionSettings.MAX_JOIN_PER_IP);
        checkVeryGames = load(HooksSettings.ENABLE_VERYGAMES_IP_CHECK);
        removeJoinMessage = load(RegistrationSettings.REMOVE_JOIN_MESSAGE);
        removeLeaveMessage = load(RegistrationSettings.REMOVE_LEAVE_MESSAGE);
        delayJoinMessage = load(RegistrationSettings.DELAY_JOIN_MESSAGE);
        noTeleport = load(RestrictionSettings.NO_TELEPORT);
        crazyloginFileName = configFile.getString("Converter.CrazyLogin.fileName", "accounts.db");
        getPassRegex = configFile.getString("settings.restrictions.allowedPasswordCharacters", "[\\x21-\\x7E]*");
        applyBlindEffect = configFile.getBoolean("settings.applyBlindEffect", false);
        emailBlacklist = configFile.getStringList("Email.emailBlacklisted");
        emailWhitelist = configFile.getStringList("Email.emailWhitelisted");
        forceRegisterCommands = configFile.getStringList("settings.forceRegisterCommands");
        forceRegisterCommandsAsConsole = configFile.getStringList("settings.forceRegisterCommandsAsConsole");
        customAttributes = configFile.getBoolean("Hooks.customAttributes");
        generateImage = configFile.getBoolean("Email.generateImage", false);
        preventOtherCase = configFile.getBoolean("settings.preventOtherCase", false);
        kickPlayersBeforeStopping = configFile.getBoolean("Security.stop.kickPlayersBeforeStopping", true);
        sendPlayerTo = configFile.getString("Hooks.sendPlayerTo", "");

    }

    /**
     * Method switchAntiBotMod.
     *
     * @param mode boolean
     */
    public static void switchAntiBotMod(boolean mode) {
        if (mode) {
            isKickNonRegisteredEnabled = true;
            antiBotInAction = true;
        } else {
            isKickNonRegisteredEnabled = configFile.getBoolean("settings.restrictions.kickNonRegistered", false);
            antiBotInAction = false;
        }
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
