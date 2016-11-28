package fr.xephi.authme.settings.commandconfig;

import com.github.authme.configme.SettingsManager;
import com.github.authme.configme.resource.YamlFileResource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.util.FileUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.io.File;
import java.util.Map;

/**
 * Manages configurable commands to be run when various events occur.
 */
public class CommandManager implements Reloadable {

    private final File dataFolder;
    private final BukkitService bukkitService;
    private final CommandMigrationService commandMigrationService;

    private CommandConfig commandConfig;

    @Inject
    CommandManager(@DataFolder File dataFolder, BukkitService bukkitService,
                   CommandMigrationService commandMigrationService) {
        this.dataFolder = dataFolder;
        this.bukkitService = bukkitService;
        this.commandMigrationService = commandMigrationService;
        reload();
    }

    /**
     * Runs the configured commands for when a player has joined.
     *
     * @param player the joining player
     */
    public void runCommandsOnJoin(Player player) {
        executeCommands(player, commandConfig.getOnJoin());
    }

    /**
     * Runs the configured commands for when a player has successfully registered.
     *
     * @param player the player who has registered
     */
    public void runCommandsOnRegister(Player player) {
        executeCommands(player, commandConfig.getOnRegister());
    }

    /**
     * Runs the configured commands for when a player has logged in successfully.
     *
     * @param player the player that logged in
     */
    public void runCommandsOnLogin(Player player) {
        executeCommands(player, commandConfig.getOnLogin());
    }

    private void executeCommands(Player player, Map<String, Command> commands) {
        for (Command command : commands.values()) {
            final String execution = command.getCommand().replace("%p", player.getName());
            if (Executor.CONSOLE.equals(command.getExecutor())) {
                bukkitService.dispatchConsoleCommand(execution);
            } else {
                bukkitService.dispatchCommand(player, execution);
            }
        }
    }

    @Override
    public void reload() {
        File file = new File(dataFolder, "commands.yml");
        FileUtils.copyFileFromResource(file, "commands.yml");

        SettingsManager settingsManager = new SettingsManager(
            new YamlFileResource(file), commandMigrationService, CommandSettingsHolder.class);
        commandConfig = settingsManager.getProperty(CommandSettingsHolder.COMMANDS);
    }


}
