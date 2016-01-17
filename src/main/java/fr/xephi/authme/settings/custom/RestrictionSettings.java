package fr.xephi.authme.settings.custom;

import fr.xephi.authme.settings.domain.Comment;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.domain.PropertyType;
import fr.xephi.authme.settings.domain.SettingsClass;

import java.util.List;

import static fr.xephi.authme.settings.domain.Property.newProperty;

public class RestrictionSettings implements SettingsClass {

    @Comment({
        "Can not authenticated players chat and see the chat log?",
        "Keep in mind that this feature also blocks all commands not",
        "listed in the list below."})
    public static final Property<Boolean> ALLOW_CHAT =
        newProperty("settings.restrictions.allowChat", false);

    @Comment("Allowed commands for unauthenticated players")
    public static final Property<List<String>> ALLOW_COMMANDS =
        newProperty(PropertyType.STRING_LIST, "settings.restrictions.allowCommands",
            "login", "register", "l", "reg", "email", "captcha");

    @Comment("Max number of allowed registrations per IP")
    // TODO ljacqu 20160109: If 0 == unlimited, add this fact ot the comment
    public static final Property<Integer> MAX_REGISTRATION_PER_IP =
        newProperty("settings.restrictions.maxRegPerIp", 1);

    @Comment("Minimum allowed username length")
    public static final Property<Integer> MIN_NICKNAME_LENGTH =
        newProperty("settings.restrictions.minNicknameLength", 4);

    @Comment("Maximum allowed username length")
    public static final Property<Integer> MAX_NICKNAME_LENGTH =
        newProperty("settings.restrictions.maxNicknameLength", 16);

    @Comment({
        "When this setting is enabled, online players can't be kicked out",
        "due to \"Logged in from another Location\"",
        "This setting will prevent potential security exploits."})
    public static final Property<Boolean> FORCE_SINGLE_SESSION =
        newProperty("settings.restrictions.ForceSingleSession", true);

    @Comment({
        "If enabled, every player will be teleported to the world spawnpoint",
        "after successful authentication.",
        "The quit location of the player will be overwritten.",
        "This is different from \"teleportUnAuthedToSpawn\" that teleport player",
        "back to his quit location after the authentication."})
    public static final Property<Boolean> FORCE_SPAWN_LOCATION_AFTER_LOGIN =
        newProperty("settings.restrictions.ForceSpawnLocOnJoinEnabled", false);

    @Comment("This option will save the quit location of the players.")
    public static final Property<Boolean> SAVE_QUIT_LOCATION =
        newProperty("settings.restrictions.SaveQuitLocation", false);

    @Comment({
        "To activate the restricted user feature you need",
        "to enable this option and configure the AllowedRestrctedUser field."})
    public static final Property<Boolean> ENABLE_RESTRICTED_USERS =
        newProperty("settings.restrictions.AllowRestrictedUser", false);

    @Comment({
        "The restricted user feature will kick players listed below",
        "if they don't match the defined IP address.",
        "Example:",
        "    AllowedRestrictedUser:",
        "    - playername;127.0.0.1"})
    public static final Property<List<String>> ALLOWED_RESTRICTED_USERS =
        newProperty(PropertyType.STRING_LIST, "settings.restrictions.AllowedRestrictedUser");

    @Comment("Should unregistered players be kicked immediately?")
    public static final Property<Boolean> KICK_NON_REGISTERED =
        newProperty("settings.restrictions.kickNonRegistered", false);

    @Comment("Should players be kicked on wrong password?")
    public static final Property<Boolean> KICK_ON_WRONG_PASSWORD =
        newProperty("settings.restrictions.kickOnWrongPassword", false);

    @Comment({
        "Should not logged in players be teleported to the spawn?",
        "After the authentication they will be teleported back to",
        "their normal position."})
    public static final Property<Boolean> TELEPORT_UNAUTHED_TO_SPAWN =
        newProperty("settings.restrictions.teleportUnAuthedToSpawn", false);

    @Comment("Can unregistered players walk around?")
    public static final Property<Boolean> ALLOW_UNAUTHED_MOVEMENT =
        newProperty("settings.restrictions.allowMovement", false);

