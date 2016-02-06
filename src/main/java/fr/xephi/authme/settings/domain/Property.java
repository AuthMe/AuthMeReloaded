package fr.xephi.authme.settings.domain;

import org.bukkit.configuration.file.FileConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Property class, representing a <i>setting</i> that is read from the config.yml file.
 */
public class Property<T> {

    private final PropertyType<T> type;
    private final String path;
    private final T defaultValue;

    private Property(PropertyType<T> type, String path, T defaultValue) {
        Objects.requireNonNull(defaultValue);
        this.type = type;
        this.path = path;
        this.defaultValue = defaultValue;
    }

    /**
     * Create a new property. See also {@link #newProperty(PropertyType, String, Object[])} for lists and
     * {@link #newProperty(Class, String, Enum)}.
     *
     * @param type The property type
     * @param path The property's path
     * @param defaultValue The default value
     * @param <T> The type of the property
     * @return The created property
     */
    public static <T> Property<T> newProperty(PropertyType<T> type, String path, T defaultValue) {
        return new Property<>(type, path, defaultValue);
    }

    /**
     * Create a new list property.
     *
     * @param type The list type of the property
     * @param path The property's path
     * @param defaultValues The default value's items
     * @param <U> The list type
     * @return The created list property
     */
    @SafeVarargs
    public static <U> Property<List<U>> newProperty(PropertyType<List<U>> type, String path, U... defaultValues) {
        return new Property<>(type, path, Arrays.asList(defaultValues));
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
        return new Property<>(new EnumPropertyType<>(clazz), path, defaultValue);
    }

    // -----
    // Overloaded convenience methods for specific types
    // -----
    public static Property<Boolean> newProperty(String path, boolean defaultValue) {
        return new Property<>(PropertyType.BOOLEAN, path, defaultValue);
    }

    public static Property<Integer> newProperty(String path, int defaultValue) {
        return new Property<>(PropertyType.INTEGER, path, defaultValue);
    }

    public static Property<String> newProperty(String path, String defaultValue) {
        return new Property<>(PropertyType.STRING, path, defaultValue);
    }

    // -----
    // Hooks to the PropertyType methods
    // -----
    /**
     * Get the property value from the given configuration &ndash; guaranteed to never return null.
     *
     * @param configuration The configuration to read the value from
     * @return The value, or default if not present
     */
    public T getFromFile(FileConfiguration configuration) {
        return type.getFromFile(this, configuration);
    }

    /**
     * Return whether or not the given configuration file contains the property.
     *
     * @param configuration The configuration file to verify
     * @return True if the property is present, false otherwise
     */
    public boolean isPresent(FileConfiguration configuration) {
        return type.contains(this, configuration);
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
        return type.toYaml(getFromFile(configuration), simpleYaml, singleQuoteYaml);
    }

    // -----
    // Trivial getters
    // -----
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

}
