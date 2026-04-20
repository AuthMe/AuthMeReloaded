package fr.xephi.authme.command.executable.email;

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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link EmailBaseCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EmailBaseCommandTest {

    @InjectMocks
    private EmailBaseCommand command;

    @Mock
    private HelpProvider helpProvider;
    @Mock
    private CommandMapper commandMapper;

    @Test
    public void shouldDisplayHelp() {
        // given
        CommandSender sender = mock(CommandSender.class);
        FoundCommandResult result = mock(FoundCommandResult.class);
        given(commandMapper.mapPartsToCommand(eq(sender), anyList())).willReturn(result);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(commandMapper).mapPartsToCommand(sender, Collections.singletonList("email"));
        verify(helpProvider).outputHelp(sender, result, HelpProvider.SHOW_CHILDREN);
    }
}
