package fr.xephi.authme.service;

import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandInitializer;
import fr.xephi.authme.command.help.HelpMessage;
import fr.xephi.authme.command.help.HelpMessagesService;
import fr.xephi.authme.command.help.HelpSection;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates the full command structure for the help translation and saves it to the current help file,
 * preserving already existing entries.
 */
public class HelpTranslationGenerator {

    @Inject
    private CommandInitializer commandInitializer;

    @Inject
    private HelpMessagesService helpMessagesService;

    @Inject
    private Settings settings;

    @DataFolder
    @Inject
    private File dataFolder;

    /**
     * Updates the help file to contain entries for all commands.
     *
     * @return the help file that has been updated
     * @throws IOException if the help file cannot be written to
     */
    public File updateHelpFile() throws IOException {
        String languageCode = settings.getProperty(PluginSettings.MESSAGES_LANGUAGE);
        File helpFile = new File(dataFolder, "messages/help_" + languageCode + ".yml");
        Map<String, Object> helpEntries = generateHelpMessageEntries();

        String helpEntriesYaml = exportToYaml(helpEntries);
        Files.write(helpFile.toPath(), helpEntriesYaml.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        return helpFile;
    }

    private static String exportToYaml(Map<String, Object> helpEntries) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        return new Yaml(options).dump(helpEntries);
    }

    /**
     * Generates entries for a complete help text file.
     *
     * @return help text entries to save
     */
    private Map<String, Object> generateHelpMessageEntries() {
        Map<String, Object> messageEntries = new LinkedHashMap<>(HelpMessage.values().length);
        for (HelpMessage message : HelpMessage.values()) {
            messageEntries.put(message.getEntryKey(), helpMessagesService.getMessage(message));
        }

        Map<String, String> defaultPermissions = new LinkedHashMap<>();
        for (DefaultPermission defaultPermission : DefaultPermission.values()) {
            defaultPermissions.put(HelpMessagesService.getDefaultPermissionsSubPath(defaultPermission),
                helpMessagesService.getMessage(defaultPermission));
        }
        messageEntries.put("defaultPermissions", defaultPermissions);

        Map<String, String> sectionEntries = new LinkedHashMap<>(HelpSection.values().length);
        for (HelpSection section : HelpSection.values()) {
            sectionEntries.put(section.getEntryKey(), helpMessagesService.getMessage(section));
        }

        Map<String, Object> commandEntries = new LinkedHashMap<>();
        for (CommandDescription command : commandInitializer.getCommands()) {
            generateCommandEntries(command, commandEntries);
        }

        return ImmutableMap.of(
            "common", messageEntries,
            "section", sectionEntries,
            "commands", commandEntries);
    }

    /**
     * Adds YAML entries for the provided command its children to the given map.
     *
     * @param command the command to process (including its children)
     * @param commandEntries the map to add the generated entries to
     */
    private void generateCommandEntries(CommandDescription command, Map<String, Object> commandEntries) {
        CommandDescription translatedCommand = helpMessagesService.buildLocalizedDescription(command);
        Map<String, Object> commandData = new LinkedHashMap<>();
        commandData.put("description", translatedCommand.getDescription());
        commandData.put("detailedDescription", translatedCommand.getDetailedDescription());

        int i = 1;
        for (CommandArgumentDescription argument : translatedCommand.getArguments()) {
            Map<String, String> argumentData = new LinkedHashMap<>(2);
            argumentData.put("label", argument.getName());
            argumentData.put("description", argument.getDescription());
            commandData.put("arg" + i, argumentData);
            ++i;
        }

        commandEntries.put(HelpMessagesService.getCommandSubPath(translatedCommand), commandData);
        translatedCommand.getChildren().forEach(child -> generateCommandEntries(child, commandEntries));
    }
}
