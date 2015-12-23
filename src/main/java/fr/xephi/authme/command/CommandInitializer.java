package fr.xephi.authme.command;

import com.google.common.collect.ImmutableSet;
import fr.xephi.authme.command.executable.HelpCommand;
import fr.xephi.authme.command.executable.authme.AccountsCommand;
import fr.xephi.authme.command.executable.authme.AuthMeCommand;
import fr.xephi.authme.command.executable.authme.ChangePasswordAdminCommand;
import fr.xephi.authme.command.executable.authme.FirstSpawnCommand;
import fr.xephi.authme.command.executable.authme.ForceLoginCommand;
import fr.xephi.authme.command.executable.authme.GetEmailCommand;
import fr.xephi.authme.command.executable.authme.GetIpCommand;
import fr.xephi.authme.command.executable.authme.LastLoginCommand;
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
import fr.xephi.authme.command.executable.converter.ConverterCommand;
import fr.xephi.authme.command.executable.email.AddEmailCommand;
import fr.xephi.authme.command.executable.email.ChangeEmailCommand;
import fr.xephi.authme.command.executable.email.RecoverEmailCommand;
import fr.xephi.authme.command.executable.login.LoginCommand;
import fr.xephi.authme.command.executable.logout.LogoutCommand;
import fr.xephi.authme.command.executable.register.RegisterCommand;
import fr.xephi.authme.command.executable.unregister.UnregisterCommand;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PlayerPermission;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static fr.xephi.authme.permission.DefaultPermission.ALLOWED;
import static fr.xephi.authme.permission.DefaultPermission.OP_ONLY;

/**
 * Initializes all available AuthMe commands.
 */
public final class CommandInitializer {

    private static Set<CommandDescription> baseCommands;

    private CommandInitializer() {
        // Helper class
    }

    public static Set<CommandDescription> getBaseCommands() {
        if (baseCommands == null) {
            initializeCommands();
        }
        return baseCommands;
    }

