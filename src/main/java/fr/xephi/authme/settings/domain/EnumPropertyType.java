package fr.xephi.authme.settings.domain;

import org.bukkit.configuration.file.FileConfiguration;
import org.yaml.snakeyaml.Yaml;

/**
 * Enum property type.
 *
 * @param <E> The enum class
 */
class EnumPropertyType<E extends Enum<E>> extends PropertyType<E> {

    private Class<E> clazz;

    public EnumPropertyType(Class<E> clazz) {
        this.clazz = clazz;
    }

    @Override
    public E getFromFile(Property<E> property, FileConfiguration configuration) {
        String textValue = configuration.getString(property.getPath());
        if (textValue == null) {
            return property.getDefaultValue();
        }
        E mappedValue = mapToEnum(textValue);
        return mappedValue != null ? mappedValue : property.getDefaultValue();
    }

    @Override
    public boolean contains(Property<E> property, FileConfiguration configuration) {
        return super.contains(property, configuration)
            && mapToEnum(configuration.getString(property.getPath())) != null;
    }

    @Override
    public String toYaml(E value, Yaml simpleYaml, Yaml singleQuoteYaml) {
        return singleQuoteYaml.dump(value.name());
    }

    private E mapToEnum(String value) {
        for (E entry : clazz.getEnumConstants()) {
            if (entry.name().equalsIgnoreCase(value)) {
                return entry;
            }
        }
        return null;
    }
}
