package fr.xephi.authme.command.executable.authme;

import ch.jalu.injector.Injector;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

/**
 * The reload command.
 */
public class ReloadCommand implements ExecutableCommand {

    @Inject
    private AuthMe plugin;

    @Inject
    private Injector injector;

    @Inject
    private Settings settings;

    @Inject
    private DataSource dataSource;

    @Inject
    private CommandService commandService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        try {
            settings.reload();
            ConsoleLogger.setLoggingOptions(settings);
            // We do not change database type for consistency issues, but we'll output a note in the logs
            if (!settings.getProperty(DatabaseSettings.BACKEND).equals(dataSource.getType())) {
                ConsoleLogger.info("Note: cannot change database type during /authme reload");
                sender.sendMessage("Note: cannot change database type during /authme reload");
            }
            performReloadOnServices();
            commandService.send(sender, MessageKey.CONFIG_RELOAD_SUCCESS);
        } catch (Exception e) {
            sender.sendMessage("Error occurred during reload of AuthMe: aborting");
            ConsoleLogger.logException("Aborting! Encountered exception during reload of AuthMe:", e);
            plugin.stopOrUnload();
        }
    }

    private void performReloadOnServices() {
        Collection<Reloadable> reloadables = injector.retrieveAllOfType(Reloadable.class);
        for (Reloadable reloadable : reloadables) {
            reloadable.reload();
        }

        Collection<SettingsDependent> settingsDependents = injector.retrieveAllOfType(SettingsDependent.class);
        for (SettingsDependent dependent : settingsDependents) {
            dependent.reload(settings);
        }
    }
}
