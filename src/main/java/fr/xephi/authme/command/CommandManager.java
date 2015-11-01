package fr.xephi.authme.command;

import fr.xephi.authme.command.executable.*;
import fr.xephi.authme.command.executable.authme.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public class CommandManager {

    /** The list of commandDescriptions. */
    private List<CommandDescription> commandDescriptions = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param registerCommands True to register the commands, false otherwise.
     */
    public CommandManager(boolean registerCommands) {
        // Register the commands
        if(registerCommands)
            registerCommands();
    }

    /**
     * Register all commands.
     */
    @SuppressWarnings("SpellCheckingInspection")
    public void registerCommands() {
        // Register the base Dungeon Maze command
        CommandDescription authMeCommand = new CommandDescription(
                new AuthMeCommand(),
                new ArrayList<String>() {{
                    add("authme");
                }},
                "Main command",
                "The main AuthMeReloaded command. The root for all the other commands.", null);

        // Register the help command
        CommandDescription helpCommand = new CommandDescription(
                new HelpCommand(),
                new ArrayList<String>() {{
                    add("help");
                    add("hlp");
                    add("h");
                    add("sos");
                    add("?");
                }},
                "View help",
                "View detailed help pages about AuthMeReloaded commands.",
                authMeCommand);
        helpCommand.addArgument(new CommandArgumentDescription("query", "The command or query to view help for.", true));
        helpCommand.setMaximumArguments(false);

        /*// Register the create command
        CommandDescription createWorldCommand = new CommandDescription(
                new CreateWorldCommand(),
                new ArrayList<String>() {{
                    add("createworld");
                    add("cw");
                }},
                "Create world",
                "Create a new Dungeon Maze world, the name of the world must be unique.",
                dungeonMazeCommand);
        createWorldCommand.addArgument(new CommandArgumentDescription("world", "The name of the world to create.", false));
        createWorldCommand.addArgument(new CommandArgumentDescription("preload", "True or False to preload the world on startup.", true));
        createWorldCommand.setCommandPermissions("dungeonmaze.command.createworld", CommandPermissions.DefaultPermission.OP_ONLY);

        // Register the teleport command
        CommandDescription teleportCommand = new CommandDescription(
                new TeleportCommand(),
                new ArrayList<String>() {{
                    add("teleport");
                    add("tp");
                    add("warp");
                    add("goto");
                    add("move");
                }},
                "Teleport to world",
                "Teleports to any another world, such as a Dungeon Maze world." ,
                dungeonMazeCommand);
        teleportCommand.addArgument(new CommandArgumentDescription("world", "The name of the world to teleport to.", false));
        teleportCommand.setCommandPermissions("dungeonmaze.command.teleport", CommandPermissions.DefaultPermission.OP_ONLY);

        // Register the load world command
        CommandDescription loadWorldCommand = new CommandDescription(
                new LoadWorldCommand(),
                new ArrayList<String>() {{
                    add("loadworld");
                    add("load");
                }},
                "Load a world",
                "Load a world if it isn't loaded." ,
                dungeonMazeCommand);
        loadWorldCommand.addArgument(new CommandArgumentDescription("world", "The name of the world to load.", false));
        loadWorldCommand.setCommandPermissions("dungeonmaze.command.loadworld", CommandPermissions.DefaultPermission.OP_ONLY);

        // Register the unload world command
        CommandDescription unloadWorldCommand = new CommandDescription(
                new UnloadWorldCommand(),
                new ArrayList<String>() {{
                    add("unloadworld");
                    add("unload");
                }},
                "Unload a world",
                "Unload a loaded world." ,
                dungeonMazeCommand);
        unloadWorldCommand.addArgument(new CommandArgumentDescription("world", "The name of the world to unload.", false));
        unloadWorldCommand.setCommandPermissions("dungeonmaze.command.unloadworld", CommandPermissions.DefaultPermission.OP_ONLY);

        // Register the list world command
        CommandDescription listWorldCommand = new CommandDescription(
                new ListWorldCommand(),
                new ArrayList<String>() {{
                    add("listworlds");
                    add("listworld");
                    add("list");
                    add("worlds");
                    add("lw");
                }},
                "List Dungeon Mazes",
                "Lists the available Dungeon Maze worlds and shows some additional information.",
                dungeonMazeCommand);
        listWorldCommand.setCommandPermissions("dungeonmaze.command.listworlds", CommandPermissions.DefaultPermission.OP_ONLY);*/

        // Register the register command
        CommandDescription registerCommand = new CommandDescription(
                new RegisterCommand(),
                new ArrayList<String>() {{
                    add("register");
                    add("reg");
                }},
                "Register a player",
                "Register the specified player with the specified password.",
                authMeCommand);
        registerCommand.setCommandPermissions("authme.admin.register", CommandPermissions.DefaultPermission.OP_ONLY);
        registerCommand.addArgument(new CommandArgumentDescription("player", "Player name", false));
        registerCommand.addArgument(new CommandArgumentDescription("password", "Password", false));

        // Register the purge command
        CommandDescription lastLoginCommand = new CommandDescription(
                new LastLoginCommand(),
                new ArrayList<String>() {{
                    add("lastlogin");
                    add("ll");
                }},
                "Player's last login",
                "View the date of the specified players last login",
                authMeCommand);
        lastLoginCommand.setCommandPermissions("authme.admin.lastlogin", CommandPermissions.DefaultPermission.OP_ONLY);
        lastLoginCommand.addArgument(new CommandArgumentDescription("player", "Player name", true));

        // Register the accounts command
        CommandDescription accountsCommand = new CommandDescription(
                new AccountsCommand(),
                new ArrayList<String>() {{
                    add("accounts");
                    add("account");
                }},
                "Display player accounts",
                "Display all accounts of a player by it's player name or IP.",
                authMeCommand);
        accountsCommand.setCommandPermissions("authme.admin.accounts", CommandPermissions.DefaultPermission.OP_ONLY);
        accountsCommand.addArgument(new CommandArgumentDescription("player", "Player name or IP", true));

        // Register the getemail command
        CommandDescription getEmailCommand = new CommandDescription(
                new GetEmailCommand(),
                new ArrayList<String>() {{
                    add("getemail");
                    add("getmail");
                    add("email");
                    add("mail");
                }},
                "Display player's email",
                "Display the email address of the specified player if set.",
                authMeCommand);
        getEmailCommand.setCommandPermissions("authme.admin.getemail", CommandPermissions.DefaultPermission.OP_ONLY);
        getEmailCommand.addArgument(new CommandArgumentDescription("player", "Player name", true));

        // Register the setemail command
        CommandDescription setEmailCommand = new CommandDescription(
                new SetEmailCommand(),
                new ArrayList<String>() {{
                    add("chgemail");
                    add("chgmail");
                    add("setemail");
                    add("setmail");
                }},
                "Change player's email",
                "Change the email address of the specified player.",
                authMeCommand);
        setEmailCommand.setCommandPermissions("authme.admin.chgemail", CommandPermissions.DefaultPermission.OP_ONLY);
        setEmailCommand.addArgument(new CommandArgumentDescription("player", "Player name", false));
        setEmailCommand.addArgument(new CommandArgumentDescription("email", "Player email", false));

        // Register the setspawn command
        CommandDescription setSpawnCommand = new CommandDescription(
                new SetSpawnCommand(),
                new ArrayList<String>() {{
                    add("setspawn");
                    add("chgspawn");
                }},
                "Change the spawn",
                "Change the player's spawn to your current position.",
                authMeCommand);
        setSpawnCommand.setCommandPermissions("authme.admin.setspawn", CommandPermissions.DefaultPermission.OP_ONLY);

        // Register the setfirstspawn command
        CommandDescription setFirstSpawnCommand = new CommandDescription(
                new SetFirstSpawnCommand(),
                new ArrayList<String>() {{
                    add("setfirstspawn");
                    add("chgfirstspawn");
                }},
                "Change the first spawn",
                "Change the first player's spawn to your current position.",
                authMeCommand);
        setFirstSpawnCommand.setCommandPermissions("authme.admin.setfirstspawn", CommandPermissions.DefaultPermission.OP_ONLY);

        // Register the purge command
        CommandDescription purgeCommand = new CommandDescription(
                new PurgeCommand(),
                new ArrayList<String>() {{
                    add("purge");
                    add("delete");
                }},
                "Purge AuthMeReloaded data",
                "Purge old AuthMeReloaded data longer than the specified amount of days ago.",
                authMeCommand);
        purgeCommand.setCommandPermissions("authme.admin.purge", CommandPermissions.DefaultPermission.OP_ONLY);
        purgeCommand.addArgument(new CommandArgumentDescription("days", "Number of days", false));

        // Register the reload command
        CommandDescription reloadCommand = new CommandDescription(
                new ReloadCommand(),
                new ArrayList<String>() {{
                    add("reload");
                    add("rld");
                    add("r");
                }},
                "Reload AuthMeReloaded",
                "Reload the AuthMeReloaded plugin.",
                authMeCommand);
        reloadCommand.setCommandPermissions("authme.admin.reload", CommandPermissions.DefaultPermission.OP_ONLY);

        /* // Register the reload permissions command
        CommandDescription reloadPermissionsCommand = new CommandDescription(
                new ReloadPermissionsCommand(),
                new ArrayList<String>() {{
                    add("reloadpermissions");
                    add("reloadpermission");
                    add("reloadperms");
                    add("rp");
                }},
                "Reload permissions",
                "Reload the permissions system and rehook the installed permissions system.",
                dungeonMazeCommand);
        reloadPermissionsCommand.setCommandPermissions("dungeonmaze.command.reloadpermissions", CommandPermissions.DefaultPermission.OP_ONLY);

        // Register the restart command
        CommandDescription restartCommand = new CommandDescription(
                new RestartCommand(),
                new ArrayList<String>() {{
                    add("restart");
                    add("rstrt");
                }},
                "Restart Dungeon Maze",
                "Restart the Dungeon Maze plugin.",
                dungeonMazeCommand);
        restartCommand.setCommandPermissions("dungeonmaze.command.restart", CommandPermissions.DefaultPermission.OP_ONLY);
        restartCommand.addArgument(new CommandArgumentDescription("force", "True or False to force restart.", true));

        // Register the check updates command
        CommandDescription checkUpdatesCommand = new CommandDescription(
                new CheckUpdatesCommand(),
                new ArrayList<String>() {{
                    add("checkupdates");
                    add("checkupdate");
                    add("check");
                    add("updates");
                    add("update");
                    add("cu");
                }},
                "Check updates",
                "Check for available updates to install.",
                dungeonMazeCommand);
        checkUpdatesCommand.setCommandPermissions("dungeonmaze.command.checkupdates", CommandPermissions.DefaultPermission.OP_ONLY);

        // Register the install update command
        CommandDescription installUpdateCommand = new CommandDescription(
                new InstallUpdateCommand(),
                new ArrayList<String>() {{
                    add("installupdates");
                    add("installupdate");
                    add("install");
                    add("iu");
                }},
                "Install update",
                "Try to install any availble update.",
                dungeonMazeCommand);
        installUpdateCommand.setCommandPermissions("dungeonmaze.command.installupdate", CommandPermissions.DefaultPermission.OP_ONLY);

        // Register the status command
        CommandDescription statusCommand = new CommandDescription(
                new StatusCommand(),
                new ArrayList<String>() {{
                    add("status");
                    add("stats");
                    add("s");
                }},
                "Status info",
                "Show detailed plugin status.",
                dungeonMazeCommand);
        statusCommand.setMaximumArguments(false);
        statusCommand.setCommandPermissions("dungeonmaze.command.status", CommandPermissions.DefaultPermission.OP_ONLY);

        // Register the status command
        CommandDescription serviceCommand = new CommandDescription(
                new ServiceCommand(),
                new ArrayList<String>() {{
                    add("services");
                    add("service");
                    add("serv");
                }},
                "Services command",
                "Show detailed information about all the Dungeon Maze serivces.",
                dungeonMazeCommand);
        serviceCommand.setMaximumArguments(false);
        serviceCommand.setCommandPermissions("dungeonmaze.command.services", CommandPermissions.DefaultPermission.OP_ONLY);*/

        // Register the version command
        CommandDescription versionCommand = new CommandDescription(
                new VersionCommand(),
                new ArrayList<String>() {{
                    add("version");
                    add("ver");
                    add("v");
                    add("about");
                    add("info");
                }},
                "Version info",
                "Show detailed information about the installed AuthMeReloaded version, and shows the developers, contributors, license and other information.",
                authMeCommand);
        versionCommand.setMaximumArguments(false);

        // Add the base commands to the commands array
        this.commandDescriptions.add(authMeCommand);
    }

    /**
     * Get the list of command descriptions
     *
     * @return List of command descriptions.
     */
    public List<CommandDescription> getCommandDescriptions() {
        return this.commandDescriptions;
    }

    /**
     * Get the number of command description count.
     *
     * @return Command description count.
     */
    public int getCommandDescriptionCount() {
        return this.getCommandDescriptions().size();
    }

    /**
     * Find the best suitable command for the specified reference.
     *
     * @param queryReference The query reference to find a command for.
     *
     * @return The command found, or null.
     */
    public FoundCommandResult findCommand(CommandParts queryReference) {
        // Make sure the command reference is valid
        if(queryReference.getCount() <= 0)
            return null;

        // Get the base command description
        for(CommandDescription commandDescription : this.commandDescriptions) {
            // Check whether there's a command description available for the current command
            if(!commandDescription.isSuitableLabel(queryReference))
                continue;

            // Find the command reference, return the result
            return commandDescription.findCommand(queryReference);
        }

        // No applicable command description found, return false
        return null;
    }
}
