package fr.xephi.authme.settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.DataSource.DataSourceType;
import fr.xephi.authme.security.HashAlgorithm;

public final class Settings extends YamlConfiguration {

    public static String PLUGIN_FOLDER = "." + File.separator + "plugins" + File.separator + "AuthMe";
    public static final String CACHE_FOLDER = Settings.PLUGIN_FOLDER + File.separator + "cache";
    public static final String AUTH_FILE = Settings.PLUGIN_FOLDER + File.separator + "auths.db";
    public static final String MESSAGE_FILE = Settings.PLUGIN_FOLDER + File.separator + "messages";
    public static final String SETTINGS_FILE = Settings.PLUGIN_FOLDER + File.separator + "config.yml";
    public static List<String> allowCommands = null;
    public static List<String> getJoinPermissions = null;
    public static List<String> getUnrestrictedName = null;
    private static List<String> getRestrictedIp;
    public static List<String> getMySQLOtherUsernameColumn = null;
    public static List<String> getForcedWorlds = null;
    public static List<String> countries = null;
    public static List<String> countriesBlacklist = null;
    public static List<String> forceCommands = null;
    public static List<String> forceCommandsAsConsole = null;
    public static List<String> forceRegisterCommands = null;
    public static List<String> forceRegisterCommandsAsConsole = null;
    private AuthMe plugin;
    private final File file;
    public static DataSourceType getDataSource;
    public static HashAlgorithm getPasswordHash;
    public static Boolean useLogging = false;
    public static int purgeDelay = 60;
    public static List<String> welcomeMsg = null;
    public static List<String> unsafePasswords;
    public static List<String> emailBlacklist = null;
    public static List<String> emailWhitelist = null;

    public static Boolean isPermissionCheckEnabled, isRegistrationEnabled,
            isForcedRegistrationEnabled, isTeleportToSpawnEnabled,
            isSessionsEnabled, isChatAllowed, isAllowRestrictedIp,
            isMovementAllowed, isKickNonRegisteredEnabled,
            isForceSingleSessionEnabled, isForceSpawnLocOnJoinEnabled,
            isSaveQuitLocationEnabled, isForceSurvivalModeEnabled,
            isResetInventoryIfCreative, isCachingEnabled,
            isKickOnWrongPasswordEnabled, getEnablePasswordVerifier,
            protectInventoryBeforeLogInEnabled, isBackupActivated,
            isBackupOnStart, isBackupOnStop, enablePasspartu, isStopEnabled,
            reloadSupport, rakamakUseIp, noConsoleSpam, removePassword,
            displayOtherAccounts, useCaptcha, emailRegistration, multiverse,
            notifications, chestshop, bungee, banUnsafeIp, doubleEmailCheck,
            sessionExpireOnIpChange, disableSocialSpy, forceOnlyAfterLogin,
            useEssentialsMotd, usePurge, purgePlayerDat, purgeEssentialsFile,
            supportOldPassword, purgeLimitedCreative, purgeAntiXray,
            purgePermissions, enableProtection, enableAntiBot, recallEmail,
            useWelcomeMessage, broadcastWelcomeMessage, forceRegKick,
            forceRegLogin, checkVeryGames, delayJoinMessage, noTeleport,
            applyBlindEffect;

    public static String getNickRegex, getUnloggedinGroup, getMySQLHost,
            getMySQLPort, getMySQLUsername, getMySQLPassword, getMySQLDatabase,
            getMySQLTablename, getMySQLColumnName, getMySQLColumnPassword,
            getMySQLColumnIp, getMySQLColumnLastLogin, getMySQLColumnSalt,
            getMySQLColumnGroup, getMySQLColumnEmail, unRegisteredGroup,
            backupWindowsPath, getcUnrestrictedName, getRegisteredGroup,
            messagesLanguage, getMySQLlastlocX, getMySQLlastlocY,
            getMySQLlastlocZ, rakamakUsers, rakamakUsersIp, getmailAccount,
            getmailPassword, getmailSMTP, getMySQLColumnId, getmailSenderName,
            getMailSubject, getMailText, getMySQLlastlocWorld, defaultWorld,
            getPhpbbPrefix, getWordPressPrefix, getMySQLColumnLogged,
            spawnPriority, crazyloginFileName, getPassRegex;

    public static int getWarnMessageInterval, getSessionTimeout,
            getRegistrationTimeout, getMaxNickLength, getMinNickLength,
            getPasswordMinLen, getMovementRadius, getmaxRegPerIp,
            getNonActivatedGroup, passwordMaxLength, getRecoveryPassLength,
            getMailPort, maxLoginTry, captchaLength, saltLength,
            getmaxRegPerEmail, bCryptLog2Rounds, getPhpbbGroup,
            antiBotSensibility, antiBotDuration, delayRecall, getMaxLoginPerIp,
            getMaxJoinPerIp;

