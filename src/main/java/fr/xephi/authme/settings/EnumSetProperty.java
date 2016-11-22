package fr.xephi.authme.settings;

import com.github.authme.configme.SettingsManager;
import com.github.authme.configme.properties.StringListProperty;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Property whose value is a set of entries of a given enum.
 */
// TODO #1014: This property type currently extends StringListProperty with a dedicated method  to convert the values
// into a Set of the selected enum due to multiple issues on ConfigMe's side.
public class EnumSetProperty<E extends Enum<E>> extends StringListProperty {

    private final Class<E> enumClass;

    public EnumSetProperty(Class<E> enumClass, String path, String... values) {
        super(path, values);
        this.enumClass = enumClass;
    }

    /**
     * Returns the value as a set of enum entries.
     *
     * @param settings the settings manager to look up the raw value with
     * @return the property's value as mapped enum entries
     */
    public Set<E> asEnumSet(SettingsManager settings) {
        List<String> entries = settings.getProperty(this);
        return entries.stream()
            .map(str -> toEnum(str))
            .filter(e -> e != null)
            .collect(Collectors.toSet());
    }

    private E toEnum(String str) {
        for (E e : enumClass.getEnumConstants()) {
            if (str.equalsIgnoreCase(e.name())) {
                return e;
            }
        }
        return null;
    }
}
