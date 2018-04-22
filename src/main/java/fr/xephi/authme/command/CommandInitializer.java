package fr.xephi.authme.command;

import com.google.common.collect.ImmutableList;
import fr.xephi.authme.command.executable.HelpCommand;
import fr.xephi.authme.command.executable.authme.AccountsCommand;
import fr.xephi.authme.command.executable.authme.AuthMeCommand;
import fr.xephi.authme.command.executable.authme.BackupCommand;
import fr.xephi.authme.command.executable.authme.ChangePasswordAdminCommand;
import fr.xephi.authme.command.executable.authme.ConverterCommand;
import fr.xephi.authme.command.executable.authme.FirstSpawnCommand;
import fr.xephi.authme.command.executable.authme.ForceLoginCommand;
import fr.xephi.authme.command.executable.authme.GetEmailCommand;
import fr.xephi.authme.command.executable.authme.GetIpCommand;
import fr.xephi.authme.command.executable.authme.LastLoginCommand;
import fr.xephi.authme.command.executable.authme.PurgeBannedPlayersCommand;
import fr.xephi.authme.command.executable.authme.PurgeCommand;
import fr.xephi.authme.command.executable.authme.PurgeLastPositionCommand;
import fr.xephi.authme.command.executable.authme.PurgePlayerCommand;
import fr.xephi.authme.command.executable.authme.RecentPlayersCommand;
import fr.xephi.authme.command.executable.authme.RegisterAdminCommand;
import fr.xephi.authme.command.executable.authme.ReloadCommand;
import fr.xephi.authme.command.executable.authme.SetEmailCommand;
import fr.xephi.authme.command.executable.authme.SetFirstSpawnCommand;
import fr.xephi.authme.command.executable.authme.SetSpawnCommand;
import fr.xephi.authme.command.executable.authme.SpawnCommand;
import fr.xephi.authme.command.executable.authme.SwitchAntiBotCommand;
import fr.xephi.authme.command.executable.authme.UnregisterAdminCommand;
import fr.xephi.authme.command.executable.authme.UpdateHelpMessagesCommand;
import fr.xephi.authme.command.executable.authme.VersionCommand;
import fr.xephi.authme.command.executable.authme.debug.DebugCommand;
import fr.xephi.authme.command.executable.captcha.CaptchaCommand;
import fr.xephi.authme.command.executable.changepassword.ChangePasswordCommand;
import fr.xephi.authme.command.executable.email.AddEmailCommand;
import fr.xephi.authme.command.executable.email.ChangeEmailCommand;
import fr.xephi.authme.command.executable.email.EmailBaseCommand;
import fr.xephi.authme.command.executable.email.ProcessCodeCommand;
import fr.xephi.authme.command.executable.email.RecoverEmailCommand;
import fr.xephi.authme.command.executable.email.SetPasswordCommand;
import fr.xephi.authme.command.executable.email.ShowEmailCommand;
import fr.xephi.authme.command.executable.login.LoginCommand;
import fr.xephi.authme.command.executable.logout.LogoutCommand;
import fr.xephi.authme.command.executable.register.RegisterCommand;
import fr.xephi.authme.command.executable.totp.AddTotpCommand;
import fr.xephi.authme.command.executable.totp.ConfirmTotpCommand;
import fr.xephi.authme.command.executable.totp.RemoveTotpCommand;
import fr.xephi.authme.command.executable.totp.TotpBaseCommand;
import fr.xephi.authme.command.executable.totp.TotpCodeCommand;
import fr.xephi.authme.command.executable.unregister.UnregisterCommand;
import fr.xephi.authme.command.executable.verification.VerificationCommand;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.DebugSectionPermissions;
import fr.xephi.authme.permission.PlayerPermission;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Initializes all available AuthMe commands.
 */
public class CommandInitializer {

    private static final boolean OPTIONAL = true;
    private static final boolean MANDATORY = false;

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