    protected static YamlConfiguration configFile;

    public Settings(AuthMe plugin) {
        this.file = new File(plugin.getDataFolder(), "config.yml");
        this.plugin = plugin;
        if (exists()) {
            load();
        } else {
            loadDefaults(file.getName());
            load();
        }
        configFile = (YamlConfiguration) plugin.getConfig();
        PLUGIN_FOLDER = plugin.getDataFolder().toString();
    }

    @SuppressWarnings("unchecked")
    public void loadConfigOptions() {
        plugin.getLogger().info("Loading Configuration File...");
        mergeConfig();

        messagesLanguage = checkLang(configFile.getString("settings.messagesLanguage", "en"));
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
        isAllowRestrictedIp = configFile.getBoolean("settings.restrictions.AllowRestrictedUser", false);
        getRestrictedIp = configFile.getStringList("settings.restrictions.AllowedRestrictedUser");
        isMovementAllowed = configFile.getBoolean("settings.restrictions.allowMovement", false);
        getMovementRadius = configFile.getInt("settings.restrictions.allowedMovementRadius", 100);
        getJoinPermissions = configFile.getStringList("GroupOptions.Permissions.PermissionsOnJoin");
        isKickOnWrongPasswordEnabled = configFile.getBoolean("settings.restrictions.kickOnWrongPassword", false);
        isKickNonRegisteredEnabled = configFile.getBoolean("settings.restrictions.kickNonRegistered", false);
        isForceSingleSessionEnabled = configFile.getBoolean("settings.restrictions.ForceSingleSession", true);
        isForceSpawnLocOnJoinEnabled = configFile.getBoolean("settings.restrictions.ForceSpawnLocOnJoinEnabled", false);
        isSaveQuitLocationEnabled = configFile.getBoolean("settings.restrictions.SaveQuitLocation", false);
        isForceSurvivalModeEnabled = configFile.getBoolean("settings.GameMode.ForceSurvivalMode", false);
        isResetInventoryIfCreative = configFile.getBoolean("settings.GameMode.ResetInventoryIfCreative", false);
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
        getNonActivatedGroup = configFile.getInt("ExternalBoardOptions.nonActivedUserGroup", -1);
        unRegisteredGroup = configFile.getString("GroupOptions.UnregisteredPlayerGroup", "");
        getUnrestrictedName = configFile.getStringList("settings.unrestrictions.UnrestrictedName");
        getRegisteredGroup = configFile.getString("GroupOptions.RegisteredPlayerGroup", "");
        getEnablePasswordVerifier = configFile.getBoolean("settings.restrictions.enablePasswordVerifier", true);
        protectInventoryBeforeLogInEnabled = configFile.getBoolean("settings.restrictions.ProtectInventoryBeforeLogIn", true);
        passwordMaxLength = configFile.getInt("settings.security.passwordMaxLength", 20);
        isBackupActivated = configFile.getBoolean("BackupSystem.ActivateBackup", false);
        isBackupOnStart = configFile.getBoolean("BackupSystem.OnServerStart", false);
        isBackupOnStop = configFile.getBoolean("BackupSystem.OnServeStop", false);
        backupWindowsPath = configFile.getString("BackupSystem.MysqlWindowsPath", "C:\\Program Files\\MySQL\\MySQL Server 5.1\\");
        enablePasspartu = configFile.getBoolean("Passpartu.enablePasspartu", false);
        isStopEnabled = configFile.getBoolean("Security.SQLProblem.stopServer", true);
        reloadSupport = configFile.getBoolean("Security.ReloadCommand.useReloadCommandSupport", true);
        allowCommands = (List<String>) configFile.getList("settings.restrictions.allowCommands");
        if (configFile.contains("allowCommands")) {
            if (!allowCommands.contains("/login"))
                allowCommands.add("/login");
            if (!allowCommands.contains("/register"))
                allowCommands.add("/register");
            if (!allowCommands.contains("/l"))
                allowCommands.add("/l");
            if (!allowCommands.contains("/reg"))
                allowCommands.add("/reg");
            if (!allowCommands.contains("/passpartu"))
                allowCommands.add("/passpartu");
            if (!allowCommands.contains("/email"))
                allowCommands.add("/email");
            if (!allowCommands.contains("/captcha"))
                allowCommands.add("/captcha");
        }
        rakamakUsers = configFile.getString("Converter.Rakamak.fileName", "users.rak");
        rakamakUsersIp = configFile.getString("Converter.Rakamak.ipFileName", "UsersIp.rak");
        rakamakUseIp = configFile.getBoolean("Converter.Rakamak.useIp", false);
        noConsoleSpam = configFile.getBoolean("Security.console.noConsoleSpam", false);
        removePassword = configFile.getBoolean("Security.console.removePassword", true);
        getmailAccount = configFile.getString("Email.mailAccount", "");
        getmailPassword = configFile.getString("Email.mailPassword", "");
        getmailSMTP = configFile.getString("Email.mailSMTP", "smtp.gmail.com");
        getMailPort = configFile.getInt("Email.mailPort", 465);
        getRecoveryPassLength = configFile.getInt("Email.RecoveryPasswordLength", 8);
        getMySQLOtherUsernameColumn = (List<String>) configFile.getList("ExternalBoardOptions.mySQLOtherUsernameColumns", new ArrayList<String>());
        displayOtherAccounts = configFile.getBoolean("settings.restrictions.displayOtherAccounts", true);
        getMySQLColumnId = configFile.getString("DataSource.mySQLColumnId", "id");
        getmailSenderName = configFile.getString("Email.mailSenderName", "");
        useCaptcha = configFile.getBoolean("Security.captcha.useCaptcha", false);
        maxLoginTry = configFile.getInt("Security.captcha.maxLoginTry", 5);
        captchaLength = configFile.getInt("Security.captcha.captchaLength", 5);
        getMailSubject = configFile.getString("Email.mailSubject", "Your new AuthMe Password");
        getMailText = configFile.getString("Email.mailText", "Dear <playername>, <br /><br /> This is your new AuthMe password for the server <br /><br /> <servername> : <br /><br /> <generatedpass><br /><br />Do not forget to change password after login! <br /> /changepassword <generatedpass> newPassword");
        emailRegistration = configFile.getBoolean("settings.registration.enableEmailRegistrationSystem", false);
        saltLength = configFile.getInt("settings.security.doubleMD5SaltLength", 8);
        getmaxRegPerEmail = configFile.getInt("Email.maxRegPerEmail", 1);
        multiverse = configFile.getBoolean("Hooks.multiverse", true);
        chestshop = configFile.getBoolean("Hooks.chestshop", true);
        notifications = configFile.getBoolean("Hooks.notifications", true);
        bungee = configFile.getBoolean("Hooks.bungeecord", false);
        getForcedWorlds = (List<String>) configFile.getList("settings.restrictions.ForceSpawnOnTheseWorlds", new ArrayList<String>());
        banUnsafeIp = configFile.getBoolean("settings.restrictions.banUnsafedIP", false);
        doubleEmailCheck = configFile.getBoolean("settings.registration.doubleEmailCheck", false);
        sessionExpireOnIpChange = configFile.getBoolean("settings.sessions.sessionExpireOnIpChange", false);
        useLogging = configFile.getBoolean("Security.console.logConsole", false);
        disableSocialSpy = configFile.getBoolean("Hooks.disableSocialSpy", true);
        bCryptLog2Rounds = configFile.getInt("ExternalBoardOptions.bCryptLog2Round", 10);
        forceOnlyAfterLogin = configFile.getBoolean("settings.GameMode.ForceOnlyAfterLogin", false);
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
        // purgePermissions = configFile.getBoolean("Purge.removePermissions",
        // false);
        enableProtection = configFile.getBoolean("Protection.enableProtection", false);
        countries = (List<String>) configFile.getList("Protection.countries", new ArrayList<String>());
        enableAntiBot = configFile.getBoolean("Protection.enableAntiBot", false);
        antiBotSensibility = configFile.getInt("Protection.antiBotSensibility", 5);
        antiBotDuration = configFile.getInt("Protection.antiBotDuration", 10);
        forceCommands = (List<String>) configFile.getList("settings.forceCommands", new ArrayList<String>());
        forceCommandsAsConsole = (List<String>) configFile.getList("settings.forceCommandsAsConsole", new ArrayList<String>());
        recallEmail = configFile.getBoolean("Email.recallPlayers", false);
        delayRecall = configFile.getInt("Email.delayRecall", 5);
        useWelcomeMessage = configFile.getBoolean("settings.useWelcomeMessage", true);
        unsafePasswords = (List<String>) configFile.getList("settings.security.unsafePasswords", new ArrayList<String>());
        countriesBlacklist = (List<String>) configFile.getList("Protection.countriesBlacklist", new ArrayList<String>());
        broadcastWelcomeMessage = configFile.getBoolean("settings.broadcastWelcomeMessage", false);
        forceRegKick = configFile.getBoolean("settings.registration.forceKickAfterRegister", false);
        forceRegLogin = configFile.getBoolean("settings.registration.forceLoginAfterRegister", false);
        getMySQLColumnLogged = configFile.getString("DataSource.mySQLColumnLogged", "isLogged");
        spawnPriority = configFile.getString("settings.restrictions.spawnPriority", "authme,essentials,multiverse,default");
        getMaxLoginPerIp = configFile.getInt("settings.restrictions.maxLoginPerIp", 0);
        getMaxJoinPerIp = configFile.getInt("settings.restrictions.maxJoinPerIp", 0);
        checkVeryGames = configFile.getBoolean("VeryGames.enableIpCheck", false);
        delayJoinMessage = configFile.getBoolean("settings.delayJoinMessage", false);
        noTeleport = configFile.getBoolean("settings.restrictions.noTeleport", false);
        crazyloginFileName = configFile.getString("Converter.CrazyLogin.fileName", "accounts.db");
        getPassRegex = configFile.getString("settings.restrictions.allowedPasswordCharacters", "[a-zA-Z0-9_?!@+&-]*");
        applyBlindEffect = configFile.getBoolean("settings.applyBlindEffect", false);
        emailBlacklist = configFile.getStringList("Email.emailBlacklisted");
        emailWhitelist = configFile.getStringList("Email.emailWhitelisted");
        forceRegisterCommands = (List<String>) configFile.getList("settings.forceRegisterCommands", new ArrayList<String>());
        forceRegisterCommandsAsConsole = (List<String>) configFile.getList("settings.forceRegisterCommandsAsConsole", new ArrayList<String>());

        // Load the welcome message
        getWelcomeMessage(plugin);

        saveDefaults();
    }

