package fr.xephi.authme.settings.commandconfig;

import ch.jalu.configme.migration.MigrationService;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.resource.PropertyResource;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import fr.xephi.authme.settings.SettingsMigrationService;
import fr.xephi.authme.util.RandomStringUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Migrates the commands from their old location, in config.yml, to the dedicated commands configuration file.
 */
class CommandMigrationService implements MigrationService {

    /** List of all properties in {@link CommandConfig}. */
    @VisibleForTesting
    static final List<String> COMMAND_CONFIG_PROPERTIES = ImmutableList.of(
        "onJoin", "onLogin", "onSessionLogin", "onFirstLogin", "onRegister", "onUnregister", "onLogout");

    @Inject
    private SettingsMigrationService settingsMigrationService;

    CommandMigrationService() {
    }

    @Override
    public boolean checkAndMigrate(PropertyResource resource, List<Property<?>> properties) {
        final CommandConfig commandConfig = CommandSettingsHolder.COMMANDS.getValue(resource);
        if (moveOtherAccountsConfig(commandConfig) || isFileEmpty(resource)) {
            resource.setValue("", commandConfig);
            return true;
        }
        return false;
    }

    private boolean moveOtherAccountsConfig(CommandConfig commandConfig) {
        if (settingsMigrationService.hasOldOtherAccountsCommand()) {
            OnLoginCommand command = new OnLoginCommand(
                replaceOldPlaceholdersWithNew(settingsMigrationService.getOldOtherAccountsCommand()), Executor.CONSOLE);
            command.setIfNumberOfAccountsAtLeast(
                Optional.of(settingsMigrationService.getOldOtherAccountsCommandThreshold()));

            Map<String, OnLoginCommand> onLoginCommands = commandConfig.getOnLogin();
            onLoginCommands.put(RandomStringUtils.generate(10), command);
            return true;
        }
        return false;
    }

    private static String replaceOldPlaceholdersWithNew(String oldOtherAccountsCommand) {
        return oldOtherAccountsCommand
            .replace("%playername%", "%p")
            .replace("%playerip%", "%ip");
    }

    private static boolean isFileEmpty(PropertyResource resource) {
        return COMMAND_CONFIG_PROPERTIES.stream().anyMatch(property -> resource.getObject(property) == null);
    }
}
