package fr.xephi.authme.command.executable;

import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandMapper;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.help.HelpProvider;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static fr.xephi.authme.command.FoundResultStatus.INCORRECT_ARGUMENTS;
import static fr.xephi.authme.command.FoundResultStatus.MISSING_BASE_COMMAND;
import static fr.xephi.authme.command.FoundResultStatus.SUCCESS;
import static fr.xephi.authme.command.FoundResultStatus.UNKNOWN_LABEL;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_ALTERNATIVES;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_CHILDREN;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_COMMAND;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_DESCRIPTION;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link HelpCommand}.
 */
@ExtendWith(MockitoExtension.class)
class HelpCommandTest {

    @InjectMocks
    private HelpCommand command;

    @Mock
    private CommandMapper commandMapper;

    @Mock
    private HelpProvider helpProvider;

    @Test
    void shouldHandleMissingBaseCommand() {
        // given
        List<String> arguments = asList("some", "command");
        CommandSender sender = mock(CommandSender.class);
        FoundCommandResult foundCommandResult = new FoundCommandResult(null, null, null, 0.0, MISSING_BASE_COMMAND);
        given(commandMapper.mapPartsToCommand(sender, arguments)).willReturn(foundCommandResult);

        // when
        command.executeCommand(sender, arguments);

        // then
        verify(sender).sendMessage(argThat(containsString("Could not get base command")));
        verifyNoInteractions(helpProvider);
    }

    @Test
    void shouldHandleWrongCommandWithSuggestion() {
        // given
        List<String> arguments = asList("authme", "ragister", "test");
        CommandSender sender = mock(CommandSender.class);
        CommandDescription description = newCommandDescription("authme", "register");
        FoundCommandResult foundCommandResult = new FoundCommandResult(description, asList("authme", "ragister"),
            singletonList("test"), 0.1, UNKNOWN_LABEL);
        given(commandMapper.mapPartsToCommand(sender, arguments)).willReturn(foundCommandResult);

        // when
        command.executeCommand(sender, arguments);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(captor.capture());
        assertThat(removeColors(captor.getValue()), containsString("Assuming /authme register"));
        verify(helpProvider).outputHelp(sender, foundCommandResult, HelpProvider.ALL_OPTIONS);
    }

    @Test
    void shouldHandleWrongCommandWithoutSuggestion() {
        List<String> arguments = asList("authme", "ragister", "test");
        CommandSender sender = mock(CommandSender.class);
        FoundCommandResult foundCommandResult = new FoundCommandResult(null, asList("authme", "ragister"),
            singletonList("test"), 0.4, UNKNOWN_LABEL);
        given(commandMapper.mapPartsToCommand(sender, arguments)).willReturn(foundCommandResult);

        // when
        command.executeCommand(sender, arguments);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(captor.capture());
        assertThat(removeColors(captor.getValue()), containsString("Unknown command"));
        verifyNoInteractions(helpProvider);
    }

    @Test
    void shouldShowChildrenOfBaseCommand() {
        List<String> arguments = singletonList("authme");
        CommandSender sender = mock(CommandSender.class);
        CommandDescription commandDescription = mock(CommandDescription.class);
        given(commandDescription.getLabelCount()).willReturn(1);
        FoundCommandResult foundCommandResult = new FoundCommandResult(commandDescription, singletonList("authme"),
            Collections.emptyList(), 0.0, SUCCESS);
        given(commandMapper.mapPartsToCommand(sender, arguments)).willReturn(foundCommandResult);

        // when
        command.executeCommand(sender, arguments);

        // then
        verify(sender, never()).sendMessage(anyString());
        verify(helpProvider).outputHelp(sender, foundCommandResult,
            SHOW_DESCRIPTION | SHOW_COMMAND | SHOW_CHILDREN | SHOW_ALTERNATIVES);
    }

    @Test
    void shouldShowDetailedHelpForChildCommand() {
        List<String> arguments = asList("authme", "getpos");
        CommandSender sender = mock(CommandSender.class);
        CommandDescription commandDescription = mock(CommandDescription.class);
        given(commandDescription.getLabelCount()).willReturn(2);
        FoundCommandResult foundCommandResult = new FoundCommandResult(commandDescription, asList("authme", "getpos"),
            Collections.emptyList(), 0.0, INCORRECT_ARGUMENTS);
        given(commandMapper.mapPartsToCommand(sender, arguments)).willReturn(foundCommandResult);

        // when
        command.executeCommand(sender, arguments);

        // then
        verify(sender, never()).sendMessage(anyString());
        verify(helpProvider).outputHelp(sender, foundCommandResult, HelpProvider.ALL_OPTIONS);
    }

    private static CommandDescription newCommandDescription(String... labels) {
        CommandDescription parent = null;
        // iterate through the labels backwards so we can set the parent
        for (String label : labels) {
            CommandDescription description = mock(CommandDescription.class);
            given(description.getParent()).willReturn(parent);
            given(description.getLabels()).willReturn(singletonList(label));
            parent = description;
        }
        return parent;
    }

    private static String removeColors(String str) {
        for (ChatColor color : ChatColor.values()) {
            str = str.replace(color.toString(), "");
        }
        return str;
    }
}
