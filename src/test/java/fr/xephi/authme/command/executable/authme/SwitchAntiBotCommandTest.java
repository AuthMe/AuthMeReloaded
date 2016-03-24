package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AntiBot;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.help.HelpProvider;
import org.bukkit.command.CommandSender;
import org.junit.Test;

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
public class SwitchAntiBotCommandTest {

    @Test
    public void shouldReturnAntiBotState() {
        // given
        AntiBot antiBot = mock(AntiBot.class);
        given(antiBot.getAntiBotStatus()).willReturn(AntiBot.AntiBotStatus.ACTIVE);
        CommandService service = mock(CommandService.class);
        given(service.getAntiBot()).willReturn(antiBot);
        CommandSender sender = mock(CommandSender.class);
        ExecutableCommand command = new SwitchAntiBotCommand();

        // when
        command.executeCommand(sender, Collections.<String>emptyList(), service);

        // then
        verify(sender).sendMessage(argThat(containsString("status: ACTIVE")));
    }

    @Test
    public void shouldActivateAntiBot() {
        // given
        AntiBot antiBot = mock(AntiBot.class);
        CommandService service = mock(CommandService.class);
        given(service.getAntiBot()).willReturn(antiBot);
        CommandSender sender = mock(CommandSender.class);
        ExecutableCommand command = new SwitchAntiBotCommand();

        // when
        command.executeCommand(sender, Collections.singletonList("on"), service);

        // then
        verify(antiBot).overrideAntiBotStatus(true);
        verify(sender).sendMessage(argThat(containsString("enabled")));
    }

    @Test
    public void shouldDeactivateAntiBot() {
        // given
        AntiBot antiBot = mock(AntiBot.class);
        CommandService service = mock(CommandService.class);
        given(service.getAntiBot()).willReturn(antiBot);
        CommandSender sender = mock(CommandSender.class);
        ExecutableCommand command = new SwitchAntiBotCommand();

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

        AntiBot antiBot = mock(AntiBot.class);
        FoundCommandResult foundCommandResult = mock(FoundCommandResult.class);
        CommandService service = mock(CommandService.class);
        given(service.getAntiBot()).willReturn(antiBot);
        given(service.mapPartsToCommand(sender, asList("authme", "antibot"))).willReturn(foundCommandResult);

        ExecutableCommand command = new SwitchAntiBotCommand();

        // when
        command.executeCommand(sender, Collections.singletonList("wrong"), service);

        // then
        verify(antiBot, never()).overrideAntiBotStatus(anyBoolean());
        verify(sender).sendMessage(argThat(containsString("Invalid")));
        verify(service).outputHelp(sender, foundCommandResult, HelpProvider.SHOW_ARGUMENTS);
    }
}