    /**
     * Builds the command description objects for all available AuthMe commands.
     */
    private void buildCommands() {
        // Register /authme and /email commands
        CommandDescription authMeBase = buildAuthMeBaseCommand();
        CommandDescription emailBase = buildEmailBaseCommand();

        // Register the base login command
        CommandDescription loginBase = CommandDescription.builder()
            .parent(null)
            .labels("login", "l", "log")
            .description("Login command")
            .detailedDescription("Command to log in using AuthMeReloaded.")
            .withArgument("password", "Login password", MANDATORY)
            .permission(PlayerPermission.LOGIN)
            .executableCommand(LoginCommand.class)
            .register();

        // Register the base logout command
        CommandDescription logoutBase = CommandDescription.builder()
            .parent(null)
            .labels("logout")
            .description("Logout command")
            .detailedDescription("Command to logout using AuthMeReloaded.")
            .permission(PlayerPermission.LOGOUT)
            .executableCommand(LogoutCommand.class)
            .register();

        // Register the base register command
        CommandDescription registerBase = CommandDescription.builder()
            .parent(null)
            .labels("register", "reg")
            .description("Register an account")
            .detailedDescription("Command to register using AuthMeReloaded.")
            .withArgument("password", "Password", OPTIONAL)
            .withArgument("verifyPassword", "Verify password", OPTIONAL)
            .permission(PlayerPermission.REGISTER)
            .executableCommand(RegisterCommand.class)
            .register();

        // Register the base unregister command
        CommandDescription unregisterBase = CommandDescription.builder()
            .parent(null)
            .labels("unregister", "unreg")
            .description("Unregister an account")
            .detailedDescription("Command to unregister using AuthMeReloaded.")
            .withArgument("password", "Password", MANDATORY)
            .permission(PlayerPermission.UNREGISTER)
            .executableCommand(UnregisterCommand.class)
            .register();

        // Register the base changepassword command
        CommandDescription changePasswordBase = CommandDescription.builder()
            .parent(null)
            .labels("changepassword", "changepass", "cp")
            .description("Change password of an account")
            .detailedDescription("Command to change your password using AuthMeReloaded.")
            .withArgument("oldPassword", "Old password", MANDATORY)
            .withArgument("newPassword", "New password", MANDATORY)
            .permission(PlayerPermission.CHANGE_PASSWORD)
            .executableCommand(ChangePasswordCommand.class)
            .register();

        // Create totp base command
        CommandDescription totpBase = buildTotpBaseCommand();

        // Register the base captcha command
        CommandDescription captchaBase = CommandDescription.builder()
            .parent(null)
            .labels("captcha")
            .description("Captcha command")
            .detailedDescription("Captcha command for AuthMeReloaded.")
            .withArgument("captcha", "The Captcha", MANDATORY)
            .permission(PlayerPermission.CAPTCHA)
            .executableCommand(CaptchaCommand.class)
            .register();

        // Register the base verification code command
        CommandDescription verificationBase = CommandDescription.builder()
            .parent(null)
            .labels("verification")
            .description("Verification command")
            .detailedDescription("Command to complete the verification process for AuthMeReloaded.")
            .withArgument("code", "The code", MANDATORY)
            .permission(PlayerPermission.VERIFICATION_CODE)
            .executableCommand(VerificationCommand.class)
            .register();

        List<CommandDescription> baseCommands = ImmutableList.of(authMeBase, emailBase, loginBase, logoutBase,
            registerBase, unregisterBase, changePasswordBase, totpBase, captchaBase, verificationBase);

        setHelpOnAllBases(baseCommands);
        commands = baseCommands;
    }

