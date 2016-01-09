package fr.xephi.authme.settings.domain;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Properties (i.e. a <i>setting</i> that is read from the config.yml file).
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

    public static <T> Property<T> newProperty(PropertyType<T> type, String path, T defaultValue) {
        return new Property<>(type, path, defaultValue);
    }

    @SafeVarargs
    public static <U> Property<List<U>> newProperty(PropertyType<List<U>> type, String path, U... defaultValues) {
        return new Property<>(type, path, Arrays.asList(defaultValues));
    }

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
     * Get the property value from the given configuration.
     *
     * @param configuration The configuration to read the value from
     * @return The value, or default if not present
     */
    public T getFromFile(FileConfiguration configuration) {
        return type.getFromFile(this, configuration);
    }

    /**
     * Format the property value as YAML.
     *
     * @param configuration The configuration to read the value from
     * @return The property value as YAML
     */
    public List<String> formatValueAsYaml(FileConfiguration configuration) {
        return type.asYaml(this, configuration);
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
