package fr.xephi.authme.command;

import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.command.CommandSender;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static fr.xephi.authme.command.TestCommandsUtil.getCommandWithLabel;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link CommandMapper}.
 */
public class CommandMapperTest {

    private static Set<CommandDescription> commands;
    private static CommandMapper mapper;
    private static PermissionsManager permissionsManagerMock;

    @BeforeClass
    public static void setUpCommandHandler() {
        commands = TestCommandsUtil.generateCommands();
    }

    @Before
    public void setUpMocks() {
        permissionsManagerMock = mock(PermissionsManager.class);
        mapper = new CommandMapper(commands, permissionsManagerMock);
    }

    // -----------
    // mapPartsToCommand() tests
    // -----------
    @Test
    public void shouldMapPartsToLoginChildCommand() {
        // given
        List<String> parts = Arrays.asList("authme", "login", "test1");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManagerMock.hasPermission(eq(sender), any(CommandDescription.class))).willReturn(true);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "authme", "login")));
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
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManagerMock.hasPermission(eq(sender), any(CommandDescription.class))).willReturn(true);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "authme", "register")));
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.SUCCESS));
        assertThat(result.getLabels(), equalTo(parts.subList(0, 2)));
        assertThat(result.getArguments(), contains("arg1", "arg2"));
        assertThat(result.getDifference(), equalTo(0.0));
    }

    @Test
    public void shouldRejectCommandWithTooManyArguments() {
        // given
        List<String> parts = Arrays.asList("authme", "register", "pass123", "pass123", "pass123");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManagerMock.hasPermission(eq(sender), any(CommandDescription.class))).willReturn(true);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "authme", "register")));
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.INCORRECT_ARGUMENTS));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getLabels(), equalTo(parts.subList(0, 2)));
        assertThat(result.getArguments(), equalTo(parts.subList(2, 5)));
    }

    @Test
    public void shouldRejectCommandWithTooFewArguments() {
        // given
        List<String> parts = Arrays.asList("authme", "Reg");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManagerMock.hasPermission(eq(sender), any(CommandDescription.class))).willReturn(true);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "authme", "register")));
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.INCORRECT_ARGUMENTS));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getLabels(), equalTo(parts));
        assertThat(result.getArguments(), empty());
    }

    @Test
    public void shouldSuggestCommandWithSimilarLabel() {
        // given
        List<String> parts = Arrays.asList("authme", "reh", "pass123", "pass123");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManagerMock.hasPermission(eq(sender), any(CommandDescription.class))).willReturn(true);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "authme", "register")));
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
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManagerMock.hasPermission(eq(sender), any(CommandDescription.class))).willReturn(true);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

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
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManagerMock.hasPermission(eq(sender), any(CommandDescription.class))).willReturn(true);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.INCORRECT_ARGUMENTS));
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "unregister")));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getArguments(), empty());
        assertThat(result.getLabels(), equalTo(parts));
    }

    @Test
    public void shouldHandleUnknownBase() {
        // given
        List<String> parts = asList("bogus", "label1", "arg1");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManagerMock.hasPermission(eq(sender), any(CommandDescription.class))).willReturn(true);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.MISSING_BASE_COMMAND));
        assertThat(result.getCommandDescription(), nullValue());
    }

    @Test
    public void shouldHandleNullInput() {
        // given / when
        FoundCommandResult result = mapper.mapPartsToCommand(mock(CommandSender.class), null);

        // then
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.MISSING_BASE_COMMAND));
        assertThat(result.getCommandDescription(), nullValue());
    }

    @Test
    public void shouldMapToBaseWithProperArguments() {
        // given
        List<String> parts = asList("Unreg", "player1");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManagerMock.hasPermission(eq(sender), any(CommandDescription.class))).willReturn(true);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.SUCCESS));
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "unregister")));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getArguments(), contains("player1"));
        assertThat(result.getLabels(), contains("Unreg"));
    }

    @Test
    public void shouldReturnChildlessBaseCommandWithArgCountError() {
        // given
        List<String> parts = asList("unregistER", "player1", "wrongArg");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManagerMock.hasPermission(eq(sender), any(CommandDescription.class))).willReturn(true);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.INCORRECT_ARGUMENTS));
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "unregister")));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getArguments(), contains("player1", "wrongArg"));
        assertThat(result.getLabels(), contains("unregistER"));
    }

    @Test
    public void shouldPassCommandPathAsArgumentsToHelpCommand() {
        // given
        List<String> parts = asList("email", "helptest", "arg1");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManagerMock.hasPermission(eq(sender), any(CommandDescription.class))).willReturn(true);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.SUCCESS));
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "email", "helptest")));
        assertThat(result.getLabels(), contains("email", "helptest"));
        assertThat(result.getArguments(), contains("email", "arg1"));
        assertThat(result.getDifference(), equalTo(0.0));
    }

    @Test
    public void shouldRecognizeMissingPermissionForCommand() {
        // given
        List<String> parts = Arrays.asList("authme", "login", "test1");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManagerMock.hasPermission(eq(sender), any(CommandDescription.class))).willReturn(false);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "authme", "login")));
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.NO_PERMISSION));
        assertThat(result.getArguments(), contains("test1"));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getLabels(), equalTo(parts.subList(0, 2)));
        assertThat(result.getArguments(), contains(parts.get(2)));
    }

}