    private static void initializeCommands() {
        // Create a list of help command labels
        final List<String> helpCommandLabels = Arrays.asList("help", "hlp", "h", "sos", "?");
        final ExecutableCommand helpCommandExecutable = new HelpCommand();

        // Register the base AuthMe Reloaded command
        final CommandDescription AUTHME_BASE = CommandDescription.builder()
            .labels("authme")
            .description("Main command")
            .detailedDescription("The main AuthMeReloaded command. The root for all admin commands.")
            .executableCommand(new AuthMeCommand())
            .build();

        // Register the help command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels(helpCommandLabels)
            .description("View help")
            .detailedDescription("View detailed help pages about AuthMeReloaded commands.")
            .withArgument("query", "The command or query to view help for.", true)
            .executableCommand(helpCommandExecutable)
            .build();

        // Register the register command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("register", "reg", "r")
            .description("Register a player")
            .detailedDescription("Register the specified player with the specified password.")
            .withArgument("player", "Player name", false)
            .withArgument("password", "Password", false)
            .permissions(OP_ONLY, AdminPermission.REGISTER)
            .executableCommand(new RegisterAdminCommand())
            .build();

        // Register the unregister command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("unregister", "unreg", "unr")
            .description("Unregister a player")
            .detailedDescription("Unregister the specified player.")
            .withArgument("player", "Player name", false)
            .permissions(OP_ONLY, AdminPermission.UNREGISTER)
            .executableCommand(new UnregisterAdminCommand())
            .build();

        // Register the forcelogin command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("forcelogin", "login")
            .description("Enforce login player")
            .detailedDescription("Enforce the specified player to login.")
            .withArgument("player", "Online player name", true)
            .permissions(OP_ONLY, PlayerPermission.CAN_LOGIN_BE_FORCED)
            .executableCommand(new ForceLoginCommand())
            .build();

        // Register the changepassword command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("password", "changepassword", "changepass", "cp")
            .description("Change a player's password")
            .detailedDescription("Change the password of a player.")
            .withArgument("player", "Player name", false)
            .withArgument("pwd", "New password", false)
            .permissions(OP_ONLY, AdminPermission.CHANGE_PASSWORD)
            .executableCommand(new ChangePasswordAdminCommand())
            .build();

        // Register the last login command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("lastlogin", "ll")
            .description("Player's last login")
            .detailedDescription("View the date of the specified players last login.")
            .withArgument("player", "Player name", true)
            .permissions(OP_ONLY, AdminPermission.LAST_LOGIN)
            .executableCommand(new LastLoginCommand())
            .build();

        // Register the accounts command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("accounts", "account")
            .description("Display player accounts")
            .detailedDescription("Display all accounts of a player by his player name or IP.")
            .withArgument("player", "Player name or IP", true)
            .permissions(OP_ONLY, AdminPermission.ACCOUNTS)
            .executableCommand(new AccountsCommand())
            .build();

        // Register the getemail command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("getemail", "getmail", "email", "mail")
            .description("Display player's email")
            .detailedDescription("Display the email address of the specified player if set.")
            .withArgument("player", "Player name", true)
            .permissions(OP_ONLY, AdminPermission.GET_EMAIL)
            .executableCommand(new GetEmailCommand())
            .build();

        // Register the setemail command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("chgemail", "chgmail", "setemail", "setmail")
            .description("Change player's email")
            .detailedDescription("Change the email address of the specified player.")
            .withArgument("player", "Player name", false)
            .withArgument("email", "Player email", false)
            .permissions(OP_ONLY, AdminPermission.CHANGE_EMAIL)
            .executableCommand(new SetEmailCommand())
            .build();

        // Register the getip command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("getip", "ip")
            .description("Get player's IP")
            .detailedDescription("Get the IP address of the specified online player.")
            .withArgument("player", "Player Name", false)
            .permissions(OP_ONLY, AdminPermission.GET_IP)
            .executableCommand(new GetIpCommand())
            .build();

        // Register the spawn command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("spawn", "home")
            .description("Teleport to spawn")
            .detailedDescription("Teleport to the spawn.")
            .withArgument("player", "Player Name", false)
            .permissions(OP_ONLY, AdminPermission.SPAWN)
            .executableCommand(new SpawnCommand())
            .build();

        // Register the setspawn command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("setspawn", "chgspawn")
            .description("Change the spawn")
            .detailedDescription("Change the player's spawn to your current position.")
            .permissions(OP_ONLY, AdminPermission.SET_SPAWN)
            .executableCommand(new SetSpawnCommand())
            .build();

        // Register the firstspawn command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("firstspawn", "firsthome")
            .description("Teleport to first spawn")
            .detailedDescription("Teleport to the first spawn.")
            .permissions(OP_ONLY, AdminPermission.FIRST_SPAWN)
            .executableCommand(new FirstSpawnCommand())
            .build();

        // Register the setfirstspawn command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("setfirstspawn", "chgfirstspawn")
            .description("Change the first spawn")
            .detailedDescription("Change the first player's spawn to your current position.")
            .permissions(OP_ONLY, AdminPermission.SET_FIRST_SPAWN)
            .executableCommand(new SetFirstSpawnCommand())
            .build();

        // Register the purge command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("purge", "delete")
            .description("Purge old data")
            .detailedDescription("Purge old AuthMeReloaded data longer than the specified amount of days ago.")
            .withArgument("days", "Number of days", false)
            .permissions(OP_ONLY, AdminPermission.PURGE)
            .executableCommand(new PurgeCommand())
            .build();

        // Register the purgelastposition command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("resetpos", "purgelastposition", "purgelastpos", "resetposition",
                "resetlastposition", "resetlastpos")
            .description("Purge player's last position")
            .detailedDescription("Purge the last know position of the specified player.")
            .withArgument("player", "Player name", false)
            .permissions(OP_ONLY, AdminPermission.PURGE_LAST_POSITION)
            .executableCommand(new PurgeLastPositionCommand())
            .build();

        // Register the purgebannedplayers command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("purgebannedplayers", "purgebannedplayer", "deletebannedplayers", "deletebannedplayer")
            .description("Purge banned players data")
            .detailedDescription("Purge all AuthMeReloaded data for banned players.")
            .permissions(OP_ONLY, AdminPermission.PURGE_BANNED_PLAYERS)
            .executableCommand(new PurgeBannedPlayersCommand())
            .build();

        // Register the switchantibot command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("switchantibot", "toggleantibot", "antibot")
            .description("Switch AntiBot mode")
            .detailedDescription("Switch or toggle the AntiBot mode to the specified state.")
            .withArgument("mode", "ON / OFF", true)
            .permissions(OP_ONLY, AdminPermission.SWITCH_ANTIBOT)
            .executableCommand(new SwitchAntiBotCommand())
            .build();

