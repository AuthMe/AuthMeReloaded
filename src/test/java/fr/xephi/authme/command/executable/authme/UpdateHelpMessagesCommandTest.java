package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.help.HelpMessagesService;
import fr.xephi.authme.service.HelpTranslationGenerator;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link UpdateHelpMessagesCommand}.
 */
@ExtendWith(MockitoExtension.class)
class UpdateHelpMessagesCommandTest {

    @InjectMocks
    private UpdateHelpMessagesCommand command;

    @Mock
    private HelpTranslationGenerator helpTranslationGenerator;
    @Mock
    private HelpMessagesService helpMessagesService;

    @BeforeAll
    static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    void shouldUpdateHelpMessage() throws IOException {
        // given
        File updatedFile = new File("some/path/help_xx.yml");
        given(helpTranslationGenerator.updateHelpFile()).willReturn(updatedFile);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(helpMessagesService).reloadMessagesFile();
        verify(sender).sendMessage("Successfully updated the help file 'help_xx.yml'");
    }

    @Test
    void shouldCatchAndReportException() throws IOException {
        // given
        given(helpTranslationGenerator.updateHelpFile()).willThrow(new IOException("Couldn't do the thing"));
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(sender).sendMessage("Could not update help file: Couldn't do the thing");
        verifyNoInteractions(helpMessagesService);
    }
}
