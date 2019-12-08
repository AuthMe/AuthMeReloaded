package fr.xephi.authme.settings.hierarchicalvalues;

import ch.jalu.configme.properties.BaseProperty;
import ch.jalu.configme.properties.types.PropertyType;
import ch.jalu.configme.resource.PropertyReader;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Property implementation containing a hierarchical list of values.
 *
 * @param <T> the value type
 */
public class HierarchicalValuesProperty<T> extends BaseProperty<HierarchicalValues<T>> {

    private final PropertyType<T> propertyType;

    /**
     * Constructor.
     *
     * @param propertyType the property type to convert the map values with
     * @param path the path of the property
     * @param defaultValue the root value to use in the default value
     */
    public HierarchicalValuesProperty(PropertyType<T> propertyType, String path, T defaultValue) {
        super(path, HierarchicalValues.createContainerWithRoot(defaultValue));
        this.propertyType = propertyType;
    }

    @Override
    protected HierarchicalValues<T> getFromReader(PropertyReader reader) {
        Object obj = reader.getObject(getPath());
        if (!(obj instanceof Map<?, ?>)) {
            return null;
        }

        Map<String, T> values = new HashMap<>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
            if (entry.getKey() instanceof String) {
                T value = propertyType.convert(entry.getValue());
                if (value != null) {
                    values.put((String) entry.getKey(), value);
                }
            }
        }
        return HierarchicalValues.createContainer(getRootValueFromDefault(), values);
    }

    public T getRootValueFromDefault() {
        return getDefaultValue().getValue("");
    }

    @Override
    public Object toExportValue(HierarchicalValues<T> rulesContainer) {
        return rulesContainer.createValuesStream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> propertyType.toExportValue(e.getValue())));
    }
}