    /**
     * Creates a command description object for {@code /authme} including its children.
     *
     * @return the authme base command description
     */
    private CommandDescription buildAuthMeBaseCommand() {
        // Register the base AuthMe Reloaded command
        CommandDescription authmeBase = CommandDescription.builder()
            .labels("authme")
            .description("AuthMe op commands")
            .detailedDescription("The main AuthMeReloaded command. The root for all admin commands.")
            .executableCommand(AuthMeCommand.class)
            .register();

        // Register the register command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("register", "reg", "r")
            .description("Register a player")
            .detailedDescription("Register the specified player with the specified password.")
            .withArgument("player", "Player name", MANDATORY)
            .withArgument("password", "Password", MANDATORY)
            .permission(AdminPermission.REGISTER)
            .executableCommand(RegisterAdminCommand.class)
            .register();

        // Register the unregister command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("unregister", "unreg", "unr")
            .description("Unregister a player")
            .detailedDescription("Unregister the specified player.")
            .withArgument("player", "Player name", MANDATORY)
            .permission(AdminPermission.UNREGISTER)
            .executableCommand(UnregisterAdminCommand.class)
            .register();

        // Register the forcelogin command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("forcelogin", "login")
            .description("Enforce login player")
            .detailedDescription("Enforce the specified player to login.")
            .withArgument("player", "Online player name", OPTIONAL)
            .permission(AdminPermission.FORCE_LOGIN)
            .executableCommand(ForceLoginCommand.class)
            .register();

        // Register the changepassword command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("password", "changepassword", "changepass", "cp")
            .description("Change a player's password")
            .detailedDescription("Change the password of a player.")
            .withArgument("player", "Player name", MANDATORY)
            .withArgument("pwd", "New password", MANDATORY)
            .permission(AdminPermission.CHANGE_PASSWORD)
            .executableCommand(ChangePasswordAdminCommand.class)
            .register();

        // Register the last login command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("lastlogin", "ll")
            .description("Player's last login")
            .detailedDescription("View the date of the specified players last login.")
            .withArgument("player", "Player name", OPTIONAL)
            .permission(AdminPermission.LAST_LOGIN)
            .executableCommand(LastLoginCommand.class)
            .register();

        // Register the accounts command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("accounts", "account")
            .description("Display player accounts")
            .detailedDescription("Display all accounts of a player by his player name or IP.")
            .withArgument("player", "Player name or IP", OPTIONAL)
            .permission(AdminPermission.ACCOUNTS)
            .executableCommand(AccountsCommand.class)
            .register();

        // Register the getemail command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("email", "mail", "getemail", "getmail")
            .description("Display player's email")
            .detailedDescription("Display the email address of the specified player if set.")
            .withArgument("player", "Player name", OPTIONAL)
            .permission(AdminPermission.GET_EMAIL)
            .executableCommand(GetEmailCommand.class)
            .register();

        // Register the setemail command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("setemail", "setmail", "chgemail", "chgmail")
            .description("Change player's email")
            .detailedDescription("Change the email address of the specified player.")
            .withArgument("player", "Player name", MANDATORY)
            .withArgument("email", "Player email", MANDATORY)
            .permission(AdminPermission.CHANGE_EMAIL)
            .executableCommand(SetEmailCommand.class)
            .register();

        // Register the getip command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("getip", "ip")
            .description("Get player's IP")
            .detailedDescription("Get the IP address of the specified online player.")
            .withArgument("player", "Player name", MANDATORY)
            .permission(AdminPermission.GET_IP)
            .executableCommand(GetIpCommand.class)
            .register();

        // Register the spawn command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("spawn", "home")
            .description("Teleport to spawn")
            .detailedDescription("Teleport to the spawn.")
            .permission(AdminPermission.SPAWN)
            .executableCommand(SpawnCommand.class)
            .register();

        // Register the setspawn command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("setspawn", "chgspawn")
            .description("Change the spawn")
            .detailedDescription("Change the player's spawn to your current position.")
            .permission(AdminPermission.SET_SPAWN)
            .executableCommand(SetSpawnCommand.class)
            .register();

        // Register the firstspawn command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("firstspawn", "firsthome")
            .description("Teleport to first spawn")
            .detailedDescription("Teleport to the first spawn.")
            .permission(AdminPermission.FIRST_SPAWN)
            .executableCommand(FirstSpawnCommand.class)
            .register();

        // Register the setfirstspawn command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("setfirstspawn", "chgfirstspawn")
            .description("Change the first spawn")
            .detailedDescription("Change the first player's spawn to your current position.")
            .permission(AdminPermission.SET_FIRST_SPAWN)
            .executableCommand(SetFirstSpawnCommand.class)
            .register();

        // Register the purge command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("purge", "delete")
            .description("Purge old data")
            .detailedDescription("Purge old AuthMeReloaded data longer than the specified number of days ago.")
            .withArgument("days", "Number of days", MANDATORY)
            .permission(AdminPermission.PURGE)
            .executableCommand(PurgeCommand.class)
            .register();

        // Purge player command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("purgeplayer")
            .description("Purges the data of one player")
            .detailedDescription("Purges data of the given player.")
            .withArgument("player", "The player to purge", MANDATORY)
            .withArgument("options", "'force' to run without checking if player is registered", OPTIONAL)
            .permission(AdminPermission.PURGE_PLAYER)
            .executableCommand(PurgePlayerCommand.class)
            .register();

        // Backup command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("backup")
            .description("Perform a backup")
            .detailedDescription("Creates a backup of the registered users.")
            .permission(AdminPermission.BACKUP)
            .executableCommand(BackupCommand.class)
            .register();

        // Register the purgelastposition command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("resetpos", "purgelastposition", "purgelastpos", "resetposition",
                "resetlastposition", "resetlastpos")
            .description("Purge player's last position")
            .detailedDescription("Purge the last know position of the specified player or all of them.")
            .withArgument("player/*", "Player name or * for all players", MANDATORY)
            .permission(AdminPermission.PURGE_LAST_POSITION)
            .executableCommand(PurgeLastPositionCommand.class)
            .register();

