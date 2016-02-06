package fr.xephi.authme.settings.domain;

import org.bukkit.configuration.file.FileConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.util.List;

/**
 * Handles a certain property type and provides type-specific functionality.
 *
 * @param <T> The value of the property
 * @see Property
 */
public abstract class PropertyType<T> {

    public static final PropertyType<Boolean> BOOLEAN = new BooleanProperty();
    public static final PropertyType<Integer> INTEGER = new IntegerProperty();
    public static final PropertyType<String>  STRING  = new StringProperty();
    public static final PropertyType<List<String>> STRING_LIST = new StringListProperty();

    /**
     * Get the property's value from the given YAML configuration.
     *
     * @param property The property to retrieve
     * @param configuration The YAML configuration to read from
     * @return The read value, or the default value if absent
     */
    public abstract T getFromFile(Property<T> property, FileConfiguration configuration);

    /**
     * Return whether the property is present in the given configuration.
     *
     * @param property The property to search for
     * @param configuration The configuration to verify
     * @return True if the property is present, false otherwise
     */
    public boolean contains(Property<T> property, FileConfiguration configuration) {
        return configuration.contains(property.getPath());
    }

    /**
     * Format the value as YAML.
     *
     * @param value The value to export
     * @param simpleYaml YAML object (default)
     * @param singleQuoteYaml YAML object set to use single quotes
     * @return The generated YAML
     */
    public String toYaml(T value, Yaml simpleYaml, Yaml singleQuoteYaml) {
        return simpleYaml.dump(value);
    }


    /**
     * Boolean property.
     */
    private static final class BooleanProperty extends PropertyType<Boolean> {
        @Override
        public Boolean getFromFile(Property<Boolean> property, FileConfiguration configuration) {
            return configuration.getBoolean(property.getPath(), property.getDefaultValue());
        }
    }

    /**
     * Integer property.
     */
    private static final class IntegerProperty extends PropertyType<Integer> {
        @Override
        public Integer getFromFile(Property<Integer> property, FileConfiguration configuration) {
            return configuration.getInt(property.getPath(), property.getDefaultValue());
        }
    }

    /**
     * String property.
     */
    private static final class StringProperty extends PropertyType<String> {
        @Override
        public String getFromFile(Property<String> property, FileConfiguration configuration) {
            return configuration.getString(property.getPath(), property.getDefaultValue());
        }
        @Override
        public String toYaml(String value, Yaml simpleYaml, Yaml singleQuoteYaml) {
            return singleQuoteYaml.dump(value);
        }
    }

    /**
     * String list property.
     */
    private static final class StringListProperty extends PropertyType<List<String>> {
        @Override
        public List<String> getFromFile(Property<List<String>> property, FileConfiguration configuration) {
            if (!configuration.isList(property.getPath())) {
                return property.getDefaultValue();
            }
            return configuration.getStringList(property.getPath());
        }

        @Override
        public boolean contains(Property<List<String>> property, FileConfiguration configuration) {
            return configuration.contains(property.getPath()) && configuration.isList(property.getPath());
        }

        @Override
        public String toYaml(List<String> value, Yaml simpleYaml, Yaml singleQuoteYaml) {
            String yaml = singleQuoteYaml.dump(value);
            // If the property is a non-empty list we need to append a new line because it will be
            // something like the following, which requires a new line:
            // - 'item 1'
            // - 'second item in list'
            return value.isEmpty() ? yaml : "\n" + yaml;
        }
    }

}
