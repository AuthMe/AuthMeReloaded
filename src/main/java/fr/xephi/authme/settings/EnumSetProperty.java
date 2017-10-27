package fr.xephi.authme.settings;

import ch.jalu.configme.properties.Property;
import ch.jalu.configme.resource.PropertyResource;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Sets.newHashSet;

/**
 * Property whose value is a set of entries of a given enum.
 *
 * @param <E> the enum type
 */
public class EnumSetProperty<E extends Enum<E>> extends Property<Set<E>> {

    private final Class<E> enumClass;

    @SafeVarargs
    public EnumSetProperty(Class<E> enumClass, String path, E... values) {
        super(path, newHashSet(values));
        this.enumClass = enumClass;
    }

    @Override
    protected Set<E> getFromResource(PropertyResource resource) {
        Object entry = resource.getObject(getPath());
        if (entry instanceof Collection<?>) {
            return ((Collection<?>) entry).stream()
                .map(val -> toEnum(String.valueOf(val)))
                .filter(e -> e != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
