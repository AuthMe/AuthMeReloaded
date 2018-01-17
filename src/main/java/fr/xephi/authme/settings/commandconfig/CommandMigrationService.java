package fr.xephi.authme.settings.commandconfig;

import ch.jalu.configme.migration.MigrationService;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.resource.PropertyResource;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Migrates the commands from their old location, in config.yml, to the dedicated commands configuration file.
 */
class CommandMigrationService implements MigrationService {

    /** List of all properties in {@link CommandConfig}. */
    @VisibleForTesting
    static final List<String> COMMAND_CONFIG_PROPERTIES = ImmutableList.of(
        "onJoin", "onLogin", "onSessionLogin", "onFirstLogin", "onRegister", "onUnregister", "onLogout");

    CommandMigrationService() {
    }

    @Override
    public boolean checkAndMigrate(PropertyResource resource, List<Property<?>> properties) {
        final CommandConfig commandConfig = CommandSettingsHolder.COMMANDS.getValue(resource);
        if (isFileEmpty(resource)) {
            resource.setValue("", commandConfig);
            return true;
        }
        return false;
    }

    private static boolean isFileEmpty(PropertyResource resource) {
        return COMMAND_CONFIG_PROPERTIES.stream().anyMatch(property -> resource.getObject(property) == null);
    }
}
