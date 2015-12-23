package fr.xephi.authme.command;

import fr.xephi.authme.command.executable.HelpCommand;
import fr.xephi.authme.permission.DefaultPermission;
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

    public static Set<CommandDescription> generateCommands() {
        // Register /authme
        CommandDescription authMeBase = createCommand(null, null, singletonList("authme"));
        // Register /authme login <password>
        createCommand(PlayerPermission.LOGIN, authMeBase, singletonList("login"), newArgument("password", false));
        // Register /authme register <password> <confirmation>, alias: /authme reg
        createCommand(PlayerPermission.LOGIN, authMeBase, asList("register", "reg"),
            newArgument("password", false), newArgument("confirmation", false));

        // Register /email [player]
        CommandDescription emailBase = createCommand(null, null, singletonList("email"));
        // Register /email helptest -- use only to test for help command arguments special case
        CommandDescription.builder().parent(emailBase).labels("helptest").executableCommand(mock(HelpCommand.class))
            .description("test").detailedDescription("Test.").withArgument("Query", "", false).build();

        // Register /unregister <player>, alias: /unreg
        CommandDescription unregisterBase = createCommand(null, null, asList("unregister", "unreg"),
            newArgument("player", false));

        return newHashSet(authMeBase, emailBase, unregisterBase);
    }

    private static CommandDescription createCommand(PlayerPermission permission, CommandDescription parent,
                                                    List<String> labels, CommandArgumentDescription... arguments) {
        CommandDescription.CommandBuilder command = CommandDescription.builder()
            .labels(labels)
            .parent(parent)
            .permissions(DefaultPermission.OP_ONLY, permission)
            .description(labels.get(0))
            .detailedDescription("'" + labels.get(0) + "' test command")
            .executableCommand(mock(ExecutableCommand.class));

        if (arguments != null && arguments.length > 0) {
            for (CommandArgumentDescription argument : arguments) {
                command.withArgument(argument.getName(), "Test description", argument.isOptional());
            }
        }

        return command.build();
    }

    private static CommandArgumentDescription newArgument(String label, boolean isOptional) {
        return new CommandArgumentDescription(label, "Test description", isOptional);
    }

    public static CommandDescription getCommandWithLabel(Collection<CommandDescription> commands, String parentLabel,
                                                         String childLabel) {
        CommandDescription parent = getCommandWithLabel(commands, parentLabel);
        return getCommandWithLabel(parent.getChildren(), childLabel);
    }

    public static CommandDescription getCommandWithLabel(Collection<CommandDescription> commands, String label) {
        for (CommandDescription child : commands) {
            if (child.hasLabel(label)) {
                return child;
            }
        }
        throw new RuntimeException("Could not find command with label '" + label + "'");
    }
}
