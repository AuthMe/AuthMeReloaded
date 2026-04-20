package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.service.BackupService;
import fr.xephi.authme.service.BackupService.BackupCause;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * Command to perform a backup.
 */
public class BackupCommand implements ExecutableCommand {

    @Inject
    private BackupService backupService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        backupService.doBackup(BackupCause.COMMAND, sender);
    }
}
