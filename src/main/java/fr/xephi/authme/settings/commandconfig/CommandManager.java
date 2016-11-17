package fr.xephi.authme.settings.commandconfig;

import com.github.authme.configme.SettingsManager;
import com.github.authme.configme.resource.YamlFileResource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.util.FileUtils;
import org.bukkit.entity.Player;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.Map;

/**
 * Manages configurable commands to be run when various events occur.
 */
public class CommandManager implements Reloadable {

    private CommandConfig commandConfig;

    @Inject
    @DataFolder
    private File dataFolder;

    @Inject
    private BukkitService bukkitService;


    CommandManager() {
    }

    public void runCommandsOnJoin(Player player) {
        executeCommands(player, commandConfig.getOnJoin());
    }

    public void runCommandsOnRegister(Player player) {
        executeCommands(player, commandConfig.getOnRegister());
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

    @PostConstruct
    @Override
    public void reload() {
        File file = new File(dataFolder, "commands.yml");
        FileUtils.copyFileFromResource(file, "commands.yml");

        SettingsManager settingsManager = new SettingsManager(
            new YamlFileResource(file), null, CommandSettingsHolder.class);
        commandConfig = settingsManager.getProperty(CommandSettingsHolder.COMMANDS);
    }


}
