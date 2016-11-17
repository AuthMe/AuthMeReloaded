package tools.filegeneration;

import com.github.authme.configme.SettingsManager;
import com.github.authme.configme.resource.YamlFileResource;
import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.settings.commandconfig.Command;
import fr.xephi.authme.settings.commandconfig.CommandConfig;
import fr.xephi.authme.settings.commandconfig.CommandSettingsHolder;
import fr.xephi.authme.settings.commandconfig.Executor;
import tools.utils.AutoToolTask;
import tools.utils.ToolsConstants;

import java.io.File;
import java.util.Scanner;

/**
 * Generates the commands.yml file that corresponds to the default in the code.
 */
public class GenerateCommandsYml implements AutoToolTask {

    private static final String COMMANDS_YML_FILE = ToolsConstants.MAIN_RESOURCES_ROOT + "commands.yml";

    @Override
    public void execute(Scanner scanner) {
        executeDefault();
    }

    @Override
    public void executeDefault() {
        File file = new File(COMMANDS_YML_FILE);

        // Get default and add sample entry
        CommandConfig commandConfig = CommandSettingsHolder.COMMANDS.getDefaultValue();
        commandConfig.setOnLogin(
            ImmutableMap.of("welcome", newCommand("msg %p Welcome back!", Executor.PLAYER)));

        // Export the value to the file
        SettingsManager settingsManager = new SettingsManager(
            new YamlFileResource(file), null, CommandSettingsHolder.class);
        settingsManager.setProperty(CommandSettingsHolder.COMMANDS, commandConfig);
        settingsManager.save();

        System.out.println("Updated " + COMMANDS_YML_FILE);
    }

    @Override
    public String getTaskName() {
        return "generateCommandsYml";
    }

    private static Command newCommand(String commandLine, Executor executor) {
        Command command = new Command();
        command.setCommand(commandLine);
        command.setExecutor(executor);
        return command;
    }
}
