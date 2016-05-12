package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.AuthMeServiceInitializer;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * The reload command.
 */
public class ReloadCommand implements ExecutableCommand {

    @Inject
    private AuthMe plugin;

    @Inject
    private AuthMeServiceInitializer initializer;

    @Inject
    private NewSetting settings;

    @Inject
    private DataSource dataSource;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        try {
            settings.reload();
            // We do not change database type for consistency issues, but we'll output a note in the logs
            if (!settings.getProperty(DatabaseSettings.BACKEND).equals(dataSource.getType())) {
                ConsoleLogger.info("Note: cannot change database type during /authme reload");
                sender.sendMessage("Note: cannot change database type during /authme reload");
            }
            initializer.performReloadOnServices();
            commandService.send(sender, MessageKey.CONFIG_RELOAD_SUCCESS);
        } catch (Exception e) {
            sender.sendMessage("Error occurred during reload of AuthMe: aborting");
            ConsoleLogger.logException("Aborting! Encountered exception during reload of AuthMe:", e);
            plugin.stopOrUnload();
        }
    }
}
