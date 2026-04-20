package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.service.BackupService;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link BackupCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BackupCommandTest {

    @InjectMocks
    private BackupCommand command;

    @Mock
    private BackupService backupService;

    @Test
    public void shouldStartBackup() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(backupService).doBackup(BackupService.BackupCause.COMMAND, sender);
    }
}
