package fr.xephi.authme.settings.commandconfig;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.SettingsMigrationService;
import fr.xephi.authme.util.RandomStringUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Migrates the commands from their old location, in config.yml, to the dedicated commands configuration file.
 */
class CommandsMigrater {

    @Inject
    private SettingsMigrationService settingsMigrationService;

    CommandsMigrater() {
    }

    /**
     * Adds command settings from their old location (in config.yml) to the given command configuration object.
     *
     * @param commandConfig the command config object to move old commands to
     * @return true if commands have been moved, false if no migration was necessary
     */
    boolean transformOldCommands(CommandConfig commandConfig) {
        boolean didMoveCommands = false;
        for (MigratableCommandSection section : MigratableCommandSection.values()) {
            didMoveCommands |= section.convertCommands(settingsMigrationService, commandConfig);
        }
        return didMoveCommands;
    }

    /**
     * Enum defining the forced command settings that should be moved from config.yml to the new commands.yml file.
     */
    private enum MigratableCommandSection {

        ON_JOIN(
            SettingsMigrationService::getOnLoginCommands,
            Executor.PLAYER,
            CommandConfig::getOnLogin),

        ON_JOIN_CONSOLE(
            SettingsMigrationService::getOnLoginConsoleCommands,
            Executor.CONSOLE,
            CommandConfig::getOnLogin),

        ON_REGISTER(
            SettingsMigrationService::getOnRegisterCommands,
            Executor.PLAYER,
            CommandConfig::getOnRegister),

        ON_REGISTER_CONSOLE(
            SettingsMigrationService::getOnRegisterConsoleCommands,
            Executor.CONSOLE,
            CommandConfig::getOnRegister);

        private final Function<SettingsMigrationService, List<String>> legacyCommandsGetter;
        private final Executor executor;
        private final Function<CommandConfig, Map<String, Command>> commandMapGetter;

        /**
         * Constructor.
         *
         * @param legacyCommandsGetter getter on MigrationService to get the deprecated command entries
         * @param executor the executor of the commands
         * @param commandMapGetter the getter for the commands map in the new settings structure to add the old
         *                         settings to after conversion
         */
        MigratableCommandSection(Function<SettingsMigrationService, List<String>> legacyCommandsGetter,
                                 Executor executor,
                                 Function<CommandConfig, Map<String, Command>> commandMapGetter) {
            this.legacyCommandsGetter = legacyCommandsGetter;
            this.executor = executor;
            this.commandMapGetter = commandMapGetter;
        }

        /**
         * Adds the commands from the sections' settings migration service to the appropriate place in the new
         * command config object.
         *
         * @param settingsMigrationService settings migration service to read old commands from
         * @param commandConfig command config object to add converted commands to
         * @return true if there were commands to migrate, false otherwise
         */
        boolean convertCommands(SettingsMigrationService settingsMigrationService, CommandConfig commandConfig) {
            List<Command> commands = legacyCommandsGetter.apply(settingsMigrationService).stream()
                .map(cmd -> new Command(cmd, executor)).collect(Collectors.toList());

            if (commands.isEmpty()) {
                return false;
            }
            Map<String, Command> commandMap = commandMapGetter.apply(commandConfig);
            commands.forEach(cmd -> commandMap.put(RandomStringUtils.generate(10), cmd));
            ConsoleLogger.info("Migrated " + commands.size() + " commands of type " + this);
            return true;
        }
    }
}
