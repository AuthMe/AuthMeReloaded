package fr.xephi.authme.command;

import com.google.common.collect.ImmutableList;
import fr.xephi.authme.command.executable.HelpCommand;
import fr.xephi.authme.command.executable.authme.AccountsCommand;
import fr.xephi.authme.command.executable.authme.AuthMeCommand;
import fr.xephi.authme.command.executable.authme.ChangePasswordAdminCommand;
import fr.xephi.authme.command.executable.authme.ConverterCommand;
import fr.xephi.authme.command.executable.authme.FirstSpawnCommand;
import fr.xephi.authme.command.executable.authme.ForceLoginCommand;
import fr.xephi.authme.command.executable.authme.GetEmailCommand;
import fr.xephi.authme.command.executable.authme.GetIpCommand;
import fr.xephi.authme.command.executable.authme.LastLoginCommand;
import fr.xephi.authme.command.executable.authme.MessagesCommand;
import fr.xephi.authme.command.executable.authme.PurgeBannedPlayersCommand;
import fr.xephi.authme.command.executable.authme.PurgeCommand;
import fr.xephi.authme.command.executable.authme.PurgeLastPositionCommand;
import fr.xephi.authme.command.executable.authme.RegisterAdminCommand;
import fr.xephi.authme.command.executable.authme.ReloadCommand;
import fr.xephi.authme.command.executable.authme.SetEmailCommand;
import fr.xephi.authme.command.executable.authme.SetFirstSpawnCommand;
import fr.xephi.authme.command.executable.authme.SetSpawnCommand;
import fr.xephi.authme.command.executable.authme.SpawnCommand;
import fr.xephi.authme.command.executable.authme.SwitchAntiBotCommand;
import fr.xephi.authme.command.executable.authme.UnregisterAdminCommand;
import fr.xephi.authme.command.executable.authme.VersionCommand;
import fr.xephi.authme.command.executable.captcha.CaptchaCommand;
import fr.xephi.authme.command.executable.changepassword.ChangePasswordCommand;
import fr.xephi.authme.command.executable.email.AddEmailCommand;
import fr.xephi.authme.command.executable.email.ChangeEmailCommand;
import fr.xephi.authme.command.executable.email.EmailBaseCommand;
import fr.xephi.authme.command.executable.email.RecoverEmailCommand;
import fr.xephi.authme.command.executable.email.ShowEmailCommand;
import fr.xephi.authme.command.executable.login.LoginCommand;
import fr.xephi.authme.command.executable.logout.LogoutCommand;
import fr.xephi.authme.command.executable.register.RegisterCommand;
import fr.xephi.authme.command.executable.unregister.UnregisterCommand;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PlayerPermission;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Initializes all available AuthMe commands.
 */
public class CommandInitializer {

    private List<CommandDescription> commands;

    public CommandInitializer() {
        buildCommands();
    }

    /**
     * Returns the description of all AuthMe commands.
     *
     * @return the command descriptions
     */
    public List<CommandDescription> getCommands() {
        return commands;
    }

