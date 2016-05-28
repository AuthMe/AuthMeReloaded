package fr.xephi.authme.settings;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import fr.xephi.authme.util.CollectionUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static fr.xephi.authme.util.FileUtils.copyFileFromResource;

/**
 * The new settings manager.
 */
public class NewSetting {

    private final File pluginFolder;
    private final File configFile;
    private final PropertyMap propertyMap;
    private final SettingsMigrationService migrationService;
    private FileConfiguration configuration;
    /** The file with the localized messages based on {@link PluginSettings#MESSAGES_LANGUAGE}. */
    private File messagesFile;
    private List<String> welcomeMessage;
    private String emailMessage;

    /**
     * Constructor. Checks the given {@link FileConfiguration} object for completeness.
     *
     * @param configFile The configuration file
     * @param pluginFolder The AuthMe plugin folder
     * @param propertyMap Collection of all available settings
     * @param migrationService Migration service to check the settings file with
     */
    public NewSetting(File configFile, File pluginFolder, PropertyMap propertyMap,
                      SettingsMigrationService migrationService) {
        this.configuration = YamlConfiguration.loadConfiguration(configFile);
        this.configFile = configFile;
        this.pluginFolder = pluginFolder;
        this.propertyMap = propertyMap;
        this.migrationService = migrationService;
        validateAndLoadOptions();
    }

    /**
     * Constructor for testing purposes, allowing more options.
     *
     * @param configuration The FileConfiguration object to use
     * @param configFile The file to write to
     * @param pluginFolder The plugin folder
     * @param propertyMap The property map whose properties should be verified for presence, or null to skip this
     * @param migrationService Migration service, or null to skip migration checks
     */
    @VisibleForTesting
    NewSetting(FileConfiguration configuration, File configFile, File pluginFolder, PropertyMap propertyMap,
               SettingsMigrationService migrationService) {
        this.configuration = configuration;
        this.configFile = configFile;
        this.pluginFolder = pluginFolder;
        this.propertyMap = propertyMap;
        this.migrationService = migrationService;

        if (propertyMap != null && migrationService != null) {
            validateAndLoadOptions();
        }
    }

    /**
     * Get the given property from the configuration.
     *
     * @param property The property to retrieve
     * @param <T> The property's type
     * @return The property's value
     */
    public <T> T getProperty(Property<T> property) {
        return property.getFromFile(configuration);
    }

    /**
     * Set a new value for the given property.
     *
     * @param property The property to modify
     * @param value The new value to assign to the property
     * @param <T> The property's type
     */
    public <T> void setProperty(Property<T> property, T value) {
        configuration.set(property.getPath(), value);
    }

    /**
     * Return the messages file based on the messages language config.
     *
     * @return The messages file to read messages from
     */
    public File getMessagesFile() {
        return messagesFile;
    }

    /**
     * Return the path to the default messages file within the JAR.
     *
     * @return The default messages file path
     */
    public String getDefaultMessagesFile() {
        return "/messages/messages_en.yml";
    }

    /**
     * Return the text to use in email registrations.
     *
     * @return The email message
     */
    public String getEmailMessage() {
        return emailMessage;
    }

    /**
     * Return the lines to output after an in-game registration.
     *
     * @return The welcome message
     */
    public List<String> getWelcomeMessage() {
        return welcomeMessage;
    }

    /**
     * Reload the configuration.
     */
    public void reload() {
        configuration = YamlConfiguration.loadConfiguration(configFile);
        validateAndLoadOptions();
    }

    /**
     * Save the config file. Use after migrating one or more settings.
     */
    public void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            Yaml simpleYaml = newYaml(false);
            Yaml singleQuoteYaml = newYaml(true);

