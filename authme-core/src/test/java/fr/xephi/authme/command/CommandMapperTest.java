package fr.xephi.authme.command;

import fr.xephi.authme.command.TestCommandsUtil.TestLoginCommand;
import fr.xephi.authme.command.TestCommandsUtil.TestRegisterCommand;
import fr.xephi.authme.command.TestCommandsUtil.TestUnregisterCommand;
import fr.xephi.authme.command.executable.HelpCommand;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static fr.xephi.authme.command.TestCommandsUtil.getCommandWithLabel;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link CommandMapper}.
 */
@ExtendWith(MockitoExtension.class)
class CommandMapperTest {

    private static List<CommandDescription> commands;

    private CommandMapper mapper;

    @Mock
    private PermissionsManager permissionsManager;

    @Mock
    private CommandInitializer commandInitializer;

    @BeforeAll
    static void setUpCommandHandler() {
        commands = TestCommandsUtil.generateCommands();
    }

    @BeforeEach
    void setUpMocksAndMapper() {
        given(commandInitializer.getCommands()).willReturn(commands);
        mapper = new CommandMapper(commandInitializer, permissionsManager);
    }

    // -----------
    // mapPartsToCommand() tests
    // -----------
    @Test
    void shouldMapPartsToLoginChildCommand() {
        // given
        List<String> parts = asList("authme", "login", "test1");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManager.hasPermission(eq(sender), any(PermissionNode.class))).willReturn(true);

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
    void shouldMapPartsToCommandWithNoCaseSensitivity() {
        // given
        List<String> parts = asList("Authme", "REG", "arg1", "arg2");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManager.hasPermission(eq(sender), any(PermissionNode.class))).willReturn(true);

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
    void shouldRejectCommandWithTooManyArguments() {
        // given
        List<String> parts = asList("authme", "register", "pass123", "pass123", "pass123");
        CommandSender sender = mock(CommandSender.class);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        verifyNoInteractions(permissionsManager);
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "authme", "register")));
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.INCORRECT_ARGUMENTS));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getLabels(), equalTo(parts.subList(0, 2)));
        assertThat(result.getArguments(), equalTo(parts.subList(2, 5)));
    }

    @Test
    void shouldRejectCommandWithTooFewArguments() {
        // given
        List<String> parts = asList("authme", "Reg");
        CommandSender sender = mock(CommandSender.class);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        verifyNoInteractions(permissionsManager);
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "authme", "register")));
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.INCORRECT_ARGUMENTS));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getLabels(), equalTo(parts));
        assertThat(result.getArguments(), empty());
    }

    @Test
    void shouldSuggestCommandWithSimilarLabel() {
        // given
        List<String> parts = asList("authme", "reh", "pass123", "pass123");
        CommandSender sender = mock(CommandSender.class);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        verifyNoInteractions(permissionsManager);
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "authme", "register")));
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.UNKNOWN_LABEL));
        assertThat(result.getDifference() < 0.75, equalTo(true));
        assertThat(result.getLabels(), equalTo(parts.subList(0, 2)));
        assertThat(result.getArguments(), contains("pass123", "pass123"));
    }

    /** In contrast to the previous test, we test a command request with a very apart label. */
    @Test
    void shouldSuggestMostSimilarCommand() {
        // given
        List<String> parts = asList("authme", "asdfawetawty4asdca");
        CommandSender sender = mock(CommandSender.class);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        verifyNoInteractions(permissionsManager);
        assertThat(result.getCommandDescription(), not(nullValue()));
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.UNKNOWN_LABEL));
        assertThat(result.getDifference() > 0.75, equalTo(true));
        assertThat(result.getLabels(), equalTo(parts));
        assertThat(result.getArguments(), empty());
    }

    @Test
    void shouldHandleBaseWithWrongArguments() {
        // given
        List<String> parts = singletonList("unregister");
        CommandSender sender = mock(CommandSender.class);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        verifyNoInteractions(permissionsManager);
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.INCORRECT_ARGUMENTS));
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "unregister")));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getArguments(), empty());
        assertThat(result.getLabels(), equalTo(parts));
    }

    @Test
    void shouldHandleUnknownBase() {
        // given
        List<String> parts = asList("bogus", "label1", "arg1");
        CommandSender sender = mock(CommandSender.class);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        verifyNoInteractions(permissionsManager);
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.MISSING_BASE_COMMAND));
        assertThat(result.getCommandDescription(), nullValue());
    }

    @Test
    void shouldHandleNullInput() {
        // given / when
        FoundCommandResult result = mapper.mapPartsToCommand(mock(CommandSender.class), null);

        // then
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.MISSING_BASE_COMMAND));
        assertThat(result.getCommandDescription(), nullValue());
    }

    @Test
    void shouldMapToBaseWithProperArguments() {
        // given
        List<String> parts = asList("Unreg", "player1");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManager.hasPermission(eq(sender), any(PermissionNode.class))).willReturn(true);

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
    void shouldReturnChildlessBaseCommandWithArgCountError() {
        // given
        List<String> parts = asList("unregistER", "player1", "wrongArg");
        CommandSender sender = mock(CommandSender.class);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        verifyNoInteractions(permissionsManager);
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.INCORRECT_ARGUMENTS));
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "unregister")));
        assertThat(result.getDifference(), equalTo(0.0));
        assertThat(result.getArguments(), contains("player1", "wrongArg"));
        assertThat(result.getLabels(), contains("unregistER"));
    }

    @Test
    void shouldPassCommandPathAsArgumentsToHelpCommand() {
        // given
        List<String> parts = asList("email", "helptest", "arg1");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManager.hasPermission(eq(sender), isNull())).willReturn(true);

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
    void shouldRecognizeMissingPermissionForCommand() {
        // given
        List<String> parts = asList("authme", "login", "test1");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManager.hasPermission(eq(sender), any(PermissionNode.class))).willReturn(false);

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

    @Test
    void shouldSupportAuthMePrefix() {
        // given
        List<String> parts = asList("authme:unregister", "Betty");
        CommandSender sender = mock(CommandSender.class);
        given(permissionsManager.hasPermission(eq(sender), any(PermissionNode.class))).willReturn(true);

        // when
        FoundCommandResult result = mapper.mapPartsToCommand(sender, parts);

        // then
        assertThat(result.getResultStatus(), equalTo(FoundResultStatus.SUCCESS));
        assertThat(result.getCommandDescription(), equalTo(getCommandWithLabel(commands, "unregister")));
    }

    @Test
    void shouldReturnExecutableCommandClasses() {
        // given / when
        Set<Class<? extends ExecutableCommand>> commandClasses = mapper.getCommandClasses();

        // then
        assertThat(commandClasses, containsInAnyOrder(ExecutableCommand.class, HelpCommand.class,
            TestLoginCommand.class, TestRegisterCommand.class, TestUnregisterCommand.class));
    }

}