    private void buildCommands() {
        // Register the base AuthMe Reloaded command
        final CommandDescription AUTHME_BASE = CommandDescription.builder()
            .labels("authme")
            .description("AuthMe op commands")
            .detailedDescription("The main AuthMeReloaded command. The root for all admin commands.")
            .executableCommand(AuthMeCommand.class)
            .register();

        // Register the register command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("register", "reg", "r")
            .description("Register a player")
            .detailedDescription("Register the specified player with the specified password.")
            .withArgument("player", "Player name", false)
            .withArgument("password", "Password", false)
            .permission(AdminPermission.REGISTER)
            .executableCommand(RegisterAdminCommand.class)
            .register();

        // Register the unregister command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("unregister", "unreg", "unr")
            .description("Unregister a player")
            .detailedDescription("Unregister the specified player.")
            .withArgument("player", "Player name", false)
            .permission(AdminPermission.UNREGISTER)
            .executableCommand(UnregisterAdminCommand.class)
            .register();

        // Register the forcelogin command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("forcelogin", "login")
            .description("Enforce login player")
            .detailedDescription("Enforce the specified player to login.")
            .withArgument("player", "Online player name", true)
            .permission(AdminPermission.FORCE_LOGIN)
            .executableCommand(ForceLoginCommand.class)
            .register();

        // Register the changepassword command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("password", "changepassword", "changepass", "cp")
            .description("Change a player's password")
            .detailedDescription("Change the password of a player.")
            .withArgument("player", "Player name", false)
            .withArgument("pwd", "New password", false)
            .permission(AdminPermission.CHANGE_PASSWORD)
            .executableCommand(ChangePasswordAdminCommand.class)
            .register();

        // Register the last login command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("lastlogin", "ll")
            .description("Player's last login")
            .detailedDescription("View the date of the specified players last login.")
            .withArgument("player", "Player name", true)
            .permission(AdminPermission.LAST_LOGIN)
            .executableCommand(LastLoginCommand.class)
            .register();

        // Register the accounts command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("accounts", "account")
            .description("Display player accounts")
            .detailedDescription("Display all accounts of a player by his player name or IP.")
            .withArgument("player", "Player name or IP", true)
            .permission(AdminPermission.ACCOUNTS)
            .executableCommand(AccountsCommand.class)
            .register();

        // Register the getemail command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("email", "mail", "getemail", "getmail")
            .description("Display player's email")
            .detailedDescription("Display the email address of the specified player if set.")
            .withArgument("player", "Player name", true)
            .permission(AdminPermission.GET_EMAIL)
            .executableCommand(GetEmailCommand.class)
            .register();

        // Register the setemail command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("setemail", "setmail", "chgemail", "chgmail")
            .description("Change player's email")
            .detailedDescription("Change the email address of the specified player.")
            .withArgument("player", "Player name", false)
            .withArgument("email", "Player email", false)
            .permission(AdminPermission.CHANGE_EMAIL)
            .executableCommand(SetEmailCommand.class)
            .register();

        // Register the getip command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("getip", "ip")
            .description("Get player's IP")
            .detailedDescription("Get the IP address of the specified online player.")
            .withArgument("player", "Player name", false)
            .permission(AdminPermission.GET_IP)
            .executableCommand(GetIpCommand.class)
            .register();

        // Register the spawn command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("spawn", "home")
            .description("Teleport to spawn")
            .detailedDescription("Teleport to the spawn.")
            .permission(AdminPermission.SPAWN)
            .executableCommand(SpawnCommand.class)
            .register();

        // Register the setspawn command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("setspawn", "chgspawn")
            .description("Change the spawn")
            .detailedDescription("Change the player's spawn to your current position.")
            .permission(AdminPermission.SET_SPAWN)
            .executableCommand(SetSpawnCommand.class)
            .register();

        // Register the firstspawn command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("firstspawn", "firsthome")
            .description("Teleport to first spawn")
            .detailedDescription("Teleport to the first spawn.")
            .permission(AdminPermission.FIRST_SPAWN)
            .executableCommand(FirstSpawnCommand.class)
            .register();

        // Register the setfirstspawn command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("setfirstspawn", "chgfirstspawn")
            .description("Change the first spawn")
            .detailedDescription("Change the first player's spawn to your current position.")
            .permission(AdminPermission.SET_FIRST_SPAWN)
            .executableCommand(SetFirstSpawnCommand.class)
            .register();

        // Register the purge command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("purge", "delete")
            .description("Purge old data")
            .detailedDescription("Purge old AuthMeReloaded data longer than the specified amount of days ago.")
            .withArgument("days", "Number of days", false)
            .withArgument("all", "Add 'all' at the end to also purge players with lastlogin = 0", true)
            .permission(AdminPermission.PURGE)
            .executableCommand(PurgeCommand.class)
            .register();

        // Register the purgelastposition command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("resetpos", "purgelastposition", "purgelastpos", "resetposition",
                "resetlastposition", "resetlastpos")
            .description("Purge player's last position")
            .detailedDescription("Purge the last know position of the specified player or all of them.")
            .withArgument("player/*", "Player name or * for all players", false)
            .permission(AdminPermission.PURGE_LAST_POSITION)
            .executableCommand(PurgeLastPositionCommand.class)
            .register();

