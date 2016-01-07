package fr.xephi.authme.settings.domain;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Handles a certain property type and provides type-specific functionality.
 *
 * @param <T> The value of the property
 * @see Property
 */
public abstract class PropertyType<T> {

    public static final PropertyType<Boolean> BOOLEAN = new BooleanProperty();
    public static final PropertyType<Double>  DOUBLE  = new DoubleProperty();
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
    public abstract T getFromFile(Property<T> property, YamlConfiguration configuration);

    /**
     * Return the property's value (or its default) as YAML.
     *
     * @param property The property to transform
     * @param configuration The YAML configuration to read from
     * @return The read value or its default in YAML format
     */
    public List<String> asYaml(Property<T> property, YamlConfiguration configuration) {
        return asYaml(getFromFile(property, configuration));
    }

    /**
     * Transform the given value to YAML.
     *
     * @param value The value to transform
     * @return The value as YAML
     */
    protected abstract List<String> asYaml(T value);

    protected boolean contains(Property<T> property, YamlConfiguration configuration) {
        return configuration.contains(property.getPath());
    }


    /**
     * Boolean property.
     */
    private static final class BooleanProperty extends PropertyType<Boolean> {
        @Override
        public Boolean getFromFile(Property<Boolean> property, YamlConfiguration configuration) {
            return configuration.getBoolean(property.getPath(), property.getDefaultValue());
        }

        @Override
        protected List<String> asYaml(Boolean value) {
            return asList(value ? "true" : "false");
        }
    }

    /**
     * Double property.
     */
    private static final class DoubleProperty extends PropertyType<Double> {
        @Override
        public Double getFromFile(Property<Double> property, YamlConfiguration configuration) {
            return configuration.getDouble(property.getPath(), property.getDefaultValue());
        }

        @Override
        protected List<String> asYaml(Double value) {
            return asList(String.valueOf(value));
        }
    }

    /**
     * Integer property.
     */
    private static final class IntegerProperty extends PropertyType<Integer> {
        @Override
        public Integer getFromFile(Property<Integer> property, YamlConfiguration configuration) {
            return configuration.getInt(property.getPath(), property.getDefaultValue());
        }

        @Override
        protected List<String> asYaml(Integer value) {
            return asList(String.valueOf(value));
        }
    }

    /**
     * String property.
     */
    private static final class StringProperty extends PropertyType<String> {
        @Override
        public String getFromFile(Property<String> property, YamlConfiguration configuration) {
            return configuration.getString(property.getPath(), property.getDefaultValue());
        }

        @Override
        protected List<String> asYaml(String value) {
            return asList(toYamlLiteral(value));
        }

        public static String toYamlLiteral(String str) {
            // TODO: Need to handle new lines properly
            return "'" + str.replace("'", "''") + "'";
        }
    }

    /**
     * String list property.
     */
    private static final class StringListProperty extends PropertyType<List<String>> {
        @Override
        public List<String> getFromFile(Property<List<String>> property, YamlConfiguration configuration) {
            if (!configuration.isList(property.getPath())) {
                return property.getDefaultValue();
            }
            return configuration.getStringList(property.getPath());
        }

        @Override
        protected List<String> asYaml(List<String> value) {
            if (value.isEmpty()) {
                return asList("[]");
            }

            List<String> resultLines = new ArrayList<>();
            resultLines.add(""); // add
            for (String entry : value) {
                // TODO: StringProperty#toYamlLiteral will return List<String>...
                resultLines.add("    - " + StringProperty.toYamlLiteral(entry));
            }
            return resultLines;
        }
    }

}
