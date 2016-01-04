package fr.xephi.authme.settings.custom;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.custom.domain.Comment;
import fr.xephi.authme.settings.custom.domain.Property;
import fr.xephi.authme.settings.custom.domain.SettingsClass;
import fr.xephi.authme.settings.custom.propertymap.PropertyMap;
import fr.xephi.authme.util.CollectionUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The new settings manager.
 */
public class NewSetting {

    private static final List<Class<? extends SettingsClass>> CONFIGURATION_CLASSES = Arrays.asList(
        ConverterSettings.class, DatabaseSettings.class, EmailSettings.class, HooksSettings.class,
        ProtectionSettings.class, PurgeSettings.class, SecuritySettings.class);

    private File file;
    private YamlConfiguration configuration;

    public NewSetting(File file) {
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.file = file;
    }

    // TODO: No way of passing just a YamlConfiguration object (later on for mocking purposes) ?
    // If not, best is probably to keep this constructor as package-private with @VisibleForTesting
    // but it's not a satisfying solution
    public NewSetting(YamlConfiguration yamlConfiguration, String file) {
        this.configuration = yamlConfiguration;
        this.file = new File(file);
    }

    public <T> T getOption(Property<T> property) {
        return property.getFromFile(configuration);
    }

    public void save() {
        PropertyMap properties = getAllPropertyFields();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("");

            // Contains all but the last node of the setting, e.g. [DataSource, mysql] for "DataSource.mysql.username"
            List<String> currentPath = new ArrayList<>();
            for (Map.Entry<Property, String[]> entry : properties.entrySet()) {
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

    private static String indent(int level) {
        // YAML uses indentation of 4 spaces
        StringBuilder sb = new StringBuilder(level * 4);
        for (int i = 0; i < level; ++i) {
            sb.append("    ");
        }
        return sb.toString();
    }

    private static PropertyMap getAllPropertyFields() {
        PropertyMap properties = new PropertyMap();
        for (Class<?> clazz : CONFIGURATION_CLASSES) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields) {
                Property property = getFieldIfRelevant(field);
                if (property != null) {
                    properties.put(property, getCommentsForField(field));
                }
            }
        }
        return properties;
    }

    private static String[] getCommentsForField(Field field) {
        if (field.isAnnotationPresent(Comment.class)) {
            return field.getAnnotation(Comment.class).value();
        }
        return new String[0];
    }

    private static Property<?> getFieldIfRelevant(Field field) {
        field.setAccessible(true);
        if (field.isAccessible() && Property.class.equals(field.getType()) && Modifier.isStatic(field.getModifiers())) {
            try {
                return (Property) field.get(null);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not fetch field '" + field.getName() + "' from class '"
                    + field.getDeclaringClass().getSimpleName() + "': " + StringUtils.formatException(e));
            }
        }
        return null;
    }



}
