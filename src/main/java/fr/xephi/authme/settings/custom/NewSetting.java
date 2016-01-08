package fr.xephi.authme.settings.custom;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.propertymap.PropertyMap;
import fr.xephi.authme.util.CollectionUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.configuration.file.YamlConfiguration;

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
    private YamlConfiguration configuration;

    public NewSetting(File file) {
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.file = file;

        PropertyMap propertyMap = SettingsFieldRetriever.getAllPropertyFields();
        if (!containsAllSettings(propertyMap)) {
            save(propertyMap);
        }
    }

    @VisibleForTesting
    NewSetting(YamlConfiguration yamlConfiguration, String file) {
        this.configuration = yamlConfiguration;
        this.file = new File(file);
    }

    public <T> T getOption(Property<T> property) {
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

    private boolean containsAllSettings(PropertyMap propertyMap) {
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
