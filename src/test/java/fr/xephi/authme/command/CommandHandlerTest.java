package fr.xephi.authme.command;

import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link CommandHandler}.
 */
public class CommandHandlerTest {

    private static Set<CommandDescription> commands;
    private static CommandHandler handler;
    private static PermissionsManager permissionsManagerMock;

    @BeforeClass
    public static void setUpCommandHandler() {
        CommandDescription authMeBase = createCommand(null, null, singletonList("authme"));
        createCommand(PlayerPermission.LOGIN, authMeBase, singletonList("login"), newArgument("password", false));
        createCommand(PlayerPermission.LOGIN, authMeBase, asList("register", "reg"),
            newArgument("password", false), newArgument("confirmation", false));

        CommandDescription testBase = createCommand(null, null, singletonList("test"), newArgument("test", true));
        commands = new HashSet<>(asList(authMeBase, testBase));
        permissionsManagerMock = mock(PermissionsManager.class);
        handler = new CommandHandler(commands, permissionsManagerMock);
    }

    @Test
    public void shouldMapPartsToLoginChildCommand() {
        // given
        List<String> parts = Arrays.asList("authme", "login", "test1");

        // when
        FoundCommandResult result = handler.mapPartsToCommand(parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getChildWithLabel("login", "authme")));
        assertThat(result.getResultStatus(), equalTo(FoundCommandResult.ResultStatus.SUCCESS));
        assertThat(result.getArguments(), contains("test1"));
        assertThat(result.getDifference(), equalTo(0.0));
    }

    @Test
    public void shouldMapPartsToCommandWithNoCaseSensitivity() {
        // given
        List<String> parts = Arrays.asList("Authme", "REG", "arg1", "arg2");

        // when
        FoundCommandResult result = handler.mapPartsToCommand(parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getChildWithLabel("register", "authme")));
        assertThat(result.getResultStatus(), equalTo(FoundCommandResult.ResultStatus.SUCCESS));
        assertThat(result.getArguments(), contains("arg1", "arg2"));
        assertThat(result.getDifference(), equalTo(0.0));
    }

    @Test
    public void shouldRejectCommandWithTooManyArguments() {
        // given
        List<String> parts = Arrays.asList("authme", "register", "pass123", "pass123", "pass123");

        // when
        FoundCommandResult result = handler.mapPartsToCommand(parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getChildWithLabel("register", "authme")));
        assertThat(result.getResultStatus(), equalTo(FoundCommandResult.ResultStatus.INCORRECT_ARGUMENTS));
        assertThat(result.getDifference(), equalTo(0.0));
    }

    @Test
    public void shouldSuggestCommandWithSimilarLabel() {
        // given
        List<String> parts = Arrays.asList("authme", "reh", "pass123", "pass123");

        // when
        FoundCommandResult result = handler.mapPartsToCommand(parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getChildWithLabel("register", "authme")));
        assertThat(result.getResultStatus(), equalTo(FoundCommandResult.ResultStatus.UNKNOWN_LABEL));
        assertThat(result.getDifference() < 0.75, equalTo(true));
    }

    /** In contrast to the previous test, we test a command request with a very apart label. */
    @Test
    public void shouldSuggestMostSimilarCommand() {
        // given
        List<String> parts = Arrays.asList("authme", "asdfawetawty4asdca");

        // when
        FoundCommandResult result = handler.mapPartsToCommand(parts);

        // then
        assertThat(result.getCommandDescription(), not(nullValue()));
        assertThat(result.getResultStatus(), equalTo(FoundCommandResult.ResultStatus.UNKNOWN_LABEL));
        assertThat(result.getDifference() > 0.75, equalTo(true));
    }

    // ----------
    // Helper methods
    // ----------
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

    private static CommandDescription getChildWithLabel(String childLabel, String parentLabel) {
        CommandDescription parent = getCommandWithLabel(parentLabel, commands);
        return getCommandWithLabel(childLabel, parent.getChildren());
    }

    private static CommandDescription getCommandWithLabel(String label, Collection<CommandDescription> commands) {
        for (CommandDescription child : commands) {
            if (child.getLabels().contains(label)) {
                return child;
            }
        }
        throw new RuntimeException("Could not find command with label '" + label + "'");
    }
}
