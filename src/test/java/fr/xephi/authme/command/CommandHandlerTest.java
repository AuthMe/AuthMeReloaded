package fr.xephi.authme.command;

import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.command.CommandSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static fr.xephi.authme.command.FoundResultStatus.INCORRECT_ARGUMENTS;
import static fr.xephi.authme.command.FoundResultStatus.MISSING_BASE_COMMAND;
import static fr.xephi.authme.command.FoundResultStatus.NO_PERMISSION;
import static fr.xephi.authme.command.FoundResultStatus.SUCCESS;
import static fr.xephi.authme.command.FoundResultStatus.UNKNOWN_LABEL;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link CommandHandler}.
 */
@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
// Justification: It's more readable to use asList() everywhere in the test when we often generated two lists where one
// often consists of only one element, e.g. myMethod(asList("authme"), asList("my", "args"), ...)
public class CommandHandlerTest {

    private CommandHandler handler;
    private CommandService serviceMock;

    @Captor
    private ArgumentCaptor<List<String>> captor;

    @Before
    public void setUpCommandHandler() {
        MockitoAnnotations.initMocks(this);
        serviceMock = mock(CommandService.class);
        handler = new CommandHandler(serviceMock);
    }

    @Test
    public void shouldCallMappedCommandWithArgs() {
        // given
        String bukkitLabel = "Authme";
        String[] bukkitArgs = {"Login", "myPass"};

        CommandSender sender = mock(CommandSender.class);
        ExecutableCommand executableCommand = mock(ExecutableCommand.class);
        CommandDescription command = mock(CommandDescription.class);
        given(command.getExecutableCommand()).willReturn(executableCommand);
        given(serviceMock.mapPartsToCommand(any(CommandSender.class), anyListOf(String.class)))
            .willReturn(new FoundCommandResult(command, asList("Authme", "Login"), asList("myPass"), 0.0, SUCCESS));

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(serviceMock).mapPartsToCommand(eq(sender), captor.capture());
        assertThat(captor.getValue(), contains("Authme", "Login", "myPass"));

        verify(executableCommand).executeCommand(eq(sender), captor.capture(), any(CommandService.class));
        assertThat(captor.getValue(), contains("myPass"));

        // Ensure that no error message was issued to the command sender
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    public void shouldNotCallExecutableCommandIfNoPermission() {
        // given
        String bukkitLabel = "unreg";
        String[] bukkitArgs = {"testPlayer"};
        CommandSender sender = mock(CommandSender.class);
        CommandDescription command = mock(CommandDescription.class);
        given(serviceMock.mapPartsToCommand(any(CommandSender.class), anyListOf(String.class)))
            .willReturn(new FoundCommandResult(command, asList("unreg"), asList("testPlayer"), 0.0, NO_PERMISSION));

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(serviceMock).mapPartsToCommand(eq(sender), captor.capture());
        assertThat(captor.getValue(), contains("unreg", "testPlayer"));

        verify(command, never()).getExecutableCommand();
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(captor.capture());
        assertThat(captor.getValue(), containsString("don't have permission"));
    }

    @Test
    public void shouldNotCallExecutableForWrongArguments() {
        // given
        String bukkitLabel = "unreg";
        String[] bukkitArgs = {"testPlayer"};
        CommandSender sender = mock(CommandSender.class);
        CommandDescription command = mock(CommandDescription.class);
        given(serviceMock.mapPartsToCommand(any(CommandSender.class), anyListOf(String.class))).willReturn(
            new FoundCommandResult(command, asList("unreg"), asList("testPlayer"), 0.0, INCORRECT_ARGUMENTS));
        PermissionsManager permissionsManager = mock(PermissionsManager.class);
        given(permissionsManager.hasPermission(sender, command)).willReturn(true);
        given(serviceMock.getPermissionsManager()).willReturn(permissionsManager);

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(serviceMock).mapPartsToCommand(eq(sender), captor.capture());
        assertThat(captor.getValue(), contains("unreg", "testPlayer"));

        verify(command, never()).getExecutableCommand();
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().get(0), containsString("Incorrect command arguments"));
    }

    @Test
    public void shouldNotCallExecutableForWrongArgumentsAndPermissionDenied() {
        // given
        String bukkitLabel = "unreg";
        String[] bukkitArgs = {"testPlayer"};
        CommandSender sender = mock(CommandSender.class);
        CommandDescription command = mock(CommandDescription.class);
        given(serviceMock.mapPartsToCommand(any(CommandSender.class), anyListOf(String.class))).willReturn(
            new FoundCommandResult(command, asList("unreg"), asList("testPlayer"), 0.0, INCORRECT_ARGUMENTS));
        PermissionsManager permissionsManager = mock(PermissionsManager.class);
        given(permissionsManager.hasPermission(sender, command)).willReturn(false);
        given(serviceMock.getPermissionsManager()).willReturn(permissionsManager);

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(serviceMock).mapPartsToCommand(eq(sender), captor.capture());
        assertThat(captor.getValue(), contains("unreg", "testPlayer"));

        verify(command, never()).getExecutableCommand();
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(captor.capture());
        assertThat(captor.getValue(), containsString("You don't have permission"));
    }

