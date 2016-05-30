package fr.xephi.authme.command;

import fr.xephi.authme.command.executable.HelpCommand;
import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PlayerPermission;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;

/**
 * Util class for generating and retrieving test commands.
 */
public final class TestCommandsUtil {

    private TestCommandsUtil() {
    }

    /**
     * Generate the collection of test commands.
     *
     * @return The generated commands
     */
    public static Set<CommandDescription> generateCommands() {
        // Register /authme
        CommandDescription authMeBase = createCommand(null, null, singletonList("authme"));
        // Register /authme login <password>
        createCommand(PlayerPermission.LOGIN, authMeBase, singletonList("login"), newArgument("password", false));
        // Register /authme register <password> <confirmation>, aliases: /authme reg, /authme r
        createCommand(PlayerPermission.LOGIN, authMeBase, asList("register", "reg", "r"),
            newArgument("password", false), newArgument("confirmation", false));

        // Register /email [player]
        CommandDescription emailBase = createCommand(null, null, singletonList("email"), newArgument("player", true));
        // Register /email helptest -- use only to test for help command arguments special case
        CommandDescription.builder().parent(emailBase).labels("helptest").executableCommand(mock(HelpCommand.class))
            .description("test").detailedDescription("Test.").withArgument("Query", "", false).build();

        // Register /unregister <player>, alias: /unreg
        CommandDescription unregisterBase = createCommand(null, null, asList("unregister", "unreg"),
            newArgument("player", false));

        return newHashSet(authMeBase, emailBase, unregisterBase);
    }

    /**
     * Retrieve the command with the given label from the collection of commands.
     * Example: <code>getCommandWithLabel(commands, "authme", "reg")</code> to find the command description
     * which defines the command /authme reg.
     *
     * @param commands The commands to search in
     * @param parentLabel The parent label to search for
     * @param childLabel The child label to find
     * @return The matched command, or throws an exception if no command could be found
     */
    public static CommandDescription getCommandWithLabel(Collection<CommandDescription> commands, String parentLabel,
                                                         String childLabel) {
        CommandDescription parent = getCommandWithLabel(commands, parentLabel);
        return getCommandWithLabel(parent.getChildren(), childLabel);
    }

    /**
     * Retrieve the command with the given label from the collection of commands.
     *
     * @param commands The commands to search in
     * @param label The label to search for
     * @return The matched command, or throws an exception if no command could be found
     */
    public static CommandDescription getCommandWithLabel(Collection<CommandDescription> commands, String label) {
        for (CommandDescription child : commands) {
            if (child.hasLabel(label)) {
                return child;
            }
        }
        throw new IllegalStateException("Could not find command with label '" + label + "'");
    }

    /** Shortcut command to initialize a new test command. */
    private static CommandDescription createCommand(PermissionNode permission, CommandDescription parent,
                                                    List<String> labels, CommandArgumentDescription... arguments) {
        PermissionNode[] notNullPermission;
        if (permission == null) {
            notNullPermission = new PermissionNode[0];
        } else {
            notNullPermission = new PermissionNode[1];
            notNullPermission[0] = permission;
        }

        CommandDescription.CommandBuilder command = CommandDescription.builder()
            .labels(labels)
            .parent(parent)
            .permissions(DefaultPermission.OP_ONLY, notNullPermission)
            .description(labels.get(0) + " cmd")
            .detailedDescription("'" + labels.get(0) + "' test command")
            .executableCommand(mock(ExecutableCommand.class));

        if (arguments != null && arguments.length > 0) {
            for (CommandArgumentDescription argument : arguments) {
                command.withArgument(argument.getName(), argument.getDescription(), argument.isOptional());
            }
        }

        return command.build();
    }

    /** Shortcut command to initialize a new argument description. */
    private static CommandArgumentDescription newArgument(String label, boolean isOptional) {
        return new CommandArgumentDescription(label, "'" + label + "' argument description", isOptional);
    }

}
