package fr.xephi.authme.settings;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.SettingsFieldRetriever;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import fr.xephi.authme.util.CollectionUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The new settings manager.
 */
public class NewSetting {

    private File file;
    private FileConfiguration configuration;

    /**
     * Constructor.
     * Loads the file as YAML and checks its integrity.
     *
     * @param configuration The configuration to interact with
     * @param file The configuration file
     */
    public NewSetting(FileConfiguration configuration, File file) {
        this.configuration = configuration;
        this.file = file;

        PropertyMap propertyMap = SettingsFieldRetriever.getAllPropertyFields();
        if (SettingsMigrationService.checkAndMigrate(configuration, propertyMap)) {
            ConsoleLogger.info("Merged new config options");
            ConsoleLogger.info("Please check your config.yml file for new settings!");
            save(propertyMap);
        }
    }

    /**
     * Constructor for testing purposes, allowing more options.
     *
     * @param configuration The FileConfiguration object to use
     * @param file The file to write to
     * @param propertyMap The property map whose properties should be verified for presence, or null to skip this
     */
    @VisibleForTesting
    NewSetting(FileConfiguration configuration, File file, PropertyMap propertyMap) {
        this.configuration = configuration;
        this.file = file;

        if (propertyMap != null && SettingsMigrationService.checkAndMigrate(configuration, propertyMap)) {
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

    public void save(PropertyMap propertyMap) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("");

            DumperOptions simpleOptions = new DumperOptions();
            simpleOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml simpleYaml = new Yaml(simpleOptions);
            DumperOptions singleQuoteOptions = new DumperOptions();
            singleQuoteOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.SINGLE_QUOTED);
            singleQuoteOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml singleQuoteYaml = new Yaml(singleQuoteOptions);

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
            ConsoleLogger.showError("Could not save config file - " + StringUtils.formatException(e));
            ConsoleLogger.writeStackTrace(e);
        }
    }

    private <T> String toYaml(Property<T> property, int indent, Yaml simpleYaml, Yaml singleQuoteYaml) {
        T value = property.getFromFile(configuration);
        String representation = property.hasSingleQuotes()
            ? singleQuoteYaml.dump(value)
            : simpleYaml.dump(value);

        // If the property is a non-empty list we need to append a new line because it will be
        // something like the following, which requires a new line:
        // - 'item 1'
        // - 'second item in list'
        if (property.isList() && !((List) value).isEmpty()) {
            representation = "\n" + representation;
        }

        return join("\n" + indent(indent), representation.split("\\n"));
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
        // YAML uses indentation of 4 spaces
        StringBuilder sb = new StringBuilder(level * 4);
        for (int i = 0; i < level; ++i) {
            sb.append("    ");
        }
        return sb.toString();
    }

}
