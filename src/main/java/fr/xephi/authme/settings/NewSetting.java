package fr.xephi.authme.settings;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Files;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.SettingsFieldRetriever;
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

import static fr.xephi.authme.settings.SettingsMigrationService.copyFileFromResource;

/**
 * The new settings manager.
 */
public class NewSetting {

    private final File pluginFolder;
    private final File configFile;
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
     */
    public NewSetting(File configFile, File pluginFolder) {
        this.configuration = YamlConfiguration.loadConfiguration(configFile);
        this.configFile = configFile;
        this.pluginFolder = pluginFolder;
        validateAndLoadOptions();
    }

    /**
     * Constructor for testing purposes, allowing more options.
     *
     * @param configuration The FileConfiguration object to use
     * @param configFile The file to write to
     * @param propertyMap The property map whose properties should be verified for presence, or null to skip this
     */
    @VisibleForTesting
    NewSetting(FileConfiguration configuration, File configFile, PropertyMap propertyMap) {
        this.configuration = configuration;
        this.configFile = configFile;
        this.pluginFolder = new File("");

        if (propertyMap != null && SettingsMigrationService.checkAndMigrate(configuration, propertyMap, pluginFolder)) {
            save(propertyMap);
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
     * Save the config file. Use after migrating one or more settings.
     */
    public void save() {
        save(SettingsFieldRetriever.getAllPropertyFields());
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

    public String getEmailMessage() {
        return emailMessage;
    }

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

    private void save(PropertyMap propertyMap) {
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
        PropertyMap propertyMap = SettingsFieldRetriever.getAllPropertyFields();
        if (SettingsMigrationService.checkAndMigrate(configuration, propertyMap, pluginFolder)) {
            ConsoleLogger.info("Merged new config options");
            ConsoleLogger.info("Please check your config.yml file for new settings!");
            save(propertyMap);
        }

        messagesFile = buildMessagesFile();
        welcomeMessage = readWelcomeMessage();
        emailMessage = readEmailMessage();
    }

    private <T> String toYaml(Property<T> property, int indent, Yaml simpleYaml, Yaml singleQuoteYaml) {
        String representation = property.toYaml(configuration, simpleYaml, singleQuoteYaml);
        return join("\n" + indent(indent), representation.split("\\n"));
    }

    private File buildMessagesFile() {
        String languageCode = getProperty(PluginSettings.MESSAGES_LANGUAGE);
        File messagesFile = buildMessagesFileFromCode(languageCode);
        if (messagesFile.exists()) {
            return messagesFile;
        }

        return copyFileFromResource(messagesFile, buildMessagesFilePathFromCode(languageCode))
            ? messagesFile
            : buildMessagesFileFromCode("en");
    }

    private File buildMessagesFileFromCode(String language) {
        return new File(pluginFolder, buildMessagesFilePathFromCode(language));
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
                return StringUtils.join("", Files.readLines(emailFile, charset));
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

    private static String join(String delimiter, String[] items) {
        StringBuilder sb = new StringBuilder();
        String delim = "";
        for (String item : items) {
            sb.append(delim).append(item);
            delim = delimiter;
        }
        return sb.toString();
    }

    private static String indent(int level) {
        // We use an indentation of 4 spaces
        StringBuilder sb = new StringBuilder(level * 4);
        for (int i = 0; i < level; ++i) {
            sb.append("    ");
        }
        return sb.toString();
    }

}
