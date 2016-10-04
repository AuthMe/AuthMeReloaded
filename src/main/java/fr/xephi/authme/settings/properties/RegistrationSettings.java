package fr.xephi.authme.settings.properties;

import com.github.authme.configme.Comment;
import com.github.authme.configme.SettingsHolder;
import com.github.authme.configme.properties.Property;

import java.util.List;

import static com.github.authme.configme.properties.PropertyInitializer.newListProperty;
import static com.github.authme.configme.properties.PropertyInitializer.newProperty;

public class RegistrationSettings implements SettingsHolder {

    @Comment("Enable registration on the server?")
    public static final Property<Boolean> IS_ENABLED =
        newProperty("settings.registration.enabled", true);

    @Comment({
        "Send every X seconds a message to a player to",
        "remind him that he has to login/register"})
    public static final Property<Integer> MESSAGE_INTERVAL =
        newProperty("settings.registration.messageInterval", 5);

    @Comment({
        "Only registered and logged in players can play.",
        "See restrictions for exceptions"})
    public static final Property<Boolean> FORCE =
        newProperty("settings.registration.force", true);

    @Comment("Do we replace password registration by an email registration method?")
    public static final Property<Boolean> USE_EMAIL_REGISTRATION =
        newProperty("settings.registration.enableEmailRegistrationSystem", false);

    @Comment({
        "Enable double check of email when you register",
        "when it's true, registration requires that kind of command:",
        "/register <email> <confirmEmail>"})
    public static final Property<Boolean> ENABLE_CONFIRM_EMAIL =
        newProperty("settings.registration.doubleEmailCheck", false);

    @Comment({
        "Do we force kick a player after a successful registration?",
        "Do not use with login feature below"})
    public static final Property<Boolean> FORCE_KICK_AFTER_REGISTER =
        newProperty("settings.registration.forceKickAfterRegister", false);

    @Comment("Does AuthMe need to enforce a /login after a successful registration?")
    public static final Property<Boolean> FORCE_LOGIN_AFTER_REGISTER =
        newProperty("settings.registration.forceLoginAfterRegister", false);

    @Comment("Force these commands after /login, without any '/', use %p to replace with player name")
    public static final Property<List<String>> FORCE_COMMANDS =
        newListProperty("settings.forceCommands");

    @Comment("Force these commands after /login as service console, without any '/'. "
        + "Use %p to replace with player name")
    public static final Property<List<String>> FORCE_COMMANDS_AS_CONSOLE =
        newListProperty("settings.forceCommandsAsConsole");

    @Comment("Force these commands after /register, without any '/', use %p to replace with player name")
    public static final Property<List<String>> FORCE_REGISTER_COMMANDS =
        newListProperty("settings.forceRegisterCommands");

    @Comment("Force these commands after /register as a server console, without any '/'. "
        + "Use %p to replace with player name")
    public static final Property<List<String>> FORCE_REGISTER_COMMANDS_AS_CONSOLE =
        newListProperty("settings.forceRegisterCommandsAsConsole");

    @Comment({
        "Enable to display the welcome message (welcome.txt) after a login",
        "You can use colors in this welcome.txt + some replaced strings:",
        "{PLAYER}: player name, {ONLINE}: display number of online players, {MAXPLAYERS}: display server slots,",
        "{IP}: player ip, {LOGINS}: number of players logged, {WORLD}: player current world, {SERVER}: server name",
        "{VERSION}: get current bukkit version, {COUNTRY}: player country"})
    public static final Property<Boolean> USE_WELCOME_MESSAGE =
        newProperty("settings.useWelcomeMessage", true);

    @Comment("Do we need to broadcast the welcome message to all server or only to the player? set true for "
        + "server or false for player")
    public static final Property<Boolean> BROADCAST_WELCOME_MESSAGE =
        newProperty("settings.broadcastWelcomeMessage", false);

    @Comment("Should we delay the join message and display it once the player has logged in?")
    public static final Property<Boolean> DELAY_JOIN_MESSAGE =
        newProperty("settings.delayJoinMessage", false);

    @Comment("Should we remove the leave messages of unlogged users?")
    public static final Property<Boolean> REMOVE_UNLOGGED_LEAVE_MESSAGE =
        newProperty("settings.removeUnloggedLeaveMessage", false);

    @Comment("Should we remove join messages altogether?")
    public static final Property<Boolean> REMOVE_JOIN_MESSAGE =
        newProperty("settings.removeJoinMessage", false);

    @Comment("Should we remove leave messages altogether?")
    public static final Property<Boolean> REMOVE_LEAVE_MESSAGE =
        newProperty("settings.removeLeaveMessage", false);

    @Comment("Do we need to add potion effect Blinding before login/reigster?")
    public static final Property<Boolean> APPLY_BLIND_EFFECT =
        newProperty("settings.applyBlindEffect", false);

    @Comment({
        "Do we need to prevent people to login with another case?",
        "If Xephi is registered, then Xephi can login, but not XEPHI/xephi/XePhI"})
    public static final Property<Boolean> PREVENT_OTHER_CASE =
        newProperty("settings.preventOtherCase", false);


    private RegistrationSettings() {
    }

}
