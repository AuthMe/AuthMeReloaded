<!-- AUTO-GENERATED FILE! Do not edit this directly -->
<!-- File auto-generated on Sun Apr 22 11:00:10 CEST 2018. See docs/config/config.tpl.md -->

## AuthMe Configuration
The first time you run AuthMe it will create a config.yml file in the plugins/AuthMe folder, 
with which you can configure various settings. This following is the initial contents of
the generated config.yml file.

```yml

DataSource:
    # What type of database do you want to use?
    # Valid values: SQLITE, MYSQL
    backend: 'SQLITE'
    # Enable the database caching system, should be disabled on bungeecord environments
    # or when a website integration is being used.
    caching: true
    # Database host address
    mySQLHost: '127.0.0.1'
    # Database port
    mySQLPort: '3306'
    # Connect to MySQL database over SSL
    mySQLUseSSL: true
    # Username to connect to the MySQL database
    mySQLUsername: 'authme'
    # Password to connect to the MySQL database
    mySQLPassword: '12345'
    # Database Name, use with converters or as SQLITE database name
    mySQLDatabase: 'authme'
    # Table of the database
    mySQLTablename: 'authme'
    # Column of IDs to sort data
    mySQLColumnId: 'id'
    # Column for storing or checking players nickname
    mySQLColumnName: 'username'
    # Column for storing or checking players RealName
    mySQLRealName: 'realname'
    # Column for storing players passwords
    mySQLColumnPassword: 'password'
    # Column for storing players emails
    mySQLColumnEmail: 'email'
    # Column for storing if a player is logged in or not
    mySQLColumnLogged: 'isLogged'
    # Column for storing if a player has a valid session or not
    mySQLColumnHasSession: 'hasSession'
    # Column for storing a player's TOTP key (for two-factor authentication)
    mySQLtotpKey: 'totp'
    # Column for storing the player's last IP
    mySQLColumnIp: 'ip'
    # Column for storing players lastlogins
    mySQLColumnLastLogin: 'lastlogin'
    # Column storing the registration date
    mySQLColumnRegisterDate: 'regdate'
    # Column for storing the IP address at the time of registration
    mySQLColumnRegisterIp: 'regip'
    # Column for storing player LastLocation - X
    mySQLlastlocX: 'x'
    # Column for storing player LastLocation - Y
    mySQLlastlocY: 'y'
    # Column for storing player LastLocation - Z
    mySQLlastlocZ: 'z'
    # Column for storing player LastLocation - World Name
    mySQLlastlocWorld: 'world'
    # Column for storing player LastLocation - Yaw
    mySQLlastlocYaw: 'yaw'
    # Column for storing player LastLocation - Pitch
    mySQLlastlocPitch: 'pitch'
    # Overrides the size of the DB Connection Pool, default = 10
    poolSize: 10
    # The maximum lifetime of a connection in the pool, default = 1800 seconds
    # You should set this at least 30 seconds less than mysql server wait_timeout
    maxLifetime: 1800
ExternalBoardOptions:
    # Column for storing players passwords salts
    mySQLColumnSalt: ''
    # Column for storing players groups
    mySQLColumnGroup: ''
    # -1 means disabled. If you want that only activated players
    # can log into your server, you can set here the group number
    # of unactivated users, needed for some forum/CMS support
    nonActivedUserGroup: -1
    # Other MySQL columns where we need to put the username (case-sensitive)
    mySQLOtherUsernameColumns: []
    # How much log2 rounds needed in BCrypt (do not change if you do not know what it does)
    bCryptLog2Round: 10
    # phpBB table prefix defined during the phpBB installation process
    phpbbTablePrefix: 'phpbb_'
    # phpBB activated group ID; 2 is the default registered group defined by phpBB
    phpbbActivatedGroupId: 2
    # IP Board table prefix defined during the IP Board installation process
    IPBTablePrefix: 'ipb_'
    # IP Board default group ID; 3 is the default registered group defined by IP Board
    IPBActivatedGroupId: 3
    # Xenforo table prefix defined during the Xenforo installation process
    XFTablePrefix: 'xf_'
    # XenForo default group ID; 2 is the default registered group defined by Xenforo
    XFActivatedGroupId: 2
    # Wordpress prefix defined during WordPress installation
    wordpressTablePrefix: 'wp_'
settings:
    sessions:
        # Do you want to enable the session feature?
        # If enabled, when a player authenticates successfully,
        # his IP and his nickname is saved.
        # The next time the player joins the server, if his IP
        # is the same as last time and the timeout hasn't
        # expired, he will not need to authenticate.
        enabled: false
        # After how many minutes should a session expire?
        # A player's session ends after the timeout or if his IP has changed
        timeout: 10
    # Message language, available languages:
    # https://github.com/AuthMe/AuthMeReloaded/blob/master/docs/translations.md
    messagesLanguage: 'en'
    # Forces authme to hook into Vault instead of a specific permission handler system.
    forceVaultHook: false
    # Log level: INFO, FINE, DEBUG. Use INFO for general messages,
    # FINE for some additional detailed ones (like password failed),
    # and DEBUG for debugging
    logLevel: 'FINE'
    # By default we schedule async tasks when talking to the database. If you want
    # typical communication with the database to happen synchronously, set this to false
    useAsyncTasks: true
    # By default we handle the AsyncPlayerPreLoginEvent which makes the plugin faster
    # but it is incompatible with any permission plugin not included in our compatibility list.
    # If you have issues with permission checks on player join please disable this option.
    useAsyncPreLoginEvent: true
    restrictions:
        # Can not authenticated players chat?
        # Keep in mind that this feature also blocks all commands not
        # listed in the list below.
        allowChat: false
        # Hide the chat log from players who are not authenticated?
        hideChat: false
        # Allowed commands for unauthenticated players
        allowCommands: 
        - '/login'
        - '/register'
        - '/l'
        - '/reg'
        - '/email'
        - '/captcha'
        - '/2fa'
        - '/totp'
        # Max number of allowed registrations per IP
        # The value 0 means an unlimited number of registrations!
        maxRegPerIp: 1
        # Minimum allowed username length
        minNicknameLength: 3
        # Maximum allowed username length
        maxNicknameLength: 16
        # When this setting is enabled, online players can't be kicked out
        # due to "Logged in from another Location"
        # This setting will prevent potential security exploits.
        ForceSingleSession: true
        ForceSpawnLocOnJoin:
            # If enabled, every player that spawn in one of the world listed in
            # "ForceSpawnLocOnJoin.worlds" will be teleported to the spawnpoint after successful
            # authentication. The quit location of the player will be overwritten.
            # This is different from "teleportUnAuthedToSpawn" that teleport player
            # to the spawnpoint on join.
            enabled: false
            # WorldNames where we need to force the spawn location
            # Case-sensitive!
            worlds: 
            - 'world'
            - 'world_nether'
            - 'world_the_end'
        # This option will save the quit location of the players.
        SaveQuitLocation: false
        # To activate the restricted user feature you need
        # to enable this option and configure the AllowedRestrictedUser field.
        AllowRestrictedUser: false
        # The restricted user feature will kick players listed below
        # if they don't match the defined IP address. Names are case-insensitive.
        # You can use * as wildcard (127.0.0.*), or regex with a "regex:" prefix regex:127\.0\.0\..*
        # Example:
        #     AllowedRestrictedUser:
        #     - playername;127.0.0.1
        #     - playername;regex:127\.0\.0\..*
        AllowedRestrictedUser: []
        # Ban unknown IPs trying to log in with a restricted username?
        banUnsafedIP: false
        # Should unregistered players be kicked immediately?
        kickNonRegistered: false
        # Should players be kicked on wrong password?
        kickOnWrongPassword: true
        # Should not logged in players be teleported to the spawn?
        # After the authentication they will be teleported back to
        # their normal position.
        teleportUnAuthedToSpawn: false
        # Can unregistered players walk around?
        allowMovement: false
        # After how many seconds should players who fail to login or register
        # be kicked? Set to 0 to disable.
        timeout: 30
        # Regex pattern of allowed characters in the player name.
        allowedNicknameCharacters: '[a-zA-Z0-9_]*'
        # How far can unregistered players walk?
        # Set to 0 for unlimited radius
        allowedMovementRadius: 100
        # Should we protect the player inventory before logging in? Requires ProtocolLib.
        ProtectInventoryBeforeLogIn: true
        # Should we deny the tabcomplete feature before logging in? Requires ProtocolLib.
        DenyTabCompleteBeforeLogin: false
        # Should we display all other accounts from a player when he joins?
        # permission: /authme.admin.accounts
        displayOtherAccounts: true
        # Spawn priority; values: authme, essentials, cmi, multiverse, default
        spawnPriority: 'authme,essentials,cmi,multiverse,default'
        # Maximum Login authorized by IP
        maxLoginPerIp: 0
        # Maximum Join authorized by IP
        maxJoinPerIp: 0
        # AuthMe will NEVER teleport players if set to true!
        noTeleport: false
        # Regex syntax for allowed chars in passwords. The default [!-~] allows all visible ASCII
        # characters, which is what we recommend. See also http://asciitable.com
        # You can test your regex with https://regex101.com
        allowedPasswordCharacters: '[!-~]*'
    GameMode:
        # Force survival gamemode when player joins?
        ForceSurvivalMode: false
    unrestrictions:
        # Below you can list all account names that AuthMe will ignore
        # for registration or login. Configure it at your own risk!!
        # This option adds compatibility with BuildCraft and some other mods.
        # It is case-insensitive! Example:
        # UnrestrictedName:
        # - 'npcPlayer'
        # - 'npcPlayer2'
        UnrestrictedName: []
    security:
        # Minimum length of password
        minPasswordLength: 5
        # Maximum length of password
        passwordMaxLength: 30
        # Possible values: SHA256, BCRYPT, BCRYPT2Y, PBKDF2, SALTEDSHA512,
        # MYBB, IPB3, PHPBB, PHPFUSION, SMF, XENFORO, XAUTH, JOOMLA, WBB3, WBB4, MD5VB,
        # PBKDF2DJANGO, WORDPRESS, ROYALAUTH, ARGON2, CUSTOM (for developers only). See full list at
        # https://github.com/AuthMe/AuthMeReloaded/blob/master/docs/hash_algorithms.md
        # If you use ARGON2, check that you have the argon2 c library on your system
        passwordHash: 'SHA256'
        # If a password check fails, AuthMe will also try to check with the following hash methods.
        # Use this setting when you change from one hash method to another.
        # AuthMe will update the password to the new hash. Example:
        # legacyHashes:
        # - 'SHA1'
        legacyHashes: []
        # Salt length for the SALTED2MD5 MD5(MD5(password)+salt)
        doubleMD5SaltLength: 8
        # Number of rounds to use if passwordHash is set to PBKDF2. Default is 10000
        pbkdf2Rounds: 10000
        # Prevent unsafe passwords from being used; put them in lowercase!
        # You should always set 'help' as unsafePassword due to possible conflicts.
        # unsafePasswords:
        # - '123456'
        # - 'password'
        # - 'help'
        unsafePasswords: 
        - '123456'
        - 'password'
        - 'qwerty'
        - '12345'
        - '54321'
        - '123456789'
        - 'help'
    registration:
        # Enable registration on the server?
        enabled: true
        # Send every X seconds a message to a player to
        # remind him that he has to login/register
        messageInterval: 5
        # Only registered and logged in players can play.
        # See restrictions for exceptions
        force: true
        # Type of registration: PASSWORD or EMAIL
        # PASSWORD = account is registered with a password supplied by the user;
        # EMAIL = password is generated and sent to the email provided by the user.
        # More info at https://github.com/AuthMe/AuthMeReloaded/wiki/Registration
        type: 'PASSWORD'
        # Second argument the /register command should take: NONE = no 2nd argument
        # CONFIRMATION = must repeat first argument (pass or email)
        # EMAIL_OPTIONAL = for password register: 2nd argument can be empty or have email address
        # EMAIL_MANDATORY = for password register: 2nd argument MUST be an email address
        secondArg: 'CONFIRMATION'
        # Do we force kick a player after a successful registration?
        # Do not use with login feature below
        forceKickAfterRegister: false
        # Does AuthMe need to enforce a /login after a successful registration?
        forceLoginAfterRegister: false
    # Enable to display the welcome message (welcome.txt) after a login
    # You can use colors in this welcome.txt + some replaced strings:
    # {PLAYER}: player name, {ONLINE}: display number of online players,
    # {MAXPLAYERS}: display server slots, {IP}: player ip, {LOGINS}: number of players logged,
    # {WORLD}: player current world, {SERVER}: server name
    # {VERSION}: get current bukkit version, {COUNTRY}: player country
    useWelcomeMessage: true
    # Broadcast the welcome message to the server or only to the player?
    # set true for server or false for player
    broadcastWelcomeMessage: false
    # Should we delay the join message and display it once the player has logged in?
    delayJoinMessage: false
    # The custom join message that will be sent after a successful login,
    # keep empty to use the original one.
    # Available variables:
    # {PLAYERNAME}: the player name (no colors)
    # {DISPLAYNAME}: the player display name (with colors)
    # {DISPLAYNAMENOCOLOR}: the player display name (without colors)
    customJoinMessage: ''
    # Should we remove the leave messages of unlogged users?
    removeUnloggedLeaveMessage: false
    # Should we remove join messages altogether?
    removeJoinMessage: false
    # Should we remove leave messages altogether?
    removeLeaveMessage: false
    # Do we need to add potion effect Blinding before login/reigster?
    applyBlindEffect: false
    # Do we need to prevent people to login with another case?
    # If Xephi is registered, then Xephi can login, but not XEPHI/xephi/XePhI
    preventOtherCase: true
GroupOptions:
    # Enables switching a player to defined permission groups before they log in.
    # See below for a detailed explanation.
    enablePermissionCheck: false
    # This is a very important option: if a registered player joins the server
    # AuthMe will switch him to unLoggedInGroup. This should prevent all major exploits.
    # You can set up your permission plugin with this special group to have no permissions,
    # or only permission to chat (or permission to send private messages etc.).
    # The better way is to set up this group with few permissions, so if a player
    # tries to exploit an account they can do only what you've defined for the group.
    # After login, the player will be moved to his correct permissions group!
    # Please note that the group name is case-sensitive, so 'admin' is different from 'Admin'
    # Otherwise your group will be wiped and the player will join in the default group []!
    # Example: registeredPlayerGroup: 'NotLogged'
    registeredPlayerGroup: ''
    # Similar to above, unregistered players can be set to the following
    # permissions group
    unregisteredPlayerGroup: ''
Email:
    # Email SMTP server host
    mailSMTP: 'smtp.gmail.com'
    # Email SMTP server port
    mailPort: 465
    # Only affects port 25: enable TLS/STARTTLS?
    useTls: true
    # Email account which sends the mails
    mailAccount: ''
    # Email account password
    mailPassword: ''
    # Email address, fill when mailAccount is not the email address of the account
    mailAddress: ''
    # Custom sender name, replacing the mailAccount name in the email
    mailSenderName: ''
    # Recovery password length
    RecoveryPasswordLength: 8
    # Mail Subject
    mailSubject: 'Your new AuthMe password'
    # Like maxRegPerIP but with email
    maxRegPerEmail: 1
    # Recall players to add an email?
    recallPlayers: false
    # Delay in minute for the recall scheduler
    delayRecall: 5
    # Blacklist these domains for emails
    emailBlacklisted: 
    - '10minutemail.com'
    # Whitelist ONLY these domains for emails
    emailWhitelisted: []
    # Send the new password drawn in an image?
    generateImage: false
    # The OAuth2 token
    emailOauth2Token: ''
Hooks:
    # Do we need to hook with multiverse for spawn checking?
    multiverse: true
    # Do we need to hook with BungeeCord?
    bungeecord: false
    # Send player to this BungeeCord server after register/login
    sendPlayerTo: ''
    # Do we need to disable Essentials SocialSpy on join?
    disableSocialSpy: false
    # Do we need to force /motd Essentials command on join?
    useEssentialsMotd: false
Protection:
    # Enable some servers protection (country based login, antibot)
    enableProtection: false
    # Apply the protection also to registered usernames
    enableProtectionRegistered: true
    # Countries allowed to join the server and register. For country codes, see
    # https://dev.maxmind.com/geoip/legacy/codes/iso3166/
    # PLEASE USE QUOTES!
    countries: 
    - 'US'
    - 'GB'
    # Countries not allowed to join the server and register
    # PLEASE USE QUOTES!
    countriesBlacklist: 
    - 'A1'
    # Do we need to enable automatic antibot system?
    enableAntiBot: true
    # The interval in seconds
    antiBotInterval: 5
    # Max number of players allowed to login in the interval
    # before the AntiBot system is enabled automatically
    antiBotSensibility: 10
    # Duration in minutes of the antibot automatic system
    antiBotDuration: 10
    # Delay in seconds before the antibot activation
    antiBotDelay: 60
    quickCommands:
        # Kicks the player that issued a command before the defined time after the join process
        denyCommandsBeforeMilliseconds: 1000
Purge:
    # If enabled, AuthMe automatically purges old, unused accounts
    useAutoPurge: false
    # Number of days after which an account should be purged
    daysBeforeRemovePlayer: 60
    # Do we need to remove the player.dat file during purge process?
    removePlayerDat: false
    # Do we need to remove the Essentials/userdata/player.yml file during purge process?
    removeEssentialsFile: false
    # World in which the players.dat are stored
    defaultWorld: 'world'
    # Remove LimitedCreative/inventories/player.yml, player_creative.yml files during purge?
    removeLimitedCreativesInventories: false
    # Do we need to remove the AntiXRayData/PlayerData/player file during purge process?
    removeAntiXRayFile: false
    # Do we need to remove permissions?
    removePermissions: false
Security:
    SQLProblem:
        # Stop the server if we can't contact the sql database
        # Take care with this, if you set this to false,
        # AuthMe will automatically disable and the server won't be protected!
        stopServer: true
    console:
        # Copy AuthMe log output in a separate file as well?
        logConsole: true
    captcha:
        # Enable captcha when a player uses wrong password too many times
        useCaptcha: false
        # Max allowed tries before a captcha is required
        maxLoginTry: 5
        # Captcha length
        captchaLength: 5
        # Minutes after which login attempts count is reset for a player
        captchaCountReset: 60
        # Require captcha before a player may register?
        requireForRegistration: false
    tempban:
        # Tempban a user's IP address if they enter the wrong password too many times
        enableTempban: false
        # How many times a user can attempt to login before their IP being tempbanned
        maxLoginTries: 10
        # The length of time a IP address will be tempbanned in minutes
        # Default: 480 minutes, or 8 hours
        tempbanLength: 480
        # How many minutes before resetting the count for failed logins by IP and username
        # Default: 480 minutes (8 hours)
        minutesBeforeCounterReset: 480
        # The command to execute instead of using the internal ban system, empty if disabled.
        # Available placeholders: %player%, %ip%
        customCommand: ''
    recoveryCode:
        # Number of characters a recovery code should have (0 to disable)
        length: 8
        # How many hours is a recovery code valid for?
        validForHours: 4
        # Max number of tries to enter recovery code
        maxTries: 3
        # How long a player has after password recovery to change their password
        # without logging in. This is in minutes.
        # Default: 2 minutes
        passwordChangeTimeout: 2
    emailRecovery:
        # Seconds a user has to wait for before a password recovery mail may be sent again
        # This prevents an attacker from abusing AuthMe's email feature.
        cooldown: 60
    privacy:
        # The mail shown using /email show will be partially hidden
        # E.g. (if enabled)
        #  original email: my.email@example.com
        #  hidden email: my.***@***mple.com
        enableEmailMasking: false
        # Minutes after which a verification code will expire
        verificationCodeExpiration: 10
# Before a user logs in, various properties are temporarily removed from the player,
# such as OP status, ability to fly, and walk/fly speed.
# Once the user is logged in, we add back the properties we previously saved.
# In this section, you may define how these properties should be handled.
# Read more at https://github.com/AuthMe/AuthMeReloaded/wiki/Limbo-players
limbo:
    persistence:
        # Besides storing the data in memory, you can define if/how the data should be persisted
        # on disk. This is useful in case of a server crash, so next time the server starts we can
        # properly restore things like OP status, ability to fly, and walk/fly speed.
        # DISABLED: no disk storage,
        # INDIVIDUAL_FILES: each player data in its own file,
        # DISTRIBUTED_FILES: distributes players into different files based on their UUID, see below
        type: 'INDIVIDUAL_FILES'
        # This setting only affects DISTRIBUTED_FILES persistence. The distributed file
        # persistence attempts to reduce the number of files by distributing players into various
        # buckets based on their UUID. This setting defines into how many files the players should
        # be distributed. Possible values: ONE, FOUR, EIGHT, SIXTEEN, THIRTY_TWO, SIXTY_FOUR,
        # ONE_TWENTY for 128, TWO_FIFTY for 256.
        # For example, if you expect 100 non-logged in players, setting to SIXTEEN will average
        # 6.25 players per file (100 / 16).
        # Note: if you change this setting all data will be migrated. If you have a lot of data,
        # change this setting only on server restart, not with /authme reload.
        distributionSize: 'SIXTEEN'
    # Whether the player is allowed to fly: RESTORE, ENABLE, DISABLE, NOTHING.
    # RESTORE sets back the old property from the player. NOTHING will prevent AuthMe
    # from modifying the 'allow flight' property on the player.
    restoreAllowFlight: 'RESTORE'
    # Restore fly speed: RESTORE, DEFAULT, MAX_RESTORE, RESTORE_NO_ZERO.
    # RESTORE: restore the speed the player had;
    # DEFAULT: always set to default speed;
    # MAX_RESTORE: take the maximum of the player's current speed and the previous one
    # RESTORE_NO_ZERO: Like 'restore' but sets speed to default if the player's speed was 0
    restoreFlySpeed: 'RESTORE_NO_ZERO'
    # Restore walk speed: RESTORE, DEFAULT, MAX_RESTORE, RESTORE_NO_ZERO.
    # See above for a description of the values.
    restoreWalkSpeed: 'RESTORE_NO_ZERO'
BackupSystem:
    # General configuration for backups: if false, no backups are possible
    ActivateBackup: false
    # Create backup at every start of server
    OnServerStart: false
    # Create backup at every stop of server
    OnServerStop: true
    # Windows only: MySQL installation path
    MysqlWindowsPath: 'C:\Program Files\MySQL\MySQL Server 5.1\'
# Converter settings: see https://github.com/AuthMe/AuthMeReloaded/wiki/Converters
Converter:
    Rakamak:
        # Rakamak file name
        fileName: 'users.rak'
        # Rakamak use IP?
        useIP: false
        # Rakamak IP file name
        ipFileName: 'UsersIp.rak'
    CrazyLogin:
        # CrazyLogin database file name
        fileName: 'accounts.db'
    loginSecurity:
        # LoginSecurity: convert from SQLite; if false we use MySQL
        useSqlite: true
        mySql:
            # LoginSecurity MySQL: database host
            host: ''
            # LoginSecurity MySQL: database name
            database: ''
            # LoginSecurity MySQL: database user
            user: ''
            # LoginSecurity MySQL: password for database user
            password: ''
```

To change settings on a running server, save your changes to config.yml and use 
`/authme reload`.

---

This page was automatically generated on the [AuthMe/AuthMeReloaded repository](https://github.com/AuthMe/AuthMeReloaded/tree/master/docs/) on Sun Apr 22 11:00:10 CEST 2018
