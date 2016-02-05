package fr.xephi.authme.settings;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSource.DataSourceType;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.util.Wrapper;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
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
    private static final File SETTINGS_FILE = new File(PLUGIN_FOLDER, "config.yml");
    public static final File LOG_FILE = new File(PLUGIN_FOLDER, "authme.log");
    // This is not an option!
    public static boolean antiBotInAction = false;
    public static List<String> allowCommands;
    public static List<String> getJoinPermissions;
    public static List<String> getUnrestrictedName;
    public static List<String> getRestrictedIp;
    public static List<String> getMySQLOtherUsernameColumn;
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
        isCachingEnabled,
        isKickOnWrongPasswordEnabled, enablePasswordConfirmation,
        protectInventoryBeforeLogInEnabled, isStopEnabled, reloadSupport,
        rakamakUseIp, noConsoleSpam, removePassword, displayOtherAccounts,
        useCaptcha, emailRegistration, multiverse, bungee,
        banUnsafeIp, doubleEmailCheck, sessionExpireOnIpChange,
        disableSocialSpy, useEssentialsMotd, usePurge,
        purgePlayerDat, purgeEssentialsFile, supportOldPassword,
        purgeLimitedCreative, purgeAntiXray, purgePermissions,
        enableProtection, enableAntiBot, recallEmail, useWelcomeMessage,
        broadcastWelcomeMessage, forceRegKick, forceRegLogin,
        checkVeryGames, delayJoinLeaveMessages, noTeleport, applyBlindEffect,
        kickPlayersBeforeStopping,
        customAttributes, generateImage, isRemoveSpeedEnabled, preventOtherCase;
    public static String getNickRegex, getUnloggedinGroup, getMySQLHost,
        getMySQLPort, getMySQLUsername, getMySQLPassword, getMySQLDatabase,
        getMySQLTablename, getMySQLColumnName, getMySQLColumnPassword,
        getMySQLColumnIp, getMySQLColumnLastLogin, getMySQLColumnSalt,
        getMySQLColumnGroup, getMySQLColumnEmail, unRegisteredGroup,
        backupWindowsPath, getRegisteredGroup,
        getMySQLlastlocX, getMySQLlastlocY,
        getMySQLlastlocZ, rakamakUsers, rakamakUsersIp, getmailAccount,
        getMySQLColumnId, getMySQLlastlocWorld, defaultWorld,
        getPhpbbPrefix, getWordPressPrefix, getMySQLColumnLogged,
        spawnPriority, crazyloginFileName, getPassRegex,
        getMySQLColumnRealName, sendPlayerTo;
    public static int getWarnMessageInterval, getSessionTimeout,
        getRegistrationTimeout, getMaxNickLength, getMinNickLength,
        getPasswordMinLen, getMovementRadius, getmaxRegPerIp,
        getNonActivatedGroup, passwordMaxLength, getRecoveryPassLength,
        getMailPort, maxLoginTry, captchaLength, saltLength,
        getmaxRegPerEmail, bCryptLog2Rounds, getPhpbbGroup,
        antiBotSensibility, antiBotDuration, delayRecall, getMaxLoginPerIp,
        getMaxJoinPerIp;
    protected static YamlConfiguration configFile;
    private static AuthMe plugin;
    private static Settings instance;

    /**
     * Constructor for Settings.
     *
     * @param pl AuthMe
     */
    public Settings(AuthMe pl) {
        instance = this;
        plugin = pl;
        configFile = (YamlConfiguration) plugin.getConfig();
    }

    /**
     * Method reload.
     *
     * @throws Exception if something went wrong
     */
    public static void reload() throws Exception {
        plugin.getLogger().info("Loading Configuration File...");
        boolean exist = SETTINGS_FILE.exists();
        if (!exist) {
            plugin.saveDefaultConfig();
        }
        configFile.load(SETTINGS_FILE);
        loadVariables();
        if (exist) {
            instance.saveDefaults();
        }
    }

    public static void loadVariables() {
        isPermissionCheckEnabled = configFile.getBoolean("permission.EnablePermissionCheck", false);
        isForcedRegistrationEnabled = configFile.getBoolean("settings.registration.force", true);
        isRegistrationEnabled = configFile.getBoolean("settings.registration.enabled", true);
        isTeleportToSpawnEnabled = configFile.getBoolean("settings.restrictions.teleportUnAuthedToSpawn", false);
        getWarnMessageInterval = configFile.getInt("settings.registration.messageInterval", 5);
        isSessionsEnabled = configFile.getBoolean("settings.sessions.enabled", false);
        getSessionTimeout = configFile.getInt("settings.sessions.timeout", 10);
        getRegistrationTimeout = configFile.getInt("settings.restrictions.timeout", 30);
        isChatAllowed = configFile.getBoolean("settings.restrictions.allowChat", false);
        getMaxNickLength = configFile.getInt("settings.restrictions.maxNicknameLength", 20);
        getMinNickLength = configFile.getInt("settings.restrictions.minNicknameLength", 3);
        getPasswordMinLen = configFile.getInt("settings.security.minPasswordLength", 4);
        getNickRegex = configFile.getString("settings.restrictions.allowedNicknameCharacters", "[a-zA-Z0-9_?]*");
        nickPattern = Pattern.compile(getNickRegex);
        isAllowRestrictedIp = configFile.getBoolean("settings.restrictions.AllowRestrictedUser", false);
        getRestrictedIp = configFile.getStringList("settings.restrictions.AllowedRestrictedUser");
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
        getPasswordHash = getPasswordHash();
        getUnloggedinGroup = configFile.getString("settings.security.unLoggedinGroup", "unLoggedInGroup");
        getDataSource = getDataSource();
        isCachingEnabled = configFile.getBoolean("DataSource.caching", true);
        getMySQLHost = configFile.getString("DataSource.mySQLHost", "127.0.0.1");
        getMySQLPort = configFile.getString("DataSource.mySQLPort", "3306");
        getMySQLUsername = configFile.getString("DataSource.mySQLUsername", "authme");
        getMySQLPassword = configFile.getString("DataSource.mySQLPassword", "12345");
        getMySQLDatabase = configFile.getString("DataSource.mySQLDatabase", "authme");
        getMySQLTablename = configFile.getString("DataSource.mySQLTablename", "authme");
        getMySQLColumnEmail = configFile.getString("DataSource.mySQLColumnEmail", "email");
        getMySQLColumnName = configFile.getString("DataSource.mySQLColumnName", "username");
        getMySQLColumnPassword = configFile.getString("DataSource.mySQLColumnPassword", "password");
        getMySQLColumnIp = configFile.getString("DataSource.mySQLColumnIp", "ip");
        getMySQLColumnLastLogin = configFile.getString("DataSource.mySQLColumnLastLogin", "lastlogin");
        getMySQLColumnSalt = configFile.getString("ExternalBoardOptions.mySQLColumnSalt");
        getMySQLColumnGroup = configFile.getString("ExternalBoardOptions.mySQLColumnGroup", "");
        getMySQLlastlocX = configFile.getString("DataSource.mySQLlastlocX", "x");
        getMySQLlastlocY = configFile.getString("DataSource.mySQLlastlocY", "y");
        getMySQLlastlocZ = configFile.getString("DataSource.mySQLlastlocZ", "z");
        getMySQLlastlocWorld = configFile.getString("DataSource.mySQLlastlocWorld", "world");
        getMySQLColumnRealName = configFile.getString("DataSource.mySQLRealName", "realname");
        getNonActivatedGroup = configFile.getInt("ExternalBoardOptions.nonActivedUserGroup", -1);
        unRegisteredGroup = configFile.getString("GroupOptions.UnregisteredPlayerGroup", "");

        getUnrestrictedName = new ArrayList<>();
        for (String name : configFile.getStringList("settings.unrestrictions.UnrestrictedName")) {
            getUnrestrictedName.add(name.toLowerCase());
        }

        getRegisteredGroup = configFile.getString("GroupOptions.RegisteredPlayerGroup", "");
        enablePasswordConfirmation = configFile.getBoolean("settings.restrictions.enablePasswordConfirmation", true);

        protectInventoryBeforeLogInEnabled = configFile.getBoolean("settings.restrictions.ProtectInventoryBeforeLogIn", true);
        plugin.checkProtocolLib();

        passwordMaxLength = configFile.getInt("settings.security.passwordMaxLength", 20);
        backupWindowsPath = configFile.getString("BackupSystem.MysqlWindowsPath", "C:\\Program Files\\MySQL\\MySQL Server 5.1\\");
        isStopEnabled = configFile.getBoolean("Security.SQLProblem.stopServer", true);
        reloadSupport = configFile.getBoolean("Security.ReloadCommand.useReloadCommandSupport", true);

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
        getMySQLOtherUsernameColumn = configFile.getStringList("ExternalBoardOptions.mySQLOtherUsernameColumns");
        displayOtherAccounts = configFile.getBoolean("settings.restrictions.displayOtherAccounts", true);
        getMySQLColumnId = configFile.getString("DataSource.mySQLColumnId", "id");
        useCaptcha = configFile.getBoolean("Security.captcha.useCaptcha", false);
        maxLoginTry = configFile.getInt("Security.captcha.maxLoginTry", 5);
        captchaLength = configFile.getInt("Security.captcha.captchaLength", 5);
        emailRegistration = configFile.getBoolean("settings.registration.enableEmailRegistrationSystem", false);
        saltLength = configFile.getInt("settings.security.doubleMD5SaltLength", 8);
        getmaxRegPerEmail = configFile.getInt("Email.maxRegPerEmail", 1);
        multiverse = configFile.getBoolean("Hooks.multiverse", true);
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
        getPhpbbPrefix = configFile.getString("ExternalBoardOptions.phpbbTablePrefix", "phpbb_");
        getPhpbbGroup = configFile.getInt("ExternalBoardOptions.phpbbActivatedGroupId", 2);
        supportOldPassword = configFile.getBoolean("settings.security.supportOldPasswordHash", false);
        getWordPressPrefix = configFile.getString("ExternalBoardOptions.wordpressTablePrefix", "wp_");
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
        forceRegLogin = configFile.getBoolean("settings.registration.forceLoginAfterRegister", false);
        getMySQLColumnLogged = configFile.getString("DataSource.mySQLColumnLogged", "isLogged");
        spawnPriority = configFile.getString("settings.restrictions.spawnPriority", "authme,essentials,multiverse,default");
        getMaxLoginPerIp = configFile.getInt("settings.restrictions.maxLoginPerIp", 0);
        getMaxJoinPerIp = configFile.getInt("settings.restrictions.maxJoinPerIp", 0);
        checkVeryGames = configFile.getBoolean("VeryGames.enableIpCheck", false);
        delayJoinLeaveMessages = configFile.getBoolean("settings.delayJoinLeaveMessages", false);
        noTeleport = configFile.getBoolean("settings.restrictions.noTeleport", false);
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
     * Method getPasswordHash.
     *
     * @return HashAlgorithm
     */
    private static HashAlgorithm getPasswordHash() {
        String key = "settings.security.passwordHash";
        try {
            return HashAlgorithm.valueOf(configFile.getString(key, "SHA256").toUpperCase());
        } catch (IllegalArgumentException ex) {
            ConsoleLogger.showError("Unknown Hash Algorithm; defaulting to SHA256");
            return HashAlgorithm.SHA256;
        }
    }

    /**
     * Method getDataSource.
     *
     * @return DataSourceType
     */
    private static DataSourceType getDataSource() {
        String key = "DataSource.backend";
        try {
            return DataSource.DataSourceType.valueOf(configFile.getString(key, "sqlite").toUpperCase());
        } catch (IllegalArgumentException ex) {
            ConsoleLogger.showError("Unknown database backend; defaulting to SQLite database");
            return DataSource.DataSourceType.SQLITE;
        }
    }

    /**
     * Saves the configuration to disk
     *
     * @return True if saved successfully
     */
    private static boolean save() {
        try {
            configFile.save(SETTINGS_FILE);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Method checkLang.
     *
     * @param lang String
     *
     * @return String
     */
    private static String checkLang(String lang) {
        if (new File(PLUGIN_FOLDER, "messages" + File.separator + "messages_" + lang + ".yml").exists()) {
            ConsoleLogger.info("Set Language to: " + lang);
            return lang;
        }
        if (AuthMe.class.getResourceAsStream("/messages/messages_" + lang + ".yml") != null) {
            ConsoleLogger.info("Set Language to: " + lang);
            return lang;
        }
        ConsoleLogger.info("Language file not found for " + lang + ", set to default language: en !");
        return "en";
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
     * Saves current configuration (plus defaults) to disk.
     * <p>
     * If defaults and configuration are empty, saves blank file.
     *
     * @return True if saved successfully
     */
    private boolean saveDefaults() {
        configFile.options()
            .copyDefaults(true)
            .copyHeader(true);
        boolean success = save();
        configFile.options()
            .copyDefaults(false)
            .copyHeader(false);
        return success;
    }
}
