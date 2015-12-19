package fr.xephi.authme.command;

import fr.xephi.authme.command.executable.HelpCommand;
import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import org.bukkit.command.CommandSender;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link CommandHandler}.
 */
public class CommandHandlerTest {

    private static Set<CommandDescription> commands;
    private static CommandHandler handler;
    private static PermissionsManager permissionsManagerMock;

    @BeforeClass
    public static void setUpCommandHandler() {
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

        commands = newHashSet(authMeBase, emailBase, unregisterBase);
    }

    @Before
    public void setUpMocks() {
        permissionsManagerMock = mock(PermissionsManager.class);
        handler = new CommandHandler(commands, permissionsManagerMock);
    }

    // -----------
    // mapPartsToCommand() tests
    // -----------
    @Test
    public void shouldMapPartsToLoginChildCommand() {
        // given
        List<String> parts = Arrays.asList("authme", "login", "test1");

        // when
        FoundCommandResult result = handler.mapPartsToCommand(parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getChildWithLabel("login", "authme")));
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.SUCCESS));
        assertThat(result.getArguments(), contains("test1"));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getLabels(), equalTo(parts.subList(0, 2)));
        assertThat(result.getArguments(), contains(parts.get(2)));
    }

    @Test
    public void shouldMapPartsToCommandWithNoCaseSensitivity() {
        // given
        List<String> parts = Arrays.asList("Authme", "REG", "arg1", "arg2");

        // when
        FoundCommandResult result = handler.mapPartsToCommand(parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getChildWithLabel("register", "authme")));
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.SUCCESS));
        assertThat(result.getLabels(), equalTo(parts.subList(0, 2)));
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
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.INCORRECT_ARGUMENTS));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getLabels(), equalTo(parts.subList(0, 2)));
        assertThat(result.getArguments(), equalTo(parts.subList(2, 5)));
    }

    @Test
    public void shouldRejectCommandWithTooFewArguments() {
        // given
        List<String> parts = Arrays.asList("authme", "Reg");

        // when
        FoundCommandResult result = handler.mapPartsToCommand(parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getChildWithLabel("register", "authme")));
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.INCORRECT_ARGUMENTS));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getLabels(), equalTo(parts));
        assertThat(result.getArguments(), empty());
    }

    @Test
    public void shouldSuggestCommandWithSimilarLabel() {
        // given
        List<String> parts = Arrays.asList("authme", "reh", "pass123", "pass123");

        // when
        FoundCommandResult result = handler.mapPartsToCommand(parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getChildWithLabel("register", "authme")));
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.UNKNOWN_LABEL));
        assertThat(result.getDifference() < 0.75, equalTo(true));
        assertThat(result.getLabels(), equalTo(parts.subList(0, 2)));
        assertThat(result.getArguments(), contains("pass123", "pass123"));
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
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.UNKNOWN_LABEL));
        assertThat(result.getDifference() > 0.75, equalTo(true));
        assertThat(result.getLabels(), equalTo(parts));
        assertThat(result.getArguments(), empty());
    }

    @Test
    public void shouldHandleBaseWithWrongArguments() {
        // given
        List<String> parts = singletonList("unregister");

        // when
        FoundCommandResult result = handler.mapPartsToCommand(parts);

        // then
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.INCORRECT_ARGUMENTS));
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel("unregister", commands)));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getArguments(), empty());
        assertThat(result.getLabels(), equalTo(parts));
    }

    @Test
    public void shouldHandleUnknownBase() {
        // given
        List<String> parts = asList("bogus", "label1", "arg1");

        // when
        FoundCommandResult result = handler.mapPartsToCommand(parts);

        // then
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.MISSING_BASE_COMMAND));
        assertThat(result.getCommandDescription(), nullValue());
    }

    @Test
    public void shouldHandleNullInput() {
        // given / when
        FoundCommandResult result = handler.mapPartsToCommand(null);

        // then
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.MISSING_BASE_COMMAND));
        assertThat(result.getCommandDescription(), nullValue());
    }

    @Test
    public void shouldMapToBaseWithProperArguments() {
        // given
        List<String> parts = asList("Unreg", "player1");

        // when
        FoundCommandResult result = handler.mapPartsToCommand(parts);

        // then
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.SUCCESS));
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel("unregister", commands)));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getArguments(), contains("player1"));
        assertThat(result.getLabels(), contains("Unreg"));
    }

    @Test
    public void shouldReturnChildlessBaseCommandWithArgCountError() {
        // given
        List<String> parts = asList("unregistER", "player1", "wrongArg");

        // when
        FoundCommandResult result = handler.mapPartsToCommand(parts);

        // then
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.INCORRECT_ARGUMENTS));
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel("unregister", commands)));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getArguments(), contains("player1", "wrongArg"));
        assertThat(result.getLabels(), contains("unregistER"));
    }

    // ----------
    // processCommand() tests
    // ----------
    @Test
    public void shouldCallMappedCommandWithArgs() {
        // given
        String bukkitLabel = "Authme";
        String[] bukkitArgs = {"Login", "myPass"};
        CommandSender sender = mock(CommandSender.class);

        CommandDescription command = getChildWithLabel("login", "authme");
        given(permissionsManagerMock.hasPermission(sender, command)).willReturn(true);

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        ArgumentCaptor<List> argsCaptor = ArgumentCaptor.forClass(List.class);
        verify(command.getExecutableCommand()).executeCommand(eq(sender), argsCaptor.capture());
        List<String> argument = argsCaptor.getValue();
        assertThat(argument, contains("myPass"));
        // Ensure that no error message was issued to the command sender
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    public void shouldNotCallExecutableCommandIfNoPermission() {
        // given
        String bukkitLabel = "unreg";
        String[] bukkitArgs = {"testPlayer"};
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManagerMock.hasPermission(any(CommandSender.class), any(CommandDescription.class)))
            .willReturn(false);

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        CommandDescription command = getCommandWithLabel("unregister", commands);
        verify(permissionsManagerMock).hasPermission(sender, command);
        verify(command.getExecutableCommand(), never())
            .executeCommand(any(CommandSender.class), anyListOf(String.class));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(messageCaptor.capture());
        String message = messageCaptor.getValue();
        assertThat(message, stringContainsInOrder("You don't have permission"));
    }

    @Test
    public void shouldStripWhitespace() {
        // given
        String bukkitLabel = "AuthMe";
        String[] bukkitArgs = {" ", "", "LOGIN", "  ", "testArg", " "};
        CommandSender sender = mock(CommandSender.class);

        CommandDescription command = getChildWithLabel("login", "authme");
        given(permissionsManagerMock.hasPermission(sender, command)).willReturn(true);

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        ArgumentCaptor<List> argsCaptor = ArgumentCaptor.forClass(List.class);
        verify(command.getExecutableCommand()).executeCommand(eq(sender), argsCaptor.capture());
        List<String> arguments = argsCaptor.getValue();
        assertThat(arguments, contains("testArg"));
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    public void shouldPassCommandPathAsArgumentsToHelpCommand() {
        // given
        String bukkitLabel = "email";
        String[] bukkitArgs = {"helptest", "arg1"};
        CommandSender sender = mock(CommandSender.class);

        CommandDescription command = getChildWithLabel("helptest", "email");
        given(permissionsManagerMock.hasPermission(sender, command)).willReturn(true);

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        ArgumentCaptor<List> argsCaptor = ArgumentCaptor.forClass(List.class);
        verify(command.getExecutableCommand()).executeCommand(eq(sender), argsCaptor.capture());
        List<String> arguments = argsCaptor.getValue();
        assertThat(arguments, contains("email", "arg1"));
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
            if (child.hasLabel(label)) {
                return child;
            }
        }
        throw new RuntimeException("Could not find command with label '" + label + "'");
    }
}