        // Register the purgebannedplayers command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("purgebannedplayers", "purgebannedplayer", "deletebannedplayers", "deletebannedplayer")
            .description("Purge banned players data")
            .detailedDescription("Purge all AuthMeReloaded data for banned players.")
            .permission(AdminPermission.PURGE_BANNED_PLAYERS)
            .executableCommand(PurgeBannedPlayersCommand.class)
            .register();

        // Register the switchantibot command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("switchantibot", "toggleantibot", "antibot")
            .description("Switch AntiBot mode")
            .detailedDescription("Switch or toggle the AntiBot mode to the specified state.")
            .withArgument("mode", "ON / OFF", OPTIONAL)
            .permission(AdminPermission.SWITCH_ANTIBOT)
            .executableCommand(SwitchAntiBotCommand.class)
            .register();

        // Register the reload command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("reload", "rld")
            .description("Reload plugin")
            .detailedDescription("Reload the AuthMeReloaded plugin.")
            .permission(AdminPermission.RELOAD)
            .executableCommand(ReloadCommand.class)
            .register();

        // Register the version command
        CommandDescription.builder()
            .parent(authmeBase)
            .labels("version", "ver", "v", "about", "info")
            .description("Version info")
            .detailedDescription("Show detailed information about the installed AuthMeReloaded version, the "
                + "developers, contributors, and license.")
            .executableCommand(VersionCommand.class)
            .register();

        CommandDescription.builder()
            .parent(authmeBase)
            .labels("converter", "convert", "conv")
            .description("Converter command")
            .detailedDescription("Converter command for AuthMeReloaded.")
            .withArgument("job", "Conversion job: xauth / crazylogin / rakamak / "
                + "royalauth / vauth / sqliteToSql / mysqlToSqlite / loginsecurity", OPTIONAL)
            .permission(AdminPermission.CONVERTER)
            .executableCommand(ConverterCommand.class)
            .register();

        CommandDescription.builder()
            .parent(authmeBase)
            .labels("messages", "msg")
            .description("Add missing help messages")
            .detailedDescription("Adds missing texts to the current help messages file.")
            .permission(AdminPermission.UPDATE_MESSAGES)
            .executableCommand(UpdateHelpMessagesCommand.class)
            .register();

        CommandDescription.builder()
            .parent(authmeBase)
            .labels("recent")
            .description("See players who have recently logged in")
            .detailedDescription("Shows the last players that have logged in.")
            .permission(AdminPermission.SEE_RECENT_PLAYERS)
            .executableCommand(RecentPlayersCommand.class)
            .register();

        CommandDescription.builder()
            .parent(authmeBase)
            .labels("debug", "dbg")
            .description("Debug features")
            .detailedDescription("Allows various operations for debugging.")
            .withArgument("child", "The child to execute", OPTIONAL)
            .withArgument("arg", "argument (depends on debug section)", OPTIONAL)
            .withArgument("arg", "argument (depends on debug section)", OPTIONAL)
            .permission(DebugSectionPermissions.DEBUG_COMMAND)
            .executableCommand(DebugCommand.class)
            .register();