    @Test
    public void shouldNotCallExecutableForFailedParsing() {
        // given
        String bukkitLabel = "unreg";
        String[] bukkitArgs = {"testPlayer"};
        CommandSender sender = mock(CommandSender.class);
        CommandDescription command = mock(CommandDescription.class);
        given(serviceMock.mapPartsToCommand(any(CommandSender.class), anyListOf(String.class))).willReturn(
            new FoundCommandResult(command, asList("unreg"), asList("testPlayer"), 0.0, MISSING_BASE_COMMAND));

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(serviceMock).mapPartsToCommand(eq(sender), captor.capture());
        assertThat(captor.getValue(), contains("unreg", "testPlayer"));

        verify(command, never()).getExecutableCommand();
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(captor.capture());
        assertThat(captor.getValue(), containsString("Failed to parse"));
    }

    @Test
    public void shouldNotCallExecutableForUnknownLabelAndHaveSuggestion() {
        // given
        String bukkitLabel = "unreg";
        String[] bukkitArgs = {"testPlayer"};
        CommandSender sender = mock(CommandSender.class);
        CommandDescription command = mock(CommandDescription.class);
        given(command.getLabels()).willReturn(Collections.singletonList("test_cmd"));
        given(serviceMock.mapPartsToCommand(any(CommandSender.class), anyListOf(String.class))).willReturn(
            new FoundCommandResult(command, asList("unreg"), asList("testPlayer"), 0.01, UNKNOWN_LABEL));

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(serviceMock).mapPartsToCommand(eq(sender), captor.capture());
        assertThat(captor.getValue(), contains("unreg", "testPlayer"));

        verify(command, never()).getExecutableCommand();
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(3)).sendMessage(captor.capture());
        assertThat(captor.getAllValues().get(0), containsString("Unknown command"));
        assertThat(captor.getAllValues().get(1), containsString("Did you mean"));
        assertThat(captor.getAllValues().get(1), containsString("/test_cmd"));
        assertThat(captor.getAllValues().get(2), containsString("Use the command"));
        assertThat(captor.getAllValues().get(2), containsString("to view help"));
    }

    @Test
    public void shouldNotCallExecutableForUnknownLabelAndNotSuggestCommand() {
        // given
        String bukkitLabel = "unreg";
        String[] bukkitArgs = {"testPlayer"};
        CommandSender sender = mock(CommandSender.class);
        CommandDescription command = mock(CommandDescription.class);
        given(command.getLabels()).willReturn(Collections.singletonList("test_cmd"));
        given(serviceMock.mapPartsToCommand(any(CommandSender.class), anyListOf(String.class))).willReturn(
            new FoundCommandResult(command, asList("unreg"), asList("testPlayer"), 1.0, UNKNOWN_LABEL));

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(serviceMock).mapPartsToCommand(eq(sender), captor.capture());
        assertThat(captor.getValue(), contains("unreg", "testPlayer"));

        verify(command, never()).getExecutableCommand();
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(2)).sendMessage(captor.capture());
        assertThat(captor.getAllValues().get(0), containsString("Unknown command"));
        assertThat(captor.getAllValues().get(1), containsString("Use the command"));
        assertThat(captor.getAllValues().get(1), containsString("to view help"));
    }

    @Test
    public void shouldStripWhitespace() {
        // given
        String bukkitLabel = "AuthMe";
        String[] bukkitArgs = {" ", "", "LOGIN", "  ", "testArg", " "};
        CommandSender sender = mock(CommandSender.class);

        ExecutableCommand executableCommand = mock(ExecutableCommand.class);
        CommandDescription command = mock(CommandDescription.class);
        given(command.getExecutableCommand()).willReturn(executableCommand);
        given(serviceMock.mapPartsToCommand(eq(sender), anyListOf(String.class)))
            .willReturn(new FoundCommandResult(command, asList("AuthMe", "LOGIN"), asList("testArg"), 0.0, SUCCESS));

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(serviceMock).mapPartsToCommand(eq(sender), captor.capture());
        assertThat(captor.getValue(), contains("AuthMe", "LOGIN", "testArg"));

        verify(command.getExecutableCommand()).executeCommand(eq(sender), captor.capture(), eq(serviceMock));
        assertThat(captor.getValue(), contains("testArg"));

        verify(sender, never()).sendMessage(anyString());
    }

}