    @SuppressWarnings("unchecked")
    public static void reloadConfigOptions(YamlConfiguration newConfig) {
        configFile = newConfig;

        messagesLanguage = checkLang(configFile.getString("settings.messagesLanguage", "en"));
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
        isAllowRestrictedIp = configFile.getBoolean("settings.restrictions.AllowRestrictedUser", false);
        getRestrictedIp = configFile.getStringList("settings.restrictions.AllowedRestrictedUser");
        isMovementAllowed = configFile.getBoolean("settings.restrictions.allowMovement", false);
        getMovementRadius = configFile.getInt("settings.restrictions.allowedMovementRadius", 100);
        getJoinPermissions = configFile.getStringList("GroupOptions.Permissions.PermissionsOnJoin");
        isKickOnWrongPasswordEnabled = configFile.getBoolean("settings.restrictions.kickOnWrongPassword", false);
        isKickNonRegisteredEnabled = configFile.getBoolean("settings.restrictions.kickNonRegistered", false);
        isForceSingleSessionEnabled = configFile.getBoolean("settings.restrictions.ForceSingleSession", true);
        isForceSpawnLocOnJoinEnabled = configFile.getBoolean("settings.restrictions.ForceSpawnLocOnJoinEnabled", false);
        isSaveQuitLocationEnabled = configFile.getBoolean("settings.restrictions.SaveQuitLocation", false);
        isForceSurvivalModeEnabled = configFile.getBoolean("settings.GameMode.ForceSurvivalMode", false);
        isResetInventoryIfCreative = configFile.getBoolean("settings.GameMode.ResetInventoryIfCreative", false);
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
        getMySQLlastlocX = configFile.getString("DataSource.mySQLlastlocX", "x");
        getMySQLlastlocY = configFile.getString("DataSource.mySQLlastlocY", "y");
        getMySQLlastlocZ = configFile.getString("DataSource.mySQLlastlocZ", "z");
        getMySQLlastlocWorld = configFile.getString("DataSource.mySQLlastlocWorld", "world");
        getMySQLColumnSalt = configFile.getString("ExternalBoardOptions.mySQLColumnSalt", "");
        getMySQLColumnGroup = configFile.getString("ExternalBoardOptions.mySQLColumnGroup", "");
        getNonActivatedGroup = configFile.getInt("ExternalBoardOptions.nonActivedUserGroup", -1);
        unRegisteredGroup = configFile.getString("GroupOptions.UnregisteredPlayerGroup", "");
        getUnrestrictedName = configFile.getStringList("settings.unrestrictions.UnrestrictedName");
        getRegisteredGroup = configFile.getString("GroupOptions.RegisteredPlayerGroup", "");
        getEnablePasswordVerifier = configFile.getBoolean("settings.restrictions.enablePasswordVerifier", true);
        protectInventoryBeforeLogInEnabled = configFile.getBoolean("settings.restrictions.ProtectInventoryBeforeLogIn", true);
        passwordMaxLength = configFile.getInt("settings.security.passwordMaxLength", 20);
        isBackupActivated = configFile.getBoolean("BackupSystem.ActivateBackup", false);
        isBackupOnStart = configFile.getBoolean("BackupSystem.OnServerStart", false);
        isBackupOnStop = configFile.getBoolean("BackupSystem.OnServeStop", false);
        backupWindowsPath = configFile.getString("BackupSystem.MysqlWindowsPath", "C:\\Program Files\\MySQL\\MySQL Server 5.1\\");
        enablePasspartu = configFile.getBoolean("Passpartu.enablePasspartu", false);
        isStopEnabled = configFile.getBoolean("Security.SQLProblem.stopServer", true);
        reloadSupport = configFile.getBoolean("Security.ReloadCommand.useReloadCommandSupport", true);
        allowCommands = (List<String>) configFile.getList("settings.restrictions.allowCommands");
        if (configFile.contains("allowCommands")) {
            if (!allowCommands.contains("/login"))
                allowCommands.add("/login");
            if (!allowCommands.contains("/register"))
                allowCommands.add("/register");
            if (!allowCommands.contains("/l"))
                allowCommands.add("/l");
            if (!allowCommands.contains("/reg"))
                allowCommands.add("/reg");
            if (!allowCommands.contains("/passpartu"))
                allowCommands.add("/passpartu");
            if (!allowCommands.contains("/email"))
                allowCommands.add("/email");
            if (!allowCommands.contains("/captcha"))
                allowCommands.add("/captcha");
        }
        rakamakUsers = configFile.getString("Converter.Rakamak.fileName", "users.rak");
        rakamakUsersIp = configFile.getString("Converter.Rakamak.ipFileName", "UsersIp.rak");
        rakamakUseIp = configFile.getBoolean("Converter.Rakamak.useIp", false);
        noConsoleSpam = configFile.getBoolean("Security.console.noConsoleSpam", false);
        removePassword = configFile.getBoolean("Security.console.removePassword", true);
        getmailAccount = configFile.getString("Email.mailAccount", "");
        getmailPassword = configFile.getString("Email.mailPassword", "");
        getmailSMTP = configFile.getString("Email.mailSMTP", "smtp.gmail.com");
        getMailPort = configFile.getInt("Email.mailPort", 465);
        getRecoveryPassLength = configFile.getInt("Email.RecoveryPasswordLength", 8);
        getMySQLOtherUsernameColumn = (List<String>) configFile.getList("ExternalBoardOptions.mySQLOtherUsernameColumns", new ArrayList<String>());
        displayOtherAccounts = configFile.getBoolean("settings.restrictions.displayOtherAccounts", true);
        getMySQLColumnId = configFile.getString("DataSource.mySQLColumnId", "id");
        getmailSenderName = configFile.getString("Email.mailSenderName", "");
        useCaptcha = configFile.getBoolean("Security.captcha.useCaptcha", false);
        maxLoginTry = configFile.getInt("Security.captcha.maxLoginTry", 5);
        captchaLength = configFile.getInt("Security.captcha.captchaLength", 5);
        getMailSubject = configFile.getString("Email.mailSubject", "Your new AuthMe Password");
        getMailText = configFile.getString("Email.mailText", "Dear <playername>, <br /><br /> This is your new AuthMe password for the server <br /><br /> <servername> : <br /><br /> <generatedpass><br /><br />Do not forget to change password after login! <br /> /changepassword <generatedpass> newPassword");
        emailRegistration = configFile.getBoolean("settings.registration.enableEmailRegistrationSystem", false);
        saltLength = configFile.getInt("settings.security.doubleMD5SaltLength", 8);
        getmaxRegPerEmail = configFile.getInt("Email.maxRegPerEmail", 1);
        multiverse = configFile.getBoolean("Hooks.multiverse", true);
        chestshop = configFile.getBoolean("Hooks.chestshop", true);
        notifications = configFile.getBoolean("Hooks.notifications", true);
        bungee = configFile.getBoolean("Hooks.bungeecord", false);
        getForcedWorlds = (List<String>) configFile.getList("settings.restrictions.ForceSpawnOnTheseWorlds");
        banUnsafeIp = configFile.getBoolean("settings.restrictions.banUnsafedIP", false);
        doubleEmailCheck = configFile.getBoolean("settings.registration.doubleEmailCheck", false);
        sessionExpireOnIpChange = configFile.getBoolean("settings.sessions.sessionExpireOnIpChange", false);
        useLogging = configFile.getBoolean("Security.console.logConsole", false);
        disableSocialSpy = configFile.getBoolean("Hooks.disableSocialSpy", true);
        bCryptLog2Rounds = configFile.getInt("ExternalBoardOptions.bCryptLog2Round", 10);
        forceOnlyAfterLogin = configFile.getBoolean("settings.GameMode.ForceOnlyAfterLogin", false);
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
        // purgePermissions = configFile.getBoolean("Purge.removePermissions",
        // false);
        enableProtection = configFile.getBoolean("Protection.enableProtection", false);
        countries = (List<String>) configFile.getList("Protection.countries");
        enableAntiBot = configFile.getBoolean("Protection.enableAntiBot", false);
        antiBotSensibility = configFile.getInt("Protection.antiBotSensibility", 5);
        antiBotDuration = configFile.getInt("Protection.antiBotDuration", 10);
        forceCommands = (List<String>) configFile.getList("settings.forceCommands", new ArrayList<String>());
        forceCommandsAsConsole = (List<String>) configFile.getList("settings.forceCommandsAsConsole", new ArrayList<String>());
        recallEmail = configFile.getBoolean("Email.recallPlayers", false);
        delayRecall = configFile.getInt("Email.delayRecall", 5);
        useWelcomeMessage = configFile.getBoolean("settings.useWelcomeMessage", true);
        unsafePasswords = (List<String>) configFile.getList("settings.security.unsafePasswords", new ArrayList<String>());
        countriesBlacklist = (List<String>) configFile.getList("Protection.countriesBlacklist", new ArrayList<String>());
        broadcastWelcomeMessage = configFile.getBoolean("settings.broadcastWelcomeMessage", false);
        forceRegKick = configFile.getBoolean("settings.registration.forceKickAfterRegister", false);
        forceRegLogin = configFile.getBoolean("settings.registration.forceLoginAfterRegister", false);
        getMySQLColumnLogged = configFile.getString("DataSource.mySQLColumnLogged", "isLogged");
        spawnPriority = configFile.getString("settings.restrictions.spawnPriority", "authme,essentials,multiverse,default");
        getMaxLoginPerIp = configFile.getInt("settings.restrictions.maxLoginPerIp", 0);
        getMaxJoinPerIp = configFile.getInt("settings.restrictions.maxJoinPerIp", 0);
        checkVeryGames = configFile.getBoolean("VeryGames.enableIpCheck", false);
        delayJoinMessage = configFile.getBoolean("settings.delayJoinMessage", false);
        noTeleport = configFile.getBoolean("settings.restrictions.noTeleport", false);
        crazyloginFileName = configFile.getString("Converter.CrazyLogin.fileName", "accounts.db");
        getPassRegex = configFile.getString("settings.restrictions.allowedPasswordCharacters", "[a-zA-Z0-9_?!@+&-]*");
        applyBlindEffect = configFile.getBoolean("settings.applyBlindEffect", false);
        emailBlacklist = configFile.getStringList("Email.emailBlacklisted");
        emailWhitelist = configFile.getStringList("Email.emailWhitelisted");
        forceRegisterCommands = (List<String>) configFile.getList("settings.forceRegisterCommands", new ArrayList<String>());
        forceRegisterCommandsAsConsole = (List<String>) configFile.getList("settings.forceRegisterCommandsAsConsole", new ArrayList<String>());

        // Reload the welcome message
        getWelcomeMessage(AuthMe.getInstance());

    }

