package fr.xephi.authme.command;

import fr.xephi.authme.command.executable.HelpCommand;
import fr.xephi.authme.command.executable.authme.AccountsCommand;
import fr.xephi.authme.command.executable.authme.AuthMeCommand;
import fr.xephi.authme.command.executable.authme.ChangePasswordCommand;
import fr.xephi.authme.command.executable.authme.FirstSpawnCommand;
import fr.xephi.authme.command.executable.authme.ForceLoginCommand;
import fr.xephi.authme.command.executable.authme.GetEmailCommand;
import fr.xephi.authme.command.executable.authme.GetIpCommand;
import fr.xephi.authme.command.executable.authme.LastLoginCommand;
import fr.xephi.authme.command.executable.authme.PurgeBannedPlayersCommand;
import fr.xephi.authme.command.executable.authme.PurgeCommand;
import fr.xephi.authme.command.executable.authme.PurgeLastPositionCommand;
import fr.xephi.authme.command.executable.authme.RegisterCommand;
import fr.xephi.authme.command.executable.authme.ReloadCommand;
import fr.xephi.authme.command.executable.authme.SetEmailCommand;
import fr.xephi.authme.command.executable.authme.SetFirstSpawnCommand;
import fr.xephi.authme.command.executable.authme.SetSpawnCommand;
import fr.xephi.authme.command.executable.authme.SpawnCommand;
import fr.xephi.authme.command.executable.authme.SwitchAntiBotCommand;
import fr.xephi.authme.command.executable.authme.UnregisterCommand;
import fr.xephi.authme.command.executable.authme.VersionCommand;
import fr.xephi.authme.command.executable.captcha.CaptchaCommand;
import fr.xephi.authme.command.executable.converter.ConverterCommand;
import fr.xephi.authme.command.executable.email.AddEmailCommand;
import fr.xephi.authme.command.executable.email.ChangeEmailCommand;
import fr.xephi.authme.command.executable.email.RecoverEmailCommand;
import fr.xephi.authme.command.executable.login.LoginCommand;
import fr.xephi.authme.command.executable.logout.LogoutCommand;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.util.Wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fr.xephi.authme.command.CommandPermissions.DefaultPermission.ALLOWED;
import static fr.xephi.authme.command.CommandPermissions.DefaultPermission.OP_ONLY;

/**
 * Initializes all available AuthMe commands.
 */
public final class CommandInitializer {

    private static List<CommandDescription> baseCommands;

    private CommandInitializer() {
        // Helper class
    }

