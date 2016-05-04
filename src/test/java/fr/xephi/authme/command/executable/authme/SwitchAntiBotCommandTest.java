package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AntiBot;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.help.HelpProvider;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link SwitchAntiBotCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SwitchAntiBotCommandTest {

    @InjectMocks
    private SwitchAntiBotCommand command;

    @Mock
    private AntiBot antiBot;

    @Mock
    private CommandService service;

    @Test
    public void shouldReturnAntiBotState() {
        // given
        given(antiBot.getAntiBotStatus()).willReturn(AntiBot.AntiBotStatus.ACTIVE);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.<String>emptyList(), service);

        // then
        verify(sender).sendMessage(argThat(containsString("status: ACTIVE")));
    }

    @Test
    public void shouldActivateAntiBot() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList("on"), service);

        // then
        verify(antiBot).overrideAntiBotStatus(true);
        verify(sender).sendMessage(argThat(containsString("enabled")));
    }

    @Test
    public void shouldDeactivateAntiBot() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList("Off"), service);

        // then
        verify(antiBot).overrideAntiBotStatus(false);
        verify(sender).sendMessage(argThat(containsString("disabled")));
    }

    @Test
    public void shouldShowHelpForUnknownState() {
        // given
        CommandSender sender = mock(CommandSender.class);
        FoundCommandResult foundCommandResult = mock(FoundCommandResult.class);
        given(service.mapPartsToCommand(sender, asList("authme", "antibot"))).willReturn(foundCommandResult);

        // when
        command.executeCommand(sender, Collections.singletonList("wrong"), service);

        // then
        verify(antiBot, never()).overrideAntiBotStatus(anyBoolean());
        verify(sender).sendMessage(argThat(containsString("Invalid")));
        verify(service).outputHelp(sender, foundCommandResult, HelpProvider.SHOW_ARGUMENTS);
    }
}