    public void mergeConfig() {
        boolean changes = false;
        if (contains("Xenoforo.predefinedSalt"))
            set("Xenoforo.predefinedSalt", null);
        if (configFile.getString("settings.security.passwordHash", "SHA256").toUpperCase().equals("XFSHA1") || configFile.getString("settings.security.passwordHash", "SHA256").toUpperCase().equals("XFSHA256"))
            set("settings.security.passwordHash", "XENFORO");
        if (!contains("Protection.enableProtection")) {
            set("Protection.enableProtection", false);
            changes = true;
        }
        if (!contains("Protection.countries")) {
            countries = new ArrayList<String>();
            countries.add("US");
            countries.add("GB");
            set("Protection.countries", countries);
            changes = true;
        }
        if (!contains("Protection.enableAntiBot")) {
            set("Protection.enableAntiBot", false);
            changes = true;
        }
        if (!contains("Protection.antiBotSensibility")) {
            set("Protection.antiBotSensibility", 5);
            changes = true;
        }
        if (!contains("Protection.antiBotDuration")) {
            set("Protection.antiBotDuration", 10);
            changes = true;
        }
        if (!contains("settings.forceCommands")) {
            set("settings.forceCommands", new ArrayList<String>());
            changes = true;
        }
        if (!contains("settings.forceCommandsAsConsole")) {
            set("settings.forceCommandsAsConsole", new ArrayList<String>());
            changes = true;
        }
        if (!contains("Email.recallPlayers")) {
            set("Email.recallPlayers", false);
            changes = true;
        }
        if (!contains("Email.delayRecall")) {
            set("Email.delayRecall", 5);
            changes = true;
        }
        if (!contains("settings.useWelcomeMessage")) {
            set("settings.useWelcomeMessage", true);
            changes = true;
        }
        if (!contains("settings.security.unsafePasswords")) {
            List<String> str = new ArrayList<String>();
            str.add("123456");
            str.add("password");
            set("settings.security.unsafePasswords", str);
            changes = true;
        }
        if (!contains("Protection.countriesBlacklist")) {
            countriesBlacklist = new ArrayList<String>();
            countriesBlacklist.add("A1");
            set("Protection.countriesBlacklist", countriesBlacklist);
            changes = true;
        }
        if (!contains("settings.broadcastWelcomeMessage")) {
            set("settings.broadcastWelcomeMessage", false);
            changes = true;
        }
        if (!contains("settings.registration.forceKickAfterRegister")) {
            set("settings.registration.forceKickAfterRegister", false);
            changes = true;
        }
        if (!contains("settings.registration.forceLoginAfterRegister")) {
            set("settings.registration.forceLoginAfterRegister", false);
            changes = true;
        }
        if (!contains("DataSource.mySQLColumnLogged")) {
            set("DataSource.mySQLColumnLogged", "isLogged");
            changes = true;
        }
        if (!contains("settings.restrictions.spawnPriority")) {
            set("settings.restrictions.spawnPriority", "authme,essentials,multiverse,default");
            changes = true;
        }
        if (!contains("settings.restrictions.maxLoginPerIp")) {
            set("settings.restrictions.maxLoginPerIp", 0);
            changes = true;
        }
        if (!contains("settings.restrictions.maxJoinPerIp")) {
            set("settings.restrictions.maxJoinPerIp", 0);
            changes = true;
        }
        if (!contains("VeryGames.enableIpCheck")) {
            set("VeryGames.enableIpCheck", false);
            changes = true;
        }
        if (getString("settings.restrictions.allowedNicknameCharacters").equals("[a-zA-Z0-9_?]*"))
            set("settings.restrictions.allowedNicknameCharacters", "[a-zA-Z0-9_]*");
        if (!contains("settings.delayJoinMessage")) {
            set("settings.delayJoinMessage", false);
            changes = true;
        }
        if (!contains("settings.restrictions.noTeleport")) {
            set("settings.restrictions.noTeleport", false);
            changes = true;
        }
        if (contains("Converter.Rakamak.newPasswordHash"))
            set("Converter.Rakamak.newPasswordHash", null);
        if (!contains("Converter.CrazyLogin.fileName")) {
            set("Converter.CrazyLogin.fileName", "accounts.db");
            changes = true;
        }
        if (!contains("settings.restrictions.allowedPasswordCharacters")) {
            set("settings.restrictions.allowedPasswordCharacters", "[a-zA-Z0-9_?!@+&-]*");
            changes = true;
        }
        if (!contains("settings.applyBlindEffect")) {
            set("settings.applyBlindEffect", false);
            changes = true;
        }
        if (!contains("Email.emailBlacklisted")) {
            set("Email.emailBlacklisted", new ArrayList<String>());
            changes = true;
        }
        if (contains("Performances.useMultiThreading"))
            set("Performances.useMultiThreading", null);
        if (!contains("Email.emailWhitelisted")) {
            set("Email.emailWhitelisted", new ArrayList<String>());
            changes = true;
        }
        if (!contains("settings.forceRegisterCommands")) {
            set("settings.forceRegisterCommands", new ArrayList<String>());
            changes = true;
        }
        if (!contains("settings.forceRegisterCommandsAsConsole")) {
            set("settings.forceRegisterCommandsAsConsole", new ArrayList<String>());
            changes = true;
        }

        if (changes) {
            plugin.getLogger().warning("Merge new Config Options - I'm not an error, please don't report me");
            plugin.getLogger().warning("Please check your config.yml file for new configs!");
        }
        plugin.saveConfig();

        return;
    }