        // Register the reload command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("reload", "rld")
            .description("Reload plugin")
            .detailedDescription("Reload the AuthMeReloaded plugin.")
            .permissions(OP_ONLY, AdminPermission.RELOAD)
            .executableCommand(new ReloadCommand())
            .build();

        // Register the version command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("version", "ver", "v", "about", "info")
            .description("Version info")
            .detailedDescription("Show detailed information about the installed AuthMeReloaded version, the "
                + "developers, contributors, and license.")
            .executableCommand(new VersionCommand())
            .build();

        // Register the base login command
        final CommandDescription LOGIN_BASE = CommandDescription.builder()
            .parent(null)
            .labels("login", "l")
            .description("Login command")
            .detailedDescription("Command to log in using AuthMeReloaded.")
            .withArgument("password", "Login password", false)
            .permissions(ALLOWED, PlayerPermission.LOGIN)
            .executableCommand(new LoginCommand())
            .build();

        // Register the help command
        CommandDescription.builder()
            .parent(LOGIN_BASE)
            .labels(helpCommandLabels)
            .description("View Help")
            .detailedDescription("View detailed help pages about AuthMeReloaded login commands.")
            .withArgument("query", "The command or query to view help for.", true)
            .executableCommand(helpCommandExecutable)
            .build();

        // Register the base logout command
        CommandDescription LOGOUT_BASE = CommandDescription.builder()
            .parent(null)
            .labels("logout")
            .description("Logout command")
            .detailedDescription("Command to logout using AuthMeReloaded.")
            .permissions(ALLOWED, PlayerPermission.LOGOUT)
            .executableCommand(new LogoutCommand())
            .build();

        // Register the help command
        CommandDescription.builder()
            .parent(LOGOUT_BASE)
            .labels(helpCommandLabels)
            .description("View help")
            .detailedDescription("View detailed help pages about AuthMeReloaded logout commands.")
            .withArgument("query", "The command or query to view help for.", true)
            .executableCommand(helpCommandExecutable)
            .build();

        // Register the base register command
        final CommandDescription REGISTER_BASE = CommandDescription.builder()
            .parent(null)
            .labels("register", "reg")
            .description("Registration command")
            .detailedDescription("Command to register using AuthMeReloaded.")
            .withArgument("password", "Password", false)
            .withArgument("verifyPassword", "Verify password", true)
            .permissions(ALLOWED, PlayerPermission.REGISTER)
            .executableCommand(new RegisterCommand())
            .build();

        // Register the help command
        CommandDescription.builder()
            .parent(REGISTER_BASE)
            .labels(helpCommandLabels)
            .description("View help")
            .detailedDescription("View detailed help pages about AuthMeReloaded register commands.")
            .withArgument("query", "The command or query to view help for.", true)
            .executableCommand(helpCommandExecutable)
            .build();

        // Register the base unregister command
        CommandDescription UNREGISTER_BASE = CommandDescription.builder()
            .parent(null)
            .labels("unreg", "unregister")
            .description("Unregistration Command")
            .detailedDescription("Command to unregister using AuthMeReloaded.")
            .withArgument("password", "Password", false)
            .permissions(ALLOWED, PlayerPermission.UNREGISTER)
            .executableCommand(new UnregisterCommand())
            .build();

        // Register the help command
        CommandDescription.builder()
            .parent(UNREGISTER_BASE)
            .labels(helpCommandLabels)
            .description("View help")
            .detailedDescription("View detailed help pages about AuthMeReloaded unregister commands.")
            .withArgument("query", "The command or query to view help for.", true)
            .executableCommand(helpCommandExecutable)
            .build();

        // Register the base changepassword command
        final CommandDescription CHANGE_PASSWORD_BASE = CommandDescription.builder()
            .parent(null)
            .labels("changepassword", "changepass", "cp")
            .description("Change password Command")
            .detailedDescription("Command to change your password using AuthMeReloaded.")
            .withArgument("oldPassword", "Old Password", false)
            .withArgument("newPassword", "New Password.", false)
            .permissions(ALLOWED, PlayerPermission.CHANGE_PASSWORD)
            .executableCommand(new ChangePasswordCommand())
            .build();