        // Register the purgebannedplayers command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("purgebannedplayers", "purgebannedplayer", "deletebannedplayers", "deletebannedplayer")
            .description("Purge banned players data")
            .detailedDescription("Purge all AuthMeReloaded data for banned players.")
            .permission(AdminPermission.PURGE_BANNED_PLAYERS)
            .executableCommand(PurgeBannedPlayersCommand.class)
            .register();

        // Register the switchantibot command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("switchantibot", "toggleantibot", "antibot")
            .description("Switch AntiBot mode")
            .detailedDescription("Switch or toggle the AntiBot mode to the specified state.")
            .withArgument("mode", "ON / OFF", true)
            .permission(AdminPermission.SWITCH_ANTIBOT)
            .executableCommand(SwitchAntiBotCommand.class)
            .register();

        // Register the reload command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("reload", "rld")
            .description("Reload plugin")
            .detailedDescription("Reload the AuthMeReloaded plugin.")
            .permission(AdminPermission.RELOAD)
            .executableCommand(ReloadCommand.class)
            .register();

        // Register the version command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("version", "ver", "v", "about", "info")
            .description("Version info")
            .detailedDescription("Show detailed information about the installed AuthMeReloaded version, the "
                + "developers, contributors, and license.")
            .executableCommand(VersionCommand.class)
            .register();

        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("converter", "convert", "conv")
            .description("Converter command")
            .detailedDescription("Converter command for AuthMeReloaded.")
            .withArgument("job", "Conversion job: xauth / crazylogin / rakamak / " +
                "royalauth / vauth / sqliteToSql / mysqlToSqlite", false)
            .permission(AdminPermission.CONVERTER)
            .executableCommand(ConverterCommand.class)
            .register();

        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("messages", "msg")
            .description("Add missing messages")
            .detailedDescription("Adds missing messages to the current messages file.")
            .permission(AdminPermission.UPDATE_MESSAGES)
            .executableCommand(MessagesCommand.class)
            .register();

        // Register the base login command
        final CommandDescription LOGIN_BASE = CommandDescription.builder()
            .parent(null)
            .labels("login", "l", "log")
            .description("Login command")
            .detailedDescription("Command to log in using AuthMeReloaded.")
            .withArgument("password", "Login password", false)
            .permission(PlayerPermission.LOGIN)
            .executableCommand(LoginCommand.class)
            .register();

        // Register the base logout command
        CommandDescription LOGOUT_BASE = CommandDescription.builder()
            .parent(null)
            .labels("logout")
            .description("Logout command")
            .detailedDescription("Command to logout using AuthMeReloaded.")
            .permission(PlayerPermission.LOGOUT)
            .executableCommand(LogoutCommand.class)
            .register();

        // Register the base register command
        final CommandDescription REGISTER_BASE = CommandDescription.builder()
            .parent(null)
            .labels("register", "reg")
            .description("Register an account")
            .detailedDescription("Command to register using AuthMeReloaded.")
            .withArgument("password", "Password", true)
            .withArgument("verifyPassword", "Verify password", true)
            .permission(PlayerPermission.REGISTER)
            .executableCommand(RegisterCommand.class)
            .register();

        // Register the base unregister command
        CommandDescription UNREGISTER_BASE = CommandDescription.builder()
            .parent(null)
            .labels("unregister", "unreg")
            .description("Unregister an account")
            .detailedDescription("Command to unregister using AuthMeReloaded.")
            .withArgument("password", "Password", false)
            .permission(PlayerPermission.UNREGISTER)
            .executableCommand(UnregisterCommand.class)
            .register();