    private static HashAlgorithm getPasswordHash() {
        String key = "settings.security.passwordHash";
        try {
            return HashAlgorithm.valueOf(configFile.getString(key, "SHA256").toUpperCase());
        } catch (IllegalArgumentException ex) {
            ConsoleLogger.showError("Unknown Hash Algorithm; defaulting to SHA256");
            return HashAlgorithm.SHA256;
        }
    }

    private static DataSourceType getDataSource() {
        String key = "DataSource.backend";
        try {
            return DataSource.DataSourceType.valueOf(configFile.getString(key).toUpperCase());
        } catch (IllegalArgumentException ex) {
            ConsoleLogger.showError("Unknown database backend; defaulting to file database");
            return DataSource.DataSourceType.FILE;
        }
    }

    /**
     * Config option for setting and check restricted user by username;ip ,
     * return false if ip and name doesnt amtch with player that join the
     * server, so player has a restricted access
     */
    public static Boolean getRestrictedIp(String name, String ip) {

        Iterator<String> iter = getRestrictedIp.iterator();
        Boolean trueonce = false;
        Boolean namefound = false;
        while (iter.hasNext()) {
            String[] args = iter.next().split(";");
            String testname = args[0];
            String testip = args[1];
            if (testname.equalsIgnoreCase(name)) {
                namefound = true;
                if (testip.equalsIgnoreCase(ip)) {
                    trueonce = true;
                }
                ;
            }
        }
        if (namefound == false) {
            return true;
        } else {
            if (trueonce == true) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Loads the configuration from disk
     *
     * @return True if loaded successfully
     */
    public final boolean load() {
        try {
            load(file);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public final void reload() {
        load();
        loadDefaults(file.getName());
    }

    /**
     * Saves the configuration to disk
     *
     * @return True if saved successfully
     */
    public final boolean save() {
        try {
            save(file);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Simple function for if the Configuration file exists
     *
     * @return True if configuration exists on disk
     */
    public final boolean exists() {
        return file.exists();
    }

    /**
     * Loads a file from the plugin jar and sets as default
     *
     * @param filename
     *            The filename to open
     */
    public final void loadDefaults(String filename) {
        InputStream stream = plugin.getResource(filename);
        if (stream == null)
            return;

        setDefaults(YamlConfiguration.loadConfiguration(stream));
    }

    /**
     * Saves current configuration (plus defaults) to disk.
     *
     * If defaults and configuration are empty, saves blank file.
     *
     * @return True if saved successfully
     */
    public final boolean saveDefaults() {
        options().copyDefaults(true);
        options().copyHeader(true);
        boolean success = save();
        options().copyDefaults(false);
        options().copyHeader(false);
        return success;
    }

    /**
     * Clears current configuration defaults
     */
    public final void clearDefaults() {
        setDefaults(new MemoryConfiguration());
    }

    /**
     * Check loaded defaults against current configuration
     *
     * @return false When all defaults aren't present in config
     */
    public boolean checkDefaults() {
        if (getDefaults() == null) {
            return true;
        }
        return getKeys(true).containsAll(getDefaults().getKeys(true));
    }

    public static String checkLang(String lang) {
        for (messagesLang language : messagesLang.values()) {
            if (lang.toLowerCase().contains(language.toString())) {
                ConsoleLogger.info("Set Language: " + lang);
                return lang;
            }
        }
        ConsoleLogger.info("Set Default Language: En ");
        return "en";
    }

    public static void switchAntiBotMod(boolean mode) {
        if (mode)
            isKickNonRegisteredEnabled = true;
        else isKickNonRegisteredEnabled = configFile.getBoolean("settings.restrictions.kickNonRegistered", false);
    }

    private static void getWelcomeMessage(AuthMe plugin) {
        welcomeMsg = new ArrayList<String>();
        if (!useWelcomeMessage) {
            return;
        }
        if (!(new File(plugin.getDataFolder() + File.separator + "welcome.txt").exists())) {
            try {
                FileWriter fw = new FileWriter(plugin.getDataFolder() + File.separator + "welcome.txt", true);
                BufferedWriter w = new BufferedWriter(fw);
                w.write("Welcome {PLAYER} on {SERVER} server");
                w.newLine();
                w.write("This server use AuthMe protection!");
                w.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileReader fr = new FileReader(plugin.getDataFolder() + File.separator + "welcome.txt");
            BufferedReader br = new BufferedReader(fr);
            String line = "";
            while ((line = br.readLine()) != null) {
                welcomeMsg.add(line);
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isEmailCorrect(String email) {
        if (!email.contains("@"))
            return false;
        if (email.equalsIgnoreCase("your@email.com"))
            return false;
        String emailDomain = email.split("@")[1];
        boolean correct = true;
        if (emailWhitelist != null && !emailWhitelist.isEmpty()) {
            for (String domain : emailWhitelist) {
                if (!domain.equalsIgnoreCase(emailDomain)) {
                    correct = false;
                } else {
                    correct = true;
                    break;
                }
            }
            return correct;
        }
        if (emailBlacklist != null && !emailBlacklist.isEmpty()) {
            for (String domain : emailBlacklist) {
                if (domain.equalsIgnoreCase(emailDomain)) {
                    correct = false;
                    break;
                }
            }
        }
        return correct;
    }

    public enum messagesLang {
        en,
        de,
        br,
        cz,
        pl,
        fr,
        uk,
        ru,
        hu,
        sk,
        es,
        fi,
        zhtw,
        zhhk,
        zhcn,
        lt,
        it,
        ko,
        pt,
        nl,
        gl
    }
}
