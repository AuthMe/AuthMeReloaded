package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.command.CommandMapper;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.help.HelpProvider;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link TotpBaseCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class TotpBaseCommandTest {

    @InjectMocks
    private TotpBaseCommand command;

    @Mock
    private CommandMapper mapper;
    @Mock
    private HelpProvider helpProvider;

    @Test
    public void shouldOutputHelp() {
        // given
        CommandSender sender = mock(CommandSender.class);
        FoundCommandResult mappingResult = mock(FoundCommandResult.class);
        given(mapper.mapPartsToCommand(sender, Collections.singletonList("totp"))).willReturn(mappingResult);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(mapper).mapPartsToCommand(sender, Collections.singletonList("totp"));
        verify(helpProvider).outputHelp(sender, mappingResult, HelpProvider.SHOW_CHILDREN);
    }
}
