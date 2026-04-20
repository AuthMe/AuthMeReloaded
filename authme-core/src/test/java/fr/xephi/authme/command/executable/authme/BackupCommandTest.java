package fr.xephi.authme.command.executable.authme;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import fr.xephi.authme.service.BackupService;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link BackupCommand}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
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


