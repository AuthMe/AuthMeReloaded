package fr.xephi.authme.command;

import ch.jalu.injector.Injector;
import ch.jalu.injector.factory.Factory;
import com.google.common.collect.Sets;
import fr.xephi.authme.command.TestCommandsUtil.TestLoginCommand;
import fr.xephi.authme.command.TestCommandsUtil.TestRegisterCommand;
import fr.xephi.authme.command.TestCommandsUtil.TestUnregisterCommand;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.command.CommandSender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static fr.xephi.authme.command.FoundResultStatus.INCORRECT_ARGUMENTS;
import static fr.xephi.authme.command.FoundResultStatus.MISSING_BASE_COMMAND;
import static fr.xephi.authme.command.FoundResultStatus.NO_PERMISSION;
import static fr.xephi.authme.command.FoundResultStatus.SUCCESS;
import static fr.xephi.authme.command.FoundResultStatus.UNKNOWN_LABEL;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link CommandHandler}.
 */
// Justification: It's more readable to use asList() everywhere in the test when we often generated two lists where one
// often consists of only one element, e.g. myMethod(asList("authme"), asList("my", "args"), ...)
@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(MockitoJUnitRunner.class)
public class CommandHandlerTest {

    private CommandHandler handler;

    @Mock
    private Factory<ExecutableCommand> commandFactory;
    @Mock
    private CommandMapper commandMapper;
    @Mock
    private PermissionsManager permissionsManager;
    @Mock
    private Messages messages;
    @Mock
    private HelpProvider helpProvider;

    private Map<Class<? extends ExecutableCommand>, ExecutableCommand> mockedCommands = new HashMap<>();

    @Before
    @SuppressWarnings("unchecked")
    public void initializeCommandMapper() {
        given(commandMapper.getCommandClasses()).willReturn(Sets.newHashSet(
            ExecutableCommand.class, TestLoginCommand.class, TestRegisterCommand.class, TestUnregisterCommand.class));
        setInjectorToMockExecutableCommandClasses();

        handler = new CommandHandler(commandFactory, commandMapper, permissionsManager, messages, helpProvider);
    }