    @Comment({
        "Should not authenticated players have speed = 0?",
        "This will reset the fly/walk speed to default value after the login."})
    public static final Property<Boolean> REMOVE_SPEED =
        newProperty("settings.restrictions.removeSpeed", true);

    @Comment({
        "After how many seconds should players who fail to login or register",
        "be kicked? Set to 0 to disable."})
    public static final Property<Integer> TIMEOUT =
        newProperty("settings.restrictions.timeout", 30);

    @Comment("Regex syntax of allowed characters in the player name.")
    public static final Property<String> ALLOWED_NICKNAME_CHARACTERS =
        newProperty("settings.restrictions.allowedNicknameCharacters", "[a-zA-Z0-9_]*");

    @Comment({
        "How far can unregistered players walk?",
        "Set to 0 for unlimited radius"
    })
    public static final Property<Integer> ALLOWED_MOVEMENT_RADIUS =
        newProperty("settings.restrictions.allowedMovementRadius", 100);

    @Comment({
        "Enable double check of password when you register",
        "when it's true, registration requires that kind of command:",
        "/register <password> <confirmPassword>"})
    public static final Property<Boolean> ENABLE_PASSWORD_CONFIRMATION =
        newProperty("settings.restrictions.enablePasswordConfirmation", true);

    @Comment("Should we protect the player inventory before logging in?")
    public static final Property<Boolean> PROTECT_INVENTORY_BEFORE_LOGIN =
        newProperty("settings.restrictions.ProtectInventoryBeforeLogIn", true);

    @Comment({
        "Should we display all other accounts from a player when he joins?",
        "permission: /authme.admin.accounts"})
    public static final Property<Boolean> DISPLAY_OTHER_ACCOUNTS =
        newProperty("settings.restrictions.displayOtherAccounts", true);

    @Comment({
        "WorldNames where we need to force the spawn location for ForceSpawnLocOnJoinEnabled",
        "Case-sensitive!"})
    public static final Property<List<String>> FORCE_SPAWN_ON_WORLDS =
        newProperty(PropertyType.STRING_LIST, "settings.restrictions.ForceSpawnOnTheseWorlds",
            "world", "world_nether", "world_the_end");

    @Comment("Ban ip when the ip is not the ip registered in database")
    public static final Property<Boolean> BAN_UNKNOWN_IP =
        newProperty("settings.restrictions.banUnsafedIP", false);

    @Comment("Spawn priority; values: authme, essentials, multiverse, default")
    public static final Property<String> SPAWN_PRIORITY =
        newProperty("settings.restrictions.spawnPriority", "authme,essentials,multiverse,default");

    @Comment("Maximum Login authorized by IP")
    public static final Property<Integer> MAX_LOGIN_PER_IP =
        newProperty("settings.restrictions.maxLoginPerIp", 0);

    @Comment("Maximum Join authorized by IP")
    public static final Property<Integer> MAX_JOIN_PER_IP =
        newProperty("settings.restrictions.maxJoinPerIp", 0);

    @Comment("AuthMe will NEVER teleport players if set to true!")
    public static final Property<Boolean> NO_TELEPORT =
        newProperty("settings.restrictions.noTeleport", false);

    @Comment("Regex syntax for allowed chars in passwords")
    public static final Property<String> ALLOWED_PASSWORD_REGEX =
        newProperty("settings.restrictions.allowedPasswordCharacters", "[\\x21-\\x7E]*");

    @Comment("Force survival gamemode when player joins?")
    public static final Property<Boolean> FORCE_SURVIVAL_MODE =
        newProperty("settings.GameMode.ForceSurvivalMode", false);

    @Comment({
        "Below you can list all account names that",
        "AuthMe will ignore for registration or login, configure it",
        "at your own risk!! Remember that if you are going to add",
        "nickname with [], you have to delimit name with ' '.",
        "this option add compatibility with BuildCraft and some",
        "other mods.",
        "It is case-sensitive!"
    })
    public static final Property<List<String>> UNRESTRICTED_NAMES =
        newProperty(PropertyType.STRING_LIST, "settings.unrestrictions.UnrestrictedName");


    private RestrictionSettings() {
    }

}
