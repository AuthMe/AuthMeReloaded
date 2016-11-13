package fr.xephi.authme.settings;

import com.github.authme.configme.properties.Property;
import com.github.authme.configme.resource.PropertyResource;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Property whose value is a set of entries of a given enum.
 */
// TODO https://github.com/AuthMe/ConfigMe/issues/27: Should be a Property of Set<E> type
public class EnumSetProperty<E extends Enum<E>> extends Property<List<E>> {

    private final Class<E> enumClass;

    public EnumSetProperty(Class<E> enumClass, String path, List<E> defaultValue) {
        super(path, defaultValue);
        this.enumClass = enumClass;
    }

    @Override
    protected List<E> getFromResource(PropertyResource resource) {
        List<?> elements = resource.getList(getPath());
        if (elements != null) {
            return elements.stream()
                .map(val -> toEnum(String.valueOf(val)))
                .filter(e -> e != null)
                .distinct()
                .collect(Collectors.toList());
        }
        return null;
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
