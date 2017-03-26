package tools.filegeneration;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.resource.YamlFileResource;
import fr.xephi.authme.settings.commandconfig.CommandConfig;
import fr.xephi.authme.settings.commandconfig.CommandSettingsHolder;
import tools.utils.AutoToolTask;
import tools.utils.ToolsConstants;

import java.io.File;

/**
 * Generates the commands.yml file that corresponds to the default in the code.
 */
public class GenerateCommandsYml implements AutoToolTask {

    private static final String COMMANDS_YML_FILE = ToolsConstants.MAIN_RESOURCES_ROOT + "commands.yml";

    @Override
    public void executeDefault() {
        File file = new File(COMMANDS_YML_FILE);

        // Get the default
        CommandConfig commandConfig = CommandSettingsHolder.COMMANDS.getDefaultValue();

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
}
