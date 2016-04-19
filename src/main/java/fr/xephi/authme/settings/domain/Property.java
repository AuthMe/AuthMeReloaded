package fr.xephi.authme.settings.domain;

import org.bukkit.configuration.file.FileConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Property class, representing a <i>setting</i> that is read from the config.yml file.
 */
public abstract class Property<T> {

    private final String path;
    private final T defaultValue;

    protected Property(String path, T defaultValue) {
        Objects.requireNonNull(defaultValue);
        this.path = path;
        this.defaultValue = defaultValue;
    }

    /**
     * Create a new string list property.
     *
     * @param path The property's path
     * @param defaultValues The items in the default list
     * @return The created list property
     */
    public static Property<List<String>> newListProperty(String path, String... defaultValues) {
        // does not have the same name as not to clash with #newProperty(String, String)
        return new StringListProperty(path, defaultValues);
    }

    /**
     * Create a new enum property.
     *
     * @param clazz The enum class
     * @param path The property's path
     * @param defaultValue The default value
     * @param <E> The enum type
     * @return The created enum property
     */
    public static <E extends Enum<E>> Property<E> newProperty(Class<E> clazz, String path, E defaultValue) {
        return new EnumProperty<>(clazz, path, defaultValue);
    }

    public static Property<Boolean> newProperty(String path, boolean defaultValue) {
        return new BooleanProperty(path, defaultValue);
    }

    public static Property<Integer> newProperty(String path, int defaultValue) {
        return new IntegerProperty(path, defaultValue);
    }

    public static Property<String> newProperty(String path, String defaultValue) {
        return new StringProperty(path, defaultValue);
    }

    /**
     * Get the property value from the given configuration &ndash; guaranteed to never return null.
     *
     * @param configuration The configuration to read the value from
     * @return The value, or default if not present
     */
    public abstract T getFromFile(FileConfiguration configuration);

    /**
     * Return whether or not the given configuration file contains the property.
     *
     * @param configuration The configuration file to verify
     * @return True if the property is present, false otherwise
     */
    public boolean isPresent(FileConfiguration configuration) {
        return configuration.contains(path);
    }

    /**
     * Format the property's value as YAML.
     *
     * @param configuration The file configuration
     * @param simpleYaml YAML object (default)
     * @param singleQuoteYaml YAML object using single quotes
     * @return The generated YAML
     */
    public String toYaml(FileConfiguration configuration, Yaml simpleYaml, Yaml singleQuoteYaml) {
        return simpleYaml.dump(getFromFile(configuration));
    }

    /**
     * Return the default value of the property.
     *
     * @return The default value
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Return the property path (i.e. the node at which this property is located in the YAML file).
     *
     * @return The path
     */
    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "Property '" + path + "'";
    }


    /**
     * Boolean property.
     */
    private static final class BooleanProperty extends Property<Boolean> {

        public BooleanProperty(String path, Boolean defaultValue) {
            super(path, defaultValue);
        }

        @Override
        public Boolean getFromFile(FileConfiguration configuration) {
            return configuration.getBoolean(getPath(), getDefaultValue());
        }
    }

    /**
     * Integer property.
     */
    private static final class IntegerProperty extends Property<Integer> {

        public IntegerProperty(String path, Integer defaultValue) {
            super(path, defaultValue);
        }

        @Override
        public Integer getFromFile(FileConfiguration configuration) {
            return configuration.getInt(getPath(), getDefaultValue());
        }
    }

    /**
     * String property.
     */
    private static final class StringProperty extends Property<String> {

        public StringProperty(String path, String defaultValue) {
            super(path, defaultValue);
        }

        @Override
        public String getFromFile(FileConfiguration configuration) {
            return configuration.getString(getPath(), getDefaultValue());
        }

        @Override
        public String toYaml(FileConfiguration configuration, Yaml simpleYaml, Yaml singleQuoteYaml) {
            return singleQuoteYaml.dump(getFromFile(configuration));
        }
    }

    /**
     * String list property.
     */
    private static final class StringListProperty extends Property<List<String>> {

        public StringListProperty(String path, String[] defaultValues) {
            super(path, Arrays.asList(defaultValues));
        }

        @Override
        public List<String> getFromFile(FileConfiguration configuration) {
            if (!configuration.isList(getPath())) {
                return getDefaultValue();
            }
            return configuration.getStringList(getPath());
        }

        @Override
        public boolean isPresent(FileConfiguration configuration) {
            return configuration.isList(getPath());
        }

        @Override
        public String toYaml(FileConfiguration configuration, Yaml simpleYaml, Yaml singleQuoteYaml) {
            List<String> value = getFromFile(configuration);
            String yaml = singleQuoteYaml.dump(value);
            // If the property is a non-empty list we need to append a new line because it will be
            // something like the following, which requires a new line:
            // - 'item 1'
            // - 'second item in list'
            return value.isEmpty() ? yaml : "\n" + yaml;
        }
    }

}