    /**
     * Makes the injector return a mock when {@link Injector#newInstance(Class)} is invoked
     * with (a child of) ExecutableCommand.class. The mocks the injector creates are stored in {@link #mockedCommands}.
     * <p>
     * The {@link CommandMapper} is mocked in {@link #initializeCommandMapper()} to return certain test classes.
     */
    @SuppressWarnings("unchecked")
    private void setInjectorToMockExecutableCommandClasses() {
        given(commandFactory.newInstance(any(Class.class))).willAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Class<?> clazz = invocation.getArgument(0);
                if (ExecutableCommand.class.isAssignableFrom(clazz)) {
                    Class<? extends ExecutableCommand> commandClass = (Class<? extends ExecutableCommand>) clazz;
                    ExecutableCommand mock = mock(commandClass);
                    mockedCommands.put(commandClass, mock);
                    return mock;
                }
                throw new IllegalStateException("Unexpected class '" + clazz.getName()
                    + "': Not a child of ExecutableCommand");
            }
        });
    }


    @Test
    public void shouldCallMappedCommandWithArgs() {
        // given
        String bukkitLabel = "Authme";
        String[] bukkitArgs = {"Login", "myPass"};

        CommandSender sender = mock(CommandSender.class);
        CommandDescription command = mock(CommandDescription.class);
        doReturn(TestLoginCommand.class).when(command).getExecutableCommand();
        given(commandMapper.mapPartsToCommand(any(CommandSender.class), anyList()))
            .willReturn(new FoundCommandResult(command, asList("Authme", "Login"), asList("myPass"), 0.0, SUCCESS));

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        ExecutableCommand executableCommand = mockedCommands.get(TestLoginCommand.class);
        verify(commandMapper).mapPartsToCommand(sender, asList("Authme", "Login", "myPass"));
        verify(executableCommand).executeCommand(sender, asList("myPass"));
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
        given(commandMapper.mapPartsToCommand(any(CommandSender.class), anyList()))
            .willReturn(new FoundCommandResult(command, asList("unreg"), asList("testPlayer"), 0.0, NO_PERMISSION));

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(commandMapper).mapPartsToCommand(sender, asList("unreg", "testPlayer"));
        verify(command, never()).getExecutableCommand();
        verify(messages).send(sender, MessageKey.NO_PERMISSION);
    }

    @Test
    public void shouldNotCallExecutableForWrongArguments() {
        // given
        String bukkitLabel = "unreg";
        String[] bukkitArgs = {"testPlayer"};
        CommandSender sender = mock(CommandSender.class);
        CommandDescription command = mock(CommandDescription.class);
        given(command.getExecutableCommand()).willReturn((Class) TestUnregisterCommand.class);
        given(commandMapper.mapPartsToCommand(any(CommandSender.class), anyList())).willReturn(
            new FoundCommandResult(command, asList("unreg"), asList("testPlayer"), 0.0, INCORRECT_ARGUMENTS));
        given(permissionsManager.hasPermission(sender, command.getPermission())).willReturn(true);

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(commandMapper).mapPartsToCommand(sender, asList("unreg", "testPlayer"));
        verify(sender, atLeastOnce()).sendMessage(argThat(containsString("Incorrect command arguments")));
    }

    @Test
    public void shouldUseCustomMessageUponArgumentMismatch() {
        // given
        String bukkitLabel = "unreg";
        String[] bukkitArgs = {"testPlayer"};
        CommandSender sender = mock(CommandSender.class);
        CommandDescription command = mock(CommandDescription.class);
        given(command.getExecutableCommand()).willReturn((Class) TestUnregisterCommand.class);
        given(mockedCommands.get(TestUnregisterCommand.class).getArgumentsMismatchMessage())
            .willReturn(MessageKey.USAGE_RECOVER_EMAIL);
        given(commandMapper.mapPartsToCommand(any(CommandSender.class), anyList())).willReturn(
            new FoundCommandResult(command, asList("unreg"), asList("testPlayer"), 0.0, INCORRECT_ARGUMENTS));
        given(permissionsManager.hasPermission(sender, command.getPermission())).willReturn(true);

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(commandMapper).mapPartsToCommand(sender, asList("unreg", "testPlayer"));
        verify(messages).send(sender, MessageKey.USAGE_RECOVER_EMAIL);
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    public void shouldNotCallExecutableForWrongArgumentsAndPermissionDenied() {
        // given
        String bukkitLabel = "unreg";
        String[] bukkitArgs = {"testPlayer"};
        CommandSender sender = mock(CommandSender.class);
        CommandDescription command = mock(CommandDescription.class);
        given(commandMapper.mapPartsToCommand(any(CommandSender.class), anyList())).willReturn(
            new FoundCommandResult(command, asList("unreg"), asList("testPlayer"), 0.0, INCORRECT_ARGUMENTS));
        given(permissionsManager.hasPermission(sender, command.getPermission())).willReturn(false);

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(commandMapper).mapPartsToCommand(sender, asList("unreg", "testPlayer"));
        verify(command, never()).getExecutableCommand();
        verify(messages).send(sender, MessageKey.NO_PERMISSION);
    }

    @Test
    public void shouldNotCallExecutableForFailedParsing() {
        // given
        String bukkitLabel = "unreg";
        String[] bukkitArgs = {"testPlayer"};
        CommandSender sender = mock(CommandSender.class);
        CommandDescription command = mock(CommandDescription.class);
        given(commandMapper.mapPartsToCommand(any(CommandSender.class), anyList())).willReturn(
            new FoundCommandResult(command, asList("unreg"), asList("testPlayer"), 0.0, MISSING_BASE_COMMAND));

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(commandMapper).mapPartsToCommand(sender, asList("unreg", "testPlayer"));
        verify(command, never()).getExecutableCommand();
        verify(sender).sendMessage(argThat(containsString("Failed to parse")));
    }

    @Test
    public void shouldNotCallExecutableForUnknownLabelAndHaveSuggestion() {
        // given
        String bukkitLabel = "unreg";
        String[] bukkitArgs = {"testPlayer"};
        CommandSender sender = mock(CommandSender.class);
        CommandDescription command = mock(CommandDescription.class);
        given(command.getLabels()).willReturn(Collections.singletonList("test_cmd"));
        given(commandMapper.mapPartsToCommand(any(CommandSender.class), anyList())).willReturn(
            new FoundCommandResult(command, asList("unreg"), asList("testPlayer"), 0.01, UNKNOWN_LABEL));

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(commandMapper).mapPartsToCommand(sender, asList("unreg", "testPlayer"));
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
        given(commandMapper.mapPartsToCommand(any(CommandSender.class), anyList())).willReturn(
            new FoundCommandResult(command, asList("unreg"), asList("testPlayer"), 1.0, UNKNOWN_LABEL));

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        verify(commandMapper).mapPartsToCommand(sender, asList("unreg", "testPlayer"));
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
        String[] bukkitArgs = {" ", "", "REGISTER", "  ", "testArg", " "};
        CommandSender sender = mock(CommandSender.class);

        CommandDescription command = mock(CommandDescription.class);
        doReturn(TestRegisterCommand.class).when(command).getExecutableCommand();
        given(commandMapper.mapPartsToCommand(eq(sender), anyList()))
            .willReturn(new FoundCommandResult(command, asList("AuthMe", "REGISTER"), asList("testArg"), 0.0, SUCCESS));

        // when
        handler.processCommand(sender, bukkitLabel, bukkitArgs);

        // then
        ExecutableCommand executableCommand = mockedCommands.get(TestRegisterCommand.class);
        verify(commandMapper).mapPartsToCommand(sender, asList("AuthMe", "REGISTER", "testArg"));
        verify(executableCommand).executeCommand(sender, asList("testArg"));
        verify(sender, never()).sendMessage(anyString());
    }

}