            writer.write("");
            // Contains all but the last node of the setting, e.g. [DataSource, mysql] for "DataSource.mysql.username"
            List<String> currentPath = new ArrayList<>();
            for (Map.Entry<Property<?>, String[]> entry : propertyMap.entrySet()) {
                Property<?> property = entry.getKey();

                // Handle properties
                List<String> propertyPath = Arrays.asList(property.getPath().split("\\."));
                List<String> commonPathParts = CollectionUtils.filterCommonStart(
                    currentPath, propertyPath.subList(0, propertyPath.size() - 1));
                List<String> newPathParts = CollectionUtils.getRange(propertyPath, commonPathParts.size());

                if (commonPathParts.isEmpty()) {
                    writer.append("\n");
                }

                int indentationLevel = commonPathParts.size();
                if (newPathParts.size() > 1) {
                    for (String path : newPathParts.subList(0, newPathParts.size() - 1)) {
                        writer.append("\n")
                            .append(indent(indentationLevel))
                            .append(path)
                            .append(": ");
                        ++indentationLevel;
                    }
                }
                for (String comment : entry.getValue()) {
                    writer.append("\n")
                        .append(indent(indentationLevel))
                        .append("# ")
                        .append(comment);
                }
                writer.append("\n")
                    .append(indent(indentationLevel))
                    .append(CollectionUtils.getRange(newPathParts, newPathParts.size() - 1).get(0))
                    .append(": ")
                    .append(toYaml(property, indentationLevel, simpleYaml, singleQuoteYaml));

                currentPath = propertyPath.subList(0, propertyPath.size() - 1);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            ConsoleLogger.logException("Could not save config file:", e);
        }
    }

    private void validateAndLoadOptions() {
        if (migrationService.checkAndMigrate(configuration, propertyMap, pluginFolder)) {
            ConsoleLogger.info("Merged new config options");
            ConsoleLogger.info("Please check your config.yml file for new settings!");
            save();
        }

        messagesFile = buildMessagesFile();
        welcomeMessage = readWelcomeMessage();
        emailMessage = readEmailMessage();
    }

    private <T> String toYaml(Property<T> property, int indent, Yaml simpleYaml, Yaml singleQuoteYaml) {
        String representation = property.toYaml(configuration, simpleYaml, singleQuoteYaml);
        return Joiner.on("\n" + indent(indent)).join(representation.split("\\n"));
    }

    private File buildMessagesFile() {
        String languageCode = getProperty(PluginSettings.MESSAGES_LANGUAGE);

        String filePath = buildMessagesFilePathFromCode(languageCode);
        File messagesFile = new File(pluginFolder, filePath);
        if (copyFileFromResource(messagesFile, filePath)) {
            return messagesFile;
        }

        // File doesn't exist or couldn't be copied - try again with default, "en"
        String defaultFilePath = buildMessagesFilePathFromCode("en");
        File defaultFile = new File(pluginFolder, defaultFilePath);
        copyFileFromResource(defaultFile, defaultFilePath);

        // No matter the result, need to return a file
        return defaultFile;
    }

    private static String buildMessagesFilePathFromCode(String language) {
        return StringUtils.makePath("messages", "messages_" + language + ".yml");
    }

    private List<String> readWelcomeMessage() {
        if (getProperty(RegistrationSettings.USE_WELCOME_MESSAGE)) {
            final File welcomeFile = new File(pluginFolder, "welcome.txt");
            final Charset charset = Charset.forName("UTF-8");
            if (copyFileFromResource(welcomeFile, "welcome.txt")) {
                try {
                    return Files.readLines(welcomeFile, charset);
                } catch (IOException e) {
                    ConsoleLogger.logException("Failed to read file '" + welcomeFile.getPath() + "':", e);
                }
            }
        }
        return new ArrayList<>(0);
    }

    private String readEmailMessage() {
        final File emailFile = new File(pluginFolder, "email.html");
        final Charset charset = Charset.forName("UTF-8");
        if (copyFileFromResource(emailFile, "email.html")) {
            try {
                return Files.toString(emailFile, charset);
            } catch (IOException e) {
                ConsoleLogger.logException("Failed to read file '" + emailFile.getPath() + "':", e);
            }
        }
        return "";
    }

    private static Yaml newYaml(boolean useSingleQuotes) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        if (useSingleQuotes) {
            options.setDefaultScalarStyle(DumperOptions.ScalarStyle.SINGLE_QUOTED);
        }
        return new Yaml(options);
    }

    private static String indent(int level) {
        // We use an indentation of 4 spaces
        return Strings.repeat(" ", level * 4);
    }

}
