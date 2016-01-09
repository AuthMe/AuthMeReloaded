package fr.xephi.authme.settings.custom;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import fr.xephi.authme.util.CollectionUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;

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

        // TODO ljacqu 20160109: Ensure that save() works as desired (i.e. that it always produces valid YAML)
        // and then uncomment the lines below. Once this is uncommented, the checks in the old Settings.java should
        // be removed as we should check to rewrite the config.yml file only at one place
        // --------
        // PropertyMap propertyMap = SettingsFieldRetriever.getAllPropertyFields();
        // if (!containsAllSettings(propertyMap)) {
        //     save(propertyMap);
        // }
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

        if (propertyMap != null && !containsAllSettings(propertyMap)) {
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

    public void save() {
        save(SettingsFieldRetriever.getAllPropertyFields());
    }

    public void save(PropertyMap propertyMap) {
        try (FileWriter writer = new FileWriter(file)) {
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
                    .append(": ");

                List<String> yamlLines = property.formatValueAsYaml(configuration);
                String delim = "";
                for (String yamlLine : yamlLines) {
                    writer.append(delim).append(yamlLine);
                    delim = "\n" + indent(indentationLevel);
                }

                currentPath = propertyPath.subList(0, propertyPath.size() - 1);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            ConsoleLogger.showError("Could not save config file - " + StringUtils.formatException(e));
            ConsoleLogger.writeStackTrace(e);
        }
    }

    @VisibleForTesting
    boolean containsAllSettings(PropertyMap propertyMap) {
        for (Property<?> property : propertyMap.keySet()) {
            if (!property.isPresent(configuration)) {
                return false;
            }
        }
        return true;
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