    public static List<CommandDescription> getBaseCommands() {
        if (baseCommands == null) {
            Wrapper.getInstance().getLogger().info("Initializing AuthMe commands");
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
            .parent(authMeBaseCommand)
            .permissions(OP_ONLY, PlayerPermission.REGISTER)
            .withArgument("player", "Player name", false)
            .withArgument("password", "Password", false)
            .permissions(OP_ONLY, UserPermission.REGISTER)
            .executableCommand(new RegisterCommand())
            .build();

        // Register the unregister command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("unregister", "unreg", "unr")
            .description("Unregister a player")
            .detailedDescription("Unregister the specified player.")
            .parent(authMeBaseCommand)
            .permissions(OP_ONLY, PlayerPermission.UNREGISTER)
            .withArgument("player", "Player name", false)
            .permissions(OP_ONLY, UserPermission.UNREGISTER)
            .executableCommand(new UnregisterCommand())
            .build();

        // Register the forcelogin command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("forcelogin", "login")
            .description("Enforce login player")
            .detailedDescription("Enforce the specified player to login.")
            .parent(authMeBaseCommand)
            .permissions(OP_ONLY, PlayerPermission.CAN_LOGIN_BE_FORCED)
            .withArgument("player", "Online player name", true)
            .permissions(OP_ONLY, UserPermission.CAN_LOGIN_BE_FORCED)
            .executableCommand(new ForceLoginCommand())
            .build();

        // Register the changepassword command
        CommandDescription.builder()
            .parent(AUTHME_BASE)
            .labels("password", "changepassword", "changepass", "cp")
            .description("Change a player's password")
            .detailedDescription("Change the password of a player.")
            .parent(authMeBaseCommand)
            .permissions(OP_ONLY, PlayerPermission.CHANGE_PASSWORD)
            .withArgument("player", "Player name", false)
            .withArgument("pwd", "New password", false)
            .permissions(OP_ONLY, UserPermission.CHANGE_PASSWORD)
            .executableCommand(new ChangePasswordCommand())
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
        CommandDescription getEmailCommand = CommandDescription.builder()
            .executableCommand(new GetEmailCommand())
            .labels("getemail", "getmail", "email", "mail")
            .description("Display player's email")
            .detailedDescription("Display the email address of the specified player if set.")
            .parent(authMeBaseCommand)
            .permissions(OP_ONLY, AdminPermission.GET_EMAIL)
            .withArgument("player", "Player name", true)
            .build();

        // Register the setemail command
        CommandDescription setEmailCommand = CommandDescription.builder()
            .executableCommand(new SetEmailCommand())
            .labels("chgemail", "chgmail", "setemail", "setmail")
            .description("Change player's email")
            .detailedDescription("Change the email address of the specified player.")
            .parent(authMeBaseCommand)
            .permissions(OP_ONLY, AdminPermission.CHANGE_EMAIL)
            .withArgument("player", "Player name", false)
            .withArgument("email", "Player email", false)
            .build();

        // Register the getip command
        CommandDescription getIpCommand = new CommandDescription(new GetIpCommand(), new ArrayList<String>() {
            {
                add("getip");
                add("ip");
            }
        }, "Get player's IP", "Get the IP address of the specified online player.", AUTHME_BASE);
        getIpCommand.setCommandPermissions(AdminPermission.GET_IP, OP_ONLY);
        getIpCommand.addArgument(new CommandArgumentDescription("player", "Online player name", true));

        // Register the spawn command
        CommandDescription spawnCommand = new CommandDescription(new SpawnCommand(), new ArrayList<String>() {
            {
                add("spawn");
                add("home");
            }
        }, "Teleport to spawn", "Teleport to the spawn.", AUTHME_BASE);
        spawnCommand.setCommandPermissions(AdminPermission.SPAWN, OP_ONLY);

        // Register the setspawn command
        CommandDescription setSpawnCommand = new CommandDescription(new SetSpawnCommand(), new ArrayList<String>() {
            {
                add("setspawn");
                add("chgspawn");
            }
        }, "Change the spawn", "Change the player's spawn to your current position.", AUTHME_BASE);
        setSpawnCommand.setCommandPermissions(AdminPermission.SET_SPAWN, OP_ONLY);

        // Register the firstspawn command
        CommandDescription firstSpawnCommand = new CommandDescription(new FirstSpawnCommand(), new ArrayList<String>() {
            {
                add("firstspawn");
                add("firsthome");
            }
        }, "Teleport to first spawn", "Teleport to the first spawn.", AUTHME_BASE);
        firstSpawnCommand.setCommandPermissions(AdminPermission.FIRST_SPAWN, OP_ONLY);

        // Register the setfirstspawn command
        CommandDescription setFirstSpawnCommand = new CommandDescription(new SetFirstSpawnCommand(), new ArrayList<String>() {
            {
                add("setfirstspawn");
                add("chgfirstspawn");
            }
        }, "Change the first spawn", "Change the first player's spawn to your current position.", AUTHME_BASE);
        setFirstSpawnCommand.setCommandPermissions(AdminPermission.SET_FIRST_SPAWN, OP_ONLY);

        // Register the purge command
        CommandDescription purgeCommand = new CommandDescription(new PurgeCommand(), new ArrayList<String>() {
            {
                add("purge");
                add("delete");
            }
        }, "Purge old data", "Purge old AuthMeReloaded data longer than the specified amount of days ago.", AUTHME_BASE);
        purgeCommand.setCommandPermissions(AdminPermission.PURGE, OP_ONLY);
        purgeCommand.addArgument(new CommandArgumentDescription("days", "Number of days", false));

        // Register the purgelastposition command
        CommandDescription purgeLastPositionCommand = new CommandDescription(new PurgeLastPositionCommand(), new ArrayList<String>() {
            {
                add("resetpos");
                add("purgelastposition");
                add("purgelastpos");
                add("resetposition");
                add("resetlastposition");
                add("resetlastpos");
            }
        }, "Purge player's last position", "Purge the last know position of the specified player.", AUTHME_BASE);
        purgeLastPositionCommand.setCommandPermissions(AdminPermission.PURGE_LAST_POSITION, OP_ONLY);
        purgeLastPositionCommand.addArgument(new CommandArgumentDescription("player", "Player name", true));

        // Register the purgebannedplayers command
        CommandDescription purgeBannedPlayersCommand = new CommandDescription(new PurgeBannedPlayersCommand(), new ArrayList<String>() {
            {
                add("purgebannedplayers");
                add("purgebannedplayer");
                add("deletebannedplayers");
                add("deletebannedplayer");
            }
        }, "Purge banned palyers data", "Purge all AuthMeReloaded data for banned players.", AUTHME_BASE);
        purgeBannedPlayersCommand.setCommandPermissions(AdminPermission.PURGE_BANNED_PLAYERS, OP_ONLY);

        // Register the switchantibot command
        CommandDescription switchAntiBotCommand = new CommandDescription(new SwitchAntiBotCommand(), new ArrayList<String>() {
            {
                add("switchantibot");
                add("toggleantibot");
                add("antibot");
            }
        }, "Switch AntiBot mode", "Switch or toggle the AntiBot mode to the specified state.", AUTHME_BASE);
        switchAntiBotCommand.setCommandPermissions(AdminPermission.SWITCH_ANTIBOT, OP_ONLY);
        switchAntiBotCommand.addArgument(new CommandArgumentDescription("mode", "ON / OFF", true));

        // // Register the resetname command
        // CommandDescription resetNameCommand = new CommandDescription(
        // new ResetNameCommand(),
        // new ArrayList<String>() {{
        // add("resetname");
        // add("resetnames");
        // }},
        // "Reset name",
        // "Reset name",
        // authMeCommand);
        // resetNameCommand.setCommandPermissions("authme.admin.resetname",
        // CommandPermissions.DefaultPermission.OP_ONLY);

        // Register the reload command
        CommandDescription reloadCommand = new CommandDescription(new ReloadCommand(), new ArrayList<String>() {
            {
                add("reload");
                add("rld");
            }
        }, "Reload plugin", "Reload the AuthMeReloaded plugin.", AUTHME_BASE);
        reloadCommand.setCommandPermissions(AdminPermission.RELOAD, OP_ONLY);

        // Register the version command
        CommandDescription versionCommand = CommandDescription.builder()
            .executableCommand(new VersionCommand())
            .labels("version", "ver", "v", "about", "info")
            .description("Version info")
            .detailedDescription("Show detailed information about the installed AuthMeReloaded version, and shows the "
                + "developers, contributors, license and other information.")
            .parent(AUTHME_BASE)
            .build();

        // Register the base login command
        CommandDescription loginBaseCommand = CommandDescription.builder()
            .executableCommand(new LoginCommand())
            .labels("login", "l")
            .description("Login command")
            .detailedDescription("Command to log in using AuthMeReloaded.")
            .parent(null)
            .permissions(ALLOWED, PlayerPermission.LOGIN)
            .withArgument("password", "Login password", false)
            .build();

        // Register the help command
        CommandDescription loginHelpCommand = new CommandDescription(helpCommandExecutable, helpCommandLabels,
            "View help", "View detailed help pages about AuthMeReloaded login commands.", loginBaseCommand);
        loginHelpCommand.addArgument(new CommandArgumentDescription("query", "The command or query to view help for.", true));

        // Register the base logout command
        CommandDescription logoutBaseCommand = new CommandDescription(new LogoutCommand(), new ArrayList<String>() {
            {
                add("logout");
            }
        }, "Logout command", "Command to logout using AuthMeReloaded.", null);
        logoutBaseCommand.setCommandPermissions(PlayerPermission.LOGOUT, CommandPermissions.DefaultPermission.ALLOWED);

        // Register the help command
        CommandDescription logoutHelpCommand = new CommandDescription(helpCommandExecutable, helpCommandLabels,
            "View help", "View detailed help pages about AuthMeReloaded logout commands.", logoutBaseCommand);
        logoutHelpCommand.addArgument(new CommandArgumentDescription("query", "The command or query to view help for.", true));

        // Register the base register command
        CommandDescription registerBaseCommand = new CommandDescription(new fr.xephi.authme.command.executable.register.RegisterCommand(), new ArrayList<String>() {
            {
                add("register");
                add("reg");
            }
        }, "Registration command", "Command to register using AuthMeReloaded.", null);
        registerBaseCommand.setCommandPermissions(PlayerPermission.REGISTER, CommandPermissions.DefaultPermission.ALLOWED);
        registerBaseCommand.addArgument(new CommandArgumentDescription("password", "Password", false));
        registerBaseCommand.addArgument(new CommandArgumentDescription("verifyPassword", "Verify password", false));

        // Register the help command
        CommandDescription registerHelpCommand = new CommandDescription(helpCommandExecutable, helpCommandLabels,
            "View help", "View detailed help pages about AuthMeReloaded register commands.", registerBaseCommand);
        registerHelpCommand.addArgument(new CommandArgumentDescription("query", "The command or query to view help for.", true));

        // Register the base unregister command
        CommandDescription unregisterBaseCommand = new CommandDescription(new fr.xephi.authme.command.executable.unregister.UnregisterCommand(), new ArrayList<String>() {
            {
                add("unregister");
                add("unreg");
            }
        }, "Unregistration command", "Command to unregister using AuthMeReloaded.", null);
        unregisterBaseCommand.setCommandPermissions(PlayerPermission.UNREGISTER, CommandPermissions.DefaultPermission.ALLOWED);
        unregisterBaseCommand.addArgument(new CommandArgumentDescription("password", "Password", false));

        // Register the help command
        CommandDescription unregisterHelpCommand = new CommandDescription(helpCommandExecutable, helpCommandLabels, "View help", "View detailed help pages about AuthMeReloaded unregister commands.", unregisterBaseCommand);
        unregisterHelpCommand.addArgument(new CommandArgumentDescription("query", "The command or query to view help for.", true));

        // Register the base changepassword command
        CommandDescription changePasswordBaseCommand = new CommandDescription(new fr.xephi.authme.command.executable.changepassword.ChangePasswordCommand(), new ArrayList<String>() {
            {
                add("changepassword");
                add("changepass");
            }
        }, "Change password command", "Command to change your password using AuthMeReloaded.", null);
        changePasswordBaseCommand.setCommandPermissions(PlayerPermission.CHANGE_PASSWORD, CommandPermissions.DefaultPermission.ALLOWED);
        changePasswordBaseCommand.addArgument(new CommandArgumentDescription("password", "Password", false));
        changePasswordBaseCommand.addArgument(new CommandArgumentDescription("verifyPassword", "Verify password", false));

        // Register the help command
        CommandDescription changePasswordHelpCommand = new CommandDescription(helpCommandExecutable, helpCommandLabels,
            "View help", "View detailed help pages about AuthMeReloaded change password commands.", changePasswordBaseCommand);
        changePasswordHelpCommand.addArgument(new CommandArgumentDescription("query", "The command or query to view help for.", true));

        // Register the base Dungeon Maze command
        CommandDescription emailBaseCommand = new CommandDescription(helpCommandExecutable, new ArrayList<String>() {
            {
                add("email");
                add("mail");
            }
        }, "E-mail command", "The AuthMe Reloaded E-mail command. The root for all E-mail commands.", null);

        // Register the help command
        CommandDescription emailHelpCommand = new CommandDescription(helpCommandExecutable, helpCommandLabels,
            "View help", "View detailed help pages about AuthMeReloaded help commands.", emailBaseCommand);
        emailHelpCommand.addArgument(new CommandArgumentDescription("query", "The command or query to view help for.", true));

        // Register the add command
        CommandDescription addEmailCommand = new CommandDescription(new AddEmailCommand(), new ArrayList<String>() {
            {
                add("add");
                add("addemail");
                add("addmail");
            }
        }, "Add E-mail", "Add an new E-Mail address to your account.", emailBaseCommand);
        addEmailCommand.setCommandPermissions(PlayerPermission.ADD_EMAIL, CommandPermissions.DefaultPermission.ALLOWED);
        addEmailCommand.addArgument(new CommandArgumentDescription("email", "Email address", false));
        addEmailCommand.addArgument(new CommandArgumentDescription("verifyEmail", "Email address verification", false));

        // Register the change command
        CommandDescription changeEmailCommand = new CommandDescription(new ChangeEmailCommand(), new ArrayList<String>() {
            {
                add("change");
                add("changeemail");
                add("changemail");
            }
        }, "Change E-mail", "Change an E-Mail address of your account.", emailBaseCommand);
        changeEmailCommand.setCommandPermissions(PlayerPermission.CHANGE_EMAIL, CommandPermissions.DefaultPermission.ALLOWED);
        changeEmailCommand.addArgument(new CommandArgumentDescription("oldEmail", "Old email address", false));
        changeEmailCommand.addArgument(new CommandArgumentDescription("newEmail", "New email address", false));

        // Register the recover command
        CommandDescription recoverEmailCommand = new CommandDescription(new RecoverEmailCommand(), new ArrayList<String>() {
            {
                add("recover");
                add("recovery");
                add("recoveremail");
                add("recovermail");
            }
        }, "Recover using E-mail", "Recover your account using an E-mail address.", emailBaseCommand);
        recoverEmailCommand.setCommandPermissions(PlayerPermission.RECOVER_EMAIL, CommandPermissions.DefaultPermission.ALLOWED);
        recoverEmailCommand.addArgument(new CommandArgumentDescription("email", "Email address", false));

        // Register the base captcha command
        CommandDescription captchaBaseCommand = new CommandDescription(new CaptchaCommand(), new ArrayList<String>() {
            {
                add("captcha");
                add("capt");
            }
        }, "Captcha command", "Captcha command for AuthMeReloaded.", null);
        captchaBaseCommand.setCommandPermissions(PlayerPermission.CAPTCHA, CommandPermissions.DefaultPermission.ALLOWED);
        captchaBaseCommand.addArgument(new CommandArgumentDescription("captcha", "The captcha", false));

        // Register the help command
        CommandDescription captchaHelpCommand = new CommandDescription(helpCommandExecutable, helpCommandLabels,
            "View help", "View detailed help pages about AuthMeReloaded change captcha commands.", captchaBaseCommand);
        captchaHelpCommand.addArgument(new CommandArgumentDescription("query", "The command or query to view help for.", true));

        // Register the base converter command
        CommandDescription converterBaseCommand = new CommandDescription(new ConverterCommand(), new ArrayList<String>() {
            {
                add("converter");
                add("convert");
                add("conv");
            }
        }, "Convert command", "Convert command for AuthMeReloaded.", null);
        converterBaseCommand.setCommandPermissions(AdminPermission.CONVERTER, OP_ONLY);
        converterBaseCommand.addArgument(new CommandArgumentDescription("job", "Conversion job: flattosql / flattosqlite /| xauth / crazylogin / rakamak / royalauth / vauth / sqltoflat", false));

        // Register the help command
        CommandDescription converterHelpCommand = new CommandDescription(helpCommandExecutable, helpCommandLabels,
            "View help", "View detailed help pages about AuthMeReloaded change captcha commands.", converterBaseCommand);
        converterHelpCommand.addArgument(new CommandArgumentDescription("query", "The command or query to view help for.", true));

        // Add the base commands to the commands array
        baseCommands = Arrays.asList(
            AUTHME_BASE,
            loginBaseCommand,
            logoutBaseCommand,
            registerBaseCommand,
            unregisterBaseCommand,
            changePasswordBaseCommand,
            emailBaseCommand,
            captchaBaseCommand,
            converterBaseCommand);
    }
}