        // Register the help command
        CommandDescription.builder()
            .parent(CHANGE_PASSWORD_BASE)
            .labels(helpCommandLabels)
            .description("View help")
            .detailedDescription("View detailed help pages about AuthMeReloaded changepassword commands.")
            .withArgument("query", "The command or query to view help for.", true)
            .executableCommand(helpCommandExecutable)
            .build();

        // Register the base Email command
        CommandDescription EMAIL_BASE = CommandDescription.builder()
            .parent(null)
            .labels("email", "mail")
            .description("Email command")
            .detailedDescription("The AuthMeReloaded Email command base.")
            .executableCommand(helpCommandExecutable)
            .build();

        // Register the help command
        CommandDescription.builder()
            .parent(EMAIL_BASE)
            .labels(helpCommandLabels)
            .description("View help")
            .detailedDescription("View detailed help pages about AuthMeReloaded email commands.")
            .withArgument("query", "The command or query to view help for.", true)
            .executableCommand(helpCommandExecutable)
            .build();

        // Register the add command
        CommandDescription.builder()
            .parent(EMAIL_BASE)
            .labels("add", "addemail", "addmail")
            .description("Add Email")
            .detailedDescription("Add a new email address to your account.")
            .withArgument("email", "Email address", false)
            .withArgument("verifyEmail", "Email address verification", false)
            .permissions(ALLOWED, PlayerPermission.ADD_EMAIL)
            .executableCommand(new AddEmailCommand())
            .build();

        // Register the change command
        CommandDescription.builder()
            .parent(EMAIL_BASE)
            .labels("change", "changeemail", "changemail")
            .description("Change Email")
            .detailedDescription("Change an email address of your account.")
            .withArgument("oldEmail", "Old email address", false)
            .withArgument("newEmail", "New email address", false)
            .permissions(ALLOWED, PlayerPermission.CHANGE_EMAIL)
            .executableCommand(new ChangeEmailCommand())
            .build();

        // Register the recover command
        CommandDescription.builder()
            .parent(EMAIL_BASE)
            .labels("recover", "recovery", "recoveremail", "recovermail")
            .description("Recover password using Email")
            .detailedDescription("Recover your account using an Email address by sending a mail containing " +
                "a new password.")
            .withArgument("email", "Email address", false)
            .permissions(ALLOWED, PlayerPermission.RECOVER_EMAIL)
            .executableCommand(new RecoverEmailCommand())
            .build();

        // Register the base captcha command
        CommandDescription CAPTCHA_BASE = CommandDescription.builder()
            .parent(null)
            .labels("captcha", "capt")
            .description("Captcha Command")
            .detailedDescription("Captcha command for AuthMeReloaded.")
            .withArgument("captcha", "The Captcha", false)
            .permissions(ALLOWED, PlayerPermission.CAPTCHA)
            .executableCommand(new CaptchaCommand())
            .build();

        // Register the help command
        CommandDescription.builder()
            .parent(CAPTCHA_BASE)
            .labels(helpCommandLabels)
            .description("View help")
            .detailedDescription("View detailed help pages about AuthMeReloaded captcha commands.")
            .withArgument("query", "The command or query to view help for.", true)
            .executableCommand(helpCommandExecutable)
            .build();

        // Register the base converter command
        CommandDescription CONVERTER_BASE = CommandDescription.builder()
            .parent(null)
            .labels("converter", "convert", "conv")
            .description("Converter Command")
            .detailedDescription("Converter command for AuthMeReloaded.")
            .withArgument("job", "Conversion job: flattosql / flattosqlite /| xauth / crazylogin / rakamak / " +
                "royalauth / vauth / sqltoflat / sqlitetosql", false)
            .permissions(OP_ONLY, AdminPermission.CONVERTER)
            .executableCommand(new ConverterCommand())
            .build();

        // Register the help command
        CommandDescription.builder()
            .parent(CONVERTER_BASE)
            .labels(helpCommandLabels)
            .description("View help")
            .detailedDescription("View detailed help pages about AuthMeReloaded converter commands.")
            .withArgument("query", "The command or query to view help for.", true)
            .executableCommand(helpCommandExecutable)
            .build();

        // Add the base commands to the commands array
        baseCommands = ImmutableSet.of(
            AUTHME_BASE,
            LOGIN_BASE,
            LOGOUT_BASE,
            REGISTER_BASE,
            UNREGISTER_BASE,
            CHANGE_PASSWORD_BASE,
            EMAIL_BASE,
            CAPTCHA_BASE,
            CONVERTER_BASE);
    }
}