        return authmeBase;
    }

    /**
     * Creates a command description for {@code /email} including its children.
     *
     * @return the email base command description
     */
    private CommandDescription buildEmailBaseCommand() {
        // Register the base Email command
        CommandDescription emailBase = CommandDescription.builder()
            .parent(null)
            .labels("email")
            .description("Add email or recover password")
            .detailedDescription("The AuthMeReloaded email command base.")
            .executableCommand(EmailBaseCommand.class)
            .register();

        // Register the show command
        CommandDescription.builder()
            .parent(emailBase)
            .labels("show", "myemail")
            .description("Show Email")
            .detailedDescription("Show your current email address.")
            .permission(PlayerPermission.SEE_EMAIL)
            .executableCommand(ShowEmailCommand.class)
            .register();

        // Register the add command
        CommandDescription.builder()
            .parent(emailBase)
            .labels("add", "addemail", "addmail")
            .description("Add Email")
            .detailedDescription("Add a new email address to your account.")
            .withArgument("email", "Email address", MANDATORY)
            .withArgument("verifyEmail", "Email address verification", MANDATORY)
            .permission(PlayerPermission.ADD_EMAIL)
            .executableCommand(AddEmailCommand.class)
            .register();

        // Register the change command
        CommandDescription.builder()
            .parent(emailBase)
            .labels("change", "changeemail", "changemail")
            .description("Change Email")
            .detailedDescription("Change an email address of your account.")
            .withArgument("oldEmail", "Old email address", MANDATORY)
            .withArgument("newEmail", "New email address", MANDATORY)
            .permission(PlayerPermission.CHANGE_EMAIL)
            .executableCommand(ChangeEmailCommand.class)
            .register();

        // Register the recover command
        CommandDescription.builder()
            .parent(emailBase)
            .labels("recover", "recovery", "recoveremail", "recovermail")
            .description("Recover password using email")
            .detailedDescription("Recover your account using an Email address by sending a mail containing "
                + "a new password.")
            .withArgument("email", "Email address", MANDATORY)
            .permission(PlayerPermission.RECOVER_EMAIL)
            .executableCommand(RecoverEmailCommand.class)
            .register();

        // Register the process recovery code command
        CommandDescription.builder()
            .parent(emailBase)
            .labels("code")
            .description("Submit code to recover password")
            .detailedDescription("Recover your account by submitting a code delivered to your email.")
            .withArgument("code", "Recovery code", MANDATORY)
            .permission(PlayerPermission.RECOVER_EMAIL)
            .executableCommand(ProcessCodeCommand.class)
            .register();

        // Register the change password after recovery command
        CommandDescription.builder()
            .parent(emailBase)
            .labels("setpassword")
            .description("Set new password after recovery")
            .detailedDescription("Set a new password after successfully recovering your account.")
            .withArgument("password", "New password", MANDATORY)
            .permission(PlayerPermission.RECOVER_EMAIL)
            .executableCommand(SetPasswordCommand.class)
            .register();

        return emailBase;
    }

    /**
     * Creates a command description object for {@code /totp} including its children.
     *
     * @return the totp base command description
     */
    private CommandDescription buildTotpBaseCommand() {
        // Register the base totp command
        CommandDescription totpBase = CommandDescription.builder()
            .parent(null)
            .labels("totp", "2fa")
            .description("TOTP commands")
            .detailedDescription("Performs actions related to two-factor authentication.")
            .executableCommand(TotpBaseCommand.class)
            .register();

        // Register the base totp code
        CommandDescription.builder()
            .parent(totpBase)
            .labels("code", "c")
            .description("Command for logging in")
            .detailedDescription("Processes the two-factor authentication code during login.")
            .withArgument("code", "The TOTP code to use to log in", MANDATORY)
            .executableCommand(TotpCodeCommand.class)
            .register();

        // Register totp add
        CommandDescription.builder()
            .parent(totpBase)
            .labels("add")
            .description("Enables TOTP")
            .detailedDescription("Enables two-factor authentication for your account.")
            .permission(PlayerPermission.ENABLE_TWO_FACTOR_AUTH)
            .executableCommand(AddTotpCommand.class)
            .register();

        // Register totp confirm
        CommandDescription.builder()
            .parent(totpBase)
            .labels("confirm")
            .description("Enables TOTP after successful code")
            .detailedDescription("Saves the generated TOTP secret after confirmation.")
            .withArgument("code", "Code from the given secret from /totp add", MANDATORY)
            .permission(PlayerPermission.ENABLE_TWO_FACTOR_AUTH)
            .executableCommand(ConfirmTotpCommand.class)
            .register();

        // Register totp remove
        CommandDescription.builder()
            .parent(totpBase)
            .labels("remove")
            .description("Removes TOTP")
            .detailedDescription("Disables two-factor authentication for your account.")
            .withArgument("code", "Current 2FA code", MANDATORY)
            .permission(PlayerPermission.DISABLE_TWO_FACTOR_AUTH)
            .executableCommand(RemoveTotpCommand.class)
            .register();

        return totpBase;
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
                .withArgument("query", "The command or query to view help for.", OPTIONAL)
                .executableCommand(HelpCommand.class)
                .register();
        }
    }
}
