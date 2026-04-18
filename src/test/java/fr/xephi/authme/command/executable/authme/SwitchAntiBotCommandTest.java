package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandMapper;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.AntiBotService;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
    private AntiBotService antiBot;

    @Mock
    private CommandMapper commandMapper;

    @Mock
    private HelpProvider helpProvider;

    @Mock
    private Messages messages;

    @Test
    public void shouldReturnAntiBotState() {
        // given
        given(antiBot.getAntiBotStatus()).willReturn(AntiBotService.AntiBotStatus.ACTIVE);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(messages).send(eq(sender), eq(MessageKey.ANTIBOT_STATUS), eq("ACTIVE"));
    }

    @Test
    public void shouldActivateAntiBot() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList("on"));

        // then
        verify(antiBot).overrideAntiBotStatus(true);
        verify(messages).send(sender, MessageKey.ANTIBOT_OVERRIDE_ENABLED);
    }

    @Test
    public void shouldDeactivateAntiBot() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList("Off"));

        // then
        verify(antiBot).overrideAntiBotStatus(false);
        verify(messages).send(sender, MessageKey.ANTIBOT_OVERRIDE_DISABLED);
    }

    @Test
    public void shouldShowHelpForUnknownState() {
        // given
        CommandSender sender = mock(CommandSender.class);
        FoundCommandResult foundCommandResult = mock(FoundCommandResult.class);
        given(commandMapper.mapPartsToCommand(sender, asList("authme", "antibot"))).willReturn(foundCommandResult);

        // when
        command.executeCommand(sender, Collections.singletonList("wrong"));

        // then
        verify(antiBot, never()).overrideAntiBotStatus(anyBoolean());
        verify(messages).send(sender, MessageKey.ANTIBOT_INVALID_MODE);
        verify(helpProvider).outputHelp(sender, foundCommandResult, HelpProvider.SHOW_ARGUMENTS);
        verify(messages).send(eq(sender), eq(MessageKey.COMMAND_DETAILED_HELP), eq("authme help antibot"));
    }
}
