package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.service.BackupService;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link BackupCommand}.
 */
@ExtendWith(MockitoExtension.class)
class BackupCommandTest {

    @InjectMocks
    private BackupCommand command;

    @Mock
    private BackupService backupService;

    @Test
    void shouldStartBackup() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(backupService).doBackup(BackupService.BackupCause.COMMAND, sender);
    }
}