        // Register the base changepassword command
        final CommandDescription CHANGE_PASSWORD_BASE = CommandDescription.builder()
            .parent(null)
            .labels("changepassword", "changepass", "cp")
            .description("Change password of an account")
            .detailedDescription("Command to change your password using AuthMeReloaded.")
            .withArgument("oldPassword", "Old password", false)
            .withArgument("newPassword", "New password", false)
            .permission(PlayerPermission.CHANGE_PASSWORD)
            .executableCommand(ChangePasswordCommand.class)
            .register();

        // Register the base Email command
        CommandDescription EMAIL_BASE = CommandDescription.builder()
            .parent(null)
            .labels("email")
            .description("Add email or recover password")
            .detailedDescription("The AuthMeReloaded email command base.")
            .executableCommand(EmailBaseCommand.class)
            .register();

        // Register the show command
        CommandDescription.builder()
            .parent(EMAIL_BASE)
            .labels("show", "myemail")
            .description("Show Email")
            .detailedDescription("Show your current email address.")
            .executableCommand(ShowEmailCommand.class)
            .register();

        // Register the add command
        CommandDescription.builder()
            .parent(EMAIL_BASE)
            .labels("add", "addemail", "addmail")
            .description("Add Email")
            .detailedDescription("Add a new email address to your account.")
            .withArgument("email", "Email address", false)
            .withArgument("verifyEmail", "Email address verification", false)
            .permission(PlayerPermission.ADD_EMAIL)
            .executableCommand(AddEmailCommand.class)
            .register();

        // Register the change command
        CommandDescription.builder()
            .parent(EMAIL_BASE)
            .labels("change", "changeemail", "changemail")
            .description("Change Email")
            .detailedDescription("Change an email address of your account.")
            .withArgument("oldEmail", "Old email address", false)
            .withArgument("newEmail", "New email address", false)
            .permission(PlayerPermission.CHANGE_EMAIL)
            .executableCommand(ChangeEmailCommand.class)
            .register();

        // Register the recover command
        CommandDescription.builder()
            .parent(EMAIL_BASE)
            .labels("recover", "recovery", "recoveremail", "recovermail")
            .description("Recover password using email")
            .detailedDescription("Recover your account using an Email address by sending a mail containing " +
                "a new password.")
            .withArgument("email", "Email address", false)
            .withArgument("code", "Recovery code", true)
            .permission(PlayerPermission.RECOVER_EMAIL)
            .executableCommand(RecoverEmailCommand.class)
            .register();

        // Register the base captcha command
        CommandDescription CAPTCHA_BASE = CommandDescription.builder()
            .parent(null)
            .labels("captcha")
            .description("Captcha Command")
            .detailedDescription("Captcha command for AuthMeReloaded.")
            .withArgument("captcha", "The Captcha", false)
            .permission(PlayerPermission.CAPTCHA)
            .executableCommand(CaptchaCommand.class)
            .register();

        List<CommandDescription> baseCommands = ImmutableList.of(
            AUTHME_BASE,
            LOGIN_BASE,
            LOGOUT_BASE,
            REGISTER_BASE,
            UNREGISTER_BASE,
            CHANGE_PASSWORD_BASE,
            EMAIL_BASE,
            CAPTCHA_BASE);

        setHelpOnAllBases(baseCommands);
        commands = baseCommands;
    }

    /**
     * Sets the help command on all base commands, e.g. to register /authme help or /register help.
     *
     * @param commands the list of base commands to register a help child command on
     */
    private void setHelpOnAllBases(Collection<CommandDescription> commands) {
        final List<String> helpCommandLabels = Arrays.asList("help", "hlp", "h", "sos", "?");

        for (CommandDescription base : commands) {
            CommandDescription.builder()
                .parent(base)
                .labels(helpCommandLabels)
                .description("View help")
                .detailedDescription("View detailed help for /" + base.getLabels().get(0) + " commands.")
                .withArgument("query", "The command or query to view help for.", true)
                .executableCommand(HelpCommand.class)
                .register();
        }
    }
}
