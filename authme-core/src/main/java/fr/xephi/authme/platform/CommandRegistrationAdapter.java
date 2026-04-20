package fr.xephi.authme.platform;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandHandler;

import java.util.Collection;

/**
 * Allows a platform module to register commands using platform-specific APIs.
 */
public interface CommandRegistrationAdapter {

    /**
     * Registers commands for the current platform.
     *
     * @param plugin the plugin instance
     * @param commandHandler the shared AuthMe command handler
     * @param commands the base command descriptions
     */
    default void registerCommands(AuthMe plugin, CommandHandler commandHandler,
                                  Collection<CommandDescription> commands) {
        // Default no-op: platforms without a dedicated command API continue using plugin.yml + onCommand.
    }
}
